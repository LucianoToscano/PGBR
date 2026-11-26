"""Pure-stdlib binary image prober with decode bounds, CRC32 & format verification (G-049-06).

Covers:
- (2) Dimensions (PNG, WebP, GIF, JPEG)
- (3) Transparency / alpha detection
- (4) Real binary format vs file extension check
- (5) Truncated / corrupt file detection (PNG chunk framing + CRC32 + IEND, JPEG EOI, WebP RIFF)
- (9) Maximum file size, dimension, pixel-count (decompression bomb) & chunk limits
"""
from __future__ import annotations

from dataclasses import dataclass
import hashlib
from pathlib import Path
import struct
from typing import Any, Dict, List, Optional
import zlib

PNG_SIG = b"\x89PNG\r\n\x1a\n"
JPEG_SIG = b"\xff\xd8\xff"
GIF_SIGS = (b"GIF87a", b"GIF89a")

EXT_TO_FORMAT = {
    ".png": "png",
    ".webp": "webp",
    ".jpg": "jpeg",
    ".jpeg": "jpeg",
    ".gif": "gif",
    ".svg": "svg",
}


@dataclass(frozen=True)
class DecodeLimits:
    max_file_bytes: int = 2 * 1024 * 1024  # 2 MiB
    max_dimension: int = 4096
    max_pixels: int = 4096 * 4096
    max_chunks: int = 2048


def detect_magic_format(data: bytes) -> str:
    if data.startswith(PNG_SIG):
        return "png"
    if len(data) >= 12 and data[:4] == b"RIFF" and data[8:12] == b"WEBP":
        return "webp"
    if data.startswith(JPEG_SIG):
        return "jpeg"
    if len(data) >= 6 and data[:6] in GIF_SIGS:
        return "gif"
    head = data[:256].lstrip()
    if head.startswith(b"<svg") or (head.startswith(b"<?xml") and b"<svg" in data[:1024]):
        return "svg"
    return "unknown"


def _probe_png(data: bytes, limits: DecodeLimits) -> Dict[str, Any]:
    errors: List[str] = []
    width = height = 0
    color_type: Optional[int] = None
    has_alpha = False
    saw_ihdr = False
    saw_iend = False
    offset = 8
    chunks = 0

    while offset < len(data):
        chunks += 1
        if chunks > limits.max_chunks:
            errors.append("png_chunk_limit_exceeded")
            break
        if offset + 12 > len(data):
            errors.append("png_truncated_chunk_header")
            break
        length = struct.unpack(">I", data[offset : offset + 4])[0]
        ctype = data[offset + 4 : offset + 8]
        chunk_data_start = offset + 8
        chunk_data_end = chunk_data_start + length
        crc_end = chunk_data_end + 4
        if crc_end > len(data):
            errors.append("png_truncated_chunk_payload")
            break
        chunk_payload = data[chunk_data_start:chunk_data_end]
        stored_crc = struct.unpack(">I", data[chunk_data_end:crc_end])[0]
        calc_crc = zlib.crc32(ctype + chunk_payload) & 0xFFFFFFFF
        if stored_crc != calc_crc:
            errors.append(f"png_bad_crc:{ctype.decode('ascii', errors='replace')}")
            break
        if chunks == 1:
            if ctype != b"IHDR" or length != 13:
                errors.append("png_missing_or_invalid_ihdr")
                break
            saw_ihdr = True
            width, height, _bit_depth, color_type = struct.unpack(">IIBB", chunk_payload[:10])
            if color_type in (4, 6):
                has_alpha = True
        elif ctype == b"tRNS":
            has_alpha = True
        elif ctype == b"IEND":
            saw_iend = True
            offset = crc_end
            break
        offset = crc_end

    if not saw_ihdr:
        errors.append("png_no_ihdr")
    if not saw_iend:
        errors.append("png_truncated_no_iend")
    return {
        "width": width,
        "height": height,
        "has_alpha": has_alpha,
        "color_type": color_type,
        "errors": errors,
    }


def _probe_webp(data: bytes) -> Dict[str, Any]:
    errors: List[str] = []
    width = height = 0
    has_alpha = False
    if len(data) < 20:
        return {"width": 0, "height": 0, "has_alpha": False, "errors": ["webp_truncated_header"]}
    riff_size = struct.unpack("<I", data[4:8])[0]
    if riff_size + 8 > len(data):
        errors.append("webp_truncated_riff")
    chunk_fourcc = data[12:16]
    if chunk_fourcc == b"VP8X" and len(data) >= 30:
        flags = data[20]
        has_alpha = bool(flags & 0x10)
        width = 1 + int.from_bytes(data[24:27], "little")
        height = 1 + int.from_bytes(data[27:30], "little")
    elif chunk_fourcc == b"VP8L" and len(data) >= 25 and data[20] == 0x2F:
        bits = struct.unpack("<I", data[21:25])[0]
        width = (bits & 0x3FFF) + 1
        height = ((bits >> 14) & 0x3FFF) + 1
        has_alpha = bool((bits >> 28) & 1)
    elif chunk_fourcc == b"VP8 " and len(data) >= 30:
        width = struct.unpack("<H", data[26:28])[0] & 0x3FFF
        height = struct.unpack("<H", data[28:30])[0] & 0x3FFF
    else:
        errors.append("webp_unsupported_or_truncated_chunk")
    return {"width": width, "height": height, "has_alpha": has_alpha, "errors": errors}


