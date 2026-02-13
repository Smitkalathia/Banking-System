import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;

// this is the main entry point for the front end program
// it reads transactions from stdin and prints messages to stdout
// it also loads accounts from currentaccounts.txt and writes the daily .atf file on logout
//
// usage:
//   java AtmApp <currentaccounts.txt> <transout.atf>
//
// note: this is a first version (not fully tested yet)

public final class AtmApp {
    public static void main(String[] args) {

        // needs 2 command-line args:
        // args[0] = path to currentaccounts.txt
        // args[1] = path to output transaction file (.atf)
        if (args.length != 2) {
            System.out.println("Usage: java AtmApp <currentaccounts.txt> <transout.atf>");
            return;
        }

        Path accountsPath = Path.of(args[0]);   // input accounts file
        Path transOutPath = Path.of(args[1]);   // output transaction file

        // wraps stdin/stdout so the rest of the code stays clean
        ConsoleIO io = new ConsoleIO(
                new BufferedReader(new InputStreamReader(System.in)),
                System.out
        );

        // loads all accounts into memory at startup
        AccountsRepository repo = new AccountsRepository();
        repo.load(accountsPath);

        // handles writing the daily transaction file on logout
        TransactionRecorder recorder = new TransactionRecorder(transOutPath);

        // main driver that reads transaction codes and runs handlers
        TransactionProcessor processor = new TransactionProcessor(io, repo, recorder);
        processor.run();
    }
}
