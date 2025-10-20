package peer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class HandshakeMessage {
    public static final String HEADER = "P2PFILESHARINGPROJ";
    public static final int HANDSHAKE_LENGTH = 32;

    private int peerId;

    public HandshakeMessage(int peerId) {
        this.peerId = peerId;
    }

    public int getPeerId() {
        return peerId;
    }

    // Build handshake message as byte array
    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(HANDSHAKE_LENGTH);

        // Header: 18 bytes
        buffer.put(HEADER.getBytes(StandardCharsets.US_ASCII));

        // Zero bits: 10 bytes
        buffer.put(new byte[10]);

        // Peer ID: 4 bytes (big-endian)
        buffer.putInt(peerId);

        return buffer.array();
    }

    // Parse handshake from incoming bytes
    public static HandshakeMessage fromBytes(byte[] data) {
        if (data.length != HANDSHAKE_LENGTH) throw new IllegalArgumentException("Invalid handshake length.");
        String header = new String(data, 0, 18, StandardCharsets.US_ASCII);
        if (!HEADER.equals(header)) throw new IllegalArgumentException("Invalid handshake header.");
        // skip 10 bytes (18-27)
        ByteBuffer buffer = ByteBuffer.wrap(data, 28, 4);
        int peerId = buffer.getInt();
        return new HandshakeMessage(peerId);
    }
}
