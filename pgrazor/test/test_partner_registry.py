"""Validate the non-secret PGBR partner/source inventory used by integration planning."""
import json
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[2]
PATH = ROOT / "docs" / "PGBR_PARCEIROS_REGISTRO_20261120.json"


class PartnerRegistryTests(unittest.TestCase):
    def test_schema_and_unique_partner_ids(self):
        manifest = json.loads(PATH.read_text(encoding="utf-8"))
        self.assertEqual(manifest["schema_version"], 1)
        partners = manifest["partners"]
        ids = [p["id"] for p in partners]
        self.assertEqual(len(ids), len(set(ids)), "Partner IDs must be unique")
        self.assertGreaterEqual(len(partners), 10)
        self.assertTrue(all(p["import_state"] == "inventory_only" for p in partners),
                        "A source audit is not proof that a vendor SDK was integrated")
        for p in partners:
            self.assertTrue(p["capabilities"], p["id"])
            self.assertIn("source_type", p)
            if p.get("zip_sha256"):
                self.assertRegex(p["zip_sha256"], r"^[0-9a-f]{64}$")

    def test_do_not_embed_secrets_or_private_documents(self):
        manifest = json.loads(PATH.read_text(encoding="utf-8"))
        text = PATH.read_text(encoding="utf-8").lower()
        # Forbidden fields may be named in the policy, but must not appear in
        # any actual partner record or credentials payload.
        for partner in manifest["partners"]:
            forbidden = {"token", "password", "secret", "key", "authbearer",
                         "deviceauthtoken", "private_drive_url"}
            self.assertFalse(set(k.lower() for k in partner).intersection(forbidden))
        for value in ("-----begin private key-----", "bearer eyj",
                      "drive.google.com/file/d/", "docs.google.com/document/d/"):
            self.assertNotIn(value, text)
        self.assertIn("license", manifest["evidence_policy"]["permission_evidence"].lower()
                      .replace("contratos", "license"))  # nonsecret traceability metadata

    def test_game_version_and_external_providers(self):
        manifest = json.loads(PATH.read_text(encoding="utf-8"))
        self.assertRegex(manifest["compatibility_target"]["pokemon_go"],
                         r"^\d+\.\d+\.\d+$")
        self.assertEqual(set(manifest["compatibility_target"]["flavors"]),
                         {"root", "nonroot"})
        self.assertEqual(len({x["id"] for x in manifest["external_provider_boundaries"]}),
                         len(manifest["external_provider_boundaries"]))


if __name__ == "__main__":
    unittest.main()
