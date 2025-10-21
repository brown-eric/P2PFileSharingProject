package peer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.util.BitSet;
import java.util.Arrays;

public class PeerState {
    private boolean[] pieces;
    private byte[][] pieceData;
    private int pieceSize;
    private int peerId;
    private String fileName = "tree.jpg"; // default value for testing


    public PeerState(int numPieces, boolean hasFullFile, int peerId, int pieceSize, String fileName) {
        this.peerId = peerId;
        this.pieceSize = pieceSize;
        this.fileName = fileName;
        pieces = new boolean[numPieces];
        Arrays.fill(pieces, hasFullFile);
    }

    public boolean[] getPieces() {
        return pieces;
    }

    // Return the data for a piece index
    // TODO: still some bugs when requesting necessary pieces especially towards the end of the file
    public byte[] getPieceData(int pieceIndex) {
        try (RandomAccessFile raf = new RandomAccessFile("peer_" + peerId + "/" + fileName, "r")) {
            long offset = (long) pieceIndex * pieceSize;
            raf.seek(offset);
            byte[] buffer = new byte[pieceSize];
            int bytesRead = raf.read(buffer);
            if (bytesRead < pieceSize) {
                return Arrays.copyOf(buffer, bytesRead);
            }
            return buffer;
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    // Store a received piece as soon as it is received
    public synchronized void storePiece(int pieceIndex, byte[] data) {
        pieces[pieceIndex] = true;
        File dir = new File("peer_" + peerId);
        if (!dir.exists()) dir.mkdirs();
        String path = "peer_" + peerId + "/" + fileName;
        try (RandomAccessFile raf = new RandomAccessFile(path, "rw")) {
            long offset = (long) pieceIndex * pieceSize;
            raf.seek(offset);
            raf.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (isComplete()) {
            System.out.println("✅ Peer " + peerId + " has fully downloaded " + fileName);
        }
    }

    public boolean isComplete() {
        for (boolean p : pieces) {
            if (!p) {
                return false;
            }
        }
        return true;
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
