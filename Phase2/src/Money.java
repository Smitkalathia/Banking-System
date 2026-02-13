import java.math.BigDecimal;
import java.math.RoundingMode;

// small helper class for parsing money values
// converts strings like "100", "100.00", "00100.00" into cents (long)
// returns null if the input is invalid

public final class Money {

    // private constructor so this class cannot be instantiated
    private Money() {}

    // parses a currency string into cents
    // returns null if the string is invalid or cannot be parsed
    public static Long parseToCents(String s) {
        if (s == null) return null;

        String t = s.trim();
        if (t.isEmpty()) return null;

        try {
            // ensure 2 decimal places, then shift to cents
            BigDecimal bd = new BigDecimal(t)
                    .setScale(2, RoundingMode.HALF_UP);

            return bd.movePointRight(2).longValueExact();

        } catch (Exception e) {
            return null; // invalid number format
        }
    }
}
