package util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Password hashing and small security helpers.
 *
 * Passwords are never stored. We store a PBKDF2 hash instead:
 *  - a random "salt" per user means two users with the same password get different hashes
 *  - 65,536 iterations make each guess slow, so brute-forcing a stolen file is expensive
 *
 * Stored format:  PBKDF2$iterations$saltHex$hashHex
 */
public final class SecurityUtil {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65_536;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final HexFormat HEX = HexFormat.of();

    private SecurityUtil() {
    }

    /** Creates a new salted hash for the given password. */
    public static String hashPassword(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(password, salt, ITERATIONS, KEY_LENGTH_BITS);
        return "PBKDF2$" + ITERATIONS + "$" + HEX.formatHex(salt) + "$" + HEX.formatHex(hash);
    }

    /** Returns true if the password matches the stored hash. */
    public static boolean verifyPassword(String password, String storedHash) {
        if (password == null || storedHash == null) return false;

        String[] parts = storedHash.split("\\$");
        if (parts.length != 4 || !parts[0].equals("PBKDF2")) return false;

        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = HEX.parseHex(parts[2]);
            byte[] expected = HEX.parseHex(parts[3]);
            byte[] actual = pbkdf2(password, salt, iterations, expected.length * 8);
            // MessageDigest.isEqual always compares every byte, so the time taken
            // does not reveal how many characters were correct (timing attack).
            return MessageDigest.isEqual(actual, expected);
        } catch (IllegalArgumentException e) {
            return false; // badly formatted stored hash
        }
    }

    /** Rule: at least 8 characters, with at least one letter and one number. */
    public static void validatePasswordStrength(String password) throws ValidationException {
        if (password == null || password.length() < 8) {
            throw new ValidationException("Password must be at least 8 characters long.");
        }
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new ValidationException("Password must contain at least one letter and one number.");
        }
    }

    /**
     * Makes a value safe to put inside a quoted CSV cell.
     *
     * Excel treats a cell starting with = + - or @ as a formula, so a product
     * named "=HYPERLINK(...)" could run when the manager opens the export.
     * Adding a leading ' makes Excel show it as plain text.
     */
    public static String sanitizeForCSV(String value) {
        if (value == null) return "";
        String text = value.trim();
        if (!text.isEmpty() && "=+-@\t\r".indexOf(text.charAt(0)) >= 0) {
            text = "'" + text;
        }
        return text.replace("\"", "\"\""); // escape quotes inside a quoted cell
    }

    private static byte[] pbkdf2(String password, byte[] salt, int iterations, int keyLengthBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, keyLengthBits);
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 is not available on this Java runtime", e);
        }
    }
}
