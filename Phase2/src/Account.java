// this represents one bank account loaded from data/currentaccounts.txt
public final class Account {
    public enum Status { A, D } // active, disabled
    public enum Plan { SP, NP } // student,  non-student
    public final String number;   // 5-digit account number .txt
    public final String owner;    // trimmed owner name 
    public Status status; // current status
    public Plan plan; // current plan
    public long balanceCents;     // available balance in cents 

    // constructor used when loading account data
    public Account(String number, String owner, Status status, Plan plan, long balanceCents) {
        this.number = number;
        this.owner = owner;
        this.status = status;
        this.plan = plan;
        this.balanceCents = balanceCents;
    }
}
