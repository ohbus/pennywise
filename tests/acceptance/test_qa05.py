"""Deterministic public-interface tests for QA-05 edge-case journeys."""

import http.server
import json
import threading
import unittest
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parent))
try:
    from . import qa05
except ImportError:
    import qa05


class EdgeCaseHandler(http.server.BaseHTTPRequestHandler):
    """Contract-shaped server used without application internals."""

    renames = 0
    expected_token = "test-user"

    def log_message(self, _format: str, *_args: object) -> None:
        pass

    def _reply(self, status: int, body: object) -> None:
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        self.wfile.write(json.dumps(body).encode())

    def do_PATCH(self) -> None:
        if self.headers.get("Authorization") != f"Bearer {self.expected_token}":
            self._reply(401, {"code": "UNAUTHENTICATED"})
            return
        if self.headers.get("X-Acceptance-Fault") == "rollback":
            self._reply(409, {"code": "TEST_ROLLBACK"})
            return
        EdgeCaseHandler.renames += 1
        self._reply(409 if EdgeCaseHandler.renames > 1 else 200, {"revision": 1})

    def do_GET(self) -> None:
        if self.headers.get("Authorization") != f"Bearer {self.expected_token}":
            self._reply(401, {"code": "UNAUTHENTICATED"})
        elif self.path == "/expense-core/v1/groups":
            self._reply(200, [{"groupId": "00000000-0000-0000-0000-000000000001"}])
        else:
            self._reply(200, {"members": []})

    def do_POST(self) -> None:
        length = int(self.headers.get("Content-Length", 0))
        query = json.loads(self.rfile.read(length)).get("query", "")
        if "fanoutFailure" in query:
            self._reply(200, {"errors": [{"message": "upstream unavailable"}]})
        else:
            self._reply(200, {"data": {"groups": []}})


class Qa05Test(unittest.TestCase):
    """Exercise all QA-05 journey classifications through HTTP."""

    @classmethod
    def setUpClass(cls) -> None:
        EdgeCaseHandler.renames = 0
        cls.server = http.server.ThreadingHTTPServer(("127.0.0.1", 0), EdgeCaseHandler)
        cls.thread = threading.Thread(target=cls.server.serve_forever, daemon=True)
        cls.thread.start()

    @classmethod
    def tearDownClass(cls) -> None:
        cls.server.shutdown()
        cls.server.server_close()

    def test_edge_case_journeys(self) -> None:
        EdgeCaseHandler.expected_token = "ci-signed-token"
        results = qa05.run_qa05_journeys(
            f"http://127.0.0.1:{self.server.server_port}",
            bearer_token="ci-signed-token",
        )
        self.assertEqual([result.id for result in results], [
            "QA05-ROLLBACK", "QA05-CONCURRENCY", "QA05-AUTHORIZATION", "QA05-BFF-FANOUT", "QA05-RECOVERY"
        ])
        self.assertEqual([result.status for result in results], ["passed", "passed", "passed", "passed", "passed"])


if __name__ == "__main__":
    unittest.main()
