"""Offline Research & Rewards Catalog for Manual Consultation (G-049-10 / EVT-02).

Design guarantees:
- Pure Python stdlib, 100% offline, zero network calls, zero runtime mutation.
- Does not duplicate ResearchPreferences (Java), PR #340/#344, or 0006/0007.
- Explicitly distinguishes documented research entries from live in-game availability
  (live_availability_confirmed is ALWAYS False offline).
- Reuses tools.pgbr_data_tools.events_planner.ManualChecklist when present, with a
  strictly compatible local fallback when 0006 is not installed on main.
- Bounded local JSON import/export with O_BINARY, O_NOFOLLOW, symlink rejection,
  and PII/credential/location sanitization across both keys and free-text values.
"""

from __future__ import annotations

import copy
import json
import math
import os
import re
import stat
import unicodedata
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict, Iterable, List, Optional, Sequence, Tuple

CATALOG_SCHEMA_VERSION = 1
MAX_CATALOG_ENTRIES = 1000
MAX_EXPORT_BYTES = 1_000_000  # 1 MB hard cap for local synthetic import/export
MAX_TEXT_LEN = 240

RESEARCH_CATEGORIES: Tuple[str, ...] = ("campo", "temporaria", "especial", "colecao")
CATEGORY_LABELS_PTBR: Dict[str, str] = {
    "campo": "Campo",
    "temporaria": "Temporária",
    "especial": "Especial",
    "colecao": "Coleção",
}

REWARD_KINDS: Tuple[str, ...] = (
    "especie",
    "item",
    "poeira_estelar",
    "xp",
    "doce",
    "mega_energia",
    "desconhecido",
)

DOCUMENTATION_STATUSES: Tuple[str, ...] = (
    "documentada_sem_confirmacao_ativa",
    "disponibilidade_nao_verificada_offline",
    "expirada_por_janela_declarada",
    "sintetica_exemplo",
)

CHECKLIST_STATES: Tuple[str, ...] = ("pendente", "em_progresso", "concluido", "ignorado")

_ID_PATTERN = re.compile(r"^[A-Z0-9][A-Z0-9_.-]{1,95}$")
_CONDITION_CODE_PATTERN = re.compile(r"^[A-Z0-9][A-Z0-9_]{1,63}$")

_SENSITIVE_KEY_TOKENS: Tuple[str, ...] = (
    "token",
    "secret",
    "password",
    "passwd",
    "apikey",
    "api_key",
    "cookie",
    "session",
    "auth",
    "credential",
    "email",
    "e_mail",
    "mail",
    "phone",
    "telefone",
    "cpf",
    "trainer_name",
    "account",
    "usuario",
    "username",
    "player_id",
    "lat",
    "lon",
    "lng",
    "latitude",
    "longitude",
    "coord",
    "location",
    "localizacao",
    "gps",
    "address",
    "endereco",
    "ip",
)

_EMAIL_RE = re.compile(r"\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}\b")
_TOKEN_RE = re.compile(
    r"(?i)\b(?:bearer\s+[A-Za-z0-9._~+/=-]{8,}|(?:sk|pk|ghp|gho|AIza)[A-Za-z0-9_-]{12,}|"
    r"eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{8,})"
)
_COORD_RE = re.compile(
    r"(?<![A-Za-z0-9_])[-+]?(?:[1-8]?\d(?:\.\d{3,})|90(?:\.0{3,}))\s*,\s*"
    r"[-+]?(?:1[0-7]\d(?:\.\d{3,})|[1-9]?\d(?:\.\d{3,})|180(?:\.0{3,}))(?![A-Za-z0-9_])"
)
_HOST_PATH_RE = re.compile(
    r"(?:[A-Za-z]:\\Users\\[^\s\"']+|/(?:Users|home)/[A-Za-z0-9._-]+(?:/[^\s\"']*)?)"
)


def _is_strict_int(value: Any) -> bool:
    return isinstance(value, int) and not isinstance(value, bool)


def _is_finite_number(value: Any) -> bool:
    if isinstance(value, bool):
        return False
    if isinstance(value, (int, float)):
        return math.isfinite(float(value))
    return False


def normalize_search_text(text: str) -> str:
    """Accent-insensitive, Unicode-whitespace-normalized lowercase text for PT-BR search."""
    if not isinstance(text, str):
        raise ValueError("Texto de busca deve ser string.")
    # Remove zero-width characters before NFKD decomposition
    cleaned = re.sub(r"[\u200b-\u200f\ufeff]", "", text)
    nfkd = unicodedata.normalize("NFKD", cleaned)
    without_marks = "".join(ch for ch in nfkd if unicodedata.category(ch) != "Mn")
    # Collapse all Unicode whitespace (including NBSP \u00a0 and narrow NBSP \u202f)
    collapsed = re.sub(r"\s+", " ", without_marks, flags=re.UNICODE).strip()
    return collapsed.casefold()


