package peer;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class ConnectionHandler implements Runnable {

    private Socket socket;
    private int selfPeerId;
    private PeerState peerState;  // Your class instance tracking your pieces

    public ConnectionHandler(Socket socket, int selfPeerId, PeerState peerState) {
        this.socket = socket;
        this.selfPeerId = selfPeerId;
        this.peerState = peerState;
    }

    @Override
    public void run() {
        try {
            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();

            // Send handshake
            HandshakeMessage handshakeOut = new HandshakeMessage(selfPeerId);
            os.write(handshakeOut.toBytes());
            os.flush();

            // Receive handshake
            byte[] handshakeBuf = new byte[HandshakeMessage.HANDSHAKE_LENGTH];
            int readBytes = is.read(handshakeBuf);
            if (readBytes != HandshakeMessage.HANDSHAKE_LENGTH)
                throw new Exception("Handshake not received properly!");

            HandshakeMessage handshakeIn = HandshakeMessage.fromBytes(handshakeBuf);
            System.out.println("Connected to peer with ID: " + handshakeIn.getPeerId());

            // Send Bitfield message after handshake
            byte[] myBitfield = peerState.getBitfieldBytes(); // get your pieces
            BitfieldMessage bitfieldMsg = new BitfieldMessage(myBitfield);
            os.write(bitfieldMsg.toBytes());
            os.flush();
            System.out.println("Sent bitfield message.");

            // Read messages in a loop (simplified for demo)
            while (true) {
                // Read message length
                byte[] lengthBuf = new byte[4];
                int lenRead = is.read(lengthBuf);
                if (lenRead != 4) break; // connection closed
                int msgLength = java.nio.ByteBuffer.wrap(lengthBuf).getInt();

                // Read message type
                byte[] typeBuf = new byte[1];
                int typeRead = is.read(typeBuf);
                if (typeRead != 1) break; // connection closed
                int msgType = typeBuf[0];

                // Read payload if exists
                byte[] payload = new byte[msgLength - 1];
                int payloadRead = is.read(payload);
                if (payloadRead != payload.length) break; // incomplete message

                // Handle message types
                switch (msgType) {
                    case 5: // Bitfield
                        BitfieldMessage receivedBitfield = BitfieldMessage.fromBytes(payload);
                        System.out.println("Received bitfield message with length: " + payload.length);
                        // TODO: update remote peer state
                        break;
                    case 2: // Interested
                        System.out.println("Peer is interested.");
                        // TODO: handle interested
                        break;
                    case 3: // Not Interested
                        System.out.println("Peer is not interested.");
                        // TODO: handle not interested
                        break;
                    default:
                        System.out.println("Received unknown message type: " + msgType);
                        break;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { socket.close(); } catch (Exception ignore) {}
        }
    }
}
