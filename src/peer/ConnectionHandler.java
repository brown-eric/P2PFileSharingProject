package peer;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

public class ConnectionHandler implements Runnable {

    private Socket socket;
    private int selfPeerId;
    private int remotePeerId = -1;
    private PeerState peerState;
    private UploadManager uploadManager;
    private byte[] remoteBitfield = null; // Store remote bitfield globally

    public ConnectionHandler(Socket socket, int selfPeerId, PeerState peerState, UploadManager uploadManager) {
        this.socket = socket;
        this.selfPeerId = selfPeerId;
        this.peerState = peerState;
        this.uploadManager = uploadManager;
    }

    private int readFully(InputStream is, byte[] buffer) throws Exception {
        int bytesRead = 0;
        while (bytesRead < buffer.length) {
            int r = is.read(buffer, bytesRead, buffer.length - bytesRead);
            if (r == -1) return -1; // End of stream
            bytesRead += r;
        }
        return bytesRead;
    }

    @Override
    public void run() {
        try (InputStream is = socket.getInputStream(); OutputStream os = socket.getOutputStream()) {

            // 1. Handshake
            byte[] hsBytes = new HandshakeMessage(selfPeerId).toBytes();
            os.write(hsBytes);
            os.flush();

            byte[] hsBuf = new byte[HandshakeMessage.HANDSHAKE_LENGTH];
            if (readFully(is, hsBuf) != HandshakeMessage.HANDSHAKE_LENGTH)
                throw new Exception("Failed handshake length");
            HandshakeMessage hsIn = HandshakeMessage.fromBytes(hsBuf);
            remotePeerId = hsIn.getPeerId();
            System.out.println("Connected to peer " + remotePeerId);

            // 2. Exchange bitfield
            byte[] myBitfield = peerState.getBitfieldBytes();
            os.write(new BitfieldMessage(myBitfield).toBytes());
            os.flush();

            byte[] lenBuf = new byte[4];
            if (readFully(is, lenBuf) != 4) throw new Exception("Failed to read message length");
            int len = ByteBuffer.wrap(lenBuf).getInt();

            byte[] typeBuf = new byte[1];
            if (readFully(is, typeBuf) != 1) throw new Exception("Failed to read message type");
            if (typeBuf[0] != BitfieldMessage.TYPE)
                throw new Exception("Expected bitfield");
            byte[] payload = new byte[len - 1];
            if (readFully(is, payload) != payload.length) throw new Exception("Failed to read bitfield payload");
            BitfieldMessage remoteBitfieldMsg = BitfieldMessage.fromBytes(payload);
            System.out.println("Received bitfield of length " + payload.length);

            // Store the remote bitfield for use later
            remoteBitfield = remoteBitfieldMsg.getBitfield();

            // 3. Interest determination
            boolean interested = false;
            boolean[] havePieces = peerState.getPieces();
            for (int i = 0; i < havePieces.length; i++) {
                boolean remoteHas = ((remoteBitfield[i / 8] >> (7 - (i % 8))) & 1) == 1;
                if (!havePieces[i] && remoteHas) {
                    interested = true;
                    break;
                }
            }

            if (interested) {
                os.write(new InterestedMessage().toBytes());
                System.out.println("Sent Interested");
            } else {
                os.write(new NotInterestedMessage().toBytes());
                System.out.println("Sent Not Interested");
            }
            os.flush();

            // Notify UploadManager about connected peer to track choking state
            uploadManager.addPeer(remotePeerId, os);

            // 4. Main message loop
            while (true) {
                if (readFully(is, lenBuf) != 4) break;
                int msgLen = ByteBuffer.wrap(lenBuf).getInt();

                if (readFully(is, typeBuf) != 1) break;
                byte msgType = typeBuf[0];

                payload = new byte[msgLen - 1];
                if (msgLen > 1) {
                    if (readFully(is, payload) != payload.length) break;
                }

                // Only declare myPieces once up here
                boolean[] myPieces = peerState.getPieces();

                switch (msgType) {
                    case ChokeMessage.TYPE:
                        System.out.println("Received Choke from " + remotePeerId);
                        uploadManager.setChoked(remotePeerId, true);
                        break;
                    case UnchokeMessage.TYPE:
                        System.out.println("Received Unchoke from " + remotePeerId);
                        uploadManager.setChoked(remotePeerId, false);

                        // Request next missing piece from remote
                        for (int i = 0; i < myPieces.length; i++) {
                            boolean remoteHas = ((remoteBitfield[i / 8] >> (7 - (i % 8))) & 1) == 1;
                            if (!myPieces[i] && remoteHas) {
                                os.write(new RequestMessage(i).toBytes());
                                os.flush();
                                System.out.println("Requested piece " + i + " from peer " + remotePeerId);
                                break;
                            }
                        }
                        break;

                    case InterestedMessage.TYPE:
                        System.out.println("Peer " + remotePeerId + " is interested.");
                        uploadManager.setInterested(remotePeerId, true);
                        break;
                    case NotInterestedMessage.TYPE:
                        System.out.println("Peer " + remotePeerId + " is not interested.");
                        uploadManager.setInterested(remotePeerId, false);
                        break;
                    case RequestMessage.TYPE:
                        if (uploadManager.isChoked(remotePeerId)) {
                            System.out.println("Ignoring request from choked peer " + remotePeerId);
                        } else {
                            RequestMessage req = RequestMessage.fromBytes(payload);
                            System.out.println("Peer " + remotePeerId + " requested piece " + req.getPieceIndex());

                            byte[] block = peerState.getPieceData(req.getPieceIndex());
                            if (block == null) {
                                System.out.println("Requested piece data not available: " + req.getPieceIndex());
                                break;
                            }
                            os.write(new PieceMessage(req.getPieceIndex(), block).toBytes());
                            os.flush();
                            System.out.println("Sent piece " + req.getPieceIndex() + " to peer " + remotePeerId);
                        }
                        break;
                    case PieceMessage.TYPE:
                        PieceMessage pieceMsg = PieceMessage.fromBytes(payload);
                        System.out.println("Received piece " + pieceMsg.getPieceIndex() + " from " + remotePeerId);
                        peerState.storePiece(pieceMsg.getPieceIndex(), pieceMsg.getBlock());

                        // Immediately request next missing piece from same peer
                        for (int i = 0; i < myPieces.length; i++) {
                            boolean remoteHas = ((remoteBitfield[i / 8] >> (7 - (i % 8))) & 1) == 1;
                            if (!myPieces[i] && remoteHas) {
                                os.write(new RequestMessage(i).toBytes());
                                os.flush();
                                System.out.println("Requested piece " + i + " from peer " + remotePeerId);
                                break;
                            }
                        }
                        break;

                    default:
                        System.out.println("Unknown message type: " + msgType);
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (Exception ignore) {
            }
        }
    }
}
