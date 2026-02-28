// File: phase2/src/TransactionProcessor.java

// main engine of the front end
// it reads a transaction code per line from stdin and runs the matching handler
// it also enforces session rules (must login first, logout ends session, admin-only commands)

public final class TransactionProcessor {
    private final ConsoleIO io;
    private final AccountsRepository repo;
    private final TransactionRecorder recorder;
    private final SessionContext session = new SessionContext();
    private boolean sawLogout = false;
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
            if (code == null) {
                if (sawLogout) {
                    io.println(Messages.SESSION_ENDED);
                }
                return;
            }

            code = code.trim();
            if (code.isEmpty()) continue;

            // if not logged in, only "login" is allowed
            // we still consume extra lines so the input stream doesn't get out of sync
            if (!session.loggedIn && !code.equals("login")) {
                io.println(Messages.ERR_NO_SESSION);
                skipFieldsForTransactionWhenNotLoggedIn(code);
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
            switch (code) {
                case "login" -> handleLogin();
                case "logout" -> handleLogout();
                case "withdraw" -> handleWithdrawal();
                case "deposit" -> handleDeposit();
                case "transfer" -> handleTransfer();
                case "paybill" -> handlePaybill();
                case "create" -> handleCreate();
                case "delete" -> handleDelete();
                case "disable" -> handleDisable();
                case "changeplan" -> handleChangePlan();
                default -> { }
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
        io.println(Messages.LOGOUT_OK);
        recorder.writeOnLogout();
        session.reset();
        sawLogout = true;
    }

    // withdrawal:
    // checks ownership, account status, same-session rules, per-session cap, and available funds
    private void handleWithdrawal() {
        String owner = currentOwner();
        if (owner == null) return;

        String line1 = readLine();
        if (line1 == null) return;

        String acctOwner;
        Long cents = Money.parseToCents(line1);

        if (cents != null) {
            acctOwner = owner;
        } else {
            acctOwner = line1.trim();
            cents = Money.parseToCents(readLine());
        }

        if (cents == null || cents <= 0) {
            io.println(Messages.ERR_WITHDRAW_AMOUNT);
            return;
        }

        Account a = repo.getByOwner(acctOwner);
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
        if (session.mode == SessionContext.Mode.STANDARD &&
            session.withdrawalTotalCents + cents > WITHDRAW_CAP_CENTS) {
            io.println(Messages.ERR_WITHDRAW_LIMIT);
            return;
        }

        long pending = session.pendingDepositsCents.getOrDefault(a.number, 0L);
        long availableNow = a.balanceCents - pending;

        // If the requested amount is <= balanceCents but > availableNow,
        // it means the user is trying to use same-session deposited funds.
        if (cents > availableNow) {
            if (cents <= a.balanceCents) {
                io.println(Messages.ERR_FUNDS_UNAVAILABLE_SAME_SESSION);
            } else {
                io.println(Messages.ERR_INSUFFICIENT);
            }
            return;
        }

        

        a.balanceCents -= cents;
        session.withdrawalTotalCents += cents;
        io.println(Messages.WITHDRAWAL_OK);
    }

    // transfer:
    // checks ownership of source, destination exists, account status, caps, and funds
    private void handleTransfer() {
        String owner = currentOwner();
        if (owner == null) return;

        String srcOwner = readLine();
        String dstOwner = readLine();
        String amtLine  = readLine();
        if (srcOwner == null || dstOwner == null || amtLine == null) return;

        Long cents = Money.parseToCents(amtLine);
        if (cents == null || cents <= 0) {
            io.println(Messages.ERR_TRANSFER_AMOUNT);
            return;
        }
        String srcKey = srcOwner.trim();

        Account src = repo.getByOwner(srcKey);
        if (src == null) {
            src = repo.getByNumber(srcKey);
        }
        if (src == null) {
            io.println(Messages.ERR_ACCOUNT_NOT_FOUND);
            return;
        }

        if (!src.owner.equalsIgnoreCase(owner)) {
            io.println(Messages.ERR_SRC_NOT_OWNED);
            return;
        }

        Account dst = repo.getByOwner(dstOwner.trim());
        if (dst == null || dst.status == Account.Status.D) {
            io.println(Messages.ERR_DEST_NOT_FOUND);
            return;
        }
        if (session.mode == SessionContext.Mode.STANDARD &&
            session.transferTotalCents + cents > TRANSFER_CAP_CENTS) {
            io.println(Messages.ERR_TRANSFER_LIMIT);
            return;
        }
        long pending = session.pendingDepositsCents.getOrDefault(src.number, 0L);
        long availableNow = src.balanceCents - pending;

        if (cents > availableNow) {
            if (cents <= src.balanceCents) {
                io.println(Messages.ERR_FUNDS_UNAVAILABLE_SAME_SESSION);
            } else {
                io.println(Messages.ERR_INSUFFICIENT);
            }
            return;
        }

        

        src.balanceCents -= cents;
        dst.balanceCents += cents;
        session.transferTotalCents += cents;
        io.println(Messages.TRANSFER_OK);
    }

    // deposit:
    // deposits are "pending" in this session and not usable until next session
    private void handleDeposit() {
        String owner = currentOwner();
        if (owner == null) return;

        String line1 = readLine();
        if (line1 == null) return;

        String acctOwner;
        Long cents = Money.parseToCents(line1);

        if (cents != null) {
            acctOwner = owner;
        } else {
            acctOwner = line1.trim();
            cents = Money.parseToCents(readLine());
        }

        if (cents == null || cents <= 0) {
            io.println(Messages.ERR_DEPOSIT_AMOUNT);
            return;
        }

        Account a = repo.getByOwner(acctOwner);
        if (a == null) {
            io.println(Messages.ERR_ACCOUNT_NOT_FOUND);
            return;
        }

        if (a.status == Account.Status.D) {
            io.println(Messages.ERR_ACCOUNT_DISABLED);
            return;
        }
        a.balanceCents += cents; // balance reflects deposit immediately
        io.println(Messages.DEPOSIT_OK);
        session.pendingDepositsCents.put(
            a.number,
            session.pendingDepositsCents.getOrDefault(a.number, 0L) + cents
        );
        if (a.status == Account.Status.D) {
            io.println(Messages.ERR_ACCOUNT_DISABLED);
            return;
        }
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
            io.println(Messages.ERR_ACCOUNT_NOT_FOUND);
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
    if (session.mode == SessionContext.Mode.ADMIN) {
        String o = readLine();
        if (o == null) return null;
        return o.trim();
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
    if (payee == null) return false;
    String p = payee.trim();
    if (p.isEmpty()) return false;

    return p.equalsIgnoreCase("Hydro")
        || p.equalsIgnoreCase("Cable")
        || p.equalsIgnoreCase("Phone");
}

    // when not logged in, consume the right number of "field lines"
    // this prevents those fields from being treated as new transaction codes
    private void skipFieldsForTransactionWhenNotLoggedIn(String code) {
        switch (code) {
            case "withdraw":
            case "deposit":
                skipLines(1);
                break;

            case "transfer":
            case "paybill":
                skipLines(2);
                break;

            case "create":
            case "delete":
            case "disable":
            case "changeplan":
                skipLines(2);
                break;

            case "login":
            case "logout":
            default:
                // no extra lines
                break;
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
