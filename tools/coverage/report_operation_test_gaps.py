"""Discover contract operations with no obvious E2E or Bruno source reference.

This is a source-inventory signal only. A string reference does not prove that
the operation's authorization, failure, persistence, messaging, and durable
side-effect assertions are present; the QA-10 acceptance rows remain the
required evidence standard.
"""

from __future__ import annotations

import argparse
import ast
import json
import re
import sys
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Iterable, Literal, Sequence

OutputFormat = Literal["json", "markdown"]


@dataclass(frozen=True)
class OperationEvidence:
    """Contract operation and source-reference discovery result."""

    surface: str
    operation: str
    service: str
    method: str | None
    path: str | None
    e2e_references: tuple[str, ...]
    e2e_callable_references: tuple[str, ...]
    bruno_references: tuple[str, ...]
    acceptance_row: str = "QA10-E2E01"

    @property
    def has_e2e_signal(self) -> bool:
        """Whether any live E2E source contains the operation name."""

        return bool(self.e2e_references)

    @property
    def has_bruno_signal(self) -> bool:
        """Whether any Bruno source contains the operation name."""

        return bool(self.bruno_references)

    @property
    def has_e2e_callable_signal(self) -> bool:
        """Whether a Python callable in an E2E source contains the operation reference."""

        return bool(self.e2e_callable_references)

    @property
    def e2e_callable_status(self) -> str:
        """Describe callable discovery without implying that the callable executed."""

        if self.has_e2e_callable_signal:
            return "CALLABLE-SOURCE-REFERENCE-ONLY"
        if self.has_e2e_signal:
            return "FILE-SOURCE-REFERENCE-ONLY"
        return "MISSING-SOURCE-SIGNAL"

    @property
    def e2e_status(self) -> str:
        """Describe source discovery without implying executed acceptance."""

        return "SOURCE-REFERENCE-ONLY" if self.has_e2e_signal else "MISSING-SOURCE-SIGNAL"


def acceptance_criteria(item: OperationEvidence) -> str:
    """Return the minimum per-operation deployed acceptance contract.

    Source references are only discovery signals. This mapping makes the
    required signed-persona, failure, replay/concurrency, side-effect, and
    isolation assertions reviewable for every operation in the inventory.
    """

    operation = item.operation.lower()
    if operation in {"startlogin", "verifylogin", "refreshtoken", "logout"}:
        return (
            "Signed credential/session fixture; exact success and rejection status; "
            "single-use/replay and family ownership; durable session/outbox state; "
            "redacted credential evidence."
        )
    if operation in {"getme", "getprofilebyid", "getprofilesbatch", "updateme"}:
        return (
            "Owner, foreign-subject, missing-profile, malformed, and workload cases; "
            "exact status/code; no cross-subject leakage; persisted profile state and "
            "query isolation asserted."
        )
    if operation in {"requestdeletion", "requestexport", "listexportrequests"}:
        return (
            "Signed owner and foreign-subject cases; exact accepted/rejected status; "
            "durable request ownership, idempotent/repeated transition, and no "
            "unauthorized row or event."
        )
    if operation in {"creategroup", "getgroup", "listgroups", "updategroup", "archivegroup", "listgroupmembers"}:
        return (
            "Owner/member/non-member/removed-member personas; exact allow/deny and "
            "object-hiding behavior; persisted group/revision/membership state; "
            "no forbidden side effect."
        )
    if operation in {"createinvite", "revokeinvite", "claiminvite", "createplaceholder", "removegroupmember"}:
        return (
            "Signed owner/member/outsider and concurrent/replayed personas; expiry, "
            "revocation, duplicate, and malformed inputs; exact status/code; durable "
            "membership/invite/notification state and atomic no-mutation failures."
        )
    if operation in {"createexpense", "updateexpense", "deleteexpense", "listexpenses", "searchexpenses", "exportexpenses", "previewallocation"}:
        return (
            "Authorized member and outsider cases; validation, stale-version, replay, "
            "pagination/filter, and malformed-input outcomes; exact status/code; "
            "expense, postings, revision, sync, outbox, and zero-sum side effects "
            "asserted with no cross-group leakage."
        )
    if operation in {"recordsettlement", "reversesettlement", "getsettlementsuggestions"}:
        return (
            "Authenticated participant and unauthorized/invalid cases; currency, "
            "ownership, replay, reversal, and corruption handling; exact status/code; "
            "settlement postings, balances, revisions, and zero-sum state asserted."
        )
    if operation in {"getsnapshot", "getchanges", "getbalances"}:
        return (
            "Authorized member and removed/non-member cases; cursor ownership, expiry, "
            "ordering, limits, tombstones, and malformed cursor outcomes; exact "
            "status/code and revision-consistent durable snapshot asserted."
        )
    if "schedule" in operation:
        return (
            "Authorized member and outsider cases; date/frequency/day, pause/resume, "
            "missing schedule, duplicate/lock, catch-up, and failure behavior; exact "
            "status/code plus durable schedule, expense, notification, and revision state."
        )
    if item.surface.startswith("GraphQL"):
        return (
            "Signed transport/persona case for the root field; exact GraphQL data/errors "
            "and public extensions; authorization/isolation, upstream failure, and "
            "required durable or subscription side effects asserted."
        )
    if item.service == "Notifications API":
        return (
            "Signed subject and foreign-subject cases; exact status/code and validation "
            "errors; durable inbox/preferences isolation, duplicate behavior, and "
            "notification side effects asserted."
        )
    return (
        "Signed owner/member/workload personas; exact success and rejection status/code; "
        "validation, replay/concurrency, durable side effects, and isolation asserted."
    )