def normalize_category(category: str) -> str:
    """Normalize Portuguese research category names (accent-insensitive)."""
    norm = normalize_search_text(category)
    mapping = {
        "campo": "campo",
        "field": "campo",
        "temporaria": "temporaria",
        "timed": "temporaria",
        "especial": "especial",
        "special": "especial",
        "colecao": "colecao",
        "collection": "colecao",
    }
    if norm not in mapping:
        raise ValueError(
            f"Categoria de pesquisa inválida: {category!r}. "
            f"Permitidas: {', '.join(RESEARCH_CATEGORIES)}."
        )
    return mapping[norm]


def validate_research_id(research_id: str) -> str:
    """Validate and return a stable research identifier."""
    if not isinstance(research_id, str):
        raise ValueError("research_id deve ser string.")
    stripped = research_id.strip()
    if not _ID_PATTERN.match(stripped):
        raise ValueError(
            f"research_id inválido: {research_id!r}. "
            "Use apenas A-Z, 0-9, '_', '.' ou '-' (2..96 caracteres)."
        )
    return stripped


def _parse_utc_iso(value: Optional[str], field_name: str) -> Optional[datetime]:
    if value is None:
        return None
    if not isinstance(value, str) or not value.strip():
        raise ValueError(f"{field_name} deve ser string ISO-8601 com fuso explícito ou None.")
    raw = value.strip()
    if raw.endswith("Z"):
        raw = raw[:-1] + "+00:00"
    try:
        dt = datetime.fromisoformat(raw)
    except ValueError as exc:
        raise ValueError(f"{field_name} inválido (ISO-8601 esperado): {value!r}") from exc
    if dt.tzinfo is None:
        raise ValueError(f"{field_name} exige fuso horário explícito (UTC ou offset): {value!r}")
    return dt.astimezone(timezone.utc)


def sanitize_free_text(text: str) -> str:
    """Redact emails, tokens/JWTs, GPS coordinate pairs, and local host paths in free text."""
    if not isinstance(text, str):
        return text
    out = _EMAIL_RE.sub("[REDACTED_EMAIL]", text)
    out = _TOKEN_RE.sub("[REDACTED_TOKEN]", out)
    out = _COORD_RE.sub("[REDACTED_COORD]", out)
    out = _HOST_PATH_RE.sub("[REDACTED_PATH]", out)
    return out


def sanitize_research_payload(payload: Any) -> Any:
    """Recursively redact sensitive keys and sensitive free-text values."""
    if isinstance(payload, dict):
        cleaned: Dict[str, Any] = {}
        for k, v in payload.items():
            key_str = str(k)
            key_norm = normalize_search_text(key_str).replace("-", "_")
            if any(tok in key_norm for tok in _SENSITIVE_KEY_TOKENS):
                cleaned[key_str] = "[REDACTED]"
            else:
                cleaned[key_str] = sanitize_research_payload(v)
        return cleaned
    if isinstance(payload, list):
        return [sanitize_research_payload(item) for item in payload]
    if isinstance(payload, tuple):
        return [sanitize_research_payload(item) for item in payload]
    if isinstance(payload, str):
        return sanitize_free_text(payload)
    if isinstance(payload, float) and not math.isfinite(payload):
        raise ValueError("Valores NaN ou infinito não são permitidos em payload de pesquisa.")
    return payload


@dataclass(frozen=True)
class ResearchCondition:
    """Structured research condition separated from its translated PT-BR display label."""

    code: str
    declared_label_ptbr: str
    target_count: Optional[int] = None
    parameters: Dict[str, Any] = field(default_factory=dict)

    def __post_init__(self) -> None:
        if not isinstance(self.code, str) or not _CONDITION_CODE_PATTERN.match(self.code.strip()):
            raise ValueError(f"Código de condição inválido: {self.code!r}")
        if not isinstance(self.declared_label_ptbr, str) or not self.declared_label_ptbr.strip():
            raise ValueError("declared_label_ptbr deve ser não vazio.")
        if len(self.declared_label_ptbr) > MAX_TEXT_LEN:
            raise ValueError(f"declared_label_ptbr excede {MAX_TEXT_LEN} caracteres.")
        if self.target_count is not None:
            if not _is_strict_int(self.target_count) or self.target_count <= 0:
                raise ValueError(f"target_count deve ser inteiro positivo ou None: {self.target_count!r}")
        if not isinstance(self.parameters, dict):
            raise ValueError("parameters deve ser um dicionário.")
        for v in self.parameters.values():
            if isinstance(v, float) and not math.isfinite(v):
                raise ValueError("parameters não pode conter NaN/infinito.")

    def to_dict(self) -> Dict[str, Any]:
        return {
            "code": self.code.strip(),
            "declared_label_ptbr": sanitize_free_text(self.declared_label_ptbr.strip()),
            "target_count": self.target_count,
            "parameters": sanitize_research_payload(copy.deepcopy(self.parameters)),
        }

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "ResearchCondition":
        if not isinstance(data, dict):
            raise ValueError("Condição deve ser objeto JSON.")
        return cls(
            code=data.get("code", ""),
            declared_label_ptbr=data.get("declared_label_ptbr", ""),
            target_count=data.get("target_count"),
            parameters=dict(data.get("parameters") or {}),
        )


