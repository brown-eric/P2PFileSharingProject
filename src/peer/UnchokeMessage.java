package peer;

import java.nio.ByteBuffer;

public class UnchokeMessage {
    public static final int TYPE = 1;

    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(5);
        buffer.putInt(1); // length
        buffer.put((byte) TYPE);
        return buffer.array();
    }
}
