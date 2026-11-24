"""Prevent an Android installer versionCode downgrade when marking LT builds as QA."""

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
from rooted_installer import installer_version_code


class RootInstallerVersionTests(unittest.TestCase):
    def test_lt_qa_installer_upgrades_from_lt040(self):
        self.assertEqual(installer_version_code("v0.00.40-LT"), 100000040)
        self.assertEqual(installer_version_code("v0.00.41-LT-QA"), 100000041)
        self.assertGreater(installer_version_code("v0.00.41-LT-QA"),
                           installer_version_code("v0.00.40-LT"))

    def test_regular_lt_not_regressed(self):
        self.assertEqual(installer_version_code("v0.00.41-LT"), 100000041)
        self.assertEqual(installer_version_code("v0.00.39-LT"), 100000039)

    def test_non_lt_preview_retains_its_old_code(self):
        self.assertEqual(installer_version_code("v0.00.41-dev"), 41)

    def test_invalid_versions_are_rejected(self):
        for value in ("", "v0.00.41-LT+QA", "v0.-1.0-LT"):
            with self.subTest(version=value), self.assertRaises(ValueError):
                installer_version_code(value)


if __name__ == "__main__":
    unittest.main()
