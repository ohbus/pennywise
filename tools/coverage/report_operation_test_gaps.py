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
from typing import Iterable, Literal, Mapping, Sequence

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
    e2e_request_references: tuple[str, ...]
    bruno_references: tuple[str, ...]
    acceptance_row: str = "QA10-E2E01"
    execution_status: str = "NO-EXECUTION-ARTIFACT-INGESTED"

    @property
    def has_e2e_signal(self) -> bool:
        """Whether any live E2E source contains the operation name."""

        return bool(self.e2e_references)

    @property
    def has_bruno_signal(self) -> bool:
        """Whether any Bruno source contains the operation name."""

        return bool(self.bruno_references)

    @property
    def bruno_status(self) -> str:
        """Describe Bruno discovery without implying that a request ran."""

        return "BRUNO-SOURCE-REFERENCE-ONLY" if self.has_bruno_signal else "NO-BRUNO-SOURCE-REFERENCE"

    @property
    def has_e2e_callable_signal(self) -> bool:
        """Whether a Python callable in an E2E source contains the operation reference."""

        return bool(self.e2e_callable_references)

    @property
    def has_e2e_request_signal(self) -> bool:
        """Whether source contains a surface-shaped REST path or GraphQL field."""

        return bool(self.e2e_request_references)

    @property
    def e2e_request_status(self) -> str:
        """Describe request-shaped source discovery without implying execution."""

        return "REQUEST-SOURCE-REFERENCE-ONLY" if self.has_e2e_request_signal else "NO-REQUEST-SOURCE-SIGNAL"

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


def request_references(
    item: OperationEvidence, sources: Iterable[tuple[str, str]]
) -> tuple[str, ...]:
    """Return E2E files containing a surface-shaped request reference.

    REST matching normalizes path placeholders so ``{accountId}`` and
    ``{account_id}`` are equivalent. GraphQL matching requires the operation
    name to be followed by an argument list or selection set. This remains
    static evidence: it does not prove that a runner discovered, invoked, or
    asserted the request.
    """

    if item.surface == "REST" and item.path is not None:
        expected = re.sub(r"\{[^}]+\}", "{}", item.path)
        pattern = re.compile(re.escape(expected))
    elif item.surface.startswith("GraphQL"):
        pattern = re.compile(rf"(?<![A-Za-z0-9_]){re.escape(item.operation)}\s*(?:\(|\{{)")
    else:
        return ()
    return tuple(path for path, text in sources if pattern.search(re.sub(r"\{[A-Za-z0-9_]+\}", "{}", text)) is not None)


def bruno_references(
    item: OperationEvidence, sources: Iterable[tuple[str, str]]
) -> tuple[str, ...]:
    """Find Bruno requests using collection surface and contract path shape.

    A generic identifier scan is unsafe for Bruno because an Accounts request
    contains ``/me`` while a GraphQL operation is also named ``me``. Collection
    layout supplies the surface boundary, and REST rows additionally require
    the contract path to occur in the request text with template placeholders.
    """

    matches: list[str] = []
    for path, text in sources:
        normalized_path = path.replace("\\", "/")
        if item.surface.startswith("GraphQL"):
            if not normalized_path.startswith("tools/bruno/bff/"):
                continue
            pattern = re.compile(
                rf"(?<![A-Za-z0-9_]){re.escape(item.operation)}\s*(?:\(|\{{)"
            )
        elif item.surface == "REST":
            if normalized_path.startswith("tools/bruno/bff/") or item.path is None:
                continue
            url_paths = re.findall(r"(?m)^\s*url:\s*([^\s]+)", text)
            normalized_urls = [
                re.sub(r"\{\{[^}]+\}\}", "{}", url).strip('"\'')
                for url in url_paths
            ]
            method_match = re.search(
                r"(?mi)^\s*(get|post|put|patch|delete)\s*\{", text
            )
            method_matches = method_match is not None and item.method == method_match.group(1).upper()
            if method_matches and any(
                "/v1" in url and url.split("/v1", maxsplit=1)[1] == item.path
                for url in normalized_urls
            ):
                matches.append(path)
            continue
        else:
            continue
        if pattern.search(text) is not None:
            matches.append(path)
    return tuple(matches)


def _required_string(record: Mapping[str, object], field: str) -> str:
    """Read a required non-blank string from an execution artifact record."""

    value = record.get(field)
    if not isinstance(value, str) or not value.strip():
        raise ValueError(f"execution artifact field {field!r} must be a non-blank string")
    return value


