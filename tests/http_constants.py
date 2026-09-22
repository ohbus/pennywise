"""Canonical HTTP header names and media types used by test clients."""

from typing import Final

ACCEPT: Final[str] = "Accept"
AUTHORIZATION: Final[str] = "Authorization"
CONTENT_TYPE: Final[str] = "Content-Type"
CONTENT_LENGTH: Final[str] = "Content-Length"
IDEMPOTENCY_KEY: Final[str] = "Idempotency-Key"
ACCEPTANCE_FAULT: Final[str] = "X-Acceptance-Fault"
REQUIRED_WATERMARK: Final[str] = "X-Pennywise-Required-Watermark"
WRITER_WATERMARK: Final[str] = "X-Pennywise-Writer-Watermark"
APPLICATION_JSON: Final[str] = "application/json"
TEXT_CSV: Final[str] = "text/csv"
BEARER_PREFIX: Final[str] = "Bearer "
GRAPHQL_PATH: Final[str] = "/graphql"
