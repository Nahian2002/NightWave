# Current verification — NW121 premium account/recovery

27 October 2026. Branch: `update/nightwave-nw121-premium-account-recovery`. Parent: verified NW120 `eb2722002fdd047bc9f0bff0a1d0e8be83eb167e`.

## Executed locally

- Complete one-click Windows builder: **BUILD SUCCESS**.
- JVM regression suite: **1,453 tests in 183 suites, zero failures/errors/skips**. Twenty new methods execute account access, wrapped-error, presentation and isolated Drive roundtrip cases.
- Android production Kotlin/Compose and instrumentation-source compilation: **PASS**. Four new Android store tests were compiled; no Android/device tests were executed.
- Debug APK assembly and package/version/signature validation: **PASS**. Independent signer comparison against saved NW120: **MATCH**; installed phone remains untested.
- Current structural/negative and Windows preflights: **49/49 PASS**.
- Android lint: **PASS**, zero errors, 313 warnings and 17 hints. No new suppression or warning baseline was added.
- Source attestation: **792 build-affecting files match the compiled working-tree snapshot**.

## What the roundtrip proves

The real `NightWaveDriveClient`, multipart upload, remote listing/readback and cloud/guest codecs execute through an in-memory HTTP interceptor. A disposable synthetic guest archive is uploaded, downloaded and decoded unchanged; identical verified copies are reused, while corrupt/denied readback and network or invalid input fail safely. Tests assert appDataFolder scope and no overwrite/delete requests. No Google account, OAuth token, internet request, real audio or on-device library was used.

This does **not** execute live Google OAuth, installed-signer setup, Android account switching or the real local-library restore transaction on a phone. Existing restore preview, same-account revision and local-change guards remain compiled and covered by their existing policy tests.

## Visual and device boundary

The redesigned hub is real Compose source compiled into the APK. Pure layout/action tests cover compact width and 2x font decisions; no device/emulator screenshot, pixel comparison or TalkBack run was performed. See the [NW121 acceptance checklist](docs/NW121_PREMIUM_ACCOUNT_RECOVERY_EVIDENCE.md) for live account, layout, cancellation, observer, low-storage and restore acceptance.

Physical Samsung validation, installed-phone signer, production Google restore/readback and rights/privacy approval remain open. Public release remains **NO-GO**. Next ordered wave: **NW122 measured performance**.

Earlier results remain in [NW120 evidence](docs/NW120_AUDIO_SESSION_EVIDENCE.md), [NW119 evidence](docs/NW119_CLEANUP_AND_LYRICS_EVIDENCE.md), and the [historical diagnostic inventory](docs/HISTORICAL_CI_SOURCE_CHECKS_20261027.md).
