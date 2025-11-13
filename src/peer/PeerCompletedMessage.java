package peer;

public class PeerCompletedMessage extends Message {
    public static final byte TYPE = 5; // Choose a unique type
    private int peerId;

    public PeerCompletedMessage(int peerId) {
        this.peerId = peerId;
    }

    public int getPeerId() {
        return peerId;
    }

    @Override
    public byte[] toBytes() {
        byte[] payload = new byte[4];
        payload[0] = (byte) (peerId >> 24);
        payload[1] = (byte) (peerId >> 16);
        payload[2] = (byte) (peerId >> 8);
        payload[3] = (byte) peerId;
        return Message.buildMessage(TYPE, payload);
    }

    public static PeerCompletedMessage fromBytes(byte[] payload) {
        int peerId = ((payload[0] & 0xFF) << 24) |
                ((payload[1] & 0xFF) << 16) |
                ((payload[2] & 0xFF) << 8)  |
                (payload[3] & 0xFF);
        return new PeerCompletedMessage(peerId);
    }
}