@dataclass(frozen=True)
class ResearchReward:
    """Typed reward (species, item, stardust, XP, candy, mega energy, or explicit unknown)."""

    kind: str
    display_name_ptbr: str
    target_id: Optional[str] = None
    quantity: Optional[int] = None
    quantity_min: Optional[int] = None
    quantity_max: Optional[int] = None
    is_unknown: bool = False

    def __post_init__(self) -> None:
        if not isinstance(self.kind, str) or self.kind not in REWARD_KINDS:
            raise ValueError(f"Tipo de recompensa inválido: {self.kind!r}. Permitidos: {REWARD_KINDS}")
        if not isinstance(self.display_name_ptbr, str) or not self.display_name_ptbr.strip():
            raise ValueError("display_name_ptbr da recompensa deve ser não vazio.")
        if len(self.display_name_ptbr) > MAX_TEXT_LEN:
            raise ValueError(f"display_name_ptbr excede {MAX_TEXT_LEN} caracteres.")
        if not isinstance(self.is_unknown, bool):
            raise ValueError("is_unknown deve ser booleano.")
        if self.kind == "desconhecido" and not self.is_unknown:
            object.__setattr__(self, "is_unknown", True)
        if self.target_id is not None:
            if not isinstance(self.target_id, str) or not self.target_id.strip():
                raise ValueError("target_id deve ser string não vazia ou None.")
        for fname, val in (
            ("quantity", self.quantity),
            ("quantity_min", self.quantity_min),
            ("quantity_max", self.quantity_max),
        ):
            if val is not None:
                if not _is_strict_int(val) or val <= 0:
                    raise ValueError(f"{fname} deve ser inteiro positivo ou None: {val!r}")
        if self.quantity is not None and (self.quantity_min is not None or self.quantity_max is not None):
            raise ValueError("Use quantity fixa OU intervalo quantity_min/quantity_max, não ambos.")
        if (self.quantity_min is None) != (self.quantity_max is None):
            raise ValueError("quantity_min e quantity_max devem ser fornecidos juntos.")
        if self.quantity_min is not None and self.quantity_max is not None:
            if self.quantity_min > self.quantity_max:
                raise ValueError("quantity_min não pode ser maior que quantity_max.")

    def to_dict(self) -> Dict[str, Any]:
        return {
            "kind": self.kind,
            "target_id": self.target_id.strip() if self.target_id else None,
            "display_name_ptbr": sanitize_free_text(self.display_name_ptbr.strip()),
            "quantity": self.quantity,
            "quantity_min": self.quantity_min,
            "quantity_max": self.quantity_max,
            "is_unknown": self.is_unknown,
        }

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "ResearchReward":
        if not isinstance(data, dict):
            raise ValueError("Recompensa deve ser objeto JSON.")
        return cls(
            kind=data.get("kind", ""),
            target_id=data.get("target_id"),
            display_name_ptbr=data.get("display_name_ptbr", ""),
            quantity=data.get("quantity"),
            quantity_min=data.get("quantity_min"),
            quantity_max=data.get("quantity_max"),
            is_unknown=bool(data.get("is_unknown", False)),
        )


@dataclass(frozen=True)
class ResearchProvenance:
    """Provenance, revision, and declared validity window for a catalog record."""

    source_name: str
    source_type: str  # e.g. "sintetico_exemplo", "local_autorizado", "observado_usuario"
    revision: str
    valid_from_utc: Optional[str] = None
    valid_until_utc: Optional[str] = None
    is_synthetic: bool = True
    notes_ptbr: str = ""

    def __post_init__(self) -> None:
        if not isinstance(self.source_name, str) or not self.source_name.strip():
            raise ValueError("source_name obrigatório.")
        if not isinstance(self.source_type, str) or not self.source_type.strip():
            raise ValueError("source_type obrigatório.")
        if not isinstance(self.revision, str) or not self.revision.strip():
            raise ValueError("revision obrigatória.")
        if not isinstance(self.is_synthetic, bool):
            raise ValueError("is_synthetic deve ser booleano.")
        start_dt = _parse_utc_iso(self.valid_from_utc, "valid_from_utc")
        end_dt = _parse_utc_iso(self.valid_until_utc, "valid_until_utc")
        if start_dt is not None and end_dt is not None and start_dt > end_dt:
            raise ValueError("Janela de validade inválida: valid_from_utc > valid_until_utc.")

    def to_dict(self) -> Dict[str, Any]:
        return {
            "source_name": sanitize_free_text(self.source_name.strip()),
            "source_type": self.source_type.strip(),
            "revision": self.revision.strip(),
            "valid_from_utc": self.valid_from_utc,
            "valid_until_utc": self.valid_until_utc,
            "is_synthetic": self.is_synthetic,
            "notes_ptbr": sanitize_free_text(self.notes_ptbr.strip()),
        }

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "ResearchProvenance":
        if not isinstance(data, dict):
            raise ValueError("Proveniência deve ser objeto JSON.")
        return cls(
            source_name=data.get("source_name", ""),
            source_type=data.get("source_type", ""),
            revision=data.get("revision", ""),
            valid_from_utc=data.get("valid_from_utc"),
            valid_until_utc=data.get("valid_until_utc"),
            is_synthetic=bool(data.get("is_synthetic", True)),
            notes_ptbr=data.get("notes_ptbr", ""),
        )


