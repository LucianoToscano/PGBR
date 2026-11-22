#!/usr/bin/env python3
"""Compile and run the sniper engine on a plain JVM, without Android Studio or Gradle.

Why this exists: this repository's Java is normally only compiled by the Android build, so a typo in a
new class could reach a PR unchecked. This script compiles the *real* engine sources plus the panel
against minimal stand-ins for the Android/PGBR symbols they touch, runs the engine test suite, and
fails the build on any compiler error or failing assertion.

What it does NOT do (state this in any report):
  * it does not build an APK, sign anything, or touch the assets;
  * the Android classes it compiles against are hand-written stubs in ``stubs/``; compiling proves
    syntax, signature and type compatibility with those stubs, not runtime behaviour on a device;
  * it never contacts a provider.

Usage:
    python3 tools/sniper_jvm/compile_engine.py            # compile + run tests
    python3 tools/sniper_jvm/compile_engine.py --keep     # keep the temporary build directory

The compiler comes from ``$ECJ_JAR`` or ``tools/sniper_jvm/ecj.jar`` and runs on the JDK from
``$JAVA_HOME`` or ``java`` on ``PATH``. When either is missing the script exits with status 77
("skipped") and prints exactly what is missing, instead of pretending to pass. CI installs a JDK and
downloads ECJ before calling it, so the job really compiles.
"""
import argparse
import os
import pathlib
import shutil
import subprocess
import sys
import tempfile

ROOT = pathlib.Path(__file__).resolve().parents[2]
STUBS = pathlib.Path(__file__).resolve().parent / 'stubs'

ENGINE = [
    'pgrazor/src/dev/pgrazor/LiveFeedData.java',
    'pgrazor/src/dev/pgrazor/PokemonFeedSearch.java',
    'pgrazor/src/dev/pgrazor/SpawnContract.java',
    'pgrazor/src/dev/pgrazor/SpawnSearch.java',
    'pgrazor/src/dev/pgrazor/SpawnFilters.java',
    'pgrazor/src/dev/pgrazor/SpawnProfiles.java',
    'pgrazor/src/dev/pgrazor/SpawnSourceRegistry.java',
    'pgrazor/src/dev/pgrazor/PokeListPartnerClient.java',
    'pgrazor/src/dev/pgrazor/SpeciesGroups.java',
    # Contrato de URL/erro do servidor RocketMap: a matriz 401/403/429 e a recusa de
    # credencial-na-URL são verificadas aqui, não em revisão manual.
    # LiveFeedClient/Control stubs come from the stubs dir already; only the transport
    # and its parser are added, so the error contract is compiled for real.
    'pgrazor/src/dev/pgrazor/RocketMapFeedData.java',
    'pgrazor/src/dev/pgrazor/RocketMapFeedClient.java',
]

UI = [
    'pgrazor/src/dev/pgrazor/SpawnConnectors.java',
    'pgrazor/src/dev/pgrazor/SniperPanel.java',
    'pgrazor/src/dev/pgrazor/SnipeAdvancedFilterDialog.java',
]

TESTS = [
    'pgrazor/test/SnipeEngineTest.java',
    'pgrazor/test/HttpErrorMatrixTest.java',
    'pgrazor/test/PokeListPartnerClientTest.java',
]


def java_launcher():
    home = os.environ.get('JAVA_HOME')
    if home:
        candidate = pathlib.Path(home) / 'bin' / 'java'
        if candidate.exists():
            return str(candidate)
    found = shutil.which('java')
    if found:
        return found
    here = pathlib.Path('/usr/local/lib/python3.11/dist-packages/jdk4py/java-runtime/bin/java')
    return str(here) if here.exists() else None


def ecj_jar():
    explicit = os.environ.get('ECJ_JAR')
    if explicit and pathlib.Path(explicit).exists():
        return explicit
    bundled = pathlib.Path(__file__).resolve().parent / 'ecj.jar'
    return str(bundled) if bundled.exists() else None


