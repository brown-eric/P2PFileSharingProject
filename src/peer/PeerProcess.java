package peer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PeerProcess {

    private static final int THREAD_POOL_SIZE = 10;
    private final int peerId;
    private final int port;
    private final List<Integer> knownPeers; // list of peer IDs this peer can connect to
    private PeerState peerState;
    private UploadManager uploadManager;
    private ExecutorService threadPool;

    public PeerProcess(int peerId, int port, List<Integer> knownPeers, PeerState peerState) {
        this.peerId = peerId;
        this.port = port;
        this.knownPeers = knownPeers;
        this.peerState = peerState;
        this.uploadManager = new UploadManager(new ArrayList<>(knownPeers));
        this.threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }

    public void start() throws IOException {
        // Start the upload manager thread
        new Thread(uploadManager).start();

        // Start server socket to accept incoming connections
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Peer " + peerId + " listening on port " + port);

        // Start background thread to accept incoming peers
        threadPool.submit(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Socket socket = serverSocket.accept();
                    // For incoming connections, create a ConnectionHandler with uploadManager
                    ConnectionHandler handler = new ConnectionHandler(socket, peerId, peerState, uploadManager);
                    threadPool.submit(handler);
                }
            } catch (IOException e) {
                System.out.println("Server socket closed or error: " + e.getMessage());
            }
        });

        // Connect to known peers (excluding self)
        for (int remotePeerId : knownPeers) {
            if (remotePeerId != this.peerId) {
                // Assuming ports are mapped or known; simplify by using port + remotePeerId offset
                int remotePort = port + (remotePeerId - peerId);
                try {
                    Socket socket = new Socket("localhost", remotePort);
                    ConnectionHandler handler = new ConnectionHandler(socket, peerId, peerState, uploadManager);
                    threadPool.submit(handler);
                } catch (IOException e) {
                    System.out.println("Failed to connect to peer " + remotePeerId + ": " + e.getMessage());
                }
            }
        }
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java peer.PeerProcess <peerId> <port> <commaSeparatedKnownPeers>");
            return;
        }

        int peerId = Integer.parseInt(args[0]);
        int port = Integer.parseInt(args[1]);
        String[] peerStrings = args[2].split(",");
        List<Integer> knownPeers = new ArrayList<>();
        for (String s : peerStrings) {
            knownPeers.add(Integer.parseInt(s));
        }

        // Example: initialize PeerState, all pieces missing except peer 1001
        int numPieces = 16;
        boolean hasFullFile = (peerId == 1001); // ONLY 1001 starts with all pieces
        PeerState peerState = new PeerState(numPieces, hasFullFile);

        PeerProcess peerProcess = new PeerProcess(peerId, port, knownPeers, peerState);
        try {
            peerProcess.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
