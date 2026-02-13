// File: phase2/src/AtmApp.java
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;

/**
 * Banking System Front End (ATM) console application.
 *
 * Intended usage:
 *   java AtmApp <currentaccounts.txt> <transout.atf>
 *
 * Inputs:
 * - stdin: transaction stream (one transaction code per line, followed by required fields)
 * - file: current bank accounts file (fixed-width records, 37 chars per line)
 *
 * Outputs:
 * - stdout: user-visible responses (success/error messages)
 * - file: daily transaction file written on logout (<transout.atf>)
 *
 * Phase 2 rapid prototype: architecture and readability prioritized; full test conformance is finalized in later phases.
 */
public final class AtmApp {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java AtmApp <currentaccounts.txt> <transout.atf>");
            return;
        }

        Path accountsPath = Path.of(args[0]);
        Path transOutPath = Path.of(args[1]);

        ConsoleIO io = new ConsoleIO(
                new BufferedReader(new InputStreamReader(System.in)),
                System.out
        );

        AccountsRepository repo = new AccountsRepository();
        repo.load(accountsPath);

        TransactionRecorder recorder = new TransactionRecorder(transOutPath);

        TransactionProcessor processor = new TransactionProcessor(io, repo, recorder);
        processor.run();
    }
}
