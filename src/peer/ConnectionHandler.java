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
    private byte[] remoteBitfield = null;
    private boolean completionChecked = false;

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
            if (r == -1) return -1;
            bytesRead += r;
        }
        return bytesRead;
    }

    @Override
    public void run() {
        try (InputStream is = socket.getInputStream(); OutputStream os = socket.getOutputStream()) {
            // Handshake
            byte[] hsBytes = new HandshakeMessage(selfPeerId).toBytes();
            os.write(hsBytes);
            os.flush();

            byte[] hsBuf = new byte[HandshakeMessage.HANDSHAKE_LENGTH];
            if (readFully(is, hsBuf) != HandshakeMessage.HANDSHAKE_LENGTH)
                throw new Exception("Failed handshake length");
            HandshakeMessage hsIn = HandshakeMessage.fromBytes(hsBuf);
            remotePeerId = hsIn.getPeerId();
            System.out.println("[Peer " + selfPeerId + "] Connected to peer " + remotePeerId);

            // Exchange bitfield
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

            remoteBitfield = remoteBitfieldMsg.getBitfield();

            // Initial interest determination
            updateInterest(os);

            // Notify uploadManager
            uploadManager.addPeer(remotePeerId, os);

            // Main message loop
            while (true) {
                if (readFully(is, lenBuf) != 4) break;
                int msgLen = ByteBuffer.wrap(lenBuf).getInt();

                if (readFully(is, typeBuf) != 1) break;
                byte msgType = typeBuf[0];

                payload = new byte[msgLen - 1];
                if (msgLen > 1) {
                    if (readFully(is, payload) != payload.length) break;
                }

                boolean[] myPieces = peerState.getPieces();

                switch (msgType) {
                    case ChokeMessage.TYPE:
                        uploadManager.setChoked(remotePeerId, true);
                        break;

                    case UnchokeMessage.TYPE:
                        uploadManager.setChoked(remotePeerId, false);

                        if (!completionChecked) {
                            boolean[] myPieces2 = peerState.getPieces();
                            for (int i = 0; i < myPieces2.length; i++) {
                                boolean remoteHas = ((remoteBitfield[i / 8] >> (7 - (i % 8))) & 1) == 1;
                                if (!myPieces2[i] && remoteHas) {
                                    os.write(new RequestMessage(i).toBytes());
                                    os.flush();
                                    break;
                                }
                            }
                        }
                        break;

                    case InterestedMessage.TYPE:
                        uploadManager.setInterested(remotePeerId, true);
                        break;
                    case NotInterestedMessage.TYPE:
                        uploadManager.setInterested(remotePeerId, false);
                        break;
                    case RequestMessage.TYPE:
                        if (!uploadManager.isChoked(remotePeerId)) {
                            RequestMessage req = RequestMessage.fromBytes(payload);
                            byte[] block = peerState.getPieceData(req.getPieceIndex());
                            if (block != null) {
                                os.write(new PieceMessage(req.getPieceIndex(), block).toBytes());
                                os.flush();
                            }
                        }
                        break;
                    case PieceMessage.TYPE:
                        PieceMessage pieceMsg = PieceMessage.fromBytes(payload);
                        peerState.storePiece(pieceMsg.getPieceIndex(), pieceMsg.getBlock());
                        uploadManager.broadcastHave(pieceMsg.getPieceIndex());

                        boolean complete1 = peerState.isComplete();

                        if (complete1 && !completionChecked) {
                            try {
                                completionChecked = true;
                                os.write(new NotInterestedMessage().toBytes());
                                os.flush();

                                if (peerState.verifyFileHash()) {
                                    System.out.println("[Peer " + selfPeerId + "] File integrity verified.");
                                } else {
                                    System.out.println("[Peer " + selfPeerId + "] File integrity verification failed!");
                                }

                                uploadManager.updatePeerCompletion(selfPeerId, true);
                            } catch (Exception e) {
                                System.err.println("[Peer " + selfPeerId + "] Exception during completion: " + e.getMessage());
                                e.printStackTrace();
                            }
                        } else if (!complete1) {
                            boolean[] myPieces1 = peerState.getPieces();
                            for (int i = 0; i < myPieces1.length; i++) {
                                boolean remoteHas = ((remoteBitfield[i / 8] >> (7 - (i % 8))) & 1) == 1;
                                if (!myPieces1[i] && remoteHas) {
                                    os.write(new RequestMessage(i).toBytes());
                                    os.flush();
                                    break;
                                }
                            }
                        }
                        break;

                    case HaveMessage.TYPE:
                        HaveMessage recvHave = HaveMessage.fromBytes(payload);
                        int haveIndex = recvHave.getPieceIndex();

                        if (remoteBitfield == null) {
                            remoteBitfield = new byte[(peerState.getPieces().length + 7) / 8];
                        }
                        int byteIndex = haveIndex / 8;
                        int bitIndex = 7 - (haveIndex % 8);
                        remoteBitfield[byteIndex] |= (1 << bitIndex);

                        updateInterest(os);
                        break;
                    default:
                        break;
                }
            }
        } catch (Exception e) {
            System.err.println("[Peer " + selfPeerId + "] Exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (Exception ignore) {
            }
        }
    }

    private void updateInterest(OutputStream os) throws Exception {
        boolean complete = peerState.isComplete();
        if (complete) {
            os.write(new NotInterestedMessage().toBytes());
            os.flush();
            return;
        }
        boolean[] havePieces = peerState.getPieces();
        boolean interested = false;
        for (int i = 0; i < havePieces.length; i++) {
            boolean remoteHas = ((remoteBitfield[i / 8] >> (7 - (i % 8))) & 1) == 1;
            if (!havePieces[i] && remoteHas) {
                interested = true;
                break;
            }
        }
        if (interested) {
            os.write(new InterestedMessage().toBytes());
            os.flush();
        } else {
            os.write(new NotInterestedMessage().toBytes());
            os.flush();
        }
    }
}
