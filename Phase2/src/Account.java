// File: phase2/src/Account.java
/**
 * In-memory model of one bank account record loaded from the accounts file.
 *
 * Note:
 * - This is not a database model; it is a simple in-memory structure for the Front End session.
 */
public final class Account {
    public enum Status { A, D } // Active / Disabled
    public enum Plan { SP, NP } // Student / Non-student

    public final String number;   // 5-digit account number as text (e.g., "00001")
    public final String owner;    // trimmed owner name (case-insensitive comparisons)
    public Status status;
    public Plan plan;
    public long balanceCents;     // available balance in cents (pending deposits excluded)

    public Account(String number, String owner, Status status, Plan plan, long balanceCents) {
        this.number = number;
        this.owner = owner;
        this.status = status;
        this.plan = plan;
        this.balanceCents = balanceCents;
    }
}
