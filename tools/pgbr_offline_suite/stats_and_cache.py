"""Offline Session Stats Aggregator (SYS-02) & Versioned Bounded Cache Migration Engine (DATA-03 / SYS-05).

- Computes 1h/24h rolling rates with explicit denominators (caught/seen, XP/h, dust/h).
- Deduplicates overlapping session samples by timestamp.
- Versioned schema migration (v1 -> v2 -> v3) with bounded LRU cache cap,
  SHA-256 verification, and rollback on failure.
"""
from __future__ import annotations

from collections import OrderedDict
from dataclasses import dataclass
import hashlib
import json
import math
from typing import Any, Callable, Dict, List, Optional


@dataclass(frozen=True)
class SessionSample:
    timestamp_ms: int
    xp: int
    caught: int
    seen: int
    stops: int
    stardust_earned: int
    km: float


def aggregate_session_window(
    samples: List[SessionSample],
    window_ms: int,
    goals: Optional[Dict[str, float]] = None,
) -> Dict[str, Any]:
    """Aggregate deduplicated chronological samples over a window with explicit denominators (SYS-02)."""
    if isinstance(window_ms, bool) or not isinstance(window_ms, int) or window_ms <= 0:
        raise ValueError("window_ms deve ser > 0.")
    dedup: Dict[int, SessionSample] = {}
    for s in samples:
        if isinstance(s.km, bool) or not math.isfinite(float(s.km)) or s.timestamp_ms <= 0 or s.xp < 0 or s.caught < 0 or s.seen < 0 or s.stops < 0 or s.km < 0:
            raise ValueError("Amostra de sessão inválida.")
        dedup[s.timestamp_ms] = s
    ordered = [dedup[k] for k in sorted(dedup.keys())]
    if len(ordered) < 2:
        return {"samples": len(ordered), "covered_ms": 0, "rates": {}, "denominators": {}, "goals": {}}

    newest = ordered[-1]
    cutoff = newest.timestamp_ms - window_ms
    in_win = [s for s in ordered if s.timestamp_ms >= cutoff]
    if len(in_win) < 2:
        in_win = ordered[-2:]
    first = in_win[0]
    covered_ms = max(0, newest.timestamp_ms - first.timestamp_ms)
    hours = covered_ms / 3_600_000.0 if covered_ms > 0 else 0.0

    d_xp = max(0, newest.xp - first.xp)
    d_caught = max(0, newest.caught - first.caught)
    d_seen = max(0, newest.seen - first.seen)
    d_stops = max(0, newest.stops - first.stops)
    d_dust = max(0, newest.stardust_earned - first.stardust_earned)
    d_km = round(max(0.0, newest.km - first.km), 3)

    catch_rate = round(d_caught / d_seen, 4) if d_seen > 0 else None
    rates = {
        "xp_per_hour": round(d_xp / hours, 2) if hours > 0 else 0.0,
        "stardust_per_hour": round(d_dust / hours, 2) if hours > 0 else 0.0,
        "stops_per_hour": round(d_stops / hours, 2) if hours > 0 else 0.0,
        "km_per_hour": round(d_km / hours, 3) if hours > 0 else 0.0,
        "catch_rate": catch_rate,
    }
    deltas = {"xp": d_xp, "caught": d_caught, "seen": d_seen, "stops": d_stops, "stardust": d_dust, "km": d_km}
    goal_status: Dict[str, Any] = {}
    for k, target in (goals or {}).items():
        if k in deltas and target > 0:
            goal_status[k] = {
                "current": deltas[k],
                "target": target,
                "percent": round(min(100.0, (deltas[k] / target) * 100.0), 2),
                "met": deltas[k] >= target,
            }
    return {
        "samples": len(ordered),
        "covered_ms": covered_ms,
        "deltas": deltas,
        "rates": rates,
        "denominators": {"catch_rate_seen": d_seen, "hours": round(hours, 4)},
        "goals": goal_status,
        "device_tested": False,
    }


