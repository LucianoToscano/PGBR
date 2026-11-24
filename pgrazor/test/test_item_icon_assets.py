"""Verify original item sprites are bundled for all three Android installation paths."""
from pathlib import Path
import importlib.util
import struct
import zlib
import unittest

ROOT=Path(__file__).resolve().parents[1]
EXPECTED={
    1,2,3,101,102,103,104,201,202,301,401,
    701,702,703,704,705,706,707,
    1201,1202,1203,1204,1301,1350,1351,
    1401,1402,1403,1404,1406,1408,
    1600,1601,1602,1606,1607,1609,
}
class ItemIconAssetsTest(unittest.TestCase):
    def assert_png_integrity(self,data,name):
        self.assertEqual(data[:8],b"\x89PNG\r\n\x1a\n",name)
        cursor=8
        seen_header=False
        seen_data=False
        seen_end=False
        while cursor<len(data):
            self.assertGreaterEqual(len(data)-cursor,12,name)
            length,kind=struct.unpack_from(">I4s",data,cursor)
            end=cursor+12+length
            self.assertLessEqual(end,len(data),name)
            payload=data[cursor+8:end-4]
            expected=struct.unpack_from(">I",data,end-4)[0]
            self.assertEqual(zlib.crc32(kind+payload)&0xffffffff,expected,name)
            if not seen_header:
                self.assertEqual(kind,b"IHDR",name)
                self.assertEqual(length,13,name)
                w,h=struct.unpack_from(">II",payload)
                self.assertTrue(1<=w<=8192 and 1<=h<=8192,name)
                seen_header=True
            elif kind==b"IHDR":
                self.fail("Duplicate IHDR: "+name)
            if kind==b"IDAT":
                seen_data=True
            if kind==b"IEND":
                self.assertEqual(length,0,name)
                self.assertEqual(end,len(data),name)
                seen_end=True
                break
            cursor=end
        self.assertTrue(seen_header and seen_data and seen_end,name)

    def test_verified_assets_and_packagers(self):
        spec=importlib.util.spec_from_file_location("pgbr_build_assets",ROOT/"build.py")
        module=importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        icons=module.item_icon_assets()
        self.assertEqual(set(icons),{"item-%d.png"%i for i in EXPECTED})
        for name,path in icons.items():
            self.assertGreater(path.stat().st_size,100)
            self.assert_png_integrity(path.read_bytes(),name)
        for package in ("build.py","nonroot.py","rooted.py"):
            self.assertIn("item_icon_assets()", (ROOT/package).read_text(encoding="utf-8"))
        badge=(ROOT/"src/dev/pgrazor/ItemBadge.java").read_text(encoding="utf-8")
        self.assertIn('"pgrazor/item-icons/item-"+id+".png"',badge)
        # No fallback may invent a Potion/Revive/Egg for an unrelated item.
        self.assertNotIn("itemId>=100&&itemId<200",badge)
        self.assertNotIn("itemId>=200&&itemId<300",badge)
        self.assertNotIn("itemId>=300&&itemId<400",badge)
        self.assertIn("itemId>=101&&itemId<=104",badge)
        self.assertIn("itemId==201||itemId==202",badge)
        self.assertIn("itemId==301",badge)
if __name__=="__main__":
    unittest.main()
