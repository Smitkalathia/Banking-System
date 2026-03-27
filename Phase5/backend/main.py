"""
main.py

Back End prototype for the Banking System.

This program reads a current accounts file and a daily transaction file,
processes the transactions in memory, and writes an updated current accounts
file.

Intended usage:
    python main.py <current_accounts_file> <transaction_file> <output_file>

Inputs:
    1. Current accounts file
    2. Daily transaction file

Output:
    1. Updated current accounts file
"""

import sys

from read import read_current_accounts
from transaction_reader import read_transactions
from backend_processor import process_transactions
from write import write_current_accounts
from print_error import print_error


def main():
    """
    Run the Banking System Back End prototype.

    This function validates command line arguments, reads the input files,
    processes all transactions, and writes the updated accounts to the
    output file.
    """
    if len(sys.argv) != 4:
        print_error(
            "fatal",
            "Usage: python main.py <current_accounts_file> <transaction_file> <output_file>"
        )

    current_accounts_file = sys.argv[1]
    transaction_file = sys.argv[2]
    output_file = sys.argv[3]

    accounts = read_current_accounts(current_accounts_file)
    transactions = read_transactions(transaction_file)

    process_transactions(accounts, transactions)

    write_current_accounts(output_file, accounts)

    print("Back End processing complete.")
    print(f"Read accounts from: {current_accounts_file}")
    print(f"Read transactions from: {transaction_file}")
    print(f"Wrote updated accounts to: {output_file}")


if __name__ == "__main__":
    main()