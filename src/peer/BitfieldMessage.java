package peer;

import java.nio.ByteBuffer;

public class BitfieldMessage {
    public static final int TYPE = 5;
    private byte[] bitfield;

    public BitfieldMessage(byte[] bitfield) {
        this.bitfield = bitfield;
    }

    public byte[] toBytes() {
        int length = 1 + bitfield.length; // 1 byte for type + payload
        ByteBuffer buffer = ByteBuffer.allocate(4 + length);
        buffer.putInt(length);
        buffer.put((byte) TYPE);
        buffer.put(bitfield);
        return buffer.array();
    }

    public static BitfieldMessage fromBytes(byte[] msg) {
        return new BitfieldMessage(msg);
    }

    public byte[] getBitfield() {
        return bitfield;
    }
}
