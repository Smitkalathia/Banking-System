// this loads and manages all bank accounts from data/currentaccounts.txt
// accounts are stored in memory using a map (account number -> Account)

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class AccountsRepository {

    // stores accounts by account number
    private final Map<String, Account> byNumber = new HashMap<>();

    // loads the accounts file into memory
    // ignores short lines and stops at END_OF_FILE
    // does not print anything (for test comparison)
    public void load(Path path) {
        byNumber.clear();

        try {
            for (String line : Files.readAllLines(path)) {
                if (line == null) continue;
                if (line.length() < 37) continue;

                String acct = line.substring(0, 5);            // account number
                String name = line.substring(6, 26).trim();    // owner name
                String statusChar = line.substring(27, 28);    // A or D
                String balStr = line.substring(29, 37).trim(); // balance

                // stop reading when we hit end marker
                if ("END_OF_FILE".equals(name)) break;

                Account.Status st =
                        "D".equalsIgnoreCase(statusChar)
                        ? Account.Status.D
                        : Account.Status.A;

                Long cents = Money.parseToCents(balStr);
                if (cents == null) cents = 0L;

                // prototype: plan not stored in file, default to student plan
                byNumber.put(acct,
                        new Account(acct, name, st, Account.Plan.SP, cents));
            }
        } catch (Exception ignored) {
            // no stdout here (tests compare exact output)
        }
    }
    
    // returns the account by number, or null if it does not exist
    public Account getByNumber(String acctNum) {
        if (acctNum == null) return null;
        return byNumber.get(acctNum.trim());
    }

    // returns true if an owner exists (case-insensitive check)
    public boolean ownerExists(String owner) {
        if (owner == null) return false;

        String o = owner.trim();
        if (o.isEmpty()) return false;

        for (Account a : byNumber.values()) {
            if (a.owner.equalsIgnoreCase(o)) return true;
        }
        return false;
    }

    // returns true if the account number exists
    public boolean existsAccountNumber(String acctNum) {
        if (acctNum == null) return false;
        return byNumber.containsKey(acctNum.trim());
    }

    // adds a new account (used for admin create)
    public void add(Account a) {
        byNumber.put(a.number, a);
    }

    // removes an account (used for admin delete)
    public void remove(String acctNum) {
        if (acctNum == null) return;
        byNumber.remove(acctNum.trim());
    }

    // generates the next available 5-digit account number (prototype logic)
    public String nextAccountNumber() {
        int max = 0;

        for (String k : byNumber.keySet()) {
            try {
                max = Math.max(max, Integer.parseInt(k));
            } catch (Exception ignored) {}
        }

        return String.format("%05d", max + 1);
    }
    // gets owner type
    // Lookup account by owner name (case-insensitive)
    public Account getByOwner(String owner) {
        if (owner == null) return null;

        String o = owner.trim();
        if (o.isEmpty()) return null;

        for (Account a : byNumber.values()) {
            if (a.owner.equalsIgnoreCase(o)) {
                return a;
            }
        }
        return null;
    }
}
