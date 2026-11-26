"""Offline Events, Research Checklists, Bonus Windows, Local Reminders & Sanitization (EVT-01..08 / SYS-03).

Strictly offline planning and data-sanitization toolkit:
- Zero network calls, zero Android runtime mutations, zero scheduled game actions.
- Expired events are NEVER reported as active.
- Checklist progress is strictly user-confirmed (unknown != completed).
- Diagnostic/export sanitizer redacts PII, coordinates, emails, and tokens.
"""
from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime, timezone
import math
import re
from typing import Any, Dict, List, Optional, Tuple
from zoneinfo import ZoneInfo, ZoneInfoNotFoundError

VALID_EVENT_CATEGORIES = frozenset({
    "community_day",
    "spotlight_hour",
    "raid_hour",
    "go_fest",
    "go_tour",
    "wild_area",
    "safari",
    "halloween",
    "eternatus",
    "max_event",
    "rocket_takeover",
    "seasonal",
})

VALID_RESEARCH_KINDS = frozenset({"field", "timed", "special", "collection_challenge"})
VALID_CHECKLIST_STATES = frozenset({"unknown", "pending", "done"})


@dataclass(frozen=True)
class EventEntry:
    event_id: str
    title_pt: str
    category: str
    start_utc: str  # ISO-8601 UTC string, e.g. "2026-11-26T14:00:00Z"
    end_utc: str
    source: str
    bonuses: Tuple[str, ...] = ()
    exclusive_moves: Tuple[str, ...] = ()
    eligibility_note: str = "Confirmar elegibilidade na fonte oficial antes de evoluir."


def _parse_utc(iso_str: str) -> datetime:
    if not isinstance(iso_str, str) or not iso_str.strip():
        raise ValueError("Timestamp ISO-8601 ausente.")
    raw = iso_str.strip()
    if raw.endswith("Z"):
        raw = raw[:-1] + "+00:00"
    dt = datetime.fromisoformat(raw)
    if dt.tzinfo is None:
        raise ValueError("Timestamp exige fuso explícito (UTC ou offset).")
    return dt.astimezone(timezone.utc)


def resolve_iana_zone(tz_name: str) -> ZoneInfo:
    """Validate and resolve an IANA timezone name (fail-closed on unknown zones)."""
    if not isinstance(tz_name, str) or not tz_name.strip():
        raise ValueError("Fuso horário IANA obrigatório.")
    try:
        return ZoneInfo(tz_name.strip())
    except ZoneInfoNotFoundError as exc:
        raise ValueError(f"Fuso horário IANA desconhecido: {tz_name}") from exc


def resolve_local_datetime(
    local_iso: str,
    tz_name: str,
    *,
    allow_ambiguous: bool = False,
) -> Dict[str, Any]:
    """Resolve a local wall-clock ISO timestamp in an IANA zone and detect DST ambiguity or gaps."""
    if not isinstance(local_iso, str) or not local_iso.strip():
        raise ValueError("Timestamp local ISO-8601 obrigatório.")
    zone = resolve_iana_zone(tz_name)
    naive = datetime.fromisoformat(local_iso.strip())
    if naive.tzinfo is not None:
        utc_dt = naive.astimezone(timezone.utc)
        return {
            "utc": utc_dt,
            "local": utc_dt.astimezone(zone),
            "timezone": tz_name.strip(),
            "status": "explicit_offset",
            "ambiguous": False,
            "nonexistent": False,
        }
    dt0 = naive.replace(tzinfo=zone, fold=0)
    dt1 = naive.replace(tzinfo=zone, fold=1)
    rt0 = dt0.astimezone(timezone.utc).astimezone(zone)
    nonexistent = (rt0.replace(tzinfo=None) != naive)
    ambiguous = (not nonexistent) and (dt0.utcoffset() != dt1.utcoffset())
    if nonexistent:
        raise ValueError(f"Horário local inexistente (transição DST) em {tz_name}: {local_iso}")
    if ambiguous and not allow_ambiguous:
        raise ValueError(f"Horário local ambíguo (repetido em DST) em {tz_name}: {local_iso}")
    return {
        "utc": dt0.astimezone(timezone.utc),
        "local": dt0,
        "timezone": tz_name.strip(),
        "status": "ambiguous_dst" if ambiguous else "unambiguous",
        "ambiguous": ambiguous,
        "nonexistent": False,
    }


