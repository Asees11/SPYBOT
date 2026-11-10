package spybot;

public class Encryption {

    public static String caesarEncrypt(String text, int key) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (Character.isLetter(c)) {
                char base = Character.isUpperCase(c) ? 'A' : 'a';
                sb.append((char)((c - base + key) % 26 + base));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String caesarDecrypt(String text, int key) {
        return caesarEncrypt(text, 26 - (key % 26));
    }
}
