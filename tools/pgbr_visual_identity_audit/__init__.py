"""PGBR Offline Visual Identity & Provenance Auditor for Item/Map Icons (P0 / LT050).

Strictly read-only offline auditor that separates PNG byte/structural integrity
from semantic visual identity and live runtime verification, generating an
auditable ID -> image -> source -> visual-identity-status receipt without
mutating assets or inventing unverified mappings.
"""

from .auditor import (
    MAX_ICON_BYTES,
    P0_CATEGORIES,
    VISUAL_IDENTITY_STATUSES,
    IconIdentityRecord,
    VisualIdentityReport,
    audit_visual_identity,
    classify_p0_category,
    detect_duplicate_image_hashes,
    format_receipt_text,
    verify_png_integrity_bytes,
)

__all__ = [
    "MAX_ICON_BYTES",
    "P0_CATEGORIES",
    "VISUAL_IDENTITY_STATUSES",
    "IconIdentityRecord",
    "VisualIdentityReport",
    "audit_visual_identity",
    "classify_p0_category",
    "detect_duplicate_image_hashes",
    "format_receipt_text",
    "verify_png_integrity_bytes",
]
