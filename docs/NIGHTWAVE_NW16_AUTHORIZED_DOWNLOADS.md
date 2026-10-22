# NightWave NW-16 — Offline Direct-Audio Downloader

**Date:** 22 October 2026  
**Parent:** NW-15D `f78edaa57d06736b778639f0e3476addd0d7bf16`  
**Branch:** `feature/nightwave-v2-nw16-authorized-offline-downloads`  
**Release status:** **NO-GO** until a complete Android/Gradle build, signed APK and on-device tests pass.

## Device feedback now accepted

- **Working on user's phone:** audio file import, locked-screen/background playback, YouTube search, and most basic navigation/playback controls.
- **Known failures:** Google account chooser opens but no successful Drive connection; local library/search relevance is unsatisfactory.
- **Requested next:** offline music download is the highest priority. Real-time synchronized lyrics can wait.

## What NW-16 actually implements

NightWave gains a real, foreground **direct HTTPS audio-file downloader**, visible through **Offline Library -> Download permitted audio**. User pastes an HTTPS URL pointing directly at a music file, optionally chooses a filename, confirms they have permission to download it, and starts the transfer.

- Supports directly served `MP3, M4A, AAC, FLAC, WAV, OGG, OPUS, WebM` compatible audio.
- Shows transfer progress, size when known, failure messages and a cancellation control.
- Blocks HTTP, local/unsafe DNS addresses, IP literals, credentials in the URL, malformed links, redirects, protected streaming pages and YouTube video / Googlevideo endpoints.
- Validates status 200, content MIME, plausible audio file signature, and 250 MiB per-song cap; does not save HTML error pages as MP3s.
- Streams into a UUID-named pending file inside the application's private directory. Only after the complete file is checked does it copy into `OfflineAudioLibrary` and atomically commit the existing library index.
- Cancelling or leaving the screen cleans up **only the in-progress temporary file**, not saved songs.
- Does **not** require a NightWave server, Google backup connection or a paid API.
- Does not change the original app package, minSdk, signer, current playlists, local imported songs, Google account state or remote cloud files.

## Important limits

1. **This is not a YouTube-to-MP3 extractor.** YouTube's API developer policies prohibit apps from offering downloading, offline storage or audio separation from YouTube audiovisual content without the necessary prior approval. Existing YouTube online playback remains unchanged.
2. It downloads audio in its **original format**. An MP3 link creates an offline MP3, but an M4A link remains M4A; it does **not** transcode.
3. It is a foreground first implementation. Keep the download screen open. Exiting or Activity destruction cancels the transfer. Resume after process death, batch downloads, background WorkManager notification and storage quota customization belong to later phases.
4. The existing Offline Library has a **400-song index cap**, **250 MiB/song**, **3 GiB total**. These existing limits still apply.
5. The source must be a direct **HTTPS audio file**, not a download landing page, account-login page, YouTube watch URL, playlist manifest or site requiring cookies.
6. Neither user-supplied checkboxes nor MIME/signature checks establish music licensing. User must have the rights needed to save the file.

## Test evidence collected

- **10/10 actual JVM assertions passed** with `kotlinc-jvm 1.9.0` on OpenJDK 21 using the exact original `PermittedAudioDownloadPolicy.kt` source retrieved from GitHub.
- The compiled local file's Git blob SHA-1 was `c2c4c7e952d793f3b0bee99e193da3326e338ac6`, identical to the connected GitHub file blob.
- 12 more JUnit regression methods were added under `PermittedAudioDownloadPolicyTest.kt` but their Gradle execution is **not** demonstrated yet.
- 20/20 targeted GitHub source integration checks passed, including manifest, private activity, URL validation, existing Offline Library, temporary import behavior and CI integration.
- **NOT VERIFIED:** Kotlin/Compose Android build, the Android network downloader, cancellation during real HTTP transfer, rendered Android download UI, signed APK or Samsung offline playback.
- GitHub Actions has previously reported failed jobs with zero executed steps; it cannot substitute for local Gradle evidence.

## Google Drive and local search remain separate

The user's Google account chooser opens and returns without a connected state. Current code obtains `drive.appdata` authorization but also expects the authorization response to provide an account email. Google's Android authorization documentation states that obtaining a Drive token **does not itself provide account identity**. Proper account identity (Credential Manager or explicit supported identity flow) and OAuth client package/signing configuration need a separate fix. Do not silently count the existing chooser as successful Google backup.

Current `LocalSearchSuggestions` derives suggestions only from saved video references; offline imported files are not reliably represented in that suggestion index. This is a relevant NW-17 search-integration target.

## Deferred Samsung acceptance checklist

After a **complete passing** Windows build on this exact source ZIP:

1. Confirm the APK signer matches the installed app using NightWave's existing read-only device compatibility checker. Never uninstall or clear the user's library to bypass an incompatible signer.
2. Open Offline Library, select **Download permitted audio**, paste a direct HTTPS MP3 that you own or are allowed to save, confirm authorization, download.
3. Verify the saved song appears in Offline Library and plays in airplane mode after fully closing and reopening NightWave.
4. Try a fake HTML URL with .mp3 filename, an invalid content type, `youtube.com/watch`, an HTTP URL, and an excessively large file: all must be safely rejected without adding damaged library entries.
5. Cancel a partial download and verify no ghost song is added; existing songs stay playable.
6. Repeat with M4A/FLAC, switch songs, lock the phone, use notification playback controls and confirm no app crash.
7. Regress public YouTube search and streaming, imported tracks, Shuffle, Previous/Next, Sleep Timer, lyrics and Google-screen cancellation.

**Release decision: NO-GO.** An actual on-device downloader is authored, but the full Android build and runtime acceptance are still pending.
