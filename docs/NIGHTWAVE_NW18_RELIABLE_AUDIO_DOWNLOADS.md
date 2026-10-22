# NightWave NW18 — Reliable authorized audio downloads

**Date:** 23 October 2026. **Base:** NW17 Drive identity + offline search. **Status: DRAFT / NO-GO until full Gradle and Samsung device QA.**

## What is implemented

- For a directly downloadable **HTTPS** audio file, URLs no longer require a literal `.mp3`/`.m4a` suffix. A user may supply a track name without an extension.
- Recognize the actual format by combining MIME type, file signature and Android MediaExtractor validation. The file must contain an audio track and **no video track** before Offline Library indexing.
- Supports MP3, M4A, AAC, FLAC, WAV, OGG, OPUS and WebM, subject to Android codec support. The original audio format is preserved: **no falsely renamed MP3**.
- Manual HTTPS-only redirects: at most four, with all hop URLs validated against banned hosts, user info, IP literals, local names and streaming manifests. OkHttp still disables automatic redirects and rejects private/reserved resolved addresses.
- Up to two clean-start retries on temporary IO errors. Previously received partial bytes are discarded rather than incorrectly concatenated with a new server response.
- Maintain strict 250 MiB file cap, private staging, byte-count verification, cancel/cleanup behavior, atomic Offline Library index updates, and current user data.
- Keeps the original media service, background playback, Google Drive connection changes, and local Search enhancements.

## What is intentionally not offered

- No arbitrary YouTube-to-MP3 ripping, bypassing DRM or regional/login restrictions, scraping private content, or converting M4A/Opus into fake MP3 extensions.
- No guarantee that **every** song is downloadable. Sites without a direct permitted HTTPS audio endpoint, streams/manifests, inaccessible content, unsupported audio codecs and large files are not accepted.
- No server, subscription, payment API or token persistence.

## Test requirements before installing an APK

1. Run the full NW18 branch **build-windows.cmd**, including Gradle/JUnit, instrumented test-source compilation, APK verification and signer-compatibility checks.
2. On Galaxy, use an authorized direct MP3, FLAC, M4A and OGG/Opus sample. Test a no-extension CDN download and a safe HTTPS redirect. Verify each file's real format, filename, duration, offline playback and restart recovery.
3. Reject HTML labeled MP3, wrong MIME, truncated transfer, MP4/WebM containing video, streaming manifest, redirect to HTTP/private network/YouTube and over-250-MiB transfer. Check that failed transfers never create library rows.
4. Disconnect Wi-Fi mid-transfer, verify bounded retry/cancel cleanup, then recheck preexisting offline songs and newly downloaded songs.
5. Regress Drive connect/backup, imported music local search, YouTube discovery/streaming, lock-screen controls and resumed queue playback.

## Verification limitations

The source was updated on GitHub with 16 JUnit policy regression methods and manual integration checks. No complete Android Gradle build, real HTTPS transfer, Android MediaExtractor validation, signed APK compatibility or Samsung test has been run in this session. Do not treat source-only checks as release certification.
## GitHub CI observation

Two NW18 GitHub Actions runs (push `39829926119`, pull request `39829933135`) completed with failure and **zero job steps reported**; GitHub returned no usable job log (BlobNotFound). They never established a Kotlin compiler error or a passing Android build. Source readback showed 13/13 structural checks satisfied, but these checks are not Android/JUnit execution. Keep NO-GO and use the existing local Windows builder to obtain real compile diagnostics.
