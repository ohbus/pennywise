#!/usr/bin/env sh
set -eu

repo_root=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
cd "$repo_root"

mkdir -p build/reports/acceptance
uv run --frozen --no-build python3 tests/acceptance/runner.py "$@"
