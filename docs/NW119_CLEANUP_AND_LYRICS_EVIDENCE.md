# NW119 — repository cleanup, build recovery and lyrics reliability

Date: 27 October 2026. Parent: `64160f1388b6dcef6439f384602d1a7c9a6c63d8`. Branch: `update/nightwave-cleanup-nw119-lyrics`.

## Recovered baseline

The configured OneDrive workspace was empty and rejected creating `.git` with a Windows file-not-found error. The authenticated GitHub account identified `Nahian2002/NightWave`. The newest cumulative ref was NW118; default `main` still contained v0.8. Recent chats were found, but their assistant answers were exposed only as content references. The repository's NW111–NW125 roadmap supplied the implementation order; NW119 lyrics is the next ordered wave.

Work is in `C:\Users\jayed\NightWave-work`. A complete baseline Git bundle and original commit SHA were saved before edits in `C:\Users\jayed\NightWave-checkpoints\20261027-cleanup-nw119`.

## Concrete repairs

- A missing `assertTrue` import in `OfflineLibraryHealthPolicyTest` blocked the actual unit-test compiler.
- Windows preflights were stale after NW113 availability snapshots, NW116 mini-player layout and NW117 Home routing changes. Checks now inspect current delegation and preserve unknown-index, width/font, Home destination and playback-hide safeguards. The syntax checker also includes the previously omitted NW76/NW77 scripts. JVM acceptance floor increased from 1,000 to 1,400 executed tests.
- Fixed a Python syntax error in the hosted-runner triage self-test. The structural scanner now identifies real PEM key headers without flagging its own marker declarations, with a negative fixture that still detects a key header.
- Media3 callers now explicitly opt into the pinned unstable API at the component scope. Session error replies use the current typed `SessionError` constants. The API-27 navigation-bar setting moved into a qualified theme while API 26 retains the base palette. Legacy artwork taps call `performClick`; small text and asymmetric padding were corrected.
- Android Auto browsing/search remain present. Voice-initiated playback remains unsupported; a narrowly scoped lint annotation documents that missing optional capability instead of adding a false intent filter.

## NW119 lyric changes

The parser previously found timestamp-like text anywhere in a row, including metadata/prose, and silently truncated long lyric text. It now accepts only contiguous leading timestamps, rejects ambiguous/malformed rows and fails overlong lines before saving. Review diagnostics reuse the same row parser, preventing disagreement about ignored/duplicate/out-of-order cues. UTF-8, 64 KiB and 1,200-cue bounds, simultaneous-line order, source review, explicit replacement/fingerprint checks and track-bound seeking remain.

Coarse and fine timing adjustments now both reject invalid saved offsets. Extreme cue seek values saturate instead of wrapping to zero, and a very large declared duration cannot overflow the warning calculation. Nine new JUnit methods cover metadata/prose injection, malformed mixed prefixes, embedded timestamps, Unicode boundaries, Bangla roundtrip, review agreement, safe failure feedback and timing overflow. Existing wrong-song, follow/TalkBack policy, editor/export and large-LRC tests still execute in the full suite.

## Cleanup and organization

- Removed only `app/src/main/res/drawable/bg_avatar.xml` and `app/src/main/res/drawable/bg_card.xml`. Android lint identified both as unused and a whole-repo name search found no source/tool/doc references. The baseline Git bundle and parent commit retain both files for recovery.
- No byte-identical tracked-file groups were found. Historical checks, migrations, source tests, signing tools and release evidence were retained. No user music, account, playlist, local preferences or key was read or deleted.
- Replaced conflicting current-branch announcements with a short README, current build guide and test report. Complete earlier text is archived with links corrected for its new location.
- The old `android.yml` remains manual historical diagnostics. A 188-command baseline audit had 98 failures; those are recorded individually and not claimed repaired. Current CI uses `current-android.yml`: current source/negative checks, Windows preflights, complete Gradle tests, Android instrumentation-source compilation, APK assembly, lint, source attestation, executed-test minimum and APK identity/signature. No hosted run success is claimed from local results.
- Retained the compiler-exported Room schema v1 as migration evidence; no database version or persisted schema was changed.

## Executed verification

- `python tools/verify_current.py`: **49/49 PASS**, including Windows PowerShell 5.1 safety checks and negative fixtures.
- Gradle `:app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin`: **PASS**, 1,421 tests in 178 suites, no failures/errors/skips.
- `:app:lintDebug`: **PASS**, zero errors, 315 warnings, 17 hints. Remaining localization/helper/dependency recommendations are not silently suppressed or claimed fixed.
- Final one-click Windows build and signature/source snapshot receipts: see the final verification addendum below and external checkpoint manifest.

## Remaining gates and next work

Physical-device testing, installed-app signer continuity, Google Drive production roundtrip, measured performance, remaining NW118 large-library acceptance and legal/privacy approval remain open. Public release remains **NO-GO**. The next ordered feature wave is **NW120 Audio/EQ**; source completion and local tests do not finish NW125 release certification.

## Recovery

Use the external `baseline.bundle` to recover the exact NW118 parent. Preserve the update branch for review; do not reset a populated phone or merge into old `main` as a cleanup shortcut. The checkpoint folder contains changed-file hashes, a complete source ZIP, checksums, debug candidate and resume instructions after final packaging. No update is installed by this work.

## Final Windows verification addendum

The complete `tools/build-windows.ps1` finished **BUILD SUCCESS** after every source preflight, full Gradle/JVM execution, APK package/signature verification and before/after source fingerprint comparison. The independent NW101 recheck also returned `NW101_SOURCE_SNAPSHOT_MATCH files=783`.

- Package/version: `com.nightwave.player` / `2.0.0-alpha5s` / code `35`.
- Final JVM receipt: **1,421 passed / 0 failed / 0 errors / 0 skipped**, 178 suites; required minimum 1,400.
- Compiled working-tree source fingerprint SHA256: `EC0EA3E072EED93206BEB263C48F04B1C06D668E316AE1CF2819E14EA3EC0CDC`. The manifest records 783 build-affecting source/resource/test/tool files, including actual working-tree line endings.
- Debug APK SHA256: `1BFA3C03146B60F4E2C110E2A1C09B77642A7D15C5502A77365D1C8759C13635`.
- Debug certificate SHA256: `EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394`.
- Installed-phone signing continuity: **NOT CHECKED**, no previous APK/device comparison was supplied. Cryptographic signature verification is not an installation recommendation.
- Android lint reports zero errors, 315 warnings and 17 hints; all current app changes precede this successful check.

Build receipts are in local `dist/`; the external checkpoint retains copies, exact ZIP member hashes and CRC verification, the original Git bundle, and changed-file/recovery instructions. No local build output, signing key, account data or music is committed.
