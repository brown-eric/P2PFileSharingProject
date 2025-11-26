package utils;

import java.io.*;
import java.util.*;

public class ConfigReader {

    public static class PeerInfo {
        public int peerId;
        public String host;
        public int port;
        public boolean hasFile;

        public PeerInfo(int peerId, String host, int port, boolean hasFile) {
            this.peerId = peerId;
            this.host = host;
            this.port = port;
            this.hasFile = hasFile;
        }
    }

    private Map<String, String> commonCfg = new HashMap<>();
    private List<PeerInfo> peers = new ArrayList<>();

    private int selfPeerId;
    private PeerInfo selfInfo;

    public ConfigReader(String commonPath, String peerInfoPath, int selfPeerId) {
        this.selfPeerId = selfPeerId;

        loadCommonConfig(commonPath);
        loadPeerInfo(peerInfoPath);

        for (PeerInfo p : peers) {
            if (p.peerId == selfPeerId) {
                selfInfo = p;
                break;
            }
        }

        if (selfInfo == null)
            throw new RuntimeException("Peer ID " + selfPeerId + " not found in PeerInfo.cfg");
    }

    private void loadCommonConfig(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.trim().split("\\s+");
                commonCfg.put(parts[0], parts[1]);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error reading Common.cfg: " + e.getMessage());
        }
    }

    private void loadPeerInfo(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] p = line.trim().split("\\s+");
                int id = Integer.parseInt(p[0]);
                String host = p[1];
                int port = Integer.parseInt(p[2]);
                boolean hasFile = p.length > 3 && p[3].equals("1");

                peers.add(new PeerInfo(id, host, port, hasFile));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error reading PeerInfo.cfg: " + e.getMessage());
        }
    }

    public String getFileName() { return commonCfg.get("FileName"); }
    public int getFileSize() { return Integer.parseInt(commonCfg.get("FileSize")); }
    public int getPieceSize() { return Integer.parseInt(commonCfg.get("PieceSize")); }
    public int getPort() { return selfInfo.port; }

    public boolean selfHasFile() { return selfInfo.hasFile; }

    public List<Integer> getKnownPeers() {
        List<Integer> list = new ArrayList<>();
        for (PeerInfo p : peers) {
            list.add(p.peerId);
        }
        return list;
    }

    public PeerInfo getPeerInfo(int pid) {
        return peers.stream().filter(p -> p.peerId == pid).findFirst().orElse(null);
    }

    public peer.PeerState buildPeerState() {

        boolean hasFile = selfInfo.hasFile;
        int pieceSize = getPieceSize();
        String fileName = getFileName();
        long fileBytes = getFileSize();

        int numPieces = (int)Math.ceil((double) fileBytes / pieceSize);

        return new peer.PeerState(numPieces, hasFile, selfPeerId, pieceSize, fileName);
    }
}
