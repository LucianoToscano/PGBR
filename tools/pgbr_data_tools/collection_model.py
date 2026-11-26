"""Canonical Pokemon collection model, tri-state protection, Pokematrix tags, berry fallback & atomic cache (COL-02, VGP-01, INV-02, DATA-03, SYS-05)."""
from __future__ import annotations

from dataclasses import dataclass, field, asdict
import hashlib
import json
import os
from pathlib import Path
from typing import Any, Dict, Iterable, List, Optional, Sequence, Set, Tuple


@dataclass(frozen=True)
class PokemonRecord:
    """Canonical offline Pokemon collection entry (Pokematrix / DATA-03).

    Tri-state booleans (`Optional[bool]`): None means UNKNOWN and is NEVER
    treated as False when evaluating collection protection.
    """
    uid: str
    species_id: int
    species_name: str
    family_id: int
    form: str = "NORMAL"
    atk_iv: Optional[int] = None
    def_iv: Optional[int] = None
    sta_iv: Optional[int] = None
    level: Optional[float] = None
    shiny: Optional[bool] = None
    rare: Optional[bool] = None
    event: Optional[bool] = None
    costume: Optional[bool] = None
    background: Optional[bool] = None
    favorite: Optional[bool] = None
    shadow: Optional[bool] = None
    purified: Optional[bool] = None
    dynamax: Optional[bool] = None
    gigantamax: Optional[bool] = None
    xxl: Optional[bool] = None
    xxs: Optional[bool] = None
    legacy_move: Optional[bool] = None
    tags: Tuple[str, ...] = ()
    tags_loaded: bool = False


@dataclass(frozen=True)
class ProtectionDecision:
    status: str  # "PROTECTED" | "UNCERTAIN" | "MATCH"
    reasons: Tuple[str, ...]


PROTECTABLE_FIELDS: Tuple[Tuple[str, str], ...] = (
    ("favorite", "favorito"),
    ("shiny", "brilhante (shiny)"),
    ("rare", "categoria rara"),
    ("event", "evento"),
    ("costume", "fantasia"),
    ("background", "fundo especial"),
    ("shadow", "sombrio"),
    ("dynamax", "Dynamax"),
    ("gigantamax", "Gigantamax"),
    ("xxl", "XXL"),
    ("xxs", "XXS"),
    ("legacy_move", "ataque legado"),
)


def evaluate_protection(
    rec: PokemonRecord,
    *,
    min_iv_percent: int = 90,
    protect_nundo: bool = True,
    protected_species: Set[int] | None = None,
    protected_families: Set[int] | None = None,
    protected_tags: Set[str] | None = None,
    enabled_flags: Set[str] | None = None,
) -> ProtectionDecision:
    """Evaluate tri-state collection protection (COL-02 / VGP-01).

    Never authorizes deletion; returns PROTECTED, UNCERTAIN, or MATCH.
    Unknown attributes or unloaded tags fail-closed to UNCERTAIN.
    """
    reasons: List[str] = []
    unknowns: List[str] = []
    flags = enabled_flags if enabled_flags is not None else {k for k, _ in PROTECTABLE_FIELDS}

    if rec.shadow is True and rec.purified is True:
        unknowns.append("Contradição: sombrio e purificado simultâneos")
    if rec.xxl is True and rec.xxs is True:
        unknowns.append("Contradição: XXL e XXS simultâneos")

    if protected_species and rec.species_id in protected_species:
        reasons.append("espécie protegida")
    if protected_families and rec.family_id in protected_families:
        reasons.append("família protegida")

    # Tag protection: fail-closed if tags are not loaded yet.
    if protected_tags:
        if not rec.tags_loaded:
            unknowns.append("etiquetas ainda não carregadas")
        else:
            norm_protected = {t.strip().lower() for t in protected_tags if t.strip()}
            matched = [t for t in rec.tags if t.strip().lower() in norm_protected]
            if matched:
                reasons.append(f"etiqueta protegida ({matched[0]})")

    # IV checks (including lower-bound protection on partial IVs)
    ivs = (rec.atk_iv, rec.def_iv, rec.sta_iv)
    valid_ivs = [v for v in ivs if isinstance(v, int) and 0 <= v <= 15]
    if len(valid_ivs) == 3:
        total_iv = sum(valid_ivs)
        if min_iv_percent <= 100 and total_iv * 100 >= min_iv_percent * 45:
            reasons.append(f"IV >= {min_iv_percent}%")
        if protect_nundo and total_iv == 0:
            reasons.append("IV 0% (nundo)")
    else:
        unknowns.append("IV incompleto ou desconhecido")
        lower = sum(valid_ivs)
        if min_iv_percent <= 100 and lower * 100 >= min_iv_percent * 45:
            reasons.append(f"IV mínimo confirmado >= {min_iv_percent}%")

    for attr, label in PROTECTABLE_FIELDS:
        if attr not in flags and attr != "favorite":
            continue
        val = getattr(rec, attr)
        if val is True:
            reasons.append(label)
        elif val is None:
            unknowns.append(f"{label} não confirmado")

    if reasons:
        return ProtectionDecision("PROTECTED", tuple(reasons + unknowns))
    if unknowns:
        return ProtectionDecision("UNCERTAIN", tuple(unknowns))
    return ProtectionDecision("MATCH", ("Corresponde ao filtro; nenhuma ação executada.",))


