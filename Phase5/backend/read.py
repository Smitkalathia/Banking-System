"""
read.py

Reads the current bank accounts file for the Banking System Back End.

This module validates each account record and converts each valid line
into an Account object.
"""

from account import Account
from print_error import print_error


def read_current_accounts(filename):
    """
    Read the current accounts file and return a dictionary of Account objects.

    Parameters:
        filename (str): Path to the current accounts file.

    Returns:
        dict: A dictionary mapping account numbers to Account objects.
    """
    accounts = {}

    try:
        with open(filename, "r") as file:
            for line_number, line in enumerate(file, start=1):
                clean_line = line.rstrip("\n")

                if clean_line == "END_OF_FILE":
                    break

                if not clean_line.strip():
                    continue

                try:
                    account = parse_account_line(clean_line, line_number)
                    accounts[account.account_number] = account
                except ValueError as error_message:
                    print_error("error", f"Line {line_number}: {error_message}")

    except FileNotFoundError:
        print_error("fatal", f"File '{filename}' not found.")
        return {}

    except Exception as error_message:
        print_error("fatal", f"Unexpected error while reading '{filename}': {error_message}")
        return {}

    return accounts


def parse_account_line(line, line_number=0):
    """
    Parse one current account record into an Account object.

    Expected fixed-width format:
        account number : columns 0-4   (5 chars)
        name           : columns 5-24  (20 chars)
        status         : column  25    (1 char)
        balance        : columns 26-33 (8 chars, e.g. 00123.45)
        transaction ct : columns 34-37 (4 chars)
        plan           : columns 38-39 (2 chars)

    Total minimum line length: 40 characters

    Parameters:
        line (str): One line from the current accounts file.
        line_number (int): The line number for error reporting.

    Returns:
        Account: A parsed Account object.

    Raises:
        ValueError: If the line format is invalid.
    """
    if len(line) < 40:
        raise ValueError("line is too short to be a valid account record")

    account_number = line[0:5].strip()
    name = line[5:25].strip()
    status = line[25:26].strip()
    balance_str = line[26:34].strip()
    transaction_count_str = line[34:38].strip()
    plan = line[38:40].strip()

    if not account_number.isdigit() or len(account_number) != 5:
        raise ValueError("account number must be exactly 5 digits")

    if len(name) == 0:
        raise ValueError("account name cannot be empty")

    if status not in ("A", "D"):
        raise ValueError("status must be 'A' or 'D'")

    try:
        balance = float(balance_str)
    except ValueError:
        raise ValueError("balance must be a valid number")

    if balance < 0:
        raise ValueError("balance cannot be negative")

    if not transaction_count_str.isdigit():
        raise ValueError("transaction count must be numeric")

    transaction_count = int(transaction_count_str)

    if plan not in ("SP", "NP"):
        raise ValueError("plan must be 'SP' or 'NP'")

    return Account(
        account_number=account_number,
        name=name,
        status=status,
        balance=balance,
        transaction_count=transaction_count,
        plan=plan
    )