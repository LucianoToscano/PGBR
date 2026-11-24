"""Check RocketMap optional source wiring and guard rails."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]

class RocketMapWiringTests(unittest.TestCase):
    def test_rocketmap_opt_in(self):
        ui=(ROOT/"src/dev/pgrazor/LiveFeedDialog.java").read_text(encoding="utf-8")
        api=(ROOT/"src/dev/pgrazor/RocketMapFeedClient.java").read_text(encoding="utf-8")
        data=(ROOT/"src/dev/pgrazor/RocketMapFeedData.java").read_text(encoding="utf-8")
        self.assertIn('rocketmap_https_url_v1',ui)
        self.assertIn('RocketMapFeedClient.validateBaseUrl(',ui)
        self.assertIn('rocketMap.fetch(configuredRocket, lat, lon, radiusKm, raidSearchMode,',ui)
        self.assertIn('secretAtRequest', ui)
        self.assertIn('X-Feed-Secret', api)
        self.assertIn('rocketReadSecret = "";', ui)
        self.assertNotIn('putString("rocketmap_read_secret', ui)
        self.assertIn('rocketMap.cancel()',ui)
        self.assertIn('rocketMapAccessDenied',ui)
        self.assertIn('provider == 3',ui)
        self.assertIn('"RocketMap (meu servidor)"',ui)
        self.assertIn('"/raw_data"',api)
        self.assertIn('getRawUserInfo()',api)
        self.assertIn('setInstanceFollowRedirects(false)',api)
        self.assertIn('conn.getResponseCode()',api)
        self.assertIn('RocketMapFeedData.parse(',api)
        self.assertIn('disappear_time',data)
        self.assertIn('gyms',data)
        self.assertIn('raid',data)
        self.assertIn('Fonte: servidor RocketMap',data)
        self.assertIn('Seu servidor RocketMap recusou acesso',ui)

if __name__=="__main__":
    unittest.main()
