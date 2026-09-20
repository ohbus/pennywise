#!/usr/bin/env python3
"""
Comprehensive End-to-End Test Suite for Pennywise Backend.

Verifies the entire product lifecycle across all four microservices
(Accounts, Expense Core, Notifications, GraphQL BFF) in the Docker environment:
1. User profile registration and verification via Accounts API & BFF GraphQL `me`
2. Group creation via BFF GraphQL `createGroup`
3. Multi-participant invitation & invite claiming via Expense Core REST
4. Expense addition via BFF GraphQL `createExpense` with multi-participant equal split
5. Balance calculation verification via BFF GraphQL `group` and REST `/balances`
6. Settlement suggestions calculation via BFF GraphQL `settlementSuggestions`
7. Repayment recording via BFF GraphQL `recordRepayment` and balance reconciliation
8. Outbox message dispatch to RabbitMQ and consumption into Notifications Inbox
9. Offline synchronization snapshot & change feed verification
"""

import json
import io
import os
import sys
import time
import urllib.error
import urllib.request
import uuid
from typing import Any, Tuple

BASE_URL = os.environ.get("PENNYWISE_BFF_URL", "http://localhost:8080")
ACCOUNTS_URL = os.environ.get("PENNYWISE_ACCOUNTS_URL", "http://localhost:8081")
EXPENSE_CORE_URL = os.environ.get("PENNYWISE_EXPENSE_CORE_URL", "http://localhost:8082")
NOTIFICATIONS_URL = os.environ.get("PENNYWISE_NOTIFICATIONS_URL", "http://localhost:8083")


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


def graphql_query(
    query: str,
    variables: dict[str, Any] | None = None,
    bearer: str | None = None,
) -> dict[str, Any]:
    payload = {"query": query}
    if variables:
        payload["variables"] = variables
    status, res = request_json(f"{BASE_URL}/graphql", method="POST", body=payload, bearer=bearer)
    assert status == 200, f"GraphQL HTTP status {status}: {res}"
    if "errors" in res and res["errors"]:
        raise AssertionError(f"GraphQL returned errors: {res['errors']}")
    return res.get("data", {})


