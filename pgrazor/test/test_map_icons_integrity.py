"""Full CRC and zlib integrity of all PGBR map PNGs. No Pillow dependency."""
from pathlib import Path
import struct
import unittest
import zlib

ASSETS = Path(__file__).resolve().parents[1] / "assets/map-icons"

def verify(path):
    data = path.read_bytes()
    if not data.startswith(bytes.fromhex("89504e470d0a1a0a")):
        raise ValueError("Invalid PNG signature")
    pos, idats, ihdr, done = 8, [], False, False
    while pos < len(data):
        if pos + 12 > len(data):
            raise ValueError("PNG truncated chunk header")
        n = struct.unpack_from(">I", data, pos)[0]
        tag = data[pos + 4:pos + 8]
        end = pos + 12 + n
        if end > len(data):
            raise ValueError("PNG truncated chunk body")
        payload = data[pos + 8:pos + 8 + n]
        stored = struct.unpack_from(">I", data, pos + 8 + n)[0]
        if zlib.crc32(tag + payload) & 0xffffffff != stored:
            raise ValueError("PNG wrong CRC")
        if tag == b"IHDR":
            if ihdr or pos != 8 or n != 13:
                raise ValueError("PNG malformed IHDR")
            ihdr = True
        if tag == b"IDAT":
            idats.append(payload)
        if tag == b"IEND":
            if n or end != len(data):
                raise ValueError("PNG invalid IEND")
            done = True
            break
        pos = end
    if not (ihdr and idats and done):
        raise ValueError("PNG incomplete")
    zlib.decompress(b"".join(idats))

class MapIconIntegrityTests(unittest.TestCase):
    def test_map_pngs_are_complete(self):
        files = list(ASSETS.glob("*.png"))
        self.assertGreaterEqual(len(files), 30)
        for path in files:
            with self.subTest(path=path.name):
                verify(path)

    def test_vgp_off_exists(self):
        target = ASSETS / "vgp-device-off.png"
        self.assertTrue(target.is_file())
        verify(target)
