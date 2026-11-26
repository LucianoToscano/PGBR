"""Free-only proposal dispatcher; ChatGPT and Arena use shared coordination. No model output is executed."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import urllib.error
import urllib.request

REPO = "LucianoToscano/PGBR"
ISSUE = "320"
AGENTS = {"chatgpt", "gemini", "arena"}
ID = re.compile(r"[A-Z][A-Z0-9-]{2,47}")
MODEL = re.compile(r"gemini-[A-Za-z0-9._-]{1,100}")
MARKER = re.compile(r"^<!-- pgbr-auto-dev:v1:([A-Z][A-Z0-9-]{2,47}):reserved -->$")
PAUSE_MARKER = re.compile(r"^<!-- pgbr-auto-dev:v1:([A-Z][A-Z0-9-]{2,47}):paused -->$")
MAX_OUTPUT = 4096
MAX_BYTES = 256 * 1024
MAX_CALLS = 1


def validate_queue(data):
    if data.get("version") != 1 or not isinstance(data.get("batches"), list):
        raise ValueError("invalid queue schema")
    if len(data["batches"]) > 100:
        raise ValueError("queue too large")
    seen = set()
    for row in data["batches"]:
        if set(row) != {"id", "agent", "tasks"} or not ID.fullmatch(row["id"]):
            raise ValueError("invalid batch")
        if row["id"] in seen or row["agent"] not in AGENTS:
            raise ValueError("duplicate ID or unknown agent")
        seen.add(row["id"])
        tasks = row["tasks"]
        if not isinstance(tasks, list) or not 1 <= len(tasks) <= 15:
            raise ValueError("invalid task count")
        if any(not isinstance(t, str) or not t.strip() or len(t) > 800 for t in tasks):
            raise ValueError("invalid task")
    return data["batches"]


def reserved_ids(pages):
    # Only the repository workflow bot may make durable reservations.
    return {m[1] for page in pages for c in page
            if c.get("user", {}).get("login") == "github-actions[bot]"
            and (m := MARKER.fullmatch(c.get("body", "").split("\n", 1)[0]))}


def paused_batch(pages):
    """Latest bot-recorded quota/access block; comments are chronological."""
    blocked = None
    for page in pages:
        for comment in page:
            if comment.get("user", {}).get("login") != "github-actions[bot]":
                continue
            marker = PAUSE_MARKER.fullmatch(comment.get("body", "").split("\n", 1)[0])
            if marker:
                blocked = marker[1]
    return blocked


def select(rows, used, configured):
    selected = []
    busy = set()
    for row in rows:
        agent = row["agent"]
        if row["id"] in used or agent in busy or agent not in configured or agent != "gemini":
            continue
        selected.append(row)
        busy.add(agent)
        if len(selected) >= MAX_CALLS:
            break
    return selected


def free_configuration(env):
    """Explicit owner attestation; a key alone cannot prove billing status."""
    if (env.get("PGBR_AUTO_DEV_MODE") != "free" or
            env.get("PGBR_FREE_BILLING_CONFIRMED") != "true" or
            env.get("PGBR_GITHUB_FREE_BUDGET_CONFIRMED") != "true"):
        return {}
    # Reuse the legacy secret only on explicit selection and billing attestation.
    source = env.get("PGBR_GEMINI_KEY_SOURCE", "dedicated")
    if source not in ("dedicated", "existing"):
        return {}
    key = env.get("GEMINI_API_KEY" if source == "existing" else "PGBR_GEMINI_FREE_KEY", "")
    model = env.get("PGBR_GEMINI_FREE_MODEL", "")
    return {"gemini": (model, key)} if key and MODEL.fullmatch(model) else {}


def request_spec(agent, model, key, prompt):
    if agent != "gemini":
        raise ValueError("provider disabled in free mode")
    if not MODEL.fullmatch(model):
        raise ValueError("invalid Gemini model ID")
    return ("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent",
            {"x-goog-api-key": key},
            {"contents": [{"parts": [{"text": prompt}]}],
             "generationConfig": {"maxOutputTokens": MAX_OUTPUT}})


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        raise urllib.error.HTTPError(req.full_url, code, "redirect refused", headers, fp)


def response_text(agent, data):
    if agent != "gemini":
        raise ValueError("provider disabled in free mode")
    choice = data["candidates"][0]
    if choice.get("finishReason") != "STOP":
        raise ValueError("incomplete response")
    chunks = [p["text"] for p in choice["content"]["parts"] if "text" in p and not p.get("thought")]
    result = "\n".join(chunks).strip()
    if not result or len(result.encode("utf-8")) > MAX_BYTES:
        raise ValueError("empty or oversized response")
    return result


def call(row, model, key):
    # Only the reviewed queue text is transmitted. No files, issue comments,
    # private APKs, environment data or credentials are put into the prompt.
    prompt = ("PGBR LT049, papel " + row["agent"] + ", lote " + row["id"] +
              ". Proponha implementações e testes concretos para as tarefas a seguir. "
              "Não alegue execução ou testes. Não amplie automação de jogo, nem contorne "
              "controles, licenças ou privacidade. Não solicite segredos. Preserve V5. "
              "Responda com proposta técnica em PT-BR, critérios de aceite e riscos.\n" +
              "\n".join(row["tasks"]))
    url, headers, body = request_spec(row["agent"], model, key, prompt)
    request = urllib.request.Request(url, json.dumps(body).encode(),
                                     {"Content-Type": "application/json", **headers}, method="POST")
    with urllib.request.build_opener(NoRedirect()).open(request, timeout=90) as stream:
        raw = stream.read(MAX_BYTES + 1)
    if len(raw) > MAX_BYTES:
        raise ValueError("oversized provider response")
    data = json.loads(raw)
    return {"proposal": response_text(row["agent"], data), "usage": data.get("usage", data.get("usageMetadata", {}))}


def gh(*args):
    # The GitHub subprocess receives only the GitHub credential, not API keys.
    env = {k: os.environ[k] for k in ("PATH", "HOME", "GH_TOKEN") if k in os.environ}
    p = subprocess.run(["gh", "api", *args], env=env, capture_output=True, text=True, timeout=30)
    if p.returncode:
        raise RuntimeError("GitHub API failed")
    return json.loads(p.stdout)


def note(body):
    return gh("repos/" + REPO + "/issues/" + ISSUE + "/comments", "--method", "POST", "-f", "body=" + body)


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--queue", type=Path, required=True)
    p.add_argument("--output", type=Path, required=True)
    p.add_argument("--live", action="store_true")
    args = p.parse_args()
    rows = validate_queue(json.loads(args.queue.read_text(encoding="utf-8")))
    if not args.live:
        print(json.dumps({"mode": "dry-run", "batches": [r["id"] for r in rows]}))
        return
    if (os.environ.get("GITHUB_REPOSITORY") != REPO or os.environ.get("GITHUB_REF") != "refs/heads/main"
            or os.environ.get("PGBR_AUTO_DEV_ENABLED") != "true"):
        raise SystemExit("live dispatch denied")
    if gh("repos/" + REPO + "/issues/" + ISSUE).get("state") != "open":
        raise SystemExit("coordination issue closed")
    configured = free_configuration(os.environ)
    if not configured:
        print("Free API not configured; no request made.")
        return
    pages = gh("repos/" + REPO + "/issues/" + ISSUE + "/comments?per_page=100",
               "--paginate", "--slurp")
    blocked = paused_batch(pages)
    if blocked and os.environ.get("PGBR_GEMINI_RESUME_AFTER") != blocked:
        print("Gemini paused after quota/access error; no request made.")
        return
    used = reserved_ids(pages)
    selected = select(rows, used, configured)
    args.output.mkdir(parents=True, exist_ok=True)
    # Reserve before calling. No automatic retry, paid fallback or quota bypass.
    for row in selected:
        note("<!-- pgbr-auto-dev:v1:" + row["id"] + ":reserved -->\n"
             "Lote reservado para Gemini free; proposta, não execução de código.")
        state = "failed"
        try:
            report = call(row, *configured[row["agent"]])
            payload = json.dumps(report, ensure_ascii=False, indent=2).encode("utf-8")
            (args.output / (row["id"] + ".json")).write_bytes(payload)
            state = "proposal"
            digest = hashlib.sha256(payload).hexdigest()
        except urllib.error.HTTPError as error:
            state = "quota-or-access-blocked" if error.code in (401, 403, 429) else "failed"
            digest = "none"
            if state == "quota-or-access-blocked":
                note("<!-- pgbr-auto-dev:v1:" + row["id"] + ":paused -->\n"
                     "Gemini pausado: verificar cota, acesso e faturamento antes de retomar.")
        except Exception:
            digest = "none"
        run = os.environ.get("GITHUB_RUN_ID", "")
        attempt = os.environ.get("GITHUB_RUN_ATTEMPT", "")
        note("AUTO DEV " + row["id"] + ": " + state + "; SHA256 " + digest +
             "; run " + run + " attempt " + attempt +
             ". Proposta não aplicada. Sem retry automático ou fallback pago.")
    print("Dispatched:", len(selected))


if __name__ == "__main__":
    main()
