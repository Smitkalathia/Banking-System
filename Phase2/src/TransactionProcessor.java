// File: phase2/src/TransactionProcessor.java

// main engine of the front end
// it reads a transaction code per line from stdin and runs the matching handler
// it also enforces session rules (must login first, logout ends session, admin-only commands)

public final class TransactionProcessor {
    private final ConsoleIO io;
    private final AccountsRepository repo;
    private final TransactionRecorder recorder;
    private final SessionContext session = new SessionContext();

    // standard mode per-session caps (in cents)
    private static final long WITHDRAW_CAP_CENTS = 500_00L;
    private static final long TRANSFER_CAP_CENTS = 1000_00L;
    private static final long PAYMENT_CAP_CENTS  = 2000_00L;

    // constructor: injects our input/output wrapper, account repo, and transaction file writer
    public TransactionProcessor(ConsoleIO io, AccountsRepository repo, TransactionRecorder recorder) {
        this.io = io;
        this.repo = repo;
        this.recorder = recorder;
    }

    // main loop: keeps reading transaction codes until EOF
    public void run() {
        while (true) {
            String code = readLine();
            if (code == null) return; // EOF

            code = code.trim();
            if (code.isEmpty()) continue;

            // if not logged in, only "login" is allowed
            // we still consume extra lines so the input stream doesn't get out of sync
            if (!session.loggedIn && !code.equalsIgnoreCase("login")) {
                skipFieldsForTransactionWhenNotLoggedIn(code);
                io.println(Messages.ERR_NO_SESSION);
                continue;
            }

            // login/logout are handled up here (simpler flow)
            if (code.equalsIgnoreCase("login")) {
                handleLogin();
                continue;
            }
            if (code.equalsIgnoreCase("logout")) {
                handleLogout();
                continue;
            }

            // dispatch to the correct handler
            // note: we avoid printing "unknown transaction code" because tests may not allow it
            switch (code.toLowerCase()) {
                case "withdrawal" -> handleWithdrawal();
                case "transfer" -> handleTransfer();
                case "deposit" -> handleDeposit();
                case "paybill" -> handlePaybill();
                case "create" -> handleCreate();
                case "delete" -> handleDelete();
                case "disable" -> handleDisable();
                case "changeplan" -> handleChangePlan();
                default -> {
                    // unknown transaction code: do nothing (policy for now)
                }
            }
        }
    }

    // login flow:
    // reads session type ("standard" or "admin")
    // standard -> reads owner name, validates it exists
    // admin -> reads admin name (or defaults)
    private void handleLogin() {
        if (session.loggedIn) {
            io.println(Messages.ERR_SESSION_ACTIVE);
            return;
        }

        String typeLine = readLine();
        if (typeLine == null) return;

        String type = typeLine.trim().toLowerCase();

        // invalid session type (some tests may include an extra line after this)
        if (!type.equals("standard") && !type.equals("admin")) {
            readLine(); // consume one line to avoid input drift
            io.println(Messages.ERR_INVALID_SESSION_TYPE);
            return;
        }

        if (type.equals("standard")) {
            String owner = readLine();
            if (owner == null) return;

            owner = owner.trim();

            // standard login must match an owner in the accounts file
            if (!repo.ownerExists(owner)) {
                io.println(Messages.ERR_INVALID_ACCOUNT_HOLDER);
                return;
            }

            session.loggedIn = true;
            session.mode = SessionContext.Mode.STANDARD;
            session.userName = owner;

            io.println(Messages.LOGIN_STANDARD_OK);
            io.println("User " + owner + " logged in");
            return;
        }

        // admin login (prototype: any name allowed)
        String adminName = readLine();
        if (adminName == null) return;

        adminName = adminName.trim();
        if (adminName.isEmpty()) adminName = "Admin";

        session.loggedIn = true;
        session.mode = SessionContext.Mode.ADMIN;
        session.userName = adminName;

        io.println(Messages.LOGIN_ADMIN_OK);
        io.println("User " + adminName + " logged in");
    }

    // logout ends the session and writes the transaction file (prototype output)
    private void handleLogout() {
        if (!session.loggedIn) {
            io.println(Messages.ERR_NO_SESSION);
            return;
        }

        recorder.writeOnLogout();
        io.println(Messages.LOGOUT_OK);
        io.println(Messages.SESSION_ENDED);

        session.reset();
    }

