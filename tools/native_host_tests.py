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
FIXTURES=(
    'battle_timing_policy_test.cpp',
    'auto_raid_policy_test.cpp',
    'batch_evolution_policy_test.cpp',
    'buddy_emotion_policy_test.cpp',
    'incense_guard_policy_test.cpp',
    'remote_gym_policy_test.cpp',
    'research_policy_test.cpp',
    'shiny_scan_policy_test.cpp',
    'battle_counter_test.cpp',
    'smart_battle_party_test.cpp',
    'pvp_rank_test.cpp',
)

# Explicitly linked, host-only tests of the real gameplay controller. These are
# not Android hooks: PGR_*_DIRECT_TEST replaces runtime dynamic linking with the
# local exported native table. Both use simulated objects and lifecycle events.
LINKED_FIXTURES = {
    'rocket_combat_test': {
        'source': ('rocket_combat_test.cpp', 'rocket_combat.cpp', 'rocket_actions.cpp'),
        'flags': ('-DPGR_RC_DIRECT_TEST', '-pthread'),
    },
    'battles_test': {
        'source': ('battles_test.cpp', 'battles.cpp'),
        'flags': ('-DPGR_BATTLE_DIRECT_TEST', '-pthread'),
    },
}

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
        for name, cfg in LINKED_FIXTURES.items():
            target = Path(tmp) / name
            native = ROOT / 'pgrazor/native'
            command = [args.compiler, '-std=c++17', '-O2', '-Wall', '-Wextra',
                       '-I', str(native), *cfg['flags'],
                       *[str(native / src) for src in cfg['source']],
                       '-o', str(target)]
            subprocess.run(command, check=True, timeout=120)
            subprocess.run([str(target)], check=True, timeout=150)
            print('PASS linked host C++:', name, flush=True)
    count = len(FIXTURES) + len(LINKED_FIXTURES)
    print(f'Validated {count} host C++ fixtures (including PvP/Rocket and battle controllers); '
          'Android device and full NDK suite not covered.')

if __name__=='__main__':
    main()