@dataclass(frozen=True)
class ResearchEntry:
    """Single documented offline research entry (never implies live availability)."""

    research_id: str
    category: str
    title_ptbr: str
    conditions: Tuple[ResearchCondition, ...]
    rewards: Tuple[ResearchReward, ...]
    provenance: ResearchProvenance
    documentation_status: str = "documentada_sem_confirmacao_ativa"
    tags: Tuple[str, ...] = ()

    def __post_init__(self) -> None:
        canonical_id = validate_research_id(self.research_id)
        object.__setattr__(self, "research_id", canonical_id)
        norm_cat = normalize_category(self.category)
        object.__setattr__(self, "category", norm_cat)
        if not isinstance(self.title_ptbr, str) or not self.title_ptbr.strip():
            raise ValueError("title_ptbr obrigatório.")
        if len(self.title_ptbr) > MAX_TEXT_LEN:
            raise ValueError(f"title_ptbr excede {MAX_TEXT_LEN} caracteres.")
        if not self.conditions or not all(isinstance(c, ResearchCondition) for c in self.conditions):
            raise ValueError("conditions deve conter ao menos uma ResearchCondition válida.")
        if not self.rewards or not all(isinstance(r, ResearchReward) for r in self.rewards):
            raise ValueError("rewards deve conter ao menos uma ResearchReward válida.")
        if not isinstance(self.provenance, ResearchProvenance):
            raise ValueError("provenance deve ser ResearchProvenance.")
        if self.documentation_status not in DOCUMENTATION_STATUSES:
            raise ValueError(
                f"documentation_status inválido: {self.documentation_status!r}. "
                f"Permitidos: {DOCUMENTATION_STATUSES}"
            )
        if not isinstance(self.tags, tuple) or not all(isinstance(t, str) for t in self.tags):
            raise ValueError("tags deve ser tupla de strings.")

    def to_dict(self) -> Dict[str, Any]:
        return {
            "research_id": self.research_id,
            "category": self.category,
            "category_label_ptbr": CATEGORY_LABELS_PTBR[self.category],
            "title_ptbr": sanitize_free_text(self.title_ptbr.strip()),
            "conditions": [c.to_dict() for c in self.conditions],
            "rewards": [r.to_dict() for r in self.rewards],
            "provenance": self.provenance.to_dict(),
            "documentation_status": self.documentation_status,
            "live_availability_confirmed": False,
            "tags": [sanitize_free_text(t.strip()) for t in self.tags if t.strip()],
        }

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "ResearchEntry":
        if not isinstance(data, dict):
            raise ValueError("Entrada de pesquisa deve ser objeto JSON.")
        conditions = tuple(ResearchCondition.from_dict(c) for c in (data.get("conditions") or ()))
        rewards = tuple(ResearchReward.from_dict(r) for r in (data.get("rewards") or ()))
        provenance = ResearchProvenance.from_dict(data.get("provenance") or {})
        tags = tuple(str(t) for t in (data.get("tags") or ()))
        return cls(
            research_id=data.get("research_id", ""),
            category=data.get("category", ""),
            title_ptbr=data.get("title_ptbr", ""),
            conditions=conditions,
            rewards=rewards,
            provenance=provenance,
            documentation_status=data.get("documentation_status", "documentada_sem_confirmacao_ativa"),
            tags=tags,
        )


@dataclass(frozen=True)
class AvailabilityAssessment:
    """Explicit separation between documented status and live in-game availability."""

    research_id: str
    documented: bool
    live_availability_confirmed: bool  # Always False offline
    window_status: str  # "sem_janela_declarada" | "antes_da_janela" | "dentro_da_janela_declarada" | "expirada_por_janela"
    is_synthetic: bool
    advisory_ptbr: str


def assess_availability(
    entry: ResearchEntry,
    now_utc: Optional[datetime] = None,
) -> AvailabilityAssessment:
    """Assess declared validity window without ever inferring live in-game availability."""
    ref = now_utc if now_utc is not None else datetime.now(timezone.utc)
    if ref.tzinfo is None:
        raise ValueError("now_utc exige tzinfo explícito (UTC ou offset).")
    ref_utc = ref.astimezone(timezone.utc)

    start_dt = _parse_utc_iso(entry.provenance.valid_from_utc, "valid_from_utc")
    end_dt = _parse_utc_iso(entry.provenance.valid_until_utc, "valid_until_utc")

    if entry.provenance.is_synthetic:
        advisory = (
            "Registro sintético de exemplo para consulta/teste offline. "
            "Não representa pesquisa ativa no jogo."
        )
    else:
        advisory = (
            "Pesquisa documentada offline; modo offline não confirma disponibilidade "
            "ativa nem rotação atual no jogo."
        )

    if start_dt is None and end_dt is None:
        window_status = "sem_janela_declarada"
    elif start_dt is not None and ref_utc < start_dt:
        window_status = "antes_da_janela"
    elif end_dt is not None and ref_utc > end_dt:
        window_status = "expirada_por_janela"
    else:
        window_status = "dentro_da_janela_declarada"

    return AvailabilityAssessment(
        research_id=entry.research_id,
        documented=True,
        live_availability_confirmed=False,
        window_status=window_status,
        is_synthetic=entry.provenance.is_synthetic,
        advisory_ptbr=advisory,
    )