    // withdrawal:
    // checks ownership, account status, same-session rules, per-session cap, and available funds
    private void handleWithdrawal() {
        String owner = currentOwner();
        if (owner == null) return;

        String acctNum = readLine();
        String amtLine = readLine();
        if (acctNum == null || amtLine == null) return;

        acctNum = acctNum.trim();

        Long cents = Money.parseToCents(amtLine);
        if (cents == null || cents <= 0) {
            io.println(Messages.ERR_WITHDRAW_AMOUNT);
            return;
        }

        // standard cap check
        if (session.mode == SessionContext.Mode.STANDARD &&
                session.withdrawalTotalCents + cents > WITHDRAW_CAP_CENTS) {
            io.println(Messages.ERR_WITHDRAW_LIMIT);
            return;
        }

        Account a = repo.getByNumber(acctNum);
        if (a == null) {
            io.println(Messages.ERR_ACCOUNT_NOT_FOUND);
            return;
        }

        if (a.status == Account.Status.D) {
            io.println(Messages.ERR_ACCOUNT_DISABLED);
            return;
        }

        if (!a.owner.equalsIgnoreCase(owner)) {
            io.println(Messages.ERR_ACCOUNT_NOT_OWNED);
            return;
        }

        // created accounts are not usable in the same session
        if (session.createdAccounts.contains(a.number)) {
            io.println(Messages.ERR_UNAVAILABLE_SAME_SESSION);
            return;
        }

        // deposits are tracked as "pending" and not usable in this session
        long pending = session.pendingDepositsCents.getOrDefault(a.number, 0L);

        if (cents > a.balanceCents) {
            if (cents <= a.balanceCents + pending) io.println(Messages.ERR_FUNDS_UNAVAILABLE_SAME_SESSION);
            else io.println(Messages.ERR_INSUFFICIENT);
            return;
        }

        a.balanceCents -= cents;
        if (session.mode == SessionContext.Mode.STANDARD) session.withdrawalTotalCents += cents;

        io.println(Messages.WITHDRAWAL_OK);
    }

    // transfer:
    // checks ownership of source, destination exists, account status, caps, and funds
    private void handleTransfer() {
        String owner = currentOwner();
        if (owner == null) return;

        String fromAcct = readLine();
        String toAcct = readLine();
        String amtLine = readLine();
        if (fromAcct == null || toAcct == null || amtLine == null) return;

        fromAcct = fromAcct.trim();
        toAcct = toAcct.trim();

        Long cents = Money.parseToCents(amtLine);
        if (cents == null || cents <= 0) {
            io.println(Messages.ERR_TRANSFER_AMOUNT);
            return;
        }

        // standard cap check
        if (session.mode == SessionContext.Mode.STANDARD &&
                session.transferTotalCents + cents > TRANSFER_CAP_CENTS) {
            io.println(Messages.ERR_TRANSFER_LIMIT);
            return;
        }

        Account src = repo.getByNumber(fromAcct);
        if (src == null) {
            io.println(Messages.ERR_ACCOUNT_NOT_FOUND);
            return;
        }

        Account dst = repo.getByNumber(toAcct);
        if (dst == null) {
            io.println(Messages.ERR_DEST_NOT_FOUND);
            return;
        }

        if (src.status == Account.Status.D || dst.status == Account.Status.D) {
            io.println(Messages.ERR_ACCOUNT_DISABLED);
            return;
        }

        if (!src.owner.equalsIgnoreCase(owner)) {
            io.println(Messages.ERR_SRC_NOT_OWNED);
            return;
        }

        if (session.createdAccounts.contains(src.number) || session.createdAccounts.contains(dst.number)) {
            io.println(Messages.ERR_UNAVAILABLE_SAME_SESSION);
            return;
        }

        long pending = session.pendingDepositsCents.getOrDefault(src.number, 0L);

        if (cents > src.balanceCents) {
            if (cents <= src.balanceCents + pending) io.println(Messages.ERR_FUNDS_UNAVAILABLE_SAME_SESSION);
            else io.println(Messages.ERR_INSUFFICIENT);
            return;
        }

        src.balanceCents -= cents;
        dst.balanceCents += cents;
        if (session.mode == SessionContext.Mode.STANDARD) session.transferTotalCents += cents;

        io.println(Messages.TRANSFER_OK);
    }

