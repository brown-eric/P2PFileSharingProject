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

    private final String expectedFileHash = "D7C1822574DFC87DC4A70E47BBEEFC3F15A9EF5B902794084B06DF507713E5DC"; // precomputed for thefile
    //private final String expectedFileHash = "987c7cb3bf013388cc3fe6aa3094ef954b5616e682842ecdfcb85670d8b1087a"; //precomputed for tree.jpg

    public boolean verifyFileHash() {
        try {
            byte[] fileBytes = Files.readAllBytes(Paths.get("peer_" + peerId + "/" + fileName));
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] actualHash = digest.digest(fileBytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : actualHash) {
                sb.append(String.format("%02x", b));
            }
            String actualHashStr = sb.toString();
            boolean matches = actualHashStr.equalsIgnoreCase(expectedFileHash);
            if (matches) {
                System.out.println("File hash verification passed for " + fileName);
            } else {
                System.out.println("File hash verification failed! Expected " + expectedFileHash + " but got " + actualHashStr);
            }
            return matches;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public int getNumberOfPiecesOwned() {
        int count = 0;
        for (boolean hasPiece : pieces) {  // assuming you have boolean[] pieces
            if (hasPiece) count++;
        }
        return count;
    }

}
