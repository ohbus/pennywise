"""Enumerate concrete production methods with no executed JaCoCo instructions."""

from __future__ import annotations

import argparse
import json
import re
import sys
import xml.etree.ElementTree as ET
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Iterable, Sequence

from tools.coverage.report_branch_gaps import (
    module_name,
    qa_acceptance,
    qa_assignment,
)


@dataclass(frozen=True)
class ExecutionGap:
    """Concrete production method with zero covered instructions."""

    module: str
    class_name: str
    source_file: str
    method: str
    source_line: int | None
    missed_instructions: int
    qa_row: str
    acceptance_criteria: str
    status: str
    next_action: str
    report: str


def generated_method(method: str) -> bool:
    """Exclude compiler-generated accessors and dispatch scaffolding."""

    return (
        method in {"<init>", "<clinit>", "main", "invokeSuspend"}
        or method.startswith(("get", "set", "component", "copy"))
        or method in {"equals", "hashCode", "toString"}
        or "$default" in method
        or "$lambda" in method
        or "access$" in method
    )


def source_path(root: Path, report: Path, package: str, source_file: str) -> Path | None:
    """Resolve a JaCoCo source file to the production source tree."""

    module_root = root / module_name(root, report)
    package_path = Path(package)
    for source_root in ("main/kotlin", "main/java"):
        candidate = module_root / "src" / source_root / package_path / source_file
        if candidate.exists():
            return candidate
    return None


def interface_source(root: Path, report: Path, package: str, source_file: str, class_name: str) -> bool:
    """Return whether the owning source declaration is an interface."""

    path = source_path(root, report, package, source_file)
    if path is None:
        return False
    simple_name = class_name.rsplit("/", maxsplit=1)[-1].split("$", maxsplit=1)[0]
    source = path.read_text(encoding="utf-8")
    return re.search(rf"\binterface\s+{re.escape(simple_name)}\b", source) is not None


def inline_source(
    root: Path,
    report: Path,
    package: str,
    source_file: str,
    method: str,
    source_line: int | None,
) -> bool:
    """Return whether a zero-execution method is a Kotlin inline declaration."""

    if source_line is None:
        return False
    path = source_path(root, report, package, source_file)
    if path is None:
        return False
    lines = path.read_text(encoding="utf-8").splitlines()
    declaration = " ".join(lines[max(0, source_line - 4) : source_line + 1])
    return re.search(
        rf"\binline\s+fun\b.*\b{re.escape(method)}\s*\(", declaration
    ) is not None


def next_action(class_name: str, method: str, inline: bool) -> str:
    """Describe the exact closure decision for a concrete execution gap."""

    if class_name.endswith("ProfileController") and method in {"problem", "mapErrorCode"}:
        return (
            "ProfileControllerTest covers the public profile/deletion/export problem behavior, "
            "but source search found no production caller for this private helper. Keep the "
            "helper as an OPEN-DESIGN item until a real public failure is routed through it "
            "or a separately reviewed cleanup decision is made; do not use reflection-only "
            "coverage or delete it to change JaCoCo."
        )
    if inline:
        return (
            "Retain direct behavior tests at call sites; Kotlin inline expansion does not execute "
            "this JaCoCo method node, so do not add reflection-only coverage or change the contract."
        )
    return (
        "Add a direct unit/integration test for the public behavior, or document why the method is "
        "framework/bootstrap wiring; do not delete it or bypass its contract for coverage."
    )


def all_gaps(root: Path) -> list[ExecutionGap]:
    """Load concrete methods with no covered instructions."""

    gaps: list[ExecutionGap] = []
    reports = [
        path
        for parent in (root / "app", root / "libs")
        if parent.exists()
        for path in parent.glob("*/build/reports/jacoco/test/jacocoTestReport.xml")
    ]
    for report in sorted(reports):
        document = ET.parse(report)
        for package_node in document.findall("./package"):
            package = package_node.get("name", "")
            for clazz in package_node.findall("./class"):
                class_name = clazz.get("name", "")
                source_file = clazz.get("sourcefilename", "")
                if interface_source(root, report, package, source_file, class_name):
                    continue
                for method_node in clazz.findall("./method"):
                    method = method_node.get("name", "")
                    instruction = next(
                        (
                            counter
                            for counter in method_node.findall("counter")
                            if counter.get("type") == "INSTRUCTION"
                        ),
                        None,
                    )
                    if (
                        instruction is None
                        or instruction.get("covered") != "0"
                        or generated_method(method)
                    ):
                        continue
                    line = method_node.get("line")
                    source_line = int(line) if line is not None else None
                    inline = inline_source(
                        root,
                        report,
                        package,
                        source_file,
                        method,
                        source_line,
                    )
                    qa_row, _ = qa_assignment(module_name(root, report), class_name)
                    gaps.append(
                        ExecutionGap(
                            module=module_name(root, report),
                            class_name=class_name,
                            source_file=source_file,
                            method=method,
                            source_line=source_line,
                            missed_instructions=int(instruction.get("missed", "0")),
                            qa_row=qa_row,
                            acceptance_criteria=qa_acceptance(qa_row),
                            status=("INLINE-EXPANDED" if inline else "NO-INSTRUCTION-EXECUTION"),
                            next_action=next_action(class_name, method, inline),
                            report=str(report.relative_to(root)).replace("\\", "/"),
                        )
                    )
    return sorted(
        gaps,
        key=lambda gap: (gap.module, gap.class_name, gap.source_line or -1, gap.method),
    )


def markdown(gaps: Iterable[ExecutionGap]) -> str:
    """Render execution gaps as an acceptance ledger."""

    rows = [
        "| Module | Production class | Source | Method | Line | Missed instructions | QA row | Acceptance criteria | Status | Next action | Report |",
        "| --- | --- | --- | --- | ---: | ---: | --- | --- | --- | --- | --- |",
    ]
    for gap in gaps:
        rows.append(
            f"| `{gap.module}` | `{gap.class_name}` | `{gap.source_file}` | `{gap.method}` | "
            f"{gap.source_line or ''} | {gap.missed_instructions} | `{gap.qa_row}` | "
            f"{gap.acceptance_criteria} | **{gap.status}** | {gap.next_action} | `{gap.report}` |"
        )
    return "\n".join(rows)


def arguments(argv: Sequence[str]) -> argparse.Namespace:
    """Parse execution-gap output options."""

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path.cwd())
    parser.add_argument("--format", choices=("json", "markdown"), default="markdown")
    return parser.parse_args(argv)


def main(argv: Sequence[str] | None = None) -> int:
    """Emit the current concrete zero-execution method inventory."""

    options = arguments(sys.argv[1:] if argv is None else argv)
    gaps = all_gaps(options.root.resolve())
    if options.format == "json":
        print(json.dumps([asdict(gap) for gap in gaps], indent=2))
    else:
        print(markdown(gaps))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
