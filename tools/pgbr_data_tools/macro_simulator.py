"""Offline Local Routine/Macro Editor, Safe Simulator, Seasonal Templates & Sanitized History (AUTO-09..AUTO-14).

Strictly offline planning/data routines only:
- Explicit capability allowlist (notify, refresh_cache, evaluate_checklist, export_summary, log_note).
- Unknown or irreversible game actions are ALWAYS rejected (fail-closed).
- Bounded loop/step/timeout limits, pause/stop/cancel, and sanitized export (strips tokens/coords).
"""
from __future__ import annotations

from dataclasses import dataclass, field, asdict
from datetime import datetime, timezone
import json
import math
import re
from typing import Any, Dict, List, Optional, Sequence, Set
from zoneinfo import ZoneInfo

SAFE_CAPABILITIES: Set[str] = {
    "notify_local",
    "refresh_offline_cache",
    "evaluate_checklist",
    "export_sanitized_summary",
    "log_note",
}

SENSITIVE_KEY_PATTERN = re.compile(
    r"(token|secret|password|passwd|cookie|auth|api_key|apikey|lat|lon|lng|coord|email)",
    re.IGNORECASE,
)


@dataclass
class MacroCard:
    card_id: str
    action: str
    label: str
    params: Dict[str, Any] = field(default_factory=dict)
    repeat: int = 1
    condition_flag: Optional[str] = None


@dataclass
class MacroRoutine:
    routine_id: str
    name: str
    description: str
    revision: str
    cards: List[MacroCard]
    favorite: bool = False
    schema_version: int = 1

    def duplicate(self, new_id: str, new_name: str) -> "MacroRoutine":
        return MacroRoutine(
            routine_id=new_id,
            name=new_name,
            description=self.description,
            revision=self.revision,
            cards=[MacroCard(**asdict(c)) for c in self.cards],
            favorite=False,
            schema_version=self.schema_version,
        )

    def to_json(self) -> str:
        return json.dumps(asdict(self), ensure_ascii=False, indent=2, allow_nan=False)

    @classmethod
    def from_json(cls, raw: str) -> "MacroRoutine":
        payload = json.loads(raw)
        if payload.get("schema_version") != 1:
            raise ValueError("Unsupported macro schema_version")
        cards = [MacroCard(**c) for c in payload.get("cards", [])]
        return cls(
            routine_id=str(payload["routine_id"]),
            name=str(payload["name"]),
            description=str(payload.get("description", "")),
            revision=str(payload.get("revision", "1.0")),
            cards=cards,
            favorite=bool(payload.get("favorite", False)),
            schema_version=1,
        )


def validate_routine_safety(routine: MacroRoutine, *, max_repeat: int = 10, max_cards: int = 30) -> List[str]:
    """Return list of safety violations; empty list means safe for offline simulation (AUTO-14)."""
    errors: List[str] = []
    if not routine.routine_id.strip() or not routine.name.strip():
        errors.append("Identidade ou nome da rotina vazio")
    if len(routine.cards) > max_cards:
        errors.append(f"Excede limite de cartões ({len(routine.cards)} > {max_cards})")
    seen_card_ids: set[str] = set()
    for card in routine.cards:
        if card.card_id in seen_card_ids:
            errors.append(f"card_id duplicado na rotina: {card.card_id}")
        seen_card_ids.add(card.card_id)
        if card.action not in SAFE_CAPABILITIES:
            errors.append(f"Ação não permitida ou desconhecida: {card.action}")
        if isinstance(card.repeat, bool) or not isinstance(card.repeat, int) or card.repeat < 1 or card.repeat > max_repeat:
            errors.append(f"Repetição fora do limite em {card.card_id}: {card.repeat}")
    return errors


