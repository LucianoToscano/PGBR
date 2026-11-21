#!/usr/bin/env python3
"""LT025: minimally test documented public PokeList and iFlowGo feed contracts.

Read-only probes, no tokens, no account data, no retries, no redirects,
no fake mobile browser headers or anti-bot circumvention. No row values,
identifiers, spawn coordinates or server bodies in log output.
"""
import json
import urllib.error
import urllib.parse
import urllib.request

MAX_BYTES = 262_144
TIMEOUT = 12

class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None

def probe(name, url, body=None):
    headers = {"User-Agent": "PGBR-LT025-contract-probe/1.0", "Accept": "application/json"}
    if body is not None:
        headers["Content-Type"] = "application/x-www-form-urlencoded"
    request = urllib.request.Request(url, data=body, headers=headers,
                                     method="POST" if body is not None else "GET")
    try:
        with urllib.request.build_opener(NoRedirect).open(request, timeout=TIMEOUT) as response:
            status = response.status
            content_type = response.headers.get("Content-Type", "").split(";", 1)[0].lower()
            payload = response.read(MAX_BYTES + 1)
    except urllib.error.HTTPError as exc:
        print(f"{name}: blocked_or_failed HTTP_{exc.code} (no retry, no bypass)")
        return
    except (urllib.error.URLError, TimeoutError, OSError) as exc:
        print(f"{name}: unavailable {type(exc).__name__}")
        return
    if len(payload) > MAX_BYTES:
        print(f"{name}: response_too_large HTTP_{status}")
        return
    try:
        result = json.loads(payload.decode("utf-8"))
    except (ValueError, UnicodeError):
        print(f"{name}: not_json HTTP_{status} MIME={content_type}")
        return
    if isinstance(result, dict):
        # Static field names only: never disclose source payload values.
        keys = [k for k in ("pokemons", "results", "data", "spawns", "raids", "items",
                             "total", "recordsTotal", "error", "message")
                if k in result]
        sizes = {k: len(result[k]) for k in keys if isinstance(result[k], list)}
        print(f"{name}: HTTP_{status} JSON_object fields={','.join(keys)} list_lengths={sizes}")
        for group in ("spawns", "raids"):
            arr = result.get(group)
            if isinstance(arr, list) and arr and isinstance(arr[0], dict):
                # Only field names/types; no coordinates, IV, encounter IDs or private values.
                fields = sorted((str(k), type(v).__name__) for k, v in arr[0].items())
                print(f"{name}: {group}_schema={fields[:45]}")
    elif isinstance(result, list):
        print(f"{name}: HTTP_{status} JSON_list count={len(result)}")
    else:
        print(f"{name}: HTTP_{status} JSON_{type(result).__name__}")

def main():
    # One bounded spawn request and one bounded raid request, separate layers.
    # No unauthorized access attempts or retries.
    for layer in ("spawns", "raids"):
        query = urllib.parse.urlencode({
            "lat": "40.7590", "lon": "-73.9855",
            "radius_km": "3", "layers": layer, "limit": "5"
        })
        probe("IFLOWGO_" + layer.upper(),
              "https://pokecoords.iflowgo.com/iflowgopokecoords/api/v1/nearby?" + query)

if __name__ == "__main__":
    main()
