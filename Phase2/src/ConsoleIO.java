// File: phase2/src/ConsoleIO.java
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Small wrapper around stdin/stdout to keep I/O testable and isolated.
 * - Reads lines from stdin
 * - Prints lines to stdout
 */
public final class ConsoleIO {
    private final BufferedReader in;
    private final PrintStream out;

    public ConsoleIO(BufferedReader in, PrintStream out) {
        this.in = in;
        this.out = out;
    }

    /** Reads a single line from stdin; returns null on EOF or I/O failure. */
    public String readLine() {
        try {
            return in.readLine();
        } catch (IOException e) {
            return null;
        }
    }

    /** Prints exactly one line to stdout. */
    public void println(String s) {
        out.println(s);
    }
}
