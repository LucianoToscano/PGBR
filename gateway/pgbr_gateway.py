#!/usr/bin/env python3
"""PGBR Data Gateway — the project's own service behind the five source integrations.

This is infrastructure common to PokeList, iFlowGo, PoGoCoordinates, PGSharp and iPogo. It is NOT a
sixth source, and none of its paths belong to a provider: every endpoint here is the PGBR's own
contract (OpenAPI 3.1 served at /openapi.json).

What it does
------------
* Normalises allowed records into one canonical model with provenance
  (``live_external`` / ``personal_import`` / ``static_catalog`` / ``demo_fixture``).
* Serves health, capabilities, sources, search, imports, presets, events and webhook subscriptions.
* Deduplicates by ``(source_id, event_id)`` and enforces TTL.
* Dispatches signed webhooks (HMAC-SHA256, event id, timestamp, idempotency, anti-replay, retry with
  backoff, dead-letter queue, revocation).

What it deliberately does NOT do
--------------------------------
* It does not scrape. No page of a provider has a verified, explicit automated-collection
  permission in this repository, so ``PolicyGate`` refuses remote collection by default and the
  gateway keeps working with the user's own imports and with the project's own feed.
* It does not bypass 401/403/429, logins, quotas or paywalls: a refused upstream is recorded as
  ``blocked`` and surfaced as such.
* It does not emit spawn events for personal imports or static catalogues: only ``import.completed``,
  ``preset.*`` and ``source.status_changed`` are legitimate there.

Standard library only. Configuration comes from the environment:

  PGBR_GATEWAY_TOKEN   bearer token; when unset the gateway refuses every authenticated call
  PGBR_GATEWAY_SECRET  webhook signing secret (never shipped inside the APK)
  PGBR_GATEWAY_HOST    default 127.0.0.1
  PGBR_GATEWAY_PORT    default 8080

Run:  python3 gateway/pgbr_gateway.py            (serve)
      python3 gateway/pgbr_gateway.py --selftest (offline checks)
"""
from __future__ import annotations

import base64
import hashlib
import hmac
import json
import math
import os
import re
import ssl
import threading
import time
import urllib.error
import urllib.request
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse

API_VERSION = "v1"
SERVICE = "pgbr-data-gateway"
SPEC_VERSION = "1.0.0"
MAX_BODY_BYTES = 2 * 1024 * 1024
MAX_EVENTS = 5000
MAX_PAGE_LIMIT = 100
DEFAULT_PAGE_LIMIT = 25
WEBHOOK_TOLERANCE_SECONDS = 300
WEBHOOK_MAX_ATTEMPTS = 5
WEBHOOK_BACKOFF_SECONDS = (1, 2, 4, 8, 16)
DLQ_MAX = 500
REPLAY_CACHE_MAX = 10000

ORIGIN_KINDS = ("live_external", "personal_import", "static_catalog", "demo_fixture")
KINDS = ("pokemon", "raid", "quest", "rocket", "powerspot", "nest", "route")
SOURCE_IDS = ("pokelist", "iflowgo", "pogocoordinates", "pgsharp", "ipogo",
              "pgbr_hundo", "livepokemap")
INTEGRATION_IDS = ("pokelist", "iflowgo", "pogocoordinates", "pgsharp", "ipogo")

# Declared capabilities. Nothing is "connected" until a real request proves it; the five
# integrations are exactly these, and Pokemod/PGTools are deliberately absent.
SOURCES = {
    "pokelist": {
        "status": "unverified", "origin_kind": "live_external", "supports_remote_search": True,
        "supports_pagination": False, "live_feed_available": False, "supports_webhook": False,
        "attribution": "https://moonani.com/PokeList/",
        "coverage": "Consulta por espécie/área; a fonte pode recusar (401/403/429).",
        "note": "Contrato observado no APK 1.3.18; servidor pode recusar. O PGBR não contorna a recusa.",
    },
    "iflowgo": {
        "status": "unverified", "origin_kind": "live_external", "supports_remote_search": True,
        "supports_pagination": True, "live_feed_available": False, "supports_webhook": False,
        "attribution": "https://pokecoords.iflowgo.com",
        "coverage": "Nearby por raio limitado; busca global por espécie com página declarada.",
        "note": "25 itens por página não são cobertura mundial; a resposta decide has_more.",
    },
    "pogocoordinates": {
        "status": "unverified", "origin_kind": "live_external", "supports_remote_search": True,
        "supports_pagination": False, "live_feed_available": False, "supports_webhook": False,
        "coordinate_precision": "unavailable",
        "attribution": "https://pogocoordinates.com",
        "coverage": "Resultados podem vir com reveal_code em vez de latitude/longitude.",
        "note": "reveal_code NÃO é coordenada; o gateway não revela nem converte códigos.",
    },
    "pgsharp": {
        "status": "local_only", "origin_kind": "personal_import", "supports_remote_search": False,
        "supports_pagination": False, "live_feed_available": False, "supports_webhook": False,
        "attribution": "https://www.pgsharp.com/",
        "coverage": "Somente arquivos convertidos pelo próprio usuário.",
        "note": "Sem API pública documentada: apenas importação pessoal e filtros locais.",
    },
    "ipogo": {
        "status": "local_only", "origin_kind": "personal_import", "supports_remote_search": False,
        "supports_pagination": False, "live_feed_available": False, "supports_webhook": False,
        "attribution": "https://ipogo.app/features.php",
        "coverage": "Somente exportações pessoais (JSON/CSV/GPX) ou catálogos estáticos.",
        "note": "O APK/iOS do iPogo não expõe API pública de coordenadas: nenhuma chamada remota.",
    },
    "pgbr_hundo": {
        "status": "unverified", "origin_kind": "live_external", "supports_remote_search": True,
        "supports_pagination": False, "live_feed_available": True, "supports_webhook": False,
        "attribution": "https://github.com/LucianoToscano/PGBR (tools/hundo-feed)",
        "coverage": "Feed 100% IV coletado pelo projeto.",
        "note": "A fonte declara 100% IV; o gateway não recalcula o encontro.",
    },
    "livepokemap": {
        "status": "unverified", "origin_kind": "live_external", "supports_remote_search": True,
        "supports_pagination": False, "live_feed_available": False, "supports_webhook": False,
        "attribution": "https://play.livepokemap.com",
        "coverage": "Viewport do mapa; login próprio do LivePokeMap.",
        "note": "Serviço externo com login; pode recusar. Credencial separada por conta.",
    },
}


class GatewayError(Exception):
    """Error carrying an HTTP status, a stable code and a PT-BR message."""

    def __init__(self, status: int, code: str, message: str, retry_after: int = 0):
        super().__init__(message)
        self.status = status
        self.code = code
        self.message = message
        self.retry_after = retry_after


# ---------------------------------------------------------------------------------------------
# Policy: what may be collected at all. Fails closed.
# ---------------------------------------------------------------------------------------------


class PolicyGate:
    """Decides whether a record may enter the store. No provider permission is presumed."""

    #: origin kinds that may produce live spawn/raid/quest/rocket webhooks.
    LIVE_ORIGINS = frozenset({"live_external"})

    def __init__(self, allow_remote: bool = False, allowed_origins=None):
        self.allow_remote = allow_remote
        self.allowed_origins = set(allowed_origins or
                                   {"live_external", "personal_import", "static_catalog"})

    def admit(self, event: dict) -> tuple[bool, str]:
        origin = event.get("origin_kind")
        if origin not in self.allowed_origins:
            return False, "origem não permitida por política: %s" % origin
        if origin == "live_external" and not self.allow_remote:
            return False, ("coleta remota desativada: nenhuma página/fonte com permissão explícita "
                           "verificada; use importação pessoal ou o feed do próprio projeto")
        if origin == "demo_fixture":
            return False, "fixture sintética é permitida somente em testes automatizados"
        return True, ""

    def may_emit_live_event(self, event: dict) -> bool:
        return event.get("origin_kind") in self.LIVE_ORIGINS


