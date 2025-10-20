package peer;

import java.net.ServerSocket;
import java.net.Socket;

// args[0] = "server" or "client"
// args[1] = peerId
// args[2] = port (for server) or serverPort (for client)
public class PeerProcess {
    public static void main(String[] args) throws Exception {
        int peerId = Integer.parseInt(args[1]);
        int numPieces = 16; // Placeholder, should be set from config
        boolean hasFile = args[0].equalsIgnoreCase("server"); // Example usage

        PeerState peerState = new PeerState(numPieces, hasFile);

        if (args[0].equalsIgnoreCase("server")) {
            int port = Integer.parseInt(args[2]);
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Listening as peer " + peerId + " on port " + port);
            Socket socket = serverSocket.accept();
            new Thread(new ConnectionHandler(socket, peerId, peerState)).start();
        } else if (args[0].equalsIgnoreCase("client")) {
            String host = "localhost";
            int port = Integer.parseInt(args[2]);
            Socket socket = new Socket(host, port);
            System.out.println("Connected to server from peer " + peerId);
            new Thread(new ConnectionHandler(socket, peerId, peerState)).start();
        }
    }
}
