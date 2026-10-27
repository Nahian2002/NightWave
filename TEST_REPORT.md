# Current verification — NW119 cumulative update

27 October 2026. Review branch: `update/nightwave-cleanup-nw119-lyrics`. Parent: `64160f1388b6dcef6439f384602d1a7c9a6c63d8` (NW118).

## Executed locally

- Android app Kotlin/Compose compilation: PASS.
- JVM test compilation and execution: **1,421 tests in 178 suites, zero failures/errors/skips**.
- Debug APK assembly: PASS.
- Android instrumentation-source compilation: PASS. Instrumentation was **not executed on a device**.
- Current structural/negative and Windows safety checks: **49/49 PASS** via `python tools/verify_current.py`.
- Android lint: **zero errors; 315 warnings and 17 hints** after repairing missing Media3 opt-ins, API-26 theme compatibility, session result constants, small text, RTL padding and legacy artwork click accessibility. Most remaining warnings concern localization, suggested Kotlin helpers and dependency updates. No lint baseline or global error suppression was introduced.

The optional Android Auto voice-playback lint rule is locally suppressed with a manifest comment because no voice-playback handler exists. Browsing/search support does not justify falsely advertising a voice-play intent.

See [NW119 evidence](docs/NW119_CLEANUP_AND_LYRICS_EVIDENCE.md) for final builder/signature results, source fingerprints, remaining gates and checkpoint locations. Generated reports and APKs are kept outside Git.

## Historical diagnostic status

The [old CI source-check inventory](docs/HISTORICAL_CI_SOURCE_CHECKS_20261027.md) records a baseline run of 188 commands, with 90 passes and 98 failures. Many tests assert old strings/hashes; some still need individual review. These remain available in the manual historical workflow and are **not relabeled as passed**. The new required workflow runs maintained structural checks plus the complete real JVM suite, Android compilation, lint, source attestation, fresh test counts and APK identity verification.

## Pending acceptance

Physical Samsung/TalkBack/Bangla lyric timing, 5k-library and undo stress, background/headset/EQ behavior, installed-phone signer continuity, production Google OAuth/Drive backup/readback/restore, and distribution rights/privacy approval remain unverified. Public release remains **NO-GO**.

Historical v0.8 evidence is preserved in [the archive](docs/TEST_REPORT_V08_ARCHIVE.md). Earlier phone observations do not certify the current cumulative build.
