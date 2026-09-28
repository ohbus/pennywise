"""Validate dependency boundaries that can be checked without build plugins."""

from __future__ import annotations

from pathlib import Path
import re
import sys


ROOT = Path(__file__).resolve().parents[2]
APP_PACKAGES = {
    "accounts": "com.subhrodip.pennywise.accounts",
    "expense-core": "com.subhrodip.pennywise.expensecore",
    "notifications": "com.subhrodip.pennywise.notifications",
    "bff": "com.subhrodip.pennywise.bff",
}
IMPORT_PATTERN = re.compile(r"^import\s+(com\.subhrodip\.pennywise\.[\w.]+)")


def source_files(app: str) -> list[Path]:
    """Return Kotlin production files belonging to an application."""
    return sorted((ROOT / "app" / app / "src" / "main").rglob("*.kt"))


def violations() -> list[str]:
    """Find production imports crossing independently deployable app boundaries."""
    findings: list[str] = []
    for app, own_package in APP_PACKAGES.items():
        for path in source_files(app):
            for line_number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
                match = IMPORT_PATTERN.match(line.strip())
                if match is None:
                    continue
                imported = match.group(1)
                if any(
                    imported.startswith(other_package)
                    for other_app, other_package in APP_PACKAGES.items()
                    if other_app != app
                ):
                    findings.append(f"{path.relative_to(ROOT)}:{line_number}: {imported}")
    return findings


def main() -> int:
    """Run the architecture boundary gate."""
    findings = violations()
    if findings:
        print("forbidden cross-service imports:")
        print("\n".join(findings))
        return 1
    print("architecture boundary validation passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
