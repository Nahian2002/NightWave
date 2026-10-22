# NightWave NW19 — Audio correctness and offline file integrity

**Date:** 23 October 2026. **Parent:** NW18 reliable direct-audio downloader. **Release decision: DRAFT / NO-GO for device installation until Android compilation, signing and phone QA.**

## Root cause fixed: MP3 without an ID3 tag could be mislabeled AAC

NW18 used `0xFF` and a high-nibble check to identify AAC ADTS. An MPEG Layer III audio frame can also start with `FF FB`, satisfying the old high-nibble check. Since format detection checked AAC before raw MP3, some genuine MP3 streams could be incorrectly classified as AAC and rejected.

NW19 delegates raw-frame validation to `AudioFrameSignature.kt`:
- MPEG Layer III checks MPEG version, non-reserved Layer, bitrate index and sample-frequency index, rather than only sync bits.
- AAC ADTS checks the 12-bit sync word, required Layer 00, sample-frequency index and plausible frame-header length.
- A raw MP3 frame can no longer also satisfy the AAC signature rule. ID3-prefixed MP3, FLAC, WAV, M4A, OGG, WebM and existing MIME guards remain supported.

## More exact offline import behavior

- Before copying fully downloaded staged audio into the private offline library, NightWave computes its streaming SHA-256.
- After the staged file is copied, NightWave compares the private pending copy's SHA-256 with the original before the atomic rename and index commit. Mere file-size equality is not considered byte integrity.
- If the same complete bytes and MIME format are already present, an existing healthy Offline Library entry is reused; it does not consume another song/index/storage slot.
- Same size but different audio bytes remain distinct songs.
- Failure/cancellation still must leave the old library and original user-imported files intact. Existing NW18 HTTPS host checks, bounded redirection/retries and Android MediaExtractor validation remain unchanged.
- Download UI now reports the audio as **ready**, not incorrectly claiming a new file was always saved.

## Evidence

1. On the standalone Linux JVM, the **exact GitHub SHA-matched `AudioFrameSignature.kt`** compiled using `kotlinc-jvm 1.9.0` / OpenJDK 21.
2. **Six of six** constructed MPEG/AAC cases passed: raw MP3, AAC ADTS MPEG-4, AAC ADTS MPEG-2, invalid AAC frequency, malformed sync headers and truncated frame.
3. Generated *real* audio using FFmpeg `libmp3lame` with ID3 suppressed and FFmpeg ADTS AAC. The exact compiled signature helper correctly recognized each and rejected cross-format classification.
4. Enumerated 256 possible second header bytes in a focused overlap check: **zero MP3/AAC ambiguous classifications** within the tested sample pattern.
5. Added three JUnit policy regressions (now 19 methods) and two Android instrumentation tests for byte-copy/dedup integrity. They are authored, **not yet executed under Android Gradle or on Samsung**.
6. GitHub readback and source-level checks are separate from executable/phone QA; the prior GitHub Actions runner failed before any steps, so it supplies no Android build confirmation.

## What 100% cannot mean

A universal or guaranteed 100% success rate for **every online song** is not possible. Songs can be inaccessible, unlicensed for offline saving, geo-restricted, DRM-protected, stream-only, oversized or encoded in a format Android cannot play. This implementation deliberately accepts only authorized direct HTTPS audio downloads and preserves the true audio format. It does not rip arbitrary YouTube video audio or transcode to MP3.

Even with header checks and SHA-256 local-copy matching, a source server might provide an incorrect or truncated audio file, particularly without a reliable content-length or authoritative source checksum. Android's MediaExtractor track checks are valuable, but they cannot prove an entire song is correct or complete. Never represent these checks as 100% certainty.

## Release acceptance

- Run `build-windows.cmd` on the complete NW19 branch source. Require all unit tests, Android test-source compilation, signed APK verification and compatibility with the existing installed NightWave certificate.
- Test a *real authorized MP3 without ID3*, a real AAC ADTS track, and MP3/M4A/FLAC/OGG plus a direct extensionless file. Check filenames, actual decoded duration and airplane-mode playback.
- Repeat download of the same file: a single Offline Library entry should remain. Two equal-sized but different audio files must both be kept.
- Test fake HTML, video-containing WebM/MP4, truncation, timeout/retry, cancel, malformed redirect and private-network redirect. No fake successes or modified old entries.
- Check original imported songs, Google Drive authorization, YouTube search, background playback, queues and old library entries.

**No installed Android APK has been built or verified in this development turn. Keep the user's currently working installation and data unchanged.**
