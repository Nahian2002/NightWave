# NightWave

## NightWave 2.0 — Phase 5A stability and crash diagnostics

**Version:** \`2.0.0-alpha5a\` (code 17) · **branch:** \`feature/nightwave-v2-phase5a-stability\` · **not Android-verified yet**.

[Download the full Phase 5A source ZIP](https://github.com/Nahian2002/NightWave/archive/refs/heads/feature/nightwave-v2-phase5a-stability.zip) · [Phase 5A QA and crash guidance](docs/NIGHTWAVE_V2_PHASE5A_STABILITY_QA.md)

This phase addresses the user's recurring Android crash warning **without guessing the actual fatal exception**:
- Media3 no longer resolves the last YouTube stream automatically when Android recreates the service; background work and saved-position writes only happen when music actually plays. Saved track/queue metadata remains available immediately, and explicit Play (including supported Bluetooth play commands) restores the stream.
- Corrupt legacy guest preferences, bookmark fields and offline-file indices no longer have to crash startup or background playback. No original library values are automatically deleted.
- Room/DataStore mirroring runs on screen resume or explicit storage checks, not on every playback state update.
- A privacy-limited on-device crash summary stores up to five exception classes and NightWave-only stack frames, retaining Android's normal crash reporting. Open classic Home → **Diagnostics · Share recent crash summary** to share it voluntarily, or use \`capture-nightwave-crash.cmd\` with ADB.
- The previous compact NightWave design, offline audio imports, YouTube search, existing playlists/favorites, backup format and current launcher remain preserved.

**Release warning:** Android Gradle compilation, new device tests and the user's S24 Ultra crash reproduction have **not** been run here. Extract the source ZIP in a fresh folder and run \`build-windows.cmd\`; install the resulting APK **only after BUILD SUCCESS**, without uninstalling or clearing app data. If the build fails, send \`dist/NightWave-build-errors.txt\`. Do not run destructive instrumentation tests on a phone containing important playlists.

This is a draft stability milestone. Google sign-in, cloud recovery and permanent Room-primary writes are still future phases; the root cause of the repeated-crash notification still requires the phone's error trace.

---


## NightWave Alpha4 Hotfix2 — Premium startup repair + offline audio

**Development branch:** \`feature/nightwave-alpha4-ui-offline-repair\` · **Version:** 2.0.0-alpha4-hotfix2 / code 16.

[Download the complete Hotfix2 source ZIP](https://github.com/Nahian2002/NightWave/archive/refs/heads/feature/nightwave-alpha4-ui-offline-repair.zip) · [Detailed QA and limitations](docs/NIGHTWAVE_ALPHA4_HOTFIX2_QA.md)

Responds to phone screenshots/feedback:
- The header now shows **NightWave** in two-tone typography, without the inert Night Mode badge. Smaller Home/Library cards and compact selected navigation tabs reduce clipping.
- Classic Now Playing is collapsed to its core controls and progress, with a collapse control and swipe-down gesture on the artwork; tap the small Now Playing pill to restore it. Expanded player still contains secondary actions.
- Fixes a Compose ARGB rendering error that could cause the premium preview to fail: \`Color(raw.toULong())\` incorrectly interpreted 32-bit ARGB values as packed Compose color values. It now uses \`Color(raw.toInt())\`. The actual cause of the reported crash still needs confirmation with an Android crash log.
- Adds **Offline library** to Home and Library in the classic app and Library in the premium preview. Import personal/licensed MP3, M4A, AAC, FLAC, WAV, OGG, OPUS, MP4, WebM or 3GP files through the Android picker; NightWave copies these files to private storage and plays them through the Media3 music session without Wi-Fi. Import caps: 250 MiB per file, 3 GiB total, 400 tracks. Files are removed by Android if app data is cleared or NightWave is uninstalled.
- **This is not YouTube download functionality.** NightWave still streams public video audio when online; it does not download videos, bypass restrictions, or save temporary signed audio streams.
- The classic Home remains the launcher until the premium screen is physically tested. Existing likes, playlists, guest settings and Room mirrors are preserved.

**Build/QA:** Source uploaded but APK compilation and real-phone playback are **NOT verified**. Download, extract to a new folder, run \`build-windows.cmd\`. Do not uninstall the current app or clear its data. If build fails, send \`dist/NightWave-build-errors.txt\`. If the premium preview still exits, use \`capture-nightwave-crash.cmd\` and share a redacted Android \`FATAL EXCEPTION\` stack.

---


## Alpha 4 Hotfix 1 — startup recovery (Android crash triage)

**This recovery release has NOT been compiled or run on a phone here.** It is based on Alpha 4 and retains all saved music and playback data.

**[Download the full recovery ZIP](https://github.com/Nahian2002/NightWave/archive/refs/heads/fix/alpha4-launcher-recovery.zip)**

After reports that NightWave Alpha 4 installs but immediately closes on Android, this hotfix restores the previously usable classic Home as the default launcher while preserving the new premium Compose interface as an **opt-in preview** through classic Home → **Explore the new premium interface**. The Media3 session's notification also returns to the classic Activity. The Android application ID and private data locations remain unchanged, so **install the debug APK as an update, without uninstalling or clearing data**.

The root cause of the premium screen's runtime crash is **not identified yet**. Build with `build-windows.cmd`, then install only if `BUILD SUCCESS`. To diagnose the remaining premium crash, enable USB debugging, connect the phone, run `capture-nightwave-crash.cmd`, tap the premium preview until it closes, then collect `NightWave-crash.log` and share the part starting with `FATAL EXCEPTION`. Review the log for personal information first.

Hotfix version: **2.0.0-alpha4-hotfix1** (versionCode 15). Stable `main` is unchanged. See [hotfix QA](docs/NIGHTWAVE_ALPHA4_STARTUP_RECOVERY.md).

---


## NightWave Alpha 4 — Premium UI + last-session recovery

**Current development preview:** \`2.0.0-alpha4\` on \`feature/nightwave-v2-phase4-premium-session\`.

[Download full Phase 4 source ZIP](https://github.com/Nahian2002/NightWave/archive/refs/heads/feature/nightwave-v2-phase4-premium-session.zip) · [Phase 4 QA and release limits](docs/NIGHTWAVE_V2_PHASE4_QA.md)

**Major change:** the premium Jetpack Compose Home/Search/Library/Profile is the default launcher in this branch. The classic player is still available via the top-right Settings icon and still handles Android share-to-NightWave links.

The prior song and queue are restored after app reopen, with the last saved playback position and metadata. Media3 resolves a fresh stream and prepares it **paused**; tapping Play explicitly resumes. Neither authentication nor expiring signed audio stream links are saved. A compact mini-player (~60dp) supports swipe down to hide, swipe up to expand, and a tappable "Now playing" control to show it again. Four compact tabs replace the oversized old navigation; album artwork, title layouts and Profile wording were refined for phone screen ratios.

**Build status:** Source uploaded and reviewed, but the full Android Gradle build and real-phone tests for Alpha 4 are **not verified here**. Download/extract to a new folder, run \`build-windows.cmd\`, and only install the debug APK over your existing installation after \`BUILD SUCCESS\`. Never uninstall or clear app data just to upgrade. On compilation errors send \`dist/NightWave-build-errors.txt\`.

Upstream YouTube search is unchanged and may still fail for some queries; the new UI does not promise a search backend fix. Room-primary writing and Google account/Drive recovery remain separate future milestones.

---


## NightWave 2.0 Alpha 3D — guarded single-writer storage

**Branch:** \`feature/nightwave-v2-phase3d-single-writer\` · **Version:** 2.0.0-alpha3d (code 13) · **Draft, NOT Android verified**

[Download the full Phase 3D GitHub source ZIP](https://github.com/Nahian2002/NightWave/archive/refs/heads/feature/nightwave-v2-phase3d-single-writer.zip) · [Detailed Phase 3D QA and limitations](docs/NIGHTWAVE_V2_PHASE3D_QA.md)

This phase consolidates all local favorites, listening-history and playlist mutations into one guarded, synchronous SharedPreferences writer. Each operation validates its source, persists a recovery snapshot and commits the logical library state together rather than publishing playlist names before songs. Manual backup imports share the same lock. The preview's Profile screen offers **UNDO MOST RECENT LIBRARY EDIT** with explicit confirmation and a fingerprint that refuses to recover over newer changes.

**Room primary writing remains disabled** until migration and rollback are actually verified. The current Room copy and DataStore preferences are still secondary; the legacy player and Android media service are preserved. The source includes 10 new JVM mutation tests and seven Android device regression tests, but no Android build or hardware test has run here.

Extract the source ZIP into a fresh folder and run \`build-windows.cmd\`. **Do not uninstall the working app or clear its data** if compilation fails. Room/device tests require a disposable test profile and a separate \`:app:connectedDebugAndroidTest\`; those tests clear test preferences and must NOT be run against personal playlists.

---


## NightWave 2.0 — Phase 3C verified storage preview

**Current development branch:** \`feature/nightwave-v2-phase3c-verified-read-routing\`  
**Version:** \`2.0.0-alpha3c\` · code 12 · **not an APK verified on Android**

[Download the Phase 3C source ZIP](https://github.com/Nahian2002/NightWave/archive/refs/heads/feature/nightwave-v2-phase3c-verified-read-routing.zip) · [Full QA and safety requirements](docs/NIGHTWAVE_V2_PHASE3C_QA.md)

Phase 3C adds an **optional, session-only Room library read preview** in the Compose Profile: choose **TRY VERIFIED ROOM READS** or **RETURN TO ORIGINAL LIBRARY**. The preview only activates if the copied Room database **exactly matches** the current legacy favorites, recent-history order, playlist order, track metadata and source checkpoint. Every unmatched or stale copy returns to legacy reads without exposing Room rows. Song and playlist **writes still go to the original preferences**. No migration deletes original songs, no account sync is claimed, and a vanished legacy source cannot silently clear a populated Room mirror.

New tests: 10 JVM parity tests, six Room reader Android tests, and one unexpected-source-loss Android test (in addition to previous Phase 3B tests). **None have yet been executed in this environment.** Windows build with \`build-windows.cmd\` remains necessary; Android instrumentation requires a test phone/emulator and a separate \`:app:connectedDebugAndroidTest\` run. Keep a manual guest backup outside the app before installing any alpha release.

**Stable main remains NightWave v0.8**, and the development branches have not been merged into main. Google login, cloud backup and the final Room write cutover are not yet implemented.

---


## NightWave 2.0 Phase 3B — Room + DataStore storage preview

**Branch:** \`feature/nightwave-v2-phase3b-room-datastore\` · **Version:** \`2.0.0-alpha3b\` · **Production main:** unchanged.

- [Download Phase 3B source ZIP](https://github.com/Nahian2002/NightWave/archive/refs/heads/feature/nightwave-v2-phase3b-room-datastore.zip)
- [Storage and test instructions](docs/NIGHTWAVE_V2_PHASE3B_STORAGE_QA.md)

The preview now builds an **isolated Room library copy** and a **DataStore guest-profile copy** when you enter the new interface. It does not delete old preferences or change the Media3 playback owner. Reopening the preview rechecks for changed playlists and guest settings. Profile → Storage upgrade preview shows its status and offers a manual recheck.

Room stores public video metadata, ordered playlists, favorites and locally held recent history, with its migration checkpoint committed in the same SQLite transaction. DataStore receives guest settings through a non-destructive, atomic edit. This is deliberately a **shadow migration**, not the final cutover to Room/DataStore as the canonical writer.

The new branch includes eight JVM snapshot tests, five Android Room instrumented tests and one Android DataStore instrumented test. **None have been executed here**, and the Android/KSP build has not been verified. Build on Windows with \`build-windows.cmd\` before installing; do not uninstall or clear the old app. The separate instrumented tests require a connected phone or emulator and \`:app:connectedDebugAndroidTest\`.

**Not provided yet:** Google sign-in, cloud restore, full dual-write/cutover, and a proven Android APK for this phase.

---


> **NightWave 2.0 Phase 2 — opt-in developer preview**: A new native Compose music player is available on this branch. It adds a Media3-connected mini-player, full-screen artwork player, track queue, shuffle/repeat, usable playlists and on-screen actions. YouTube website song searches now return to the Compose player. The legacy launch screen and playback service remain intact. **This is NOT a verified Android build or stable release yet.**
>
> **Download Phase 2:** [Source ZIP](https://github.com/Nahian2002/NightWave/archive/refs/heads/feature/nightwave-v2-phase2-player-ui.zip) · [Phase 2 QA guide](docs/NIGHTWAVE_V2_PHASE2_QA.md) · [Overall phased plan](docs/NIGHTWAVE_2_PHASES.md)
>
> Extract ZIP to a fresh folder and run \`build-windows.cmd\` without GitHub Actions billing. If the Windows Gradle tests and build succeed, install the new debug APK **over**, not instead of, your previous app. Then open **Home → Explore the new premium interface**. If it fails compilation, keep the old app installed and share the first Kotlin compiler error. Version: **2.0.0-alpha2** / code **9**.


### A personal Android music player · v0.8.0

NightWave is an experimental, independent Android music app. Explore public YouTube videos by song or artist name, maintain a local music library, and listen using Android's Media3 background player. **NightWave is not Spotify or YouTube, and it is not affiliated with either company.**

**Release status:** v0.8 source is uploaded. The new YouTube website search and video-selection workflow still need to be compiled and tested on an Android device; do **not** treat this as a confirmed fix until those tests pass. A previous version's direct-link playback passed on the owner's phone. See [TEST_REPORT.md](TEST_REPORT.md).

## v0.8 — Direct YouTube search without failing public API servers

**Why this changed:** v0.6 native search yielded \`raw=0, streams=0, errors=4\`, and the community Invidious search provider in v0.7 also failed on the owner's phone. More retries against unavailable endpoints won't produce music.

NightWave's **main Search button now opens YouTube's real search-result website in an Android WebView**, using only the song or artist name. Tap a public video to return its link to NightWave's existing media player. The app intercepts standard video URL navigation and YouTube's JavaScript-driven thumbnail clicks; **▶ PLAY** also lets you select the currently opened YouTube video manually if automatic interception fails. An **↗ BROWSER** fallback opens the same query in your browser/YouTube app; choose a video there and use **Share → NightWave** if necessary.

**Native song lists are now clearly labeled experimental** rather than being presented as a working primary search engine. The six Home discovery categories open direct YouTube web searches. The optional API-based native list may still fail because upstream services are unavailable.

The in-app YouTube browser does not expose a privileged JavaScript interface to websites, restricts navigation to known HTTPS YouTube hosts, disables WebView file access and blocks other-site navigation inside the app. Avoid signing into Google within the embedded search browser; use the external-browser button if an account is required.

**Limitations:** YouTube may block pages in embedded WebViews; this has not yet been device-tested. The website browser can show results without an API key, but selecting a video does **not** guarantee playback—NewPipe stream resolution is still needed. Private, restricted, protected, and otherwise unsupported videos may fail. Official YouTube APIs prohibit background playing through their player, and unofficial extraction remains subject to platform changes.

## Features

- **Discover:** song-name search through YouTube's own website, plus six tappable genres and moods (Bangla, Bollywood, Chill & Lofi, Night Drive, Workout, Acoustic).
- **Experimental native results:** optional NewPipe/Invidious-based lists; these may fail and are no longer the default. Website search uses real YouTube results.
- **Library:** Liked Songs, local playlists (create/rename/delete), recent listening history, saved playback queue, and adding a public YouTube link to a playlist.
- **Player:** audio-focused streaming, lock-screen/notification media controls, Bluetooth play/pause/next/previous, seek, shuffle/repeat, immersive Now Playing, and automatic next-song playback from the background service.
- **Visual identity:** Midnight Sessions — navy, violet, aqua, layered cover art and native lightweight motion.
- No root, YouTube/Spotify login, or payment required for this locally built personal prototype.

### Limitations

A result appearing in search **does not guarantee it will play**. Age gates, region restrictions, private/paid videos, DRM, extractor failures, platform changes and network problems can prevent streaming. NightWave does not have Spotify's licensed catalog, algorithms, account sync or offline downloads. The current queue can restore its list after a service restart, but cannot promise seamless resumption after a force-stop or device reboot.

YouTube's API policies prohibit separating audio from video or background playback through the official API. This unofficial approach can conflict with platform terms and may break when YouTube changes its services. Do not redistribute without appropriate policy and license review.

## Download and build for free on Windows

You **do not** need GitHub billing or a payment card. Builds run on your own PC.

1. While signed into the private GitHub repo, select **Code → Download ZIP**, or [download the current main-branch ZIP](https://github.com/Nahian2002/NightWave/archive/refs/heads/main.zip).
2. Extract into a **new folder** rather than overwriting an old extracted version.
3. Double-click **build-windows.cmd**. The script uses cached JDK/Gradle/Android SDK downloads when possible; the first run can take several minutes and several GB of space. Accept Google SDK licenses only if you agree.
4. It runs Android unit tests and assembles a debug APK. Wait for **BUILD SUCCESS**.
5. Copy **dist\NightWave-debug.apk** to your Android device and install it **over the old debug APK**. Do not uninstall or clear the current app if you have saved playlists and favorites.

**If the build fails:** keep the existing phone app and send the first Gradle \`e:\` Kotlin error or \`FAILURE:\` section. The new version has not been verified until that build succeeds.

## Test checklist on your phone

1. Search \`Baby shark\`, \`saiyara\`, or \`Adele Hello\`. The main search arrow should open YouTube's search website **inside NightWave** (not a blank native result page).
2. Confirm YouTube displays matching video thumbnails; tap a video. If it opens in the embedded browser, press **▶ PLAY**. Then lock the screen to check playback.
3. Use **Home** mood cards; create and rename a playlist; like a song; add one via a full YouTube URL.
4. Queue multiple tracks and verify next/previous, shuffle/repeat and Bluetooth media buttons.
5. Visit **Now Playing** and test artwork, seeking, and the expanded controls.
6. If the embedded website does not load, press **↗ BROWSER**, select a YouTube video, and choose **Share → NightWave**. Report what the embedded browser shows. Optional filtered log: \`adb logcat -d -s NightWaveSearch:D\`.

### Search privacy

Direct search uses the NewPipe Extractor. If it fails, the app may send the **query text** to one of several public [Invidious instances](https://docs.invidious.io/instances/), which are independent servers not controlled by NightWave. Avoid submitting personal or sensitive search strings. NightWave does not send Google login credentials or signed playback URLs to those search instances. Instance availability and privacy practices may vary.

The optional native search fallback uses the Invidious [public search endpoint](https://docs.invidious.io/api/) and locally cached results for up to 10 minutes during the app process lifetime.

## Development notes

- Kotlin, Android Gradle Plugin, Media3, NewPipeExtractor, OkHttp, Android local preferences.
- \`app/src/main/java/com/nightwave/player/net/YouTubeGateway.kt\`: main search, ranking, stream resolution and caching.
- \`app/src/main/java/com/nightwave/player/net/CommunityVideoSearch.kt\`: independent search and strict JSON parsing.
- \`app/src/main/java/com/nightwave/player/ui/MainActivity.kt\`: home, search, library, animations, and player controller UI.
- \`app/src/main/java/com/nightwave/player/playback/PlaybackService.kt\`: background MediaSessionService and automatic queue advancement.
- \`app/src/main/java/com/nightwave/player/library/MusicLibrary.kt\`: local favorites, history and playlists.
- \`app/src/test\`: keyword routing, queue tests, search parser and fake-server failover tests.
- \`tools/build-windows.ps1\`: local no-billing Windows build bootstrap.

The current GitHub-hosted Actions runner has an account billing/spending restriction. The local builder avoids those hosted runners. No remote APK build is claimed.

## License and upstream acknowledgement

See [LICENSE](LICENSE) and [NOTICE](NOTICE). This project incorporates NewPipeExtractor, GPL-3.0-or-later. Respect its copyright/license obligations, particularly if distributing binaries. This is an independent, privately developed experimental client and does not claim affiliation with YouTube, Google, NewPipe or Spotify.
