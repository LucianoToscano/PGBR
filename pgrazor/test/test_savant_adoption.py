"""Host regression coverage for conservative PGRazor-Savant ideas in PGBR.

No device, APK, root privilege, network access or external module is required.
"""
import os
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ZYGISK = ROOT / "zygisk"
NATIVE = ROOT / "native"


class SavantAdoptionTest(unittest.TestCase):
    def test_root_peer_guard_matrix(self):
        cxx = shutil.which("g++") or shutil.which("clang++")
        if not cxx:
            self.skipTest("host C++ compiler unavailable")
        with tempfile.TemporaryDirectory(prefix="pgbr-peer-matrix-") as tmp:
            program = Path(tmp) / ("peer-test.exe" if os.name == "nt" else "peer-test")
            subprocess.run(
                [cxx, "-std=c++17", "-Wall", "-Wextra", "-Werror",
                 "-I", str(ZYGISK), str(ZYGISK / "peer_guard_test.cpp"),
                 "-o", str(program)],
                check=True, timeout=30,
            )
            result = subprocess.run([str(program)], capture_output=True,
                                    text=True, check=True, timeout=15)
            self.assertIn("64 fail-closed", result.stdout)

    def test_loader_checks_peer_before_payload(self):
        source = (ZYGISK / "loader.cpp").read_text(encoding="utf-8")
        off = source.index('faccessat(dir, "pgrazor/injection-off", F_OK, 0)')
        peer = source.index('if (!peer_allows_injection(api_))')
        dex = source.index('read_at(dir, "pgrazor/classes.dex"')
        self.assertLess(off, peer)
        self.assertLess(peer, dex)
        self.assertIn("REGISTER_ZYGISK_COMPANION(peer_companion)", source)
        self.assertIn("target_ = false;", source[peer:dex])
        self.assertIn("MSG_DONTWAIT", source)
        self.assertIn("pgsavant", (ZYGISK / "peer_guard.h").read_text())

    def test_root_namespace_is_preserved(self):
        source = (ROOT / "rooted.py").read_text(encoding="utf-8")
        self.assertIn("MODULE_ID = 'pgrazor'", source)
        self.assertIn("INSTALLER_PACKAGE = 'dev.pgrazor.root'", source)

    def test_vsseeker_is_observation_only(self):
        host = (NATIVE / "combat_host.cpp").read_text(encoding="utf-8")
        probe = (NATIVE / "pvp_vsseeker_probe.inc").read_text(encoding="utf-8")
        scene = (NATIVE / "pvp_scene.h").read_text(encoding="utf-8")
        self.assertIn("install_optional_vsseeker_probe()", host)
        self.assertIn('m.name="EnterState"', probe)
        self.assertIn("meta->validate_method", probe)
        self.assertIn("pve_exclusion_clear(&tid)", probe)
        self.assertIn("g_pvp_scene.verified_vs_seeker(now_ms())", probe)
        self.assertNotIn("PGR_RC_OBS_SCOPE_ENTER", probe)
        self.assertNotIn("PGR_RC_OBS_PVP_SCOPE_ENTER", probe)
        self.assertIn("stage!=HUB||mode!=PVP", scene)
        self.assertIn("typed_hub", scene)

    def test_existing_pvp_policy_with_witness(self):
        cxx = shutil.which("g++") or shutil.which("clang++")
        if not cxx:
            self.skipTest("host C++ compiler unavailable")
        with tempfile.TemporaryDirectory(prefix="pgbr-vsseeker-scene-") as tmp:
            program = Path(tmp) / ("scene-test.exe" if os.name == "nt" else "scene-test")
            subprocess.run(
                [cxx, "-std=c++17", "-Wall", "-Wextra", "-I", str(NATIVE),
                 str(NATIVE / "pvp_scene_test.cpp"), "-o", str(program)],
                check=True, timeout=30,
            )
            result = subprocess.run([str(program)], check=True, timeout=15,
                                    text=True, capture_output=True)
            self.assertIn("PgrPvpSceneTest passed", result.stdout)


if __name__ == "__main__":
    unittest.main()
