// File: phase2/src/Messages.java
/**
 * Central catalog of all user-visible console messages.
 *
 * Policy:
 * - The Front End must only print lines that appear in this file (or "User <name> logged in").
 * - This stabilizes output for automated requirements tests.
 */
public final class Messages {
    private Messages() {}

    // Session / login
    public static final String LOGIN_STANDARD_OK = "Login accepted: standard session";
    public static final String LOGIN_ADMIN_OK = "Login accepted: admin session";
    public static final String LOGOUT_OK = "Logout accepted";
    public static final String SESSION_ENDED = "Session ended";

    public static final String ERR_NO_SESSION = "ERROR: No active session";
    public static final String ERR_SESSION_ACTIVE = "ERROR: Session already active";
    public static final String ERR_INVALID_SESSION_TYPE = "ERROR: Invalid session type";
    public static final String ERR_INVALID_ACCOUNT_HOLDER = "ERROR: Invalid account holder name";
    public static final String ERR_PRIVILEGED_NOT_PERMITTED = "ERROR: Privileged transaction not permitted";

    // Acceptance lines
    public static final String WITHDRAWAL_OK = "Withdrawal accepted";
    public static final String TRANSFER_OK = "Transfer accepted";
    public static final String DEPOSIT_OK = "Deposit accepted";
    public static final String PAYBILL_OK = "Bill payment accepted";

    // Admin success lines
    public static final String CREATE_OK = "Account created successfully";
    public static final String DELETE_OK = "Account deleted successfully";
    public static final String DISABLE_OK = "Account disabled successfully";
    public static final String CHANGEPLAN_OK = "Account plan changed successfully";

    // Account / validation errors
    public static final String ERR_ACCOUNT_DOES_NOT_EXIST = "ERROR: Account does not exist";
    public static final String ERR_ACCOUNT_DISABLED = "ERROR: Account is disabled";
    public static final String ERR_ACCOUNT_NOT_FOUND = "ERROR: Account not found";
    public static final String ERR_ACCOUNT_NOT_OWNED = "ERROR: Account not owned by user";
    public static final String ERR_SRC_NOT_OWNED = "ERROR: Source account not owned by user";
    public static final String ERR_DEST_NOT_FOUND = "ERROR: Destination account not found";
    public static final String ERR_UNAVAILABLE_SAME_SESSION = "ERROR: Account unavailable in same session";
    public static final String ERR_FUNDS_UNAVAILABLE_SAME_SESSION = "ERROR: Funds unavailable in same session";

    public static final String ERR_INSUFFICIENT = "ERROR: Insufficient funds";

    public static final String ERR_WITHDRAW_AMOUNT = "ERROR: Invalid withdrawal amount";
    public static final String ERR_TRANSFER_AMOUNT = "ERROR: Invalid transfer amount";
    public static final String ERR_DEPOSIT_AMOUNT = "ERROR: Invalid deposit amount";
    public static final String ERR_PAYMENT_AMOUNT = "ERROR: Invalid payment amount";
    public static final String ERR_INVALID_PAYEE = "ERROR: Invalid payee";

    public static final String ERR_WITHDRAW_LIMIT = "ERROR: Session withdrawal limit exceeded";
    public static final String ERR_TRANSFER_LIMIT = "ERROR: Session transfer limit exceeded";
    public static final String ERR_PAYMENT_LIMIT = "ERROR: Session payment limit exceeded";

    // Create validation
    public static final String ERR_NAME_TOO_LONG = "ERROR: Account holder name too long";
    public static final String ERR_INITIAL_BAL_LIMIT = "ERROR: Initial balance exceeds limit";
}
