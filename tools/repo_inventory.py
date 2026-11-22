#!/usr/bin/env python3
"""Inventário SOMENTE LEITURA dos arquivos rastreados do PGBR.

Não apaga, move, compacta, sincroniza, instala, publica nem altera o Git.
Use: python -B tools/repo_inventory.py [--json] [--strict]
A leitura prévia de PGBR_ARQUIVO_MESTRE_UNICO.md continua obrigatória.
"""
from __future__ import annotations

import argparse
from collections import Counter, defaultdict
import hashlib
import json
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
SENSITIVE_SUFFIXES = {".apk", ".apkm", ".apks", ".aab",
                      ".jks", ".keystore", ".p12", ".pem", ".key"}
GENERATED_SUFFIXES = {".pyc", ".pyo", ".log", ".dmp", ".hprof",
                      ".tmp", ".orig", ".rej", ".swp", ".swo"}


def category(path: str) -> str:
    """Classificação de inventário; NÃO é política automática de remoção."""
    if path.startswith(("upstream-history/", "upstream-workflows/",
                        "pgrazor/vendor/")):
        return "historico_terceiros"
    if path.startswith(("pgrazor/test/", "website/tests/")):
        return "testes"
    if path.startswith((".github/workflows/",)):
        return "ci_workflows"
    if path.startswith(("pgrazor/assets/", "pgrazor/branding/",
                        "website/public/images/", "website/public/logo",
                        "pgrazor/root-installer/res/")):
        return "assets"
    if path.startswith(("docs/",)) or path.endswith(".md"):
        return "documentacao"
    if path.startswith(("tools/",)):
        return "ferramentas"
    if path.startswith(("pgrazor/", "website/src/")):
        return "codigo"
    return "configuracao_outros"


def concern(path: str) -> str | None:
    """Apenas alerta: nenhuma ação destrutiva resulta destas classificações."""
    p = Path(path)
    name = p.name.lower()
    if name == ".env" or (name.startswith(".env.") and name != ".env.example"):
        return "ambiente_local"
    if p.suffix.lower() in SENSITIVE_SUFFIXES:
        return "binario_ou_material_sensivel"
    if p.suffix.lower() in GENERATED_SUFFIXES:
        return "residuo_gerado"
    if any(x in path.split("/") for x in ("__pycache__", "node_modules", ".pytest_cache")):
        return "diretorio_gerado"
    return None


def duplicate_groups(records: list[dict]) -> list[list[str]]:
    by_hash = defaultdict(list)
    for record in records:
        if record.get("sha256") and record.get("bytes", 0) > 0:
            by_hash[(record["sha256"], record["bytes"])].append(record["path"])
    return sorted((sorted(v) for v in by_hash.values() if len(v) > 1),
                  key=lambda group: (group[0], len(group)))


def tracked_paths(root: Path) -> list[str]:
    result = subprocess.run(["git", "-C", str(root), "ls-files", "-z"],
                            stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True)
    return sorted(p.decode("utf-8", "surrogateescape")
                  for p in result.stdout.split(b"\0") if p)


def inventory(root: Path = ROOT) -> dict:
    paths = tracked_paths(root)
    records = []
    missing = []
    for rel in paths:
        path = root / rel
        if path.is_symlink():
            records.append({"path": rel, "bytes": 0, "sha256": None,
                            "category": category(rel), "note": "symlink_nao_lido"})
            continue
        if not path.is_file():
            missing.append(rel)
            continue
        digest = hashlib.sha256()
        with path.open("rb") as stream:
            for chunk in iter(lambda: stream.read(1024 * 1024), b""):
                digest.update(chunk)
        records.append({"path": rel, "bytes": path.stat().st_size,
                        "sha256": digest.hexdigest(), "category": category(rel)})
    counts = Counter(r["category"] for r in records)
    alerts = [{"path": p, "reason": c} for p in paths if (c := concern(p))]
    return {"tracked": len(paths), "readable": len(records),
            "bytes": sum(r["bytes"] for r in records),
            "categories": dict(sorted(counts.items())),
            "duplicates_identical": duplicate_groups(records),
            "attention_only": alerts, "missing": missing,
            "files": records}


def as_markdown(report: dict) -> str:
    out = [
        "# Inventário somente leitura — PGBR",
        "",
        "Referência obrigatória: PGBR_ARQUIVO_MESTRE_UNICO.md.",
        "Nenhum item abaixo é autorizado para exclusão automática.",
        "",
        f"- Rastreados: **{report['tracked']}**; legíveis: **{report['readable']}**",
        f"- Bytes legíveis: **{report['bytes']}**",
        f"- Grupos de cópias byte a byte: **{len(report['duplicates_identical'])}**",
        "",
        "## Áreas",
    ]
    out += [f"- {name}: {count}" for name, count in report["categories"].items()]
    out += ["", "## Conteúdo idêntico (preservar até verificar consumidores)"]
    for paths in report["duplicates_identical"]:
        out.append("- " + " | ".join(paths))
    if not report["duplicates_identical"]:
        out.append("Nenhum grupo encontrado.")
    out += ["", "## Revisão manual requerida"]
    for alert in report["attention_only"]:
        out.append(f"- {alert['reason']}: {alert['path']}")
    for path in report["missing"]:
        out.append(f"- rastreado, mas ausente: {path}")
    if not report["attention_only"] and not report["missing"]:
        out.append("Nenhum nome de arquivo com alerta simples encontrado.")
    out += ["", "Este inventário não verifica referências de runtime, licenças, "
            "direitos, credenciais dentro do conteúdo nem homologação Android."]
    return "\n".join(out) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--json", action="store_true",
                        help="emitir relatório completo como JSON em stdout")
    parser.add_argument("--strict", action="store_true",
                        help="retornar 2 se houver arquivos rastreados suspeitos ou ausentes")
    args = parser.parse_args()
    try:
        report = inventory()
    except (OSError, subprocess.CalledProcessError) as exc:
        print(f"Falha de inventário: {exc}", file=sys.stderr)
        return 1
    print(json.dumps(report, ensure_ascii=False, indent=2) if args.json
          else as_markdown(report), end="" if args.json else "")
    return 2 if args.strict and (report["attention_only"] or report["missing"]) else 0


if __name__ == "__main__":
    raise SystemExit(main())
