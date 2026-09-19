#!/usr/bin/env python3
"""Validates contracts/errors/error-catalog.yaml against schema rules."""
import sys
from pathlib import Path

def main() -> int:
    root = Path(__file__).parents[2]
    catalog_path = root / "contracts/errors/error-catalog.yaml"
    if not catalog_path.exists():
        print(f"Error: {catalog_path} does not exist", file=sys.stderr)
        sys.exit(1)

    text = catalog_path.read_text(encoding="utf-8")
    # Simple line-based or YAML-parser validation without external dependencies
    # Each entry in error-catalog.yaml has code, service, component, operation, httpStatus, severity, retryable, safeDetail
    import re
    codes = re.findall(r"-\s+code:\s+([A-Za-z0-9_-]+)", text)
    if not codes:
        print("Error: No error codes found in catalog", file=sys.stderr)
        sys.exit(1)

    seen = set()
    for code in codes:
        if not code.startswith("ERR-"):
            print(f"Error: Code {code} does not match prefix ERR-", file=sys.stderr)
            sys.exit(1)
        if code in seen:
            print(f"Error: Duplicate code {code}", file=sys.stderr)
            sys.exit(1)
        seen.add(code)

    print(f"valid error catalog: {len(seen)} unique codes ({', '.join(sorted(seen))})")
    return 0

if __name__ == "__main__":
    main()
