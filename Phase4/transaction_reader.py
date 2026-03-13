"""
transaction_reader.py

Reads the daily transaction file for the Banking System Back End.

This module validates transaction records and converts each valid line
into a Transaction object.
"""

from transaction import Transaction
from print_error import print_error


def read_transactions(filename):
    """
    Read the transaction file and return a list of Transaction objects.

    Parameters:
        filename (str): Path to the daily transaction file.

    Returns:
        list: A list of Transaction objects.
    """
    transactions = []

    try:
        with open(filename, "r") as file:
            for line_number, line in enumerate(file, start=1):
                clean_line = line.rstrip("\n")

                if not clean_line.strip():
                    continue

                try:
                    transaction = parse_transaction_line(clean_line, line_number)
                    transactions.append(transaction)

                    if transaction.is_end_of_session():
                        break

                except ValueError as error_message:
                    print_error("error", f"Line {line_number}: {error_message}")

    except FileNotFoundError:
        print_error("fatal", f"Transaction file '{filename}' not found.")

    except Exception as error_message:
        print_error("fatal", f"Unexpected error while reading '{filename}': {error_message}")

    return transactions


def parse_transaction_line(line, line_number=0):
    """
    Parse one transaction line into a Transaction object.

    Expected simple space-separated format:
        code account_number amount misc

    Examples:
        DEP 12345 200.00
        WDR 12345 50.00
        XFR 12345 75.00 54321
        NEW 12345 0.00 John_Doe
        EOS

    Parameters:
        line (str): One line from the transaction file.
        line_number (int): The line number for error reporting.

    Returns:
        Transaction: A parsed Transaction object.

    Raises:
        ValueError: If the line format is invalid.
    """
    parts = line.strip().split()

    if len(parts) == 0:
        raise ValueError("empty transaction line")

    code = parts[0].upper()

    valid_codes = {"DEP", "WDR", "XFR", "NEW", "DEL", "DSB", "CHG", "EOS"}

    if code not in valid_codes:
        raise ValueError(f"invalid transaction code '{code}'")

    if code == "EOS":
        return Transaction(code="EOS")

    account_number = None
    amount = 0.0
    misc = None

    if len(parts) >= 2:
        account_number = parts[1]

    if account_number is None or not account_number.isdigit() or len(account_number) != 5:
        raise ValueError("account number must be exactly 5 digits")

    if len(parts) >= 3:
        try:
            amount = float(parts[2])
        except ValueError:
            raise ValueError("amount must be a valid number")

    if len(parts) >= 4:
        misc = " ".join(parts[3:])

    return Transaction(
        code=code,
        account_number=account_number,
        amount=amount,
        misc=misc
    )