def _probe_gif(data: bytes) -> Dict[str, Any]:
    if len(data) < 13:
        return {"width": 0, "height": 0, "has_alpha": False, "errors": ["gif_truncated"]}
    width, height = struct.unpack("<HH", data[6:10])
    # GCE block 0x21 0xF9 with transparent color flag bit 0
    has_alpha = b"\x21\xf9\x04" in data
    errors = [] if data.endswith(b"\x3b") else ["gif_missing_trailer"]
    return {"width": width, "height": height, "has_alpha": has_alpha, "errors": errors}


def _probe_jpeg(data: bytes) -> Dict[str, Any]:
    errors: List[str] = []
    width = height = 0
    if not data.endswith(b"\xff\xd9"):
        errors.append("jpeg_truncated_no_eoi")
    i = 2
    while i + 4 < len(data):
        if data[i] != 0xFF:
            break
        marker = data[i + 1]
        if marker in (0xD8, 0xD9, 0x01) or (0xD0 <= marker <= 0xD7):
            i += 2
            continue
        seg_len = struct.unpack(">H", data[i + 2 : i + 4])[0]
        if seg_len < 2 or i + 2 + seg_len > len(data):
            errors.append("jpeg_truncated_segment")
            break
        if marker in (0xC0, 0xC1, 0xC2) and seg_len >= 7:
            height, width = struct.unpack(">HH", data[i + 5 : i + 9])
            break
        i += 2 + seg_len
    return {"width": width, "height": height, "has_alpha": False, "errors": errors}


def probe_image_file(path: Path, limits: DecodeLimits = DecodeLimits()) -> Dict[str, Any]:
    """Read-only probe of a single image file with strict bounds and integrity checks."""
    size = path.stat().st_size
    ext = path.suffix.lower()
    expected_fmt = EXT_TO_FORMAT.get(ext, "other")
    if size == 0:
        return {
            "bytes": 0,
            "sha256": hashlib.sha256(b"").hexdigest(),
            "extension": ext,
            "expected_format": expected_fmt,
            "detected_format": "empty",
            "extension_mismatch": True,
            "width": 0,
            "height": 0,
            "has_alpha": False,
            "valid": False,
            "errors": ["empty_file"],
        }
    if size > limits.max_file_bytes:
        h = hashlib.sha256()
        with path.open("rb") as fh:
            for chunk in iter(lambda: fh.read(65536), b""):
                h.update(chunk)
        return {
            "bytes": size,
            "sha256": h.hexdigest(),
            "extension": ext,
            "expected_format": expected_fmt,
            "detected_format": "oversize_skipped",
            "extension_mismatch": False,
            "width": 0,
            "height": 0,
            "has_alpha": False,
            "valid": False,
            "errors": [f"file_exceeds_max_bytes:{size}>{limits.max_file_bytes}"],
        }

    data = path.read_bytes()
    digest = hashlib.sha256(data).hexdigest()
    detected = detect_magic_format(data)
    errors: List[str] = []
    width = height = 0
    has_alpha = False

    if detected == "png":
        info = _probe_png(data, limits)
        width, height, has_alpha, errors = info["width"], info["height"], info["has_alpha"], list(info["errors"])
    elif detected == "webp":
        info = _probe_webp(data)
        width, height, has_alpha, errors = info["width"], info["height"], info["has_alpha"], list(info["errors"])
    elif detected == "gif":
        info = _probe_gif(data)
        width, height, has_alpha, errors = info["width"], info["height"], info["has_alpha"], list(info["errors"])
    elif detected == "jpeg":
        info = _probe_jpeg(data)
        width, height, has_alpha, errors = info["width"], info["height"], info["has_alpha"], list(info["errors"])
    elif detected == "svg":
        has_alpha = True
    else:
        errors.append("unrecognized_image_format")

    ext_mismatch = expected_fmt != "other" and detected != expected_fmt
    if ext_mismatch:
        errors.append(f"extension_mismatch:{ext}!={detected}")

    if detected in ("png", "webp", "gif", "jpeg"):
        if width <= 0 or height <= 0:
            errors.append("invalid_zero_dimensions")
        if width > limits.max_dimension or height > limits.max_dimension:
            errors.append(f"dimension_limit_exceeded:{width}x{height}>{limits.max_dimension}")
        if width * height > limits.max_pixels:
            errors.append(f"pixel_limit_exceeded:{width * height}>{limits.max_pixels}")

    return {
        "bytes": size,
        "sha256": digest,
        "extension": ext,
        "expected_format": expected_fmt,
        "detected_format": detected,
        "extension_mismatch": ext_mismatch,
        "width": width,
        "height": height,
        "has_alpha": has_alpha,
        "valid": len(errors) == 0,
        "errors": errors,
    }
