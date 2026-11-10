package spybot;

public class DeliveryRecord {
    String timestamp, sender, message, status;
    char senderLocation, destination;
    boolean encryptionEnabled;
    int key, distanceCm, navTimeSec, authRetries;

    DeliveryRecord(String ts, String snd, char sl, char dst, String msg,
                   boolean enc, int k, String st, int dist, int nav, int ret) {
        timestamp      = ts;
        sender         = snd;
        senderLocation = sl;
        destination    = dst;
        message        = msg;
        encryptionEnabled = enc;
        key            = k;
        status         = st;
        distanceCm     = dist;
        navTimeSec     = nav;
        authRetries    = ret;
    }
}
