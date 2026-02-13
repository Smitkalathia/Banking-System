// File: phase2/src/SessionContext.java
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Holds session state for a single ATM session between login and logout.
 * - Tracks mode (standard/admin), current user, and per-session totals
 * - Tracks same-session availability rules for created accounts and deposited funds
 */
public final class SessionContext {
    public enum Mode { STANDARD, ADMIN }

    public boolean loggedIn = false;
    public Mode mode = Mode.STANDARD;
    public String userName = null; // standard: account holder name; admin: admin name

    // Standard mode session totals (cents)
    public long withdrawalTotalCents = 0;
    public long transferTotalCents = 0;
    public long paymentTotalCents = 0;

    // Same-session rules
    public final Set<String> createdAccounts = new HashSet<>();
    public final Map<String, Long> pendingDepositsCents = new HashMap<>();

    /** Resets the session to logged-out state after logout. */
    public void reset() {
        loggedIn = false;
        mode = Mode.STANDARD;
        userName = null;
        withdrawalTotalCents = 0;
        transferTotalCents = 0;
        paymentTotalCents = 0;
        createdAccounts.clear();
        pendingDepositsCents.clear();
    }
}
