package peer;

import java.nio.ByteBuffer;

public class RequestMessage {
    public static final int TYPE = 6;
    private int pieceIndex;

    public RequestMessage(int pieceIndex) {
        this.pieceIndex = pieceIndex;
    }

    public int getPieceIndex() {
        return pieceIndex;
    }

    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(4 + 1 + 4); // length(4) + type(1) + pieceIndex(4)
        buffer.putInt(5); // length after this field = 1 + 4
        buffer.put((byte) TYPE);
        buffer.putInt(pieceIndex);
        return buffer.array();
    }

    public static RequestMessage fromBytes(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        int index = buffer.getInt();
        return new RequestMessage(index);
    }
}
