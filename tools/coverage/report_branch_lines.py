"""Emit the exact JaCoCo source lines containing missed branches."""

from __future__ import annotations

import argparse
import json
from dataclasses import asdict
from pathlib import Path
import sys
from typing import Sequence

from tools.coverage.report_branch_gaps import line_gaps, line_markdown


def arguments(argv: Sequence[str]) -> argparse.Namespace:
    """Parse line-inventory output options."""

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path.cwd())
    parser.add_argument("--format", choices=("json", "markdown"), default="markdown")
    return parser.parse_args(argv)


def main(argv: Sequence[str] | None = None) -> int:
    """Emit exact source-line branch gaps."""

    options = arguments(sys.argv[1:] if argv is None else argv)
    gaps = line_gaps(options.root.resolve())
    if options.format == "json":
        print(json.dumps([asdict(gap) for gap in gaps], indent=2))
    else:
        print(line_markdown(gaps))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
