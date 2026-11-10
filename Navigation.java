package spybot;

import swiftbot.SwiftBotAPI;

public class Navigation {

    private final SwiftBotAPI swiftBot;
    private final LEDController led;

    // ===================== CALIBRATION =====================
    // Adjust these values for your robot
    private static final int MOVE_SPEED = 80;  // max 100
    private static final int TURN_SPEED = 80;  // max 100
    private static final int TRAVEL_MS  = 5000; // time to travel 50cm
    private static final int TURN_MS    = 720;  // time to turn 120 degrees
    // Too much turning  → lower TURN_MS
    // Too little turning → raise TURN_MS
    // =======================================================

    public static final int SIDE_CM = 50;

    private char currentLocation = 'A';

    public Navigation(SwiftBotAPI swiftBot, LEDController led) {
        this.swiftBot = swiftBot;
        this.led      = led;
    }

    public int navigateTo(char destination) {
        if (currentLocation == destination) {
            System.out.println("[NAV] Already at " + destination);
            return 0;
        }
        System.out.println("[NAV] From: " + currentLocation + "  To: " + destination);
        travelTo(currentLocation, destination);
        currentLocation = destination;
        System.out.println("[NAV] Arrived at: " + destination);
        return TRAVEL_MS / 1000;
    }

    private void travelTo(char from, char to) {
        // Equilateral triangle — always 120 degree turns
        //        A
        //       / \
        //      /   \
        //     C-----B
        // A->B: straight (no turn)
        // B->C: turn LEFT 120
        // C->A: turn LEFT 120
        // Reverse paths turn RIGHT 120

        boolean needTurn = true;
        boolean turnLeft = true;

        if      (from=='A' && to=='B') needTurn = false;
        else if (from=='B' && to=='C') turnLeft = true;
        else if (from=='C' && to=='A') turnLeft = true;
        else if (from=='B' && to=='A') turnLeft = false;
        else if (from=='C' && to=='B') turnLeft = false;
        else if (from=='A' && to=='C') turnLeft = false;

        if (needTurn) {
            try {
                if (turnLeft) {
                    System.out.println("[NAV] Turning LEFT 120 degrees");
                    swiftBot.move(-TURN_SPEED, TURN_SPEED, TURN_MS);
                } else {
                    System.out.println("[NAV] Turning RIGHT 120 degrees");
                    swiftBot.move(TURN_SPEED, -TURN_SPEED, TURN_MS);
                }
                Thread.sleep(TURN_MS + 500);
            } catch (Exception e) {
                System.out.println("[NAV] Turn error: " + e.getMessage());
            }
        } else {
            System.out.println("[NAV] No turn needed — going straight");
        }

        // Move forward one side of triangle
        System.out.println("[NAV] Moving FORWARD " + SIDE_CM + "cm");
        try {
            led.setColour("GREEN");
            swiftBot.move(MOVE_SPEED, MOVE_SPEED, TRAVEL_MS);
            led.off();
            Thread.sleep(TRAVEL_MS + 300);
        } catch (Exception e) {
            System.out.println("[NAV] Move error: " + e.getMessage());
        }
    }

    public char getCurrentLocation() { return currentLocation; }
}