# NightWave Changelog

## 2026-10-22 — NW-09 Google consent & equalizer device reliability (development)

- Preserved only the pending Google Drive authorization action across Android Activity recreation while the account/consent screen is open; does not persist tokens or account identifiers.
- Added a bounded Google services authorization-request timeout (not an account picker timeout) and ignored stale callbacks after request cancellation/recreation.
- Added Google Play services availability/status diagnostics before starting Drive authorization, plus clearer messages for orphaned permission results.
- Updated audio session discovery to avoid showing stale `EFFECT ACTIVE` states after playback stops or the service process changes.
- Added short asynchronous Android equalizer-capability retries and more tolerant command receipt polling in Sound Studio.
- Added three JVM recovery-policy tests and the NW-09 source gate to CI while preserving previous NW-01–NW-08 checks and source attestation.
- **Not verified:** full Gradle build, actual Google permission screen, audible DSP on a Samsung device, live UI and crash/ANR behavior. A verified release APK has not been produced. See [NW-09 detailed evidence](docs/NIGHTWAVE_NW09_DEVICE_RELIABILITY.md).


## 2026-10-22 — NW-08 complete source fingerprint & safer APK publication

- Identified NW-07's ten-file SHA-256 source fingerprint as insufficient to detect simultaneous changes in other Kotlin classes, XML resources, unit tests or build scripts during a Windows Gradle build.
- Introduced a source-tree enumerator for app source/resources/tests, mandatory build configuration, and tooling, excluding generated artifacts and user libraries.
- Re-enumerates after Gradle before APK promotion, blocking new/deleted source files and changed hashes; source count and manifest hash are written into the build status.
- Registered NW-08 source enumeration in the Windows syntax preflight and GitHub Actions.
- Added independent Python negative fixtures for changed, added, deleted and missing configuration source scenarios; an independently reproduced fixture exercise passed locally outside Android.
- Kept earlier NW-01–NW-07 source/runtime functionality unchanged and preserved APK signer, SHA-256, old APK copy and diagnostic safeguards.
- **Release is still NO-GO:** Hosted GitHub Actions runners have reported no executed steps, and no fresh Windows Gradle/Kotlin test run or safe phone install has been verified. See [NW-08 QA and evidence](docs/NIGHTWAVE_NW08_RELEASE_HARDENING.md).


## 2026-10-22 — NW-07 Build Evidence & Release Gate (development, NO-GO)

- Introduced a dependency-free, **read-only Windows NW-01–NW-06 integration source gate**, including positive/negative self-tests for duplicate imports and missing source markers.
- Registered the new source gate in the Windows PowerShell syntax checker and local build entrypoint; incomplete old/merged source ZIPs are rejected before Gradle setup.
- Added `dist/NightWave-source-fingerprint.txt` with SHA-256 hashes of key source files, and recorded its SHA-256 in the build status alongside APK identity and signer.
- Prevented APK promotion if tracked source or the fingerprint manifest changes during Gradle compilation. Previous APK preservation and failure-first reports remain.
- Included NW-07 source contracts and the PowerShell self-test in the GitHub Actions workflow, preserving every earlier phase verifier and the full JVM/APK/instrumentation-source Gradle gate.
- Observed NW-06 GitHub Actions run `39285086913` complete `failure`, but its one job reported no executed steps and logs were unavailable. This is not evidence that Kotlin compiled or failed.
- **Release blocked** until a complete local/CI build, tested signer compatibility, Android audio/search/Google consent and data-preserving device acceptance. Read [NW-07 QA and release readiness](docs/NIGHTWAVE_NW07_RELEASE_READINESS.md).


## 2026-10-19 — NW-06 Music Discovery & Search Refinement (development)

