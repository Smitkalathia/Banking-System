from pathlib import Path
import sys

sys.path.append(str(Path(__file__).resolve().parents[1] / 'backend'))

"""Supplementary defect checks used for the Phase 5 failure table."""

from account import Account
from backend_processor import process_transactions
from transaction import Transaction


def make_accounts():
    return {
        "12345": Account("12345", "John Doe", "A", 100.00, 0, "NP"),
        "54321": Account("54321", "Jane Smith", "A", 250.50, 0, "SP"),
    }


def run_failure_checks():
    rows = []

    accounts = make_accounts()
    process_transactions(accounts, [Transaction("WDR", "12345", 150.00)])
    rows.append({
        "failure_id": "F-1",
        "scenario": "Withdrawal larger than available balance",
        "expected": "Transaction rejected and balance remains 100.00",
        "actual": f"Balance became {accounts['12345'].balance:.2f}",
        "status": "FAIL" if accounts["12345"].balance < 0 else "PASS",
        "cause": "handle_withdrawal() and Account.withdraw() do not validate sufficient funds",
    })

    accounts = make_accounts()
    process_transactions(accounts, [Transaction("DEP", "12345", -20.00)])
    rows.append({
        "failure_id": "F-2",
        "scenario": "Negative deposit amount",
        "expected": "Transaction rejected and balance remains 100.00",
        "actual": f"Balance became {accounts['12345'].balance:.2f}",
        "status": "FAIL" if accounts["12345"].balance != 100.00 else "PASS",
        "cause": "handle_deposit() and Account.deposit() do not validate positive amounts",
    })

    accounts = make_accounts()
    process_transactions(accounts, [Transaction("XFR", "12345", 500.00, "54321")])
    rows.append({
        "failure_id": "F-3",
        "scenario": "Transfer larger than source balance",
        "expected": "Transfer rejected and balances remain unchanged",
        "actual": (
            f"Source became {accounts['12345'].balance:.2f}; "
            f"destination became {accounts['54321'].balance:.2f}"
        ),
        "status": "FAIL" if accounts["12345"].balance < 0 else "PASS",
        "cause": "handle_transfer() and Account.transfer_out() do not validate sufficient funds",
    })

    return rows


if __name__ == "__main__":
    for row in run_failure_checks():
        print(
            f"{row['failure_id']}: {row['status']} | {row['scenario']} | "
            f"Expected: {row['expected']} | Actual: {row['actual']}"
        )
