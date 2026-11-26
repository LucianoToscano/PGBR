"""Offline inventory & provenance auditor for embedded text/JSON datasets (G-049-08 / DATA-01 / DATA-02).

Strictly read-only offline auditor: never mutates repository files, never
touches images (#340), Android runtime/C++/Java, V5, network, or credentials.
"""
from . import dataset_auditor

__all__ = ["dataset_auditor"]
