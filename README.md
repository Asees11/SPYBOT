# SpyBot – Secure Message Delivery Robot

A Java program for the **SwiftBot** (a Raspberry Pi robot) that works like a spy courier. An agent proves who they are with a QR code, types a secret message in **Morse code using the robot's buttons**, and can **encrypt it with a Caesar cipher**. SwiftBot then drives to one of three "safe houses" on a triangular track, checks the receiver's QR code, and sends the message back out by **flashing its LEDs in Morse code**.

Built for CS1814 Software Implementation at Brunel University London (2025/26).

## How a delivery works

1. **Authenticate:** the sender scans a QR code in the format `CALLSIGN:LOCATION` (for example `ALPHA7:A`)
2. **Choose encryption:** turn the Caesar cipher on or off and enter a key
3. **Pick a destination:** press a button (A = safe house A, B = B, X = C)
4. **Type the message:** in Morse on the buttons, with live decoding shown on screen
5. **Confirm:** Confirm, Re-enter or Cancel
6. **Drive:** SwiftBot goes to the destination corner of the triangle
7. **Verify the receiver:** the receiver scans their QR code (3 attempts allowed)
8. **Send:** the message flashes out as LED Morse code, then SwiftBot drives back to the sender
9. **Log:** the delivery is saved to history and to a session log file

## Features

- **QR code authentication.** Checks the format: the call sign must be letters and numbers only, and the location must be A, B or C. Bad codes are rejected with a clear error and a prompt to try again, and the user is locked out after 3 failed tries.
- **Manual QR fallback.** If the camera can't read the code, the user can type it in, and it's checked against the same rules.
- **Morse code input.** X = dot, Y = dash, A = end of character, B = end of word, B on an empty word = end of message. Decoded live through a 36-character dictionary (A–Z, 0–9).
- **Caesar cipher encryption.** Optional, with a key the user chooses. Upper and lower case are kept, and anything that isn't a letter is left unchanged. The key stays in memory and isn't saved anywhere in plain text.
- **Triangle navigation.** Drives between three safe houses on an equilateral triangle, working out whether to turn left or right by 120° from where it currently is.
- **LED Morse transmission.** White = dot, blue = dash, amber = end of character, red = end of word, green = end of message.
- **Delivery history and statistics.** A table of every delivery, a full view of each record, success and failure counts, average travel time and the most common destination.
- **Session logging.** Each session is saved to a log file with a timestamp under `logs/`.

## Requirements and testing

- **24 functional and 7 non-functional requirements** were written up and implemented. They cover authentication, Morse input, navigation, delivery, logging, usability, reliability, security and performance.
- **22 black-box test cases**, all passing. They include valid and invalid QR codes, Morse decoding, a full message, encryption, history and the manual fallback.

## Problems I hit and how I solved them

| Problem | What I did |
|---|---|
| The SwiftBot camera often failed to read QR codes because of lighting, angle and distance | Added a manual typed fallback, checked with the same rules. It turned out to be needed in most test sessions. |
| The robot kept stopping short of the 50 cm target because of motor slip and surface friction | Calibrated travel and turn times by testing on the real track |
| Putting the destination letter at the start of the Morse message caused input mistakes | Made destination a separate button-press step before the message |
| Morse input was easy to get wrong with no feedback | Added live on-screen display of the current character and word |
| Messages could be sent by accident | Added a Confirm / Re-enter / Cancel step |

## Project structure

| Class | What it does |
|---|---|
| `Main` | Menus, send/receive/history modes, how a delivery runs |
| `Authentication` | QR scanning, manual fallback, checking |
| `MorseInput` | Event-driven button handlers that turn Morse into text |
| `Encryption` | Caesar cipher encrypt/decrypt |
| `Navigation` | Moving and turning around the triangle, with calibration |
| `LEDController` | LED colours and Morse flashing |
| `FileLogger` | Writes session logs |
| `DeliveryRecord`, `QRResult` | Data classes |

## Tech

Java · Maven · Raspberry Pi · SwiftBot API 6.0.0 (camera, motors, buttons, underlights) · OOP · event-driven programming

## How to run

On a SwiftBot connected to the Brunel SwiftBot Maven repository:

```bash
mvn package
java -cp target/classes:<path-to-SwiftBot-API.jar> spybot.Main
```

## What I'd improve next

- Fix history logging, which currently stores the plaintext in the encrypted field
- Make receive mode actually decrypt and show an incoming message
- Replace movement based on timing with sensor feedback for more accurate navigation
- Use a cipher that's harder to break than Caesar, such as Vigenère or AES
- Add JUnit tests for `Encryption` and QR validation
