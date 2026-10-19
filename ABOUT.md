# About NightWave

**NightWave** is an independent Android music-player project built around the user's own offline audio library, saved public music/video links, playlists, on-device recommendations, synced LRC lyrics and polished media controls. The interface is Spotify-inspired, but NightWave does **not** claim Spotify's catalog, subscription service, licenses or affiliation.

**Repository:** `Nahian2002/NightWave`  
**Active audited branch:** [Build-recovery source](https://github.com/Nahian2002/NightWave/tree/fix/nightwave-v2-phase6x8c4-daily-mix-unit-test-compile)  
**Application ID:** `com.nightwave.player`  
**Development version:** `2.0.0-alpha5s` (version code `35`)  
**Minimum Android:** API 26 · **target:** API 35  
**Language and technology:** Kotlin, Jetpack Compose, AndroidX Media3/ExoPlayer, Room/DataStore, Gradle.

## Capabilities in the development source

- Personal offline audio import, app-private library, playlists, favorites, recent tracks, folders and smart collections.
- Media3 background playback, notification controls, queues, repeat, shuffle, resume, widget and a development-stage Android Auto media browser.
- Gapless-capable local queue, adjustable crossfade, opt-in volume balancing, EQ and Sleep Timer.
- Saved references to supported public video streams, public-source music search and local creator discovery.
- On-device Daily Mixes from existing saved/recent tracks; optional consented local listening memory.
- User-provided synchronized LRC import, playback, editor, timing adjustments and export.
- Guest profile and optional Google services for library backup and a personal audio locker.

## Non-goals and dependencies

There is **no dedicated NightWave streaming server**. Public video playback depends on extractor and third-party endpoint availability; saving a link is **not** importing an MP3. There is no licensed subscription catalog and no guaranteed online lyrics extraction. Google Drive needs a user-approved account and Google's infrastructure. No DRM or access-restriction bypass is promised.

## Engineering status — 19 October 2026

The latest complete user-provided Windows build **assembled the debug APK** and compiled main Android and Android-test Kotlin, but **failed 1 of 771 JVM tests** (an offline queue jump/Previous boundary bug). The source has been patched; additional repeat, shuffle and URL-deduplication regressions have been written. Kotlin warning cleanup and source verification wiring have also been committed.

**The final audited branch has not yet produced a confirmed passing full Gradle build, compatible APK signer report or complete on-phone feature acceptance.** It is a development candidate, not a verified release.

Read [README](README.md), [Changelog](CHANGELOG.md), [Verification Matrix](docs/VERIFICATION_MATRIX.md), [Build Guide](docs/BUILD_AND_RELEASE.md), and [Final Acceptance Checklist](docs/NIGHTWAVE_FINAL_ACCEPTANCE_20261019.md).

## GitHub repository sidebar metadata

These values are prepared for the repository's **About** settings; editing this tracked Markdown file does not automatically change GitHub's sidebar.

**Suggested short description:**

> NightWave is a Kotlin Android music player for offline audio, saved tracks, LRC lyrics, local mixes, playlists and Media3 playback.

**Suggested topics:** `android`, `kotlin`, `jetpack-compose`, `media3`, `music-player`, `offline-music`, `lyrics`, `android-auto`.

**Suggested website:** Leave blank until there is an official product page. Do not invent a product domain, paid service or release claim.

The maintainer can update the GitHub sidebar from **Repository → About → Edit**. Repository `main` is historically behind the current integrated development branch; do not infer that the sidebar or default branch was changed by this documentation update.
