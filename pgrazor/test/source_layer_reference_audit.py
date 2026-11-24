"""Static reference audit for the canonical source layer (no javac required).

The repository's device-free gate needs a real JDK; when it is unavailable this module still
verifies, without executing Java, that every ``Receiver.member`` reference in the audited files
resolves against a declaration in the same package (including inherited members and nested
types). It is a reference checker, not a compiler: it cannot prove assignability or overload
choice, and it skips receivers whose type it cannot infer. Unknown receivers are counted and
reported so a silent "pass" never hides an unchecked file.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "pgrazor" / "src" / "dev" / "pgrazor"

AUDITED = [
    "OriginKind.java", "SourceEvent.java", "SourceCapabilities.java", "SourceRegistry.java",
    "SourceQuery.java", "EventFilterEngine.java", "LocalEventImport.java", "LocalEventStore.java",
    "SourceSearchResult.java", "SourceAdapter.java", "SourceUnavailableException.java",
    "SourceAdapterSupport.java", "PresetCodec.java", "AlertEngine.java", "SniperSearch.java",
    "PgSharpFilterMapping.java", "LocalStoreAdapter.java", "LocalSourceAdapters.java",
    "PoGoCoordinatesSourceAdapter.java", "PGBRHundoSourceAdapter.java", "PokeListSourceAdapter.java",
    "IFlowGoSourceAdapter.java", "SourceDataStore.java", "LocalImportDialog.java",
    "SourceRadarDialog.java", "SniperDialog.java", "FeedChooser.java",
]

TYPE_DECL = re.compile(r"\b(?:class|interface|enum)\s+(\w+)(?:<[^{]*?>)?\s*"
                       r"(?:extends\s+([\w\.,\s<>]+?))?\s*(?:implements\s+([\w\.,\s<>]+?))?\s*\{")
# Declarations are matched line by line: whitespace inside the modifier and type groups is limited
# to spaces/tabs so a match can never spill into the next declaration.
MODIFIERS = r"(?:(?:public|private|protected|static|final|abstract|synchronized|native|default|" \
            r"strictfp|transient|volatile)[ \t]+)*[ \t]*"
METHOD = re.compile(r"^[ \t]*(?:@[\w.]+(?:\([^)]*\))?[ \t]*)*" + MODIFIERS +
                    r"[ \t]*([\w$][\w<>\[\],\. ]*?)[ \t]+(\w+)[ \t]*\(([^;{]*)\)\s*"
                    r"(?:throws[\w\s,\.]+)?\s*\{", re.M)
# abstract/interface methods end with ';' instead of a body; the return type must look like a type
# so a call such as "adapter.search(q);" is not mistaken for a declaration.
METHOD_SEMI = re.compile(r"^[ \t]*(?:@[\w.]+(?:\([^)]*\))?[ \t]*)*" + MODIFIERS +
                         r"[ \t]*([A-Z][\w<>\[\],\.]*|void|boolean|int|long|double|float)"
                         r"[\w<>\[\],\.]*[ \t]+(\w+)[ \t]*\(([^;{]*)\)\s*"
                         r"(?:throws[\w\s,\.]+)?\s*;", re.M)
FIELD = re.compile(r"^[ \t]*" + MODIFIERS +
                   r"[ \t]*([\w$][\w<>\[\],\. ]*?)[ \t]+(\w+)[ \t]*(?:=[^;]*)?;", re.M)
# "public final Integer atkMin, atkMax, defMin;" declares several members at once
MULTI_FIELD = re.compile(r"^[ \t]*" + MODIFIERS +
                         r"[ \t]*[\w$][\w<>\[\],\.]*[ \t]+(\w+(?:[ \t]*,[ \t]*\w+)+)"
                         r"[ \t]*(?:=[^;]*)?;", re.M)
ALWAYS = {"values", "valueOf", "name", "ordinal", "toString", "equals", "hashCode", "length",
          "getMessage", "getClass"}
VAR = re.compile(r"\b([\w$][\w<>\[\],\.]*)\s+(\w+)\s*(?:=|;|:)")
PARAM = re.compile(r"[\(\s,]([\w$][\w<>\[\],\.]*)\s+(\w+)\s*[,)]")
REF = re.compile(r"\b(\w+)\s*\.\s*(\w+)")
CTOR = re.compile(r"\bnew\s+([\w$][\w<>\[\],\.]*)\s*\(")
SKIP_RECEIVERS = {"String", "Integer", "Double", "Long", "Boolean", "Math", "System", "Object",
                  "Thread", "Looper", "Handler"}
# Android/JDK types that share a simple name with a PGBR class (the repository has its own
# `Handler`); references through them are framework calls, not PGBR API references.
EXTERNAL_TYPES = {"Handler", "Looper", "ArrayAdapter", "Toast", "Thread", "Context", "Activity",
                  "View", "Intent", "Uri", "Bitmap", "Dialog", "EditText", "TextView", "Button"}


def strip(source: str) -> str:
    """Blank out comments and literals with a single scanner (a URL inside a string is not a comment)."""
    out = []
    index, length = 0, len(source)
    while index < length:
        char = source[index]
        following = source[index + 1] if index + 1 < length else ""
        if char == "/" and following == "/":
            end = source.find("\n", index)
            end = length if end < 0 else end
            out.append(" " * (end - index))
            index = end
        elif char == "/" and following == "*":
            end = source.find("*/", index + 2)
            end = length if end < 0 else end + 2
            out.append(" " * (end - index))
            index = end
        elif char in ('"', "'"):
            quote = char
            end = index + 1
            while end < length:
                if source[end] == "\\":
                    end += 2
                    continue
                if source[end] == quote:
                    end += 1
                    break
                end += 1
            out.append('""' if quote == '"' else "''")
            out.append(" " * (end - index - 2))
            index = end
        else:
            out.append(char)
            index += 1
    return "".join(out)


def base_type(name: str) -> str:
    name = name.split("<")[0].strip()
    while name.endswith("[]"):
        name = name[:-2].strip()
    return name.split(".")[-1]


class TypeIndex:
    def __init__(self, sources):
        self.types = {}
        self.supers = {}
        for path in sources:
            text = strip(path.read_text(encoding="utf-8"))
            for match in TYPE_DECL.finditer(text):
                name = match.group(1)
                self.types.setdefault(name, set()).update(
                    member for member in self._members(text, match.end()))
                parents = []
                for group in (2, 3):
                    if match.group(group):
                        parents += [base_type(part) for part in match.group(group).split(",")]
                self.supers[name] = parents
        # nested types are declared in the same file; index them by their own name
        self.owners = {}
        for path in sources:
            text = strip(path.read_text(encoding="utf-8"))
            for match in TYPE_DECL.finditer(text):
                self.owners.setdefault(match.group(1), path.name)

    @staticmethod
    def _members(text, start):
        # the opening brace was consumed by TYPE_DECL, so the body starts at depth 1
        depth, index, body_end = 1, start, len(text)
        while index < len(text):
            if text[index] == "{":
                depth += 1
            elif text[index] == "}":
                depth -= 1
                if depth == 0:
                    body_end = index
                    break
            index += 1
        body = text[start:body_end] + "}"  # the closing brace terminates the last enum constant
        names = {m.group(2) for m in METHOD.finditer(body)}
        names |= {m.group(2) for m in METHOD_SEMI.finditer(body)}
        names |= {m.group(2) for m in FIELD.finditer(body)}
        names |= {part.strip() for m in MULTI_FIELD.finditer(body) for part in m.group(1).split(",")}
        names |= {m.group(1) for m in re.finditer(r"\b([A-Z][A-Z0-9_]*)\s*[\(,;}]", body)}
        # nested types are reachable as Outer.Nested
        names |= {m.group(1) for m in TYPE_DECL.finditer(body)}
        return names

    def has(self, type_name, member):
        if member in ALWAYS:
            return True
        type_name = base_type(type_name)
        seen, queue = set(), [type_name]
        while queue:
            current = queue.pop()
            if current in seen or current not in self.types:
                continue
            seen.add(current)
            if member in self.types[current]:
                return True
            queue.extend(self.supers.get(current, []))
        return False

    def knows(self, type_name):
        return base_type(type_name) in self.types


def audit(path: Path, index: TypeIndex):
    text = strip(path.read_text(encoding="utf-8"))
    # Java scopes are not modelled: a name declared with two different types in one file is
    # ambiguous here and is skipped instead of guessed.
    candidates = {}
    for match in VAR.finditer(text):
        candidates.setdefault(match.group(2), set()).add(match.group(1))
    for match in PARAM.finditer(text):
        candidates.setdefault(match.group(2), set()).add(match.group(1))
    declared = {name: sorted(types)[0] for name, types in candidates.items() if len(types) == 1}
    problems, checked, unknown = [], 0, 0
    for match in REF.finditer(text):
        receiver, member = match.group(1), match.group(2)
        if receiver in ("this", "super") or receiver in SKIP_RECEIVERS or member in ("class",):
            continue
        if member in ("length", "size") and receiver in declared:
            pass
        type_name = declared.get(receiver)
        if type_name is not None and base_type(type_name) in EXTERNAL_TYPES:
            continue
        if type_name is None:
            if index.knows(receiver):
                type_name = receiver
            else:
                unknown += 1
                continue
        if not index.knows(type_name):
            unknown += 1
            continue
        checked += 1
        if not index.has(type_name, member):
            problems.append("%s.%s" % (receiver, member))
    return problems, checked, unknown


def main():
    sources = sorted(SRC.glob("*.java"))
    index = TypeIndex(sources)
    failures, total_checked, total_unknown = [], 0, 0
    for name in AUDITED:
        path = SRC / name
        if not path.exists():
            failures.append("%s: arquivo ausente" % name)
            continue
        problems, checked, unknown = audit(path, index)
        total_checked += checked
        total_unknown += unknown
        if problems:
            failures.append("%s: referências não resolvidas: %s" % (name, ", ".join(sorted(set(problems)))))
    print("audited %d files, %d references checked, %d receivers unresolvable"
          % (len(AUDITED), total_checked, total_unknown))
    for failure in failures:
        print("FAIL", failure)
    if not failures:
        print("source layer reference audit: OK")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
