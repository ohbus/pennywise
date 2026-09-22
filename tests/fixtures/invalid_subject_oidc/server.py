"""Minimal test-only OIDC issuer for malformed-subject validation coverage."""

from __future__ import annotations

import base64
import json
import os
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from typing import Final
from tests.http_constants import APPLICATION_JSON, CONTENT_LENGTH, CONTENT_TYPE

from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.primitives.hashes import SHA256
from cryptography.hazmat.primitives.serialization import Encoding, PublicFormat
from cryptography.hazmat.primitives.asymmetric.padding import PKCS1v15


ISSUER: Final[str] = os.environ.get("INVALID_SUBJECT_OIDC_ISSUER", "http://invalid-subject-oidc:8080")
AUDIENCE: Final[str] = "pennywise-api"
PRIVATE_KEY = rsa.generate_private_key(public_exponent=65537, key_size=2048)
PUBLIC_KEY = PRIVATE_KEY.public_key()
KEY_ID: Final[str] = "invalid-subject-test-key"


def encoded(value: bytes) -> str:
    """Encode a JWT or JWK integer without padding."""
    return base64.urlsafe_b64encode(value).decode("ascii").rstrip("=")


def integer_bytes(value: int) -> bytes:
    """Encode a non-negative RSA integer in unsigned big-endian form."""
    return value.to_bytes((value.bit_length() + 7) // 8, "big")


def encode_json(value: object) -> str:
    """Serialize a JWT segment as unpadded base64url JSON."""
    return encoded(json.dumps(value, separators=(",", ":")).encode())


def token() -> str:
    """Create a signed token whose subject is deliberately whitespace-only."""
    now = int(time.time())
    header = {"alg": "RS256", "kid": KEY_ID, "typ": "JWT"}
    payload = {
        "iss": ISSUER,
        "aud": [AUDIENCE],
        "sub": "   ",
        "iat": now,
        "exp": now + 300,
        "scope": "openid",
    }
    signing_input = f"{encode_json(header)}.{encode_json(payload)}".encode("ascii")
    signature = PRIVATE_KEY.sign(signing_input, PKCS1v15(), SHA256())
    return f"{signing_input.decode('ascii')}.{encoded(signature)}"


class Handler(BaseHTTPRequestHandler):
    """Serve only the OIDC discovery, JWKS, and deterministic token endpoint."""

    def do_GET(self) -> None:  # noqa: N802
        """Return discovery metadata or the public signing key."""
        if self.path == "/.well-known/openid-configuration":
            self.send_json({"issuer": ISSUER, "jwks_uri": f"{ISSUER}/jwks"})
            return
        if self.path == "/jwks":
            numbers = PUBLIC_KEY.public_numbers()
            self.send_json({"keys": [{"kty": "RSA", "use": "sig", "alg": "RS256", "kid": KEY_ID,
                                      "n": encoded(integer_bytes(numbers.n)), "e": encoded(integer_bytes(numbers.e))}]})
            return
        self.send_error(404)

    def do_POST(self) -> None:  # noqa: N802
        """Return a signed malformed-subject access token."""
        if self.path != "/token":
            self.send_error(404)
            return
        self.send_json({"access_token": token(), "token_type": "Bearer", "expires_in": 300})

    def send_json(self, value: object) -> None:
        """Write a JSON response with an explicit content length."""
        body = json.dumps(value, separators=(",", ":")).encode()
        self.send_response(200)
        self.send_header(CONTENT_TYPE, APPLICATION_JSON)
        self.send_header(CONTENT_LENGTH, str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, format: str, *args: object) -> None:
        """Suppress request logs so test output never resembles credential logging."""


if __name__ == "__main__":
    ThreadingHTTPServer(("0.0.0.0", 8080), Handler).serve_forever()
