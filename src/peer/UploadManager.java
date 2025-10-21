package peer;

import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class UploadManager implements Runnable {

    private static final int PREFERRED_NEIGHBORS_COUNT = 2;
    private static final int UNCHOKE_INTERVAL_MS = 5000;
    private static final int OPTIMISTIC_UNCHOKE_INTERVAL_MS = 15000;

    private Map<Integer, Long> downloadRates = new HashMap<>();
    private Set<Integer> preferredNeighbors = new HashSet<>();
    private Integer optimisticNeighbor = null;

    private Map<Integer, Boolean> chokeStatus = new HashMap<>();
    private Map<Integer, Boolean> interestedStatus = new HashMap<>();
    private Map<Integer, OutputStream> peerOutputs = new HashMap<>();

    private final Object lock = new Object();

    public UploadManager(List<Integer> initialPeers) {
        for (Integer peerId : initialPeers) {
            chokeStatus.put(peerId, true);
            interestedStatus.put(peerId, false);
            downloadRates.put(peerId, 0L);
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
            }
        } catch (InterruptedException e) {
            // Thread interrupted, exit gracefully
        }
    }

    private void updatePreferredNeighbors() {
        List<Map.Entry<Integer, Long>> sorted = new ArrayList<>();
        synchronized (lock) {
            sorted.addAll(downloadRates.entrySet());
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
                        }
                    } else {
                        if (!choked) {
                            // Choke
                            os.write(new ChokeMessage().toBytes());
                            os.flush();
                            chokeStatus.put(peerId, true);
                            System.out.println("Choked peer " + peerId);
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Failed to send choke/unchoke to peer " + peerId + ": " + e);
                }
            }
            preferredNeighbors = newPreferred;
        }
    }

    private void updateOptimisticUnchoke() {
        List<Integer> candidates = new ArrayList<>();
        synchronized (lock) {
            for (Integer peerId : peerOutputs.keySet()) {
                if (!preferredNeighbors.contains(peerId)) {
                    candidates.add(peerId);
                }
            }
        }
        if (!candidates.isEmpty()) {
            optimisticNeighbor = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
            System.out.println("Optimistically unchoking peer " + optimisticNeighbor);
        }
    }
}
