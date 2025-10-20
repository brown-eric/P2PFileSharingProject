package peer;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class ConnectionHandler implements Runnable {
    private Socket socket;
    private int selfPeerId;
    private int expectedPeerId;  // Can be set for validation

    public ConnectionHandler(Socket socket, int selfPeerId, int expectedPeerId) {
        this.socket = socket;
        this.selfPeerId = selfPeerId;
        this.expectedPeerId = expectedPeerId;
    }

    @Override
    public void run() {
        try {
            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();

            // Send handshake
            HandshakeMessage outHandshake = new HandshakeMessage(selfPeerId);
            os.write(outHandshake.toBytes());
            os.flush();

            // Receive handshake
            byte[] handshakeBuf = new byte[HandshakeMessage.HANDSHAKE_LENGTH];
            int read = is.read(handshakeBuf);
            if (read != HandshakeMessage.HANDSHAKE_LENGTH) throw new Exception("Handshake not received properly!");

            HandshakeMessage inHandshake = HandshakeMessage.fromBytes(handshakeBuf);
            System.out.println("Connected to peer with ID: " + inHandshake.getPeerId());

            // Optionally: validate expected peer ID here

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { socket.close(); } catch (Exception ignore) {}
        }
    }
}
