"""Enumerate every production method with missed JaCoCo branches.

The report is intentionally a discovery artifact, not a pass/fail coverage
threshold. A QA-10 row must assign each emitted method to a behavior test,
environment test, or reviewed structural classification.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
import xml.etree.ElementTree as ET
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Iterable, Literal, Sequence

OutputFormat = Literal["json", "markdown"]


@dataclass(frozen=True)
class BranchGap:
    """One production method whose JaCoCo branch counter is not complete."""

    module: str
    class_name: str
    source_file: str
    method: str
    source_line: int | None
    missed_branches: int
    covered_branches: int
    report: str
    qa_row: str
    assignment_basis: str
    acceptance_criteria: str
    closure_status: str
    next_action: str


def qa_assignment(module: str, class_name: str) -> tuple[str, str]:
    """Assign a discovered method to the narrowest current QA-10 row.

    The assignment is deliberately provisional: it guarantees that every
    discovery record has an accountable acceptance row, while the reviewer
    still verifies the class-level classification before closure.
    """

    qualified = class_name.lower()
    patterns: tuple[tuple[str, str, str], ...]
    if module == "app/accounts":
        patterns = (
            ("emailaddress", "QA10-A08", "email canonicalization value object"),
            ("authemailoutboxpublisher", "QA10-A01", "auth email publication"),
            ("outboxauthemailsender|authemailoutboxservice", "QA10-A02", "auth email outbox sender"),
            ("ratelimit|abuse", "QA10-A03", "authentication abuse and rate limiting"),
            ("externaloidctokenprovider|authsessionconfiguration", "QA10-A04", "external identity-provider path"),
            ("productionsecurityconfig|oidcsubject|jwtdecoder", "QA10-A05", "resource-server security wiring"),
            ("profilecontroller|profile.store|jpaprofilestore", "QA10-A06", "profile boundary and authorization"),
        )
        default = ("QA10-A07", "authentication session and identity behavior")
    elif module == "app/bff":
        patterns = (
            ("gateway|restgateway", "QA10-B01", "upstream transport gateway"),
            ("exceptionresolver|limiterror|scalar", "QA10-B02", "GraphQL error and limit boundary"),
            ("fanout|eventconsumer|rabbit.*listener", "QA10-B03", "realtime fanout and broker consumer"),
            ("browserorigin|csrf|cookie|session", "QA10-B04", "browser security filter chain"),
        )
        default = ("QA10-B02", "GraphQL transport and resolver behavior")
    elif module == "app/expense-core":
        patterns = (
            ("jpaexpensestore|expensecontroller", "QA10-C01", "expense persistence and transport"),
            ("validator|allocationcalculator|financialarithmetic", "QA10-C02", "financial arithmetic and validation"),
            ("recurring", "QA10-C03", "recurring expense lifecycle"),
            ("jpagroupstore|invite|member", "QA10-C04", "group and membership lifecycle"),
            ("settlement", "QA10-C05", "settlement and balance invariants"),
            ("synchronization|sync", "QA10-C06", "synchronization and cursor behavior"),
        )
        default = ("QA10-C01", "Expense Core persistence and financial behavior")
    elif module == "app/notifications":
        patterns = (
            ("authemail|envelope", "QA10-D01", "authentication email consumer"),
            ("notificationevent|rabbitnotification|broker", "QA10-D02", "notification event processing"),
            ("smtp|mail|emaildispatcher", "QA10-D03", "mail delivery adapter"),
            ("inbox|preference", "QA10-D04", "notification inbox and preferences"),
        )
        default = ("QA10-D02", "notification event delivery")
    elif module == "libs/errors":
        patterns = ()
        default = ("QA10-E01", "error and request-correlation boundary")
    elif module == "libs/db":
        patterns = ()
        default = ("QA10-E02", "database routing and operational infrastructure")
    elif module == "libs/security":
        patterns = ()
        default = ("QA10-E03", "OIDC decoder and security infrastructure")
    elif module == "libs/ids":
        patterns = ()
        default = ("QA10-E04", "IDs, constants, and static contract invariants")
    else:
        patterns = ()
        default = ("QA10-E05", "observability and cross-cutting infrastructure")

    for expression, row, basis in patterns:
        if re.search(expression, qualified):
            return row, basis
    return default


def qa_acceptance(qa_row: str) -> str:
    """Return the minimum evidence required for an assigned QA-10 row."""

    criteria = {
        "QA10-A01": "Auth-email publication, acknowledgement/retry state, redaction, and deployed broker delivery.",
        "QA10-A02": "Protected outbox handoff, caller-transaction rollback, replay safety, and durable delivery state.",
        "QA10-A03": "Normalized identity limits, atomic shared-Redis behavior, outage recovery, and public 429 semantics.",
        "QA10-A04": "Explicit external OIDC selection, exchange, failure mapping, and no fallback authority.",
        "QA10-A05": "Issuer, audience, algorithm, key discovery/rotation, invalid-token rejection, and fail-closed wiring.",
        "QA10-A06": "Subject-scoped profile/deletion/export authorization, exact errors, durable state, and redaction.",
        "QA10-A07": "Identity/session/replay/expiry/deletion invariants, durable transitions, concurrency, and redacted outcomes.",
        "QA10-A08": "Canonical email normalization plus malformed, length, whitespace, IDN, and label boundaries.",
        "QA10-B01": "Bearer/watermark propagation, transport failure mapping, redaction, and deployed gateway behavior.",
        "QA10-B02": "GraphQL resolver/scalar/error/limit behavior, admission validation, and exact public extensions.",
        "QA10-B03": "Fanout ordering, deduplication, expiry/revocation, broker ack/retry, reconnect, and WebSocket isolation.",
        "QA10-B04": "Origin/CSRF/cookie/session policy for browser, native, preflight, and WebSocket paths.",
        "QA10-C01": "Financial validation, idempotency, persistence, postings, revisions, side effects, rollback, and isolation.",
        "QA10-C02": "Arithmetic conservation, deterministic allocation, overflow/invalid-input rejection, and zero-sum invariants.",
        "QA10-C03": "Recurring date/catch-up/locking/membership/duplicate/failure behavior and notification/outbox effects.",
        "QA10-C04": "Group/invite/member lifecycle, expiry/replay/races, archive/removal behavior, revisions, and side effects.",
        "QA10-C05": "Settlement/reversal validation, idempotency, balances, corruption detection, concurrency, and zero-sum state.",
        "QA10-C06": "Sync ordering, cursor ownership/expiry/limits, tombstones, membership loss, and revision invariants.",
        "QA10-D01": "Auth-email envelope validation, decrypt/expiry handling, ack/retry/DLQ, deduplication, and redaction.",
        "QA10-D02": "Notification transaction/ack coupling, deduplication, poison/retry behavior, Redis admission, and inbox effects.",
        "QA10-D03": "SMTP mapping, validation, failure classification, retry/parking, metrics, and redaction.",
        "QA10-D04": "Subject-isolated preferences/inbox behavior, cursors/versioning, duplicate mark-read, and database failures.",
        "QA10-E01": "Every catalog/framework error envelope, correlation lifecycle, negotiation, headers, and redaction.",
        "QA10-E02": "Writer/reader routing, lag/fallback/recovery, causal watermarks, pool bounds, and safe write routing.",
        "QA10-E03": "Servlet/reactive OIDC parity, issuer/audience/algorithm/time/key rotation, and fail-closed headers.",
        "QA10-E04": "Endpoint/event/error/ID uniqueness and contract drift, with explicit generated-code classification.",
        "QA10-E05": "Bounded observability labels and exact success/failure/slow/fallback metric behavior.",
    }
    return criteria.get(qa_row, "Exact behavior test or reviewed structural rationale linked to this production branch.")


def closure_review(class_name: str, method: str) -> tuple[str, str]:
    """Assign an explicit review status and next action to a residual branch.

    These statuses never close a branch automatically. They make the residual
    inventory auditable: structural candidates still require reviewer sign-off,
    while reachable or unreferenced behavior remains an open test/design item.
    """

    qualified = class_name.lower()
    if qualified.endswith("profilecontroller") and method == "mapErrorCode":
        return (
            "OPEN-DESIGN",
            "Route a real controller failure through problem() and assert its public envelope, or record a separately reviewed cleanup decision; do not use reflection-only coverage.",
        )
    if qualified.endswith("recurringexpenseservice") and method == "emitSchedulePausedNotification":
        return (
            "OPEN-BEHAVIOR",
            "Add a focused service test proving the optional outbox absence is a safe no-op while configured outbox delivery remains asserted.",
        )
    if "jpagroupstore" in qualified:
        return (
            "CANDIDATE-STRUCTURAL",
            "Reconfirm foreign-key/membership invariants and retain lifecycle tests; classify only the exact defensive mapping after reviewer sign-off.",
        )
    if "expensecontroller" in qualified:
        return (
            "CANDIDATE-STRUCTURAL",
            "Retain the public ensureActiveMember rejection tests; the nullable principal forwarding arm is unreachable after authentication and must not be exercised by bypassing the guard.",
        )
    if "clientaddressresolver" in qualified:
        return (
            "CANDIDATE-STRUCTURAL",
            "Retain IPv4/IPv6/malformed boundary tests and classify only the unsupported-address-family defensive mapping under the JDK family invariant.",
        )
    if "emailaddress" in qualified:
        return (
            "CANDIDATE-STRUCTURAL",
            "Retain canonicalization and malformed/length/IDN tests; review the residual parser short-circuit mapping without weakening validation.",
        )
    if "loginverificationservice" in qualified:
        return (
            "CANDIDATE-STRUCTURAL",
            "Retain canonical-email enrollment/reuse tests; the fallback display name is unreachable after EmailAddress local-part validation.",
        )
    if "sessionpolicy" in qualified:
        return (
            "CANDIDATE-STRUCTURAL",
            "Retain idle, absolute, and skew boundary tests; classify only the unreachable short-circuit under SessionExpiry ordering.",
        )
    if "fallbackjwtdecoder" in qualified:
        return (
            "CANDIDATE-STRUCTURAL",
            "Retain first-success, later-success, and final-failure tests; the terminal failure guard follows the non-empty decoder-list invariant.",
        )
    if "browseroriginpolicy" in qualified or "bffgatewayfilters" in qualified:
        return (
            "CANDIDATE-STRUCTURAL",
            "Retain origin/watermark boundary tests and classify only the Kotlin collection or short-circuit mapping after source/bytecode review.",
        )
    if "liveupdatefanout" in qualified:
        return (
            "CANDIDATE-STRUCTURAL",
            "Retain revocation, expiry, and membership tests; classify generated predicate/iterator mappings separately from the still-open broker/WebSocket E2E evidence.",
        )
    if "recurrencedomain" in qualified or "recurrenceschedule" in qualified:
        return (
            "CANDIDATE-STRUCTURAL",
            "Retain constructor/date/frequency boundary tests and review only compiler-generated validation short-circuits.",
        )
    if "recurringexpenseservice" in qualified:
        return (
            "CANDIDATE-STRUCTURAL",
            "Retain date, catch-up, membership, custom participant, duplicate, and failure tests; classify only impossible empty-member/fallback mappings after invariant review.",
        )
    if "searchcontroller" in qualified or "expensesearch" in qualified:
        return (
            "CANDIDATE-STRUCTURAL",
            "Retain authorization/filter/pagination and populated/header-only CSV tests; classify only generated telemetry/iteration mappings.",
        )
    if "settlementsuggestionengine" in qualified:
        return (
            "CANDIDATE-STRUCTURAL",
            "Retain zero-sum, duplicate-row, one-sided, and multi-currency tests; classify only the strictly-positive transfer guard under queue invariants.",
        )
    if "synccontroller" in qualified:
        return (
            "CANDIDATE-STRUCTURAL",
            "Retain malformed, cross-group, expired, and blank-cursor public tests; classify only the non-null exception-message fallback.",
        )
    if "emaildispatcher" in qualified:
        return (
            "CANDIDATE-STRUCTURAL",
            "Retain first/subsequent success, retry exhaustion, permanent failure, interruption, and zero-attempt tests; classify only the defensive loop-exit mapping.",
        )
    if "dbreaderhealth" in qualified or "dboperationpolicy" in qualified:
        return (
            "CANDIDATE-STRUCTURAL",
            "Retain timing, routing, and all policy invariant tests; review the residual nullability/redundant short-circuit mapping without removing a guard.",
        )
    if "globalerrorhandler" in qualified:
        return (
            "CANDIDATE-STRUCTURAL",
            "Retain exhaustive catalog and invalid-status fallback tests; the remaining arms are defensive against impossible enum/status combinations.",
        )
    return (
        "OPEN-REVIEW",
        "Inspect the exact source/bytecode mapping and add a behavior test or a reviewed invariant classification; do not alter implementation for JaCoCo.",
    )


def report_paths(root: Path) -> list[Path]:
    """Return application and library JaCoCo XML reports in stable order."""

    candidates = [
        path
        for parent in (root / "app", root / "libs")
        if parent.exists()
        for path in parent.glob("*/build/reports/jacoco/test/jacocoTestReport.xml")
    ]
    return sorted(candidates)


def module_name(root: Path, report: Path) -> str:
    """Derive the repository module name from a report path."""

    relative_parts = report.relative_to(root).parts
    return "/".join(relative_parts[:2])


def counter(method: ET.Element) -> ET.Element | None:
    """Return a method's branch counter, if JaCoCo emitted one."""

    return next(
        (item for item in method.findall("counter") if item.get("type") == "BRANCH"),
        None,
    )


