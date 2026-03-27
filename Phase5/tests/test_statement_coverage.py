from pathlib import Path
import sys

sys.path.append(str(Path(__file__).resolve().parents[1] / 'backend'))

import unittest

from read import parse_account_line


class TestParseAccountLineStatementCoverage(unittest.TestCase):
    """White box unit tests for statement coverage of parse_account_line()."""

    def make_line(
        self,
        account_number="12345",
        name="John Doe",
        status="A",
        balance="00100.00",
        transaction_count="0000",
        plan="NP",
    ):
        return f"{account_number:<5}{name:<20}{status}{balance}{transaction_count}{plan}"

    def test_sc1_valid_account_line(self):
        line = self.make_line()
        account = parse_account_line(line)
        self.assertEqual(account.account_number, "12345")
        self.assertEqual(account.name, "John Doe")
        self.assertEqual(account.status, "A")
        self.assertEqual(account.balance, 100.00)
        self.assertEqual(account.transaction_count, 0)
        self.assertEqual(account.plan, "NP")

    def test_sc2_line_too_short(self):
        with self.assertRaisesRegex(ValueError, "line is too short"):
            parse_account_line("12345Short")

    def test_sc3_invalid_status(self):
        line = self.make_line(status="X")
        with self.assertRaisesRegex(ValueError, "status must be 'A' or 'D'"):
            parse_account_line(line)

    def test_sc4_invalid_balance_text(self):
        line = self.make_line(balance="ABCDEFGH")
        with self.assertRaisesRegex(ValueError, "balance must be a valid number"):
            parse_account_line(line)

    def test_sc5_invalid_plan(self):
        line = self.make_line(plan="XX")
        with self.assertRaisesRegex(ValueError, "plan must be 'SP' or 'NP'"):
            parse_account_line(line)


if __name__ == "__main__":
    unittest.main()
