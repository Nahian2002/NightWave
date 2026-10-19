# NightWave

> **NW-02 Sound Studio development — 19 October 2026:** The next [Spotify-style refinement branch](https://github.com/Nahian2002/NightWave/tree/feature/nightwave-v2-nw02-sound-studio) replaces the old equalizer dialogs with an in-app Compose Sound Studio. It adds device-reported frequency response, manual bands, audio-session diagnostics and fixes ignored preset changes on existing sessions. [NW-02 scope and acceptance](docs/NIGHTWAVE_NW02_SOUND_STUDIO_QA.md). The previous NW-01 Shuffle and Google Drive changes remain included. **Full Android compilation and device testing of NW-02 remain pending; this is development source, not a validated APK.**


> **NW-01 field-recovery candidate — 19 October 2026:** Following real-device reports of delayed public-video Shuffle, non-working/unavailable EQ, and no visible Google authorization, a separate [NW-01 repair branch](https://github.com/Nahian2002/NightWave/tree/fix/nightwave-v2-nw01-shuffle-eq-google-recovery) adds immediate acknowledged Shuffle requests, detailed EQ diagnostics, and a visible optional Google Drive connection entry. **This is not a verified APK**: the Android build and on-device recheck remain pending. Review [exact test instructions](docs/NIGHTWAVE_NW01_FIELD_FAILURES_QA.md). The earlier `6960dc5` archive remains unchanged.


**A personal Android music player focused on private libraries, expressive playback controls and a clean, Spotify-inspired interface.**

NightWave brings together user-imported offline audio, saved references to public videos, playlists, local listening tools, synchronized lyrics and optional Google Drive integration—without requiring a dedicated NightWave-hosted server.

> **Release status · 19 October 2026:** This is a development branch, **not a verified production release**. In the most recent user-supplied Windows build, Android debug compilation and instrumentation-test source compilation completed, but **1 of 771 JVM tests failed** (`OfflinePlaybackQueueTest.jumpHandlesBoundsWithoutDamagingSelection`). The queue implementation and regression tests have since been corrected in source. **A fresh complete build and device acceptance are still pending.** Do not treat an existing/CI APK as verified for installation.

## At a glance

| Project detail | Value |
| --- | --- |
| Platform | Android 8.0+ (API 26); target SDK 35 |
| Current development build | `2.0.0-alpha5s` · version code `35` |
| Application ID | `com.nightwave.player` |
| UI and application | Kotlin, Jetpack Compose, Android Jetpack |
| Playback | AndroidX Media3 / ExoPlayer media service |
| Local data | App-private storage, Room/DataStore and existing legacy library |
| Optional integrations | NewPipe Extractor/public search endpoints; Google Drive via Google services |
| Backend | No dedicated NightWave-hosted backend |
| Branch for the latest audited source | [6X8C4 build-recovery branch](https://github.com/Nahian2002/NightWave/tree/fix/nightwave-v2-phase6x8c4-daily-mix-unit-test-compile) |

**Branch note:** The repository's default `main` branch is not the current integrated development snapshot. Select the linked development branch before downloading or reviewing its source.

## Implemented in the development source

These features have source implementations and varying degrees of automated test coverage. Their presence here is **not** a promise that every Android device scenario has passed.

| Area | Current scope | Important limitations |
| --- | --- | --- |
| Music library | Import personally owned local audio, favorites, recent tracks, playlists and private library organization | Offline files must exist locally; saving a web link does not download audio |
| Playback | Media3 media session, background/lock-screen controls, queue navigation, repeat/shuffle, resuming checkpoints, Sleep Timer | Audio focus, Bluetooth and process-recovery scenarios need device testing |
| Advanced audio | Local gapless-capable timeline, bounded crossfade, optional non-boosting normalization, listening controls and EQ | Results vary with device/audio routes; controls are not yet release-qualified |
| Discovery | Search public-video references and browse existing saved tracks and creators | Public endpoints can fail; no licensed commercial music catalog |
| Recommendations | On-device Daily Mix 1–3 from saved/recent tracks, with optional consented listening-memory signals | No fabricated artist catalog, streaming recommendation backend or inferred music rights |
| Lyrics | Import, review, edit, follow and export user-supplied synchronized LRC, with timing adjustment | No guaranteed online real-time lyrics retrieval |
| Optional cloud | Google sign-in/Drive library backup and personal audio locker workflows with preview/recovery safeguards | Uses Google's services and account authorization; test sign-in, quotas and restoration before relying on backups |
| System integration | Home-screen playback widget, Media3 notification/transport controls, development-stage Android Auto browsing | Android Auto host/device acceptance still pending |

### Outside the present scope

NightWave is not Spotify, is not affiliated with Spotify/Google/YouTube/NewPipe, does not supply a licensed subscription catalog, and does not guarantee that a public video remains playable. It does **not** offer an unrestricted YouTube-to-MP3 downloader. User-imported local audio and saved online references are intentionally separate.

## Build and verify on Windows

1. Choose **Code → Download ZIP** while viewing the [current build-recovery branch](https://github.com/Nahian2002/NightWave/tree/fix/nightwave-v2-phase6x8c4-daily-mix-unit-test-compile), and extract the complete archive to a new, short directory.
2. Run `build-windows.cmd` from that directory. The local builder performs source checks, then executes `:app:testDebugUnitTest`, `:app:assembleDebug` and `:app:compileDebugAndroidTestKotlin`.
3. Require a **fresh** `BUILD SUCCESSFUL` and inspect `dist/NightWave-build-status.txt`. On failure, retain the `dist/NightWave-build-errors.txt`, `dist/NightWave-build-diagnostic.txt` and `dist/NightWave-build-full.log` outputs.
4. Compare the new APK's signing identity with your currently installed NightWave before attempting an in-place update. **Never uninstall or clear app data to work around a certificate mismatch.**
5. Complete the on-device acceptance tests in [Build & Release](docs/BUILD_AND_RELEASE.md) before treating the update as validated.

An assembled APK does not by itself mean the overall build passed. GitHub Actions artifacts may have different debug signing certificates and must not be installed over an existing data-bearing app without compatibility checks.

## Verification and security principles

- **Verified evidence is separated from implemented code.** The latest supplied test output failed one queue test; the corrective source has been committed, but a fresh complete result has not been supplied.
- **User data takes priority.** Imported music, playlists, app-private lyrics and recovery checkpoints should not be erased by an APK update or build troubleshooting.
- **No silent backup restore.** Cloud recovery needs explicit user initiation, validation and preview.
- **No bypass claims.** Public-stream extraction is best effort; unsupported, restricted or DRM-protected sources are not promised to work.
- **Privacy by design.** Saved lyrics, on-device listening analysis and imported audio are not described as a NightWave-hosted cloud catalog.

See the [Verification Matrix](docs/VERIFICATION_MATRIX.md) for tested, source-reviewed and outstanding areas.

## Repository guide

| Location | Purpose |
| --- | --- |
| `app/src/main/java/com/nightwave/player/playback/` | Media3 service, remote transport and online queue |
| `app/src/main/java/com/nightwave/player/offline/` | Imported audio, offline queue and continuity |
| `app/src/main/java/com/nightwave/player/v2/` | Compose interface, local mixes, lyrics, cloud, library and optional integrations |
| `app/src/test/` | JVM policy/regression tests |
| `app/src/androidTest/` | Android instrumentation tests (compiled by the build; not automatically run on a phone) |
| `tools/` | Windows builder and source/CI verification utilities |
| `docs/` | Release instructions, test evidence and phase-specific QA notes |

**Project documents:** [About](ABOUT.md) · [Changelog](CHANGELOG.md) · [Build & Release](docs/BUILD_AND_RELEASE.md) · [Verification Matrix](docs/VERIFICATION_MATRIX.md) · [Final Source Audit](docs/FINAL_SOURCE_AUDIT_20261019.md) · [Android Acceptance Checklist](docs/NIGHTWAVE_FINAL_ACCEPTANCE_20261019.md) · [Historical development notes](docs/README_LEGACY_PHASE_NOTES.md).

## Licensing and upstream projects

Refer to [LICENSE](LICENSE) and [NOTICE](NOTICE) for licensing and upstream acknowledgements, including NewPipeExtractor and its redistribution obligations. Distribution requires the maintainer to review dependency and license compliance; an experimental build is not a release declaration.

---

*NightWave is an independent personal project in active development.*
