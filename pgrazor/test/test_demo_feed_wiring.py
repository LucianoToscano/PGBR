"""Static barriers for a clearly fictional, non-teleporting PGBR demo source."""
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]

class DemoFeedWiringTests(unittest.TestCase):
    def test_demo_only_source_and_no_movement(self):
        ui=(ROOT/"src/dev/pgrazor/LiveFeedDialog.java").read_text(encoding="utf-8")
        data=(ROOT/"src/dev/pgrazor/DemoLiveFeedData.java").read_text(encoding="utf-8")
        self.assertIn('Button demo = segButton("DEMO OFFLINE")',ui)
        self.assertIn("selectProvider(4)",ui)
        self.assertIn("DemoLiveFeedData.sample(lat, lon, System.currentTimeMillis(), raidSearchMode)",ui)
        self.assertIn("if (provider == 4) showDemoDetail(visible.get(pos));",ui)
        self.assertIn("if (closed || provider == 4) return;",ui)
        self.assertIn('if (provider == 4) {showDemoDetail(e);return;}',ui)
        self.assertIn("Teleporte, caminhada e cópia de coordenadas indisponíveis.",ui)
        self.assertIn("MODO DEMONSTRAÇÃO",ui)
        self.assertIn("DADOS FICTÍCIOS",data)
        self.assertIn("raidBossLive", (ROOT/"src/dev/pgrazor/LiveFeedData.java").read_text(encoding="utf-8"))

if __name__=="__main__":unittest.main()