@dataclass(frozen=True)
class LocalSourceInventory:
    """Inventory of local repository files relevant to research and evidence boundaries."""

    repo_root_checked: bool
    research_preferences_present: bool
    research_preferences_format_version: Optional[str]
    research_preferences_categories: Tuple[str, ...]
    pokemon_names_catalog_present: bool
    pokemon_names_count: int
    authorized_embedded_research_dataset_present: bool
    supported_local_research_types: Tuple[str, ...]
    evidence_limits_ptbr: Tuple[str, ...]


def inspect_local_research_sources(repo_root: Path) -> LocalSourceInventory:
    """Read-only inventory of local authorized research sources and evidence limits."""
    root = Path(repo_root)
    if not root.exists() or not root.is_dir():
        raise ValueError("repo_root deve ser um diretório existente.")

    rp_path = root / "pgrazor" / "src" / "dev" / "pgrazor" / "ResearchPreferences.java"
    rp_present = rp_path.is_file() and not rp_path.is_symlink()
    rp_format: Optional[str] = None
    rp_cats: List[str] = []

    if rp_present:
        text = rp_path.read_text(encoding="utf-8", errors="replace")
        if 'researchCatalogFormat", "2"' in text or '"2".equals(p.getProperty("researchCatalogFormat"))' in text:
            rp_format = "2"
        if 'ITEM = "i"' in text:
            rp_cats.append("i:item")
        if 'ENCOUNTER = "e"' in text:
            rp_cats.append("e:encounter")
        if 'EVENT = "v"' in text:
            rp_cats.append("v:event")

    names_path = root / "pgrazor" / "assets" / "map-icons" / "pokemon-names.json"
    names_present = names_path.is_file() and not names_path.is_symlink()
    names_count = 0
    if names_present:
        try:
            parsed = json.loads(names_path.read_text(encoding="utf-8"))
            if isinstance(parsed, dict):
                names_count = len(parsed)
            elif isinstance(parsed, list):
                names_count = len(parsed)
        except Exception:
            names_count = 0

    limits = (
        "ResearchPreferences.java armazena apenas tarefas de pesquisa de campo observadas em execução pelo próprio usuário; não é base embarcada de tarefas reais.",
        "Não existe dataset público autorizado embarcado de pesquisas de campo/temporárias/especiais/coleção neste repositório; ausência marcada explicitamente.",
        "pgrazor/assets/map-icons/pokemon-names.json fornece apenas nomes de espécies autorizados localmente, sem tabelas de tarefas ou recompensas.",
        "Nenhum registro real de pesquisa ativa deve ser inventado ou inferido em modo offline.",
    )

    return LocalSourceInventory(
        repo_root_checked=True,
        research_preferences_present=rp_present,
        research_preferences_format_version=rp_format,
        research_preferences_categories=tuple(rp_cats),
        pokemon_names_catalog_present=names_present,
        pokemon_names_count=names_count,
        authorized_embedded_research_dataset_present=False,
        supported_local_research_types=RESEARCH_CATEGORIES,
        evidence_limits_ptbr=limits,
    )


