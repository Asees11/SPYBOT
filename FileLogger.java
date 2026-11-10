package spybot;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class FileLogger {

    private final String logFilePath;
    private final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public FileLogger() {
        this.logFilePath = "logs/session_" +
                LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".txt";
    }

    public void saveLog(List<DeliveryRecord> history) {
        try {
            Files.createDirectories(Paths.get("logs"));
            StringBuilder sb = new StringBuilder();
            sb.append("SPYBOT SESSION LOG - ").append(nowTs()).append("\n\n");
            sb.append("TOTAL DELIVERIES: ").append(history.size()).append("\n\n");

            for (int i = 0; i < history.size(); i++) {
                DeliveryRecord r = history.get(i);
                String dec = r.encryptionEnabled ?
                        Encryption.caesarDecrypt(r.message, r.key) : r.message;

                sb.append("RECORD #").append(i + 1).append("\n");
                sb.append("Time      : ").append(r.timestamp).append("\n");
                sb.append("Sender    : ").append(r.sender)
                  .append(" (").append(r.senderLocation).append(")\n");
                sb.append("To        : ").append(r.destination).append("\n");
                sb.append("Encrypted : ")
                  .append(r.message.isEmpty() ? "(empty)" : r.message).append("\n");
                sb.append("Decoded   : ")
                  .append(dec.isEmpty() ? "(empty)" : dec).append("\n");
                sb.append("Enc       : ")
                  .append(r.encryptionEnabled ?
                          "ENABLED (key=" + r.key + ")" : "DISABLED").append("\n");
                sb.append("Status    : ").append(r.status).append("\n\n");
            }

            Files.writeString(Paths.get(logFilePath), sb.toString());
            System.out.println("[LOG] Saved: " + logFilePath);
        } catch (IOException e) {
            System.out.println("[LOG] Error: " + e.getMessage());
        }
    }

    public String getLogFilePath() { return logFilePath; }

    private String nowTs() {
        return LocalDateTime.now().format(TS);
    }
}