def text_files(directory: Path, root: Path) -> list[tuple[str, str]]:
    """Read supported source files and return relative path/text pairs."""

    if not directory.exists():
        return []
    return [
        (str(path.relative_to(root)).replace("\\", "/"), path.read_text(encoding="utf-8"))
        for path in sorted(directory.rglob("*"))
        if path.is_file() and path.suffix in {".py", ".sh", ".bru"}
    ]


def references(operation: str, sources: Iterable[tuple[str, str]]) -> tuple[str, ...]:
    """Return source paths containing the operation as a standalone identifier.

    A substring search is too permissive for short operation names: for example,
    ``group`` would incorrectly match ``groups`` and ``groupId``.  Such false
    positives hide missing E2E evidence, so the inventory uses identifier
    boundaries while retaining support for operation names embedded in quoted
    GraphQL/REST request text.
    """

    pattern = re.compile(rf"(?<![A-Za-z0-9_]){re.escape(operation)}(?![A-Za-z0-9_])")
    return tuple(path for path, text in sources if pattern.search(text) is not None)


def callable_references(
    operation: str, sources: Iterable[tuple[str, str]]
) -> tuple[str, ...]:
    """Return Python callable locations containing a standalone operation reference.

    This is still static source evidence: it does not prove that a callable was
    discovered by a test runner, invoked, or asserted the required behavior.
    Non-Python E2E sources remain represented by the file-level reference scan.
    """

    pattern = re.compile(rf"(?<![A-Za-z0-9_]){re.escape(operation)}(?![A-Za-z0-9_])")
    matches: list[str] = []

    for path, text in sources:
        if not path.endswith(".py"):
            continue
        try:
            tree = ast.parse(text, filename=path)
        except SyntaxError:
            continue

        class CallableVisitor(ast.NodeVisitor):
            """Collect operation references from nested Python callables."""

            def __init__(self) -> None:
                self.names: list[str] = []
                self.stack: list[str] = []

            def visit_ClassDef(self, node: ast.ClassDef) -> None:
                self.stack.append(node.name)
                self.generic_visit(node)
                self.stack.pop()

            def visit_FunctionDef(self, node: ast.FunctionDef) -> None:
                self.stack.append(node.name)
                segment = ast.get_source_segment(text, node) or ""
                if pattern.search(segment) is not None:
                    self.names.append(f"{path}::{'/'.join(self.stack)}")
                self.generic_visit(node)
                self.stack.pop()

            def visit_AsyncFunctionDef(self, node: ast.AsyncFunctionDef) -> None:
                self.stack.append(node.name)
                segment = ast.get_source_segment(text, node) or ""
                if pattern.search(segment) is not None:
                    self.names.append(f"{path}::{'/'.join(self.stack)}")
                self.generic_visit(node)
                self.stack.pop()

        visitor = CallableVisitor()
        visitor.visit(tree)
        matches.extend(visitor.names)

    return tuple(matches)


