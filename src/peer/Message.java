package peer;
public abstract class Message {
    // Possible shared utilities, message header methods, etc.
    public static byte[] buildMessage(byte type, byte[] payload) {
        int totalLen = payload.length + 1;
        byte[] out = new byte[4 + totalLen];
        out[0] = (byte)((totalLen >> 24) & 0xff);
        out[1] = (byte)((totalLen >> 16) & 0xff);
        out[2] = (byte)((totalLen >> 8) & 0xff);
        out[3] = (byte)(totalLen & 0xff);
        out[4] = type;
        System.arraycopy(payload, 0, out, 5, payload.length);
        return out;
    }
    public abstract byte[] toBytes();
}
