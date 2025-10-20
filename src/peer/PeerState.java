package peer;

import java.util.BitSet;

public class PeerState {
    private boolean[] pieces;

    public PeerState(int numPieces, boolean hasCompleteFile) {
        pieces = new boolean[numPieces];
        for (int i = 0; i < numPieces; i++) {
            pieces[i] = hasCompleteFile;
        }
    }

    public boolean[] getPieces() {
        return pieces;
    }

    public byte[] getBitfieldBytes() {
        BitSet bitSet = new BitSet(pieces.length);
        for (int i = 0; i < pieces.length; i++) {
            if (pieces[i]) bitSet.set(i);
        }
        byte[] array = bitSet.toByteArray();
        int neededLength = (int) Math.ceil(pieces.length / 8.0);
        if (array.length < neededLength) {
            byte[] padded = new byte[neededLength];
            System.arraycopy(array, 0, padded, 0, array.length);
            array = padded;
        }
        return array;
    }
}
