package peer;

public class HaveMessage {
    public static final byte TYPE = 4;
    private final int pieceIndex;

    public HaveMessage(int pieceIndex) {
        this.pieceIndex = pieceIndex;
    }

    public int getPieceIndex() {
        return pieceIndex;
    }

    // Returns: 4 bytes length + 1 byte type + 4 bytes pieceIndex = 9 bytes
    public byte[] toBytes() {
        int length = 5;
        byte[] msg = new byte[9];
        msg[0] = (byte) ((length >> 24) & 0xff);
        msg[1] = (byte) ((length >> 16) & 0xff);
        msg[2] = (byte) ((length >> 8) & 0xff);
        msg[3] = (byte) (length & 0xff);
        msg[4] = TYPE;
        msg[5] = (byte) ((pieceIndex >> 24) & 0xff);
        msg[6] = (byte) ((pieceIndex >> 16) & 0xff);
        msg[7] = (byte) ((pieceIndex >> 8) & 0xff);
        msg[8] = (byte) (pieceIndex & 0xff);
        return msg;
    }

    // Expects payload to be only the 4 bytes representing the pieceIndex
    public static HaveMessage fromBytes(byte[] payload) {
        int index = ((payload[0] & 0xff) << 24) | ((payload[1] & 0xff) << 16) |
                ((payload[2] & 0xff) << 8) | (payload[3] & 0xff);
        return new HaveMessage(index);
    }
}
