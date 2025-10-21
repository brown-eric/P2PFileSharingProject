package peer;

import java.util.BitSet;
import java.util.Arrays;

public class PeerState {
    private boolean[] pieces;
    private byte[][] pieceData;


    public PeerState(int numPieces, boolean hasFullFile) {
        pieces = new boolean[numPieces];
        pieceData = new byte[numPieces][];
        for (int i = 0; i < numPieces; i++) {
            pieces[i] = hasFullFile;
            if (hasFullFile) {
                // For demo, fill with dummy data
                pieceData[i] = new byte[1024];
                Arrays.fill(pieceData[i], (byte) i);
            }
        }
    }

    public boolean[] getPieces() {
        return pieces;
    }

    // Return the data for a piece index, or dummy data if you wish
    public byte[] getPieceData(int pieceIndex) {
        if (pieceIndex < 0 || pieceIndex >= pieceData.length || pieceData[pieceIndex] == null) {
            // If you don't have the piece, send a blank array or throw error
            return new byte[1024]; // or throw new IllegalArgumentException(...)
        }
        return pieceData[pieceIndex];
    }

    // Store a received piece
    public void storePiece(int pieceIndex, byte[] data) {
        if (pieceIndex >= 0 && pieceIndex < pieces.length) {
            pieces[pieceIndex] = true;
            pieceData[pieceIndex] = data.clone();
            System.out.println("Stored piece " + pieceIndex);
        }
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
