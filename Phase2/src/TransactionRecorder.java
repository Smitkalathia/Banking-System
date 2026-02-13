import java.nio.file.Files;
import java.nio.file.Path;

// handles writing the daily transaction file (.atf)
// for phase 2, we are not focusing on formatting the file yet
// it simply creates or overwrites the file on logout

public final class TransactionRecorder {

    private final Path out; // path to the output .atf file

    public TransactionRecorder(Path out) {
        this.out = out;
    }

    // called when logout happens
    // currently just creates/clears the file (prototype behavior)
    public void writeOnLogout() {
        try {
            Files.writeString(out, "");
        } catch (Exception ignored) {
            // prototype: no detailed error handling yet
        }
    }
}
