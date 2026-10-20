"""Read-only latency samples: no real accounts, tokens, passwords or response bodies are logged."""
import json
import time
import urllib.error
import urllib.request

BASE = "https://wtcsync-api.onrender.com"
checks = [
    ("health_first", "/api/health", None, 90),
    ("login_invalid_first", "/api/auth/login", {"email": "latency-check@example.invalid", "password": "invalid-test-only"}, 30),
    ("health_repeat", "/api/health", None, 30),
    ("login_invalid_repeat", "/api/auth/login", {"email": "latency-check@example.invalid", "password": "invalid-test-only"}, 30),
]
for name, path, body, timeout in checks:
    started = time.perf_counter()
    try:
        request = urllib.request.Request(BASE + path, data=None if body is None else json.dumps(body).encode(),
            headers={"Content-Type": "application/json"})
        with urllib.request.urlopen(request, timeout=timeout) as response:
            status = response.status
            response.read()
    except urllib.error.HTTPError as error:
        status = error.code
        error.close()
    except Exception as error:
        status = type(error).__name__
    print(json.dumps({"check": name, "status": status, "seconds": round(time.perf_counter() - started, 3)}), flush=True)
