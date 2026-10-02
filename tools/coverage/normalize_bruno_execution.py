"""Normalize assertion-backed Bruno results into QA-10 execution evidence."""

from __future__ import annotations

import argparse
from dataclasses import dataclass
import json
from pathlib import Path
from typing import Any, Iterable, Mapping, Sequence

from tools.coverage.report_operation_test_gaps import OperationEvidence, inventory


@dataclass(frozen=True)
class BrunoRequest:
    """The small, non-secret subset of one Bruno result used for QA evidence."""

    path: str
    status: str
    checks: tuple[str, ...]


def _string(value: object) -> str:
    return value.strip() if isinstance(value, str) else ""


def _check_name(check: Mapping[str, object]) -> str:
    """Render a stable assertion/test label without copying response secrets."""

    for key in ("name", "description", "lhsExpr"):
        value = _string(check.get(key))
        if value:
            return value
    return "Bruno assertion/test executed"


def _checks(record: Mapping[str, object]) -> tuple[str, ...]:
    checks: list[str] = []
    for key in ("testResults", "assertionResults", "preRequestTestResults", "postResponseTestResults"):
        values = record.get(key)
        if isinstance(values, list):
            checks.extend(
                _check_name(value)
                for value in values
                if isinstance(value, dict)
            )
    return tuple(dict.fromkeys(check for check in checks if check))


def _request_records(value: object) -> Iterable[BrunoRequest]:
    """Extract request records from Bruno's iteration-array JSON report."""

    if isinstance(value, list):
        for item in value:
            yield from _request_records(item)
        return
    if not isinstance(value, dict):
        return
    path = _string(value.get("path"))
    if path:
        status = _string(value.get("status")) or ("failed" if value.get("error") else "passed")
        yield BrunoRequest(path=path, status=status, checks=_checks(value))
        return
    for child in value.values():
        yield from _request_records(child)


def _bruno_path(path: str) -> str:
    normalized = path.replace("\\", "/").lstrip("./")
    if normalized.startswith("tools/bruno/"):
        return normalized if normalized.endswith(".bru") else f"{normalized}.bru"
    normalized = normalized.removesuffix(".bru")
    return f"tools/bruno/{normalized}.bru"


def _expected_surface(path: str) -> str | None:
    """Use collection layout as a safety check for REST/GraphQL collisions."""

    normalized = path.replace("\\", "/").lstrip("./")
    if normalized.startswith("bff/"):
        return "GraphQL"
    if normalized.startswith(("accounts/", "expense-core/", "notifications/", "quality/")):
        return "REST"
    return None


def _operation_sources(root: Path) -> dict[str, tuple[OperationEvidence, ...]]:
    sources: dict[str, list[OperationEvidence]] = {}
    for operation in inventory(root):
        for reference in operation.bruno_references:
            sources.setdefault(reference, []).append(operation)
    return {reference: tuple(items) for reference, items in sources.items()}


def normalize(
    root: Path,
    report_path: Path,
    source_revision: str,
    environment: str,
    artifact_path: str,
) -> dict[str, object]:
    """Build strict QA-10 evidence from unique Bruno request mappings.

    A Bruno request is omitted when its path maps to no contract operation or
    to multiple operations. This deliberate false negative prevents a shared
    fixture, such as logout/refresh, from crediting the wrong operation.
    """

    raw: object = json.loads(report_path.read_text(encoding="utf-8"))
    source_map = _operation_sources(root)
    records: list[dict[str, object]] = []
    seen: set[tuple[str, str]] = set()
    for request in _request_records(raw):
        operations = source_map.get(_bruno_path(request.path), ())
        expected_surface = _expected_surface(request.path)
        if expected_surface is not None:
            operations = tuple(
                operation
                for operation in operations
                if operation.surface.startswith(expected_surface)
            )
        if len(operations) != 1:
            continue
        operation = operations[0]
        key = (operation.surface, operation.operation)
        if key in seen:
            continue
        seen.add(key)
        checks = request.checks or ("Bruno request completed without assertion-backed checks",)
        status = request.status.lower()
        if status in {"pass", "passed", "success", "completed"}:
            result = "passed" if request.checks else "blocked"
        elif status in {"skip", "skipped", "blocked"}:
            result = "blocked"
        else:
            result = "failed"
        records.append(
            {
                "surface": operation.surface,
                "operation": operation.operation,
                "status": result,
                "artifact": artifact_path,
                "assertions": list(checks),
            }
        )
    if not records:
        raise ValueError("Bruno report produced no unique contract-operation evidence")
    return {
        "schema": "qa10-operation-execution-v1",
        "source_revision": source_revision,
        "environment": environment,
        "operations": records,
    }


def main(argv: Sequence[str] | None = None) -> int:
    """Normalize a Bruno JSON report and write a versioned QA-10 artifact."""

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--report", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--source-revision", required=True)
    parser.add_argument("--environment", required=True)
    args = parser.parse_args(argv)
    document = normalize(
        Path.cwd(),
        args.report,
        args.source_revision,
        args.environment,
        args.report.as_posix(),
    )
    operations = document["operations"]
    if not isinstance(operations, list):
        raise TypeError("normalized operation evidence must be a list")
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(document, indent=2) + "\n", encoding="utf-8")
    print(f"normalized Bruno execution evidence: {len(operations)} operations")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
