#!/usr/bin/env python3
"""One-request, read-only live probe of the PokeList/Moonani endpoint.

Uses a public reference location and a single species. Logs *only aggregate
validation results*, never coordinates, spawn IDs, response bodies or secrets.
Neither bypasses auth nor retries rate limits.
"""
import datetime as dt
import json
import math
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

ENDPOINT = "https://moonani.com/PokeList/api.v2.php"
MAX_BYTES = 512_000
TIMEOUT_SECONDS = 18
REFERENCE = {"type": "pokemon", "mons": "25", "lat": "-23.550520",
             "lng": "-46.633310", "distance": "1"}

def parse_epoch(value):
    """Return UTC milliseconds only for clearly recognized Unix timestamps."""
    try:
        number = int(value)
    except (TypeError, ValueError, OverflowError):
        return None
    if 1_000_000_000 <= number < 10_000_000_000:
        return number * 1000
    if 1_000_000_000_000 <= number < 10_000_000_000_000:
        return number
    return None

def summarize(payload, now_ms):
    if not isinstance(payload, dict):
        return {"schema_valid": False, "problem": "non_object_json"}
    if str(payload.get("type", "pokemon")).lower() not in {"pokemon", ""}:
        return {"schema_valid": False, "problem": "unexpected_type"}
    rows = payload.get("pokemons")
    if not isinstance(rows, list):
        return {"schema_valid": False, "problem": "missing_pokemons_array"}
    stats = {"schema_valid": True, "rows": len(rows), "valid_coords": 0,
             "unexpired": 0, "expiry_known": 0, "dex25": 0, "unknown_expiry": 0}
    for item in rows[:3000]:
        if not isinstance(item, dict):
            continue
        try:
            lat, lon = float(item.get("latitude")), float(item.get("longitude"))
            dex = int(item.get("number"))
        except (TypeError, ValueError, OverflowError):
            continue
        if not (math.isfinite(lat) and math.isfinite(lon) and -90 <= lat <= 90
                and -180 <= lon <= 180 and (lat, lon) != (0.0, 0.0)
                and 1 <= dex <= 1025):
            continue
        stats["valid_coords"] += 1
        if dex == 25:
            stats["dex25"] += 1
        end = parse_epoch(item.get("endTime"))
        if end is None:
            stats["unknown_expiry"] += 1
        else:
            stats["expiry_known"] += 1
            if end > now_ms:
                stats["unexpired"] += 1
    return stats

class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None

def main():
    query = urllib.parse.urlencode(REFERENCE)
    request = urllib.request.Request(
        ENDPOINT + "?" + query,
        headers={"Accept": "application/json", "User-Agent": "PGBR-feed-contract-check/1.0"},
        method="GET",
    )
    opener = urllib.request.build_opener(NoRedirect)
    try:
        with opener.open(request, timeout=TIMEOUT_SECONDS) as response:
            status = response.status
            mime = response.headers.get("Content-Type", "").split(";", 1)[0].strip()
            raw = response.read(MAX_BYTES + 1)
    except urllib.error.HTTPError as error:
        print(f"RESULT=not_confirmed HTTP_STATUS={error.code} (no retry, no bypass)")
        return 2
    except (urllib.error.URLError, TimeoutError, OSError) as error:
        print(f"RESULT=not_confirmed TRANSPORT_ERROR={type(error).__name__}")
        return 2
    print(f"HTTP_STATUS={status} MIME_TYPE={mime} RESPONSE_BYTES={len(raw)}")
    if len(raw) > MAX_BYTES:
        print("RESULT=not_confirmed RESPONSE_LIMIT_EXCEEDED")
        return 2
    try:
        payload = json.loads(raw.decode("utf-8"))
    except (ValueError, UnicodeError):
        print("RESULT=not_confirmed NOT_JSON")
        return 2
    summary = summarize(payload, int(time.time() * 1000))
    # Aggregate counts only. Do not log actual spawn coordinates.
    print("SCHEMA_RESULT=" + json.dumps(summary, sort_keys=True, ensure_ascii=False))
    if not summary["schema_valid"]:
        print("RESULT=not_confirmed INVALID_SCHEMA")
        return 2
    if summary["valid_coords"] == 0:
        print("RESULT=endpoint_reachable_no_coordinates_observed")
        return 0
    if summary["unexpired"] == 0:
        print("RESULT=valid_coordinates_but_freshness_unverified")
        return 0
    print("RESULT=live_valid_coordinates_observed")
    return 0

if __name__ == "__main__":
    sys.exit(main())
