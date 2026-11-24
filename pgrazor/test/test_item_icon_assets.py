"""Verify original item sprites are bundled for all three Android installation paths."""
from pathlib import Path
import importlib.util
import hashlib
import json
import re
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

    def test_drive_reference_inventory_and_gallery(self):
        data=json.loads((ROOT.parent/'docs/validation/LT046_ITEM_ICON_REFERENCE.json').read_text(encoding='utf-8'))
        self.assertEqual({row['id'] for row in data['items']},EXPECTED)
        self.assertEqual(data['counts']['total'],len(EXPECTED))
        self.assertEqual(data['follow_up']['named_balls_same_size'],3)
        self.assertEqual(data['follow_up']['potion_revive_named_different_size'],6)
        self.assertEqual(data['follow_up']['generic_egg_unresolved'],0)
        expected_counts={'byte_identical':30,'visual_match_distinct_bytes':7,'unresolved_generic_egg':0}
        for status,count in expected_counts.items():
            self.assertEqual(sum(r['comparison']['status']==status for r in data['items']),count)
        self.assertEqual(data['follow_up']['binary_audit']['assets_replaced'],0)
        badge=(ROOT/'src/dev/pgrazor/ItemBadge.java').read_text(encoding='utf-8')
        self.assertIn('itemId==101?0xffc449d9:itemId==102?0xffffa520:itemId==103?0xfff4a9c1:0xff268cde',badge)
        self.assertIn('if(itemId==202){',badge)
        self.assertIn('i<10;i++',badge)
        self.assertIn('fill(0xfff4c64f)',badge)
        by_id={row['id']:row for row in data['items']}
        self.assertEqual(by_id[301]['comparison']['reference_filename'],'lucky-egg.png')
        self.assertIn('drawLuckyEgg(c)',badge)
        self.assertTrue(all(by_id[i]['drive_named_reference']['same_size'] for i in (1,2,3)))
        self.assertTrue(all(not by_id[i]['drive_named_reference']['same_size'] for i in (101,102,103,104,201,202)))
        expansion=json.loads((ROOT.parent/"docs/validation/LT046_ITEM_ICON_EXPANSION.json").read_text(encoding="utf-8"))
        expanded={row["id"]:row for row in expansion["items"]}
        # A dictionary would silently collapse duplicate IDs. Reject ambiguity before
        # comparing binary receipts, and require the ID-exact inventory path.
        self.assertEqual(len(by_id),len(data["items"]))
        self.assertEqual(len(expanded),len(expansion["items"]))
        for row in data["items"]:
            self.assertIs(type(row["id"]),int)
            self.assertGreater(row["id"],0)
            self.assertEqual(row["filename"],f'item-{row["id"]}.png')
        for row in expansion["items"]:
            self.assertIs(type(row["id"]),int)
            self.assertGreater(row["id"],0)
            self.assertEqual(row["path"],f'pgrazor/assets/item-icons/item-{row["id"]}.png')
        receipts=expansion["items"]+expansion["named"]
        self.assertEqual(len({row["path"] for row in receipts}),len(receipts))
        for row in expansion["named"]:
            self.assertRegex(row["path"],r'^pgrazor/assets/map-icons/[a-z0-9-]+\.png
            expected=expanded.get(row["id"],row)
            payload=(ROOT/"assets/item-icons"/row["filename"]).read_bytes()
            self.assertEqual(len(payload),expected["bytes"])
            self.assertEqual(hashlib.sha1(b"blob "+str(len(payload)).encode()+b"\0"+payload).hexdigest(),expected["git_blob_sha1"])
        for row in expansion["items"]+expansion["named"]:
            payload=(ROOT.parent/row["path"]).read_bytes()
            self.assertEqual(len(payload),row["bytes"])
            self.assertEqual(hashlib.sha1(b"blob "+str(len(payload)).encode()+b"\0"+payload).hexdigest(),row["git_blob_sha1"])
            self.assert_png_integrity(payload,row["path"])
        self.assertEqual(expanded[401]["source"],"incense.png")
        self.assertEqual(expanded[406]["source"],"daily-adventure-incense.png")
        self.assertIn("stardust.png",{row["source"] for row in expansion["named"]})
        source=(ROOT/'src/dev/pgrazor/LocalAuditDialog.java').read_text(encoding='utf-8')
        ids=re.search(r'GALLERY_IDS=\{([0-9,]+)\}',source).group(1)
        self.assertEqual(set(map(int,ids.split(','))),EXPECTED|set(expanded))
        self.assertIn('badge.hasDecodedSprite()',source)
        self.assertIn('int pageSize=9',source)
        theme=(ROOT/'src/dev/pgrazor/MapTheme.java').read_text(encoding='utf-8')
        self.assertIn('dark() ? 0xff202b39 : 0xffffffff',theme)
        self.assertIn('dark()?0x33ffffff:0x22000000',theme)

    def test_visual_action_consistency_and_icon_status(self):
        theme=(ROOT/'src/dev/pgrazor/FeedTheme.java').read_text(encoding='utf-8')
        primary=theme.split('static Drawable dialogPrimaryPill(Context c)',1)[1].split('private static ColorStateList dialogActionText()',1)[0]
        self.assertIn('shape(c, dlgCard(), 0, 100)',primary)
        self.assertNotIn('GradientDrawable.Orientation',primary)
        self.assertEqual(theme.count('b.setBackground(dialogPrimaryPill(c)); b.setTextColor(dialogActionText())'),2)
        self.assertIn('b.setBackgroundTintList(null)',theme)
        map_theme=(ROOT/'src/dev/pgrazor/MapTheme.java').read_text(encoding='utf-8')
        self.assertIn('view.setBackgroundTintList(null)',map_theme)
        badge=(ROOT/'src/dev/pgrazor/ItemBadge.java').read_text(encoding='utf-8')
        self.assertIn('Boolean.TRUE.equals(MISSED.get(itemId))',badge)
        self.assertIn('Boolean.TRUE.equals(WARNED.get(itemId))',badge)
        self.assertIn('paint.measureText(label)',badge)
        self.assertIn('metrics.ascent+metrics.descent',badge)
        self.assertIn('badge.spriteStatus()', (ROOT/'src/dev/pgrazor/LocalAuditDialog.java').read_text(encoding='utf-8'))

    def test_verified_assets_and_packagers(self):
        spec=importlib.util.spec_from_file_location("pgbr_build_assets",ROOT/"build.py")
        module=importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        icons=module.item_icon_assets()
        expansion=json.loads((ROOT.parent/"docs/validation/LT046_ITEM_ICON_EXPANSION.json").read_text(encoding="utf-8"))
        self.assertEqual(set(icons),{"item-%d.png"%i for i in EXPECTED|{r["id"] for r in expansion["items"]}})
        self.assertIn("stardust.png",module.map_icon_assets())
        self.assertIn("daily-adventure-incense.png",module.map_icon_assets())
        for package in ("build.py","nonroot.py","rooted.py"):
            self.assertIn("map_icon_assets()", (ROOT/package).read_text(encoding="utf-8"))
        feed=(ROOT/"src/dev/pgrazor/PogoFeedIcons.java").read_text(encoding="utf-8")
        self.assertIn('"stardust".equals(e.questRewardKind)',feed)
        self.assertIn('"pgrazor/item-icons/item-"+e.questItemId+".png"',feed)
        for name in ("AutomationPanel.java","SettingsPanel.java"):
            self.assertIn("ItemBadge.decorate(",(ROOT/"src/dev/pgrazor"/name).read_text(encoding="utf-8"))
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
)
        for row in data["items"]+receipts:
            self.assertIs(type(row["bytes"]),int)
            self.assertGreater(row["bytes"],100)
            self.assertRegex(row["git_blob_sha1"],r'^[0-9a-f]{40}
            expected=expanded.get(row["id"],row)
            payload=(ROOT/"assets/item-icons"/row["filename"]).read_bytes()
            self.assertEqual(len(payload),expected["bytes"])
            self.assertEqual(hashlib.sha1(b"blob "+str(len(payload)).encode()+b"\0"+payload).hexdigest(),expected["git_blob_sha1"])
        for row in expansion["items"]+expansion["named"]:
            payload=(ROOT.parent/row["path"]).read_bytes()
            self.assertEqual(len(payload),row["bytes"])
            self.assertEqual(hashlib.sha1(b"blob "+str(len(payload)).encode()+b"\0"+payload).hexdigest(),row["git_blob_sha1"])
            self.assert_png_integrity(payload,row["path"])
        self.assertEqual(expanded[401]["source"],"incense.png")
        self.assertEqual(expanded[406]["source"],"daily-adventure-incense.png")
        self.assertIn("stardust.png",{row["source"] for row in expansion["named"]})
        source=(ROOT/'src/dev/pgrazor/LocalAuditDialog.java').read_text(encoding='utf-8')
        ids=re.search(r'GALLERY_IDS=\{([0-9,]+)\}',source).group(1)
        self.assertEqual(set(map(int,ids.split(','))),EXPECTED|set(expanded))
        self.assertIn('badge.hasDecodedSprite()',source)
        self.assertIn('int pageSize=9',source)
        theme=(ROOT/'src/dev/pgrazor/MapTheme.java').read_text(encoding='utf-8')
        self.assertIn('dark() ? 0xff202b39 : 0xffffffff',theme)
        self.assertIn('dark()?0x33ffffff:0x22000000',theme)

    def test_visual_action_consistency_and_icon_status(self):
        theme=(ROOT/'src/dev/pgrazor/FeedTheme.java').read_text(encoding='utf-8')
        primary=theme.split('static Drawable dialogPrimaryPill(Context c)',1)[1].split('private static ColorStateList dialogActionText()',1)[0]
        self.assertIn('shape(c, dlgCard(), 0, 100)',primary)
        self.assertNotIn('GradientDrawable.Orientation',primary)
        self.assertEqual(theme.count('b.setBackground(dialogPrimaryPill(c)); b.setTextColor(dialogActionText())'),2)
        self.assertIn('b.setBackgroundTintList(null)',theme)
        map_theme=(ROOT/'src/dev/pgrazor/MapTheme.java').read_text(encoding='utf-8')
        self.assertIn('view.setBackgroundTintList(null)',map_theme)
        badge=(ROOT/'src/dev/pgrazor/ItemBadge.java').read_text(encoding='utf-8')
        self.assertIn('Boolean.TRUE.equals(MISSED.get(itemId))',badge)
        self.assertIn('Boolean.TRUE.equals(WARNED.get(itemId))',badge)
        self.assertIn('paint.measureText(label)',badge)
        self.assertIn('metrics.ascent+metrics.descent',badge)
        self.assertIn('badge.spriteStatus()', (ROOT/'src/dev/pgrazor/LocalAuditDialog.java').read_text(encoding='utf-8'))

    def test_verified_assets_and_packagers(self):
        spec=importlib.util.spec_from_file_location("pgbr_build_assets",ROOT/"build.py")
        module=importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        icons=module.item_icon_assets()
        expansion=json.loads((ROOT.parent/"docs/validation/LT046_ITEM_ICON_EXPANSION.json").read_text(encoding="utf-8"))
        self.assertEqual(set(icons),{"item-%d.png"%i for i in EXPECTED|{r["id"] for r in expansion["items"]}})
        self.assertIn("stardust.png",module.map_icon_assets())
        self.assertIn("daily-adventure-incense.png",module.map_icon_assets())
        for package in ("build.py","nonroot.py","rooted.py"):
            self.assertIn("map_icon_assets()", (ROOT/package).read_text(encoding="utf-8"))
        feed=(ROOT/"src/dev/pgrazor/PogoFeedIcons.java").read_text(encoding="utf-8")
        self.assertIn('"stardust".equals(e.questRewardKind)',feed)
        self.assertIn('"pgrazor/item-icons/item-"+e.questItemId+".png"',feed)
        for name in ("AutomationPanel.java","SettingsPanel.java"):
            self.assertIn("ItemBadge.decorate(",(ROOT/"src/dev/pgrazor"/name).read_text(encoding="utf-8"))
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
)
        for row in data["items"]:
            expected=expanded.get(row["id"],row)
            payload=(ROOT/"assets/item-icons"/row["filename"]).read_bytes()
            self.assertEqual(len(payload),expected["bytes"])
            self.assertEqual(hashlib.sha1(b"blob "+str(len(payload)).encode()+b"\0"+payload).hexdigest(),expected["git_blob_sha1"])
        for row in expansion["items"]+expansion["named"]:
            payload=(ROOT.parent/row["path"]).read_bytes()
            self.assertEqual(len(payload),row["bytes"])
            self.assertEqual(hashlib.sha1(b"blob "+str(len(payload)).encode()+b"\0"+payload).hexdigest(),row["git_blob_sha1"])
            self.assert_png_integrity(payload,row["path"])
        self.assertEqual(expanded[401]["source"],"incense.png")
        self.assertEqual(expanded[406]["source"],"daily-adventure-incense.png")
        self.assertIn("stardust.png",{row["source"] for row in expansion["named"]})
        source=(ROOT/'src/dev/pgrazor/LocalAuditDialog.java').read_text(encoding='utf-8')
        ids=re.search(r'GALLERY_IDS=\{([0-9,]+)\}',source).group(1)
        self.assertEqual(set(map(int,ids.split(','))),EXPECTED|set(expanded))
        self.assertIn('badge.hasDecodedSprite()',source)
        self.assertIn('int pageSize=9',source)
        theme=(ROOT/'src/dev/pgrazor/MapTheme.java').read_text(encoding='utf-8')
        self.assertIn('dark() ? 0xff202b39 : 0xffffffff',theme)
        self.assertIn('dark()?0x33ffffff:0x22000000',theme)

    def test_visual_action_consistency_and_icon_status(self):
        theme=(ROOT/'src/dev/pgrazor/FeedTheme.java').read_text(encoding='utf-8')
        primary=theme.split('static Drawable dialogPrimaryPill(Context c)',1)[1].split('private static ColorStateList dialogActionText()',1)[0]
        self.assertIn('shape(c, dlgCard(), 0, 100)',primary)
        self.assertNotIn('GradientDrawable.Orientation',primary)
        self.assertEqual(theme.count('b.setBackground(dialogPrimaryPill(c)); b.setTextColor(dialogActionText())'),2)
        self.assertIn('b.setBackgroundTintList(null)',theme)
        map_theme=(ROOT/'src/dev/pgrazor/MapTheme.java').read_text(encoding='utf-8')
        self.assertIn('view.setBackgroundTintList(null)',map_theme)
        badge=(ROOT/'src/dev/pgrazor/ItemBadge.java').read_text(encoding='utf-8')
        self.assertIn('Boolean.TRUE.equals(MISSED.get(itemId))',badge)
        self.assertIn('Boolean.TRUE.equals(WARNED.get(itemId))',badge)
        self.assertIn('paint.measureText(label)',badge)
        self.assertIn('metrics.ascent+metrics.descent',badge)
        self.assertIn('badge.spriteStatus()', (ROOT/'src/dev/pgrazor/LocalAuditDialog.java').read_text(encoding='utf-8'))

    def test_verified_assets_and_packagers(self):
        spec=importlib.util.spec_from_file_location("pgbr_build_assets",ROOT/"build.py")
        module=importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        icons=module.item_icon_assets()
        expansion=json.loads((ROOT.parent/"docs/validation/LT046_ITEM_ICON_EXPANSION.json").read_text(encoding="utf-8"))
        self.assertEqual(set(icons),{"item-%d.png"%i for i in EXPECTED|{r["id"] for r in expansion["items"]}})
        self.assertIn("stardust.png",module.map_icon_assets())
        self.assertIn("daily-adventure-incense.png",module.map_icon_assets())
        for package in ("build.py","nonroot.py","rooted.py"):
            self.assertIn("map_icon_assets()", (ROOT/package).read_text(encoding="utf-8"))
        feed=(ROOT/"src/dev/pgrazor/PogoFeedIcons.java").read_text(encoding="utf-8")
        self.assertIn('"stardust".equals(e.questRewardKind)',feed)
        self.assertIn('"pgrazor/item-icons/item-"+e.questItemId+".png"',feed)
        for name in ("AutomationPanel.java","SettingsPanel.java"):
            self.assertIn("ItemBadge.decorate(",(ROOT/"src/dev/pgrazor"/name).read_text(encoding="utf-8"))
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
