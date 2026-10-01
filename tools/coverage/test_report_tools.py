"""Regression tests for QA-10 coverage inventory tooling."""

from __future__ import annotations

from collections import Counter
from contextlib import redirect_stdout
from io import StringIO
from pathlib import Path
import re
import unittest

from tools.coverage.report_branch_gaps import (
    all_gaps,
    main as branch_report_main,
    markdown as render_branch_gaps,
)
from tools.coverage.report_operation_test_gaps import inventory, references


ROOT = Path(__file__).resolve().parents[2]


class CoverageInventoryTest(unittest.TestCase):
    """Protect the exhaustive branch and operation discovery invariants."""

    def test_every_current_branch_gap_has_a_qa_row(self) -> None:
        gaps = all_gaps(ROOT)

        self.assertEqual(78, len(gaps))
        self.assertTrue(all(gap.qa_row.startswith("QA10-") for gap in gaps))
        self.assertTrue(all(gap.assignment_basis for gap in gaps))
        self.assertTrue(all(gap.acceptance_criteria for gap in gaps))
        expected_counts = {
            "QA10-A01": 0,
                "QA10-A02": 0,
                "QA10-A03": 1,
                "QA10-A04": 0,
                "QA10-A05": 1,
                "QA10-A06": 1,
                "QA10-A07": 7,
                "QA10-A08": 1,
                "QA10-B01": 2,
                "QA10-B02": 2,
                "QA10-B03": 3,
                "QA10-B04": 2,
                "QA10-C01": 17,
                "QA10-C02": 0,
                "QA10-C03": 8,
                "QA10-C04": 9,
                "QA10-C05": 3,
                "QA10-C06": 3,
                "QA10-D01": 5,
                "QA10-D02": 2,
                "QA10-D03": 6,
                "QA10-D04": 2,
                "QA10-E01": 1,
                "QA10-E02": 2,
                "QA10-E03": 0,
                "QA10-E04": 0,
                "QA10-E05": 0,
        }
        actual_counts = Counter(gap.qa_row for gap in gaps)
        self.assertEqual(
            expected_counts,
            {row: actual_counts.get(row, 0) for row in expected_counts},
        )
        self.assertEqual(144, sum(gap.missed_branches for gap in gaps))

    def test_operation_inventory_is_complete_and_assigned(self) -> None:
        operations = inventory(ROOT)

        self.assertEqual(54, len(operations))
        self.assertEqual(
            {"REST": 45, "GraphQL Query": 4, "GraphQL Mutation": 4, "GraphQL Subscription": 1},
            Counter(item.surface for item in operations),
        )
        self.assertTrue(all(item.acceptance_row == "QA10-E2E01" for item in operations))
        self.assertEqual(
            32,
            sum(not item.has_e2e_signal for item in operations),
        )
        self.assertEqual(
            49,
            sum(not item.has_bruno_signal for item in operations),
        )

    def test_operation_references_do_not_accept_identifier_substrings(self) -> None:
        sources = (
            ("tests/e2e/unrelated.py", "groups = []\ngroupId = 'x'"),
            ("tests/e2e/actual.py", "query = '{ group }'"),
        )

        self.assertEqual(("tests/e2e/actual.py",), references("group", sources))

    def test_branch_closure_gate_rejects_current_gaps(self) -> None:
        output = StringIO()

        with redirect_stdout(output):
            result = branch_report_main(
                ["--root", str(ROOT), "--format", "json", "--fail-on-gaps"]
            )

        self.assertEqual(1, result)
        self.assertIn('"qa_row"', output.getvalue())

    def test_branch_markdown_is_an_exact_per_record_ledger(self) -> None:
        gaps = all_gaps(ROOT)
        rendered = render_branch_gaps(gaps)
        rows = rendered.splitlines()

        self.assertEqual(80, len(rows))
        self.assertEqual(
            "| Module | QA row | Production class | Source | Method | Line | Missed | Covered | Assignment | Report | Acceptance criteria |",
            rows[0],
        )
        self.assertEqual(
            "| --- | --- | --- | --- | --- | ---: | ---: | ---: | --- | --- | --- |",
            rows[1],
        )
        self.assertEqual(
            sum(gap.missed_branches for gap in gaps),
            sum(int(row.split("|")[7].strip()) for row in rows[2:]),
        )

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

    def test_audit_requires_per_record_and_operation_evidence(self) -> None:
        audit = (ROOT / "docs/quality/test-coverage-gap-audit.md").read_text(
            encoding="utf-8"
        )

        for requirement in (
            "### Per-record closure contract",
            "test file and test name",
            "exact JaCoCo mapping",
            "| Authentication |",
            "| Authorization |",
            "| Durable state |",
            "| Asynchronous state |",
            "| Replay and concurrency |",
            "| Isolation and redaction |",
            "### Operation-specific E2E acceptance matrix",
            "### Current per-record ledger status",
            "does **not** yet have closure evidence for",
            "Passwordless authentication",
            "Expense financial/search",
            "Notifications: `getPreferences`",
            "### Reviewed structural branch candidates",
            "The private `ProfileController.mapErrorCode` record is intentionally **not**",
            "do not simplify the terminal guard",
        ):
            with self.subTest(requirement=requirement):
                self.assertIn(requirement, audit)

    def test_audit_has_complete_acceptance_cells_for_unit_and_e2e_rows(self) -> None:
        audit = (ROOT / "docs/quality/test-coverage-gap-audit.md").read_text(
            encoding="utf-8"
        )
        unit_rows = re.findall(
            r"(?m)^\| (QA10-(?:A0[1-8]|B0[1-4]|C0[1-6]|D0[1-4]|E0[1-5]))"
            r" \| ([^|]+) \| ([^|]+) \| ([^|]+) \|$",
            audit,
        )
        self.assertEqual(27, len(unit_rows))
        for row, target, missing, acceptance in unit_rows:
            with self.subTest(row=row):
                self.assertTrue(target.strip())
                self.assertTrue(missing.strip())
                self.assertTrue(acceptance.strip())

        e2e_section = audit.split("## Missing deployed E2E and environment tests", 1)[1]
        e2e_rows = re.findall(
            r"(?m)^\| (QA10-E2E0[1-7]) \| ([^|]+) \| ([^|]+) \|$",
            e2e_section,
        )
        self.assertEqual(7, len(e2e_rows))
        for row, destination, acceptance in e2e_rows:
            with self.subTest(row=row):
                self.assertTrue(destination.strip())
                self.assertTrue(acceptance.strip())

    def test_audit_names_each_current_a07_method_target(self) -> None:
        audit = (ROOT / "docs/quality/test-coverage-gap-audit.md").read_text(
            encoding="utf-8"
        )
        targets = (
            "AesGcmCredentialEnvelopeProtector.kt:38",
            "JpaAccountIdentityStore.kt:54",
            "JpaAccountIdentityStore.kt:74",
            "JpaAccountIdentityStore.kt:104",
            "JpaAccountIdentityStore.kt:145",
            "LoginVerificationService.kt:60",
            "SessionPolicy.kt:82",
            "TokenSessionService.kt:116",
        )
        for target in targets:
            with self.subTest(target=target):
                self.assertIn(target, audit)

    def test_audit_lists_every_current_gap_row_count_and_missing_e2e_operation(self) -> None:
        audit = (ROOT / "docs/quality/test-coverage-gap-audit.md").read_text(
            encoding="utf-8"
        )
        gaps = all_gaps(ROOT)
        row_counts = Counter(gap.qa_row for gap in gaps)
        for row, count in row_counts.items():
            with self.subTest(row=row):
                self.assertIn(f"| {row} | {count} |", audit)

        operations = inventory(ROOT)
        missing_e2e = [item.operation for item in operations if not item.has_e2e_signal]
        self.assertEqual(32, len(missing_e2e))
        matrix = audit.split("### Operation-specific E2E acceptance matrix", 1)[1].split(
            "The GraphQL roots currently have literal E2E references", 1
        )[0]
        for operation in (item.operation for item in operations):
            with self.subTest(operation=operation):
                self.assertRegex(
                    matrix,
                    rf"(?<![A-Za-z0-9_])`{re.escape(operation)}`(?![A-Za-z0-9_])",
                )

    def test_ci_documentation_uses_current_branch_baseline(self) -> None:
        ci = (ROOT / "docs/operations/ci.md").read_text(encoding="utf-8")

        self.assertIn("78 methods containing 144 missed", ci)
        self.assertNotIn("220 missed-branch methods", ci)

    def test_ci_python_tooling_is_locked_and_build_hooks_are_disabled(self) -> None:
        workflow = (ROOT / ".github/workflows/_reusable-ci.yml").read_text(
            encoding="utf-8"
        )
        self.assertIn(
            "astral-sh/setup-uv@d0cc045d04ccac9d8b7881df0226f9e82c39688e",
            workflow,
        )
        for line in workflow.splitlines():
            if "uv sync" in line or "uv run" in line:
                self.assertIn("--frozen", line)
                self.assertIn("--no-build", line)

    def test_ci_third_party_actions_are_immutable(self) -> None:
        workflow_paths = sorted((ROOT / ".github/workflows").glob("*.yml"))
        action_refs: list[str] = []
        for path in workflow_paths:
            action_refs.extend(
                re.findall(r"uses:\s*([^\s#]+)", path.read_text(encoding="utf-8"))
            )

        for ref in action_refs:
            owner = ref.split("/", 1)[0]
            if owner == "actions" or ref.startswith("./"):
                continue
            self.assertRegex(
                ref,
                r"^[^@]+@[0-9a-f]{40}$",
                msg=f"third-party action must use a full commit SHA: {ref}",
            )

    def test_change_audit_reaches_current_branch_tip(self) -> None:
        audit = (ROOT / "docs/quality/test-coverage-change-audit.md").read_text(
            encoding="utf-8"
        )

        for marker in (
            "through the latest audited coverage commit `0be4bbc`",
            "`2110ffd` is the explicit restoration/audit commit",
            "`3b3a3da` changes only `libs/security/build.gradle.kts`",
            "`cff7f76`\nchanges CI/Makefile Python execution",
            "`9b2fa85`",
            "`6ef0803`",
            "`52f270c`",
            "`bb35873`",
            "`8034208`",
            "`dbac7e7`",
            "`c4b8852`",
            "`69f2d27`",
            "`4b391a6`",
            "`cb02a39`",
            "`811bcdd`",
            "`2a532eb`",
            "`0e66e36`",
            "`7d8e316`",
            "`95390e8`",
            "`ce406b6`",
            "`7ee42a1`",
            "`0f79f86`",
            "`05c9417`",
            "`48fe0db`",
            "`0e267b7`",
            "`6b267bf`",
            "`fa41e93`",
            "`5007283`",
            "`01f5513`",
            "`9422d9a`",
            "`6f2d5f9`",
            "`4145a05`",
            "`eb045bb`",
            "`8b26630`",
            "`c5b924c`",
            "`9692227`",
            "`b60f458`",
            "`ff5fac3`",
            "`9b3e19e`",
            "`080a970`",
            "`cb41222`",
            "`47603a8`",
            "`53ad6ed`",
            "`3a98e20`",
            "`5e4a0a4`",
            "`a399b2a`",
            "`6adca2d`",
            "`13f6c30`",
            "`35c0242`",
            "`c4d3553`",
            "`dcdc2c2`",
            "`7a9689f`",
            "`433ebe2`",
            "`5592be7`",
            "`7d1fbce`",
            "`b84502b`",
            "`80c3f5d`",
            "`22a6be3`",
            "`dce224a`",
            "`bcb5c8a`",
            "`eb5b6f5`",
            "`efbc4b9`",
            "`7f184e4`",
            "`6a2027e`",
            "`a4c7f8f`",
            "`5fc7009`",
            "`bf15f07`",
            "`14eeb9c`",
            "`1974d8f`",
            "`d661fb8`",
            "with no production implementation\nor contract-file change",
        ):
            with self.subTest(marker=marker):
                self.assertIn(marker, audit)


if __name__ == "__main__":
    unittest.main()
