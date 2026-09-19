"""Public-interface edge-case journeys for the QA-05 acceptance gate."""

from __future__ import annotations

import json
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass
from typing import Any
from urllib.error import HTTPError
from urllib.request import Request, urlopen


@dataclass(frozen=True)
class JourneyResult:
    """Outcome of one public-interface journey."""

    id: str
    status: str
    detail: str


def request_json(base_url: str, path: str, method: str = "GET", body: dict[str, Any] | None = None,
                 token: str | None = "test-user", extra_headers: dict[str, str] | None = None) -> tuple[int, Any]:
    """Call a JSON endpoint and preserve error response bodies."""
    headers = {"Accept": "application/json", "Content-Type": "application/json"}
    if token is not None:
        headers["Authorization"] = f"Bearer {token}"
    if extra_headers:
        headers.update(extra_headers)
    request = Request(f"{base_url}{path}", method=method, headers=headers)
    if body is not None:
        request.data = json.dumps(body).encode("utf-8")
    try:
        with urlopen(request, timeout=2) as response:
            return response.status, json.loads(response.read() or b"{}")
    except HTTPError as error:
        try:
            return error.code, json.loads(error.read() or b"{}")
        except Exception:
            return error.code, {}
    except (Exception) as error:
        return 503, {"error": str(error)}


def run_qa05_journeys(base_url: str) -> list[JourneyResult]:
    """Run rollback, concurrency, authorization, fanout, and recovery probes."""
    group = "/expense-core/v1/groups/00000000-0000-0000-0000-000000000001"
    status, _ = request_json(base_url, group, "PATCH", {"name": "QA rollback"},
                             extra_headers={"X-Acceptance-Fault": "rollback"})
    results = [JourneyResult("QA05-ROLLBACK", "blocked" if status == 503 else ("passed" if status == 409 else "failed"),
                             f"PATCH rollback probe returned HTTP {status}")]

    def rename(name: str) -> int:
        return request_json(base_url, group, "PATCH", {"name": name})[0]

    with ThreadPoolExecutor(max_workers=2) as pool:
        statuses = list(pool.map(rename, ("QA concurrent A", "QA concurrent B")))
    results.append(JourneyResult("QA05-CONCURRENCY", "blocked" if (501 in statuses or 503 in statuses) else
                                ("passed" if sorted(statuses) == [200, 409] else "failed"),
                                f"Concurrent rename responses: {statuses}"))
    status, _ = request_json(base_url, f"{group}/members", token=None)
    results.append(JourneyResult("QA05-AUTHORIZATION", "blocked" if status == 503 else ("passed" if status in (401, 403) else "failed"),
                                f"Unauthenticated members response: HTTP {status}"))
    status, body = request_json(base_url, "/graphql", "POST", {"query": "query fanoutFailure { groups { id members { subject } } }"})
    results.append(JourneyResult("QA05-BFF-FANOUT", "blocked" if status in (501, 503) else
                                ("passed" if status == 200 and body.get("errors") else "failed"),
                                f"BFF fanout failure response: HTTP {status}"))
    status, body = request_json(base_url, "/graphql", "POST", {"query": "query recovery { groups { id name } }"})
    results.append(JourneyResult("QA05-RECOVERY", "blocked" if status == 503 else ("passed" if status == 200 and body.get("data") else "failed"),
                                "BFF query recovered with a data payload" if status == 200 and body.get("data") else f"BFF recovery returned HTTP {status}"))
    return results