def load_execution_artifact(path: Path) -> dict[tuple[str, str], str]:
    """Load strict per-operation execution evidence without inferring results.

    The artifact must identify the source revision, environment, exact surface
    and operation, a durable report path, and at least one named assertion.
    Source references alone never enter this mapping.
    """

    raw: object = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(raw, dict):
        raise ValueError("execution artifact root must be an object")
    document: Mapping[str, object] = raw
    if document.get("schema") != "qa10-operation-execution-v1":
        raise ValueError("unsupported QA-10 execution artifact schema")
    _required_string(document, "source_revision")
    _required_string(document, "environment")
    operations = document.get("operations")
    if not isinstance(operations, list):
        raise ValueError("execution artifact operations must be an array")

    results: dict[tuple[str, str], str] = {}
    for raw_record in operations:
        if not isinstance(raw_record, dict):
            raise ValueError("each execution operation record must be an object")
        record: Mapping[str, object] = raw_record
        surface = _required_string(record, "surface")
        if surface not in {"REST", "GraphQL Query", "GraphQL Mutation", "GraphQL Subscription"}:
            raise ValueError(f"unsupported execution surface {surface!r}")
        operation = _required_string(record, "operation")
        status = _required_string(record, "status")
        if status not in {"passed", "failed", "blocked"}:
            raise ValueError(f"unsupported execution status {status!r}")
        _required_string(record, "artifact")
        assertions = record.get("assertions")
        if (
            not isinstance(assertions, list)
            or not assertions
            or not all(isinstance(assertion, str) and assertion.strip() for assertion in assertions)
        ):
            raise ValueError("execution artifact assertions must contain at least one non-blank string")
        key = (surface, operation)
        if key in results:
            raise ValueError(f"duplicate execution artifact record for {surface}/{operation}")
        results[key] = f"EXECUTION-ARTIFACT-{status.upper()}"
    return results


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
                        e2e_request_references=(),
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
                    e2e_request_references=(),
                    bruno_references=(),
                )
            )
    return operations


def inventory(root: Path, execution_artifact: Path | None = None) -> list[OperationEvidence]:
    """Build stable REST and GraphQL source-reference evidence."""

    e2e = text_files(root / "tests/e2e", root)
    bruno = text_files(root / "tools/bruno", root)
    execution = load_execution_artifact(execution_artifact) if execution_artifact else {}
    result: list[OperationEvidence] = []
    for operation in [*rest_operations(root), *graphql_operations(root)]:
        key = (operation.surface, operation.operation)
        result.append(
            OperationEvidence(
                **{
                    **asdict(operation),
                        "e2e_references": references(operation.operation, e2e),
                    "e2e_callable_references": callable_references(operation.operation, e2e),
                    "e2e_request_references": request_references(
                        operation, e2e
                    ),
                    "bruno_references": bruno_references(operation, bruno),
                    "execution_status": execution.get(key, "NO-EXECUTION-ARTIFACT-INGESTED"),
                }
            )
        )
    unknown = set(execution) - {(item.surface, item.operation) for item in result}
    if unknown:
        raise ValueError(f"execution artifact contains unknown operations: {sorted(unknown)}")
    return sorted(result, key=lambda item: (item.surface, item.service, item.operation))


def render_markdown(items: Iterable[OperationEvidence]) -> str:
    """Render operation signals as a review table."""

    rows = [
        "| Surface | Service | Operation | Method | Path | Acceptance row | Required acceptance criteria | E2E source status | E2E request signal | E2E callable signal | E2E file signal | Bruno status | Execution artifact status |",
        "| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |",
    ]
    for item in items:
        e2e = ", ".join(f"`{path}`" for path in item.e2e_references) or "missing"
        callable_e2e = ", ".join(
            f"`{path}`" for path in item.e2e_callable_references
        ) or "missing"
        request_e2e = ", ".join(
            f"`{path}`" for path in item.e2e_request_references
        ) or "missing"
        bruno = ", ".join(f"`{path}`" for path in item.bruno_references) or "missing"
        rows.append(
            f"| {item.surface} | {item.service} | `{item.operation}` | "
            f"{item.method or ''} | `{item.path or ''}` | `{item.acceptance_row}` | "
            f"{acceptance_criteria(item)} | **{item.e2e_callable_status}** | "
            f"**{item.e2e_request_status}** ({request_e2e}) | {callable_e2e} | {e2e} | "
            f"**{item.bruno_status}** ({bruno}) | "
            f"**{item.execution_status}** |"
        )
    return "\n".join(rows)


def parse_args(argv: Sequence[str]) -> argparse.Namespace:
    """Parse operation inventory options."""

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path.cwd())
    parser.add_argument("--format", choices=("json", "markdown"), default="markdown")
    parser.add_argument("--execution-artifact", type=Path, default=None)
    return parser.parse_args(argv)


def main(argv: Sequence[str] | None = None) -> int:
    """Emit the operation reference inventory."""

    options = parse_args(sys.argv[1:] if argv is None else argv)
    items = inventory(
        options.root.resolve(),
        options.execution_artifact.resolve() if options.execution_artifact else None,
    )
    if options.format == "json":
        records: list[dict[str, object]] = [
            {
                **asdict(item),
                "e2e_status": item.e2e_status,
                "e2e_callable_status": item.e2e_callable_status,
                "e2e_request_status": item.e2e_request_status,
                "bruno_status": item.bruno_status,
                "execution_status": item.execution_status,
            }
            for item in items
        ]
        print(json.dumps(records, indent=2))
    else:
        print(render_markdown(items))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
