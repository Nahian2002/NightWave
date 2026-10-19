# About NightWave

**NightWave** is a personal Android music-player project with a Spotify-inspired experience and no dedicated NightWave streaming server. It combines user-imported offline music, user-saved public streaming references, media-session playback, playlists, local recommendations and optional Google Drive backup.

**Repository:** `Nahian2002/NightWave`  
**Android application ID:** `com.nightwave.player`  
**Development identity:** `2.0.0-alpha5s` / version code `35`  
**Target:** Android 15 (API 35); minimum Android 8.0 (API 26)  
**Code:** Kotlin, Jetpack Compose, Media3, Room/DataStore and Android Gradle.

## Current implementation scope

The latest integrated development source includes offline-imported music, saved public video references, playlists and private library organization, repeat/shuffle, gapless-capable offline queues, local crossfade, optional non-boosting volume balancing, Sleep Timer, EQ, user-imported LRC lyrics and editor, private daily mixes, optional listening memory, launcher playback widget and a development-stage Android Auto browsable library.

The app does **not** include Spotify's licensed music catalog. A saved YouTube page reference is **not** the same as an offline MP3; playable public sources depend on outside service availability and the existing extractor. Android Auto browse/play and some new audio controls require real-device acceptance before release. Google Drive features rely on Google services but do not require a NightWave-hosted backend.

## Development / release notice

Latest compiler recovery: [Phase 6X8C3](https://github.com/Nahian2002/NightWave/tree/fix/nightwave-v2-phase6x8c3-compiler-and-repo-audit). The previous Windows run of 6X8C2 compiled Android resources but failed `:app:compileDebugKotlin` on two proven source errors; 6X8C3 corrects those expressions. **No verified successful Android build exists for this branch yet.**

Do not uninstall the existing NightWave or clear app data to solve APK signing errors. Check [build instructions](docs/BUILD_AND_RELEASE.md), [verification status](docs/VERIFICATION_MATRIX.md), [changelog](CHANGELOG.md), and the [project README](README.md).

## Copy-ready GitHub sidebar About

Suggested description (under 160 characters):

> NightWave — a Kotlin Android music player for offline audio, saved streaming tracks, playlists, lyrics, local mixes and Media3 playback.

Suggested topics:

`android`, `kotlin`, `jetpack-compose`, `media3`, `music-player`, `offline-music`, `lyrics`, `android-auto`

GitHub's repository sidebar **About description and topics** are repository settings, separate from these tracked files. A maintainer with repository settings access must apply them in GitHub → repository → About → edit. This document does not claim they were changed remotely.
