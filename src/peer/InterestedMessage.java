package peer;

import java.nio.ByteBuffer;

public class InterestedMessage {
    public static final int TYPE = 2;

    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(5);
        buffer.putInt(1); // length
        buffer.put((byte) TYPE);
        return buffer.array();
    }
}
