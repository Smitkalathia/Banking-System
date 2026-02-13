// File: phase2/src/Money.java
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Minimal money parsing helper.
 * Converts a string like "100", "100.00", "00100.00" into cents.
 */
public final class Money {
    private Money() {}

    /** Parses a decimal currency string to cents. Returns null if invalid. */
    public static Long parseToCents(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;

        try {
            BigDecimal bd = new BigDecimal(t).setScale(2, RoundingMode.HALF_UP);
            return bd.movePointRight(2).longValueExact();
        } catch (Exception e) {
            return null;
        }
    }
}
