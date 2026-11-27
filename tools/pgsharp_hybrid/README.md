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

### Isolated payload artifact

The PR-only `pgbr-private-hybrid-payload.yml` workflow compiles and tests only the PGBR-owned panel on Linux, then uploads the DEX and SHA-256 as a seven-day QA artifact. No proprietary host, signing key, APK or production changes are included. This is a build aid, not a hybrid APK. The workflow must succeed before the artifact can be used.

The first isolated run 46051500585 failed before compilation: setup-android attempted the obsolete `tools` package. The workflow now explicitly requests platform-tools with the pinned action already used by successful PGBR CI, before installing API 36 and build-tools 36.0.0.

### Portable private assembly

The isolated payload build succeeded in run 46051673935: four tests passed, 16,604-byte DEX SHA-256 79c69d28642683e18ca873779c6eaf0ac9521a9479aa22cbbcb0e7cb51ca98db, artifact 15224522369. To avoid uploading the private PGSharp input to public GitHub, the workflow also prepares a portable assembly bundle using the official Apache-2.0 apksig Maven library 8.7.3, with license and POM. The helper signs only with an independent local PKCS12 test key, verifies input and output, checks 16 KiB alignment, hashes and CRC, and never changes host authentication or integrity code. The workflow tests signing/alignment on a synthetic fixture using SDK tools before uploading. The actual private candidate has not yet been assembled or device-tested.

### Actual private candidate assembled and verified — 2026-11-27

- Code HEAD 540b445ebfd7751443f8a4f04a23d6b249153b23; isolated workflow [46052015004](https://github.com/LucianoToscano/PGBR/actions/runs/46052015004) succeeded, including real Android DEX compilation, four tests, official apksig library fixture signing, SDK apksigner verification and 16 KiB zipalign check. Assembly tool bundle artifact 15223829811, SHA-256 8b4cc5c200005f1c92ddd9156a8e76dcf6f3fdb18f947ff12a3e12b07f47f01e, downloaded and all file hashes verified.
- Private local result: PGBR-PGSharp-1.279.0-Hybrid-QA.apk, 210978037 bytes, SHA-256 a91af744a414c0dfa4bfadb89e9de1cc992ff6b92b2a4d319625bc1279be99da. Original pinned source signature verified via official apksig; output signature verified, independent test certificate SHA-256 03dbcd6968d95d71038eeabf95a0e565b7a8fcd7468695535afd6c8c17543491. No signing key or proprietary APK uploaded to GitHub.
- Verified output ZIP alignment and CRC; 2160 original non-signature entries unchanged by SHA-256, only AndroidManifest.xml changed and classes5.dex added. All four original DEX files, PGSharp native libraries and resources preserved. Added DEX has 15 permitted classes and zero descriptor collisions. The local assembly command timed out during the final verification; the completed temporary signed APK was then independently reverified and copied to the final path, with hash equality confirmed.
- This is a private coexistence candidate with PGSharp as host and the PGBR read-only panel, not full PGBR gameplay or integrated live feed. The separate certificate cannot replace installations using a different signer. No device/emulator available; device_tested=false, provider_connected=false. If the PGSharp host rejects repackaging, stop rather than change its integrity or access controls. Full CI on the cited code HEAD was still running; the two policy/governance workflows succeeded. Main and previous releases untouched.
