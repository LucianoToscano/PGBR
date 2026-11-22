"""Protect the visible PGBR branding without altering legacy runtime identifiers."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]

class PGBRBrandingTests(unittest.TestCase):
    def test_main_readme(self):
        readme = (ROOT / "README.md").read_text(encoding="utf-8")
        self.assertIn("<h1 align=\"center\">PGBR</h1>", readme)
        version = (ROOT / "pgrazor/VERSION").read_text(encoding="utf-8").strip().removeprefix("v")
        self.assertIn(f"PGBR-v{version}", readme)
        self.assertIn(f"PGBR-v{version}.apk", readme)
        self.assertNotIn('href="https://jpitty03.github.io/PGRazor/"', readme)
        self.assertIn("https://github.com/jpitty03/PGRazor", readme)  # Credits are required.
        self.assertNotIn("website/public/images/main-menu.png", readme)  # Old screenshot.

    def test_branding_files(self):
        for path in (ROOT / "pgrazor/branding/pgrazor.svg",
                     ROOT / "website/public/logo.svg"):
            data = path.read_text(encoding="utf-8")
            self.assertIn("<title id=\"name\">PGBR</title>", data)
            self.assertNotIn("<title>PGRazor</title>", data)

    def test_site_base_and_download(self):
        vite = (ROOT / "website/vite.config.js").read_text(encoding="utf-8")
        content = (ROOT / "website/src/content.js").read_text(encoding="utf-8")
        self.assertIn("base: '/PGBR/'", vite)
        self.assertIn("LucianoToscano/PGBR/actions", content)
        self.assertNotIn("jpitty03/PGRazor/releases/latest", content)

    def test_release_names_and_runtime_compatibility(self):
        release = (ROOT / "pgrazor/release_apk.py").read_text(encoding="utf-8")
        self.assertIn("PGBR-{build.release_version(build.ROOT)}.apk", release)
        rooted = (ROOT / "pgrazor/rooted.py").read_text(encoding="utf-8")
        self.assertIn("MODULE_ID = 'pgrazor'", rooted)  # Legacy Zygisk state is intentional.
        self.assertIn("name=PGBR Root", rooted)
        self.assertIn("dev.pgrazor", (ROOT / "pgrazor/AndroidManifest.xml").read_text(encoding="utf-8"))

if __name__ == "__main__":
    unittest.main()
