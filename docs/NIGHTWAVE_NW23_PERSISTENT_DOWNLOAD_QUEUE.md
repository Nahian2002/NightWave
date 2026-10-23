# NightWave NW23 — persistent licensed-audio download queue

**23 October 2026 — DRAFT / NOT RELEASE CERTIFIED.** Branch: `feature/nightwave-v2-nw23-persistent-download-queue`. Preserves NW22 audit tooling and NW21 source-level download functionality.

## Implemented

1. **WorkManager-owned offline transfers:** The Commons CC0/public-domain download catalog now offers **Queue in background** without pasting any URL. The existing **Download here** foreground option is retained as a fallback. Search, licensing checks, and the existing YouTube online player remain separate.
2. **Persistent identity and duplicate prevention:** Each job has a unique deterministic SHA-256 key derived from the validated media URL and source checksum. `ExistingWorkPolicy.KEEP` refuses duplicate *unfinished* jobs for the same catalog file.
3. **Network constraints/retries:** Only runs with network connectivity. A bounded exponential retry policy handles interrupted network operations; retries restart the file cleanly from byte zero. The existing downloader also handles bounded retries within a run. No misleading claim of HTTP Range/partial-byte resume is made.
4. **Foreground progress and cancellation:** The WorkManager worker creates an Android notification channel and a foreground `dataSync` notification with individual Cancel action; progress is also published to WorkManager's persisted work state. `onStopped()` cancels the active HTTP request. On modern Android, notifications and foreground service starts remain subject to OS limits and permissions; phone QA is mandatory.
5. **Private Downloads screen:** NightWave Offline Library and the catalog link to a native Downloads screen with live queued/running/success/failure/cancelled status, per-job cancel, and a shortcut to saved tracks. The WorkManager queue does not depend on a single Activity instance.
6. **Storage and integrity:** Before downloading, the worker checks free private storage for approximately two audio-sized copies plus a 16 MiB buffer. It rechecks catalog rights, source host, original Commons checksum, and maximum file size; the pre-existing downloader validates original SHA-1/bytes and Android audio-only streams before the SHA-256-checked private-library import. Invalid files cannot be recorded as a successful track.
7. **Manifest safety:** Adds `FOREGROUND_SERVICE_DATA_SYNC`, a private non-exported Downloads Activity, and a merged WorkManager `SystemForegroundService` dataSync declaration. Package ID and existing library format are unchanged.

## Regression files

- `CatalogDownloadQueue.kt`: scheduler, input verification, network constraint and unique-work policy
- `CatalogDownloadWorker.kt`: bounded foreground/background lifecycle, cancellation, disk headroom, progress, retry and byte-integrity
- `CatalogDownloadsActivity.kt`: native persistent queue status and cancel UI
- `CatalogWorkIdentity.kt`: JVM-only unique identity utility
- `CatalogWorkIdentityTest.kt`: JUnit identity tests (added but not yet executed under Android Gradle)
- `tools/kotlin/NW23QueueSmoke.kt` and `tools/nw23_queue_smoke.py`: runnable exact-source Kotlin identity smoke (12 assertions if its compiler gate executes)
- `tools/verify_nw23_download_queue.py`: fail-closed source contract gate, wired into GitHub CI
- Existing `FreeAudioCatalogActivity.kt`, `OfflineLibraryActivity.kt` and `AndroidManifest.xml` modified for navigation

## Explicit limitations

- **Not YouTube-to-MP3:** User-protected or unauthorized YouTube video/audio downloads and DRM bypass are out of scope. This queue accepts eligible original media files in the independently licensed catalog.
- **Not true pause/byte-resume:** A killed or interrupted transfer starts again from zero; the intermediate file is never indexed. Pause, ranged-byte resume and download reordering require a later phase with strict source/ETag support and test coverage.
- **Direct-link download UI still foreground-only:** NW23 background queue is for Commons catalog results; the separate direct HTTPS permissioned-downloader screen still cancels when closed.
- **Not unlimited storage:** 400 songs, 3 GiB total library, and 250 MiB per audio file are preserved. A scalable library requires NW24.
- **No unlimited concurrency guarantee:** WorkManager manages scheduling based on platform constraints and quotas.
- **No actual Android build/device proof:** Repository source and regression tests are present, but full Gradle compile, emulator/instrumentation, notification permission tests, reboot/rotation/cancellation, background-download behavior and Samsung playback remain unverified. The GitHub Actions runner may still fail before any steps. The user requested to defer download/installation and manual testing until later.

## Future acceptance checks — deferred by user

- Build `:app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin` from this exact branch
- Verify WorkManager foreground-service manifest merger and Android 13–16 notification/dataSync behavior
- Queue two distinct eligible recordings; reject duplicate tap; keep task after leaving catalog; cancel from queue and notification; verify no partial song was indexed
- Airplane mode/network loss/reboot/process kill: resume **job state** and restart file transfer cleanly
- Verify successful copied audio plays in offline library and survives app restart; original title/metadata and SHA validated
- Test little storage, corrupt audio, changed source SHA, large track, HTTP 429/503 and clean exponential retries
- Check compatibility with the installed APK signing identity before any upgrade; do not uninstall or clear data
