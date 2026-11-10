package spybot;

import swiftbot.SwiftBotAPI;
import java.awt.image.BufferedImage;
import java.util.Scanner;

public class Authentication {

    private final SwiftBotAPI swiftBot;
    private final LEDController led;
    private final Scanner sc;

    public Authentication(SwiftBotAPI swiftBot, LEDController led, Scanner sc) {
        this.swiftBot = swiftBot;
        this.led      = led;
        this.sc       = sc;
    }

    // Authenticate sender
    public QRResult authenticateUser() {
        System.out.println("Hold QR code in front of camera.");
        System.out.println("Format: CALLSIGN:LOCATION  (e.g. ALPHA7:A)\n");

        for (int attempt = 1; attempt <= 3; attempt++) {
            System.out.println("Scanning... (attempt " + attempt + " of 3)");
            String qr = scanQRCode();
            QRResult res = validateQR(qr);
            if (res.valid) {
                System.out.println("\nAuthentication successful!");
                System.out.println("Call Sign : " + res.callSign);
                System.out.println("Location  : " + res.location);
                led.blinkLED("GREEN", 500);
                System.out.print("\nPress ENTER to continue...");
                sc.nextLine();
                return res;
            }
            System.out.println("ERROR : Invalid QR");
            System.out.println("Cause : " + res.reason);
            System.out.println("Fix   : Try again.\n");
        }

        System.out.println("Authentication failed.");
        led.blinkLED("RED", 400);
        led.blinkLED("RED", 400);
        led.blinkLED("RED", 400);
        return new QRResult(false, null, '?', "Failed after 3 attempts.");
    }

    // Authenticate receiver at destination
    public boolean authenticateReceiver() {
        System.out.println("Receiver: hold QR code in front of camera.\n");

        for (int attempt = 1; attempt <= 3; attempt++) {
            led.blinkLED("YELLOW", 400);
            System.out.println("Scanning... (attempt " + attempt + " of 3)");
            String qr = scanQRCode();
            QRResult res = validateQR(qr);
            if (res.valid) {
                System.out.println("Authenticated: " + res.callSign + " (" + res.location + ")");
                led.blinkLED("GREEN", 500);
                return true;
            }
            System.out.println("ERROR : Auth failed");
            System.out.println("Cause : " + res.reason);
            System.out.println("Fix   : Re-scan.\n");
            if (attempt < 3) {
                System.out.println("Retrying in 3s...");
                pause(3000);
            }
        }

        System.out.println("AUTHENTICATION FAILED.");
        led.blinkLED("RED", 400);
        led.blinkLED("RED", 400);
        led.blinkLED("RED", 400);
        return false;
    }

    // ===================== QR SCANNING =====================
    // Teammate's working scan method - 4 attempts of 1 second each
    private String scanQRCode() {
        System.out.println(">> Scanning... hold QR steady!");
        int attempts = 0;
        while (attempts < 4) {
            try {
                Thread.sleep(1000);
                BufferedImage img = swiftBot.getQRImage();
                if (img != null) {
                    String rawQR = swiftBot.decodeQRImage(img);
                    if (rawQR != null && !rawQR.trim().isEmpty()) {
                        System.out.println("[CAM] QR decoded: " + rawQR.trim());
                        return rawQR.trim();
                    }
                }
            } catch (Exception e) {
                // keep trying
            }
            attempts++;
            System.out.print(".");
        }
        System.out.println("\n[CAM] Could not scan QR. Type manually:");
        System.out.print("QR Input (CALLSIGN:LOCATION): ");
        return sc.nextLine().trim();
    }

    // Validate QR format: CALLSIGN:LOCATION
    public QRResult validateQR(String qr) {
        if (qr == null || qr.isEmpty())
            return new QRResult(false, null, '?', "QR cannot be empty.");
        String[] parts = qr.split(":");
        if (parts.length != 2)
            return new QRResult(false, null, '?', "Format: CALLSIGN:LOCATION (e.g. ALPHA7:A).");
        String cs  = parts[0].trim();
        String loc = parts[1].trim();
        if (!cs.matches("[A-Za-z0-9]+"))
            return new QRResult(false, null, '?', "Call sign must be alphanumeric.");
        if (loc.length() != 1)
            return new QRResult(false, null, '?', "Location must be A, B, or C.");
        char l = Character.toUpperCase(loc.charAt(0));
        if (l != 'A' && l != 'B' && l != 'C')
            return new QRResult(false, null, '?', "Location must be A, B, or C.");
        return new QRResult(true, cs.toUpperCase(), l, null);
    }

    private void pause(int ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}