class ResearchCatalog:
    """In-memory offline catalog with strict duplicate/unknown-ID validation and PT-BR search."""

    def __init__(self, entries: Iterable[ResearchEntry] = ()) -> None:
        self._entries: Dict[str, ResearchEntry] = {}
        for entry in entries:
            self.add(entry)

    def add(self, entry: ResearchEntry) -> None:
        if not isinstance(entry, ResearchEntry):
            raise ValueError("Esperado ResearchEntry.")
        if len(self._entries) >= MAX_CATALOG_ENTRIES:
            raise ValueError(f"Limite máximo de {MAX_CATALOG_ENTRIES} registros atingido.")
        if entry.research_id in self._entries:
            raise ValueError(f"research_id duplicado no catálogo: {entry.research_id}")
        self._entries[entry.research_id] = entry

    def get(self, research_id: str) -> ResearchEntry:
        canonical = validate_research_id(research_id)
        if canonical not in self._entries:
            raise KeyError(f"research_id desconhecido no catálogo: {canonical}")
        return self._entries[canonical]

    def __contains__(self, research_id: str) -> bool:
        try:
            canonical = validate_research_id(research_id)
        except ValueError:
            return False
        return canonical in self._entries

    def __len__(self) -> int:
        return len(self._entries)

    def all_entries(self) -> List[ResearchEntry]:
        return [self._entries[k] for k in sorted(self._entries.keys())]

    def search(
        self,
        *,
        category: Optional[str] = None,
        query: Optional[str] = None,
        reward_kind: Optional[str] = None,
        reward_target: Optional[str] = None,
        include_synthetic: bool = True,
    ) -> List[ResearchEntry]:
        """Manual PT-BR accent-insensitive search by category, title/condition, or reward species/item."""
        cat_filter = normalize_category(category) if category is not None else None
        if reward_kind is not None and reward_kind not in REWARD_KINDS:
            raise ValueError(f"Tipo de recompensa inválido para filtro: {reward_kind!r}")
        q_norm = normalize_search_text(query) if query else ""
        rt_norm = normalize_search_text(reward_target) if reward_target else ""

        results: List[ResearchEntry] = []
        for entry in self.all_entries():
            if not include_synthetic and entry.provenance.is_synthetic:
                continue
            if cat_filter is not None and entry.category != cat_filter:
                continue
            if reward_kind is not None and not any(r.kind == reward_kind for r in entry.rewards):
                continue
            if rt_norm:
                reward_haystack = " ".join(
                    f"{r.target_id or ''} {r.display_name_ptbr}" for r in entry.rewards
                )
                if rt_norm not in normalize_search_text(reward_haystack):
                    continue
            if q_norm:
                parts = [
                    entry.research_id,
                    entry.title_ptbr,
                    CATEGORY_LABELS_PTBR[entry.category],
                    *(c.code for c in entry.conditions),
                    *(c.declared_label_ptbr for c in entry.conditions),
                    *(r.display_name_ptbr for r in entry.rewards),
                    *(r.target_id or "" for r in entry.rewards),
                    *entry.tags,
                ]
                haystack = normalize_search_text(" ".join(parts))
                if q_norm not in haystack:
                    continue
            results.append(entry)
        return results

    def validate_references(self, referenced_ids: Sequence[str]) -> Dict[str, Any]:
        """Validate a sequence of IDs for duplicates and unknown catalog references."""
        seen: set[str] = set()
        duplicates: List[str] = []
        unknown: List[str] = []
        invalid: List[str] = []

        for raw_id in referenced_ids:
            try:
                cid = validate_research_id(raw_id)
            except ValueError:
                invalid.append(str(raw_id))
                continue
            if cid in seen:
                if cid not in duplicates:
                    duplicates.append(cid)
            else:
                seen.add(cid)
            if cid not in self._entries and cid not in unknown:
                unknown.append(cid)

        return {
            "ok": not (duplicates or unknown or invalid),
            "duplicates": duplicates,
            "unknown_ids": unknown,
            "invalid_ids": invalid,
        }

    def export_dict(self) -> Dict[str, Any]:
        return {
            "schema_version": CATALOG_SCHEMA_VERSION,
            "live_availability_confirmed": False,
            "entry_count": len(self._entries),
            "entries": [e.to_dict() for e in self.all_entries()],
        }

    def export_json(self) -> str:
        payload = sanitize_research_payload(self.export_dict())
        raw = json.dumps(payload, ensure_ascii=False, indent=2, sort_keys=True, allow_nan=False)
        if len(raw.encode("utf-8")) > MAX_EXPORT_BYTES:
            raise ValueError(f"Exportação excede limite de {MAX_EXPORT_BYTES} bytes.")
        return raw

    @classmethod
    def from_json(cls, raw: str, *, max_bytes: int = MAX_EXPORT_BYTES) -> "ResearchCatalog":
        if not isinstance(raw, str):
            raise ValueError("JSON de catálogo deve ser string.")
        encoded = raw.encode("utf-8")
        if len(encoded) > max_bytes:
            raise ValueError(f"Payload JSON excede limite de {max_bytes} bytes.")
        try:
            data = json.loads(raw)
        except json.JSONDecodeError as exc:
            raise ValueError(f"JSON inválido no catálogo de pesquisas: {exc}") from exc
        if not isinstance(data, dict) or data.get("schema_version") != CATALOG_SCHEMA_VERSION:
            raise ValueError("schema_version ausente ou não suportada no catálogo de pesquisas.")
        raw_entries = data.get("entries")
        if not isinstance(raw_entries, list):
            raise ValueError("Campo 'entries' deve ser lista.")
        if len(raw_entries) > MAX_CATALOG_ENTRIES:
            raise ValueError(f"Catálogo excede o limite de {MAX_CATALOG_ENTRIES} registros.")

        sanitized = sanitize_research_payload(raw_entries)
        catalog = cls()
        for item in sanitized:
            catalog.add(ResearchEntry.from_dict(item))
        return catalog


def _read_bounded_regular_file(path: Path, max_bytes: int) -> bytes:
    """Safely read a regular non-symlink file with bounded size and O_BINARY support."""
    if not _is_strict_int(max_bytes) or max_bytes <= 0:
        raise ValueError("max_bytes deve ser inteiro positivo.")
    if path.is_symlink():
        raise ValueError(f"Symlink recusado por segurança: {path.name}")
    flags = os.O_RDONLY | getattr(os, "O_BINARY", 0) | getattr(os, "O_NOFOLLOW", 0)
    fd = os.open(str(path), flags)
    try:
        st = os.fstat(fd)
        if not stat.S_ISREG(st.st_mode):
            raise ValueError("Apenas arquivos regulares são permitidos.")
        chunks: List[bytes] = []
        total = 0
        while total <= max_bytes:
            chunk = os.read(fd, min(65536, max_bytes + 1 - total))
            if not chunk:
                break
            chunks.append(chunk)
            total += len(chunk)
        if total > max_bytes:
            raise ValueError(f"Arquivo excede o limite máximo de {max_bytes} bytes.")
        return b"".join(chunks)
    finally:
        os.close(fd)


