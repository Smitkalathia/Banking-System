from pathlib import Path
import sys

sys.path.append(str(Path(__file__).resolve().parents[1] / 'backend'))

import unittest

from account import Account
from backend_processor import process_transactions
from transaction import Transaction


class TestProcessTransactionsDecisionLoopCoverage(unittest.TestCase):
    """White box unit tests for decision and loop coverage of process_transactions()."""

    def make_accounts(self):
        return {
            "12345": Account("12345", "John Doe", "A", 100.00, 0, "NP"),
            "54321": Account("54321", "Jane Smith", "A", 250.50, 0, "SP"),
        }

    def test_dlc1_loop_executes_zero_times(self):
        accounts = self.make_accounts()
        process_transactions(accounts, [])
        self.assertEqual(accounts["12345"].balance, 100.00)
        self.assertEqual(accounts["54321"].balance, 250.50)

    def test_dlc2_single_deposit(self):
        accounts = self.make_accounts()
        transactions = [Transaction("DEP", "12345", 50.00)]
        process_transactions(accounts, transactions)
        self.assertEqual(accounts["12345"].balance, 150.00)
        self.assertEqual(accounts["12345"].transaction_count, 1)

    def test_dlc3_single_withdrawal(self):
        accounts = self.make_accounts()
        transactions = [Transaction("WDR", "54321", 20.00)]
        process_transactions(accounts, transactions)
        self.assertEqual(accounts["54321"].balance, 230.50)
        self.assertEqual(accounts["54321"].transaction_count, 1)

    def test_dlc4_valid_transfer(self):
        accounts = self.make_accounts()
        transactions = [Transaction("XFR", "12345", 25.00, "54321")]
        process_transactions(accounts, transactions)
        self.assertEqual(accounts["12345"].balance, 75.00)
        self.assertEqual(accounts["54321"].balance, 275.50)
        self.assertEqual(accounts["12345"].transaction_count, 1)

    def test_dlc5_create_new_account(self):
        accounts = self.make_accounts()
        transactions = [Transaction("NEW", "11111", 0.00, "Alice Lee")]
        process_transactions(accounts, transactions)
        self.assertIn("11111", accounts)
        self.assertEqual(accounts["11111"].name, "Alice Lee")
        self.assertEqual(accounts["11111"].status, "A")
        self.assertEqual(accounts["11111"].plan, "NP")

    def test_dlc6_delete_account(self):
        accounts = self.make_accounts()
        transactions = [Transaction("DEL", "12345", 0.00)]
        process_transactions(accounts, transactions)
        self.assertNotIn("12345", accounts)

    def test_dlc7_disable_account(self):
        accounts = self.make_accounts()
        transactions = [Transaction("DSB", "54321", 0.00)]
        process_transactions(accounts, transactions)
        self.assertEqual(accounts["54321"].status, "D")

    def test_dlc8_change_plan_valid(self):
        accounts = self.make_accounts()
        transactions = [Transaction("CHG", "12345", 0.00, "SP")]
        process_transactions(accounts, transactions)
        self.assertEqual(accounts["12345"].plan, "SP")
        self.assertEqual(accounts["12345"].transaction_count, 1)

    def test_dlc9_change_plan_invalid(self):
        accounts = self.make_accounts()
        transactions = [Transaction("CHG", "12345", 0.00, "XX")]
        process_transactions(accounts, transactions)
        self.assertEqual(accounts["12345"].plan, "NP")
        self.assertEqual(accounts["12345"].transaction_count, 0)

    def test_dlc10_eos_breaks_processing(self):
        accounts = self.make_accounts()
        transactions = [
            Transaction("DEP", "12345", 10.00),
            Transaction("EOS"),
            Transaction("WDR", "12345", 5.00),
        ]
        process_transactions(accounts, transactions)
        self.assertEqual(accounts["12345"].balance, 110.00)
        self.assertEqual(accounts["12345"].transaction_count, 1)

    def test_dlc11_unsupported_code_else_branch(self):
        accounts = self.make_accounts()
        transactions = [Transaction("ABC", "12345", 10.00)]
        process_transactions(accounts, transactions)
        self.assertEqual(accounts["12345"].balance, 100.00)
        self.assertEqual(accounts["12345"].transaction_count, 0)

    def test_dlc12_loop_executes_more_than_once(self):
        accounts = self.make_accounts()
        transactions = [
            Transaction("DEP", "12345", 50.00),
            Transaction("WDR", "54321", 20.00),
            Transaction("CHG", "12345", 0.00, "SP"),
        ]
        process_transactions(accounts, transactions)
        self.assertEqual(accounts["12345"].balance, 150.00)
        self.assertEqual(accounts["54321"].balance, 230.50)
        self.assertEqual(accounts["12345"].plan, "SP")


if __name__ == "__main__":
    unittest.main()