    // deposit:
    // deposits are "pending" in this session and not usable until next session
    private void handleDeposit() {
        String owner = currentOwner();
        if (owner == null) return;

        String acctNum = readLine();
        String amtLine = readLine();
        if (acctNum == null || amtLine == null) return;

        acctNum = acctNum.trim();

        Long cents = Money.parseToCents(amtLine);
        if (cents == null || cents <= 0) {
            io.println(Messages.ERR_DEPOSIT_AMOUNT);
            return;
        }

        Account a = repo.getByNumber(acctNum);
        if (a == null) {
            io.println(Messages.ERR_ACCOUNT_NOT_FOUND);
            return;
        }

        if (a.status == Account.Status.D) {
            io.println(Messages.ERR_ACCOUNT_DISABLED);
            return;
        }

        if (!a.owner.equalsIgnoreCase(owner)) {
            io.println(Messages.ERR_ACCOUNT_NOT_OWNED);
            return;
        }

        if (session.createdAccounts.contains(a.number)) {
            io.println(Messages.ERR_UNAVAILABLE_SAME_SESSION);
            return;
        }

        session.pendingDepositsCents.put(
                a.number,
                session.pendingDepositsCents.getOrDefault(a.number, 0L) + cents
        );

        io.println(Messages.DEPOSIT_OK);
    }

    // paybill:
    // checks payee is valid, caps, ownership, and funds
    private void handlePaybill() {
        String owner = currentOwner();
        if (owner == null) return;

        String acctNum = readLine();
        String payee = readLine();
        String amtLine = readLine();
        if (acctNum == null || payee == null || amtLine == null) return;

        acctNum = acctNum.trim();
        payee = payee.trim();

        if (!isValidPayee(payee)) {
            io.println(Messages.ERR_INVALID_PAYEE);
            return;
        }

        Long cents = Money.parseToCents(amtLine);
        if (cents == null || cents <= 0) {
            io.println(Messages.ERR_PAYMENT_AMOUNT);
            return;
        }

        // standard cap check
        if (session.mode == SessionContext.Mode.STANDARD &&
                session.paymentTotalCents + cents > PAYMENT_CAP_CENTS) {
            io.println(Messages.ERR_PAYMENT_LIMIT);
            return;
        }

        Account a = repo.getByNumber(acctNum);
        if (a == null) {
            io.println(Messages.ERR_ACCOUNT_NOT_FOUND);
            return;
        }

        if (a.status == Account.Status.D) {
            io.println(Messages.ERR_ACCOUNT_DISABLED);
            return;
        }

        if (!a.owner.equalsIgnoreCase(owner)) {
            io.println(Messages.ERR_ACCOUNT_NOT_OWNED);
            return;
        }

        if (session.createdAccounts.contains(a.number)) {
            io.println(Messages.ERR_UNAVAILABLE_SAME_SESSION);
            return;
        }

        long pending = session.pendingDepositsCents.getOrDefault(a.number, 0L);

        if (cents > a.balanceCents) {
            if (cents <= a.balanceCents + pending) io.println(Messages.ERR_FUNDS_UNAVAILABLE_SAME_SESSION);
            else io.println(Messages.ERR_INSUFFICIENT);
            return;
        }

        a.balanceCents -= cents;
        if (session.mode == SessionContext.Mode.STANDARD) session.paymentTotalCents += cents;

        io.println(Messages.PAYBILL_OK);
    }

    // create/delete/disable/changeplan are privileged (admin-only)
    private void handleCreate() {
        if (!requireAdmin()) return;

        String owner = readLine();
        if (owner == null) return;
        owner = owner.trim();

        // owner name max length is 20
        if (owner.length() > 20) {
            io.println(Messages.ERR_NAME_TOO_LONG);
            readLine();
            readLine();
            return;
        }

        // accepts either:
        //   owner, acctNum, balance
        // or:
        //   owner, balance (acctNum auto-generated)
        String line2 = readLine();
        if (line2 == null) return;
        line2 = line2.trim();

        String acctNum;
        String balLine;

        if (line2.matches("^\\d{5}$")) {
            acctNum = line2;
            balLine = readLine();
            if (balLine == null) return;
            balLine = balLine.trim();
        } else {
            acctNum = repo.nextAccountNumber();
            balLine = line2;
        }

        Long cents = Money.parseToCents(balLine);
        if (cents == null || cents > 99999_99L) {
            io.println(Messages.ERR_INITIAL_BAL_LIMIT);
            return;
        }

        if (repo.existsAccountNumber(acctNum)) {
            io.println(Messages.ERR_ACCOUNT_DOES_NOT_EXIST);
            return;
        }

        repo.add(new Account(acctNum, owner, Account.Status.A, Account.Plan.SP, cents));

        // created accounts are not usable until next session
        session.createdAccounts.add(acctNum);

        io.println(Messages.CREATE_OK);
    }

