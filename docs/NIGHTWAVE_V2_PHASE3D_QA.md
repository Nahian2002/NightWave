# NightWave 2.0 — Phase 3D single-writer guard

**Version:** 2.0.0-alpha3d (code 13)  
**Branch:** feature/nightwave-v2-phase3d-single-writer  
**Release status:** Source committed. Full Android Gradle compilation, JVM tests, Android instrumentation and real-phone regressions are **NOT VERIFIED**.

## What Phase 3D changes

Previously the legacy MusicLibrary used separate SharedPreferences.apply() calls for playlist names and contents, allowing crashes between the two writes. In Phase 3D:

- All MusicLibrary mutations (favorites, history, playlist create/delete/rename, add/remove song) share an **in-process lock** and a pure immutable mutation planner.
- Each logical change publishes the entire validated library in ONE synchronous SharedPreferences commit. It does not mutate existing data if strict legacy snapshot parsing fails.
- An independent pre-write recovery snapshot is committed BEFORE a collection edit. It retains two recent pre-write snapshots for diagnostics.
- Recovery is manually requested via Compose Profile → **UNDO MOST RECENT LIBRARY EDIT**, with a confirmation describing what it may undo. A SHA-256 fingerprint over ALL relevant original keys, including orphaned playlist data, must match the expected post-write state. Recovery is one-shot and never runs automatically.
- Manual guest backup export and restore use the same mutation lock. A profile-save failure rolls back library import without clearing unrelated app preferences.
- Orphaned/unlisted old playlist values are never swept up by routine writes, and malformed source cannot be replaced by an accidentally empty collection.
- Room PRIMARY writing is explicitly DISABLED. Room remains a shadow mirror and optional strict-parity read preview from Phase 3C. DataStore remains a guest profile mirror.
- Old launcher, Media3 player and private Android storage names remain unchanged.

## Limits, intentionally not hidden

This is a **single writer for legacy SharedPreferences within one application process**. It is not a database transaction across Room, SharedPreferences and DataStore. It cannot guarantee ordering against a separate Android process, another app, or previously installed legacy code writing the same file. Synchronous commits and recovery snapshots may add disk latency; monitor UI jank and playback responsiveness.

The recovery snapshot protects only the most recent completed edit, not arbitrary edit history. If a newer library write occurs, only that newer write is eligible for undo. A recovery that cannot verify its exact post-write fingerprint refuses to proceed. Android's SharedPreferences.commit() failure handling is not an external power-loss-proof journaling guarantee.

The Phase 3C Room read parity checks still apply. When Room stores one normalized metadata row for a URL that had distinct titles in different old playlists, exact parity may fail, and the app must continue to use the original legacy collection.

Google login, automatic Drive sync, multi-device merge, and permanent Room/DataStore write ownership are **not implemented**.

## Automated tests written (not yet executed)

**10 JVM mutation planner tests:** add/remove favorites, atomic rename semantics, duplicate playlist names, playlist song removal, duplicate songs, 300-track cap, 100-entry recent history cap, untrusted URL rejection, noop rename and selective deletion.

**7 Android instrumentation tests:** atomic playlist rename/write, fail-closed corrupt source, orphan-key preservation, guarded recovery, recovery invalidation on changed orphan key, concurrent playlist creation, and manual archive import sharing the writer lock.

Additional prior Phase 3A–3C tests still apply.

## Build and real-device acceptance

1. Download the Phase 3D source ZIP while logged into the private repository and extract it into a **new folder**.
2. Run \`build-windows.cmd\`. This runs unit tests and assembles the debug APK using the local Android SDK, not GitHub hosted runners.
3. If Python 3 is available, run \`python tools/verify_v2_phase3d.py\` plus previous source verifiers.
4. If the build fails, **do not uninstall or overwrite** the existing app. Send the first Kotlin or Gradle error.
5. On a disposable test device/profile, run \`:app:connectedDebugAndroidTest\` to execute instrumented storage tests. Some tests clear the \`nightwave_library_v1\` preferences and therefore **MUST NOT run against real personal playlists**.
6. Back up existing favorites/playlists externally using Phase 3A before any alpha installation.
7. Confirm create, rename, delete and reorder playlists; check that all names and tracks survive app kill/restart.
8. Add a favorite and undo the most recent library edit after confirmation. Verify the previous state returns. Add another track externally and verify stale undo is blocked.
9. Test several simultaneous playlist edits, fake disk failure, corrupt legacy preferences and import/rollback on a **disposable** profile.
10. Confirm Now Playing, next track, notifications, locked-screen audio and Bluetooth still work. Check main-thread stall/jank caused by synchronous writes.
11. Recheck Room parity after legacy edits and after import, ensuring no duplicates or lost metadata.

## Cutover prerequisite

Do **not** enable primary Room writes, remove legacy preferences, or merge this draft into \`main\` until the app compiles, all Phase 1–3D tests pass, the snapshot and rollback path works on a phone, and a single canonical Room writer can replace old writers without two sources of truth.

Room primary storage promotion and Google-backed recovery belong to subsequent milestones.
