package peer;

import java.nio.ByteBuffer;

public class PieceMessage {
    public static final int TYPE = 7;
    private int pieceIndex;
    private byte[] block;

    public PieceMessage(int pieceIndex, byte[] block) {
        this.pieceIndex = pieceIndex;
        this.block = block;
    }

    public int getPieceIndex() {
        return pieceIndex;
    }

    public byte[] getBlock() {
        return block;
    }

    public byte[] toBytes() {
        int length = 1 + 4 + block.length;
        ByteBuffer buffer = ByteBuffer.allocate(4 + length);
        buffer.putInt(length);
        buffer.put((byte) TYPE);
        buffer.putInt(pieceIndex);
        buffer.put(block);
        return buffer.array();
    }

    public static PieceMessage fromBytes(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        int index = buffer.getInt();
        byte[] blk = new byte[data.length - 4];
        buffer.get(blk);
        return new PieceMessage(index, blk);
    }
}
