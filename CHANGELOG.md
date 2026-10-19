# NightWave Changelog

NightWave uses stacked, independently reviewable development branches. This changelog describes **source changes**. A feature marked implemented below is **not necessarily validated by a successful Android build or physical-device test**. Check [verification status](docs/VERIFICATION_MATRIX.md) before installation.

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
