# NightWave — YouTube-style Android audio player (v0.4)

An independently developed **Android app source project** for searching public videos and listening to supported audio streams with the phone locked. Built with native Kotlin UI, [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor), and AndroidX Media3.

> **Status:** The user successfully built the previous NightWave APK on Windows and reported all five real-phone playback checks passed on a Samsung Galaxy S24 Ultra. v0.4 keyword search and new layout are not yet compiled or tested on Android. This is an unofficial YouTube client.

## What's new in v0.4

- **Type any song or artist name** (e.g., `Adele Hello`); full video links are not needed.
- Search bar at the top, familiar large thumbnails, titles, channels and duration labels.
- **Songs** searches YouTube Music song results first; if empty or unavailable, the app retries regular video search.
- **Videos** searches regular public YouTube videos.
- Tap a result to start audio. Pasted video links, screen-off playback, Bluetooth and lock-screen controls are preserved.
- The existing playback service was not changed.

## Included

- Search public YouTube songs and videos by name; tap any supported result to play its audio.
- Paste YouTube, YouTube Music, Shorts, Live and youtu.be video links, including links shared from another Android app.
- Media3 foreground media session: continue streaming with display off, headset connected, or another app open.
- Android lock-screen/notification controls (play/pause) and Bluetooth/media-key controls.
- Seekbar, jump ±15 seconds, artist/title/album artwork, open the source video in YouTube.
- Dark native Android interface; no login, no root, no storage permission, no local content downloads.

## Build on your Windows PC without any GitHub billing (recommended for private repos)

1. Open the private NightWave repository. Click **Code → Download ZIP**, then extract it.
2. Double-click **build-windows.cmd** in the extracted folder. No terminal commands or Android Studio are needed.
3. The first run downloads Java 17 if needed, Gradle 8.11.1, and Google's Android SDK tools. Downloads are free but need internet and several GB of disk space.
4. Review Google's Android SDK license prompts and type **y** if you accept the terms.
5. After tests and compilation succeed, the APK will be at **dist\NightWave-debug.apk**. Copy the APK to your Android phone and tap to install.

The script uses only local Windows resources; it **does not** use GitHub-hosted runners, need any billing settings, or require a payment method. It caches tools in %LOCALAPPDATA%\NightWaveBuild. Download sources are visible in tools/build-windows.ps1.

**v0.4 test:** Download a fresh GitHub ZIP (old extracted folders do not update), run the same `build-windows.cmd`, then install the newly built APK and search a song name. Share the exact error if compilation or live search fails.

## Build an APK with GitHub Actions (only if GitHub permits jobs)

1. Open the existing private NightWave repository and confirm `.github/workflows/android.yml` is present.
2. Under GitHub **Actions**, select **Android debug APK** and click **Run workflow** (or push to `main`).
3. After the workflow finishes, download the **NightWave-debug-apk** artifact and extract `app-debug.apk`.
4. Transfer APK to your Samsung phone. Open the APK and allow installation from the app you used to open it (if Android asks).
5. Launch NightWave, allow notifications, search a video, tap it and lock the screen.

## Build in Android Studio

1. Install Android Studio, JDK 17 and Android SDK 35. Gradle 8.11.1 is used by the included GitHub Actions workflow.
2. Import this **NightWave** folder as an existing Gradle project.
3. Because this archive intentionally does **not** contain a binary Gradle wrapper JAR, install Gradle 8.11.1 and run `gradle :app:assembleDebug` from a terminal, or create a normal Gradle wrapper with `gradle wrapper --gradle-version 8.11.1` before importing if your IDE requires one.
4. APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Architecture

- `net/YouTubeGateway.kt` — discovery and resolving playable public audio stream URLs
- `net/ExtractorDownloader.kt` — OkHttp transport required by the extractor
- `playback/PlaybackService.kt` — ExoPlayer inside `MediaSessionService`, owns background playback
- `ui/MainActivity.kt` — playback controller/UI and search requests (I/O off main thread)
- `core/VideoLink.kt` — URL normalization and strict host matching
- `core/TimeLabel.kt` — clock formatting

## Limitations

- Does not and cannot promise **literally every** YouTube video. Age-gated, private, region-restricted, premium, DRM-protected, or extractor-incompatible videos may fail.
- The app prefers progressive HTTP audio streams, with fallback to combined audio/video and HLS/DASH manifests when available. Some videos still use unsupported formats, and signed stream URLs expire. Re-select the video to refresh an expired URL.
- Search covers videos but not playlists/channels or a signed-in watch history. No queue, downloads or login in v0.4.
- YouTube's API policies disallow background playing/separating streams. This prototype uses an unofficial extractor rather than the YouTube API and may conflict with platform terms. It is not intended for distribution on the Play Store without a full legal/policy review.
- v0.4 passed source-level checks and Kotlin input-routing tests, but **v0.4 has not been compiled or live-tested** in this environment. The user confirmed a previous version compiled and played with the screen off.

## Testing after you build

1. Install and launch. Enter `Adele Hello` or `Shape of You` (NOT a link) into the top search bar. Tap Search and confirm thumbnails appear.
2. Tap a search result and test playback. Switch between Songs and Videos and repeat; paste a `youtu.be` link to check backward compatibility.
3. Play an item, press Home, then lock the screen for several minutes. Test Bluetooth pause/resume and notification controls.
4. Reopen the app; check playback continuity, the seekbar, ±15s and external YouTube link.
5. Confirm that invalid URLs, airplane mode, unsupported videos and empty searches show readable errors and do not crash.
6. If Samsung's battery controls stop music, Settings → Apps → NightWave → Battery → Unrestricted.

## Local source checks (no Android SDK required)

```sh
python3 tools/verify_source.py
kotlinc app/src/main/java/com/nightwave/player/core/{VideoLink,TimeLabel,SearchInputResolver}.kt tools/SmokeTests.kt -include-runtime -d /tmp/nightwave-test.jar
java -jar /tmp/nightwave-test.jar
```

## License and attribution

The implementation is intended as a private experimental app and uses NewPipeExtractor, which is GPL-3.0-or-later. Comply with the extractor's GPL obligations if redistributing derivative binaries/source; its upstream notices are here: https://github.com/TeamNewPipe/NewPipeExtractor. No affiliation with Google or YouTube.

## Upstream references

- JitPack coordinates are case sensitive; the pinned artifact is `com.github.TeamNewPipe:NewPipeExtractor:v0.26.5` per the upstream NewPipe naming convention.
- Android Gradle Plugin 8.9.2 is paired with Gradle 8.11.1 and JDK 17; see https://developer.android.com/build/releases/agp-8-9-0-release-notes
- Android foreground media service setup: https://developer.android.com/media/media3/session/background-playback

## Repository publication status

The repository exists at https://github.com/Nahian2002/NightWave. GitHub Actions runs were blocked *before a runner started* by a GitHub account billing restriction. Use `build-windows.cmd` to compile without GitHub billing. The previous APK build and screen-off playback passed on the user's device; new v0.4 search still needs rebuild and validation.