def rest_operations(root: Path) -> list[OperationEvidence]:
    """Load REST operation IDs from the OpenAPI contracts."""

    operations: list[OperationEvidence] = []
    for contract in sorted((root / "contracts/rest").glob("*.openapi.json")):
        document = json.loads(contract.read_text(encoding="utf-8"))
        service = str(document["info"]["title"])
        for path, path_item in document["paths"].items():
            for method, operation in path_item.items():
                if method.lower() not in {"get", "post", "put", "patch", "delete"}:
                    continue
                operations.append(
                    OperationEvidence(
                        surface="REST",
                        operation=str(operation["operationId"]),
                        service=service,
                        method=method.upper(),
                        path=str(path),
                        e2e_references=(),
                        e2e_callable_references=(),
                        bruno_references=(),
                    )
                )
    return operations


def graphql_operations(root: Path) -> list[OperationEvidence]:
    """Load GraphQL root fields from the schema declaration."""

    source = (root / "contracts/graphql/10-roots.graphqls").read_text(encoding="utf-8")
    operations: list[OperationEvidence] = []
    for root_type, body in re.findall(r"type\s+(Query|Mutation|Subscription)\s*\{(.*?)\}", source, re.S):
        for field in re.findall(r"^  ([A-Za-z][A-Za-z0-9_]*)\s*(?:\(|:)", body, re.M):
            operations.append(
                OperationEvidence(
                    surface=f"GraphQL {root_type}",
                    operation=field,
                    service="BFF",
                    method=None,
                    path=None,
                    e2e_references=(),
                    e2e_callable_references=(),
                    bruno_references=(),
                )
            )
    return operations


def inventory(root: Path) -> list[OperationEvidence]:
    """Build stable REST and GraphQL source-reference evidence."""

    e2e = text_files(root / "tests/e2e", root)
    bruno = text_files(root / "tools/bruno", root)
    result: list[OperationEvidence] = []
    for operation in [*rest_operations(root), *graphql_operations(root)]:
        result.append(
            OperationEvidence(
                **{
                    **asdict(operation),
                        "e2e_references": references(operation.operation, e2e),
                    "e2e_callable_references": callable_references(operation.operation, e2e),
                    "bruno_references": references(operation.operation, bruno),
                }
            )
        )
    return sorted(result, key=lambda item: (item.surface, item.service, item.operation))


def render_markdown(items: Iterable[OperationEvidence]) -> str:
    """Render operation signals as a review table."""

    rows = [
        "| Surface | Service | Operation | Method | Path | Acceptance row | Required acceptance criteria | E2E evidence status | E2E callable signal | E2E file signal | Bruno signal |",
        "| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |",
    ]
    for item in items:
        e2e = ", ".join(f"`{path}`" for path in item.e2e_references) or "missing"
        callable_e2e = ", ".join(
            f"`{path}`" for path in item.e2e_callable_references
        ) or "missing"
        bruno = ", ".join(f"`{path}`" for path in item.bruno_references) or "missing"
        rows.append(
            f"| {item.surface} | {item.service} | `{item.operation}` | "
            f"{item.method or ''} | `{item.path or ''}` | `{item.acceptance_row}` | "
            f"{acceptance_criteria(item)} | **{item.e2e_callable_status}** | "
            f"{callable_e2e} | {e2e} | {bruno} |"
        )
    return "\n".join(rows)


def parse_args(argv: Sequence[str]) -> argparse.Namespace:
    """Parse operation inventory options."""

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path.cwd())
    parser.add_argument("--format", choices=("json", "markdown"), default="markdown")
    return parser.parse_args(argv)


def main(argv: Sequence[str] | None = None) -> int:
    """Emit the operation reference inventory."""

    options = parse_args(sys.argv[1:] if argv is None else argv)
    items = inventory(options.root.resolve())
    if options.format == "json":
        records: list[dict[str, object]] = [
            {
                **asdict(item),
                "e2e_status": item.e2e_status,
                "e2e_callable_status": item.e2e_callable_status,
            }
            for item in items
        ]
        print(json.dumps(records, indent=2))
    else:
        print(render_markdown(items))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
