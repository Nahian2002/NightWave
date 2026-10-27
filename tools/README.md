# NightWave tools — current use and historical checks

**Current source target:** `release/nightwave-nw125-final-candidate`; latest fully executed build evidence remains NW122. These scripts have different evidence strength; do not treat an old smoke PASS as current Android/phone certification.

## Recommended entrypoints

- **Windows build/diagnostics:** root `build-windows.cmd`, `diagnose-nightwave-build.cmd`; corresponding `tools/build-windows.ps1`, `tools/diagnose-build.ps1`. Use only the complete NW125 branch checkout/ZIP.
- **NW125 clean-source gate:** `python tools/nw125_release_candidate_gate.py --self-test` and `python tools/nw125_release_candidate_gate.py --json` (if Python is installed). Detects missing build inputs and accidental generated/secret filenames; **source-only**, not an APK or device pass.
- **Release preflight:** root `check-public-release.cmd`, `tools/public_release_preflight.py`, and source-rights/privacy checks. They only prove the gates they actually execute.
- **Device/signing:** root `check-device-update.cmd`, `capture-nightwave-crash.cmd`, `check-google-oauth.cmd`, and `tools/verify-apk-signature.ps1`.
- **Android CI:** `.github/workflows/current-android.yml` (required build); `android.yml` (manual historical diagnostics); review their trigger/status on the exact current SHA.

## Performance measurement

The opt-in `nw122-benchmark.init.gradle` runs the actual Kotlin host benchmark in three fresh JVMs. `compare_nw122_performance.py --self-test` checks receipt comparison safeguards; the normal command compares matching baseline/candidate receipts. See [NW122 protocol and limits](../docs/NW122_PERFORMANCE_MEASUREMENT.md). Timings are separate from correctness gates and do not establish phone/ART/battery performance.

## Current structural checks

Run `python tools/verify_current.py`. It runs current structural/negative checks and Windows source preflights, invalidates older receipts, records exit codes and fails on any required check failure. The full Gradle JVM suite, APK assembly, instrumentation-source compilation and signature checks run separately. The Windows builder and current CI require at least 1,400 executed passing tests.

The [historical CI inventory](../docs/HISTORICAL_CI_SOURCE_CHECKS_20261027.md) records all 188 old commands and their baseline results. Those source-text diagnostics remain available; they are not all compatible with current refactored source. No failing diagnostic is claimed repaired merely by moving its workflow to manual execution.

## Historical smoke suites

`tools/verify_nw*.py`, `tools/verify_v2_phase*.py`, `tools/NW*Smoke.kt`, `tools/kotlin/`, and the `tools/check-phase*.ps1` family preserve earlier compiler/source regression evidence. They are not obsolete merely because their phase number is old: later scripts, release checks, and test matrices may depend on them. Keep until references and exact replacements are proven.

## Expected evidence levels

1. **Source inspection:** no claim of compilation.
2. **Pure Kotlin/CLI smoke:** validates only named policies and executed fixtures.
3. **JVM unit suite:** record Gradle task, exact SHA, executed test count and failures.
4. **Android/Compose compilation and instrumentation:** provide current Android SDK, APK and device receipts.
5. **Release certification:** also requires signer, data/Drive restore, device matrix, privacy/security and legal source rights.

The authoritative roadmap is [NW111–NW125](../docs/NW111_NW125_SPOTIFY_QUALITY_ROADMAP.md); build handoff is [NW125 final candidate](../docs/NW125_FINAL_CANDIDATE.md). See [repository inventory and retention rules](../docs/NW118_REPOSITORY_HYGIENE_AUDIT.md). Keep reports and local APKs outside version control; do not commit credentials.
