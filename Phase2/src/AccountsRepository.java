// File: phase2/src/AccountsRepository.java
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads and provides access to bank accounts.
 *
 * Input format (fixed-width, 37 chars/line):
 * NNNNN_aaaaaaaaaaaaaaaaaaaa_S_pppppppp
 *
 * This repository is used by the Front End to validate accounts and simulate balance updates in-memory.
 */
public final class AccountsRepository {
    private final Map<String, Account> byNumber = new HashMap<>();

    /**
     * Loads the accounts file into memory.
     * - Lines shorter than 37 chars are ignored.
     * - Stops reading at END_OF_FILE record.
     * - Does not print debug output (tests compare stdout).
     */
    public void load(Path path) {
        byNumber.clear();
        try {
            for (String line : Files.readAllLines(path)) {
                if (line == null) continue;
                if (line.length() < 37) continue;

                String acct = line.substring(0, 5);
                String name = line.substring(6, 26).trim();
                String statusChar = line.substring(27, 28);
                String balStr = line.substring(29, 37).trim();

                if ("END_OF_FILE".equals(name)) break;

                Account.Status st = "D".equalsIgnoreCase(statusChar) ? Account.Status.D : Account.Status.A;
                Long cents = Money.parseToCents(balStr);
                if (cents == null) cents = 0L;

                // Prototype: plan not stored in file; default SP.
                byNumber.put(acct, new Account(acct, name, st, Account.Plan.SP, cents));
            }
        } catch (Exception ignored) {
            // No stdout output here; errors are surfaced via behaviour in tests.
        }
    }

    /** Returns the account by number, or null if missing. */
    public Account getByNumber(String acctNum) {
        if (acctNum == null) return null;
        return byNumber.get(acctNum.trim());
    }

    /** Returns true if any account exists with this owner name (case-insensitive). */
    public boolean ownerExists(String owner) {
        if (owner == null) return false;
        String o = owner.trim();
        if (o.isEmpty()) return false;
        for (Account a : byNumber.values()) {
            if (a.owner.equalsIgnoreCase(o)) return true;
        }
        return false;
    }

    /** Returns true if the account number exists in the system. */
    public boolean existsAccountNumber(String acctNum) {
        if (acctNum == null) return false;
        return byNumber.containsKey(acctNum.trim());
    }

    /** Adds a new account (admin create). */
    public void add(Account a) {
        byNumber.put(a.number, a);
    }

    /** Removes an account (admin delete). */
    public void remove(String acctNum) {
        if (acctNum == null) return;
        byNumber.remove(acctNum.trim());
    }

    /** Generates a next available 5-digit account number (prototype behaviour). */
    public String nextAccountNumber() {
        int max = 0;
        for (String k : byNumber.keySet()) {
            try {
                max = Math.max(max, Integer.parseInt(k));
            } catch (Exception ignored) {}
        }
        return String.format("%05d", max + 1);
    }
}
