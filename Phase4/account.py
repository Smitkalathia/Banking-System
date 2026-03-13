"""
account.py

Defines the Account class used by the Banking System Back End.

The Account class stores the information for one bank account and provides
simple methods to update that account during transaction processing.
"""


class Account:
    """
    Represents one bank account in memory while the back end is running.
    """

    def __init__(self, account_number, name, status, balance, transaction_count, plan):
        """
        Create a new Account object.

        Parameters:
            account_number (str): The unique account number.
            name (str): The account holder name.
            status (str): The account status, usually 'A' or 'D'.
            balance (float): The current account balance.
            transaction_count (int): The number of transactions performed.
            plan (str): The account plan, usually 'SP' or 'NP'.
        """
        self.account_number = str(account_number)
        self.name = str(name).strip()
        self.status = str(status).strip()
        self.balance = float(balance)
        self.transaction_count = int(transaction_count)
        self.plan = str(plan).strip()

    def deposit(self, amount):
        """
        Add money to the account balance.

        Parameters:
            amount (float): The amount to add.
        """
        self.balance += float(amount)
        self.transaction_count += 1

    def withdraw(self, amount):
        """
        Remove money from the account balance.

        Parameters:
            amount (float): The amount to remove.
        """
        self.balance -= float(amount)
        self.transaction_count += 1

    def transfer_out(self, amount):
        """
        Remove money from this account as part of a transfer.

        Parameters:
            amount (float): The amount to transfer out.
        """
        self.balance -= float(amount)
        self.transaction_count += 1

    def transfer_in(self, amount):
        """
        Add money to this account as part of a transfer.

        Parameters:
            amount (float): The amount to transfer in.
        """
        self.balance += float(amount)

    def enable(self):
        """
        Mark the account as enabled or active.
        """
        self.status = "A"

    def disable(self):
        """
        Mark the account as disabled.
        """
        self.status = "D"

    def change_plan(self, new_plan):
        """
        Change the account plan.

        Parameters:
            new_plan (str): The new account plan.
        """
        self.plan = str(new_plan).strip()
        self.transaction_count += 1

    def to_dict(self):
        """
        Convert the account object into a dictionary.

        Returns:
            dict: A dictionary version of the account data.
        """
        return {
            "number": self.account_number,
            "name": self.name,
            "status": self.status,
            "balance": self.balance,
            "transaction_count": self.transaction_count,
            "plan": self.plan
        }

    def __str__(self):
        """
        Return a readable string representation of the account.

        Returns:
            str: A formatted string describing the account.
        """
        return (
            f"Account(number={self.account_number}, name='{self.name}', "
            f"status='{self.status}', balance={self.balance:.2f}, "
            f"transaction_count={self.transaction_count}, plan='{self.plan}')"
        )