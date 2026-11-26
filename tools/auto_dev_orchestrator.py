"""Bounded three-provider proposal dispatcher. No model output is executed."""
import argparse
from concurrent.futures import ThreadPoolExecutor
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
AGENTS = {"chatgpt": ("OPENAI_API_KEY", "PGBR_OPENAI_MODEL"),
          "gemini": ("GEMINI_API_KEY", "PGBR_GEMINI_MODEL"),
          "arena": ("ARENA_API_KEY", "PGBR_ARENA_MODEL")}
ID = re.compile(r"[A-Z][A-Z0-9-]{2,47}")
MODEL = re.compile(r"[A-Za-z0-9][A-Za-z0-9._:/-]{0,119}")
MARKER = re.compile(r"^<!-- pgbr-auto-dev:v1:([A-Z][A-Z0-9-]{2,47}):reserved -->$")
MAX_OUTPUT = 4096
MAX_BYTES = 256 * 1024
MAX_CALLS = 3


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


def select(rows, used, configured):
    selected = []
    busy = set()
    for row in rows:
        agent = row["agent"]
        if row["id"] in used or agent in busy or agent not in configured:
            continue
        selected.append(row)
        busy.add(agent)
        if len(selected) >= MAX_CALLS:
            break
    return selected


def request_spec(agent, model, key, prompt):
    if not MODEL.fullmatch(model):
        raise ValueError("invalid model ID")
    bearer = {"Authorization": "Bearer " + key}
    if agent == "chatgpt":
        return ("https://api.openai.com/v1/responses", bearer,
                {"model": model, "input": prompt, "max_output_tokens": MAX_OUTPUT, "store": False})
    if agent == "gemini":
        if not re.fullmatch(r"[A-Za-z0-9._-]+", model):
            raise ValueError("invalid Gemini model ID")
        return ("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent",
                {"x-goog-api-key": key},
                {"contents": [{"parts": [{"text": prompt}]}],
                 "generationConfig": {"maxOutputTokens": MAX_OUTPUT}})
    if agent == "arena":
        return ("https://api.arena.ai/v1/chat/completions", bearer,
                {"model": model, "messages": [{"role": "user", "content": prompt}],
                 "max_tokens": MAX_OUTPUT, "stream": False})
    raise ValueError("unknown provider")


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        raise urllib.error.HTTPError(req.full_url, code, "redirect refused", headers, fp)


def response_text(agent, data):
    if agent == "chatgpt":
        if data.get("status") != "completed":
            raise ValueError("incomplete response")
        chunks = [p["text"] for item in data.get("output", []) if item.get("type") == "message"
                  for p in item.get("content", []) if p.get("type") == "output_text"]
    elif agent == "gemini":
        choice = data["candidates"][0]
        if choice.get("finishReason") != "STOP":
            raise ValueError("incomplete response")
        chunks = [p["text"] for p in choice["content"]["parts"] if "text" in p and not p.get("thought")]
    else:
        choice = data["choices"][0]
        if choice.get("finish_reason") != "stop":
            raise ValueError("incomplete response")
        chunks = [choice["message"]["content"]]
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
    configured = {a: (os.environ.get(model, ""), os.environ.get(key, ""))
                  for a, (key, model) in AGENTS.items()
                  if os.environ.get(key) and os.environ.get(model)}
    used = reserved_ids(gh("repos/" + REPO + "/issues/" + ISSUE + "/comments?per_page=100",
                           "--paginate", "--slurp"))
    selected = select(rows, used, configured)
    args.output.mkdir(parents=True, exist_ok=True)
    # Reserve before every potentially paid call. Cancellation/failure requires
    # a new reviewed ID; do not automatically retry an uncertain charge.
    for row in selected:
        note("<!-- pgbr-auto-dev:v1:" + row["id"] + ":reserved -->\n"
             "Lote reservado para " + row["agent"] + "; proposta, não execução de código.")
    with ThreadPoolExecutor(max_workers=MAX_CALLS) as pool:
        futures = [(row, pool.submit(call, row, *configured[row["agent"]])) for row in selected]
        for row, future in futures:
            state = "failed"
            try:
                report = future.result()
                payload = json.dumps(report, ensure_ascii=False, indent=2).encode("utf-8")
                (args.output / (row["id"] + ".json")).write_bytes(payload)
                state = "proposal"
                digest = hashlib.sha256(payload).hexdigest()
            except Exception:
                # Provider exception bodies may contain secrets. Never log them.
                digest = "none"
            run = os.environ.get("GITHUB_RUN_ID", "")
            attempt = os.environ.get("GITHUB_RUN_ATTEMPT", "")
            note("AUTO DEV " + row["id"] + ": " + state + "; SHA256 " + digest +
                 "; run " + run + " attempt " + attempt +
                 ". Proposta não aplicada. Requer auditoria independente; sem retry automático.")
    print("Dispatched:", len(selected))


if __name__ == "__main__":
    main()