def parse_report(root: Path, report: Path) -> list[BranchGap]:
    """Parse behavioral branch gaps from one JaCoCo XML report."""

    document = ET.parse(report)
    gaps: list[BranchGap] = []
    for package in document.findall("./package"):
        for clazz in package.findall("./class"):
            class_name = clazz.get("name", "")
            source_file = clazz.get("sourcefilename", "")
            for method in clazz.findall("./method"):
                branch_counter = counter(method)
                missed = int(branch_counter.get("missed", "0")) if branch_counter is not None else 0
                if missed == 0:
                    continue
                covered = int(branch_counter.get("covered", "0")) if branch_counter is not None else 0
                line_text = method.get("line")
                qa_row, assignment_basis = qa_assignment(module_name(root, report), class_name)
                closure_status, next_action = closure_review(
                    class_name, method.get("name", "")
                )
                gaps.append(
                    BranchGap(
                        module=module_name(root, report),
                        class_name=class_name,
                        source_file=source_file,
                        method=method.get("name", ""),
                        source_line=int(line_text) if line_text is not None else None,
                        missed_branches=missed,
                        covered_branches=covered,
                        report=str(report.relative_to(root)).replace("\\", "/"),
                        qa_row=qa_row,
                        assignment_basis=assignment_basis,
                        acceptance_criteria=qa_acceptance(qa_row),
                        closure_status=closure_status,
                        next_action=next_action,
                    )
                )
    return gaps


