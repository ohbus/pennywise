"""Regression tests for QA-10 coverage inventory tooling."""

from __future__ import annotations

import unittest
from collections import Counter
from contextlib import redirect_stdout
from io import StringIO
from pathlib import Path

from tools.coverage.report_branch_gaps import all_gaps, main as branch_report_main
from tools.coverage.report_operation_test_gaps import inventory


ROOT = Path(__file__).resolve().parents[2]


class CoverageInventoryTest(unittest.TestCase):
    """Protect the exhaustive branch and operation discovery invariants."""

    def test_every_current_branch_gap_has_a_qa_row(self) -> None:
        gaps = all_gaps(ROOT)

        self.assertEqual(229, len(gaps))
        self.assertTrue(all(gap.qa_row.startswith("QA10-") for gap in gaps))
        self.assertTrue(all(gap.assignment_basis for gap in gaps))
        expected_counts = {
            "QA10-A01": 0,
                "QA10-A02": 1,
                "QA10-A03": 9,
                "QA10-A04": 0,
                "QA10-A05": 1,
                "QA10-A06": 6,
                "QA10-A07": 33,
                "QA10-A08": 0,
                "QA10-B01": 14,
                "QA10-B02": 20,
                "QA10-B03": 10,
                "QA10-B04": 5,
                "QA10-C01": 44,
                "QA10-C02": 0,
                "QA10-C03": 8,
                "QA10-C04": 9,
                "QA10-C05": 9,
                "QA10-C06": 6,
                "QA10-D01": 11,
                "QA10-D02": 10,
                "QA10-D03": 10,
                "QA10-D04": 6,
                "QA10-E01": 0,
                "QA10-E02": 15,
                "QA10-E03": 2,
                "QA10-E04": 0,
                "QA10-E05": 0,
        }
        actual_counts = Counter(gap.qa_row for gap in gaps)
        self.assertEqual(
            expected_counts,
            {row: actual_counts.get(row, 0) for row in expected_counts},
        )

    def test_operation_inventory_is_complete_and_assigned(self) -> None:
        operations = inventory(ROOT)

        self.assertEqual(54, len(operations))
        self.assertEqual(
            {"REST": 45, "GraphQL Query": 4, "GraphQL Mutation": 4, "GraphQL Subscription": 1},
            Counter(item.surface for item in operations),
        )
        self.assertTrue(all(item.acceptance_row == "QA10-E2E01" for item in operations))
        self.assertEqual(
            41,
            sum(not item.has_e2e_signal for item in operations),
        )
        self.assertEqual(
            49,
            sum(not item.has_bruno_signal for item in operations),
        )

    def test_branch_closure_gate_rejects_current_gaps(self) -> None:
        output = StringIO()

        with redirect_stdout(output):
            result = branch_report_main(
                ["--root", str(ROOT), "--format", "json", "--fail-on-gaps"]
            )

        self.assertEqual(1, result)
        self.assertIn('"qa_row"', output.getvalue())

    def test_audit_documents_every_acceptance_row(self) -> None:
        audit = (ROOT / "docs/quality/test-coverage-gap-audit.md").read_text(
            encoding="utf-8"
        )
        expected_rows = [
            *(f"QA10-A0{number}" for number in range(1, 9)),
            *(f"QA10-B0{number}" for number in range(1, 5)),
            *(f"QA10-C0{number}" for number in range(1, 7)),
            *(f"QA10-D0{number}" for number in range(1, 5)),
            *(f"QA10-E0{number}" for number in range(1, 6)),
            *(f"QA10-E2E0{number}" for number in range(1, 8)),
        ]

        for row in expected_rows:
            with self.subTest(row=row):
                self.assertIn(row, audit)


if __name__ == "__main__":
    unittest.main()
