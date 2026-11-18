#!/usr/bin/env python3
"""Wire PT-BR translation only at presentation boundaries in PGRazor Java UI.

The English source literals remain intact. In particular, this script never
renames preference keys, API fields, JSON values, protocol tokens, enum names,
or native messages. It is designed to run on an isolated PGBR branch.
"""
from __future__ import annotations

import argparse
import collections
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[1]
JAVA = ROOT / "pgrazor" / "src" / "dev" / "pgrazor"
SINGLE = frozenset({
    "setText", "setTitle", "setMessage", "setHint", "setContentDescription",
    "setTextOn", "setTextOff", "setItems",
})
MULTI = frozenset({"setPositiveButton", "setNegativeButton", "setNeutralButton"})
METHODS = SINGLE | MULTI
PAT = re.compile(r"\.\s*(" + "|".join(sorted(METHODS, key=len, reverse=True)) + r")\s*\(")
PREFIX = "dev.pgrazor.PtBr.tr("


def first_arg_end(source: str, begin: int) -> tuple[int, str] | None:
    """Find argument boundary, respecting Java comments, literals and brackets."""
    stack: list[str] = []
    i = begin
    state = "code"
    while i < len(source):
        ch = source[i]
        following = source[i + 1] if i + 1 < len(source) else ""
        if state == "linecomment":
            if ch == "\n": state = "code"
        elif state == "blockcomment":
            if ch == "*" and following == "/": state = "code"; i += 1
        elif state in ("string", "character"):
            if ch == "\\": i += 1
            elif ch == ('"' if state == "string" else "'"): state = "code"
        elif ch == "/" and following == "/":
            state = "linecomment"; i += 1
        elif ch == "/" and following == "*":
            state = "blockcomment"; i += 1
        elif ch == '"': state = "string"
        elif ch == "'": state = "character"
        elif ch in "({[":
            stack.append({"(": ")", "{": "}", "[": "]"}[ch])
        elif ch in ")}]":
            if not stack:
                return (i, ")") if ch == ")" else None
            if stack[-1] != ch:
                return None
            stack.pop()
        elif ch == "," and not stack:
            return i, ","
        i += 1
    return None


def localize(source: str) -> tuple[str, collections.Counter]:
    patches: list[tuple[int, int, str]] = []
    counts = collections.Counter()
    for m in PAT.finditer(source):
        method = m.group(1)
        # Do not rewrite comments mentioning .setText() or a quoted string.
        before = source[source.rfind("\n", 0, m.start()) + 1:m.start()].lstrip()
        if before.startswith("//") or before.startswith("*") or before.startswith("/*"):
            continue
        match = first_arg_end(source, m.end())
        if match is None:
            continue
        end, delimiter = match
        if method in MULTI and delimiter != ",":
            continue
        if method in SINGLE and delimiter != ")":
            continue
        raw = source[m.end():end]
        argument = raw.strip()
        if not argument or argument.startswith(PREFIX) or "->" in argument:
            continue
        lspace = raw[:len(raw) - len(raw.lstrip())]
        rspace = raw[len(raw.rstrip()):]
        translated = lspace + PREFIX + argument + ")" + rspace
        patches.append((m.end(), end, translated))
        counts[method] += 1
    for start, end, replacement in reversed(patches):
        source = source[:start] + replacement + source[end:]
    return source, counts


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--apply", action="store_true", help="Update source files.")
    args = parser.parse_args()
    summary = collections.Counter()
    changed = []
    for path in sorted(JAVA.rglob("*.java")):
        if path.name == "PtBr.java":
            continue
        original = path.read_text("utf-8")
        updated, counts = localize(original)
        if updated != original:
            changed.append(str(path.relative_to(ROOT)))
            summary.update(counts)
            if args.apply:
                path.write_text(updated, encoding="utf-8", newline="")
    print(f"PT-BR UI instrumentation: {len(changed)} Java files, {sum(summary.values())} presentation calls.")
    for name, count in summary.most_common():
        print(f"  {name}: {count}")
    print(f"Changes {'applied' if args.apply else 'not applied (preview)'}")
    if not args.apply:
        for name in changed[:25]: print("  "+name)


if __name__ == "__main__":
    main()
