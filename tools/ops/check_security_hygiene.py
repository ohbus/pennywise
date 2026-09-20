#!/usr/bin/env python3
"""Reject obvious credentials in tracked source and configuration files."""
from __future__ import annotations
import pathlib, re, subprocess, sys
ROOT = pathlib.Path(__file__).resolve().parents[2]
SECRET = re.compile(r"(?i)(password|secret|token|private[_-]?key)\s*[:=]\s*['\"][^'\"]{12,}")
def main() -> int:
    files = subprocess.check_output(["git", "ls-files"], cwd=ROOT, text=True).splitlines()
    findings = []
    for name in files:
        path = ROOT / name
        if path.suffix not in {".yml", ".yaml", ".json", ".properties", ".env", ".md", ".kt", ".kts", ".py"}: continue
        try: lines = path.read_text(encoding="utf-8").splitlines()
        except UnicodeDecodeError: continue
        for number, line in enumerate(lines, 1):
            if (
                "local-only" in line
                or "example" in str(path)
                or "${" in line
                or "$(" in line
                or "changeme" in line.lower()
                or ("not-a-real-" in line and "tests" in name)
                or ("tests/e2e" in name and ("-user" in line or "$" in line))
                or (".github/workflows" in name and "$" in line)
            ):
                continue
            if SECRET.search(line): findings.append(f"{name}:{number}")
    if findings:
        print("possible tracked secret material:\n" + "\n".join(findings), file=sys.stderr); return 1
    print(f"security hygiene passed: scanned {len(files)} tracked files"); return 0
if __name__ == "__main__": raise SystemExit(main())
