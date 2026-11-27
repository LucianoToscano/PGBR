# Private PGSharp/PGBR coexistence candidate — experimental only

## Scope and exclusions

Keep the intact PGSharp 1.279.0 runtime, four original DEX files, native libraries, resources and application identity. Add only a separately compiled PGBR-owned read-only panel and Hundo export validator. This is deliberately a PGSharp-base coexistence test, NOT the full PGBR gameplay engine, NOT a PGSharp feed client inside the PGBR sniper, and NOT a release. The original PGSharp menu is retained in the candidate package, but its runtime, login, license and feed operation are not verified after repackaging. Do not modify authentication, integrity checks or provider access controls. If the original rejects the new certificate, stop the test.

Use only authorized private local inputs. Never store or upload proprietary APK/DEX/native files, private signing keys, user data or credentials in the repository. No network operations in the builder. No existing APK is overwritten or installed. Use a separate Android test device/emulator, not the installed stable game; the experimental APK retains the same package name and uses a different test certificate, so it cannot update an existing installation signed with another certificate.

## Actual comparison — 2026-11-27

- PGSharp 1.279.0 / 0.439.0 SHA-256 77650a0783117fafed993f327f220ae5eb784efbf18d033c7d809262ed244f34.
- PGBR LT049 R2 QA private artifact https://github.com/LucianoToscano/PGBR/actions/runs/45771185121/artifacts/15094218270 — ZIP hash 44b8b0aca2305499624874aa0b9b93dd4d9d56b64e1fcb223c1cc05f93bad357; extracted APK SHA-256 a705f220589017fe2134c42b9fc3242e04311e8b365b41c8c4407a5a4ec2ad3b.
- 2164 PGSharp entries, 3421 PGBR entries, 1697 shared paths, 1285 matching size/CRC, 412 differing size/CRC. Matching size/CRC is not a cryptographic equality proof.
- SHA-256 verification of all 28 common game native libraries: 27 identical, only libmain.so differs. libil2cpp.so, libunity.so, and global-metadata.dat are byte-identical by SHA-256. Resource tables differ; preserve PGSharp's resources and all original DEX together.
- 24781 shared class definitions; 2067 PGBR-specific definitions in classes5.dex, all dev.pgrazor namespace; zero class-descriptor overlap with PGSharp outer DEX. This does not establish runtime compatibility.
- PGSharp source public certificate fingerprint extracted from existing signature container: e601d7e0383f0cc1e0f0a4243394e42cafba6e7cedf1af377f9052c3a00a46a4. Signature verification via official apksigner remains required. The PGBR QA certificate fingerprint 7be443fec5c9d15f7c8eadb47e1c21da4c4319341a6282f74285049ee53d96e7 is also excluded from the experimental signer.
- Exclude PGBR core/ART libraries for ARM64 and x86_64, regular startup and its session recovery, native hooks, DEX rewrites, mode flags, location and gameplay owners. The normal nonroot.py clean-Niantic gate remains unchanged. Nothing is removed from the stable PGBR repository or the original PGSharp APK.
- The independent recipe reuses the existing append-only manifest editor and PgSharpHundoBridge. Its alternate BootstrapProvider and LogActivity compile only in this experimental recipe, never together with the production classes of the same names. Payload allowed classes are only those three class families. Preserve every non-signature host entry except the append-only manifest and add classes5.dex. The builder verifies source and test output signatures, 16 KiB alignment, input/output entry hashes and CRC before emitting a new local candidate. Does not claim startup compatibility.

## Reproduction

Requires Python 3.12+, JDK, official Android SDK platform 36 and build-tools 36.0.0. Audit private inputs locally:

    python tools/pgsharp_hybrid/hybrid_audit.py PGSHARP.apk PGBR-LT049-R2.apk

Compile own payload without any third-party input:

    python tools/pgsharp_hybrid/private_build.py --sdk SDK_PATH compile-payload --output local-payload.dex

For an explicitly authorized private coexistence experiment only, supply the exact preserved source APK, an existing dedicated test keystore (not PGSharp or PGBR stable signer), the alias, and PGBR_HYBRID_STORE_PASS / PGBR_HYBRID_KEY_PASS through local environment variables. Never commit them. Execute:

    python tools/pgsharp_hybrid/private_build.py --sdk SDK_PATH assemble-private --private-test --base PGSHARP.apk --output NEW_PRIVATE_QA.apk --keystore TEST_KEYSTORE --alias TEST_ALIAS

The separate build is not connected to production packaging or releases. The Android panel uses an ordinary activity lifecycle listener, a small user-initiated PGBR QA button and a private activity for validating explicit exports. It does not read PGSharp internals, transmit data or perform gameplay actions.

## Test record

Actual offline APK comparison completed on the pinned pair. Python syntax checks passed. Three local unit tests passed; Android payload compilation test skipped because Android SDK is absent. No signed hybrid APK assembled, no device execution, provider_connected=false. Existing PR CI results apply to earlier HEAD only. CI must verify the new sources; a physical private test is still needed before declaring coexistence or feed compatibility. Do not merge or publish this branch as a functional hybrid.

### Follow-up CI correction

At code HEAD c8a045f580438cd0a46f6876c949b306a20dd696, the governance and PGSharp policy workflows passed; the full suite ran 673 tests (671 passed, one error, one skipped). Run [46050475979](https://github.com/LucianoToscano/PGBR/actions/runs/46050475979), job 140680238063, failed during the real minimal-payload compilation with a javac LambdaMetafactory.metafactory resolution error. The experimental builder now uses the same javac classpath configuration as the normal PGBR build and passes a temporary JAR to D8. The existing real Android SDK compilation test is retained, not skipped or weakened. New CI is required; the correction has not yet been verified by Android compilation. No APK assembly or provider execution is claimed.
