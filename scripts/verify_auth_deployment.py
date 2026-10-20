"""Read-only smoke checks for the public Render deployment; no real credentials."""
import json
import time
import urllib.error
import urllib.request

BASE = "https://wtcsync-api.onrender.com"

def request(path, data=None):
    payload = None if data is None else json.dumps(data).encode()
    req = urllib.request.Request(BASE + path, data=payload, headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(req, timeout=15) as response:
            return response.status, response.read()
    except urllib.error.HTTPError as error:
        return error.code, error.read()

for attempt in range(30):
    try:
        code, body = request("/v3/api-docs")
        paths = json.loads(body).get("paths", {}) if code == 200 else {}
        if "clientMessageId" in json.loads(body).get("components", {}).get("schemas", {}).get("MessageDTO", {}).get("properties", {}):
            break
        print(f"Waiting for updated API deployment ({attempt + 1}/30)", flush=True)
    except Exception as error:
        print(f"API not ready: {type(error).__name__}", flush=True)
    time.sleep(10)
else:
    raise SystemExit("Render still exposes the old API. Confirm the linked repository, branch and auto-deploy setting.")

code, _ = request("/api/auth/login", {"email": "invalid-test@example.invalid", "password": "invalid-test-only"})
assert code == 401, f"Invalid password login should return 401, got {code}"
code, _ = request("/api/auth/me")
assert code in (401, 403), f"Anonymous profile should be rejected, got {code}"
for path in ("/api/campaigns/me/latest", "/api/clients", "/api/campaigns", "/api/segments", "/inbox/invalid-test", "/ws/info", "/ws/support", "/api/support/queue"):
    code, _ = request(path)
    assert code in (401, 403), f"Anonymous {path} must be rejected, got {code}"
print("Role access deployment verified: protected routes reject anonymous requests.", flush=True)
code, _ = request("/api/auth/google", {"idToken": "invalid-test-token"})
if code == 503:
    raise SystemExit("Updated API is deployed, but Firebase Admin is not configured on Render (Google returned 503).")
assert code == 401, f"Invalid Google token should return 401, got {code}"
print("Updated authentication routes are deployed; anonymous access and invalid credentials are rejected.")
