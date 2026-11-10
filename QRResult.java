package spybot;

public class QRResult {
    public boolean valid;
    public String callSign;
    public char location;
    public String reason;

    public QRResult(boolean valid, String callSign, char location, String reason) {
        this.valid    = valid;
        this.callSign = callSign;
        this.location = location;
        this.reason   = reason;
    }
}