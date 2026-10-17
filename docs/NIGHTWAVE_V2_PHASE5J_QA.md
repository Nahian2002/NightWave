# NightWave 2.0 — Phase 5J playback UI responsiveness

**Version:** 2.0.0-alpha5j (versionCode 26)  
**Branch:** `feature/nightwave-v2-phase5j-player-state-efficiency`  
**Status:** Draft, not a verified Android release.

## Concrete issue identified

The premium player previously ran `StudioPlaybackBridge.updateFromPlayer` on every 750 ms UI playhead tick. That method reads and parses the app's saved queue from SharedPreferences, checks bookmarked playback position, and when playing imported audio reads offline-session preferences. Doing these operations repeatedly on Android's main/UI thread adds avoidable work, especially with large saved queues.

**Phase 5J replaces per-tick disk-backed reconciliation with a lightweight playhead update.**

- Fast UI ticks ask the existing Media3 Controller for current position, duration, playing/buffering state, and active media ID without reading preferences.
- The authoritative `Player.Listener.onEvents` path still reconciles complete metadata/queue immediately when Media3 reports a real event.
- Every eighth progress tick (about 6 seconds while the UI remains active), the bridge performs the old complete queue reconciliation, catching queue edits issued via the service's explicit Android Intent API.
- A new media ID or loss of an actively playing MediaItem also forces a complete refresh immediately.
- Returning to the premium screen triggers a full reconciliation first; keeping the screen in the background never starts the UI ticker.
- Redundant Compose state copies are skipped when the playhead/playing/buffering values are unchanged.
- The active saved bookmark and explicit offline-resume behavior remain untouched. The background audio service itself is unchanged.

This reduces unnecessary disk reads and state updates, but **does not prove an Android ANR, existing crash warning, or pixel-level UI defect is fixed.**

## Validation

- Isolated Kotlin CLI compilation of a matching `PlayerSnapshotPolicy` implementation and 8 behavior assertions passed.
- Five JVM JUnit tests were authored in `PlayerSnapshotPolicyTest.kt` for normal ticks, periodic refresh, track changes, saved placeholders, and media loss. Android Gradle has **not** executed these JUnit tests.
- Source-only gate `python tools/verify_v2_phase5j.py` checks the Media3 connection, light fast path, no preferences scans in fast ticks, old command APIs, version, APK/device safety and prior offline resume.
- Source code readback and braces/import checks are not a replacement for actual Kotlin/Android compilation.

## Windows and Samsung acceptance

1. Download the full Phase 5J branch ZIP into a new directory. Don't uninstall NightWave and don't delete its previous music data.
2. Run `build-windows.cmd`. Inspect `dist/NightWave-build-status.txt` for **SUCCESS**, version **2.0.0-alpha5j**, version code **26**, signer and APK SHA-256. If it fails, send `NightWave-build-errors.txt` plus compiler/log excerpts.
3. Connect exactly one authorized phone, then run `check-device-update.cmd`. It checks the installed app's APK signature and version without modifying it. Do not install when the check is inconclusive.
4. On a disposable Android emulator, run `connectedDebugAndroidTest` to execute existing classic/premium startup smoke tests.
5. In the premium player, verify time/progress advances smoothly for at least 60 seconds with a large saved public queue. Pause, seek, resume, next/previous, and open Up Next.
6. Add and remove tracks in the queue through the existing service/UI actions. They should appear in Up Next within the periodic reconciliation window or sooner after a Media3 event.
7. Play imported offline music, verify explicit session Resume, progression, seek, notification, screen-off playback and queue transitions. Restore the app from background and check current metadata immediately.
8. Validate font scaling, screen rotation, split-screen if supported, and no stale artwork after track changes.
9. If the Android system again reports frequent crashes, share only the redacted NightWave FATAL EXCEPTION block (or classic Home's privacy-minimal local crash summary), not a full unreviewed device log.

## Release gate

**Do not merge** until Windows Android Gradle + JUnit and real-device startup/playback QA succeed. The authoring environment contains Java/Kotlin but no Gradle/Android SDK and the download host cannot be resolved; no verified APK is available from this phase. Stable `main` is untouched.
