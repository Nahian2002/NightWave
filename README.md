# NightWave

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
