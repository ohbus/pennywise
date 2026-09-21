"""Validate the repository's centralized dependency-version SBOM baseline."""

from __future__ import annotations

from pathlib import Path
import re
import sys


ROOT = Path(__file__).resolve().parents[2]
VERSION_PATTERN = re.compile(r"\b\d+\.\d+(?:\.\d+)?(?:[-+][\w.]+)?\b")


def main() -> int:
    """Ensure dependency declarations use the version catalog and catalog exists."""
    catalog = ROOT / "gradle" / "libs.versions.toml"
    if not catalog.is_file():
        print("missing centralized Gradle version catalog")
        return 1
    findings: list[str] = []
    for path in sorted((ROOT / "app").rglob("build.gradle.kts")) + sorted(
        (ROOT / "libs").rglob("build.gradle.kts")
    ):
        for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
            if "implementation(" in line and VERSION_PATTERN.search(line):
                findings.append(f"{path.relative_to(ROOT)}:{number}")
    if findings:
        print("dependency versions must be centralized in gradle/libs.versions.toml:")
        print("\n".join(findings))
        return 1
    print("SBOM dependency baseline validation passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
