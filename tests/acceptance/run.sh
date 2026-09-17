#!/usr/bin/env sh
set -eu

repo_root=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
cd "$repo_root"

mkdir -p build/reports/acceptance
python3 tests/acceptance/runner.py "$@"
