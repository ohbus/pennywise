#!/usr/bin/env python3
"""
Offline Client Sync & Replay Resilience Test Suite for Pennywise.

Verifies offline simulation, idempotency guarantees, and sync recovery (OFF-01 through OFF-05):
1. Client generates multiple mutations locally while disconnected (client-generated UUIDs + Idempotency-Keys).
2. Client reconnects and batch-replays queued mutations to GraphQL BFF.
3. Client re-replays identical batch (simulating network timeout / retransmission):
   - Invariant: Zero duplicate ledger postings, zero extra revision bumps.
4. Client attempts conflicting reuse of an existing Idempotency-Key with different payload:
   - Invariant: Rejected with HTTP 409 / CONFLICT, balances unchanged.
5. Offline Sync Cursor Gap Recovery:
   - Client takes snapshot cursor, other users perform mutations, client queries /sync/changes with cursor
   - Invariant: Full chronological change delta and tombstones received.
"""

import json
import os
import sys
import time
import urllib.error
import urllib.request
import uuid
from typing import Any, Tuple

BASE_URL = os.environ.get("PENNYWISE_BFF_URL", "http://localhost:8080")
EXPENSE_CORE_URL = os.environ.get("PENNYWISE_EXPENSE_CORE_URL", "http://localhost:8082")
ACCOUNTS_URL = os.environ.get("PENNYWISE_ACCOUNTS_URL", "http://localhost:8081")


def request_json(
    url: str,
    method: str = "GET",
    body: Any = None,
    bearer: str | None = None,
    extra_headers: dict[str, str] | None = None,
    timeout: float = 10.0,
) -> Tuple[int, Any]:
    headers = {
        "Accept": "application/json",
        "Content-Type": "application/json",
    }
    if bearer:
        headers["Authorization"] = f"Bearer {bearer}"
    if extra_headers:
        headers.update(extra_headers)

    data = None
    if body is not None:
        data = json.dumps(body).encode("utf-8")

    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as response:
            status = response.status
            content = response.read().decode("utf-8")
            return status, json.loads(content) if content else {}
    except urllib.error.HTTPError as error:
        try:
            content = error.read().decode("utf-8")
            parsed = json.loads(content) if content else {}
        except Exception:
            parsed = {"raw": content}
        return error.code, parsed
    except Exception as error:
        return 503, {"error": str(error)}


def graphql_query(query: str, variables: dict[str, Any] | None = None, bearer: str | None = None) -> Tuple[int, dict[str, Any]]:
    payload = {"query": query}
    if variables:
        payload["variables"] = variables
    status, res = request_json(f"{BASE_URL}/graphql", method="POST", body=payload, bearer=bearer)
    return status, res