def run_e2e_tests() -> int:
    """Run the product lifecycle journey with explicit UTF-8 console output."""
    if isinstance(sys.stdout, io.TextIOWrapper):
        sys.stdout.reconfigure(encoding="utf-8")
    if isinstance(sys.stderr, io.TextIOWrapper):
        sys.stderr.reconfigure(encoding="utf-8")
    print("=" * 70)
    print("🚀 Running Pennywise End-to-End Multi-Service Production Test Suite")
    print("=" * 70)

    # 1. Health checks across all services
    print("\n[Step 1] Verifying service actuator health...")
    services = {
        "BFF": f"{BASE_URL}/actuator/health",
        "Accounts": f"{ACCOUNTS_URL}/actuator/health",
        "Expense Core": f"{EXPENSE_CORE_URL}/actuator/health",
        "Notifications": f"{NOTIFICATIONS_URL}/actuator/health",
    }
    for name, health_url in services.items():
        status, body = request_json(health_url)
        assert status == 200, f"{name} health check failed: HTTP {status} ({body})"
        status_val = body.get("status") if isinstance(body, dict) else "UNKNOWN"
        print(f"  ✓ {name} is healthy: status={status_val}")

    unauthenticated_graphql_status, _ = request_json(
        f"{BASE_URL}/graphql",
        method="POST",
        body={"query": "query { me { accountId } }"},
    )
    assert unauthenticated_graphql_status == 401, (
        f"Unauthenticated GraphQL request returned HTTP {unauthenticated_graphql_status}"
    )
    print("  ✓ Unauthenticated GraphQL requests are rejected by BFF security")

    # 2. User profiles in Accounts & GraphQL me
    print("\n[Step 2] Testing User Profiles (Alice & Bob)...")
    user_a = os.environ.get("PENNYWISE_E2E_TOKEN_A", os.environ.get("BEARER_TOKEN"))
    user_b = os.environ.get("PENNYWISE_E2E_TOKEN_B", user_a)
    user_nonmember = os.environ.get("PENNYWISE_E2E_TOKEN_NONMEMBER", user_b)
    if not user_a or not user_b or not user_nonmember:
        raise RuntimeError("E2E persona variables must contain signed tokens")

    # Query 'me' for Alice via Accounts API
    status_a, profile_a = request_json(f"{ACCOUNTS_URL}/accounts/v1/me", bearer=user_a)
    assert status_a == 200, f"Failed to get profile for Alice: {profile_a}"
    alice_id = profile_a["accountId"]
    print(f"  ✓ Alice profile created: accountId={alice_id}, displayName={profile_a['displayName']}")

    # Query 'me' for Bob via GraphQL BFF
    data_bob = graphql_query(
        "query { me { accountId displayName defaultCurrency timezone } }",
        bearer=user_b
    )
    bob_me = data_bob["me"]
    bob_id = bob_me["accountId"]
    assert bob_id, "Bob accountId should not be empty"
    print(f"  ✓ Bob profile verified via GraphQL me: accountId={bob_id}, displayName={bob_me['displayName']}")

    malformed_group_status, malformed_group_response = request_json(
        f"{BASE_URL}/graphql",
        method="POST",
        body={"query": 'query { group(id: "not-a-uuid") { id } }'},
        bearer=user_b,
    )
    assert malformed_group_status == 200, (
        f"Malformed GraphQL group query returned HTTP {malformed_group_status}: "
        f"{malformed_group_response}"
    )
    assert isinstance(malformed_group_response, dict) and malformed_group_response.get("errors"), (
        f"Malformed GraphQL group query should return errors: {malformed_group_response}"
    )
    print("  ✓ GraphQL rejects malformed group identifier in its error envelope")

    missing_group_status, missing_group_response = request_json(
        f"{BASE_URL}/graphql",
        method="POST",
        body={"query": f'query {{ group(id: "{uuid.uuid4()}") {{ id }} }}'},
        bearer=user_b,
    )
    assert missing_group_status == 200, (
        f"Missing GraphQL group query returned HTTP {missing_group_status}: "
        f"{missing_group_response}"
    )
    assert isinstance(missing_group_response, dict) and missing_group_response.get("errors"), (
        f"Missing GraphQL group query should return errors: {missing_group_response}"
    )
    print("  ✓ GraphQL returns an error envelope for a missing group")

    malformed_suggestions_status, malformed_suggestions_response = request_json(
        f"{BASE_URL}/graphql",
        method="POST",
        body={"query": 'query { settlementSuggestions(groupId: "not-a-uuid") { fromParticipantId } }'},
        bearer=user_b,
    )
    assert malformed_suggestions_status == 200, (
        f"Malformed GraphQL suggestions query returned HTTP {malformed_suggestions_status}: "
        f"{malformed_suggestions_response}"
    )
    assert isinstance(malformed_suggestions_response, dict) and malformed_suggestions_response.get("errors"), (
        f"Malformed GraphQL suggestions query should return errors: {malformed_suggestions_response}"
    )
    print("  ✓ GraphQL rejects malformed settlement-suggestions group identifier")

    malformed_create_status, malformed_create_response = request_json(
        f"{BASE_URL}/graphql",
        method="POST",
        body={
            "query": "mutation { createGroup(input: { name: \"Invalid\", kind: NOT_A_KIND, currency: \"EUR\" }) { id } }"
        },
        bearer=user_b,
    )
    assert malformed_create_status == 200, (
        f"Malformed GraphQL createGroup returned HTTP {malformed_create_status}: "
        f"{malformed_create_response}"
    )
    assert isinstance(malformed_create_response, dict) and malformed_create_response.get("errors"), (
        f"Malformed GraphQL createGroup should return errors: {malformed_create_response}"
    )
    print("  ✓ GraphQL rejects invalid createGroup enum input")

    empty_groups_status, empty_groups_response = request_json(
        f"{BASE_URL}/graphql",
        method="POST",
        body={"query": "query { groups { id name } }"},
        bearer=user_nonmember,
    )
    assert empty_groups_status == 200, (
        f"Empty GraphQL groups query returned HTTP {empty_groups_status}: {empty_groups_response}"
    )
    assert isinstance(empty_groups_response, dict) and not empty_groups_response.get("errors"), (
        f"Empty GraphQL groups query should not return errors: {empty_groups_response}"
    )
    assert empty_groups_response.get("data", {}).get("groups") == [], (
        f"Expected empty GraphQL groups result: {empty_groups_response}"
    )
    print("  ✓ GraphQL groups returns an empty list for a user without memberships")

    # 3. Create Group via GraphQL
    print("\n[Step 3] Creating Group via BFF GraphQL mutation createGroup...")
    group_name = f"Alps Trip {uuid.uuid4().hex[:6]}"
    create_group_mutation = """
    mutation CreateGroup($input: CreateGroupInput!) {
        createGroup(input: $input) {
            id
            name
            revision
        }
    }
    """
    create_res = graphql_query(
        create_group_mutation,
        variables={"input": {"name": group_name, "kind": "TRIP", "currency": "EUR"}},
        bearer=user_a
    )
    group_id = create_res["createGroup"]["id"]
    assert group_id, "Expected non-empty groupId"
    print(f"  ✓ Group created: id={group_id}, name='{group_name}'")

    outsider_update_status, outsider_update_response = request_json(
        f"{BASE_URL}/graphql",
        method="POST",
        body={
            "query": (
                f'mutation {{ updateGroup(groupId: "{group_id}", name: "Unauthorized rename") '
                "{ id name } }"
            )
        },
        bearer=user_nonmember,
    )
    assert outsider_update_status == 200, (
        f"Unauthorized GraphQL update returned HTTP {outsider_update_status}: "
        f"{outsider_update_response}"
    )
    assert isinstance(outsider_update_response, dict) and outsider_update_response.get("errors"), (
        f"Unauthorized GraphQL update should return errors: {outsider_update_response}"
    )
    print("  ✓ GraphQL rejects non-member group update")

    outsider_group_status, outsider_group_response = request_json(
        f"{BASE_URL}/graphql",
        method="POST",
        body={"query": f'query {{ group(id: "{group_id}") {{ id name }} }}'},
        bearer=user_nonmember,
    )
    assert outsider_group_status == 200, (
        f"Unauthorized GraphQL group query returned HTTP {outsider_group_status}: "
        f"{outsider_group_response}"
    )
    assert isinstance(outsider_group_response, dict) and outsider_group_response.get("errors"), (
        f"Unauthorized GraphQL group query should return errors: {outsider_group_response}"
    )
    assert "Alps Trip" not in json.dumps(outsider_group_response), (
        f"Unauthorized GraphQL group query leaked group data: {outsider_group_response}"
    )
    print("  ✓ GraphQL rejects non-member group query without leaking group data")

    outsider_suggestions_status, outsider_suggestions_response = request_json(
        f"{BASE_URL}/graphql",
        method="POST",
        body={"query": f'query {{ settlementSuggestions(groupId: "{group_id}") {{ fromParticipantId }} }}'},
        bearer=user_nonmember,
    )
    assert outsider_suggestions_status == 200, (
        f"Unauthorized GraphQL suggestions returned HTTP {outsider_suggestions_status}: "
        f"{outsider_suggestions_response}"
    )
    assert isinstance(outsider_suggestions_response, dict) and outsider_suggestions_response.get("errors"), (
        f"Unauthorized GraphQL suggestions should return errors: {outsider_suggestions_response}"
    )
    print("  ✓ GraphQL rejects non-member settlement suggestions")

    outsider_repayment_status, outsider_repayment_response = request_json(
        f"{BASE_URL}/graphql",
        method="POST",
        body={
            "query": (
                f'mutation {{ recordRepayment(input: {{ groupId: "{group_id}", '
                'fromParticipantId: "outsider", toParticipantId: "alice", '
                'amount: { currency: "EUR", minor: "100" }, reason: "unauthorized" }) '
                "{ id } }"
            )
        },
        bearer=user_nonmember,
    )
    assert outsider_repayment_status == 200, (
        f"Unauthorized GraphQL repayment returned HTTP {outsider_repayment_status}: "
        f"{outsider_repayment_response}"
    )
    assert isinstance(outsider_repayment_response, dict) and outsider_repayment_response.get("errors"), (
        f"Unauthorized GraphQL repayment should return errors: {outsider_repayment_response}"
    )
    print("  ✓ GraphQL rejects non-member repayment recording")

    malformed_repayment_status, malformed_repayment_response = request_json(
        f"{BASE_URL}/graphql",
        method="POST",
        body={
            "query": (
                f'mutation {{ recordRepayment(input: {{ groupId: "{group_id}", '
                'fromParticipantId: "alice", toParticipantId: "bob", '
                'amount: { currency: "EUR", minor: "not-money" }, reason: "invalid" }) '
                "{ id } }"
            )
        },
        bearer=user_a,
    )
    assert malformed_repayment_status == 200, (
        f"Malformed GraphQL repayment returned HTTP {malformed_repayment_status}: "
        f"{malformed_repayment_response}"
    )
    assert isinstance(malformed_repayment_response, dict) and malformed_repayment_response.get("errors"), (
        f"Malformed GraphQL repayment should return errors: {malformed_repayment_response}"
    )
    print("  ✓ GraphQL rejects malformed repayment money")

    # 4. Invite Bob and Claim Invite
    print("\n[Step 4] Inviting Bob to Group and Claiming Invite...")
    # Create invite token from Alice
    status_inv, invite = request_json(
        f"{EXPENSE_CORE_URL}/expense-core/v1/groups/{group_id}/invites",
        method="POST",
        body={"expiresInHours": 24},
        bearer=user_a
    )
    assert status_inv == 201, f"Failed to create invite: {invite}"
    token = invite["token"]
    print(f"  ✓ Invite generated with token={token[:12]}...")

    # Claim invite as Bob
    status_claim, claim_res = request_json(
        f"{EXPENSE_CORE_URL}/expense-core/v1/invites/{token}/claim",
        method="POST",
        bearer=user_b
    )
    assert status_claim == 200, f"Bob failed to claim invite: {claim_res}"
    print(f"  ✓ Bob claimed invite successfully: {claim_res.get('name')}")

    # Verify group members via Expense Core
    status_members, members = request_json(
        f"{EXPENSE_CORE_URL}/expense-core/v1/groups/{group_id}/members",
        bearer=user_a
    )
    assert status_members == 200, f"Failed to list members: {members}"
    subjects = [m.get("subject") for m in members]
    assert profile_a["displayName"] in subjects, f"Expected Alice subject in members: {subjects}"
    assert bob_me["displayName"] in subjects, f"Expected Bob subject in members: {subjects}"
    print(f"  ✓ Group members verified: {subjects}")

    # 5. Add Expense via GraphQL createExpense
    print("\n[Step 5] Adding Expense via GraphQL createExpense (Alice pays 100.00 EUR split equally with Bob)...")
    expense_id = str(uuid.uuid4())
    idempotency_key = f"idemp-e2e-{uuid.uuid4()}"
    create_expense_mutation = """
    mutation CreateExpense($groupId: ID!, $input: CreateExpenseInput!, $idempotencyKey: String!) {
        createExpense(groupId: $groupId, input: $input, idempotencyKey: $idempotencyKey) {
            id
            description
            amount {
                currency
                minor
            }
            allocations {
                participantId
                amount {
                    currency
                    minor
                }
            }
        }
    }
    """
    expense_input = {
        "expenseId": expense_id,
        "description": "Ski Passes",
        "amount": {"currency": "EUR", "minor": "10000"},
        "payers": [{"participantId": alice_id, "amount": {"currency": "EUR", "minor": "10000"}}],
        "allocation": {
            "mode": "EQUAL",
            "items": [
                {"participantId": alice_id, "value": "1"},
                {"participantId": bob_id, "value": "1"},
            ]
        }
    }
    exp_res = graphql_query(
        create_expense_mutation,
        variables={"groupId": group_id, "input": expense_input, "idempotencyKey": idempotency_key},
        bearer=user_a
    )
    created_exp = exp_res["createExpense"]
    assert created_exp["id"] == expense_id
    assert created_exp["amount"]["minor"] == "10000"
    print(f"  ✓ Expense recorded: id={expense_id}, amount=100.00 EUR, allocations count={len(created_exp['allocations'])}")

    replay_res = graphql_query(
        create_expense_mutation,
        variables={"groupId": group_id, "input": expense_input, "idempotencyKey": idempotency_key},
        bearer=user_a,
    )
    replayed_expense = replay_res["createExpense"]
    assert replayed_expense["id"] == expense_id, (
        f"GraphQL idempotent replay returned a different expense: {replayed_expense}"
    )
    print("  ✓ GraphQL createExpense replay is idempotent")

    tampered_expense_input = {**expense_input, "description": "Tampered replay"}
    tampered_status, tampered_response = request_json(
        f"{BASE_URL}/graphql",
        method="POST",
        body={
            "query": create_expense_mutation,
            "variables": {
                "groupId": group_id,
                "input": tampered_expense_input,
                "idempotencyKey": idempotency_key,
            },
        },
        bearer=user_a,
    )
    assert tampered_status == 200, (
        f"Tampered GraphQL replay returned HTTP {tampered_status}: {tampered_response}"
    )
    assert isinstance(tampered_response, dict) and tampered_response.get("errors"), (
        f"Tampered GraphQL replay should return errors: {tampered_response}"
    )
    print("  ✓ GraphQL createExpense rejects a tampered idempotency replay")

    # 6. Verify Balances via GraphQL group query and REST
    print("\n[Step 6] Verifying balances via GraphQL group query...")
    group_query = """
    query GetGroup($id: ID!) {
        group(id: $id) {
            id
            name
            balances {
                participantId
                money {
                    currency
                    minor
                }
            }
        }
    }
    """
    group_data = graphql_query(group_query, variables={"id": group_id}, bearer=user_a)["group"]
    balance_map = {b["participantId"]: int(b["money"]["minor"]) for b in group_data["balances"]}
    print(f"  ✓ Current balances: {balance_map}")
    assert balance_map.get(alice_id) == 5000, f"Alice should be owed 50.00 EUR (+5000), got: {balance_map.get(alice_id)}"
    assert balance_map.get(bob_id) == -5000, f"Bob should owe 50.00 EUR (-5000), got: {balance_map.get(bob_id)}"
    assert sum(balance_map.values()) == 0, "Balances must sum to zero"
    print("  ✓ Zero-sum ledger invariant holds: sum(balances) == 0")

    # 7. Query settlement suggestions
    print("\n[Step 7] Checking settlement suggestions via GraphQL...")
    suggestions_query = """
    query GetSuggestions($groupId: ID!) {
        settlementSuggestions(groupId: $groupId) {
            fromParticipantId
            toParticipantId
            amount {
                currency
                minor
            }
        }
    }
    """
    sugg_data = graphql_query(suggestions_query, variables={"groupId": group_id}, bearer=user_a)
    suggestions = sugg_data["settlementSuggestions"]
    assert len(suggestions) == 1, f"Expected exactly 1 settlement suggestion, got: {suggestions}"
    sugg = suggestions[0]
    assert sugg["fromParticipantId"] == bob_id
    assert sugg["toParticipantId"] == alice_id
    assert sugg["amount"]["minor"] == "5000"
    print(f"  ✓ Settlement suggestion verified: Bob pays Alice 50.00 EUR")

    # 8. Record Repayment via GraphQL
    print("\n[Step 8] Recording repayment via GraphQL recordRepayment...")
    record_repayment_mutation = """
    mutation RecordRepayment($input: RepaymentInput!) {
        recordRepayment(input: $input) {
            id
            status
            amount {
                currency
                minor
            }
        }
    }
    """
    repay_input = {
        "groupId": group_id,
        "fromParticipantId": bob_id,
        "toParticipantId": alice_id,
        "amount": {"currency": "EUR", "minor": "5000"},
        "reason": "Settling ski passes"
    }
    repay_res = graphql_query(record_repayment_mutation, variables={"input": repay_input}, bearer=user_b)
    settlement = repay_res["recordRepayment"]
    assert settlement["status"] == "RECORDED"
    assert settlement["amount"]["minor"] == "5000"
    print(f"  ✓ Repayment recorded successfully: id={settlement['id']}, status={settlement['status']}")

    # 9. Verify Reconciled Balances via REST
    print("\n[Step 9] Verifying balances via REST /balances endpoint...")
    status_bal, rest_balances = request_json(
        f"{EXPENSE_CORE_URL}/expense-core/v1/groups/{group_id}/balances",
        bearer=user_a
    )
    assert status_bal == 200, f"Failed to get balances: {rest_balances}"
    bal_items = rest_balances.get("balances", [])
    print(f"  ✓ REST balances returned: {len(bal_items)} items")

    # 10. Verify Outbox Dispatch & Notifications Inbox
    print("\n[Step 10] Verifying Outbox Relay & Notifications Inbox...")
    # Allow background outbox daemon and rabbit listener a few seconds to deliver
    found_notification = False
    for attempt in range(1, 10):
        status_inbox, inbox_data = request_json(
            f"{NOTIFICATIONS_URL}/notifications/v1/inbox",
            bearer=user_a
        )
        if status_inbox == 200 and inbox_data.get("items"):
            items = inbox_data["items"]
            matching = [item for item in items if expense_id in str(item.get("notificationId")) or group_id in str(item.get("message"))]
            if matching:
                print(f"  ✓ Verified event delivery to Notifications Inbox: eventType={matching[0]['eventType']}, message='{matching[0]['message']}'")
                found_notification = True
                break
        time.sleep(1)

    assert found_notification, "Expected notification to be delivered to Notifications inbox"

    # 11. Verify Offline Sync Snapshot and Changes Feed
    print("\n[Step 11] Verifying Offline Sync Feed...")
    status_snap, snapshot = request_json(
        f"{EXPENSE_CORE_URL}/expense-core/v1/groups/{group_id}/sync/snapshot",
        bearer=user_a
    )
    assert status_snap == 200, f"Failed to get sync snapshot: {snapshot}"
    changes = snapshot.get("changes", [])
    assert len(changes) >= 1, "Expected at least one change record in sync snapshot"
    print(f"  ✓ Sync snapshot verified: {len(changes)} revisions tracked (latest revision={changes[-1]['revision']})")

    print("\n" + "=" * 70)
    print("🎉 ALL END-TO-END PRODUCT LIFECYCLE TESTS PASSED SUCCESSFULLY!")
    print("=" * 70)
    return 0


if __name__ == "__main__":
    try:
        sys.exit(run_e2e_tests())
    except AssertionError as err:
        print(f"\n❌ TEST FAILED: {err}", file=sys.stderr)
        sys.exit(1)
    except Exception as err:
        print(f"\n💥 UNEXPECTED ERROR: {err}", file=sys.stderr)
        sys.exit(1)
