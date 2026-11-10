package spybot;

import swiftbot.SwiftBotAPI;

public class LEDController {

    private final SwiftBotAPI swiftBot;

    public LEDController(SwiftBotAPI swiftBot) {
        this.swiftBot = swiftBot;
    }

    // Blink a colour for a given duration
    public void blinkLED(String colour, int durationMs) {
        int[] rgb = colourToRGB(colour);
        System.out.println("[LED] " + colour + " " + durationMs + "ms");
        try {
            swiftBot.fillUnderlights(rgb);
            Thread.sleep(durationMs);
            swiftBot.disableUnderlights();
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Blink RED x3 on arrival
    public void arrivalBlink() {
        System.out.println("[LED] Arrival: RED x3");
        for (int i = 0; i < 3; i++) {
            blinkLED("RED", 400);
            pause(150);
        }
    }

    // Transmit full message in Morse via LEDs
    // Dot=WHITE, Dash=BLUE, EndChar=AMBER, EndWord=RED, EndMessage=GREEN
    public void blinkMessageInMorse(String plainText,
                                    java.util.Map<Character, String> charToMorseMap) {
        System.out.println("[LED] Transmitting: \"" + plainText + "\"");
        for (String word : plainText.trim().split("\\s+")) {
            for (char ch : word.toCharArray()) {
                String morse = charToMorseMap.getOrDefault(
                        Character.toUpperCase(ch), "");
                System.out.println("[LED] '" + ch + "' = " + morse);
                for (char sym : morse.toCharArray()) {
                    if (sym == '.') blinkLED("WHITE", 300);
                    else if (sym == '-') blinkLED("BLUE", 700);
                    pause(150);
                }
                blinkLED("AMBER", 400);
                pause(150);
            }
            blinkLED("RED", 500);
            pause(300);
        }
        blinkLED("GREEN", 600);
        System.out.println("[LED] Transmission complete.\n");
    }

    // Set underlights to a solid colour (no blink)
    public void setColour(String colour) {
        int[] rgb = colourToRGB(colour);
        try {
            swiftBot.fillUnderlights(rgb);
        } catch (Exception e) {
            System.out.println("[LED] Error: " + e.getMessage());
        }
    }

    public void off() {
        try { swiftBot.disableUnderlights(); }
        catch (Exception e) { System.out.println("[LED] Off error: " + e.getMessage()); }
    }

    public int[] colourToRGB(String c) {
        switch (c.toUpperCase()) {
            case "WHITE":  return new int[]{255, 255, 255};
            case "BLUE":   return new int[]{0,   0,   255};
            case "AMBER":  return new int[]{255, 191, 0  };
            case "RED":    return new int[]{255, 0,   0  };
            case "GREEN":  return new int[]{0,   255, 0  };
            case "YELLOW": return new int[]{255, 255, 0  };
            case "PINK":   return new int[]{255, 20,  147};
            case "PURPLE": return new int[]{148, 0,   211};
            case "CYAN":   return new int[]{0,   255, 255};
            default:       return new int[]{0,   0,   0  };
        }
    }

    private void pause(int ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}