# Stubs that must be replaced by the real implementation when that class is compiled for real.
# Otherwise ECJ sees two definitions of the same type and the whole run fails.
STUB_SUPERSEDED = {'RocketMapFeedClient.java'}


def stubs():
    return sorted(p for p in STUBS.rglob('*.java') if p.name not in STUB_SUPERSEDED)


def compile_with(java, jar, sources, out):
    command = [java, '-cp', jar, 'org.eclipse.jdt.internal.compiler.batch.Main',
               '-source', '11', '-target', '11', '-encoding', 'UTF-8', '-nowarn',
               '-d', str(out)] + [str(s) for s in sources]
    result = subprocess.run(command, capture_output=True, text=True, timeout=600)
    return result


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--keep', action='store_true')
    args = parser.parse_args()

    java = java_launcher()
    jar = ecj_jar()
    missing = []
    if java is None:
        missing.append('JDK (set JAVA_HOME or put java on PATH)')
    if jar is None:
        missing.append('ECJ compiler jar (set ECJ_JAR or place tools/sniper_jvm/ecj.jar)')
    if missing:
        print('SKIPPED: ' + '; '.join(missing))
        print('Nothing was compiled or executed, so no result is claimed.')
        return 77

    work = pathlib.Path(tempfile.mkdtemp(prefix='pgbr-sniper-jvm-'))
    try:
        engine_out = work / 'engine'
        ui_out = work / 'ui'
        engine_out.mkdir()
        ui_out.mkdir()

        engine_sources = stubs() + [ROOT / p for p in ENGINE] + [ROOT / p for p in TESTS]
        result = compile_with(java, jar, engine_sources, engine_out)
        print('== engine + tests')
        print((result.stdout or '') + (result.stderr or ''))
        if result.returncode != 0:
            print('FAIL: o motor não compilou.')
            return 1

        run = subprocess.run([java, '-Dstdout.encoding=UTF-8', '-cp', str(engine_out), 'SnipeEngineTest'],
                             capture_output=True, text=True, timeout=300)
        output = (run.stdout or '') + (run.stderr or '')
        print(output.strip())
        if run.returncode != 0 or '0 falha' not in output:
            print('FAIL: a suíte do motor reportou falhas.')
            return 1

        # Matriz HTTP/URL: roda aqui também, para que "compilou" nunca seja confundido com "passou".
        run = subprocess.run([java, '-Dstdout.encoding=UTF-8', '-cp', str(engine_out),
                              'dev.pgrazor.HttpErrorMatrixTest'],
                             capture_output=True, text=True, timeout=120)
        output = (run.stdout or '') + (run.stderr or '')
        print(output.strip())
        if run.returncode != 0 or 'PASS:' not in output:
            print('FAIL: a matriz de erro HTTP não passou.')
            return 1

        partner = subprocess.run([java, '-Dstdout.encoding=UTF-8', '-cp', str(engine_out),
                                  'dev.pgrazor.PokeListPartnerClientTest'],
                                 capture_output=True, text=True, timeout=120)
        partner_output = (partner.stdout or '') + (partner.stderr or '')
        print(partner_output.strip())
        if partner.returncode != 0 or 'PASS:' not in partner_output:
            print('FAIL: contrato PokeList Partner API.')
            return 1

        ui_sources = stubs() + [ROOT / p for p in ENGINE] + [ROOT / p for p in UI]
        result = compile_with(java, jar, ui_sources, ui_out)
        print('== painel (Sniper 1 / Sniper 2)')
        print((result.stdout or '') + (result.stderr or ''))
        if result.returncode != 0:
            print('FAIL: o painel não compilou.')
            return 1

        print('OK: motor + matriz HTTP + painel compilados e suíte verde contra os stubs.')
        print('Lembrete: isto não é uma build Android nem uma execução em aparelho.')
        return 0
    finally:
        if args.keep:
            print('build mantido em ' + str(work))
        else:
            shutil.rmtree(work, ignore_errors=True)


if __name__ == '__main__':
    sys.exit(main())
