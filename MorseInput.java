package spybot;

import swiftbot.SwiftBotAPI;
import swiftbot.Button;
import java.util.*;

public class MorseInput {

    private final SwiftBotAPI swiftBot;
    private final LEDController led;
    private final Map<String, Character> morseToCharMap;

    public MorseInput(SwiftBotAPI swiftBot, LEDController led,
                      Map<String, Character> morseToCharMap) {
        this.swiftBot      = swiftBot;
        this.led           = led;
        this.morseToCharMap = morseToCharMap;
    }

    // ===================== READ MESSAGE =====================
    // X=dot, Y=dash, A=end char, B=end word, B on empty=end message
    private volatile boolean msgDone = false;
    private final StringBuilder msgChar  = new StringBuilder();
    private final StringBuilder msgWord  = new StringBuilder();
    private final List<String>   msgWords = new ArrayList<>();

    public String readMessage() {
        msgDone = false;
        msgChar.setLength(0);
        msgWord.setLength(0);
        msgWords.clear();

        System.out.println("Press SwiftBot buttons:");
        System.out.println("  X = dot    Y = dash");
        System.out.println("  A = end char   B = end word");
        System.out.println("  B on empty word = END MESSAGE\n");
        showMsgLive();

        swiftBot.enableButton(Button.X, () -> {
            msgChar.append(".");
            System.out.println("[BTN] X = dot");
            showMsgLive();
        });
        swiftBot.enableButton(Button.Y, () -> {
            msgChar.append("-");
            System.out.println("[BTN] Y = dash");
            showMsgLive();
        });
        swiftBot.enableButton(Button.A, () -> {
            if (msgChar.length() == 0) { System.out.println("[BTN] A ignored"); return; }
            String decoded = decode(msgChar.toString());
            msgWord.append(decoded);
            System.out.println("[BTN] A = char: " + msgChar + " = '" + decoded + "'");
            msgChar.setLength(0);
            showMsgLive();
        });
        swiftBot.enableButton(Button.B, () -> {
            if (msgChar.length() > 0) {
                msgWord.append(decode(msgChar.toString()));
                msgChar.setLength(0);
            }
            if (msgWord.length() == 0) {
                System.out.println("[BTN] B = END OF MESSAGE");
                msgDone = true;
            } else {
                msgWords.add(msgWord.toString());
                System.out.println("[BTN] B = word: '" + msgWord + "'");
                msgWord.setLength(0);
                showMsgLive();
            }
        });

        while (!msgDone) { pause(100); }

        swiftBot.disableButton(Button.X);
        swiftBot.disableButton(Button.Y);
        swiftBot.disableButton(Button.A);
        swiftBot.disableButton(Button.B);

        System.out.println("\nMessage complete.\n");
        return String.join(" ", msgWords);
    }

    // ===================== READ KEY (numeric via Morse) =====================
    // X=dot, Y=dash, A=confirm digit, B=finish key
    private volatile boolean keyDone = false;
    private final StringBuilder keyChar   = new StringBuilder();
    private final StringBuilder keyDigits = new StringBuilder();

