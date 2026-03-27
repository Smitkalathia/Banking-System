"""
write.py

Writes the updated current bank accounts file for the Banking System Back End.

This module takes Account objects in memory and writes them back to the
current accounts file using the required fixed-width format.
"""

from print_error import print_error


def write_current_accounts(filename, accounts):
    """
    Write Account objects to the current accounts output file.

    Parameters:
        filename (str): Path to the output file.
        accounts (dict): Dictionary mapping account numbers to Account objects.
    """
    try:
        with open(filename, "w") as file:
            for account_number in sorted(accounts.keys()):
                account = accounts[account_number]
                formatted_line = format_account_line(account)
                file.write(formatted_line + "\n")

            file.write("END_OF_FILE\n")

    except Exception as error_message:
        print_error("fatal", f"Could not write to '{filename}': {error_message}")


def format_account_line(account):
    """
    Convert one Account object into a fixed-width output line.

    Output format:
        account number : 5 chars
        name           : 20 chars
        status         : 1 char
        balance        : 8 chars
        transaction ct : 4 chars
        plan           : 2 chars

    Total record length: 40 characters

    Parameters:
        account (Account): The account to format.

    Returns:
        str: The formatted fixed-width account record.

    Raises:
        ValueError: If the account contains invalid data.
    """
    account_number = str(account.account_number).strip()
    name = str(account.name).strip()
    status = str(account.status).strip()
    plan = str(account.plan).strip()

    try:
        balance = float(account.balance)
    except ValueError:
        raise ValueError("account balance must be numeric")

    try:
        transaction_count = int(account.transaction_count)
    except ValueError:
        raise ValueError("transaction count must be numeric")

    if not account_number.isdigit() or len(account_number) != 5:
        raise ValueError("account number must be exactly 5 digits")

    if status not in ("A", "D"):
        raise ValueError("status must be 'A' or 'D'")

    if plan not in ("SP", "NP"):
        raise ValueError("plan must be 'SP' or 'NP'")

    if balance < 0:
        raise ValueError("balance cannot be negative")

    if transaction_count < 0:
        raise ValueError("transaction count cannot be negative")

    account_number_field = f"{account_number:<5}"
    name_field = f"{name:<20}"[:20]
    status_field = f"{status:<1}"
    balance_field = f"{balance:08.2f}"
    transaction_count_field = f"{transaction_count:04d}"
    plan_field = f"{plan:<2}"

    return (
        account_number_field +
        name_field +
        status_field +
        balance_field +
        transaction_count_field +
        plan_field
    )