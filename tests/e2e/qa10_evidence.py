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


class ExecutionSpec(TypedDict):
    """Reviewed operation assertion metadata used by a live suite."""

    surface: str
    operation: str
    assertions: list[str]


def load_execution_specs(path: Path) -> list[ExecutionSpec]:
    """Load and validate operation metadata without inferring execution.

    :param path: Version-controlled suite metadata file.
    :return: Typed operation specifications for a later success-only report.
    :raises ValueError: If the metadata is not a non-empty typed list.
    """

    raw: object = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(raw, list) or not raw:
        raise ValueError("execution specifications must be a non-empty array")
    specifications: list[ExecutionSpec] = []
    for item in raw:
        if not isinstance(item, dict):
            raise ValueError("each execution specification must be an object")
        surface = item.get("surface")
        operation = item.get("operation")
        assertions = item.get("assertions")
        if (
            not isinstance(surface, str)
            or not surface.strip()
            or not isinstance(operation, str)
            or not operation.strip()
            or not isinstance(assertions, list)
            or not assertions
            or not all(isinstance(assertion, str) and assertion.strip() for assertion in assertions)
        ):
            raise ValueError("execution specification fields must be non-empty typed values")
        specifications.append({"surface": surface, "operation": operation, "assertions": assertions})
    return specifications


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