# ---------------------------------------------------------------------------------------------
# Validation and normalisation
# ---------------------------------------------------------------------------------------------


def _number(value):
    if value is None or isinstance(value, bool):
        return None
    if isinstance(value, (int, float)):
        return float(value) if value == value and abs(value) != float("inf") else None
    try:
        parsed = float(str(value).strip())
    except (TypeError, ValueError):
        return None
    return parsed if parsed == parsed and abs(parsed) != float("inf") else None


def _integer(value):
    number = _number(value)
    if number is None:
        return None
    rounded = int(round(number))
    return rounded if abs(number - rounded) < 1e-9 else None


def _valid_coordinates(lat, lon):
    if lat is None or lon is None:
        return False
    return -90.0 <= lat <= 90.0 and -180.0 <= lon <= 180.0 and not (
        abs(lat) < 1e-9 and abs(lon) < 1e-9)


def _iso_ms(value):
    """Epoch seconds, epoch millis or ISO-8601 -> epoch millis; 0 when unknown."""
    if value is None:
        return 0
    if isinstance(value, (int, float)) and not isinstance(value, bool):
        raw = int(value)
        if 1_000_000_000 <= raw < 10_000_000_000:
            return raw * 1000
        if 1_000_000_000_000 <= raw < 10_000_000_000_000:
            return raw
        return 0
    text = str(value).strip()
    if not text:
        return 0
    if re.fullmatch(r"-?\d+", text):
        return _iso_ms(int(text))
    match = re.fullmatch(r"(\d{4})-(\d{2})-(\d{2})[Tt ](\d{2}):(\d{2}):(\d{2})(?:\.(\d{1,6}))?"
                         r"(?:[Zz]|([+-])(\d{2}):?(\d{2}))?", text)
    if not match:
        return 0
    year, month, day, hour, minute, second = (int(match.group(i)) for i in range(1, 7))
    micros = int((match.group(7) or "0").ljust(6, "0"))
    offset = 0
    if match.group(8):
        sign = -1 if match.group(8) == "-" else 1
        offset = sign * (int(match.group(9)) * 3600 + int(match.group(10)) * 60)
    try:
        import calendar
        epoch = calendar.timegm((year, month, day, hour, minute, second, 0, 0, 0)) - offset
    except Exception:
        return 0
    return epoch * 1000 + micros // 1000


def normalise(row: dict, source_id: str | None = None, origin_kind: str | None = None,
              retrieved_at: int | None = None) -> dict | None:
    """One raw row -> canonical event, or ``None`` when it has no valid coordinates/identity.

    Missing fields stay ``None``; a numeric 0 (including IV 0) is preserved.
    """
    if not isinstance(row, dict):
        return None
    lat = _number(row.get("latitude", row.get("lat")))
    lon = _number(row.get("longitude", row.get("lon", row.get("lng"))))
    coordinates_valid = _valid_coordinates(lat, lon)
    source = source_id or row.get("source_id") or "import"
    event_id = row.get("event_id") or row.get("id") or row.get("spawn_id") or row.get("encounter_id")
    if not event_id:
        if not coordinates_valid:
            return None
        event_id = "%.6f,%.6f" % (lat, lon)
    kind_raw = str(row.get("kind") or row.get("type") or "pokemon").strip().lower()
    kind = "pokemon"
    for candidate in KINDS:
        if candidate in kind_raw:
            kind = candidate
            break
    iv = _number(row.get("iv_percent", row.get("iv", row.get("percent_iv"))))
    if iv is not None and not 0.0 <= iv <= 100.0:
        iv = None
    atk = _integer(row.get("iv_atk", row.get("attack", row.get("attack_iv", row.get("atk")))))
    dfn = _integer(row.get("iv_def", row.get("defense", row.get("defense_iv", row.get("def")))))
    sta = _integer(row.get("iv_sta", row.get("hp", row.get("stamina_iv", row.get("sta")))))
    if atk is not None and not 0 <= atk <= 15:
        atk = None
    if dfn is not None and not 0 <= dfn <= 15:
        dfn = None
    if sta is not None and not 0 <= sta <= 15:
        sta = None
    cp = _integer(row.get("cp"))
    level = _integer(row.get("level"))
    gender_raw = row.get("gender")
    gender = None
    if gender_raw is not None:
        text = str(gender_raw).strip().lower()
        if text in ("1", "m", "male", "macho"):
            gender = "MALE"
        elif text in ("2", "f", "female", "femea", "fêmea"):
            gender = "FEMALE"
        elif text in ("3", "n", "none", "genderless"):
            gender = "GENDERLESS"
    size_raw = row.get("size")
    sizes = {"1": "XXS", "2": "XS", "3": "S", "4": "M", "5": "XL", "6": "XXL",
             "xxs": "XXS", "xs": "XS", "s": "S", "m": "M", "xl": "XL", "xxl": "XXL"}
    size = sizes.get(str(size_raw).strip().lower()) if size_raw is not None else None
    origin = origin_kind or row.get("origin_kind") or "personal_import"
    event = {
        "source_id": source,
        "event_id": str(event_id),
        "kind": kind,
        "origin_kind": origin,
        "pokemon_id": _integer(row.get("pokemon_id", row.get("pokemon", row.get("dex")))),
        "form_id": _integer(row.get("form_id", row.get("form"))),
        "name": (str(row.get("name") or "").strip() or None),
        "latitude": lat if coordinates_valid else None,
        "longitude": lon if coordinates_valid else None,
        "coordinate_precision": ("unavailable" if not coordinates_valid
                                 else row.get("coordinate_precision")
                                 if row.get("coordinate_precision") in ("exact", "approximate")
                                 else "exact"),
        "iv_percent": iv,
        "iv_atk": atk,
        "iv_def": dfn,
        "iv_sta": sta,
        "cp": cp,
        "level": level,
        "gender": gender,
        "size": size,
        "weather_boost": (None if row.get("weather_boost") is None
                          else bool(row.get("weather_boost"))),
        "pvp_great_rank": _integer(row.get("pvp_great_rank")),
        "pvp_ultra_rank": _integer(row.get("pvp_ultra_rank")),
        "shiny_observed": (None if row.get("shiny_observed") is None
                           else bool(row.get("shiny_observed"))),
        "observed_at": _iso_ms(row.get("observed_at", row.get("startTime"))) or int(time.time() * 1000),
        "retrieved_at": retrieved_at or int(time.time() * 1000),
        "expires_at": _iso_ms(row.get("expires_at", row.get("endTime", row.get("end_time")))),
        "is_demo": origin == "demo_fixture" or bool(row.get("is_demo", False)),
        "data_quality": ("provider_claimed" if origin == "live_external"
                         else "imported" if origin == "personal_import" else "unknown"),
        "coverage": row.get("coverage"),
        "source_url": row.get("source_url"),
    }
    return event