def plan_berry_fallback(
    preferred_order: Sequence[str],
    stock: Dict[str, Optional[int]],
    reserves: Dict[str, int] | None = None,
) -> Dict[str, Any]:
    """Select first berry with confirmed stock above reserve (Pokematrix / INV-02).

    `None` or negative stock means UNKNOWN and is explicitly reported without
    assuming availability.
    """
    res = reserves or {}
    earlier_unknown: List[str] = []
    for berry in preferred_order:
        qty = stock.get(berry)
        min_keep = max(0, res.get(berry, 0))
        if qty is None or qty < 0:
            earlier_unknown.append(berry)
            continue
        if qty > min_keep:
            return {
                "selected": berry,
                "available_above_reserve": qty - min_keep,
                "earlier_unknown": earlier_unknown,
                "status": "OK",
            }
    return {
        "selected": None,
        "available_above_reserve": 0,
        "earlier_unknown": earlier_unknown,
        "status": "UNKNOWN" if earlier_unknown else "DEPLETED",
    }


class AtomicSnapshotStore:
    """Versioned offline JSON snapshot store with SHA-256 integrity & rollback (DATA-03 / SYS-05)."""

    SCHEMA_VERSION = 1

    def __init__(self, path: Path, *, max_bytes: int = 2_000_000) -> None:
        self.path = Path(path)
        self.backup_path = self.path.with_suffix(self.path.suffix + ".bak")
        self.max_bytes = max_bytes

    @staticmethod
    def _sha256(payload: bytes) -> str:
        return hashlib.sha256(payload).hexdigest()

    def save(self, namespace: str, revision: str, data: Any) -> str:
        """Atomically write envelope + checksum, preserving previous valid snapshot as .bak."""
        if not namespace.strip() or not revision.strip():
            raise ValueError("namespace and revision must be non-empty")
        canonical = json.dumps(data, sort_keys=True, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
        digest = self._sha256(canonical)
        envelope = {
            "schema": self.SCHEMA_VERSION,
            "namespace": namespace.strip(),
            "revision": revision.strip(),
            "sha256": digest,
            "data": data,
        }
        raw = json.dumps(envelope, ensure_ascii=False, indent=2).encode("utf-8")
        if len(raw) > self.max_bytes:
            raise ValueError(f"Snapshot exceeds max_bytes ({len(raw)} > {self.max_bytes})")
        self.path.parent.mkdir(parents=True, exist_ok=True)
        if self.path.exists():
            prev = self.path.read_bytes()
            tmp_bak = self.backup_path.with_suffix(".bak.tmp")
            tmp_bak.write_bytes(prev)
            os.replace(tmp_bak, self.backup_path)
        tmp = self.path.with_suffix(self.path.suffix + ".tmp")
        tmp.write_bytes(raw)
        os.replace(tmp, self.path)
        return digest

    def load(self) -> Dict[str, Any]:
        """Load and verify checksum; raises ValueError on corruption (never silently resets)."""
        raw = self.path.read_bytes()
        if len(raw) > self.max_bytes:
            raise ValueError("Snapshot exceeds size limit")
        env = json.loads(raw.decode("utf-8"))
        if not isinstance(env, dict) or env.get("schema") != self.SCHEMA_VERSION:
            raise ValueError("Unsupported snapshot schema")
        canonical = json.dumps(
            env["data"], sort_keys=True, ensure_ascii=False, separators=(",", ":")
        ).encode("utf-8")
        if self._sha256(canonical) != env.get("sha256"):
            raise ValueError("Snapshot checksum mismatch")
        return env

    def rollback(self) -> Dict[str, Any]:
        """Restore previous verified snapshot from .bak when current file is corrupted."""
        if not self.backup_path.exists():
            raise FileNotFoundError("No backup snapshot available for rollback")
        bak_bytes = self.backup_path.read_bytes()
        env = json.loads(bak_bytes.decode("utf-8"))
        canonical = json.dumps(
            env["data"], sort_keys=True, ensure_ascii=False, separators=(",", ":")
        ).encode("utf-8")
        if self._sha256(canonical) != env.get("sha256"):
            raise ValueError("Backup snapshot is also corrupted")
        tmp = self.path.with_suffix(self.path.suffix + ".tmp")
        tmp.write_bytes(bak_bytes)
        os.replace(tmp, self.path)
        return env
