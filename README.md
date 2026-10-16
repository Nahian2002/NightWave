# NightWave
### A personal Android music player · v0.7.0

NightWave is an experimental, independent Android music app. Explore public YouTube videos by song or artist name, maintain a local music library, and listen using Android's Media3 background player. **NightWave is not Spotify or YouTube, and it is not affiliated with either company.**

**Release status:** v0.7 source is uploaded, but the new search providers and UI have **not yet been verified on a physical phone**. An earlier version compiled on Windows and successfully played audio with the screen locked. See [TEST_REPORT.md](TEST_REPORT.md).

## Features

- **Discover:** song/artist keyword search, video search, six tappable genres and moods (Bangla, Bollywood, Chill & Lofi, Night Drive, Workout, Acoustic).
- **More resilient search:** use current NewPipe extraction, then attempt documented independent Invidious public search APIs when its search parser returns no results. Results include title, creator, duration and artwork.
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

1. Search \`Baby shark\`, \`saiyara\`, \`Adele Hello\` and \`Bangla songs\`, then try switching **Songs** and **Videos**.
2. Confirm real thumbnails/titles/creators appear. Tap a result and lock the screen to check audio continuity.
3. Use **Home** mood cards; create and rename a playlist; like a song; add one via a full YouTube URL.
4. Queue multiple tracks and verify next/previous, shuffle/repeat and Bluetooth media buttons.
5. Visit **Now Playing** and test artwork, seeking, and the expanded controls.
6. If searching fails, open **Connection details** to see the direct and independent search failures. Optional filtered log: \`adb logcat -d -s NightWaveSearch:D\`.

### Search privacy

Direct search uses the NewPipe Extractor. If it fails, the app may send the **query text** to one of several public [Invidious instances](https://docs.invidious.io/instances/), which are independent servers not controlled by NightWave. Avoid submitting personal or sensitive search strings. NightWave does not send Google login credentials or signed playback URLs to those search instances. Instance availability and privacy practices may vary.

The search fallback uses the Invidious [public search endpoint](https://docs.invidious.io/api/) and locally cached results for up to 10 minutes during the app process lifetime.

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
