"""Run every device-free test; naming conventions keep new suites in CI."""
from pathlib import Path
import json
import os
import re
import shutil
import subprocess
import sys
from zoneinfo import ZoneInfo, ZoneInfoNotFoundError

ROOT = Path(__file__).resolve().parents[1]
PROJECT = ROOT / 'pgrazor'
sys.path.insert(0, str(PROJECT))
from script_bundle import bundle


def run(*args, **kwargs):
    subprocess.run([str(x) for x in args], cwd=ROOT, check=True, **kwargs)


def preflight():
    """Explain missing prerequisites before invoking subprocesses."""
    missing=[]
    try:
        ZoneInfo('America/Fortaleza')
    except ZoneInfoNotFoundError:
        missing.append('IANA time zone data: python -m pip install --require-hashes --only-binary=:all: -r tools/requirements-offline.txt')
    for executable in ('javac', 'java', 'node'):
        if not shutil.which(executable):
            missing.append(executable)
    sdk_root = os.environ.get('ANDROID_SDK_ROOT') or os.environ.get('ANDROID_HOME')
    if not sdk_root:
        local_app_data = os.environ.get('LOCALAPPDATA')
        sdk_root = str(Path(local_app_data) / 'Android' / 'Sdk') if local_app_data else str(Path.home() / 'Android' / 'Sdk')
    sdk = Path(sdk_root)
    platform = os.environ.get('PGRAZOR_ANDROID_PLATFORM', 'android-36')
    if not (sdk / 'platforms' / platform / 'android.jar').is_file():
        missing.append(f'Android SDK platform {platform} under {sdk}')
    if not (sdk / 'sources' / platform / 'org' / 'json').is_dir():
        missing.append(f'Android SDK source package {platform} under {sdk}')
    if missing:
        raise SystemExit('PGBR verification prerequisites missing:\n  - ' +
                         '\n  - '.join(missing) +
                         '\nInstall JDK 17+, Node.js 22+, and Android SDK platform/sources; ' +
                         'see docs/BUILDING.md. No files were modified.')
    version = subprocess.run(['javac', '-version'], capture_output=True, text=True, check=True)
    match = re.search(r'javac\s+(\d+)', version.stdout + version.stderr)
    if not match or int(match.group(1)) < 17:
        raise SystemExit(f'PGBR requires JDK 17+ (found {version.stdout + version.stderr})')


def main():
    preflight()

    run(sys.executable, '-B', ROOT / 'tools/game_update.py', 'generate', '--check')
    run(sys.executable, '-B', ROOT / 'tools/game_update.py', 'audit')
    run(sys.executable, '-B', ROOT / 'tools/native_routes_schema.py', '--check')
    run(sys.executable, '-B', ROOT / 'tools/native_gift_schema.py', '--check')
    run(sys.executable, '-B', ROOT / 'tools/pvp_data.py', 'check')
    run(sys.executable, '-B', PROJECT / 'build.py', '--test-only')
    run(sys.executable, '-B', '-m', 'unittest', 'discover', '-s', PROJECT / 'test', '-p', 'test_*.py')
    for path in [*sorted((PROJECT / 'test').glob('*-test.cjs')),
                 *sorted((PROJECT / 'vendor').glob('test_*.cjs'))]:
        print('Checking', path.relative_to(ROOT), flush=True)
        run('node', path)
    for path in [PROJECT / 'embedded-agent.js',
                 *sorted((PROJECT / 'diagnostics').glob('*.js')),
                 *sorted((PROJECT / 'runtime').rglob('*.js'))]:
        run('node', '--check', path)
    # Parse retained research bundles; production has no JavaScript assets.
    for name in json.loads((PROJECT / 'runtime/bundles.json').read_text(encoding='utf-8')):
        run('node', '--check', input=bundle(PROJECT, name))


if __name__ == '__main__':
    main()
