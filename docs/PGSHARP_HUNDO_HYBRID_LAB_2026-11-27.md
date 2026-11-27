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


## Follow-up: actual DEX dependency comparison (2026-11-27)

Re-inspected both preserved native libraries from the original files, verified SHA-256, recovered both embedded DEX archives in memory and verified ZIP CRC. The 1.278.0 library SHA-256 is 912b8a2d064b82eed1fe68c704a7dee2e89394846bc16dcb3e71c46a292d60e3; the 1.279.0 baseline SHA-256 is recorded above. The prior v5 forensic dossier establishes the reference coordinate-result routine and its companion filter submission routine. This follow-up compares their static call patterns, not private request payloads.

| Static observation | 1.278.0 | 1.279.0 |
|---|---:|---:|
| Result-reader direct invoke instructions targeting JSON | 6 | 6 |
| Result-reader direct invoke instructions targeting Mapbox | 3 | 3 |
| Result-reader direct invoke instructions targeting plugin-local classes | 23 | 23 |
| Result-reader field references | 14 | 14 |
| Result-reader direct host-bridge calls | 2 | 2 |
| Result-reader static path length to a declared native method | 2 calls | 2 calls |
| Companion request routine direct invoke instructions | 14 | 14 |
| Companion request routine field references | 7 | 7 |
| Companion request routine direct host-bridge calls | 2 | 2 |
| Companion request routine shortest static path to declared native code | 3 calls | 3 calls |

The matching methods are non-static and depend on internal objects and host state. Both versions exhibit the same JNI dependency for the identified result reader. The companion request routine also has a static dependency on native code. The new DEX contains 5,068 class definitions, 28,788 defined methods, of which 801 carry the native flag; the local instruction scanner parsed 26,549 code-bearing methods without a parsing exception. Seven candidate routines matched the conservative JSON-coordinate/array/Mapbox pattern; the comparison supports continuity of the identified reader but is not an executed call trace, nor evidence that every path always enters native code.

**Decision:** do not insert the identified method or the loader into the PGBR process based only on static similarity. A stand-alone data retrieval module was not established. A pure result reader alone cannot obtain the upstream response. Require an independently usable, approved component or documented export before binding the existing ReadOnlyModule boundary. No third-party binary, private wire protocol, license credential, integrity bypass, host hook or new network path was added. No Android ARM64 runtime/ADB target is available in the local analysis environment, so no native execution or in-game test was possible.

### Verified QA of the existing laboratory

HEAD efa160ea9476a27c2d6c97c45f6e176b48f05ea1, draft PR #366:
- PGSharp policy and full Android Java compilation: https://github.com/LucianoToscano/PGBR/actions/runs/46047614154 — success.
- Master and inventory governance: https://github.com/LucianoToscano/PGBR/actions/runs/46047614146 — success.
- Full device-free source suite, Java/Root compilation, ARM64 NDK checks, native host and website: https://github.com/LucianoToscano/PGBR/actions/runs/46047614143 — success (5 jobs).
- The actual standalone bridge JVM suite was also rerun locally: 1 test passed.

These results apply to the cited code HEAD, not to a connected PGSharp provider or an assembled hybrid APK. This follow-up changes documentation only. Any new checks of the documentation revision are separate.