def validate_search_payload(payload: dict) -> dict:
    if not isinstance(payload, dict):
        raise GatewayError(422, "invalid_body", "Corpo deve ser um objeto JSON.")
    sources = payload.get("sources")
    if not isinstance(sources, list) or not sources:
        raise GatewayError(422, "invalid_sources", "Informe ao menos uma fonte em 'sources'.")
    for source in sources:
        if source not in SOURCE_IDS:
            raise GatewayError(422, "unknown_source", "Fonte desconhecida: %s" % source)
    kind = payload.get("kind")
    if kind is not None and kind not in KINDS:
        raise GatewayError(422, "invalid_kind", "Tipo de evento inválido: %s" % kind)
    query = {"sources": list(sources), "kind": kind}
    for key, low, high in (("iv_min", 0, 100), ("iv_max", 0, 100)):
        value = payload.get(key)
        if value is None:
            query[key] = None
            continue
        number = _number(value)
        if number is None or not low <= number <= high:
            raise GatewayError(422, "invalid_range", "%s fora de %d..%d." % (key, low, high))
        query[key] = number
    if query["iv_min"] is not None and query["iv_max"] is not None \
            and query["iv_min"] > query["iv_max"]:
        raise GatewayError(422, "invalid_range", "IV mínimo maior que o máximo.")
    for key in ("atk_min", "atk_max", "def_min", "def_max", "sta_min", "sta_max"):
        value = payload.get(key)
        if value is None:
            query[key] = None
            continue
        number = _integer(value)
        if number is None or not 0 <= number <= 15:
            raise GatewayError(422, "invalid_range", "%s fora de 0..15." % key)
        query[key] = number
    for key, ceiling in (("cp_min", 99999), ("cp_max", 99999), ("level_min", 50), ("level_max", 50)):
        value = payload.get(key)
        if value is None:
            query[key] = None
            continue
        number = _integer(value)
        if number is None or not 0 <= number <= ceiling:
            raise GatewayError(422, "invalid_range", "%s fora do intervalo." % key)
        query[key] = number
    for key in ("cp_min", "level_min", "atk_min", "def_min", "sta_min"):
        max_key = key.replace("_min", "_max")
        if query.get(key) is not None and query.get(max_key) is not None and query[key] > query[max_key]:
            raise GatewayError(422, "invalid_range", "%s maior que %s." % (key, max_key))
    lat = _number(payload.get("latitude"))
    lon = _number(payload.get("longitude"))
    radius = _number(payload.get("radius_km"))
    if lat is not None or lon is not None:
        if not _valid_coordinates(lat, lon):
            raise GatewayError(422, "invalid_coordinates", "Centro de busca inválido.")
    if radius is not None and not 0 <= radius <= 20000:
        raise GatewayError(422, "invalid_range", "radius_km fora do intervalo.")
    query.update({"latitude": lat, "longitude": lon, "radius_km": radius})
    limit = payload.get("limit", DEFAULT_PAGE_LIMIT)
    if not isinstance(limit, int) or isinstance(limit, bool) or not 1 <= limit <= MAX_PAGE_LIMIT:
        raise GatewayError(422, "invalid_limit", "limit deve estar entre 1 e %d." % MAX_PAGE_LIMIT)
    query["limit"] = limit
    pokemon_ids = payload.get("pokemon_ids")
    if pokemon_ids is not None:
        if not isinstance(pokemon_ids, list) or not all(isinstance(v, int) and 1 <= v <= 1025
                                                        for v in pokemon_ids):
            raise GatewayError(422, "invalid_pokemon_ids", "pokemon_ids deve conter ids 1..1025.")
        query["pokemon_ids"] = list(pokemon_ids)
    query["origin_kinds"] = [value for value in (payload.get("origin_kinds") or [])
                             if value in ORIGIN_KINDS]
    return query


def validate_import_payload(payload: dict) -> dict:
    if not isinstance(payload, dict):
        raise GatewayError(422, "invalid_body", "Corpo deve ser um objeto JSON.")
    source = payload.get("source")
    if source not in ("pgsharp", "ipogo", "import"):
        raise GatewayError(422, "invalid_source", "Importação aceita source=pgsharp|ipogo|import.")
    rows = payload.get("events")
    if not isinstance(rows, list):
        raise GatewayError(422, "invalid_events", "Campo 'events' deve ser uma lista.")
    if len(rows) > MAX_EVENTS:
        raise GatewayError(422, "too_many_events", "Limite de %d eventos por importação." % MAX_EVENTS)
    return {"source": source, "events": rows, "note": str(payload.get("note") or "")[:500]}


# ---------------------------------------------------------------------------------------------
# Store and filtering
# ---------------------------------------------------------------------------------------------


class DedupTtlStore:
    """In-memory store keyed by (source_id, event_id), with TTL and provenance separation."""

    def __init__(self, max_events: int = MAX_EVENTS):
        self.max_events = max_events
        self._lock = threading.RLock()
        self._events: dict[tuple[str, str], dict] = {}
        self._order: list[tuple[str, str]] = []
        self.imports: list[dict] = []

    def _key(self, event: dict) -> tuple[str, str]:
        return event["source_id"], event["event_id"]

    def add_many(self, events, now_ms: int | None = None) -> dict:
        now = now_ms or int(time.time() * 1000)
        created, updated, duplicates, expired = [], [], 0, 0
        with self._lock:
            for event in events:
                if event is None:
                    continue
                expires = event.get("expires_at") or 0
                if expires and expires <= now:
                    expired += 1
                    continue
                key = self._key(event)
                if key in self._events:
                    previous = self._events[key]
                    if previous.get("retrieved_at", 0) >= event.get("retrieved_at", 0):
                        duplicates += 1
                        continue
                    self._events[key] = event
                    updated.append(event)
                    continue
                self._events[key] = event
                self._order.append(key)
                created.append(event)
                while len(self._order) > self.max_events:
                    oldest = self._order.pop(0)
                    self._events.pop(oldest, None)
        return {"created": created, "updated": updated, "duplicates": duplicates, "expired": expired}

    def values(self, now_ms: int | None = None) -> list[dict]:
        now = now_ms or int(time.time() * 1000)
        with self._lock:
            return [dict(event) for event in self._events.values()
                    if not (event.get("expires_at") and event["expires_at"] <= now)]

    def count(self) -> int:
        with self._lock:
            return len(self._events)

    def record_import(self, report: dict) -> dict:
        with self._lock:
            report["id"] = "imp-%d" % (len(self.imports) + 1)
            report["received_at"] = int(time.time() * 1000)
            self.imports.append(report)
            return dict(report)

    def import_by_id(self, import_id: str):
        with self._lock:
            for report in self.imports:
                if report["id"] == import_id:
                    return dict(report)
        return None


def _haversine_km(lat1, lon1, lat2, lon2):
    import math
    radius = 6371.0088
    d_lat = math.radians(lat2 - lat1)
    d_lon = math.radians(lon2 - lon1)
    a = (math.sin(d_lat / 2) ** 2
         + math.cos(math.radians(lat1)) * math.cos(math.radians(lat2)) * math.sin(d_lon / 2) ** 2)
    return radius * 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))