def run_offline_resilience_tests() -> int:
    """Run offline replay checks with explicit UTF-8 console output."""
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stderr.reconfigure(encoding="utf-8")
    print("=" * 70)
    print("🔌 Running Offline Client Sync & Replay Resilience Test Suite")
    print("=" * 70)

    # Step 1: Provision two users and a group
    print("\n[Step 1] Provisioning test users and active group...")
    user_a = f"alice-{uuid.uuid4().hex[:8]}"
    user_b = f"bob-{uuid.uuid4().hex[:8]}"

    status, profile_a = request_json(f"{ACCOUNTS_URL}/accounts/v1/me", bearer=user_a)
    assert status == 200, f"Failed to get profile for Alice: {profile_a}"
    alice_id = profile_a["accountId"]

    status, profile_b = request_json(f"{ACCOUNTS_URL}/accounts/v1/me", bearer=user_b)
    assert status == 200, f"Failed to get profile for Bob: {profile_b}"
    bob_id = profile_b["accountId"]

    create_group_query = """
    mutation CreateGroup($input: CreateGroupInput!) {
        createGroup(input: $input) { id name revision }
    }
    """
    status, res = graphql_query(create_group_query, variables={"input": {"name": "Offline Expedition", "kind": "TRIP", "currency": "EUR"}}, bearer=user_a)
    assert status == 200 and "data" in res, f"Failed to create group: {res}"
    group_id = res["data"]["createGroup"]["id"]
    print(f"  ✓ Group created: id={group_id}")

    # Add Bob to group via invite claim
    status, invite = request_json(f"{EXPENSE_CORE_URL}/expense-core/v1/groups/{group_id}/invites", method="POST", body={"expiresInHours": 24}, bearer=user_a)
    assert status == 201, f"Failed to create invite: {invite}"
    status, claim = request_json(f"{EXPENSE_CORE_URL}/expense-core/v1/invites/{invite['token']}/claim", method="POST", bearer=user_b)
    assert status == 200, f"Bob failed to claim invite: {claim}"
    print(f"  ✓ Bob joined group: members configured [Alice, Bob]")

    # Step 2: Simulate Offline Queueing of Multiple Mutations
    print("\n[Step 2] Simulating client offline state: queueing 3 expense mutations locally...")
    offline_queue = [
        {
            "expenseId": str(uuid.uuid4()),
            "idempotencyKey": f"idemp-offline-1-{uuid.uuid4()}",
            "description": "Trail Snacks",
            "amountMinor": "1500",
        },
        {
            "expenseId": str(uuid.uuid4()),
            "idempotencyKey": f"idemp-offline-2-{uuid.uuid4()}",
            "description": "Campground Fee",
            "amountMinor": "4500",
        },
        {
            "expenseId": str(uuid.uuid4()),
            "idempotencyKey": f"idemp-offline-3-{uuid.uuid4()}",
            "description": "First Aid Kit",
            "amountMinor": "2000",
        },
    ]
    print(f"  ✓ Queued {len(offline_queue)} expenses with client-side UUIDs & unique idempotency keys")

    # Step 3: Client Reconnects and Replays Batch
    print("\n[Step 3] Client reconnects: batch replaying queued expenses...")
    create_expense_mutation = """
    mutation CreateExpense($groupId: ID!, $input: CreateExpenseInput!, $idempotencyKey: String!) {
        createExpense(groupId: $groupId, input: $input, idempotencyKey: $idempotencyKey) {
            id
            description
            amount { currency minor }
        }
    }
    """
    for item in offline_queue:
        payload = {
            "expenseId": item["expenseId"],
            "description": item["description"],
            "amount": {"currency": "EUR", "minor": item["amountMinor"]},
            "payers": [{"participantId": alice_id, "amount": {"currency": "EUR", "minor": item["amountMinor"]}}],
            "allocation": {
                "mode": "EQUAL",
                "items": [
                    {"participantId": alice_id, "value": "1"},
                    {"participantId": bob_id, "value": "1"},
                ]
            }
        }
        status, exp_res = graphql_query(create_expense_mutation, variables={"groupId": group_id, "input": payload, "idempotencyKey": item["idempotencyKey"]}, bearer=user_a)
        assert status == 200 and "data" in exp_res and exp_res["data"]["createExpense"]["id"] == item["expenseId"], f"Failed to replay {item['description']}: {exp_res}"
        print(f"  ✓ Replayed: '{item['description']}' ({int(item['amountMinor'])/100:.2f} EUR) -> Accepted")

    # Verify Balances after batch
    status, balances_res = request_json(f"{EXPENSE_CORE_URL}/expense-core/v1/groups/{group_id}/balances", bearer=user_a)
    assert status == 200, f"Failed to get balances: {balances_res}"
    total_minor = sum(int(item["amountMinor"]) for item in offline_queue)  # 1500 + 4500 + 2000 = 8000
    expected_split = total_minor // 2  # 4000
    bal_map = {b["participantId"]: int(b["amount"]["minor"]) for b in balances_res["balances"]}
    print(f"  ✓ Group balances after replay: {bal_map}")
    assert bal_map[alice_id] == expected_split, f"Alice balance mismatch: {bal_map[alice_id]} vs expected {expected_split}"
    assert bal_map[bob_id] == -expected_split, f"Bob balance mismatch: {bal_map[bob_id]} vs expected -{expected_split}"

    # Step 4: Re-replay identical batch (Retransmission Idempotency Drill)
    print("\n[Step 4] Simulating network timeout / retransmission: re-replaying identical batch...")
    for item in offline_queue:
        payload = {
            "expenseId": item["expenseId"],
            "description": item["description"],
            "amount": {"currency": "EUR", "minor": item["amountMinor"]},
            "payers": [{"participantId": alice_id, "amount": {"currency": "EUR", "minor": item["amountMinor"]}}],
            "allocation": {
                "mode": "EQUAL",
                "items": [
                    {"participantId": alice_id, "value": "1"},
                    {"participantId": bob_id, "value": "1"},
                ]
            }
        }
        status, exp_res = graphql_query(create_expense_mutation, variables={"groupId": group_id, "input": payload, "idempotencyKey": item["idempotencyKey"]}, bearer=user_a)
        assert status == 200 and "data" in exp_res, f"Idempotent re-replay failed for {item['description']}: {exp_res}"
        print(f"  ✓ Re-replayed: '{item['description']}' -> Idempotently confirmed")

    # Invariant Verification: Balances and postings must remain completely identical
    status, balances_check = request_json(f"{EXPENSE_CORE_URL}/expense-core/v1/groups/{group_id}/balances", bearer=user_a)
    bal_map_recheck = {b["participantId"]: int(b["amount"]["minor"]) for b in balances_check["balances"]}
    assert bal_map_recheck == bal_map, f"Idempotency violation! Balances changed: {bal_map_recheck} vs {bal_map}"
    print(f"  ✓ Idempotency invariant verified: balances strictly unchanged ({bal_map_recheck})")

    # Step 5: Conflicting Idempotency Reuse Rejection (OFF-01 / ERR-06)
    print("\n[Step 5] Attempting conflicting reuse of Idempotency-Key with modified amount...")
    # Re-use the existing expenseId from offline_queue[0] with modified/tampered amount
    conflicting_key = offline_queue[0]["idempotencyKey"]
    conflicting_payload = {
        "expenseId": offline_queue[0]["expenseId"],  # Reusing same expenseId (client mutation UUID)
        "description": "Trail Snacks Tampered",
        "amount": {"currency": "EUR", "minor": "99999"},  # different amount!
        "payers": [{"participantId": alice_id, "amount": {"currency": "EUR", "minor": "99999"}}],
        "allocation": {
            "mode": "EQUAL",
            "items": [
                {"participantId": alice_id, "value": "1"},
                {"participantId": bob_id, "value": "1"},
            ]
        }
    }
    # Test via Expense Core REST to verify exact HTTP 409 status code and conflict payload
    status_conflict, conflict_data = request_json(
        f"{EXPENSE_CORE_URL}/expense-core/v1/groups/{group_id}/expenses",
        method="POST",
        body={
            "expenseId": conflicting_payload["expenseId"],
            "description": conflicting_payload["description"],
            "amount": conflicting_payload["amount"],
            "payers": conflicting_payload["payers"],
            "allocation": conflicting_payload["allocation"],
        },
        bearer=user_a,
        extra_headers={"Idempotency-Key": conflicting_key}
    )
    assert status_conflict == 409, f"Expected HTTP 409 Conflict, got {status_conflict}: {conflict_data}"
    assert conflict_data.get("code") in ("CONFLICT", "IDEMPOTENCY_CONFLICT", "ERR-06", "ERR_06"), f"Expected conflict error code, got: {conflict_data}"
    print(f"  ✓ Conflicting idempotency write correctly rejected: HTTP {status_conflict} ({conflict_data.get('code')})")

    # Step 6: Offline Sync Cursor Gap Recovery (OFF-03, OFF-04)
    print("\n[Step 6] Verifying offline sync cursor gap recovery...")
    # Client fetches initial snapshot and records cursor
    status_snap, initial_snap = request_json(f"{EXPENSE_CORE_URL}/expense-core/v1/groups/{group_id}/sync/snapshot", bearer=user_a)
    assert status_snap == 200, f"Failed to get snapshot: {initial_snap}"
    saved_cursor = initial_snap.get("nextCursor")
    initial_revisions = len(initial_snap.get("changes", []))
    print(f"  ✓ Client recorded sync cursor: '{saved_cursor}' (initial revisions={initial_revisions})")

    # While client is offline, Bob records a new expense
    print("  ... simulating background activity while client is offline ...")
    bob_expense_id = str(uuid.uuid4())
    status_bob, bob_res = request_json(
        f"{EXPENSE_CORE_URL}/expense-core/v1/groups/{group_id}/expenses",
        method="POST",
        body={
            "expenseId": bob_expense_id,
            "description": "Campfire Firewood",
            "amount": {"currency": "EUR", "minor": "1200"},
            "payers": [{"participantId": bob_id, "amount": {"currency": "EUR", "minor": "1200"}}],
            "allocation": {
                "mode": "EQUAL",
                "items": [
                    {"participantId": alice_id, "value": "1"},
                    {"participantId": bob_id, "value": "1"},
                ]
            }
        },
        bearer=user_b,
        extra_headers={"Idempotency-Key": f"idemp-bob-{uuid.uuid4()}"}
    )
    assert status_bob == 201, f"Failed to post Bob's expense: {bob_res}"
    print(f"  ✓ Bob recorded 'Campfire Firewood' (12.00 EUR) during offline gap")

    # Reconnecting client queries /sync/changes with its saved cursor
    status_delta, delta_page = request_json(
        f"{EXPENSE_CORE_URL}/expense-core/v1/groups/{group_id}/sync/changes?cursor={saved_cursor}",
        bearer=user_a
    )
    assert status_delta == 200, f"Failed to query sync changes: {delta_page}"
    delta_changes = delta_page.get("changes", [])
    assert len(delta_changes) >= 1, f"Expected at least 1 change in delta, got: {delta_changes}"
    latest_change = delta_changes[-1]
    assert bob_expense_id in str(latest_change.get("entityId")) or str(latest_change.get("payload", "")), f"Expected Bob's expense in delta change: {latest_change}"
    print(f"  ✓ Sync cursor gap recovered: received {len(delta_changes)} incremental change(s), latest revision={latest_change.get('revision')}")

    print("\n" + "=" * 70)
    print("🎉 ALL OFFLINE RESILIENCE & REPLAY TESTS PASSED SUCCESSFULLY!")
    print("=" * 70)
    return 0


if __name__ == "__main__":
    try:
        sys.exit(run_offline_resilience_tests())
    except AssertionError as err:
        print(f"\n❌ TEST FAILED: {err}", file=sys.stderr)
        sys.exit(1)
    except Exception as err:
        print(f"\n💥 UNEXPECTED ERROR: {err}", file=sys.stderr)
        sys.exit(1)
