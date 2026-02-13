import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// this stores all session-related state between login and logout
// it tracks whether someone is logged in, what mode they are in,
// and keeps track of per-session limits and same-session rules

public final class SessionContext {

    // session mode
    // STANDARD = normal account holder
    // ADMIN = privileged user
    public enum Mode { STANDARD, ADMIN }

    public boolean loggedIn = false;  // true once login succeeds
    public Mode mode = Mode.STANDARD;
    public String userName = null;    // account holder name (or admin name)

    // session totals (used for enforcing standard mode limits)
    public long withdrawalTotalCents = 0;
    public long transferTotalCents = 0;
    public long paymentTotalCents = 0;

    // tracks accounts created in this session (not usable until next session)
    public final Set<String> createdAccounts = new HashSet<>();

    // tracks deposits made in this session (not usable until next session)
    public final Map<String, Long> pendingDepositsCents = new HashMap<>();

    // resets everything back to a logged-out state
    // called after logout
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
