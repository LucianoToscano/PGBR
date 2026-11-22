"""Portable, focused host-C++ fixture selection for PR verification.

This is an allowlist, NOT a claim that all native/NDK fixtures have run.
Add a fixture only after verifying it builds without Android headers, binary
dependencies or device state; the separate Android NDK build remains required.
"""
import argparse
from pathlib import Path
import subprocess
import tempfile

ROOT=Path(__file__).resolve().parents[1]
FIXTURES=('auto_raid_policy_test.cpp',)

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--compiler',default='g++')
    args=parser.parse_args()
    with tempfile.TemporaryDirectory(prefix='pgbr-native-host-') as tmp:
        for name in FIXTURES:
            source=ROOT/'pgrazor/native'/name
            target=Path(tmp)/source.stem
            command=[args.compiler,'-std=c++17','-O2','-Wall','-Wextra',
                     '-I',str(source.parent),str(source),'-o',str(target)]
            subprocess.run(command,check=True)
            subprocess.run([str(target)],check=True)
            print('PASS host C++:',name,flush=True)
    print(f'Validated {len(FIXTURES)} host C++ fixture(s); NDK fixtures not covered.')

if __name__=='__main__':
    main()
