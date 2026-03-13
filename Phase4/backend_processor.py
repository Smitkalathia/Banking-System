"""
backend_processor.py

Processes daily transactions for the Banking System Back End.

This module applies each transaction to the appropriate Account object(s)
and updates the current accounts data in memory.
"""

from account import Account
from print_error import print_error


def process_transactions(accounts, transactions):
    """
    Apply a list of transactions to the accounts dictionary.

    Parameters:
        accounts (dict): Dictionary mapping account numbers to Account objects.
        transactions (list): List of Transaction objects.
    """
    for transaction in transactions:
        code = transaction.code

        if code == "DEP":
            handle_deposit(accounts, transaction)
        elif code == "WDR":
            handle_withdrawal(accounts, transaction)
        elif code == "XFR":
            handle_transfer(accounts, transaction)
        elif code == "NEW":
            handle_create(accounts, transaction)
        elif code == "DEL":
            handle_delete(accounts, transaction)
        elif code == "DSB":
            handle_disable(accounts, transaction)
        elif code == "CHG":
            handle_change_plan(accounts, transaction)
        elif code == "EOS":
            break
        else:
            print_error("error", f"Unsupported transaction code: {code}")


def handle_deposit(accounts, transaction):
    """Apply a deposit transaction to an existing account."""
    account = accounts.get(transaction.account_number)

    if account is None:
        print_error("error", f"Deposit failed: account {transaction.account_number} not found")
        return

    account.deposit(transaction.amount)


def handle_withdrawal(accounts, transaction):
    """Apply a withdrawal transaction to an existing account."""
    account = accounts.get(transaction.account_number)

    if account is None:
        print_error("error", f"Withdrawal failed: account {transaction.account_number} not found")
        return

    account.withdraw(transaction.amount)


def handle_transfer(accounts, transaction):
    """
    Apply a transfer transaction.

    The source account is transaction.account_number.
    The destination account is stored in transaction.misc.
    """
    source_account = accounts.get(transaction.account_number)
    destination_account_number = transaction.misc
    destination_account = accounts.get(destination_account_number)

    if source_account is None:
        print_error("error", f"Transfer failed: source account {transaction.account_number} not found")
        return

    if destination_account is None:
        print_error("error", f"Transfer failed: destination account {destination_account_number} not found")
        return

    source_account.transfer_out(transaction.amount)
    destination_account.transfer_in(transaction.amount)


def handle_create(accounts, transaction):
    """
    Create a new account.

    For this prototype, transaction.misc is used as the account holder name.
    """
    if transaction.account_number in accounts:
        print_error("error", f"Create failed: account {transaction.account_number} already exists")
        return

    account_name = transaction.misc if transaction.misc else "NEW USER"

    accounts[transaction.account_number] = Account(
        account_number=transaction.account_number,
        name=account_name,
        status="A",
        balance=0.0,
        transaction_count=0,
        plan="NP"
    )


def handle_delete(accounts, transaction):
    """Delete an account if it exists."""
    if transaction.account_number not in accounts:
        print_error("error", f"Delete failed: account {transaction.account_number} not found")
        return

    del accounts[transaction.account_number]


def handle_disable(accounts, transaction):
    """Disable an existing account."""
    account = accounts.get(transaction.account_number)

    if account is None:
        print_error("error", f"Disable failed: account {transaction.account_number} not found")
        return

    account.disable()


def handle_change_plan(accounts, transaction):
    """
    Change the account plan.

    For this prototype, transaction.misc should contain the new plan code.
    """
    account = accounts.get(transaction.account_number)

    if account is None:
        print_error("error", f"Change plan failed: account {transaction.account_number} not found")
        return

    new_plan = transaction.misc

    if new_plan not in ("SP", "NP"):
        print_error("error", f"Change plan failed: invalid plan '{new_plan}'")
        return

    account.change_plan(new_plan)