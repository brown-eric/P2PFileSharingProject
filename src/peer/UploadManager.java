package peer;

import utils.Logger;

import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

public class UploadManager implements Runnable {

    private static final int PREFERRED_NEIGHBORS_COUNT = 4;
    private static final int UNCHOKE_INTERVAL_MS = 5000;
    private static final int OPTIMISTIC_UNCHOKE_INTERVAL_MS = 15000;
    private Map<Integer, Boolean> peerCompletionMap = new ConcurrentHashMap<>();
    private Map<Integer, Long> downloadRates = new HashMap<>();
    private Set<Integer> preferredNeighbors = new HashSet<>();
    private Integer optimisticNeighbor = null;
    private Map<Integer, Boolean> chokeStatus = new HashMap<>();
    private Map<Integer, Boolean> interestedStatus = new HashMap<>();
    private Map<Integer, OutputStream> peerOutputs = new HashMap<>();
    private volatile boolean shutdown = false;
    private final int selfPeerId;


    private final Object lock = new Object();

    public UploadManager(List<Integer> initialPeers, int selfPeerId) {
        this.selfPeerId = selfPeerId;
        for (Integer peerId : initialPeers) {
            chokeStatus.put(peerId, true);
            interestedStatus.put(peerId, false);
            downloadRates.put(peerId, 0L);
            peerCompletionMap.put(peerId, false);
        }
    }

    public void addPeer(int peerId, OutputStream outputStream) {
        synchronized (lock) {
            peerOutputs.put(peerId, outputStream);
            chokeStatus.putIfAbsent(peerId, true);
            interestedStatus.putIfAbsent(peerId, false);
            downloadRates.putIfAbsent(peerId, 0L);
        }
    }

    public void setChoked(int peerId, boolean isChoked) {
        synchronized (lock) {
            chokeStatus.put(peerId, isChoked);
        }
    }

    public void setInterested(int peerId, boolean isInterested) {
        synchronized (lock) {
            interestedStatus.put(peerId, isInterested);
        }
    }

    public boolean isChoked(int peerId) {
        synchronized (lock) {
            return chokeStatus.getOrDefault(peerId, true);
        }
    }

    public void updateDownloadRate(int peerId, long bytesDownloaded) {
        synchronized (lock) {
            downloadRates.put(peerId, bytesDownloaded);
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                updatePreferredNeighbors();
                updateOptimisticUnchoke();
                Thread.sleep(UNCHOKE_INTERVAL_MS);

                // Check if all peers are complete
                synchronized (this) {
                    if (shutdown) {
                        break;
                    }
                }
            }
        } catch (InterruptedException e) {
            // exit here
        }

        // Close all output streams
        for (OutputStream os : peerOutputs.values()) {
            try {
                os.close();
            } catch (Exception ignore) {}
        }
        System.exit(0);
    }


    private void updatePreferredNeighbors() {
        List<Map.Entry<Integer, Long>> sorted = new ArrayList<>();
        synchronized (lock) {
            // Only consider peers that are interested for preferred neighbor
            for (Map.Entry<Integer, Long> entry : downloadRates.entrySet()) {
                if (interestedStatus.getOrDefault(entry.getKey(), false)) {
                    sorted.add(entry);
                }
            }
        }
        sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

        Set<Integer> newPreferred = new HashSet<>();
        for (int i = 0; i < Math.min(PREFERRED_NEIGHBORS_COUNT, sorted.size()); i++) {
            newPreferred.add(sorted.get(i).getKey());
        }

        synchronized (lock) {
            for (Integer peerId : peerOutputs.keySet()) {
                boolean choked = chokeStatus.getOrDefault(peerId, true);
                OutputStream os = peerOutputs.get(peerId);
                try {
                    if (newPreferred.contains(peerId) || peerId.equals(optimisticNeighbor)) {
                        if (choked) {
                            // Unchoke
                            os.write(new UnchokeMessage().toBytes());
                            os.flush();
                            chokeStatus.put(peerId, false);
                            System.out.println("Unchoked peer " + peerId);
                            Logger.log("Unchoked peer " + peerId, selfPeerId);
                        }
                    } else {
                        if (!choked) {
                            // Choke
                            os.write(new ChokeMessage().toBytes());
                            os.flush();
                            chokeStatus.put(peerId, true);
                            System.out.println("Choked peer " + peerId);
                            Logger.log("Choked peer " + peerId, selfPeerId);
                        }
                    }
                } catch (Exception e) {
                    String msg = e.getMessage() != null ? e.getMessage() : "";
                    if (!msg.contains("Socket closed") && !msg.contains("Connection reset")) {
                        System.out.println("Failed to send choke/unchoke to peer " + peerId + ": " + msg);
                        Logger.log("Failed to send choke/unchoke to peer " + peerId + ": " + msg, selfPeerId);
                    }
                }
            }
            preferredNeighbors = newPreferred;
        }
    }

    private void updateOptimisticUnchoke() {
        List<Integer> candidates = new ArrayList<>();
        synchronized (lock) {
            for (Integer peerId : peerOutputs.keySet()) {
                if (!preferredNeighbors.contains(peerId) && interestedStatus.getOrDefault(peerId, false)) {
                    candidates.add(peerId);
                }
            }
        }
        if (!candidates.isEmpty()) {
            optimisticNeighbor = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
            System.out.println("Optimistically unchoking peer " + optimisticNeighbor);
            Logger.log("Optimistically unchoking peer " + optimisticNeighbor, selfPeerId);
        }
    }

    public void broadcastHave(int pieceIndex) {
        byte[] msgBytes = new HaveMessage(pieceIndex).toBytes();
        synchronized (lock) {
            for (OutputStream os : peerOutputs.values()) {
                try {
                    os.write(msgBytes);
                    os.flush();
                } catch (Exception e) {
                    String msg = e.getMessage() != null ? e.getMessage() : "";
                    if (!msg.contains("Socket closed") && !msg.contains("Connection reset")) {
                        System.out.println("Failed to send HAVE message: " + msg);
                        Logger.log("Failed to send HAVE message: " + msg, selfPeerId);
                    }
                }
            }
        }
    }

    // called when a peer has the complete file
    public void updatePeerCompletion(int peerId, boolean completed) {
        peerCompletionMap.put(peerId, completed);
        // System.out.println("[UploadManager] updatePeerCompletion called for: " + peerId + " = " + completed);
        // System.out.println("[UploadManager] peerCompletionMap: " + peerCompletionMap);

        boolean allComplete = true;
        for (boolean b : peerCompletionMap.values()) {
            if (!b) {
                allComplete = false;
                break;
            }
        }
        if (allComplete) {
            System.out.println("All peers completed. Shutting down.");
            Logger.log("All peers completed. Shutting down.", selfPeerId);
            shutdown = true;
            synchronized (this) {
                notifyAll(); // Wake up any waiting threads
            }
        }
    }


    public void broadcastPeerCompleted(int peerId) {
        byte[] msgBytes = new PeerCompletedMessage(peerId).toBytes();
        synchronized (lock) {
            for (OutputStream os : peerOutputs.values()) {
                try {
                    os.write(msgBytes);
                    os.flush();
                } catch (Exception e) {
                    String msg = e.getMessage() != null ? e.getMessage() : "";
                    if (!msg.contains("Socket closed") && !msg.contains("Connection reset")) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public Map<Integer, Boolean> getPeerCompletionMap() {
        return Collections.unmodifiableMap(peerCompletionMap);
    }

}
