package spybot;

import swiftbot.SwiftBotAPI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Main {

    // ===================== SWIFTBOT =====================
    static SwiftBotAPI swiftBot = SwiftBotAPI.INSTANCE;

    // ===================== SHARED COMPONENTS =====================
    static Scanner       sc      = new Scanner(System.in);
    static LEDController led     = new LEDController(swiftBot);
    static Navigation    nav     = new Navigation(swiftBot, led);
    static Authentication auth   = new Authentication(swiftBot, led, sc);
    static FileLogger    logger  = new FileLogger();

    // ===================== MORSE DICTIONARY =====================
    static final Map<Character, String> charToMorseMap = new HashMap<>();
    static final Map<String, Character> morseToCharMap = new HashMap<>();

    static MorseInput morseInput;

    // ===================== SESSION STATE =====================
    static String currentCallSign = null;
    static char   currentLocation = '?';
    static final List<DeliveryRecord> history = new ArrayList<>();
    static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ===================== MAIN =====================
    public static void main(String[] args) {
        loadMorseDictionary();
        morseInput = new MorseInput(swiftBot, led, morseToCharMap);

        banner();
        while (true) {
            int choice = mainMenu();
            if      (choice == 1) sendMode();
            else if (choice == 2) receiveMode();
            else if (choice == 3) viewHistoryMode();
            else if (choice == 4) {
                if (confirmExit()) {
                    logger.saveLog(history);
                    System.out.println("\nGoodbye.");
                    break;
                }
            }
        }
    }

    // ===================== MORSE DICTIONARY LOADER =====================
    static void loadMorseDictionary() {
        String[][] entries = {
            {"A",".-"},  {"B","-..."}, {"C","-.-."}, {"D","-.."}, {"E","."},
            {"F","..-."}, {"G","--."}, {"H","...."}, {"I",".."},  {"J",".---"},
            {"K","-.-"},  {"L",".-.."}, {"M","--"},  {"N","-."},  {"O","---"},
            {"P",".--."}, {"Q","--.-"}, {"R",".-."}, {"S","..."}, {"T","-"},
            {"U","..-"},  {"V","...-"}, {"W",".--"}, {"X","-..-"},{"Y","-.--"},
            {"Z","--.."},
            {"0","-----"},{"1",".----"},{"2","..---"},{"3","...--"},{"4","....-"},
            {"5","....."},{"6","-...."},{"7","--..."},{"8","---.."},{"9","----."}
        };
        for (String[] e : entries) {
            charToMorseMap.put(e[0].charAt(0), e[1]);
            morseToCharMap.put(e[1], e[0].charAt(0));
        }
        System.out.println("[DICT] Morse dictionary loaded: " + charToMorseMap.size() + " characters.");
    }

    // ===================== MENUS =====================
    static void banner() {
        System.out.println("");
        System.out.println("  +===========================================================+");
        System.out.println("  |                                                           |");
        System.out.println("  |                     S P Y B O T                          |");
        System.out.println("  |                                                           |");
        System.out.println("  +===========================================================+");
        System.out.println("");
        System.out.println("  [*] Booting secure channel................. [ OK ]");
        pause(600);
        System.out.println("  [*] Loading Morse dictionary............... [ OK ]");
        pause(600);
        System.out.println("  [*] Activating SwiftBot hardware........... [ OK ]");
        pause(600);
        System.out.println("  [*] Encrypting communications.............. [ OK ]");
        pause(600);
        System.out.println("  [*] All systems operational................ [ OK ]");
        pause(600);
        System.out.println("");
        System.out.println("  +===========================================================+");
        System.out.println("  |                                                           |");
        System.out.println("  |        *** WELCOME AGENT ***                              |");
        System.out.println("  |                                                           |");
        System.out.println("  |   Please authenticate to begin your mission.              |");
        System.out.println("  |   Stay sharp. Stay silent. Stay safe.                     |");
        System.out.println("  |                                                           |");
        System.out.println("  +===========================================================+");
        System.out.println("");
    }

    static int mainMenu() {
        System.out.println("Select Mode:");
        System.out.println("  1) SEND a message");
        System.out.println("  2) RECEIVE a message");
        System.out.println("  3) VIEW HISTORY");
        System.out.println("  4) EXIT\n");
        return readInt("Enter choice (1-4): ", 1, 4);
    }

    // ===================== SEND MODE =====================
    static void sendMode() {
        header("SEND MODE: Authentication");
        QRResult res = auth.authenticateUser();
        if (!res.valid) return;
        currentCallSign = res.callSign;
        currentLocation = res.location;

        // Encryption choice via buttons
        header("SEND MODE: Encryption");
        boolean encrypt = morseInput.readEncryptionChoice();
        int key = 0;
        if (encrypt) {
            System.out.println("\nNow enter the encryption key:\n");
            key = morseInput.readKey();
        }

        // Destination via buttons
        header("SEND MODE: Destination");
        char destination = morseInput.readDestination();

        // Message via Morse buttons
        header("SEND MODE: Enter Message");
        String message = morseInput.readMessage();

        // Confirm
        header("SEND MODE: Confirm");
        System.out.println("Destination : " + destination);
        System.out.println("Message     : " + (message.isEmpty() ? "(empty)" : message));
        System.out.println("Encryption  : " + (encrypt ? "ENABLED (key=" + key + ")" : "DISABLED") + "\n");

        int confirm = readInt("  1) CONFIRM\n  2) RE-ENTER\n  3) CANCEL\n\nEnter (1-3): ", 1, 3);
        if (confirm == 2) { sendMode(); return; }
        if (confirm == 3) return;

        deliverMessage(destination, message, encrypt, key);
    }

    // ===================== RECEIVE MODE =====================
    static void receiveMode() {
        header("RECEIVE MODE");
        QRResult res = auth.authenticateUser();
        if (!res.valid) return;
        currentCallSign = res.callSign;
        currentLocation = res.location;

        System.out.println("Enter decryption key using Morse buttons:\n");
        int key = morseInput.readKey();

        header("INCOMING MESSAGE");
        System.out.println("Key entered : " + key);
        System.out.println("Time        : " + nowTs());
        System.out.print("\nPress ENTER...");
        sc.nextLine();
    }

    // ===================== DELIVERY =====================
    static void deliverMessage(char destination, String messageBody,
                                boolean encrypt, int key) {
        header("DELIVERY IN PROGRESS");
        char senderLoc = currentLocation;
        System.out.println("");
        System.out.println("  >> Mission initiated!");
        System.out.println("  >> Agent    : " + currentCallSign);
        System.out.println("  >> From     : Safe House " + senderLoc);
        System.out.println("  >> To       : Safe House " + destination);
        System.out.println("  >> Encryption: " + (encrypt ? "ENABLED (key=" + key + ")" : "DISABLED"));
        System.out.println("");
        System.out.println("  >> SwiftBot navigating to Safe House " + destination + "...");

        int navTime  = nav.navigateTo(destination);
        int distance = Navigation.SIDE_CM;

        System.out.println("");
        System.out.println("  >> SwiftBot arrived at Safe House " + destination + "!");
        System.out.println("  >> Waiting for receiver authentication...");

        led.arrivalBlink();
        boolean authOk =  auth.authenticateReceiver();
        String  status = authOk ? "SUCCESS" : "AUTH_FAILED";

        if (authOk) {
            System.out.println("");
            System.out.println("  >> Receiver authenticated successfully!");
            System.out.println("  >> Transmitting message via LED Morse code...");
            header("LED MORSE TRANSMISSION");
            String transmit = encrypt ?
                    Encryption.caesarEncrypt(senderLoc + " " + messageBody, key) :
                    senderLoc + " " + messageBody;
            led.blinkMessageInMorse(transmit, charToMorseMap);
            System.out.println("");
            System.out.println("  >> Message transmitted successfully! ✓");
            led.blinkLED("YELLOW", 600);
            System.out.println("  >> Waiting 10 seconds before returning...");
            pause(10000);
            System.out.println("  >> SwiftBot returning to Safe House " + senderLoc + "...");
            nav.navigateTo(senderLoc);
            System.out.println("");
            System.out.println("  >> SwiftBot has returned to Safe House " + senderLoc + "! ✓");
            System.out.println("  >> Mission complete!");
        } else {
            System.out.println("");
            System.out.println("  >> Authentication FAILED! Aborting mission.");
            System.out.println("  >> SwiftBot returning to Safe House " + senderLoc + "...");
            nav.navigateTo(senderLoc);
            System.out.println("");
            System.out.println("  >> SwiftBot has returned to Safe House " + senderLoc + "! ✓");
        }

        String decodedText = encrypt ?
                Encryption.caesarDecrypt(messageBody, key) : messageBody;

        history.add(new DeliveryRecord(nowTs(), currentCallSign, senderLoc,
                destination, messageBody, encrypt, key, status,
                distance, navTime, authOk ? 0 : 3));
        logger.saveLog(history);

        header("DELIVERY RESULT");
        System.out.println("Status            : " + status);
        System.out.println("Sender            : " + currentCallSign + " (" + senderLoc + ")");
        System.out.println("Destination       : " + destination);
        System.out.println("Encrypted Message : " + (messageBody.isEmpty() ? "(empty)" : messageBody));
        System.out.println("Decoded Message   : " + (decodedText.isEmpty() ? "(empty)" : decodedText));
        System.out.println("Encryption        : " + (encrypt ? "ENABLED" : "DISABLED"));
        if (encrypt) System.out.println("Key               : " + key);
        System.out.println("Distance          : " + distance + " cm");
        System.out.println("Nav Time          : " + navTime + " s");
        System.out.println("Log saved to      : " + logger.getLogFilePath());

        int next = readInt("\n  1) Send another\n  2) View history\n  3) Exit\n\nEnter (1-3): ", 1, 3);
        if      (next == 1) sendMode();
        else if (next == 2) viewHistoryMode();
        else { if (confirmExit()) { logger.saveLog(history); System.out.println("\nGoodbye."); System.exit(0); } }
    }

    // ===================== VIEW HISTORY =====================
    static void viewHistoryMode() {
        header("DELIVERY HISTORY");
        if (history.isEmpty()) {
            System.out.println("No deliveries yet.\n");
            System.out.print("Press ENTER..."); sc.nextLine();
            return;
        }

        System.out.println("ID | Time                | From   | To | Status      | Enc | Key | Encrypted       | Decoded");
        System.out.println("---+---------------------+--------+----+-------------+-----+-----+-----------------+--------");
        for (int i = 0; i < history.size(); i++) {
            DeliveryRecord r = history.get(i);
            String dec = r.encryptionEnabled ?
                    Encryption.caesarDecrypt(r.message, r.key) : r.message;
            String ks = r.encryptionEnabled ? String.valueOf(r.key) : "N/A";
            String em = r.message.isEmpty() ? "(empty)" : r.message;
            String dm = dec.isEmpty() ? "(empty)" : dec;
            System.out.printf("%02d | %-19s | %-6s | %c  | %-11s | %-3s | %-3s | %-15s | %s%n",
                    (i + 1), r.timestamp, r.sender, r.destination, r.status,
                    (r.encryptionEnabled ? "ON" : "OFF"), ks,
                    em.length() > 15 ? em.substring(0, 12) + "..." : em,
                    dm.length() > 15 ? dm.substring(0, 12) + "..." : dm);
        }

        System.out.println("\n  1) View full record\n  2) Statistics\n  3) Back\n");
        int c = readInt("Enter (1-3): ", 1, 3);
        if (c == 3) return;
        if (c == 2) { showStats(); System.out.print("\nPress ENTER..."); sc.nextLine(); viewHistoryMode(); return; }
        int id = readInt("Record ID (1-" + history.size() + "): ", 1, history.size());
        showDetail(id - 1);
        System.out.print("\nPress ENTER..."); sc.nextLine();
        viewHistoryMode();
    }

    static void showDetail(int idx) {
        DeliveryRecord r = history.get(idx);
        String dec = r.encryptionEnabled ?
                Encryption.caesarDecrypt(r.message, r.key) : r.message;
        header("RECORD #" + String.format("%02d", idx + 1));
        System.out.println("Sender        : " + r.sender + " (" + r.senderLocation + ")");
        System.out.println("Destination   : " + r.destination);
        System.out.println("Encrypted Msg : " + (r.message.isEmpty() ? "(empty)" : r.message));
        System.out.println("Decoded Msg   : " + (dec.isEmpty() ? "(empty)" : dec));
        System.out.println("Encryption    : " + (r.encryptionEnabled ? "ENABLED" : "DISABLED"));
        if (r.encryptionEnabled) System.out.println("Key           : " + r.key);
        System.out.println("Status        : " + r.status);
        System.out.println("Distance      : " + r.distanceCm + " cm");
        System.out.println("Nav Time      : " + r.navTimeSec + " s");
        System.out.println("Timestamp     : " + r.timestamp);
    }

    static void showStats() {
        header("STATISTICS");
        int success = 0, totalTime = 0;
        Map<Character, Integer> freq = new HashMap<>();
        for (DeliveryRecord r : history) {
            if ("SUCCESS".equals(r.status)) success++;
            totalTime += r.navTimeSec;
            freq.put(r.destination, freq.getOrDefault(r.destination, 0) + 1);
        }
        char mf = '?'; int best = 0;
        for (Map.Entry<Character, Integer> e : freq.entrySet())
            if (e.getValue() > best) { best = e.getValue(); mf = e.getKey(); }
        System.out.println("Total    : " + history.size());
        System.out.println("Success  : " + success);
        System.out.println("Failed   : " + (history.size() - success));
        System.out.println("Avg time : " + (history.isEmpty() ? 0 : totalTime / history.size()) + " s");
        System.out.println("Top dest : " + (mf == '?' ? "(none)" : mf));
    }

    // ===================== UTILITIES =====================
    static void header(String t) {
        System.out.println("\n---------------------------------------");
        System.out.println(t);
        System.out.println("---------------------------------------");
    }

    static int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            try {
                int v = Integer.parseInt(sc.nextLine().trim());
                if (v >= min && v <= max) return v;
                throw new NumberFormatException();
            } catch (NumberFormatException e) {
                System.out.println("Invalid — must be " + min + "-" + max + ". Try again.");
            }
        }
    }

    static boolean confirmExit() {
        System.out.print("Are you sure? (Y/N): ");
        String a = sc.nextLine().trim().toUpperCase();
        return a.equals("Y") || a.equals("YES");
    }

    static String nowTs() {
        return LocalDateTime.now().format(TS);
    }

    static void pause(int ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}