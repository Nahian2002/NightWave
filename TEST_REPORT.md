# Current verification — NW128 shared offline shell

28 October 2026. Branch `update/nightwave-nw128-offline-shell`, protected NW127 parent `a6d8842f5379579a164517745567664751713e7c` (PR #130).

- Complete Windows builder: **BUILD SUCCESS**, including Android production Kotlin/Compose, instrumentation-source compilation and debug APK assembly.
- **1,515 JVM tests / 190 suites**, zero failures/errors/skips. Thirteen new policy cases cover exact private routes, bounded Unicode query/positions, invalid/replayed main handoff, view-only player requests, legacy-root decisions and separated catalog query/origin restoration.
- **55/55 current source checks PASS**. The new NW128 positive/negative wiring guard checks all four migrated callers, main request acknowledgment, controller lifecycle/recovery and navigation without service Play. Existing playback/account/storage/recovery/provenance guards remain.
- **Eight new Android tests compile only**: shell route/main/transport/hide/recovery callbacks, private intent normalization and a short native viewport measurement. No Android test was device-executed. Actual 30-cycle route/Back/gesture and task-stack tests remain NOT_RUN.
- Android lint: **0 errors, 313 warnings, 17 hints**, unchanged final totals; no new lint suppression or baseline. The task-ID lookup uses an API-appropriate fallback for Android 26–28.
- Exact packaged-source gate: **PASS_EMBEDDED_SOURCE_MATCH**, **818 inputs**. Build ID `NW-debug-208A902EBDBBFC18`; canonical source SHA256 `208A902EBDBBFC181507E36D430A6AF7C0D1DC36192542D70158CF5CB159EFA2`.
- APK SHA256: `42168E0715652385A1C4A8C0B82F779B424F0D8E9FE32D6418FB5D271770B28F`.
- Retained NW08 manifest fingerprint: `645643898C22A4DD115419E44A4175A166831EA6D68ABE9C255AC0251BE6C827`. It uses a different documented ordering/encoding contract from the canonical source digest.
- Package `com.nightwave.player`, version `2.0.0-alpha5s`, code35, compile/target36/min26 unchanged. Signer SHA256 `EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394` independently matches the protected NW127 APK. Installed-device comparison remains NOT_RUN.

On device, Find audio, Downloads and Audio link share themed navigation and selected-song controls around their retained native panels. Sibling routes reuse activities; acknowledged main handoff avoids replaying navigation on recreation. Browsing/form state, generation fences, a whole-panel viewport and explicit in-place native recovery are implemented. See [NW128 contract and device protocol](docs/NW128_UNIFIED_OFFLINE_SHELL.md). Native panel interiors still need later styling/adaptive work; this is an incremental migration.

`adb devices -l` returned no connected device. No installation, account operation, import, download or user-library migration was performed. All four NW128 acceptance rows are **PARTIAL**, device status **NOT_RUN**. Real task/gesture cycles, selected-song and audible continuity, picker/recreation, active transfer/recovery and input/accessibility acceptance remain required. A fresh disposable 219-case plan binds the exact verified APK/source without seeding a device.

The first lint pass caught an API-29 task-ID call; the current source fixes it with the older-API branch. A concurrent build/lint attempt collided in generated KSP caches. That failed attempt remains failed; daemons were stopped and only the explicitly checked generated cache was moved aside. The final builder and lint ran sequentially and passed. Failed and final logs are preserved separately; no source/data reset or warning baseline hid the failure.

NW126/NW127 physical and installed-source/signer/data-preservation blockers carry forward. The original 139-case matrix and frozen audit are unchanged; the 80-case extension remains separate. The provisional score stays **6.7/10** and public release **NO-GO**. Hosted CI allocation/status is recorded independently on the PR; local results do not imply hosted execution.

Next independent source batch: **NW129 — complete local search corpus**, with the [active roadmap](docs/PRODUCT_QUALITY_ROADMAP.md) and [backlog](docs/quality/NW126_NW145_BACKLOG.json) retaining all physical blockers.
