# NightWave tools — current use and historical checks

**Current implementation baseline:** OFFLINE_FINISH on `fix/nightwave-offline-finish`, [draft PR #155](https://github.com/Nahian2002/NightWave/pull/155); 1,747 JVM tests / 216 suites, 73 source checks, lint 0 errors / 317 warnings / 17 hints. [Review/plan/results](../docs/OFFLINE_FINISH_RECORDING_REVIEW.md), [verification](../TEST_REPORT.md). Twenty-one new JVM methods execute; actual phone transfer/art/removal/accessibility tests remain NOT_RUN. Actions stays disabled.

## Recommended entrypoints

- **Offline finish fences:** `python tools/verify_offline_finish.py --self-test` protects real parallel response/cancel/commit wiring, cached-only optional metadata, deletion reconciliation, buffering intent, ETA and retained routes with broken-caller fixtures. JVM HTTP/file tests and phone acceptance remain separate evidence.

- **NW136 range/recovery fences:** `python tools/verify_nw136_transfer.py --self-test` checks the real worker/caller path, validators, sync-before-checkpoint, hashed recovery, bounds and diagnostics using broken-caller fixtures. Real HTTP/file behavior executes in the JVM suite; source checks do not certify phone performance.

- **NETWORK01 transport fences:** `python tools/verify_network_throughput.py --self-test` checks pooled transport, local routing, bounded read-ahead, flush-before-sync and retained source/cancel checks with eight broken-caller fixtures. Included in current runner; phone throughput is separate.

- **NW135 import/readiness fences:** `python tools/verify_nw135_offline.py --self-test` checks pre-commit audio probe, cleanup, cached background health, index listener and source routes with nine broken-caller fixtures. Included in the current runner; actual SAF/audio/device proof is separate.

- **NW134 queue fences:** `python tools/verify_nw134_queue.py --self-test` checks actual UI/bridge/service guards and seven broken-caller fixtures. Included in the current runner; no runtime/audio certification.

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

`verify_editorial_finish.py --self-test` guards source-proportioned frames, full original Details, single lead-mix emphasis and factual summaries, plus root shelf-touch ownership/release with ten broken caller fixtures. The current runner totals 65 checks. These are source checks; the 468-case Android matrix is compiled only.

`verify_composition_finish.py --self-test` adds ten broken caller fixtures for creator-corroborated captions, edition retention, compact mix facts, sparse Library routes/disclosure and consolidated You actions. Current total: 66 source checks. The Android matrix is 14 x 36 = 504 compiled cases only; new phone evidence is pending. DESIGN04 guards now follow the compact summary caller while retaining all earlier negative cases.

`verify_continuity_finish.py --self-test` guards actual player lifetime, recording/consent controls, UTC/totals/accessibility, measured metrics and real cover wiring with ten broken fixtures. The current runner now also executes the updated `verify_nw58_overlay.py --self-test`: 68 checks total. NW58/NW59 require one synchronous transition, no full-screen alpha fade or effect delay and interrupted-enter backdrop retention; old fixtures remain, with new blank/delay negatives. Android matrix: 16 x 36 = 576 compiled only.