def export_catalog_file(catalog: ResearchCatalog, path: Path) -> int:
    """Atomically write sanitized offline catalog JSON to a local regular file."""
    target = Path(path)
    if target.exists() and target.is_symlink():
        raise ValueError("Destino symlink recusado por segurança.")
    raw_bytes = catalog.export_json().encode("utf-8")
    tmp = target.with_name(f".{target.name}.tmp")
    flags = os.O_WRONLY | os.O_CREAT | os.O_TRUNC | getattr(os, "O_BINARY", 0) | getattr(os, "O_NOFOLLOW", 0)
    fd = os.open(str(tmp), flags, 0o600)
    try:
        os.write(fd, raw_bytes)
        os.fsync(fd)
    finally:
        os.close(fd)
    os.replace(tmp, target)
    return len(raw_bytes)


def import_catalog_file(path: Path, *, max_bytes: int = MAX_EXPORT_BYTES) -> ResearchCatalog:
    """Read and validate a bounded local catalog JSON file without mutating it."""
    raw_bytes = _read_bounded_regular_file(Path(path), max_bytes=max_bytes)
    try:
        text = raw_bytes.decode("utf-8")
    except UnicodeDecodeError as exc:
        raise ValueError("Arquivo de catálogo não está em UTF-8 válido.") from exc
    return ResearchCatalog.from_json(text, max_bytes=max_bytes)


def _try_load_events_planner_checklist() -> Tuple[Optional[Any], Optional[Any]]:
    """Lazy-import ManualChecklist/ChecklistItem from 0006 (events_planner) when available."""
    try:
        from tools.pgbr_data_tools.events_planner import ChecklistItem, ManualChecklist  # type: ignore
        return ManualChecklist, ChecklistItem
    except Exception:
        return None, None


class ResearchManualChecklist:
    """User-confirmed manual research checklist without account or game access.

    Reuses the events planner when installed. The research-facing four-state
    checklist is authoritative locally because the planner exposes only three
    states; delegation preserves the planner contract without losing distinctions.
    """

    def __init__(self, catalog: ResearchCatalog) -> None:
        self._catalog = catalog
        planner_cls, item_cls = _try_load_events_planner_checklist()
        self._delegated_cls = planner_cls
        self._delegated_item_cls = item_cls
        self._delegated = planner_cls() if planner_cls is not None else None
        self._local_states: Dict[str, Tuple[str, str]] = {}  # id -> (state, window_key)

    @property
    def reuses_events_planner_checklist(self) -> bool:
        return self._delegated is not None

    def track(
        self,
        research_id: str,
        *,
        window_key: str = "manual",
        priority: int = 2,
    ) -> None:
        entry = self._catalog.get(research_id)
        if not _is_strict_int(priority) or priority not in (1, 2, 3):
            raise ValueError("priority deve ser inteiro 1..3.")
        if not isinstance(window_key, str) or not window_key.strip():
            raise ValueError("window_key obrigatório.")
        if self._delegated is not None and self._delegated_item_cls is not None:
            # Map catalog categories to events_planner ChecklistItem kinds
            kind_map = {
                "campo": "field",
                "temporaria": "timed",
                "especial": "special",
                "colecao": "collection_challenge",
            }
            item = self._delegated_item_cls(
                item_id=entry.research_id,
                title_pt=entry.title_ptbr,
                kind=kind_map[entry.category],
                state="pending",
                priority=priority,
                window_key=window_key.strip(),
            )
            self._delegated.upsert(item)
        self._local_states[entry.research_id] = ("pendente", window_key.strip())

    def confirm_state(self, research_id: str, new_state: str) -> None:
        canonical = validate_research_id(research_id)
        if canonical not in self._catalog:
            raise KeyError(f"research_id desconhecido no catálogo: {canonical}")
        if new_state not in CHECKLIST_STATES:
            raise ValueError(f"Estado inválido: {new_state!r}. Permitidos: {CHECKLIST_STATES}")
        if canonical not in self._local_states:
            raise KeyError(f"Pesquisa não adicionada ao checklist: {canonical}")
        if self._delegated is not None:
            planner_states = {"pendente": "pending", "em_progresso": "pending",
                              "concluido": "done", "ignorado": "unknown"}
            self._delegated.confirm_state(canonical, planner_states[new_state])
        _, wk = self._local_states[canonical]
        self._local_states[canonical] = (new_state, wk)

    def reset_window(self, window_key: str) -> int:
        if self._delegated is not None:
            self._delegated.reset_window(window_key)
        count = 0
        for rid, (st, wk) in list(self._local_states.items()):
            if wk == window_key and st != "pendente":
                self._local_states[rid] = ("pendente", wk)
                count += 1
        return count

    def summary(self) -> Dict[str, int]:
        counts = {s: 0 for s in CHECKLIST_STATES}
        for st, _ in self._local_states.values():
            counts[st] += 1
        return counts


