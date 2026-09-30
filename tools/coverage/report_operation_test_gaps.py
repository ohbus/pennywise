"""Discover contract operations with no obvious E2E or Bruno source reference.

This is a source-inventory signal only. A string reference does not prove that
the operation's authorization, failure, persistence, messaging, and durable
side-effect assertions are present; the QA-10 acceptance rows remain the
required evidence standard.
"""

from __future__ import annotations

import argparse
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
    """Return source paths containing the exact operation token."""

    return tuple(path for path, text in sources if operation in text)


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
                    "bruno_references": references(operation.operation, bruno),
                }
            )
        )
    return sorted(result, key=lambda item: (item.surface, item.service, item.operation))


def render_markdown(items: Iterable[OperationEvidence]) -> str:
    """Render operation signals as a review table."""

    rows = [
        "| Surface | Service | Operation | Method | Path | Acceptance row | E2E signal | Bruno signal |",
        "| --- | --- | --- | --- | --- | --- | --- | --- |",
    ]
    for item in items:
        e2e = ", ".join(f"`{path}`" for path in item.e2e_references) or "missing"
        bruno = ", ".join(f"`{path}`" for path in item.bruno_references) or "missing"
        rows.append(
            f"| {item.surface} | {item.service} | `{item.operation}` | "
            f"{item.method or ''} | `{item.path or ''}` | `{item.acceptance_row}` | "
            f"{e2e} | {bruno} |"
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
        print(json.dumps([asdict(item) for item in items], indent=2))
    else:
        print(render_markdown(items))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
