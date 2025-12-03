package peer;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.util.Arrays;
import java.security.MessageDigest;
import java.nio.file.Files;
import java.nio.file.Paths;

public class PeerState {
    private boolean[] pieces;
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

    public void storePiece(int pieceIndex, byte[] data) {
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
    }

    public boolean isComplete() {
        for (boolean hasPiece : pieces) {
            if (!hasPiece) return false;
        }
        return true;
    }

    public byte[] getBitfieldBytes() {
        int neededLength = (int) Math.ceil(pieces.length / 8.0);
        byte[] array = new byte[neededLength];
        for (int i = 0; i < pieces.length; i++) {
            if (pieces[i]) {
                array[i / 8] |= (1 << (7 - (i % 8)));
            }
        }
        return array;
    }

    public int getNumberOfPiecesOwned() {
        int count = 0;
        for (boolean hasPiece : pieces) {  // assuming you have boolean[] pieces
            if (hasPiece) count++;
        }
        return count;
    }

}
