# NightWave NW21 — In-app offline download and responsive UI hardening

**Date:** 23 October 2026. **Base:** NW20 licensed-audio catalog + NW19 integrity + NW17 Google/local search. **Current state:** DRAFT / NO-GO for APK replacement until native compile, signing check and device QA.

## What this phase actually delivers

- Main NightWave **Search** header now has an accessible 48dp "Find music to save offline" action. It forwards the current search text into the separate downloadable-audio catalog.
- Existing **song options** (Home/Search/Library) include **Find free downloadable recording**, opening that title in the catalog. This is a search for an independently licensed recording, **not** a download of the YouTube audio from the selected video.
- Offline Library promotes **Find music to save offline** before external import. The separate permitted HTTPS direct-link downloader remains available.
- The free audio catalog can **Save Offline → Play saved song**, using the registered app-private ID and the existing Media3 offline playback service. The Offline Library shortcut is preserved.
- Download results have responsive layout capped at 760dp on wide screens, >=52dp result actions, padded gradient surfaces, subtle card fade-in, actual KB/MB formatting, platform Search keyboard action, and explicit license/source labels.
- Three native offline screens receive shared gesture-nav, system bar, display-cutout and keyboard/IME safe-area padding. Manifest uses adjustResize. Search header avoids clipping when titles wrap; all four bottom tabs use improved label typography; Home hero grows at large Android font scale.

## Integrity beyond NW19

Wikimedia Commons exposes a base-36 SHA-1 original file hash (31 lowercase digits), and byte size, through imageinfo. NW21 now requests both metadata fields, **excludes any catalog result without an accepted original-file hash**, and verifies the fully downloaded private staging file's exact bytes and streaming source SHA-1 **before** Android MediaExtractor checks or any Offline Library registration. NW19 still independently compares app-private copied bytes with SHA-256. An unexpected server file update or corrupt transfer is rejected, never silently indexed.

This checksum confirms equality with the original file advertised by Commons **at search time**; it does **not** prove that the source license metadata is infallible or that every part of a file decodes perfectly. The 400-song/3GiB library and 250MiB/song caps remain.

## Tests actually performed

- Compiled the **exact GitHub blob** `FreeAudioIntegrity.kt` with the local Kotlin compiler; Git blob SHA `a2c955078f41f27ef1243b6a3149ee8ba1edc8c1` matched the local file.
- Streaming SHA-1 base36 test on actual FFmpeg-generated MP3 and AAC fixtures, plus a binary fixture. **13/13** hash/negative-match checks passed.
- Previously verified NW20 permitted-audio policy JVM smoke test: **11/11** policy assertions passed. The NW19 separate audio frame signature checks had passed in the earlier phase; those production files were preserved.
- GitHub source readback verified feature wiring, native view insets, checksum metadata, offline playback action, menu and Search route. Verified `PlaybackService.kt`, `OfflineAudioLibrary.kt`, `YouTubeGateway.kt` and `NightWaveDriveClient.kt` are byte-for-byte unchanged from NW20.
- Added JUnit tests covering checksum match/mismatch and catalog filtering, plus an instrumented compact-phone Compose Search header gate. **Not yet executed** under Gradle or device instrumentation.
- GitHub Actions failed at the runner/job setup stage with **zero executable job steps**, including run `39850017221`. This **does not prove an Android compiler failure or success**.

## Device release gate (must all pass)

1. Run the branch's complete `build-windows.cmd`. Require all `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:compileDebugAndroidTestKotlin`, source checks and Android build diagnostics to be green.
2. Check Android package ID, APK signing identity, versionCode and data preservation against the currently installed NightWave. **Never uninstall or clear app data to work around signing errors.**
3. Galaxy S24 Ultra: check Home, Search, Library and You at 320/360/411dp-equivalent widths where possible, Android font scaling 100–150%, portrait/landscape, keyboard open, gesture navigation, cutouts and 48dp touch surfaces.
4. Search for a Commons CC0 or public-domain audio file, confirm the metadata/source, tap Save Offline, watch progress, tap Play saved song, reboot the phone and play in airplane mode. Confirm genuine byte size and source SHA-1.
5. Simulate HTTP failure, damaged file, wrong MIME, fake video-containing audio, tampered SHA-1, changed server file, oversized audio, cancellation, source redirection and duplicate content; no partial track may appear as a successful save.
6. Regress previous YouTube online playback/background lock-screen play, Google account connect/backup, imported-song local search, queues, resume, mini-player, album art, lyrics and existing offline library entries.

## Limits and APK size

This app remains serverless and does not add FFmpeg or media-conversion binaries, so there's no reason to inflate the APK to 400–500MB. Keeping actual MP3/M4A/Opus/FLAC files avoids quality loss from transcoding. Mainstream copyrighted YouTube songs remain **not universally downloadable**. No DRM, account-access, region or protected-content bypass was added. The in-app catalog is an alternative searchable source for CC0/public-domain audio, not an authorized YouTube-to-MP3 downloader.

**No fully validated APK or phone-installable release is produced by this branch.**
