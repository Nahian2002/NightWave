# Current verification â€” NW132 maintenance and cleanup

> Source-publication follow-up: repository is now public on canonical `main`; the app/build inputs and APK below are byte-identical. Repository-only controls and hosted-source/privacy scans are separate from these retained app tests. See [publication record](SOURCE_PUBLICATION.md) and [PR #138](https://github.com/Nahian2002/NightWave/pull/138).

[Draft PR #137](https://github.com/Nahian2002/NightWave/pull/137).
28 October 2026. Branch `maintenance/nightwave-nw132-cleanup`; protected NW132 parent `aa6a56fd553a297d5aa790f6049a1f4634a11b63` (PR #136). Code repair `ceae46e06cc5688610e8b4cd66f3a2f3600a35fe`. [Maintenance scope and deletion inventory](docs/REPOSITORY_MAINTENANCE_20261028.md).

Library now starts a newly selected collection at its header instead of inheriting another list's deep scroll offset. Its active list still uses saveable state across recreation. A new actual-screen regression case compiles; phone execution is NOT_RUN. The NW122 host benchmark safely handles absent OS metadata; its prior nullable-receiver warning is absent from the fresh build log. Benchmark scenarios/results are not rerun or claimed improved.

Removed 354 disposable local files / 60.79 MiB after checking protected checkpoint hashes and deletion boundaries. No tracked source/migration/test/history, private media, app data or checkpoint was removed. Useful incremental/compiler/SDK caches are retained. Active information pages now point to current source/evidence; historical verification is explicitly archived.

- Complete Windows builder PASS: production Android/Compose, instrumentation-source compilation and debug assembly. **1,573 executed JVM tests / 197 suites**, zero failures/errors/skips.
- Current runner: **59/59 source checks PASS**, including the new scroll-state broken fixture. Source checks do not prove runtime behavior.
- Sequential lint: **0 errors / 313 warnings / 17 hints**, unchanged from NW132; no suppression/baseline added.
- Build **NW-debug-F3E970F569154FEB**; **843** inputs; canonical source SHA256 **F3E970F569154FEB763708A40BF9BE3416B53FF41BEB4E2ECAB8130665E64914**.
- APK SHA256 **56DA667E13A33CEF1D57CD845C1AF3A62EA699361FDFA645E8FA145151784911**; embedded-source verification **PASS_EMBEDDED_SOURCE_MATCH**. Revision context is the code repair above. NW08 manifest fingerprint **59A0F8A57888F3C3C780960E78CB450AB6DBE6A26CDD96FD226D9AEC02E37336** uses its separate retained encoding/ordering contract.
- Application `com.nightwave.player`, `2.0.0-alpha5s`, code35, compile/target36/min26 unchanged. Verified certificate **EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394** matches protected NW132. Installed-phone signer NOT_RUN.
- No Android device connected. No install/reset/account/app-data operation, dependency/permission/schema change, playback/offline-worker rewrite or repository visibility change.
- Private checkpoint: `C:\Users\jayed\NightWave-checkpoints\20261028-repository-maintenance`; protected history, raw cleanup inventory, source/APK/signing/JUnit/lint/source receipts and final recovery archive. Hosted CI is recorded separately in the review.

All prior acceptance limits remain: actual screen navigation/restoration, native dialogs/system picker/store/restart, accessible geometry/contrast/large type, audible playback, installed update and live account recovery are unrun. See [NW132 protocol](docs/NW132_CLEAR_LIBRARY.md), [active acceptance cases](docs/quality/NW126_NW145_ACCEPTANCE.csv) and [readiness](RELEASE_READINESS.md). Earlier batch evidence remains historical, not a new run on this candidate.

This maintenance is not NW133. Next: **NW133 â€” player, artwork and metadata finish**. Product score remains provisional **6.7/10**, app release **NO-GO**; source-only publication is a separate decision. [Active roadmap](docs/PRODUCT_QUALITY_ROADMAP.md).