def migrate_snapshot_schema(
    snapshot: Dict[str, Any],
    migrations: Dict[int, Callable[[Dict[str, Any]], Dict[str, Any]]],
    target_version: int,
) -> Dict[str, Any]:
    """Run deterministic, idempotent sequential schema migrations with deep-copy isolation (DATA-03)."""
    if isinstance(target_version, bool) or not isinstance(target_version, int) or target_version < 1:
        raise ValueError(f"target_version inválida: {target_version}")
    if not isinstance(snapshot, dict) or "schema_version" not in snapshot:
        raise ValueError("Snapshot sem schema_version.")
    cur_ver = snapshot["schema_version"]
    if isinstance(cur_ver, bool) or not isinstance(cur_ver, int) or cur_ver < 1 or cur_ver > target_version:
        raise ValueError(f"Versão de schema inválida ou futura: {cur_ver} > {target_version}")
    working = json.loads(json.dumps(snapshot, allow_nan=False))
    if working["schema_version"] == target_version:
        return working
    while working["schema_version"] < target_version:
        v = working["schema_version"]
        if v not in migrations:
            raise ValueError(f"Migração ausente para versão {v} -> {v + 1}")
        nxt = migrations[v](json.loads(json.dumps(working, allow_nan=False)))
        if not isinstance(nxt, dict):
            raise ValueError(f"Migração {v} -> {v + 1} deve retornar dict.")
        nxt["schema_version"] = v + 1
        working = nxt
    return working


class BoundedOfflineCache:
    """Namespace-isolated, byte- and entry-bounded LRU cache with SHA-256 entry integrity (DATA-03 / SYS-05)."""

    MAX_KEY_LEN = 256

    def __init__(self, max_bytes: int, *, max_entries: int = 10_000) -> None:
        if isinstance(max_bytes, bool) or not isinstance(max_bytes, int) or max_bytes <= 0:
            raise ValueError("max_bytes deve ser > 0.")
        if isinstance(max_entries, bool) or not isinstance(max_entries, int) or max_entries <= 0:
            raise ValueError("max_entries deve ser > 0.")
        self.max_bytes = max_bytes
        self.max_entries = max_entries
        self._entries: "OrderedDict[str, Dict[str, Any]]" = OrderedDict()
        self._used_bytes = 0

    @classmethod
    def _ns_key(cls, namespace: str, key: str) -> str:
        if not isinstance(namespace, str) or not isinstance(key, str):
            raise ValueError("Namespace e chave devem ser strings.")
        ns = namespace.strip()
        k = key.strip()
        if not ns or not k:
            raise ValueError("Namespace e chave não podem ser vazios.")
        full = f"{ns}:{k}"
        if len(full) > cls.MAX_KEY_LEN:
            raise ValueError(f"Chave excede limite de {cls.MAX_KEY_LEN} caracteres.")
        return full

    def put(self, namespace: str, key: str, value: Any) -> int:
        raw = json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=False, allow_nan=False).encode("utf-8")
        size = len(raw)
        if size > self.max_bytes:
            raise ValueError("Item excede capacidade máxima do cache.")
        full_key = self._ns_key(namespace, key)
        if full_key in self._entries:
            self._used_bytes -= self._entries.pop(full_key)["size"]
        while (self._used_bytes + size > self.max_bytes or len(self._entries) >= self.max_entries) and self._entries:
            _, evicted = self._entries.popitem(last=False)
            self._used_bytes -= evicted["size"]
        digest = hashlib.sha256(raw).hexdigest()
        self._entries[full_key] = {"raw": raw, "sha256": digest, "size": size}
        self._used_bytes += size
        return size

    def get(self, namespace: str, key: str) -> Optional[Any]:
        full_key = self._ns_key(namespace, key)
        entry = self._entries.get(full_key)
        if entry is None:
            return None
        if hashlib.sha256(entry["raw"]).hexdigest() != entry["sha256"]:
            self._used_bytes -= self._entries.pop(full_key)["size"]
            raise ValueError(f"Integridade SHA-256 violada no cache para {full_key}")
        self._entries.move_to_end(full_key)
        return json.loads(entry["raw"].decode("utf-8"))

    @property
    def used_bytes(self) -> int:
        return self._used_bytes

    def count(self) -> int:
        return len(self._entries)
