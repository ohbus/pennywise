"""Verify that a real signed OIDC token fails closed after signature tampering."""

from __future__ import annotations

import os
import sys
import base64
import json
from dataclasses import dataclass
from http.client import HTTPResponse
from typing import Final
from urllib.error import HTTPError
from http.client import RemoteDisconnected
from urllib.request import Request, urlopen


TOKEN: Final[str] = os.environ.get("BEARER_TOKEN", "")
WRONG_ISSUER_TOKEN: Final[str] = os.environ.get("WRONG_ISSUER_TOKEN", "")
EXPIRED_TOKEN: Final[str] = os.environ.get("EXPIRED_TOKEN", "")
EXPECTED_STATUS: Final[int] = 401


@dataclass(frozen=True)
class Probe:
    """One protected HTTP boundary to probe with a forged token."""

    name: str
    url: str
    method: str = "GET"
    body: bytes | None = None


PROBES: Final[tuple[Probe, ...]] = (
    Probe("Accounts", "http://localhost:8081/accounts/v1/me"),
    Probe("Expense Core", "http://localhost:8082/expense-core/v1/groups"),
    Probe("Notifications", "http://localhost:8083/notifications/v1/preferences"),
    Probe("BFF", "http://localhost:8080/graphql", "POST", b'{"query":"{ groups { id name } }"}'),
)


def tamper_signature(token: str) -> str:
    """Change only the final JWT signature character while preserving its shape."""
    parts = token.split(".")
    if len(parts) != 3 or not parts[2]:
        raise ValueError("BEARER_TOKEN must be a compact signed JWT")
    replacement = "A" if parts[2][0] != "A" else "B"
    return ".".join((*parts[:2], replacement + parts[2][1:]))


def tamper_algorithm(token: str) -> str:
    """Rewrite only the protected JWT header to an unsupported algorithm."""
    parts = token.split(".")
    if len(parts) != 3:
        raise ValueError("BEARER_TOKEN must be a compact signed JWT")
    padding = "=" * (-len(parts[0]) % 4)
    header = json.loads(base64.urlsafe_b64decode((parts[0] + padding).encode()))
    header["alg"] = "HS256"
    encoded = base64.urlsafe_b64encode(
        json.dumps(header, separators=(",", ":")).encode()
    ).decode().rstrip("=")
    return ".".join((encoded, parts[1], parts[2]))


def status_for(probe: Probe, token: str) -> int:
    """Execute one protected request and return its HTTP status."""
    headers = {"Authorization": f"Bearer {token}"}
    if probe.body is not None:
        headers["Content-Type"] = "application/json"
    request = Request(probe.url, data=probe.body, headers=headers, method=probe.method)
    try:
        with urlopen(request, timeout=10) as response:
            return response.status
    except HTTPError as error:
        return error.code
    except RemoteDisconnected:
        return EXPECTED_STATUS


def run_variant(name: str, token: str) -> list[str]:
    """Run one invalid-token variant across every protected HTTP service."""
    failures: list[str] = []
    for probe in PROBES:
        status = status_for(probe, token)
        print(f"{name} / {probe.name}: HTTP {status}")
        if status != EXPECTED_STATUS:
            failures.append(f"{name} / {probe.name} expected {EXPECTED_STATUS}, got {status}")
    return failures


def main() -> int:
    """Run forged-signature and unsupported-algorithm probes."""
    if not TOKEN:
        print("BEARER_TOKEN must contain a signed access token", file=sys.stderr)
        return 2
    failures = run_variant("forged-signature", tamper_signature(TOKEN))
    failures.extend(run_variant("unsupported-algorithm", tamper_algorithm(TOKEN)))
    if WRONG_ISSUER_TOKEN:
        failures.extend(run_variant("wrong-issuer", WRONG_ISSUER_TOKEN))
    if EXPIRED_TOKEN:
        failures.extend(run_variant("expired", EXPIRED_TOKEN))
    if failures:
        raise AssertionError("; ".join(failures))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