def apply_filters(query: dict, events: list[dict], now_ms: int | None = None) -> dict:
    """Real filtering with honest counts: received, expired, filtered, unsupported."""
    now = now_ms or int(time.time() * 1000)
    sources = set(query.get("sources") or [])
    kind = query.get("kind")
    pokemon_ids = set(query.get("pokemon_ids") or [])
    active = {}
    for key in ("iv_min", "iv_max", "atk_min", "atk_max", "def_min", "def_max", "sta_min",
                "sta_max", "cp_min", "cp_max", "level_min", "level_max", "radius_km",
                "latitude", "longitude"):
        if query.get(key) is not None:
            active[key] = query[key]
    received = len(events)
    expired = 0
    filtered = 0
    kept = []
    candidates = [event for event in events
                  if (not sources or event["source_id"] in sources)
                  and (kind is None or event["kind"] == kind)
                  and (not query.get("origin_kinds")
                       or event["origin_kind"] in query["origin_kinds"])]
    field_for = {"iv_min": "iv_percent", "iv_max": "iv_percent", "atk_min": "iv_atk",
                 "atk_max": "iv_atk", "def_min": "iv_def", "def_max": "iv_def",
                 "sta_min": "iv_sta", "sta_max": "iv_sta", "cp_min": "cp", "cp_max": "cp",
                 "level_min": "level", "level_max": "level"}
    present = set()
    for event in candidates:
        for key, field in field_for.items():
            if key in active and event.get(field) is not None:
                present.add(key)
    unsupported = sorted({key for key in field_for if key in active and key not in present})
    for event in candidates:
        expires = event.get("expires_at") or 0
        if expires and expires <= now:
            expired += 1
            continue
        if pokemon_ids and event.get("pokemon_id") not in pokemon_ids:
            filtered += 1
            continue
        ok = True
        for key, field in field_for.items():
            if key not in active:
                continue
            value = event.get(field)
            if value is None:
                ok = False
                break
            if key.endswith("_min") and value < active[key]:
                ok = False
                break
            if key.endswith("_max") and value > active[key]:
                ok = False
                break
        if not ok:
            filtered += 1
            continue
        if "radius_km" in active and "latitude" in active and "longitude" in active:
            if event.get("latitude") is None or event.get("longitude") is None:
                filtered += 1
                continue
            distance = _haversine_km(active["latitude"], active["longitude"],
                                     event["latitude"], event["longitude"])
            if distance > active["radius_km"]:
                filtered += 1
                continue
            event["distance_km"] = round(distance, 3)
        kept.append(event)
    limit = query.get("limit") or DEFAULT_PAGE_LIMIT
    kept.sort(key=lambda event: event.get("distance_km", float("inf")))
    page = kept[:limit]
    return {"items": page, "received": received, "shown": len(page), "filtered": filtered,
            "expired": expired, "has_more": len(kept) > limit,
            "unsupported_filters": unsupported}


# ---------------------------------------------------------------------------------------------
# Rate limiting, auth, webhooks
# ---------------------------------------------------------------------------------------------


class RateLimiter:
    """Fixed-window limiter per key; returns the seconds to wait when exhausted."""

    def __init__(self, limit: int = 60, window_seconds: int = 60):
        self.limit = limit
        self.window = window_seconds
        self._lock = threading.Lock()
        self._hits: dict[str, list[float]] = {}

    def check(self, key: str, now: float | None = None) -> tuple[bool, int]:
        now = now or time.time()
        with self._lock:
            window_start = now - self.window
            hits = [stamp for stamp in self._hits.get(key, []) if stamp > window_start]
            if len(hits) >= self.limit:
                # Elapsed is clamped at zero: the wall clock can step backwards between two calls,
                # and a negative elapsed would otherwise advertise Retry-After longer than the window.
                elapsed = max(0.0, now - hits[0])
                retry = int(math.ceil(self.window - elapsed))
                self._hits[key] = hits
                return False, max(1, min(self.window, retry))
            hits.append(now)
            self._hits[key] = hits
            return True, 0


