import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;

// this is a small wrapper around stdin and stdout
// it keeps input/output separate from the main logic
// makes it easier to test and keeps TransactionProcessor cleaner

public final class ConsoleIO {

    private final BufferedReader in;   // reads from standard input
    private final PrintStream out;     // writes to standard output

    // constructor that wraps stdin and stdout
    public ConsoleIO(BufferedReader in, PrintStream out) {
        this.in = in;
        this.out = out;
    }

    // reads one line from input
    // returns null if end-of-file or if something goes wrong
    public String readLine() {
        try {
            return in.readLine();
        } catch (IOException e) {
            return null;
        }
    }

    // prints one line to output
    public void println(String s) {
        out.println(s);
    }
}
