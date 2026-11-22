"""Compile and exercise PGBR's real local Pokémon live-feed matcher.

O matcher usa org.json (catalog de nomes), que o JDK não traz. Compilar sem essa
dependência falha com "package org.json does not exist" — era a falha B do CI.
Aqui a dependência é resolvida na ordem real de quem compila o app:

1. android.jar do SDK (é exatamente o org.json que o app usa no aparelho);
2. o jar que o build oficial usa (dex_bridges);
3. os stubs versionados em tools/sniper_jvm/stubs (mesma superfície do Android).

Se nenhuma existir, o teste é INCONCLUSIVO: falha com mensagem explícita em vez de
"passar" sem ter compilado nada.
"""
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

from test_control_java import java_tools

ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parent


def org_json_classpath():
    """Classpath que fornece org.json, ou None quando o ambiente não tem nenhuma opção."""
    candidates = []
    sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    if sdk:
        for platform in ("android-36", "android-36.1", "android-35", "android-34"):
            candidates.append(Path(sdk) / "platforms" / platform / "android.jar")
    # org.json que o build oficial usa, se já foi baixado.
    for relative in (ROOT / "build", REPO / "pgrazor" / "build"):
        if relative.is_dir():
            candidates.extend(sorted(relative.glob("*.jar")))
    for path in candidates:
        if path.is_file():
            return str(path)
    return None


def org_json_stub_sources():
    return sorted((REPO / "tools/sniper_jvm/stubs/org/json").glob("*.java"))


class PokemonFeedSearchTests(unittest.TestCase):
    def test_matcher(self):
        compiler, runtime = java_tools()
        classpath = org_json_classpath()
        with tempfile.TemporaryDirectory(prefix="pgbr-pokemon-search-") as work:
            src = [ROOT / "src/dev/pgrazor/PokemonFeedSearch.java",
                   ROOT / "test/PokemonFeedSearchTest.java"]
            if classpath is not None:
                build_args = [str(compiler), "-encoding", "UTF-8", "-classpath", classpath,
                              "-d", work, *map(str, src)]
            else:
                stubs = org_json_stub_sources()
                self.assertTrue(stubs, "org.json precisa vir do android.jar, de um jar do build "
                                       "ou dos stubs versionados — nenhum dos três foi encontrado")
                build_args = [str(compiler), "-encoding", "UTF-8", "-d", work,
                              *map(str, stubs), *map(str, src)]
            build = subprocess.run(build_args, capture_output=True, text=True, timeout=60)
            self.assertEqual(build.returncode, 0,
                             "o matcher real não compilou.\n" + build.stdout + build.stderr)
            run = subprocess.run(
                [str(runtime), "-cp", work, "dev.pgrazor.PokemonFeedSearchTest"],
                capture_output=True, text=True, timeout=60)
            self.assertEqual(run.returncode, 0, run.stdout + run.stderr)
            self.assertIn("PASS:", run.stdout)


if __name__ == "__main__":
    unittest.main()
