# PGBR — experimental PGSharp 100% IV module boundary

Status: research branch only. No third-party native library is included. No verified PGSharp transport or device execution. The laboratory is a read-only UI and adapter for the PGS-owned authorized export contract, not an integrated PGSharp feed.

## Compatibility evidence (2026-11-27)

Inspected preserved PGSharp 1.279.0 / Pokemon GO 0.439.0 ARM64 APK, 211162490 bytes, SHA-256 77650a0783117fafed993f327f220ae5eb784efbf18d033c7d809262ed244f34. ZIP CRC check passed; four outer DEX files and 29 ARM64 libraries. No execution.

- libmain.so: 1248368 bytes; SONAME libapp_debug_loader.so; exports JNI_OnLoad. DT_NEEDED: liblog, libm, libdl, libc.
- libhela_baseline.so: 39494560 bytes; SONAME libhela.so; exports JNI_OnLoad_IMP. DT_NEEDED: libEGL, libGLESv2, libjnigraphics, libandroid, liblog, libm, libdl, libc.
- libunity.so has DT_NEEDED libmain.so. Replacing or loading arbitrary versions of these libraries risks the game and PGBR runtime. No independently callable Hundo feed export was identified by name in the dynamic symbols examined. This does not prove there is no internal feed code.
- PGBR source already has PgSharpFeedDialog, PgSharpFeedPolicy, LocalImportDialog and an unverified provider status. Preserve the normal build and its native dependencies; do not add private binaries, override the original game input or assert provider connection. The static study of the embedded plugin from the earlier forensic dossier is not a provider interface or proof of compatibility.

## Scope of the lab

PgSharpHundoBridge.ReadOnlyModule is a typed boundary for a future approved SDK or component. Its local preview validates the same PGS-owned envelope as the offline adapter: schema pgs-authorized-hundo-export-v1; provider pgsharp; coordinate_precision exact; fetched_ms; items with wild category, explicit exact precision, 15/15/15, dex, lat, lon, expires_ms. Optional iv_percent must equal 100, form defaults to zero, encounter_id is an optional nonzero uint64 decimal string. No ID is fabricated. Empty/stale, contradictory and invalid data fail closed; duplicate IDs with conflicting coordinates are removed. This is not a PGSharp wire protocol and claimed provenance is not authenticated.

The separate lab button in the existing PGSharp menu opens a bounded preview (up to 50 rows). No persistence, no automatic actions, no server, no license key. The regular import and existing PGBR sources remain unchanged. A synthetic JVM test exercises the actual core including the module interface, validation, duplicates, conflicts and invalid/absent inputs. UI compilation and physical Android validation are still pending.

Next acceptance gates: identify a provider-approved independently usable feed component; validate its exact ABI and dependencies against the PGBR build; obtain real permitted data; connect through the typed boundary, keep the provider status truthful, and run device tests. Do not bypass access controls or merge native binaries blindly. No release or APK is claimed.
