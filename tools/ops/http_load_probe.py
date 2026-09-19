#!/usr/bin/env python3
"""Run a small dependency-free HTTP load probe and emit JSON evidence."""
from __future__ import annotations
import argparse, json, statistics, threading, time, urllib.error, urllib.request

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("url")
    parser.add_argument("--concurrency", type=int, default=4)
    parser.add_argument("--duration", type=float, default=10.0)
    args = parser.parse_args()
    if args.concurrency < 1 or args.duration <= 0:
        parser.error("concurrency must be positive and duration must be greater than zero")
    lock = threading.Lock()
    latencies: list[float] = []
    statuses: dict[str, int] = {}
    started = time.monotonic()
    def worker() -> None:
        while time.monotonic() - started < args.duration:
            began = time.monotonic()
            try:
                with urllib.request.urlopen(args.url, timeout=5) as response:
                    status = str(response.status); response.read(1)
            except (OSError, urllib.error.URLError) as error:
                status = f"error:{type(error).__name__}"
            with lock:
                latencies.append(time.monotonic() - began)
                statuses[status] = statuses.get(status, 0) + 1
    threads = [threading.Thread(target=worker, daemon=True) for _ in range(args.concurrency)]
    for thread in threads: thread.start()
    for thread in threads: thread.join()
    elapsed = max(time.monotonic() - started, 0.001)
    print(json.dumps({"url": args.url, "durationSeconds": round(elapsed, 3), "concurrency": args.concurrency,
        "requests": len(latencies), "throughputPerSecond": round(len(latencies) / elapsed, 3),
        "errorRequests": sum(v for k, v in statuses.items() if k.startswith("error:") or k.startswith("5")),
        "latencyMs": {"p50": round(statistics.median(latencies) * 1000, 3) if latencies else None,
                      "max": round(max(latencies) * 1000, 3) if latencies else None}, "statuses": statuses}, sort_keys=True))
    return 0
if __name__ == "__main__": raise SystemExit(main())
