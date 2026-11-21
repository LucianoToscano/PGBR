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
    elif isinstance(result, list):
        print(f"{name}: HTTP_{status} JSON_list count={len(result)}")
    else:
        print(f"{name}: HTTP_{status} JSON_{type(result).__name__}")

def main():
    moonani_post = urllib.parse.urlencode({
        "iv": "100", "pvp": "0", "pokemons": "",
        "start": "0", "length": "5", "draw": "1"
    }).encode("ascii")
    probe("MOONANI_AJAX",
          "https://moonani.com/PokeList/ajax.php?page=pokemon&action=load",
          moonani_post)
    query = urllib.parse.urlencode({"lat": "40.7", "lon": "-89.65",
                                    "radius_km": "1", "layers": "spawns,raids",
                                    "limit": "5"})
    probe("IFLOWGO_NEARBY",
          "https://pokecoords.iflowgo.com/iflowgopokecoords/api/v1/nearby?" + query)

if __name__ == "__main__":
    main()
