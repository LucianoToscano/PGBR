"""Run actual native seasonal event and VGP Safari fixtures in an isolated Linux host build.

Compilation and execution happen in TemporaryDirectory. No Android hooks,
game login, real-world event activity, credentials, or account are required.
"""
from pathlib import Path
import os
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
NATIVE = ROOT / "pgrazor/native"
# Each source is a self-contained fixture with its own main() that includes or
# links the production source under test; never use a mocked duplicate policy.
FIXTURES = (
    "events/event_collector_test.cpp",
    "route_gift_policy_test.cpp",
    "safari_always_hit_test.cpp",
    "safari_throws_test.cpp",
    "safari_vgp_actions_test.cpp",
)


def main():
    compiler = shutil.which("g++")
    if not compiler:
        raise SystemExit("g++ missing; Linux host compiler required")
    with tempfile.TemporaryDirectory(prefix="pgbr-seasonal-fixtures-") as scratch:
        for index, name in enumerate(FIXTURES):
            source = NATIVE / name
            if not source.is_file():
                raise SystemExit("Fixture missing: " + name)
            output = Path(scratch) / ("fixture_" + str(index))
            command = [
                compiler, "-std=c++17", "-O0", "-pthread",
                "-I", str(NATIVE), "-I", str(ROOT / "pgrazor/test/stubs"),
                str(source), "-o", str(output), "-ldl",
            ]
            subprocess.run(command, cwd=ROOT, check=True, timeout=120)
            subprocess.run([str(output)], cwd=ROOT, check=True, timeout=180)
            print("PASS seasonal native fixture:", name, flush=True)
    print(f"PASS {len(FIXTURES)} native Halloween/Safari/VGP/Mateo fixtures; Android runtime NOT tested")


if __name__ == "__main__":
    main()
