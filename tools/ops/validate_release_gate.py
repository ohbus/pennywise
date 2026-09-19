#!/usr/bin/env python3
"""Validate repository-owned production release-gate prerequisites."""

from __future__ import annotations

import json
import pathlib
import sys


ROOT = pathlib.Path(__file__).resolve().parents[2]


def require_file(path: str) -> None:
    target = ROOT / path
    if not target.is_file():
        raise AssertionError(f"missing required release asset: {path}")


def main() -> int:
    required = [
        "infra/deploy/docker-compose.prod.yml",
        "infra/observability/prometheus.yml",
        "infra/observability/rules/pennywise.yml",
        "infra/observability/grafana/dashboards/pennywise-overview.json",
        "docs/operations/release-hardening-checklist.md",
    ]
    for path in required:
        require_file(path)

    compose = (ROOT / "infra/deploy/docker-compose.prod.yml").read_text()
    for marker in ("read_only: true", "no-new-privileges:true", "healthcheck:", "prometheus"):
        if marker not in compose:
            raise AssertionError(f"production Compose is missing safety marker: {marker}")

    dashboard = json.loads(
        (ROOT / "infra/observability/grafana/dashboards/pennywise-overview.json").read_text()
    )
    if not dashboard.get("panels"):
        raise AssertionError("Grafana dashboard has no panels")

    checklist = (ROOT / "docs/operations/release-hardening-checklist.md").read_text()
    required_evidence = ("backup", "secret", "load", "rollback", "scan")
    missing = [word for word in required_evidence if word not in checklist.lower()]
    if missing:
        raise AssertionError(f"release checklist is missing evidence categories: {missing}")

    print(f"release assets valid: {len(required)} files")
    print("environment gates still required: restore, security scan, capacity, and rollback drill")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (AssertionError, OSError, json.JSONDecodeError) as error:
        print(f"release gate failed: {error}", file=sys.stderr)
        raise SystemExit(1)
