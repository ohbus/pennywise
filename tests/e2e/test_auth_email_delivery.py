"""Deployed passwordless authentication and session-revocation journey."""

from __future__ import annotations

import json
import os
import re
import time
from typing import Any
from urllib.error import HTTPError
from urllib.request import Request, urlopen

from tests.http_constants import AUTHORIZATION, CONTENT_TYPE

ACCOUNTS_URL = os.environ.get("SQUAREWISE_ACCOUNTS_URL", "http://localhost:8081")
MAILPIT_URL = os.environ.get("SQUAREWISE_MAILPIT_URL", "http://localhost:8025")


def request_json(
    url: str,
    method: str = "GET",
    body: Any = None,
    bearer: str | None = None,
) -> tuple[int, Any]:
    """Make a JSON request and return its status and decoded response."""
    headers = {CONTENT_TYPE: "application/json"}
    if bearer:
        headers[AUTHORIZATION] = f"Bearer {bearer}"
    data = json.dumps(body).encode("utf-8") if body is not None else None
    request = Request(url, data=data, headers=headers, method=method)
    try:
        with urlopen(request, timeout=10) as response:
            content = response.read().decode("utf-8")
            return response.status, json.loads(content) if content else {}
    except HTTPError as error:
        content = error.read().decode("utf-8")
        try:
            return error.code, json.loads(content) if content else {}
        except json.JSONDecodeError:
            return error.code, {"raw": content}


def wait_for_credential(recipient: str) -> str:
    """Read the unique delivered login code from Mailpit without logging it."""
    deadline = time.monotonic() + 30
    while time.monotonic() < deadline:
        status, listing = request_json(f"{MAILPIT_URL}/api/v1/messages?limit=50")
        if status == 200 and isinstance(listing, dict):
            for message in listing.get("messages", []):
                recipients = message.get("To", [])
                addresses = {
                    str(item.get("Address", ""))
                    for item in recipients
                    if isinstance(item, dict)
                }
                if recipient not in addresses:
                    continue
                message_id = message.get("ID")
                if not message_id:
                    continue
                detail_status, detail = request_json(
                    f"{MAILPIT_URL}/api/v1/message/{message_id}"
                )
                if detail_status != 200 or not isinstance(detail, dict):
                    continue
                text = str(detail.get("Text") or detail.get("text") or "")
                match = re.search(r"one-time Squarewise sign-in code is:\s*(\S+)", text)
                if match:
                    return match.group(1)
        time.sleep(1)
    raise AssertionError("Mailpit did not receive a usable passwordless login code")


def main() -> int:
    """Verify startLogin, verifyLogin, replay rejection, and logout revocation."""
    recipient = f"qa-e2e-{int(time.time() * 1000)}@example.com"
    start_status, start_response = request_json(
        f"{ACCOUNTS_URL}/accounts/v1/auth/login/start",
        method="POST",
        body={"email": recipient, "channel": "CODE", "clientKind": "NATIVE"},
    )
    assert start_status == 202, f"Accounts startLogin failed: HTTP {start_status} ({start_response})"
    assert start_response.get("status") == "ACCEPTED"

    credential = wait_for_credential(recipient)
    verify_status, tokens = request_json(
        f"{ACCOUNTS_URL}/accounts/v1/auth/login/verify",
        method="POST",
        body={"credential": credential, "clientKind": "NATIVE"},
    )
    assert verify_status == 200, f"Accounts verifyLogin failed: HTTP {verify_status} ({tokens})"
    access_token = tokens.get("accessToken")
    refresh_token = tokens.get("refreshToken")
    assert access_token and refresh_token, "verifyLogin must return access and refresh tokens"

    replay_status, _ = request_json(
        f"{ACCOUNTS_URL}/accounts/v1/auth/login/verify",
        method="POST",
        body={"credential": credential, "clientKind": "NATIVE"},
    )
    assert replay_status == 401, "verifyLogin must reject a replayed one-time credential"

    logout_status, logout_response = request_json(
        f"{ACCOUNTS_URL}/accounts/v1/auth/logout",
        method="POST",
        body={"refreshToken": refresh_token},
        bearer=access_token,
    )
    assert logout_status == 204, f"Accounts logout failed: HTTP {logout_status} ({logout_response})"

    refresh_status, _ = request_json(
        f"{ACCOUNTS_URL}/accounts/v1/auth/token/refresh",
        method="POST",
        body={"refreshToken": refresh_token},
    )
    assert refresh_status == 401, "logout must revoke the refresh-token family"
    print("  [ok] startLogin delivered, verifyLogin redeemed once, replay was rejected, and logout revoked refresh")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
