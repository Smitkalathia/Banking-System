import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class TransactionRecorder {

    private final Path out;
    private final List<String> records = new ArrayList<>();

    public TransactionRecorder(Path out) {
        this.out = out;
    }

    public void recordDeposit(String accountNumber, long cents) {
        records.add(String.format("DEP %s %.2f", accountNumber, cents / 100.0));
    }

    public void recordWithdrawal(String accountNumber, long cents) {
        records.add(String.format("WDR %s %.2f", accountNumber, cents / 100.0));
    }

    public void recordTransfer(String sourceAccountNumber, long cents, String destinationAccountNumber) {
        records.add(String.format("XFR %s %.2f %s",
                sourceAccountNumber, cents / 100.0, destinationAccountNumber));
    }

    public void recordCreate(String accountNumber, String ownerName) {
        records.add(String.format("NEW %s 0.00 %s", accountNumber, ownerName));
    }

    public void recordDelete(String accountNumber) {
        records.add(String.format("DEL %s 0.00", accountNumber));
    }

    public void recordDisable(String accountNumber) {
        records.add(String.format("DSB %s 0.00", accountNumber));
    }

    public void recordChangePlan(String accountNumber, String newPlan) {
        records.add(String.format("CHG %s 0.00 %s", accountNumber, newPlan));
    }

    public void writeOnLogout() {
        try {
            List<String> output = new ArrayList<>(records);
            output.add("EOS");
            Files.write(out, output);
            records.clear();
        } catch (IOException ignored) {
        }
    }
}