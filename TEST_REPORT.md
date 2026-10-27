# Current verification — NW126 build identity and baseline

28 October 2026. Branch: `update/nightwave-nw126-build-identity-baseline`, based on protected audit parent `daec2f4d91701286fe7168d8ecff4c3ba3c2287f`.

- Complete Windows builder: **BUILD SUCCESS**. Production Kotlin/Compose, Android test-source compilation and debug assembly pass.
- **1,496 JVM tests / 188 suites**, zero failures/errors/skips. Twelve new methods cover identity/schema/hash/count/revision validation, bounded/stalled reads, SHA256 vectors and installed artifact/signer/split-base receipt formatting.
- **54 current checks PASS**, retaining NW125 gates and adding fixture preparation and provenance negative cases. Missing/duplicate/oversized/invalid APK assets, wrong variant/count/digest and an actual older-source APK are rejected.
- Independent APK asset/source verification: **PASS_EMBEDDED_SOURCE_MATCH**; all 809 source inputs match the canonical identity. The Windows builder invalidates old provenance and refuses promotion on mismatch; current CI has the same packaged-identity gate.
- Two new Android cases compile for packaged metadata and dialog/copy semantics; no Android instrumentation test was device-executed.
- Android lint: **0 errors, 313 warnings, 17 hints**. No new warning suppression or baseline.
- Canonical source ID: `NW-debug-F2C870939591E7C8`; source SHA256 `F2C870939591E7C82DE9D357C3FD1F2379469ED920E3B477C251ABA9E3402DB9`.
- APK SHA256: `9297D5F9747D38208321652A709DE06DCDDAF4131572B5EDE2B7AE3FEDEFC581`.
- Retained NW08 source-manifest fingerprint: `95796293E6F87A84713DE48BD3524510402B235312EC60975756587AF23B4435` (809 inputs). This manifest-file hash and the new canonical digest have different documented encoding/ordering contracts.
- Package `com.nightwave.player`, version `2.0.0-alpha5s`, code35, compile/target36/min26 preserved. Certificate SHA256 `EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394`; local prior-APK signer matches. Installed-device comparison remains unrun.

You → App health → App details is implemented to show/copy source identity, installed APK/base-APK hash and public signer hashes using background bounded reads. No account, library, music or signing-key data is included. Revision is explicitly context rather than an assertion that a working snapshot equals HEAD. No on-device render or clipboard acceptance is claimed.

The final disposable fixture pack binds the exact verified APK/source to all 139 original plus 80 new acceptance IDs, six original WAVs, negative audio and a 10k synthetic metadata recipe. Preparation does not seed an app, connect to a phone or contact an account. Actual hardware conditions remain unset; all run-plan cases begin NOT_RUN while source-ledger outcomes are retained separately.

`adb devices -l` returned no connected devices. NW126-02 is PASS for executed gates; NW126-01/-03 are PARTIAL; NW126-04 and device status remain NOT_RUN. Actual recorded installed attribution, profile capture, permitted signer-preserving installation and data-preservation tests remain required. Source/local completion permits the independent NW127 design work without inventing these results.

See [NW126 evidence/protocol](docs/NW126_BUILD_IDENTITY_BASELINE.md), [active roadmap](docs/PRODUCT_QUALITY_ROADMAP.md) and [backlog](docs/quality/NW126_NW145_BACKLOG.json). The frozen [post-NW125 product score](docs/audits/2026-10-28_PRODUCT_QUALITY_AUDIT.md) remains 6.7/10. Phone performance, real restore, offline/route/soak, accessibility and applicable release approvals remain open; public release stays NO-GO. Hosted check allocation is reported separately on the PR.

Next source batch: **NW127 compact visual system and hierarchy**.