def classify_event_window(
    event: EventEntry,
    now_utc: datetime,
    tz_name: str = "America/Fortaleza",
) -> Dict[str, Any]:
    """Classify an event as 'active', 'upcoming', or 'expired' in the user's IANA zone.

    Expired events (now_utc >= end_dt) are NEVER classified as active.
    """
    if event.category not in VALID_EVENT_CATEGORIES:
        raise ValueError(f"Categoria de evento inválida: {event.category}")
    if now_utc.tzinfo is None:
        raise ValueError("now_utc deve possuir tzinfo explícito.")
    zone = resolve_iana_zone(tz_name)
    start_dt = _parse_utc(event.start_utc)
    end_dt = _parse_utc(event.end_utc)
    if end_dt <= start_dt:
        raise ValueError("Janela de evento inválida: término <= início.")
    now_norm = now_utc.astimezone(timezone.utc)
    if now_norm >= end_dt:
        status = "expired"
    elif now_norm < start_dt:
        status = "upcoming"
    else:
        status = "active"
    return {
        "event_id": event.event_id,
        "title_pt": event.title_pt,
        "category": event.category,
        "status": status,
        "local_start": start_dt.astimezone(zone).isoformat(),
        "local_end": end_dt.astimezone(zone).isoformat(),
        "timezone": tz_name,
        "source": event.source,
        "bonuses": list(event.bonuses),
        "exclusive_moves": list(event.exclusive_moves),
        "eligibility_note": event.eligibility_note,
    }


def partition_schedule(
    events: List[EventEntry],
    now_utc: datetime,
    tz_name: str = "America/Fortaleza",
) -> Dict[str, List[Dict[str, Any]]]:
    """Partition events into active, upcoming, and expired (expired never leaks into active)."""
    out: Dict[str, List[Dict[str, Any]]] = {"active": [], "upcoming": [], "expired": []}
    seen_ids: set[str] = set()
    for ev in events:
        if ev.event_id in seen_ids:
            raise ValueError(f"event_id duplicado na agenda: {ev.event_id}")
        seen_ids.add(ev.event_id)
        item = classify_event_window(ev, now_utc, tz_name)
        out[item["status"]].append(item)
    for bucket in out.values():
        bucket.sort(key=lambda x: (x["local_start"], x["event_id"]))
    return out


def cache_freshness(
    fetched_utc: str,
    now_utc: datetime,
    ttl_seconds: int = 3600,
    etag: Optional[str] = None,
) -> Dict[str, Any]:
    """Evaluate offline ETag/TTL cache freshness without performing network calls (EVT-05)."""
    if ttl_seconds <= 0:
        raise ValueError("ttl_seconds deve ser > 0.")
    fetched = _parse_utc(fetched_utc)
    if now_utc.tzinfo is None:
        raise ValueError("now_utc deve possuir tzinfo explícito.")
    age = (now_utc.astimezone(timezone.utc) - fetched).total_seconds()
    if age < 0:
        state = "clock_skew"
    elif age <= ttl_seconds:
        state = "fresh"
    else:
        state = "stale"
    return {
        "state": state,
        "age_seconds": max(0, int(age)),
        "ttl_seconds": ttl_seconds,
        "etag": etag,
    }


@dataclass
class ChecklistItem:
    item_id: str
    title_pt: str
    kind: str  # field, timed, special, collection_challenge
    priority: int = 2  # 1 (alta) .. 3 (baixa)
    state: str = "unknown"  # unknown | pending | done
    window_key: str = ""  # e.g. "2026-11-26" or "2026-W48"

    def __post_init__(self) -> None:
        if self.kind not in VALID_RESEARCH_KINDS:
            raise ValueError(f"Tipo de pesquisa inválido: {self.kind}")
        if self.state not in VALID_CHECKLIST_STATES:
            raise ValueError(f"Estado de checklist inválido: {self.state}")
        if not (1 <= self.priority <= 3):
            raise ValueError("Prioridade deve ser 1..3.")


