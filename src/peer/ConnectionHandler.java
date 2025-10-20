package peer;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class ConnectionHandler implements Runnable {
    private Socket socket;
    private int selfPeerId;
    private PeerState peerState;

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

            // Handshake
            HandshakeMessage outHandshake = new HandshakeMessage(selfPeerId);
            os.write(outHandshake.toBytes());
            os.flush();

            byte[] handshakeBuf = new byte[HandshakeMessage.HANDSHAKE_LENGTH];
            int read = is.read(handshakeBuf);
            if (read != HandshakeMessage.HANDSHAKE_LENGTH) throw new Exception("Handshake not received properly!");
            HandshakeMessage inHandshake = HandshakeMessage.fromBytes(handshakeBuf);
            System.out.println("Connected to peer with ID: " + inHandshake.getPeerId());

            // Send Bitfield message
            BitfieldMessage bitfieldMessage = new BitfieldMessage(peerState.getBitfieldBytes());
            os.write(bitfieldMessage.toBytes());
            os.flush();
            System.out.println("Sent bitfield message.");

            // Receive Bitfield message
            byte[] lengthBuf = new byte[4];
            is.read(lengthBuf);
            int msgLength = java.nio.ByteBuffer.wrap(lengthBuf).getInt();
            byte[] typeBuf = new byte[1];
            is.read(typeBuf);
            int msgType = typeBuf[0];

            if (msgType == BitfieldMessage.TYPE) {
                byte[] payload = new byte[msgLength - 1];
                is.read(payload);
                BitfieldMessage received = BitfieldMessage.fromBytes(payload);
                System.out.println("Received bitfield message with length: " + received.getBitfield().length);
                // Here, you could update internally which pieces this peer believes the other peer has.
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { socket.close(); } catch (Exception ignore) {}
        }
    }
}
