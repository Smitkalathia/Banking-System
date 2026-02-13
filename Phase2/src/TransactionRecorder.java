// File: phase2/src/TransactionRecorder.java
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes the daily transaction file on logout.
 *
 * Phase 2 note:
 * - You requested to not focus on transaction file formatting right now.
 * - This class still creates/overwrites the file to satisfy the CLI contract.
 */
public final class TransactionRecorder {
    private final Path out;

    public TransactionRecorder(Path out) {
        this.out = out;
    }

    /** Writes/overwrites the daily transaction file on logout (prototype output). */
    public void writeOnLogout() {
        try {
            Files.writeString(out, "");
        } catch (Exception ignored) {}
    }
}
