# NightWave tools — current use and historical checks

**Current implementation baseline:** DESIGN03 on `design/nightwave-content-finish`; **1,632 JVM tests / 204 suites** and **64 current source checks** pass. See [current verification](../TEST_REPORT.md) and [theme correction](../docs/design/EXPRESSIVE_THEME_FINISH.md). All hosted/device evidence remains separate; Actions stays disabled.

## Recommended entrypoints

- **Windows build/diagnostics:** root `build-windows.cmd`, `diagnose-nightwave-build.cmd`; corresponding `tools/build-windows.ps1`, `tools/diagnose-build.ps1`. Use the complete current integrated branch checkout/ZIP linked in the root README.
- **NW125 clean-source gate:** `python tools/nw125_release_candidate_gate.py --self-test` and `python tools/nw125_release_candidate_gate.py --json` (if Python is installed). Detects missing build inputs and accidental generated/secret filenames; **source-only**, not an APK or device pass.
- **Release preflight:** root `check-public-release.cmd`, `tools/public_release_preflight.py`, and source-rights/privacy checks. They only prove the gates they actually execute.
- **Device/signing:** root `check-device-update.cmd`, `capture-nightwave-crash.cmd`, `check-google-oauth.cmd`, and `tools/verify-apk-signature.ps1`.
- **Android CI:** definitions are retained under `.github/workflows/`; repository Actions is deliberately disabled. Local build/lint/source receipts are separate from hosted checks.

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

The authoritative roadmap is [NW126–NW145](../docs/PRODUCT_QUALITY_ROADMAP.md); build handoff is [NW125 final candidate](../docs/NW125_FINAL_CANDIDATE.md). See [repository inventory and retention rules](../docs/NW118_REPOSITORY_HYGIENE_AUDIT.md). Keep reports and local APKs outside version control; do not commit credentials.

## NW126 identity and baseline

`verify-build-provenance.ps1` reads the actual APK asset and recomputes its source identity; Windows promotion and current CI require it. `nw126_acceptance.py --self-test` validates mapping/fixtures; `--prepare --provenance RECEIPT --apk APK --output NEW_DIRECTORY` binds a disposable 219-case pack to a verified current artifact. It never installs, contacts accounts or seeds app data. See [NW126 evidence/protocol](../docs/NW126_BUILD_IDENTITY_BASELINE.md).

NW128 adds `verify_nw128_shell.py --self-test` to the current runner, bringing it to 55 source checks. It checks migrated callers, separate transport/navigation ownership, lifecycle release and acknowledged main handoff; it rejects broken wiring fixtures. This is not Android task-stack or device proof. See [NW128 protocol](../docs/NW128_UNIFIED_OFFLINE_SHELL.md). Do not run Gradle build and lint concurrently in the same checkout; run the complete builder first, then lint.

NW129 adds `verify_nw129_search.py --self-test` to the current runner (56 checks) for full caller membership, source observation/cleanup, revision/query tags and paging. `nw129-benchmark.init.gradle` runs the actual prepared typing runner in fresh host JVMs; it does not establish Android input/render latency or heap behavior. See [NW129 protocol](../docs/NW129_COMPLETE_LOCAL_SEARCH.md).

NW130 adds `verify_nw130_public_search.py --self-test` (57 current checks). Its negative fixtures guard evidence wiring, exact single-source selection, phases/deadline/generation and both request-scoped HTTP paths. The frozen test corpus is under `app/src/test/resources/`; JUnit emits `app/build/reports/nw130-public-corpus.json`. MockWebServer checks actual local socket/body cancellation and isolation; phone/provider acceptance remains separate. See [NW130 protocol](../docs/NW130_DECIDABLE_PUBLIC_RESULTS.md).

NW133 adds `verify_nw133_player.py --self-test` (60 current checks), guarding measured artwork/cache/fallback wiring, shared status, scoped seek and existing queue/lyrics paths with seven broken fixtures. See [NW133 protocol](../docs/NW133_PLAYER_ARTWORK_METADATA.md). Fourteen JVM tests execute; six new Android screen tests compile only.

`verify_matte_design.py --self-test` guards real interaction-source feedback, source-bound reused artwork, clipped/API-gated blur, complete measured collection pairs, native read-only theme selection and accessible seek input/listener cleanup. The source runner totals 61 checks. The existing NW59 palette literal follows approved Pearl #61527F; JVM tests still enforce actual contrast.

`verify_theme_expression.py --self-test` adds eight broken caller fixtures for actual accent fills, selected navigation/mini roles, bounded singleton art, theme-colored mood glyphs, secondary Material roles and all four native dialog selections. Current runner: 62 checks. Actual JVM contrast assertions remain required after changing the approved Pearl literal to #1C47AD.
