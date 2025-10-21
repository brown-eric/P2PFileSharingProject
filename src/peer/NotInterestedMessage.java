package peer;

import java.nio.ByteBuffer;

public class NotInterestedMessage {
    public static final int TYPE = 3;

    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(5);
        buffer.putInt(1); // length (1 byte for type)
        buffer.put((byte) TYPE);
        return buffer.array();
    }
}
