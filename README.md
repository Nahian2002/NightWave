# NightWave v0.5 — personal streaming music player for Android

An independently developed **Android app source project** for searching public videos and listening to supported audio streams with the phone locked. Built with native Kotlin UI, [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor), and AndroidX Media3.

> **Status:** v0.5 is uploaded as source and has not yet passed an Android APK build or physical-phone regression test. A previous NightWave version built successfully on the owner's Windows PC and passed five S24 Ultra playback checks. Install the new version only after building and testing it.

## NightWave v0.5 — music app upgrade

**Spotify-inspired, not a Spotify clone or Spotify catalog client.** NightWave uses its own identity and streams extraction-supported *public YouTube* videos. It does not integrate Spotify accounts, Spotify recommendations, DRM media, licensed catalog sync or offline downloads.

- **Home / Search / Library** tabs, an editorial welcome panel and music-first track rows.
- **Search by any song name or artist**; Songs and Videos filters, public YouTube fallback, result thumbnails.
- **Liked Songs** saved locally, including restoring favorites after restarting the app.
- **Create personal playlists**, save tracks with ⋮ and remove entries from playlists.
- **Listening history** and a home screen with recently played songs.
- **Up Next queue**, from search results or saved playlists. Add more tracks via ⋮; jump to one from the queue view.
- **Shuffle, repeat off/all/one**, next/previous in the player, plus Bluetooth next/previous interception.
- **Auto-next with the screen locked**: the existing media playback service owns playlist advancement and resolves the next track when playback ends. Stream URLs are resolved just-in-time rather than stored.
- **Persisted queue state**: current index and shuffle/repeat settings are kept on-device so a service restart can restore the list.
- Retains Media3 lock-screen/notification play/pause, seeking, Bluetooth controls and audio-only stream preference.
- No root, login, payment, paid GitHub runner, or broad file storage permission needed.

**Important limitations:** NightWave cannot promise access to every YouTube video or Spotify parity. Extractor failures, ads, copyright, restrictions, region blocks, service changes and background behavior on individual devices can affect playback. Queue recovery restores the list, not guaranteed automatic restart of audio after a force-stop or reboot.

## Rebuild on Windows without GitHub billing

1. Visit the **private** repository \`https://github.com/Nahian2002/NightWave\` and choose **Code → Download ZIP**. Extract into a **new** folder, not the old v0.4 folder.
2. Double-click \`build-windows.cmd\`. It downloads required free toolchain components only on the first run and uses cached downloads later.
3. Wait until \`BUILD SUCCESS\`; copy \`dist\NightWave-debug.apk\` to your Android phone.
4. Because \`applicationId\` and signing setup remain unchanged for the local debug build, it should install over the previous NightWave debug build. If Android reports a signing conflict, **back up anything important before uninstalling** — locally saved favorites and playlists are deleted by uninstall.
5. Open NightWave, tap Search, search a song, open ⋮ on any track to like/add to playlist/add to queue, and try Home and Library.
6. Play a 3–4 song list, lock the screen, leave it playing through multiple tracks, and test next/previous with Bluetooth.

**If a build fails, do not delete the installed version.** Save the terminal output, especially the first \`e:\` Kotlin error or \`FAILURE:\` Gradle section. No APK is verified until a successful build.

## Previous v0.4 search upgrade

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
- Search covers videos but not playlists/channels or a signed-in watch history. No downloads or Spotify login; queues and playlists are included in v0.5.
- YouTube's API policies disallow background playing/separating streams. This prototype uses an unofficial extractor rather than the YouTube API and may conflict with platform terms. It is not intended for distribution on the Play Store without a full legal/policy review.
- v0.5 passed source-level inspections and a standalone Kotlin queue regression suite. A full v0.5 Gradle build, on-device queue auto-advance and locked-screen regression tests are still required.

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

The repository exists at https://github.com/Nahian2002/NightWave. GitHub Actions runs were blocked *before a runner started* by a GitHub account billing restriction. Use `build-windows.cmd` to compile without GitHub billing. The prior APK build and screen-off playback passed on the user's device; new v0.5 library, queue and search still need rebuild and validation.