def all_gaps(root: Path) -> list[BranchGap]:
    """Load and stably sort all current module branch gaps."""

    gaps = [gap for report in report_paths(root) for gap in parse_report(root, report)]
    return sorted(
        gaps,
        key=lambda gap: (
            gap.module,
            gap.class_name,
            gap.source_line if gap.source_line is not None else -1,
            gap.method,
        ),
    )


def markdown(gaps: Iterable[BranchGap]) -> str:
    """Render branch gaps as a review-friendly Markdown table."""

    rows = [
        "| Module | QA row | Production class | Source | Method | Line | Missed | Covered | Assignment | Report | Acceptance criteria | Closure status | Next action |",
        "| --- | --- | --- | --- | --- | ---: | ---: | ---: | --- | --- | --- | --- | --- |",
    ]
    for gap in gaps:
        rows.append(
            f"| `{gap.module}` | `{gap.qa_row}` | `{gap.class_name}` | `{gap.source_file}` | "
            f"`{gap.method}` | {gap.source_line or ''} | {gap.missed_branches} | "
            f"{gap.covered_branches} | {gap.assignment_basis} | `{gap.report}` | "
            f"{gap.acceptance_criteria} | **{gap.closure_status}** | {gap.next_action} |"
        )
    return "\n".join(rows)


def arguments(argv: Sequence[str]) -> argparse.Namespace:
    """Parse command-line options for the inventory command."""

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path.cwd())
    parser.add_argument("--format", choices=("json", "markdown"), default="markdown")
    parser.add_argument(
        "--fail-on-gaps",
        action="store_true",
        help="return non-zero when any missed branch remains",
    )
    return parser.parse_args(argv)


def main(argv: Sequence[str] | None = None) -> int:
    """Emit the current branch-gap inventory and return a process status."""

    options = arguments(sys.argv[1:] if argv is None else argv)
    gaps = all_gaps(options.root.resolve())
    if options.format == "json":
        print(json.dumps([asdict(gap) for gap in gaps], indent=2))
    else:
        print(markdown(gaps))
    return 1 if options.fail_on_gaps and gaps else 0


if __name__ == "__main__":
    raise SystemExit(main())
