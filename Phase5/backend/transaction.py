"""
transaction.py

Defines the Transaction class used by the Banking System Back End.

A Transaction object represents one transaction record read from the
daily transaction file.
"""


class Transaction:
    """
    Represents one transaction read from the transaction file.
    """

    def __init__(self, code, account_number=None, amount=0.0, misc=None):
        """
        Create a Transaction object.

        Parameters:
            code (str): The transaction code (e.g., DEP, WDR, XFR, etc.).
            account_number (str): The primary account involved in the transaction.
            amount (float): The transaction amount.
            misc (str): Any extra information needed for certain transactions.
        """
        self.code = code.strip()
        self.account_number = account_number
        self.amount = float(amount)
        self.misc = misc

    @staticmethod
    def from_line(line):
        """
        Parse a line from the transaction file and create a Transaction object.

        Parameters:
            line (str): One line from the transaction file.

        Returns:
            Transaction: A parsed transaction object.
        """

        parts = line.strip().split()

        if len(parts) == 0:
            raise ValueError("Empty transaction line")

        code = parts[0]

        account_number = None
        amount = 0.0
        misc = None

        if len(parts) > 1:
            account_number = parts[1]

        if len(parts) > 2:
            try:
                amount = float(parts[2])
            except ValueError:
                amount = 0.0

        if len(parts) > 3:
            misc = parts[3]

        return Transaction(code, account_number, amount, misc)

    def is_end_of_session(self):
        """
        Check if this transaction marks the end of the session.

        Returns:
            bool: True if the transaction code indicates end of session.
        """
        return self.code.upper() == "EOS"

    def __str__(self):
        """
        Return a readable string representation of the transaction.

        Returns:
            str: A formatted string describing the transaction.
        """
        return (
            f"Transaction(code={self.code}, "
            f"account_number={self.account_number}, "
            f"amount={self.amount}, misc={self.misc})"
        )