- Made **Search NightWave** visible as the in-app/keyboard search path; kept an adjacent **YouTube** browser choice for provider fallback.
- Added private, bounded suggestions from saved public-video titles and creator names. Suggestions use only local library metadata and never automatically submit to an external provider.
- Matched public results against both query text and Songs/Videos mode to avoid showing stale results when filters change.
- Distinguished genuinely empty public-provider results from the separate local-library state; retained previous successful results when a same-query retry fails.
- Added ten pure JVM tests for metadata-driven suggestions, strict mode matching, locale normalization and honest provider-empty messaging.
- Enabled an NW-06 source regression gate in Android CI; earlier NW-01–NW-05 branches and functionality remain in the combined source.
- **Testing:** Code is committed, but no fresh full Android Gradle build or live device/provider acceptance is certified. Official YouTube content rights, Spotify-scale catalog licensing and real-time account sync remain out of scope.
- See [NW-06 detailed QA](docs/NIGHTWAVE_NW06_DISCOVERY_SEARCH_QA.md).


## 2026-10-19 — NW-05 integrated playback/queue/seek/EQ stabilization

- Corrected a public-video playback service inconsistency: its same-queue fast path now resolves the originally tapped song URL **before** queue URL deduplication instead of using a shifted index.
- Tied seek bar tap/drag/accessibility commits to the originating song identity. The playback controller now rejects a stale scrub request if the displayed or actual Media3 item has changed.
- Limited Sound Studio's manual band controls to the intersection of Android-supported range and the playback service's accepted ±15 dB envelope. Unsupported ranges are disabled rather than yielding guaranteed-rejected commands.
- Added nine more pure JVM test methods across service queue selection, safe Media3 seek identity and equalizer boundary cases.
- Reconciled GitHub CI for the integration branch and removed a duplicate NW-04 verifier command. Earlier NW-01–NW-04 verification gates remain.
- No app data, library storage format, signing configuration, OAuth scope or package ID change was introduced.
- **Evidence:** Source committed and audited; full Gradle tests, Kotlin compilation, Android UI/audio and Google authorization still need executed verification. See [NW-05 QA](docs/NIGHTWAVE_NW05_INTEGRATION_QA.md).


## 2026-10-19 — NW-04 premium player layout, seek and artwork (unverified)

- Replaced Now Playing's visually disconnected default Material 3 seek thumb with a slim, accessible Canvas timeline with tap/drag/cancel handling, Media3 seeks only on commit and safe duration/fraction mapping.
- Reduced oversized album-art/transport spacing and preserved direct 48dp shuffle/repeat/previous/next hit targets; older playback and lyrics behavior stays wired.
- Collapsed less-used crossfade, listening-level, sleep and offline speed controls behind **Sound & playback**, while Queue/Lyrics remain quickly accessible.
- Improved known small YouTube artwork URL quality requests without rewriting offline files or unrelated sources; falls back to the original image if upgraded content is unavailable.
- Tightened mini-player dimensions while preserving its separate dismiss/restore functionality; compact navigation remains 56dp touch-friendly.
- Corrected singular/plural Queue counts and avoided implying that the currently playing track is upcoming.
- Added eight pure JVM tests for thumbnail URL protection and seek coordinate clamping, plus an NW-04 source gate and full CI branch trigger. Historic source validators were reconciled with intended UI sizes.
- **No fresh full Android build or device/render testing of NW-04 has been confirmed.** Do not uninstall the existing installed app or treat a CI artifact as signer-compatible.
- See [NW-04 QA and deferred acceptance](docs/NIGHTWAVE_NW04_PREMIUM_PLAYER_QA.md).


## 2026-10-19 — NW-03 account and Google Drive backup hub (development)

- Added a dedicated branded Compose **Account & backup** hub on the You tab, preserving local guest identity and personal libraries.
- Distinguished **LOCAL**, **DRIVE READY**, and **BACKED UP** states; Google Drive authorization alone is not falsely shown as a successful verified backup.
- Provided visible Connect, Back up now, Preview restore, Compare & merge, and configurable optional backup controls.
- Masked connected email in the account UI and existing backup detail, and added explicit confirm dialogs before Drive account switching or disconnect.
- Avoided overlapping Compose/native dialogs when starting local library exports, restore previews or library comparisons.
- Added five JVM policy tests and NW-03 source preflight, while preserving earlier NW-01 control and NW-02 Sound Studio changes.
- **Not** a general Google login system, licensed catalog, or live account synchronization. Android builds and Google Play-services device QA remain unverified.
- See [NW-03 QA and acceptance](docs/NIGHTWAVE_NW03_ACCOUNT_HUB_QA.md).


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
