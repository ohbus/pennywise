"""Typed writers for live QA-10 execution evidence."""

from __future__ import annotations

import json
from pathlib import Path
from typing import TypedDict


class ExecutionOperation(TypedDict):
    """Machine-readable evidence for one successfully exercised operation."""

    surface: str
    operation: str
    status: str
    artifact: str
    assertions: list[str]


class ExecutionEvidence(TypedDict):
    """Versioned QA-10 execution evidence emitted after a green run."""

    schema: str
    source_revision: str
    environment: str
    operations: list[ExecutionOperation]


def write_execution_evidence(
    path: Path,
    source_revision: str,
    environment: str,
    operations: list[ExecutionOperation],
) -> None:
    """Persist attributable evidence after the associated suite succeeds.

    :param path: Destination for the versioned QA-10 JSON artifact.
    :param source_revision: Revision whose test code produced the evidence.
    :param environment: Runtime environment used by the test.
    :param operations: Exact public operations and assertions completed by the suite.
    """

    evidence: ExecutionEvidence = {
        "schema": "qa10-operation-execution-v1",
        "source_revision": source_revision,
        "environment": environment,
        "operations": operations,
    }
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(evidence, indent=2) + "\n", encoding="utf-8")