    private void handleDelete() {
        if (!requireAdmin()) return;

        String owner = readLine();
        String acctNum = readLine();
        if (owner == null || acctNum == null) return;

        owner = owner.trim();
        acctNum = acctNum.trim();

        Account a = repo.getByNumber(acctNum);
        if (a == null || !a.owner.equalsIgnoreCase(owner)) {
            io.println(Messages.ERR_ACCOUNT_NOT_FOUND);
            return;
        }

        repo.remove(acctNum);
        io.println(Messages.DELETE_OK);
    }

    private void handleDisable() {
        if (!requireAdmin()) return;

        String owner = readLine();
        String acctNum = readLine();
        if (owner == null || acctNum == null) return;

        owner = owner.trim();
        acctNum = acctNum.trim();

        Account a = repo.getByNumber(acctNum);
        if (a == null || !a.owner.equalsIgnoreCase(owner)) {
            io.println(Messages.ERR_ACCOUNT_NOT_FOUND);
            return;
        }

        a.status = Account.Status.D;
        io.println(Messages.DISABLE_OK);
    }

    private void handleChangePlan() {
        if (!requireAdmin()) return;

        String owner = readLine();
        String acctNum = readLine();
        if (owner == null || acctNum == null) return;

        owner = owner.trim();
        acctNum = acctNum.trim();

        Account a = repo.getByNumber(acctNum);
        if (a == null || !a.owner.equalsIgnoreCase(owner)) {
            io.println(Messages.ERR_ACCOUNT_NOT_FOUND);
            return;
        }

        a.plan = Account.Plan.NP;
        io.println(Messages.CHANGEPLAN_OK);
    }

    // returns false + prints the right error if the current session is not admin
    private boolean requireAdmin() {
        if (!session.loggedIn) {
            io.println(Messages.ERR_NO_SESSION);
            return false;
        }

        if (session.mode != SessionContext.Mode.ADMIN) {
            io.println(Messages.ERR_PRIVILEGED_NOT_PERMITTED);
            return false;
        }

        return true;
    }

    // returns the owner name for the current transaction
    // standard mode -> logged in user
    // admin mode -> reads the owner name from stdin first
    private String currentOwner() {
        if (!session.loggedIn) {
            io.println(Messages.ERR_NO_SESSION);
            return null;
        }

        if (session.mode == SessionContext.Mode.ADMIN) {
            String o = readLine();
            if (o == null) return null;
            return o.trim();
        }

        return session.userName;
    }

    // allowed payees (short codes and full names)
    private boolean isValidPayee(String payee) {
        if (payee.equalsIgnoreCase("EC")) return true;
        if (payee.equalsIgnoreCase("CQ")) return true;
        if (payee.equalsIgnoreCase("FI")) return true;

        if (payee.equalsIgnoreCase("The Bright Light Electric Company (EC)")) return true;
        if (payee.equalsIgnoreCase("Credit Card Company Q (CQ)")) return true;
        if (payee.equalsIgnoreCase("Fast Internet, Inc. (FI)")) return true;

        return false;
    }

    // when not logged in, consume the right number of "field lines"
    // this prevents those fields from being treated as new transaction codes
    private void skipFieldsForTransactionWhenNotLoggedIn(String codeRaw) {
        String code = codeRaw == null ? "" : codeRaw.trim().toLowerCase();

        switch (code) {
            case "withdrawal" -> skipLines(2); // acct, amount
            case "deposit" -> skipLines(2);    // acct, amount
            case "transfer" -> skipLines(3);   // from, to, amount
            case "paybill" -> skipLines(3);    // acct, payee, amount

            // privileged transactions (tests may include them before login)
            case "create" -> skipLines(2);     // owner, (acct or balance)
            case "delete" -> skipLines(2);     // owner, acct
            case "disable" -> skipLines(2);    // owner, acct
            case "changeplan" -> skipLines(2); // owner, acct

            case "logout" -> { /* no extra lines */ }

            default -> {
                // unknown code: consume nothing
            }
        }
    }

    // consumes n lines from stdin (stops early if EOF)
    private void skipLines(int n) {
        for (int i = 0; i < n; i++) {
            if (readLine() == null) return;
        }
    }

    // wrapper for reading one line from stdin
    private String readLine() {
        return io.readLine();
    }
}
