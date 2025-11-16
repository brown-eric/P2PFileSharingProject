package utils;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class Logger {
    public static void log(String message, int peerId) {
        Instant now = Instant.now();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        String formattedTimestamp = now.atZone(java.time.ZoneId.systemDefault()).format(formatter);

        String path = "logs/log_peer_" + peerId + ".log";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path, true))) {
            writer.write("[" + formattedTimestamp + "] " + message);
            writer.newLine();
        }
        catch (IOException e) {
            System.err.println("Error writing to peer_" + peerId + " log file: " + e.getMessage());
        }
    }
}