class WebhookDispatcher:
    """Signed deliveries with idempotency, anti-replay, retry, DLQ and revocation."""

    def __init__(self, secret: str, delivery=None, clock=None, sleeper=None):
        self.secret = secret.encode("utf-8") if secret else b""
        self._clock = clock or time.time
        self._sleep = sleeper or (lambda seconds: None)
        self._delivery = delivery or self._post
        self._lock = threading.RLock()
        self.subscriptions: dict[str, dict] = {}
        self.delivered: dict[str, dict] = {}
        self.dead_letters: list[dict] = []
        self.attempts: dict[str, int] = {}
        self._seen_nonces: dict[str, float] = {}

    # -- subscriptions ------------------------------------------------------------------------
    @staticmethod
    def _validate_url(url) -> str:
        if not isinstance(url, str) or not url:
            raise GatewayError(422, "invalid_url", "Informe a URL HTTPS do destino do webhook.")
        parsed = urlparse(url)
        if parsed.scheme != "https":
            raise GatewayError(422, "insecure_url",
                               "O destino do webhook precisa ser HTTPS; HTTP não é aceito.")
        if not parsed.hostname or parsed.hostname in ("localhost", "127.0.0.1", "0.0.0.0", "::1"):
            raise GatewayError(422, "insecure_url",
                               "Destino precisa de um host público; localhost não é aceito.")
        if parsed.username or parsed.password:
            raise GatewayError(422, "invalid_url", "A URL não pode conter credenciais.")
        return url

    def create_subscription(self, payload: dict) -> dict:
        if not isinstance(payload, dict):
            raise GatewayError(422, "invalid_body", "Corpo deve ser um objeto JSON.")
        url = self._validate_url(payload.get("url"))
        events = payload["events"] if "events" in payload else ["import.completed"]
        if not isinstance(events, list) or not events:
            raise GatewayError(422, "invalid_events", "Informe ao menos um tipo de evento.")
        allowed = {"import.completed", "preset.created", "preset.updated", "preset.deleted",
                   "source.status_changed", "spawn.created", "spawn.updated", "spawn.expired",
                   "raid.created", "quest.created", "rocket.created"}
        unknown = [event for event in events if event not in allowed]
        if unknown:
            raise GatewayError(422, "invalid_events", "Eventos desconhecidos: %s" % unknown)
        filters = payload.get("filters")
        if filters is not None and not isinstance(filters, dict):
            raise GatewayError(422, "invalid_filters", "'filters' deve ser um objeto.")
        with self._lock:
            subscription_id = "sub-%d" % (len(self.subscriptions) + 1)
            subscription = {"id": subscription_id, "url": url, "events": list(events),
                            "filters": filters or {}, "active": True,
                            "created_at": int(self._clock() * 1000)}
            self.subscriptions[subscription_id] = subscription
            return dict(subscription)

    def revoke(self, subscription_id: str) -> bool:
        with self._lock:
            subscription = self.subscriptions.get(subscription_id)
            if not subscription:
                return False
            subscription["active"] = False
            subscription["revoked_at"] = int(self._clock() * 1000)
            return True

    def list_subscriptions(self) -> list[dict]:
        with self._lock:
            return [dict(subscription) for subscription in self.subscriptions.values()]

    # -- signing ------------------------------------------------------------------------------
    def sign(self, event_id: str, timestamp: int, body: str) -> str:
        message = ("%s.%d.%s" % (event_id, timestamp, body)).encode("utf-8")
        return hmac.new(self.secret, message, hashlib.sha256).hexdigest()

    def verify(self, event_id: str, timestamp: int, body: str, signature: str,
               now: int | None = None) -> tuple[bool, str]:
        """Constant-time signature check plus timestamp window and replay rejection."""
        if not self.secret:
            return False, "segredo do webhook não configurado"
        expected = self.sign(event_id, timestamp, body)
        if not hmac.compare_digest(expected, signature or ""):
            return False, "assinatura inválida"
        current = int(now if now is not None else self._clock())
        if abs(current - timestamp) > WEBHOOK_TOLERANCE_SECONDS:
            return False, "timestamp fora da janela tolerada (possível replay)"
        nonce = "%s:%d" % (event_id, timestamp)
        with self._lock:
            if nonce in self._seen_nonces:
                return False, "evento já recebido (replay)"
            self._seen_nonces[nonce] = current
            if len(self._seen_nonces) > REPLAY_CACHE_MAX:
                oldest = sorted(self._seen_nonces.items(), key=lambda item: item[1])
                for key, _ in oldest[:REPLAY_CACHE_MAX // 2]:
                    self._seen_nonces.pop(key, None)
        return True, ""

    # -- delivery -----------------------------------------------------------------------------
    @staticmethod
    def _post(url: str, body: str, headers: dict) -> int:
        request = urllib.request.Request(url, data=body.encode("utf-8"),
                                         headers=headers, method="POST")
        try:
            with urllib.request.urlopen(request, timeout=10) as response:
                return response.status
        except urllib.error.HTTPError as error:
            return error.code
        except Exception:
            return 0

    def dispatch(self, event: dict) -> dict:
        """Delivers to every matching active subscription; records DLQ entries on failure."""
        event_id = event.get("id")
        if not event_id:
            raise GatewayError(422, "invalid_event", "Evento sem id.")
        if event_id in self.delivered:
            return {"idempotent": True, "delivered": 0, "failed": 0}
        body = json.dumps(event, ensure_ascii=False, separators=(",", ":"), sort_keys=True)
        timestamp = int(self._clock())
        signature = self.sign(event_id, timestamp, body)
        headers = {"Content-Type": "application/json", "X-Event-Id": event_id,
                   "X-Event-Timestamp": str(timestamp), "X-Event-Signature": signature,
                   "User-Agent": "PGBR-Gateway/1.0"}
        delivered, failed = 0, 0
        with self._lock:
            targets = [dict(subscription) for subscription in self.subscriptions.values()
                       if subscription["active"]]
        for subscription in targets:
            if event["type"] not in subscription["events"]:
                continue
            filters = subscription.get("filters") or {}
            if filters.get("source_id") and event.get("source_id") != filters["source_id"]:
                continue
            if filters.get("kind") and event.get("kind") and event["kind"] != filters["kind"]:
                continue
            if filters.get("min_iv") is not None:
                if event.get("iv_percent") is None or event["iv_percent"] < filters["min_iv"]:
                    continue
            if filters.get("radius_km") is not None and event.get("latitude") is not None:
                distance = _haversine_km(filters["latitude"], filters["longitude"],
                                         event["latitude"], event["longitude"])
                if distance > filters["radius_km"]:
                    continue
            attempts = 0
            status = 0
            while attempts < WEBHOOK_MAX_ATTEMPTS:
                attempts += 1
                status = self._delivery(subscription["url"], body, headers)
                if 200 <= status < 300:
                    break
                if status in (401, 403, 429):
                    break  # a refusal is never retried aggressively
                self._sleep(WEBHOOK_BACKOFF_SECONDS[min(attempts - 1,
                                                        len(WEBHOOK_BACKOFF_SECONDS) - 1)])
            self.attempts[event_id] = attempts
            if 200 <= status < 300:
                delivered += 1
            else:
                failed += 1
                with self._lock:
                    self.dead_letters.append({"event": event, "subscription": subscription["id"],
                                              "status": status, "attempts": attempts,
                                              "at": int(self._clock() * 1000)})
                    if len(self.dead_letters) > DLQ_MAX:
                        self.dead_letters.pop(0)
        with self._lock:
            self.delivered[event_id] = {"delivered": delivered, "failed": failed,
                                        "at": int(self._clock() * 1000)}
        return {"idempotent": False, "delivered": delivered, "failed": failed}

    def dead_letter_queue(self) -> list[dict]:
        with self._lock:
            return [dict(entry) for entry in self.dead_letters]


# ---------------------------------------------------------------------------------------------
# OpenAPI 3.1 contract
# ---------------------------------------------------------------------------------------------


def openapi_document() -> dict:
    event_properties = {
        "source_id": {"type": "string"}, "event_id": {"type": "string"},
        "kind": {"type": "string", "enum": list(KINDS)},
        "origin_kind": {"type": "string", "enum": list(ORIGIN_KINDS)},
        "pokemon_id": {"type": ["integer", "null"], "minimum": 1},
        "latitude": {"type": ["number", "null"], "minimum": -90, "maximum": 90},
        "longitude": {"type": ["number", "null"], "minimum": -180, "maximum": 180},
        "coordinate_precision": {"type": "string", "enum": ["exact", "approximate", "unavailable"]},
        "iv_percent": {"type": ["number", "null"], "minimum": 0, "maximum": 100},
        "iv_atk": {"type": ["integer", "null"], "minimum": 0, "maximum": 15},
        "iv_def": {"type": ["integer", "null"], "minimum": 0, "maximum": 15},
        "iv_sta": {"type": ["integer", "null"], "minimum": 0, "maximum": 15},
        "cp": {"type": ["integer", "null"], "minimum": 0},
        "level": {"type": ["integer", "null"], "minimum": 0, "maximum": 50},
        "gender": {"type": ["string", "null"], "enum": ["MALE", "FEMALE", "GENDERLESS", None]},
        "size": {"type": ["string", "null"], "enum": ["XXS", "XS", "S", "M", "XL", "XXL", None]},
        "weather_boost": {"type": ["boolean", "null"]},
        "pvp_great_rank": {"type": ["integer", "null"]},
        "pvp_ultra_rank": {"type": ["integer", "null"]},
        "shiny_observed": {"type": ["boolean", "null"]},
        "observed_at": {"type": "integer"}, "retrieved_at": {"type": "integer"},
        "expires_at": {"type": "integer"}, "is_demo": {"type": "boolean"},
        "data_quality": {"type": "string"}, "coverage": {"type": ["string", "null"]},
        "source_url": {"type": ["string", "null"]},
    }
    source_schema = {
        "type": "object",
        "required": ["source_id", "status", "origin_kind"],
        "properties": {
            "source_id": {"type": "string", "enum": list(SOURCE_IDS)},
            "status": {"type": "string",
                       "enum": ["connected", "unverified", "unavailable", "blocked", "local_only"]},
            "origin_kind": {"type": "string", "enum": list(ORIGIN_KINDS)},
            "supports_webhook": {"type": "boolean"},
            "live_feed_available": {"type": "boolean"},
            "supports_remote_search": {"type": "boolean"},
            "supports_pagination": {"type": "boolean"},
            "coverage": {"type": "string"},
            "attribution": {"type": "string"},
            "note": {"type": "string"},
            "last_verified_at": {"type": ["integer", "null"]},
        },
    }
    return {
        "openapi": "3.1.0",
        "info": {
            "title": "PGBR Data Gateway",
            "version": SPEC_VERSION,
            "description": ("API própria do PGBR: normalização, importação pessoal, filtros e "
                            "assinaturas de webhook. Nenhum caminho pertence a PokeList, iFlowGo, "
                            "PoGoCoordinates, PGSharp ou iPogo, e nada aqui afirma que exista feed "
                            "real de terceiros. A coleta remota fica desativada até que uma fonte "
                            "tenha permissão explícita verificada."),
        },
        "servers": [{"url": "/", "description": "Same origin (local ou proxy do projeto)"}],
        "security": [{"bearerAuth": []}],
        "paths": {
            "/v1/health": {"get": {"security": [], "summary": "Saúde do gateway",
                                   "responses": {"200": {"description": "Disponível"}}}},
            "/v1/sources": {"get": {"summary": "Fontes e estado real de conexão",
                                    "responses": {"200": {"description": "Lista de fontes",
                                                          "content": {"application/json": {"schema": {
                                                              "type": "array",
                                                              "items": source_schema}}}}}}},
            "/v1/capabilities": {"get": {"summary": "Matriz de capacidades e filtros",
                                         "parameters": [{"name": "source", "in": "query",
                                                         "required": False, "schema": {"type": "string"}}],
                                         "responses": {"200": {"description": "Capacidades"}}}},
            "/v1/search": {"post": {"summary": "Busca sobre eventos permitidos e disponíveis",
                                    "requestBody": {"required": True, "content": {"application/json": {
                                        "schema": {"type": "object", "required": ["sources"],
                                                   "properties": {"sources": {"type": "array",
                                                                              "items": {"type": "string"},
                                                                              "minItems": 1},
                                                                  "kind": {"type": "string"},
                                                                  "iv_min": {"type": ["number", "null"]},
                                                                  "iv_max": {"type": ["number", "null"]},
                                                                  "limit": {"type": "integer", "minimum": 1,
                                                                            "maximum": MAX_PAGE_LIMIT}}}}}},
                                    "responses": {"200": {"description": "Resultados reais (vazio se ausentes)"},
                                                  "401": {"description": "Sem token"},
                                                  "422": {"description": "Filtro inválido"},
                                                  "429": {"description": "Limite respeitado; Retry-After"}}}},
            "/v1/imports/pgsharp": {"post": {"summary": "Importação de dados próprios convertidos",
                                             "responses": {"202": {"description": "Importação aceita"},
                                                           "422": {"description": "Arquivo inválido"}}}},
            "/v1/imports/ipogo": {"post": {"summary": "Importação de exportação pessoal",
                                           "responses": {"202": {"description": "Importação aceita"},
                                                         "422": {"description": "Formato não suportado"}}}},
            "/v1/imports/{import_id}": {"get": {"summary": "Relatório de uma importação",
                                                "parameters": [{"name": "import_id", "in": "path",
                                                                "required": True,
                                                                "schema": {"type": "string"}}],
                                                "responses": {"200": {"description": "Relatório"},
                                                              "404": {"description": "Não encontrada"}}}},
            "/v1/presets": {"get": {"summary": "Presets do usuário", "responses": {"200": {
                "description": "Presets"}}},
                "post": {"summary": "Criar preset validado", "requestBody": {"required": True,
                    "content": {"application/json": {"schema": {"type": "object"}}}},
                    "responses": {"201": {"description": "Preset criado"},
                                  "422": {"description": "Preset inválido"}}}},
            "/v1/presets/{preset_id}": {"delete": {"summary": "Remover preset", "parameters": [
                {"name": "preset_id", "in": "path", "required": True, "schema": {"type": "string"}}],
                "responses": {"204": {"description": "Removido"}}}},
            "/v1/events": {"get": {"summary": "Eventos observados com cursor e TTL",
                                   "parameters": [{"name": "source", "in": "query", "required": False,
                                                   "schema": {"type": "string"}}],
                                   "responses": {"200": {"description": "Página autorizada"}}}},
            "/v1/webhooks/subscriptions": {
                "get": {"summary": "Listar assinaturas", "responses": {"200": {"description": "Assinaturas"}}},
                "post": {"summary": "Criar assinatura com URL HTTPS validada",
                         "requestBody": {"required": True, "content": {"application/json": {
                             "schema": {"type": "object", "required": ["url"],
                                        "properties": {"url": {"type": "string", "format": "uri"},
                                                       "events": {"type": "array",
                                                                  "items": {"type": "string"}}}}}}},
                         "responses": {"201": {"description": "Assinatura criada"},
                                       "422": {"description": "Destino inseguro ou filtros inválidos"}}}},
            "/v1/webhooks/subscriptions/{subscription_id}": {"delete": {
                "summary": "Revogar assinatura", "parameters": [{"name": "subscription_id",
                                                                "in": "path", "required": True,
                                                                "schema": {"type": "string"}}],
                "responses": {"204": {"description": "Revogada"}}}},
            "/v1/webhooks/dead-letters": {"get": {"summary": "Fila de mensagens não entregues",
                                                  "responses": {"200": {"description": "DLQ"}}}},
        },
        "components": {"securitySchemes": {"bearerAuth": {"type": "http", "scheme": "bearer"}}},
    }


# ---------------------------------------------------------------------------------------------
# Gateway
# ---------------------------------------------------------------------------------------------


class Gateway:
    """The whole service: policy, store, filters, presets and webhook dispatch."""

    def __init__(self, token: str = "", secret: str = "", allow_remote: bool = False,
                 rate_limit: int = 60, dispatcher: WebhookDispatcher | None = None):
        self.token = token
        self.policy = PolicyGate(allow_remote=allow_remote)
        self.store = DedupTtlStore()
        self.presets: dict[str, dict] = {}
        self.limiter = RateLimiter(limit=rate_limit)
        self.webhooks = dispatcher or WebhookDispatcher(secret)
        self.source_status: dict[str, str] = {key: value["status"] for key, value in SOURCES.items()}
        self.last_verified: dict[str, int] = {}
        self._lock = threading.RLock()

    # -- auth ---------------------------------------------------------------------------------
    def authorize(self, headers) -> None:
        if not self.token:
            raise GatewayError(503, "not_configured",
                               "PGBR_GATEWAY_TOKEN não configurado: a API recusa chamadas autenticadas.")
        header = headers.get("Authorization", "")
        if not header.startswith("Bearer "):
            raise GatewayError(401, "unauthorized", "Envie Authorization: Bearer <token>.")
        if not hmac.compare_digest(header[7:].strip(), self.token):
            raise GatewayError(401, "unauthorized", "Token inválido.")

    # -- sources ------------------------------------------------------------------------------
    def sources(self) -> list[dict]:
        result = []
        for source_id, declared in SOURCES.items():
            entry = dict(declared)
            entry["source_id"] = source_id
            entry["status"] = self.source_status.get(source_id, declared["status"])
            entry["last_verified_at"] = self.last_verified.get(source_id)
            entry["origin_kind"] = declared["origin_kind"]
            entry["supports_webhook"] = bool(declared["supports_webhook"])
            entry["live_feed_available"] = bool(declared["live_feed_available"])
            result.append(entry)
        return result

    def capabilities(self, source_id: str | None = None) -> dict:
        sources = self.sources()
        if source_id:
            sources = [entry for entry in sources if entry["source_id"] == source_id]
            if not sources:
                raise GatewayError(404, "unknown_source", "Fonte desconhecida: %s" % source_id)
        return {"version": API_VERSION, "sources": sources,
                "filters": {
                    "iv_percent": "0..100 por fonte; IV 0 é observação real e não ausência",
                    "iv_ads": "0..15 por componente quando a fonte informa",
                    "pvp": "rank GL/UL somente quando a fonte informa",
                    "radius_km": "recusa centro inválido em vez de ampliar a busca",
                },
                "remote_collection": self.policy.allow_remote,
                "remote_collection_reason": ("ativado por configuração explícita"
                                             if self.policy.allow_remote else
                                             "desativado: nenhuma página/fonte com permissão "
                                             "explícita verificada"),
                "integrations": list(INTEGRATION_IDS),
                "not_integrations": ["pokemond", "pgtools"]}

    # -- search -------------------------------------------------------------------------------
    def search(self, payload: dict) -> dict:
        query = validate_search_payload(payload)
        rows = self.store.values()
        outcome = apply_filters(query, rows)
        items = [self._public_event(event) for event in outcome["items"]]
        unsupported = outcome["unsupported_filters"]
        return {"version": API_VERSION, "items": items, "has_more": outcome["has_more"],
                "received": outcome["received"], "shown": outcome["shown"],
                "filtered": outcome["filtered"], "expired": outcome["expired"],
                "retrieved_at": int(time.time() * 1000),
                "filters_applied": sorted(key for key in query if key not in
                                          ("sources", "kind", "limit", "origin_kinds")
                                          and query.get(key) is not None),
                "unsupported_filters": unsupported,
                "source_status": [entry for entry in self.sources()
                                  if entry["source_id"] in query["sources"]],
                "note": ("Nenhum resultado confirmado para estes critérios."
                         if not items else
                         "Resultados limitados aos eventos realmente disponíveis no gateway.")}

    @staticmethod
    def _public_event(event: dict) -> dict:
        public = {key: event.get(key) for key in (
            "source_id", "event_id", "kind", "origin_kind", "pokemon_id", "form_id", "name",
            "latitude", "longitude", "coordinate_precision", "iv_percent", "iv_atk", "iv_def",
            "iv_sta", "cp", "level", "gender", "size", "weather_boost", "pvp_great_rank",
            "pvp_ultra_rank", "shiny_observed", "observed_at", "retrieved_at", "expires_at",
            "is_demo", "data_quality", "coverage", "source_url", "distance_km")}
        return public

    # -- imports ------------------------------------------------------------------------------
    def import_events(self, payload: dict) -> dict:
        body = validate_import_payload(payload)
        now = int(time.time() * 1000)
        events, rejected = [], []
        for index, row in enumerate(body["events"]):
            if isinstance(row, dict) and (row.get("origin_kind") == "demo_fixture"
                                          or row.get("is_demo") is True):
                rejected.append({"row": index + 1,
                                 "reason": "fixture sintética só é aceita em testes automatizados"})
                continue
            event = normalise(row, source_id=body["source"],
                              origin_kind="personal_import", retrieved_at=now)
            if event is None:
                rejected.append({"row": index + 1, "reason": "sem identidade nem coordenadas válidas"})
                continue
            # A personal import carries coordinates the user supplied: (0,0)/out-of-range is a data
            # error, not an "unavailable" coordinate. Only an explicit
            # coordinate_precision=unavailable row (a filter/route exported without location) is kept.
            if event["latitude"] is None and \
                    row.get("coordinate_precision") != "unavailable":
                rejected.append({"row": index + 1,
                                 "reason": "coordenadas inválidas ou ausentes (0,0 não é coordenada)"})
                continue
            allowed, reason = self.policy.admit(event)
            if not allowed:
                rejected.append({"row": index + 1, "reason": reason})
                continue
            events.append(event)
        stored = self.store.add_many(events, now)
        report = {"source": body["source"], "origin_kind": "personal_import",
                  "imported": len(stored["created"]), "duplicates": stored["duplicates"],
                  "expired": stored["expired"], "rejected": rejected,
                  "note": body["note"] or ("Importação pessoal do usuário; não é feed ao vivo."
                                           if body["source"] in ("pgsharp", "ipogo") else ""),
                  # Nothing created and nothing deduplicated means nothing entered the store:
                  # that is a failed import, not a success with an empty body.
                  "status": ("completed" if stored["created"] or stored["duplicates"]
                             else "failed")}
        report = self.store.record_import(report)
        if stored["created"]:
            self._emit({"id": "import-%s" % report["id"], "type": "import.completed",
                        "source_id": body["source"], "import_id": report["id"],
                        "imported": report["imported"], "rejected": len(rejected),
                        "duplicates": report["duplicates"]})
        return report

    def preset_create(self, payload: dict) -> dict:
        if not isinstance(payload, dict) or not isinstance(payload.get("name"), str) \
                or not payload["name"].strip():
            raise GatewayError(422, "invalid_preset", "Informe um nome para o preset.")
        query = payload.get("query") or {}
        filters = {}
        for key in ("iv_min", "iv_max"):
            value = _number(query.get(key))
            if value is not None and not 0 <= value <= 100:
                raise GatewayError(422, "invalid_preset", "%s fora de 0..100." % key)
            filters[key] = value
        if filters["iv_min"] is not None and filters["iv_max"] is not None \
                and filters["iv_min"] > filters["iv_max"]:
            raise GatewayError(422, "invalid_preset", "IV mínimo maior que o máximo.")
        for key in ("cp_min", "cp_max", "level_min", "level_max"):
            value = _integer(query.get(key))
            if value is not None and value < 0:
                raise GatewayError(422, "invalid_preset", "%s negativo." % key)
            filters[key] = value
        with self._lock:
            preset_id = "preset-%d" % (len(self.presets) + 1)
            preset = {"id": preset_id, "name": payload["name"].strip(), "query": filters,
                      "version": 2, "favorite": bool(payload.get("favorite", False)),
                      "created_at": int(time.time() * 1000)}
            self.presets[preset_id] = preset
        self._emit({"id": "preset-created-%s" % preset_id, "type": "preset.created",
                    "preset_id": preset_id, "name": preset["name"]})
        return dict(preset)

    def preset_delete(self, preset_id: str) -> bool:
        with self._lock:
            preset = self.presets.pop(preset_id, None)
        if not preset:
            return False
        self._emit({"id": "preset-deleted-%s" % preset_id, "type": "preset.deleted",
                    "preset_id": preset_id, "name": preset["name"]})
        return True

    def presets_list(self, source_id: str | None = None) -> list[dict]:
        with self._lock:
            presets = [dict(preset) for preset in self.presets.values()]
        if source_id:
            presets = [preset for preset in presets if preset.get("source_id") == source_id]
        return presets

    # -- events -------------------------------------------------------------------------------
    def events_page(self, source_id: str | None = None, limit: int = DEFAULT_PAGE_LIMIT) -> dict:
        events = self.store.values()
        if source_id:
            if source_id not in SOURCE_IDS:
                raise GatewayError(404, "unknown_source", "Fonte desconhecida: %s" % source_id)
            events = [event for event in events if event["source_id"] == source_id]
        events.sort(key=lambda event: event.get("retrieved_at", 0), reverse=True)
        page = events[:limit]
        return {"version": API_VERSION, "items": [self._public_event(event) for event in page],
                "has_more": len(events) > limit, "count": len(events),
                "retrieved_at": int(time.time() * 1000)}

    # -- webhooks -----------------------------------------------------------------------------
    def _emit(self, event: dict) -> None:
        """Only real changes reach subscriptions; no synthetic spawns are ever emitted."""
        if event["type"] in ("spawn.created", "spawn.updated", "spawn.expired", "raid.created",
                             "quest.created", "rocket.created"):
            # These are accepted only for live external rows, which this gateway cannot produce
            # while remote collection is disabled: refuse rather than simulate.
            return
        self.webhooks.dispatch(event)

    def emit_live_event(self, event: dict) -> dict:
        """Used by an authorised live connector; personal imports are refused here."""
        if not self.policy.may_emit_live_event(event):
            raise GatewayError(422, "not_live",
                               "Eventos de spawn só podem sair de fonte externa ao vivo; "
                               "importação pessoal e catálogo estático não geram spawn.")
        if not self.policy.allow_remote:
            raise GatewayError(403, "remote_disabled",
                               "Coleta remota desativada: nenhum evento ao vivo pode ser emitido.")
        return self.webhooks.dispatch(event)

    # -- HTTP ---------------------------------------------------------------------------------
    def handle(self, method: str, path: str, query: dict, body, headers) -> tuple[int, dict, dict]:
        try:
            return self._route(method, path, query, body, headers)
        except GatewayError as error:
            extra = {"Retry-After": str(error.retry_after)} if error.retry_after else {}
            return error.status, {"error": {"code": error.code, "message": error.message}}, extra

    def _route(self, method: str, path: str, query: dict, body, headers) -> tuple[int, dict, dict]:
        now = int(time.time() * 1000)
        if path == "/v1/health":
            return 200, {"status": "ok", "service": SERVICE, "version": API_VERSION,
                         "spec": SPEC_VERSION, "time": now,
                         "remote_collection": self.policy.allow_remote,
                         "sources_connected": [source for source, status in self.source_status.items()
                                               if status == "connected"],
                         "stored_events": self.store.count(),
                         "webhook_secret_configured": bool(self.webhooks.secret)}, {}
        if path == "/openapi.json":
            return 200, openapi_document(), {}
        if path.startswith("/v1/"):
            allowed, retry = self.limiter.check(headers.get("X-Forwarded-For", "local"))
            if not allowed:
                return 429, {"error": {"code": "rate_limited",
                                       "message": "Limite de consultas respeitado. Tente em %d s."
                                                  % retry}}, {"Retry-After": str(retry)}
            self.authorize(headers)
        if method == "GET" and path == "/v1/sources":
            return 200, {"version": API_VERSION, "sources": self.sources()}, {}
        if method == "GET" and path == "/v1/capabilities":
            return 200, self.capabilities(query.get("source", [None])[0]), {}
        if method == "POST" and path == "/v1/search":
            return 200, self.search(body or {}), {}
        if method == "POST" and path in ("/v1/imports/pgsharp", "/v1/imports/ipogo"):
            payload = dict(body or {})
            payload["source"] = "pgsharp" if path.endswith("pgsharp") else "ipogo"
            report = self.import_events(payload)
            return (202 if report["status"] == "completed" else 422), report, {}
        if method == "GET" and path.startswith("/v1/imports/"):
            report = self.store.import_by_id(path.rsplit("/", 1)[-1])
            if not report:
                raise GatewayError(404, "not_found", "Importação não encontrada.")
            return 200, report, {}
        if method == "GET" and path == "/v1/presets":
            return 200, {"version": API_VERSION,
                         "presets": self.presets_list(query.get("source", [None])[0])}, {}
        if method == "POST" and path == "/v1/presets":
            return 201, self.preset_create(body or {}), {}
        if method == "DELETE" and path.startswith("/v1/presets/"):
            removed = self.preset_delete(path.rsplit("/", 1)[-1])
            if not removed:
                raise GatewayError(404, "not_found", "Preset não encontrado.")
            return 204, {}, {}
        if method == "GET" and path == "/v1/events":
            limit = query.get("limit", [str(DEFAULT_PAGE_LIMIT)])[0]
            try:
                limit_value = int(limit)
            except ValueError:
                raise GatewayError(422, "invalid_limit", "limit deve ser um inteiro.")
            if not 1 <= limit_value <= MAX_PAGE_LIMIT:
                raise GatewayError(422, "invalid_limit",
                                   "limit deve estar entre 1 e %d." % MAX_PAGE_LIMIT)
            return 200, self.events_page(query.get("source", [None])[0], limit_value), {}
        if method == "GET" and path == "/v1/webhooks/subscriptions":
            return 200, {"version": API_VERSION,
                         "subscriptions": self.webhooks.list_subscriptions()}, {}
        if method == "POST" and path == "/v1/webhooks/subscriptions":
            return 201, self.webhooks.create_subscription(body or {}), {}
        if method == "DELETE" and path.startswith("/v1/webhooks/subscriptions/"):
            revoked = self.webhooks.revoke(path.rsplit("/", 1)[-1])
            if not revoked:
                raise GatewayError(404, "not_found", "Assinatura não encontrada.")
            return 204, {}, {}
        if method == "GET" and path == "/v1/webhooks/dead-letters":
            return 200, {"version": API_VERSION, "dead_letters": self.webhooks.dead_letter_queue()}, {}
        raise GatewayError(404, "not_found", "Rota não encontrada: %s" % path)


class _Handler(BaseHTTPRequestHandler):
    server_version = "PGBR-Gateway/1.0"
    gateway: Gateway

    def _read_body(self):
        length = int(self.headers.get("Content-Length", "0") or 0)
        if length <= 0:
            return {}
        if length > MAX_BODY_BYTES:
            raise GatewayError(413, "too_large", "Corpo maior que 2 MB.")
        raw = self.rfile.read(length)
        try:
            return json.loads(raw.decode("utf-8"))
        except Exception:
            raise GatewayError(400, "invalid_json", "Corpo não é JSON válido.")

    def _query(self):
        parsed = urlparse(self.path)
        return parsed.path, parse_qs(parsed.query)

    def _send(self, status: int, payload, headers=None):
        body = b"" if status == 204 else json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        if status != 204:
            self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        for key, value in (headers or {}).items():
            self.send_header(key, value)
        self.end_headers()
        if self.command != "HEAD":
            self.wfile.write(body)

    def _dispatch(self, method: str):
        try:
            path, query = self._query()
            body = self._read_body() if method in ("POST", "PUT", "PATCH") else {}
            status, payload, headers = self.gateway.handle(method, path, query, body, self.headers)
            self._send(status, payload, headers)
        except Exception as error:  # never leak internals
            self._send(500, {"error": {"code": "internal_error",
                                       "message": "Erro interno do gateway.",
                                       "detail": type(error).__name__}}, {})

    def do_GET(self):
        self._dispatch("GET")

    def do_POST(self):
        self._dispatch("POST")

    def do_DELETE(self):
        self._dispatch("DELETE")

    def log_message(self, *args):
        pass


def serve(host: str | None = None, port: int | None = None, gateway: Gateway | None = None):
    gateway = gateway or Gateway(token=os.environ.get("PGBR_GATEWAY_TOKEN", ""),
                                 secret=os.environ.get("PGBR_GATEWAY_SECRET", ""),
                                 allow_remote=os.environ.get("PGBR_GATEWAY_ALLOW_REMOTE", "") == "1")
    handler = type("BoundHandler", (_Handler,), {"gateway": gateway})
    server = ThreadingHTTPServer((host or os.environ.get("PGBR_GATEWAY_HOST", "127.0.0.1"),
                                  int(port or os.environ.get("PGBR_GATEWAY_PORT", "8080"))), handler)
    server.daemon_threads = True
    return server, gateway


def selftest() -> int:
    """Offline checks used by CI and by `--selftest`."""
    from types import SimpleNamespace
    gateway = Gateway(token="t", secret="s" * 32)
    headers = {"Authorization": "Bearer t", "X-Forwarded-For": "selftest"}
    status, health, _ = gateway.handle("GET", "/v1/health", {}, {}, headers)
    assert status == 200 and health["status"] == "ok", health
    status, sources, _ = gateway.handle("GET", "/v1/sources", {}, {}, headers)
    assert status == 200 and len(sources["sources"]) == 7, sources
    assert all(entry["status"] != "connected" for entry in sources["sources"]), "nothing is connected"
    now = int(time.time() * 1000)
    status, report, _ = gateway.handle("POST", "/v1/imports/pgsharp", {}, {
        "events": [
            {"id": "a", "pokemon_id": 25, "latitude": -23.55, "longitude": -46.63,
             "iv_percent": 100, "cp": 500, "level": 20, "expires_at": now + 600000},
            {"id": "b", "pokemon_id": 25, "latitude": 0, "longitude": 0},
            {"id": "a", "pokemon_id": 25, "latitude": -23.55, "longitude": -46.63,
             "iv_percent": 100, "expires_at": now + 600000},
        ]}, headers)
    assert status == 202 and report["imported"] == 1, report
    assert report["duplicates"] == 1 and len(report["rejected"]) == 1, report
    status, search, _ = gateway.handle("POST", "/v1/search", {}, {
        "sources": ["pgsharp"], "iv_min": 100, "iv_max": 100}, headers)
    assert status == 200 and len(search["items"]) == 1, search
    status, empty, _ = gateway.handle("POST", "/v1/search", {}, {
        "sources": ["pgsharp"], "iv_min": 0, "iv_max": 0}, headers)
    assert status == 200 and empty["items"] == [], empty
    assert empty["note"].startswith("Nenhum resultado"), empty
    print("PGBR gateway selftest: OK")
    return 0


if __name__ == "__main__":
    import sys

    if "--selftest" in sys.argv:
        raise SystemExit(selftest())
    server, gateway = serve()
    print("PGBR Data Gateway on %s:%d (remote collection: %s)"
          % (server.server_address[0], server.server_address[1],
             "enabled" if gateway.policy.allow_remote else "disabled"), flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
