"""Offline tests for the narrow, privacy-preserving Moonani endpoint probe."""
import unittest
from tools.pokelist_live_probe import parse_epoch, summarize

class ProbeTests(unittest.TestCase):
    def test_epoch(self):
        self.assertEqual(1_800_000_000_000, parse_epoch(1800000000))
        self.assertEqual(1_800_000_000_000, parse_epoch(1800000000000))
        self.assertIsNone(parse_epoch("garbage"))
        self.assertIsNone(parse_epoch(7))
    def test_spawns_and_no_raw_coords(self):
        data={"type":"pokemon","pokemons":[
          {"id":"secret","number":25,"latitude":-23.55,"longitude":-46.63,
           "endTime":1800000120},
          {"number":25,"latitude":99,"longitude":-46.63},
          {"number":25,"latitude":0,"longitude":0},
          {"number":150,"latitude":-23.55,"longitude":-46.63,
           "endTime":1799999999},
          {"number":150,"latitude":-23.55,"longitude":-46.63},
        ]}
        stats=summarize(data,1800000000000)
        self.assertTrue(stats["schema_valid"])
        self.assertEqual(stats["valid_coords"],3)
        self.assertEqual(stats["unexpired"],1)
        self.assertEqual(stats["expiry_known"],2)
        self.assertEqual(stats["unknown_expiry"],1)
        self.assertEqual(stats["dex25"],1)
        self.assertNotIn("latitude",stats)
        self.assertNotIn("id",stats)
    def test_bad_contract(self):
        self.assertFalse(summarize({"type":"pokemon","items":[]},1800000000000)["schema_valid"])
        self.assertFalse(summarize({"type":"raid","pokemons":[]},1800000000000)["schema_valid"])
        self.assertFalse(summarize([],1800000000000)["schema_valid"])

if __name__=="__main__":
    unittest.main()
