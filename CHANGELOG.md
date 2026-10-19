# NightWave Changelog

## 2026-10-19 — NW-02 Sound Studio (source committed, not release-verified)

- Replaced the nested Android audio equalizer dialogs with a dedicated, theme-aware Jetpack Compose Sound Studio from the You page.
- Added Android device-discovered preset chips, band-level sliders, manual Apply, flat reset, accurate session status and a response-curve preview based on actual reported band gains.
- Fixed a playback-service defect where changing EQ presets on an already active audio session could retain the old preset.
- Kept explicit, acknowledged playback-service commands and reported unsupported/missing audio sessions rather than claiming an effect was audible.
- Added new JVM policy tests for manual-band range/shape safety and preset bounds, plus a source integrity gate and CI branch trigger.
- Kept all prior NW-01 Shuffle/Google fixes; no change to guest library or app signing. Full Gradle build and Samsung real-audio verification remain pending.
- See [NW-02 acceptance and limitations](docs/NIGHTWAVE_NW02_SOUND_STUDIO_QA.md).


## 2026-10-19 — NW-01 phone-reported control and authorization recovery (unverified)

- New branch based on the previously audited `6960dc5` commit; that tested source snapshot remains intact.
- Public video Shuffle sends an absolute On/Off state with immediate UI feedback and a service receipt to reduce stale-snapshot/rapid-tap races. Four JVM policy tests added.
- Equalizer status now distinguishes waiting for an audio session, competing effect ownership, unsupported presets/bands and a device-route-dependent Android effect activation. Audible success has **not** been established.
- Optional Google Drive backup access has a visible You-page shortcut and more useful authorization error/status reporting. This does **not** provide full Google sign-in/account sync.
- The latest NW-01 GitHub Actions jobs failed before any job steps ran, so they are **not** compilation or test evidence. Full local Windows build, signer and device QA remain pending.
- See [NW-01 QA and acceptance](docs/NIGHTWAVE_NW01_FIELD_FAILURES_QA.md).


NightWave uses stacked, independently reviewable development branches. This changelog describes **source changes**. A feature marked implemented below is **not necessarily validated by a successful Android build or physical-device test**. Check [verification status](docs/VERIFICATION_MATRIX.md) before installation.

## 2026-10-19 — Final 6X8C4 branch audit and queue recovery (development)

- **Observed last build:** The supplied Windows 13:34 build passed main Android Kotlin compilation, Android-test Kotlin compilation and APK assembly but failed 1/771 JVM tests at `OfflinePlaybackQueueTest.jumpHandlesBoundsWithoutDamagingSelection`. This was an actual failed full build, **not** a release.
- **Offline queue:** Corrected non-shuffled Previous and direct jump order; retained shuffle visit history and guarded invalid jumps; fixed single-item Shuffle + Repeat All. Added regression tests for history, jump bounds, shuffle transitions and repeat.
- **Online/reference queue:** Preserve the user's selected URL when input URLs are deduplicated, avoiding an unintended change of selected song. Added regression tests.
- **Warning cleanup:** Explicitly handle nullable journal parents within app-private roots, migrate deprecated NewPipe video-only field to the officially documented `isVideoOnly()` accessor, use auto-mirrored Compose icons, remove ineffective Media3 Kotlin opt-in and update its corresponding preflight.
- **CI and docs:** Enabled CI push verification for the current build-recovery branch, refreshed the README/About/build/release guides, added a feature-level acceptance checklist and moved the full former README to historical notes without deleting it.
- **Evidence status:** All fixes above are **committed to GitHub source**. No completed fresh full Gradle verification, signing compatibility check, Android instrumentation execution, Android Auto validation or physical-phone QA is yet confirmed. Never install an APK from the failed build.

## 2026-10-19 — Phase 6X8C4: Daily Mix unit-test compilation hotfix

- **Observed:** The complete 6X8C3 Windows Gradle run **passed** `:app:compileDebugKotlin` and `:app:compileDebugAndroidTestKotlin`, but **failed** at `:app:compileDebugUnitTestKotlin` on two Daily Mix test argument types.
- **Fix:** Pass `valid + valid` rather than `listOf(valid, valid)` in the duplicate-source test; pass the flat `favorites` list rather than `listOf(favorites)` in the immutability test. All eleven Daily Mix tests remain.
- **Prevention:** Added guards to the Windows Kotlin compatibility script and original 6X6 CI source gate.
- **Not changed:** Playback services, imported songs, playlists, lyrics, user data, backup, signing, UI, Java File warning and Media3.
- **Release status:** Fresh Android Gradle build/unit tests/device QA for 6X8C4 remain **pending**. [QA details](docs/NIGHTWAVE_PHASE6X8C4_UNIT_TEST_COMPILE_QA.md).

## 2026-10-19 — Phase 6X8C3: Kotlin compiler recovery (development)

- **Fix:** `MediaSession.ConnectionResult.AcceptedResultBuilder(session)` matches the Media3 session library constructor. Removed unsupported second `controller` argument.
- **Fix:** Smart Library Long Listens uses `VideoPreview.durationSeconds`, the field defined by `YouTubeGateway.kt`, with a four-minute threshold.
- **Regression coverage:** Added two 239/240/241-second and unknown-duration tests. Updated the Windows compatibility preflight, cumulative Python CI source gates and a focused two-error regression gate.
- **Isolated policy verification:** Compiled the corrected smart-library policy with matching `VideoPreview.durationSeconds` metadata and minimal collaborators; 13/13 behavior assertions passed. This is not a full Android/JUnit build.
- **Repository documentation:** Promoted current status to top of README; added About, changelog, build and release instructions and verification matrix.
- **Evidence:** The user-supplied previous 6X8C2 Windows run reached `:app:compileDebugKotlin` and failed at these two expressions. A fresh 6X8C3 Android build/JUnit/AndroidTest/device session is **not yet confirmed**. Do not call this a release.

## 2026-10-19 — Phases 6X8C1–6X8C2: Preflight recovery

- 6X8C1: removed duplicated Media3 `UnstableApi` import.
- 6X8C2: reconciled Phase 6P dual-ExoPlayer crossfade safety checks with the temporary local decoder and Android Auto queue ordering; retained lifecycle cleanup assertions.
- The 6X8C2 Windows preflight passed and advanced into Gradle, revealing the compiler errors fixed in 6X8C3.

## 2026-10-19 — Phases 6X8–6X8C: Android integration (development)

- Android home-screen playback widget with existing Media3 transport, immutable control intents and a non-exported action receiver.
- Headphone-disconnect pause handling and background media session continuity.
- Converted one existing playback service to a browsable Media3 `MediaLibraryService`, with local library sections for imported music, favorites, recent tracks and playlists, plus locally indexed search.
- Android Auto device/emulator verification and full production release remain pending.

## 2026-10 — Phases 6X1–6X7: Music UX development

- 6X1: offline gapless-capable Media3 queue preparation.
- 6X2: adjustable local audio crossfade with a bounded secondary decoder.
- 6X3: opt-in non-boosting local volume balance for imported tracks.
- 6X4: repeat and shuffle for imported tracks, including previous-song history and fallback navigation.
- 6X5: read-only smart collections and playlist folders in independent metadata storage.
- 6X6: deterministic private Daily Mix 1/2/3 based on already saved/recent tracks, optional consented playback memory.
- 6X7: Follow lyrics, line-level progress and 50-ms per-song/LRC draft timing adjustments.

## Earlier phases

Older phase notes and original branch lineage remain in [README.md](README.md) and the `docs/` directory. Historical branch QA outcomes do not automatically certify these newer integrated development branches.