def build_synthetic_example_catalog() -> ResearchCatalog:
    """Build a clearly labeled synthetic example catalog (never real live game data)."""
    entries = (
        ResearchEntry(
            research_id="SYNTH_FIELD_001",
            category="campo",
            title_ptbr="[EXEMPLO SINTÉTICO] Capture 5 Pokémon com clima favorável",
            conditions=(
                ResearchCondition(
                    code="CATCH_POKEMON_WEATHER",
                    declared_label_ptbr="Capture 5 Pokémon com bônus de clima",
                    target_count=5,
                    parameters={"weather_boosted": True},
                ),
            ),
            rewards=(
                ResearchReward(
                    kind="poeira_estelar",
                    target_id="STARDUST",
                    display_name_ptbr="Poeira Estelar",
                    quantity=500,
                ),
            ),
            provenance=ResearchProvenance(
                source_name="PGBR_SYNTHETIC_FIXTURE",
                source_type="sintetico_exemplo",
                revision="synth-1.0",
                valid_from_utc="2026-01-01T00:00:00Z",
                valid_until_utc="2026-12-31T23:59:59Z",
                is_synthetic=True,
                notes_ptbr="Exemplo sintético offline para validação de catálogo.",
            ),
            documentation_status="sintetica_exemplo",
            tags=("sintetico", "campo", "poeira"),
        ),
        ResearchEntry(
            research_id="SYNTH_TIMED_001",
            category="temporaria",
            title_ptbr="[EXEMPLO SINTÉTICO] Faça 3 Jogadas Ótimas seguidas",
            conditions=(
                ResearchCondition(
                    code="GREAT_THROWS_IN_A_ROW",
                    declared_label_ptbr="Faça 3 Jogadas Ótimas consecutivas",
                    target_count=3,
                    parameters={"streak": True},
                ),
            ),
            rewards=(
                ResearchReward(
                    kind="especie",
                    target_id="SPECIES_0025_PIKACHU",
                    display_name_ptbr="Encontro: Pikachu (exemplo sintético)",
                    quantity=1,
                ),
            ),
            provenance=ResearchProvenance(
                source_name="PGBR_SYNTHETIC_FIXTURE",
                source_type="sintetico_exemplo",
                revision="synth-1.0",
                valid_from_utc="2026-06-01T10:00:00Z",
                valid_until_utc="2026-06-02T20:00:00Z",
                is_synthetic=True,
                notes_ptbr="Janela sintética para teste de expiração.",
            ),
            documentation_status="sintetica_exemplo",
            tags=("sintetico", "temporaria", "pikachu"),
        ),
        ResearchEntry(
            research_id="SYNTH_SPECIAL_001",
            category="especial",
            title_ptbr="[EXEMPLO SINTÉTICO] Etapa de Pesquisa Especial com recompensa não documentada",
            conditions=(
                ResearchCondition(
                    code="EVOLVE_POKEMON",
                    declared_label_ptbr="Evolua 2 Pokémon",
                    target_count=2,
                ),
            ),
            rewards=(
                ResearchReward(
                    kind="desconhecido",
                    target_id=None,
                    display_name_ptbr="Recompensa não documentada na fonte local",
                    is_unknown=True,
                ),
            ),
            provenance=ResearchProvenance(
                source_name="PGBR_SYNTHETIC_FIXTURE",
                source_type="sintetico_exemplo",
                revision="synth-1.0",
                is_synthetic=True,
                notes_ptbr="Demonstra recompensa desconhecida explícita.",
            ),
            documentation_status="sintetica_exemplo",
            tags=("sintetico", "especial", "desconhecido"),
        ),
        ResearchEntry(
            research_id="SYNTH_COLLECTION_001",
            category="colecao",
            title_ptbr="[EXEMPLO SINTÉTICO] Desafio de Coleção com intervalo de itens",
            conditions=(
                ResearchCondition(
                    code="COLLECT_SPECIES_SET",
                    declared_label_ptbr="Registre manualmente as espécies do desafio",
                    target_count=4,
                ),
            ),
            rewards=(
                ResearchReward(
                    kind="item",
                    target_id="ITEM_ULTRA_BALL",
                    display_name_ptbr="Ultra Bola",
                    quantity_min=3,
                    quantity_max=5,
                ),
            ),
            provenance=ResearchProvenance(
                source_name="PGBR_SYNTHETIC_FIXTURE",
                source_type="sintetico_exemplo",
                revision="synth-1.0",
                is_synthetic=True,
                notes_ptbr="Demonstra categoria coleção e intervalo quantity_min/quantity_max.",
            ),
            documentation_status="sintetica_exemplo",
            tags=("sintetico", "colecao", "ultra bola"),
        ),
    )
    return ResearchCatalog(entries)