def simulate_routine_offline(
    routine: MacroRoutine,
    *,
    flags: Dict[str, bool] | None = None,
    max_steps: int = 50,
    stop_after_step: Optional[int] = None,
) -> Dict[str, Any]:
    """Deterministically simulate a local routine offline with bounded steps & interruption (AUTO-10)."""
    violations = validate_routine_safety(routine)
    if violations:
        return {
            "status": "REJECTED",
            "reason": "safety_violation",
            "violations": violations,
            "steps_executed": 0,
            "trace": [],
        }
    env_flags = flags or {}
    trace: List[Dict[str, Any]] = []
    steps = 0
    for card in routine.cards:
        if card.condition_flag is not None and not env_flags.get(card.condition_flag, False):
            trace.append({"card_id": card.card_id, "action": card.action, "outcome": "skipped_condition"})
            continue
        for iteration in range(1, card.repeat + 1):
            if stop_after_step is not None and steps >= stop_after_step:
                return {
                    "status": "INTERRUPTED",
                    "reason": "user_stop",
                    "steps_executed": steps,
                    "trace": trace,
                }
            if steps >= max_steps:
                return {
                    "status": "TIMEOUT",
                    "reason": "max_steps_reached",
                    "steps_executed": steps,
                    "trace": trace,
                }
            steps += 1
            trace.append({
                "step": steps,
                "card_id": card.card_id,
                "action": card.action,
                "iteration": iteration,
                "outcome": "simulated_ok",
            })
    return {
        "status": "COMPLETED",
        "reason": "ok",
        "steps_executed": steps,
        "trace": trace,
    }


@dataclass(frozen=True)
class SeasonalEventTemplate:
    template_id: str
    title: str
    revision: str
    iana_timezone: str
    start_local_iso: str
    end_local_iso: str
    checklist: Tuple[str, ...]
    bonuses: Tuple[str, ...] = ()

    def is_active_at(self, now_utc: Optional[datetime] = None) -> bool:
        """Evaluate active window in template's IANA timezone; rejects naive now_utc and DST ambiguity (AUTO-11)."""
        from .events_planner import resolve_local_datetime

        if now_utc is not None and now_utc.tzinfo is None:
            raise ValueError("now_utc exige tzinfo explícito (UTC ou offset).")
        ref_utc = (now_utc or datetime.now(timezone.utc)).astimezone(timezone.utc)
        start_utc = resolve_local_datetime(self.start_local_iso, self.iana_timezone)["utc"]
        end_utc = resolve_local_datetime(self.end_local_iso, self.iana_timezone)["utc"]
        if end_utc <= start_utc:
            raise ValueError("Janela sazonal inválida: término <= início.")
        return start_utc <= ref_utc < end_utc


_FREE_EMAIL_RE = re.compile(r"[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}")
_FREE_TOKEN_RE = re.compile(
    r"(?i)\b(?:bearer\s+[A-Za-z0-9._~+/=-]{8,}|(?:ghp|gho|sk|pk)_[A-Za-z0-9_\-]{10,}|"
    r"AIza[0-9A-Za-z_\-]{10,}|eyJ[A-Za-z0-9_\-]{10,}\.[A-Za-z0-9_\-]{10,}\.[A-Za-z0-9_\-]{8,})"
)
_FREE_COORD_RE = re.compile(
    r"(?<!\d)-?(?:[1-8]?\d(?:\.\d{3,})|90\.0+)\s*,\s*-?(?:1[0-7]\d(?:\.\d{3,})|[1-9]?\d(?:\.\d{3,})|180\.0+)(?!\d)"
)
_FREE_HOST_PATH_RE = re.compile(r"(?:[A-Za-z]:\\Users\\[^\s\"']+|/(?:Users|home)/[A-Za-z0-9._-]+(?:/[^\s\"']*)?)")


def sanitize_export_record(obj: Any) -> Any:
    """Recursively strip credentials, tokens, emails, coordinates, and host paths in keys and free text (AUTO-13)."""
    if isinstance(obj, dict):
        out: Dict[str, Any] = {}
        for k, v in obj.items():
            if SENSITIVE_KEY_PATTERN.search(str(k)):
                out[k] = "[REDACTED]"
            else:
                out[k] = sanitize_export_record(v)
        return out
    if isinstance(obj, list):
        return [sanitize_export_record(item) for item in obj]
    if isinstance(obj, tuple):
        return [sanitize_export_record(item) for item in obj]
    if isinstance(obj, str):
        text = _FREE_EMAIL_RE.sub("[REDACTED_EMAIL]", obj)
        text = _FREE_TOKEN_RE.sub("[REDACTED_TOKEN]", text)
        text = _FREE_COORD_RE.sub("[REDACTED_COORD]", text)
        text = _FREE_HOST_PATH_RE.sub("[REDACTED_PATH]", text)
        return text
    if isinstance(obj, float) and not math.isfinite(obj):
        raise ValueError("Valores NaN ou infinito não são permitidos na exportação.")
    return obj