class ManualChecklist:
    """User-confirmed daily/weekly research checklist with window reset (EVT-02 / EVT-07)."""

    def __init__(self) -> None:
        self._items: Dict[str, ChecklistItem] = {}

    def upsert(self, item: ChecklistItem) -> None:
        self._items[item.item_id] = item

    def confirm_state(self, item_id: str, new_state: str) -> None:
        if new_state not in VALID_CHECKLIST_STATES:
            raise ValueError(f"Estado inválido: {new_state}")
        if item_id not in self._items:
            raise KeyError(f"Item ausente: {item_id}")
        self._items[item_id].state = new_state

    def reset_window(self, window_key: str) -> int:
        """Reset all items belonging to a window back to 'pending' (never auto-done)."""
        count = 0
        for item in self._items.values():
            if item.window_key == window_key:
                item.state = "pending"
                count += 1
        return count

    def summary(self) -> Dict[str, int]:
        counts = {"done": 0, "pending": 0, "unknown": 0, "total": len(self._items)}
        for item in self._items.values():
            counts[item.state] += 1
        return counts


@dataclass
class LocalReminder:
    event_id: str
    lead_minutes: int
    tz_name: str
    label_pt: str
    opt_in: bool = True
    canceled: bool = False


class LocalReminderPlanner:
    """Opt-in local reminder registry with deduplication, edit and cancel (EVT-06).

    Produces notification metadata only; NEVER dispatches game actions.
    """

    def __init__(self) -> None:
        self._reminders: Dict[Tuple[str, int], LocalReminder] = {}

    def schedule(self, reminder: LocalReminder) -> bool:
        if not reminder.opt_in:
            return False
        if reminder.lead_minutes < 0 or reminder.lead_minutes > 10080:
            raise ValueError("Antecedência inválida (0..10080 min).")
        resolve_iana_zone(reminder.tz_name)
        key = (reminder.event_id, reminder.lead_minutes)
        if key in self._reminders and not self._reminders[key].canceled:
            return False  # deduplicated
        self._reminders[key] = reminder
        return True

    def cancel(self, event_id: str, lead_minutes: int) -> bool:
        key = (event_id, lead_minutes)
        if key not in self._reminders:
            return False
        self._reminders[key].canceled = True
        return True

    def active_reminders(self) -> List[LocalReminder]:
        return [r for r in self._reminders.values() if r.opt_in and not r.canceled]


_EMAIL_RE = re.compile(r"[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}")
_COORD_RE = re.compile(r"(?<!\d)-?(?:[1-8]?\d(?:\.\d{3,})|90\.0+)\s*,\s*-?(?:1[0-7]\d(?:\.\d{3,})|[1-9]?\d(?:\.\d{3,})|180\.0+)(?!\d)")
_TOKEN_RE = re.compile(
    r"(?i)\b(?:bearer\s+[A-Za-z0-9._~+/=-]{8,}|(?:ghp|gho|sk|pk)_[A-Za-z0-9_\-]{10,}|"
    r"AIza[0-9A-Za-z_\-]{10,}|eyJ[A-Za-z0-9_\-]{10,}\.[A-Za-z0-9_\-]{10,}\.[A-Za-z0-9_\-]{8,})"
)
_HOST_PATH_RE = re.compile(r"(?:[A-Za-z]:\\Users\\[^\s\"']+|/(?:Users|home)/[A-Za-z0-9._-]+(?:/[^\s\"']*)?)")
_SENSITIVE_KEYS = frozenset({
    "trainer", "trainer_name", "email", "account", "account_id", "token",
    "api_key", "secret", "password", "lat", "lon", "coords", "coordinates",
    "cookie", "session", "authorization", "phone", "telefone", "cpf",
})


def sanitize_diagnostic_payload(payload: Any) -> Any:
    """Recursively redact private/sensitive fields and free-text values from export data (SYS-03)."""
    if isinstance(payload, dict):
        cleaned: Dict[str, Any] = {}
        for k, v in payload.items():
            if any(tok in str(k).strip().lower() for tok in _SENSITIVE_KEYS):
                cleaned[k] = "[REDACTED]"
            else:
                cleaned[k] = sanitize_diagnostic_payload(v)
        return cleaned
    if isinstance(payload, list):
        return [sanitize_diagnostic_payload(item) for item in payload]
    if isinstance(payload, tuple):
        return [sanitize_diagnostic_payload(item) for item in payload]
    if isinstance(payload, str):
        text = _EMAIL_RE.sub("[EMAIL]", payload)
        text = _TOKEN_RE.sub("[TOKEN]", text)
        text = _COORD_RE.sub("[COORDS]", text)
        text = _HOST_PATH_RE.sub("[PATH]", text)
        return text
    if isinstance(payload, float) and not math.isfinite(payload):
        raise ValueError("Valores NaN ou infinito não são permitidos na exportação.")
    return payload