    public int readKey() {
        keyDone = false;
        keyChar.setLength(0);
        keyDigits.setLength(0);

        System.out.println("Enter key using Morse:");
        System.out.println("  X=dot  Y=dash  A=confirm digit  B=finish\n");
        System.out.println("Example: 5 = ..... then A");
        System.out.println("         1 = .---- then A\n");
        showKeyLive();

        swiftBot.enableButton(Button.X, () -> {
            keyChar.append(".");
            System.out.println("[BTN] X = dot | " + keyChar);
        });
        swiftBot.enableButton(Button.Y, () -> {
            keyChar.append("-");
            System.out.println("[BTN] Y = dash | " + keyChar);
        });
        swiftBot.enableButton(Button.A, () -> {
            if (keyChar.length() == 0) { System.out.println("[BTN] A ignored"); return; }
            Character digit = morseToCharMap.get(keyChar.toString());
            if (digit != null && Character.isDigit(digit)) {
                keyDigits.append(digit);
                System.out.println("[BTN] Digit: " + digit + " | Key so far: " + keyDigits);
                keyChar.setLength(0);
                led.blinkLED("CYAN", 200);
                showKeyLive();
            } else {
                System.out.println("[BTN] Unknown pattern: " + keyChar + " — try again");
                keyChar.setLength(0);
                led.blinkLED("RED", 200);
            }
        });
        swiftBot.enableButton(Button.B, () -> {
            if (keyDigits.length() == 0) { System.out.println("[BTN] B ignored — no digits yet"); return; }
            System.out.println("[BTN] B = key complete");
            keyDone = true;
        });

        // Blink PURPLE while waiting
        while (!keyDone) {
            try {
                swiftBot.fillUnderlights(new int[]{148, 0, 211}); // PURPLE
                Thread.sleep(400);
                swiftBot.disableUnderlights();
                Thread.sleep(400);
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        swiftBot.disableUnderlights();
        swiftBot.disableButton(Button.X);
        swiftBot.disableButton(Button.Y);
        swiftBot.disableButton(Button.A);
        swiftBot.disableButton(Button.B);

        int key = Math.max(1, Math.min(999, Integer.parseInt(keyDigits.toString())));
        System.out.println("Key set to: " + key);
        led.blinkLED("GREEN", 500);
        return key;
    }

    // ===================== READ DESTINATION (single button press) =====================
    // X=A, Y=B, A button=C
    private volatile boolean destChosen = false;
    private volatile char destResult = 'A';

    public char readDestination() {
        destChosen = false;

        System.out.println("Press button to choose destination:");
        System.out.println("  X = Safe House A");
        System.out.println("  Y = Safe House B");
        System.out.println("  A = Safe House C");
        System.out.println("\nPINK light = waiting...\n");

        swiftBot.enableButton(Button.X, () -> { destResult = 'A'; destChosen = true; });
        swiftBot.enableButton(Button.Y, () -> { destResult = 'B'; destChosen = true; });
        swiftBot.enableButton(Button.A, () -> { destResult = 'C'; destChosen = true; });

        // Blink PINK while waiting
        while (!destChosen) {
            try {
                swiftBot.fillUnderlights(new int[]{255, 20, 147}); // PINK
                Thread.sleep(300);
                swiftBot.disableUnderlights();
                Thread.sleep(300);
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        swiftBot.disableUnderlights();
        swiftBot.disableButton(Button.X);
        swiftBot.disableButton(Button.Y);
        swiftBot.disableButton(Button.A);

        System.out.println("Destination selected: Safe House " + destResult);
        led.blinkLED("CYAN", 400);
        return destResult;
    }

    // ===================== READ ENCRYPTION CHOICE =====================
    // X=encrypt, Y=no encryption
    private volatile boolean encChosen = false;
    private volatile boolean encResult = false;

    public boolean readEncryptionChoice() {
        encChosen = false;

        System.out.println("Press button:");
        System.out.println("  X = ENCRYPT message");
        System.out.println("  Y = NO ENCRYPTION");
        System.out.println("\nPINK light = waiting...\n");

        swiftBot.enableButton(Button.X, () -> { encResult = true;  encChosen = true; });
        swiftBot.enableButton(Button.Y, () -> { encResult = false; encChosen = true; });

        // Blink PINK while waiting
        while (!encChosen) {
            try {
                swiftBot.fillUnderlights(new int[]{255, 20, 147}); // PINK
                Thread.sleep(300);
                swiftBot.disableUnderlights();
                Thread.sleep(300);
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        swiftBot.disableUnderlights();
        swiftBot.disableButton(Button.X);
        swiftBot.disableButton(Button.Y);

        if (encResult) {
            System.out.println("ENCRYPTION ENABLED");
            led.blinkLED("GREEN", 400);
        } else {
            System.out.println("NO ENCRYPTION");
            led.blinkLED("YELLOW", 400);
        }
        return encResult;
    }

    // ===================== HELPERS =====================
    private String decode(String morse) {
        Character ch = morseToCharMap.get(morse);
        if (ch == null) { System.out.println("[DICT] Unknown: " + morse); return "?"; }
        return String.valueOf(ch);
    }

    private void showMsgLive() {
        System.out.println("  Char    : " + (msgChar.length()==0?"(empty)":msgChar));
        System.out.println("  Word    : " + (msgWord.length()==0?"(empty)":msgWord));
        System.out.println("  Message : " + (msgWords.isEmpty()?"(empty)":String.join(" ",msgWords)));
        System.out.println();
    }

    private void showKeyLive() {
        System.out.println("  Pattern : " + (keyChar.length()==0?"(empty)":keyChar));
        System.out.println("  Key     : " + (keyDigits.length()==0?"(none)":keyDigits));
        System.out.println();
    }

    private void pause(int ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}