# NightWave NW24 — Scalable Offline Library (source phase)

**Date:** 23 October 2026. **Status:** DRAFT / Android release not certified. Branch: `feature/nightwave-v2-nw24-scalable-offline-library`. Parent: NW23 persistent permitted-audio downloads.

## Implemented

- Expanded bounded on-device index from **400 to 5,000 songs** and **3 GiB to 32 GiB aggregate audio**, while retaining the **250 MiB per-song limit** shared with the downloader and Drive uploader. Actual capacity is still limited by free device storage.
- Preserved `nightwave_offline_v1` SharedPreferences, `items` JSON list, `OfflineSong(id,title,mimeType,bytes)`, file extensions, existing private UUID filenames, all playback IDs, session schema and existing Google Drive mapping. **No migration or destructive cleanup**, and no change to Android package/signature.
- Added a strict **3-million-character upper bound** when decoding/encoding the legacy JSON index, avoiding unbounded parsed index memory on corruption.
- Manual Android SAF import now streams to the same private temporary-file flow, computes SHA-256 and reuses an existing track ID when audio length, format and SHA-256 match. Duplicate detection happens *before* the new count/aggregate capacity check, so reimporting an existing track at capacity won't create a copy.
- Stopped the per-comparison reparse of the whole song index during downloaded-audio and Google Drive deduplication: under the shared library lock, scan the already-read collection and safely inspect exact private files.
- Added a free-space headroom gate (16 MiB reserve) for manual imports and an additional check during streaming; oversize/failure deletes only the new temporary file and retains old data.
- Native Offline Library now shows only **60 rows per page** with previous/next/page count controls, resets scroll position when page or sort/search changes, and **debounces typing by 220 ms**.
- Expensive read-only file-health information is cached between page changes and searches and invalidated upon activity resume, successful import and delete. Playback/upload paths still revalidate file existence and metadata.
- Fixed tapping a track near the end of a large library: the bounded playback queue starts from the **selected song**, rather than incorrectly taking the first N tracks regardless of selection.
- Added `OfflineLibraryCapacityTest.kt` with four focused JUnit test cases, `NW24CapacitySmoke.kt` with 24 executable assertions, the standalone exact-production-source compiler runner `tools/nw24_capacity_smoke.py`, and the fail-closed compatibility gate `tools/verify_nw24_library.py`. Wired all into the GitHub Actions branch.

## Executed checks

The exact two files fetched from this commit's GitHub source were reproduced with matching Git blob IDs:
- `OfflineLibraryCapacity.kt`: `bd7857fcf871df6ed29c9b3d29d0876009cd3d52`
- `tools/kotlin/NW24CapacitySmoke.kt`: `1dab03dbe5352e500212634f2d548bee8b4c3ae4`

`kotlinc` compiled these two exact file versions into an isolated JVM jar and `java -jar` returned:

```
PASS: NW24 24 production Kotlin library capacity assertions
LIMIT: Android storage, migration and UI not compiled
```

These assertions cover overflow-safe capacity accounting, zero/negative index values, full cap, 400-track backward-compatible growth, page boundaries and headroom checks. They do **not** prove Android Activity compilation, JSON persistence, real file imports, cloud restore or Samsung UI.

## Remaining blockers / scope

- Full Android Gradle compilation, Android unit/instrumentation tests, APK signature verification, real device checks, file/disk pressure, and large-library performance benchmarks are pending. GitHub Actions has previously failed with **zero executable steps** for unrelated hosting/runners; a source-only check cannot substitute for an APK build.
- This remains a **bounded SharedPreferences JSON index**, not a Room-authoritative scalable database. For tens of thousands of tracks, full Room migration would require a separate rigorously verified dual-read/copy/migration-and-rollback plan; no untested migration is introduced here.
- Direct SAF import supports selected audio files only; there is still **no unrestricted YouTube-to-MP3 downloader**. NW23 licensed background queue remains unchanged.
- Duplicate matching uses audio bytes and MIME; tracks with the same composition but different encodings are distinct files. New files are never auto-deleted except their own staging failures.
- No real track loss, device update or data reset has been performed during this source development phase.

**Release rule:** keep the existing installed app; do not uninstall/reinstall, clear data or replace APK until a fully successful build and installed-signer match.
