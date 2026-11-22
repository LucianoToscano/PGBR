"""Restore the original OFF fallback PNG from row zero of the HD VGP WebP atlas."""
from pathlib import Path
from io import BytesIO
import argparse

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "pgrazor/assets/map-icons/vgp-hd-states.webp"
DEST = ROOT / "pgrazor/assets/map-icons/vgp-device-off.png"

def repair(check=False):
    from PIL import Image
    with Image.open(SOURCE) as src:
        src.load()
        if src.width <= 0 or src.height != src.width * 3:
            raise ValueError("Expected exactly three square VGP states")
        expected = src.crop((0, 0, src.width, src.width)).convert("RGBA")
    if check:
        with Image.open(DEST) as dst:
            dst.load()
            actual = dst.convert("RGBA")
            if actual.size != expected.size or actual.tobytes() != expected.tobytes():
                raise AssertionError("OFF PNG differs from original atlas row 0")
        print("PASS VGP fallback pixel-for-pixel match to original atlas")
        return
    out = BytesIO()
    expected.save(out, "PNG", optimize=False)
    data = out.getvalue()
    if not DEST.exists() or DEST.read_bytes() != data:
        DEST.write_bytes(data)
    repair(check=True)

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    repair(args.check)
