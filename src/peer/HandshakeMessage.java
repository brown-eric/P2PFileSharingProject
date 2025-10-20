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

    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(HANDSHAKE_LENGTH);
        buffer.put(HEADER.getBytes(StandardCharsets.US_ASCII));
        buffer.put(new byte[10]);
        buffer.putInt(peerId);
        return buffer.array();
    }

    public static HandshakeMessage fromBytes(byte[] data) {
        if (data.length != HANDSHAKE_LENGTH)
            throw new IllegalArgumentException("Invalid handshake length.");
        String header = new String(data, 0, 18, StandardCharsets.US_ASCII);
        if (!HEADER.equals(header))
            throw new IllegalArgumentException("Invalid handshake header.");
        ByteBuffer buffer = ByteBuffer.wrap(data, 28, 4);
        int peerId = buffer.getInt();
        return new HandshakeMessage(peerId);
    }
}
