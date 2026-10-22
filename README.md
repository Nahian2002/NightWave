# NightWave

> **NW-14 Long-Queue Continuity — 22 October 2026:** The [latest integrated NightWave development branch](https://github.com/Nahian2002/NightWave/tree/fix/nightwave-v2-nw14-large-queue-window) fixes a public playlist continuity issue: selecting a song beyond the 100-track service limit previously moved it to the beginning and separated it from adjacent tracks. NW-14 keeps a **contiguous, bounded playlist window** so Next and Previous follow the real surrounding songs. Six new JUnit tests were added (11 total in the selection suite), while NW-01–NW-13 changes remain. **Full Android compilation and signer-safe phone acceptance are not yet verified; release NO-GO.** See the [NW-14 report](docs/NIGHTWAVE_NW14_LARGE_QUEUE_CONTINUITY.md).


> **NW-13 Public Queue Navigation — 22 October 2026:** The latest [integrated development branch](https://github.com/Nahian2002/NightWave/tree/fix/nightwave-v2-nw13-public-queue-previous) corrects an actual public playback bug: with Shuffle off, **Previous follows the displayed song order even after tapping/jumping between tracks**. Shuffle-on Previous retraces real playback history; changing Shuffle mode clears stale history. Nine regression tests were added and older tests updated. All NW-01–NW-12 work is retained. **No full passing Android build, signer-safe APK or phone acceptance is verified; release remains NO-GO.** See [NW-13 report](docs/NIGHTWAVE_NW13_PUBLIC_QUEUE_RECOVERY.md).


> **NW-12 Compiler Proof & JUnit Parser Repair — 22 October 2026:** The latest [integrated development branch](https://github.com/Nahian2002/NightWave/tree/fix/nightwave-v2-nw12-test-failure-parser) fixes a JUnit parser that could miss consecutive failing cases, expands source fingerprinting to nested Kotlin build checks, and preserves an **executed local compiler test of six exact GitHub source blobs with 28/28 passing assertions**. All NW-01–NW-11 work remains. **Full Android Gradle compilation, APK signing compatibility and phone acceptance are still unverified; release NO-GO.** Read the [NW-12 evidence report](docs/NIGHTWAVE_NW12_COMPILER_AND_JUNIT_QA.md).


> **NW-11 JVM Failure Evidence — 22 October 2026:** The newest [integrated development branch](https://github.com/Nahian2002/NightWave/tree/fix/nightwave-v2-nw11-offline-compiler-proof) adds a bounded, privacy-reduced report of the **actual current Windows Gradle run's failing JVM tests**. Older generated test reports are cleared before compilation, so a compiler failure cannot be mistaken for a previous run's failed test. All NW-01–NW-10 features remain. **GitHub hosted runners still have not executed build steps; full Android compilation and a signer-safe APK remain unverified. Release NO-GO.** See [NW-11 evidence and deferred tests](docs/NIGHTWAVE_NW11_JVM_TEST_EVIDENCE.md).


> **NW-10 Build Recovery — 22 October 2026:** The newest [integrated development branch](https://github.com/Nahian2002/NightWave/tree/fix/nightwave-v2-nw10-compiler-recovery) adds a genuine standalone Kotlin compiler smoke test and an isolated hosted-runner canary. Both the trivial canary and Android job **failed before executing any steps** in GitHub Actions, showing an unresolved runner/account-level blocker rather than a Kotlin error log. Four hash-matched production Kotlin policies were independently compiled and passed 10 assertions locally, but a **full Android build and signer-safe release remain NO-GO**. See [NW-10 compiler and runner evidence](docs/NIGHTWAVE_NW10_COMPILER_RECOVERY.md).


> **NW-09 Device Reliability Recovery — 22 October 2026:** The latest [integrated development branch](https://github.com/Nahian2002/NightWave/tree/fix/nightwave-v2-nw09-device-reliability-diagnostics) includes NW-01–NW-08 and repairs Google Drive consent lifecycle handling (rotation/recreation, missing Play services and hung authorization requests) plus stale Equalizer status after audio session loss. Previous Shuffle/Now Playing/Search changes remain included. **No full successful Android compile or real Samsung Google/EQ acceptance has been established; release remains NO-GO.** See [NW-09 QA report](docs/NIGHTWAVE_NW09_DEVICE_RELIABILITY.md).


> **NW-08 Full-Source Build Attestation — 22 October 2026:** The [newest integrated NightWave branch](https://github.com/Nahian2002/NightWave/tree/fix/nightwave-v2-nw08-full-source-build-attestation) expands NW-07's ten-file fingerprint to cover all app source/resources, unit and instrumentation test files, key Gradle configuration and build-tool scripts. A build is rejected if any enumerated file changes, appears or disappears during compilation. Earlier NW-01–NW-07 work remains included. **Full Android compilation and real-phone acceptance have NOT passed; release remains NO-GO.** See [NW-08 report](docs/NIGHTWAVE_NW08_RELEASE_HARDENING.md).


> **NW-07 Build & Release Gate — 22 October 2026:** The newest [integrated development branch](https://github.com/Nahian2002/NightWave/tree/fix/nightwave-v2-nw07-build-release-gate) includes NW-01–NW-06 plus a Windows no-false-success source preflight, per-build source fingerprinting, refusal to publish an APK when tracked source changes during Gradle, and an updated GitHub CI gate. **No full passing Android build or Samsung device acceptance has been demonstrated yet; release is NO-GO.** See [NW-07 release evidence and safe test plan](docs/NIGHTWAVE_NW07_RELEASE_READINESS.md). Use `build-windows.cmd` only with the complete current source ZIP and keep the installed app until a signer-compatible candidate passes.


> **NW-06 Music Discovery & Search — 19 October 2026, development branch:** The [latest source](https://github.com/Nahian2002/NightWave/tree/feature/nightwave-v2-nw06-discovery-search) builds on NW-01–NW-05 with private suggestions from your saved songs and creators, a prominent in-app public-video search action, clear official YouTube fallback, and query/mode-safe result states. Public search still depends on independent providers; **a complete Android build and real-network device acceptance are not yet verified**. See [NW-06 QA and limits](docs/NIGHTWAVE_NW06_DISCOVERY_SEARCH_QA.md).


> **NW-05 Integration Stabilization — development source (19 October 2026):** The latest [NW-05 branch](https://github.com/Nahian2002/NightWave/tree/fix/nightwave-v2-nw05-integration-stability) keeps NW-01 Shuffle, NW-02 Sound Studio, NW-03 Account & Backup, and NW-04 Premium Player, while fixing cross-feature queue-selection drift, stale song seeking and manual-EQ bounds. [Read NW-05 verification and acceptance](docs/NIGHTWAVE_NW05_INTEGRATION_QA.md). **No successful complete Android Gradle build or real-device verification of NW-05 is confirmed yet.** Do not replace an installed APK without a passing build and compatible signing certificate.


> **NW-04 Premium Player — development source:** The [latest integrated branch](https://github.com/Nahian2002/NightWave/tree/feature/nightwave-v2-nw04-premium-player) improves the Now Playing timeline, reduces oversized player and navigation chrome, cautiously requests higher-quality public YouTube thumbnails, and makes advanced audio settings collapsible. NW-01 Shuffle, NW-02 Sound Studio and NW-03 Account & Backup Hub remain included. **Android compilation and phone acceptance are still pending.** Read the [NW-04 test scope](docs/NIGHTWAVE_NW04_PREMIUM_PLAYER_QA.md) before considering a device update.


> **NW-03 Account & Backup Hub — development source:** The latest [account experience branch](https://github.com/Nahian2002/NightWave/tree/feature/nightwave-v2-nw03-account-backup-hub) adds a dedicated You-page account/backup screen, explicit local-vs-Drive backup status, masked account display, safe disconnect/switch confirmations and backup/restore controls. The NW-01 Shuffle and NW-02 Sound Studio changes remain included. **Google Drive consent is not a full Google sign-in identity; fresh Android compilation and device verification remain pending.** See [NW-03 QA](docs/NIGHTWAVE_NW03_ACCOUNT_HUB_QA.md).


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
