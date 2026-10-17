# NightWave 2.0 — Phase 3C verified Room read preview

Branch: \`feature/nightwave-v2-phase3c-verified-read-routing\`  
Version: \`2.0.0-alpha3c\`, code 12  
Status: **source implemented, not compiled or phone-tested**

## What was implemented

1. **Strict parity model.** Compare the legacy favorites, listening history, playlist names/order, per-playlist tracks/order, public video metadata, and migration source digest against the Room copy. Matching counts alone are never sufficient.
2. **Snapshot-safe Room reader.** Load an ordered, transactionally consistent Room snapshot, check its migration checkpoint, and re-read the current legacy digest afterward to detect changes during the read. Return **no Room rows** unless parity passes.
3. **Optional session-only read preview.** New Compose Profile section offers "TRY VERIFIED ROOM READS" and "RETURN TO ORIGINAL LIBRARY". The default remains original SharedPreferences.
4. **Automatic legacy fallback.** A stale checkpoint, changed source, mismatched metadata, corruption or migration failure disables Room preview and refreshes UI from the original library. The UI never silently displays an unchecked Room snapshot.
5. **No destructive writes/cutover.** All song/playlist mutations still go through MusicLibrary's SharedPreferences. Neither the WebView search/playback service nor the legacy launcher is replaced by this phase.
6. **Vanished-source guard.** If a previously populated Room mirror exists but the old library preference keys disappear unexpectedly, the migrator refuses to clear Room and reports an error requiring review.
7. **Zero database schema changes.** Existing Room version 1 and its pending exported schema remain compatible with Phase 3B.

## Why this is not the final storage cutover

The existing v0.8 player and Phase 1/2/3A Compose screens can still mutate the original preferences. Switching Room into primary read/write mode now would create concurrent authors and risk data loss. Phase 3C only verifies that a **read-only** Room view can be trusted for the current session.

Room normalizes metadata so different display titles for the same URL in two old playlists may conflict. Such source data intentionally triggers fallback, rather than changing any playlist title during migration. A future canonical data model must handle that case explicitly.

A session's verified snapshot is not a globally serializable lock on the old player. The UI rechecks on resume and after local library actions. Do not interpret this as a full race-proof multi-process cutover.

## Added test coverage

- 10 JVM parity tests: exact match, missing/stale checkpoint, favorite order, recent order, playlist order, song order, metadata mismatch, empty library and duplicate song membership.
- 7 Android instrumentation tests: six verified-reader scenarios plus one new vanished-source recovery guard, in addition to the five Room migration and one DataStore tests from Phase 3B. Reader scenarios: no checkpoint, verified fresh snapshot, stale preferences/reconcile, corrupt Room metadata, conflicting metadata for same URL, malformed legacy JSON.
- Source gate: \`python tools/verify_v2_phase3c.py\`. Phase 3B source gate also updated to support this version.

**These tests were added but not executed here. No Android Gradle build was run.**

## Local Windows build

1. Download the Phase 3C branch ZIP. Extract to a **new folder**.
2. Run \`build-windows.cmd\` (local JDK/Gradle/Android SDK, no GitHub Actions billing).
3. Wait for \`BUILD SUCCESS\`. This runs JVM tests and assembles the debug APK.
4. Do not uninstall the previous version. For device instrumentation, run \`:app:connectedDebugAndroidTest\` with an Android SDK and a test phone/emulator; this is a separate step.
5. Only install the alpha build if compilation passes and you have a fresh Phase 3A manual library backup outside the app.

## Required device checks

- Open the legacy player and verify liked songs/playlists and screen-off playback are intact.
- Open "Explore the new premium interface" → Profile. Run "RECHECK NEW STORAGE".
- With a normal intact library, try "TRY VERIFIED ROOM READS". It should report exact parity and show the same songs and playlist order.
- Tap "RETURN TO ORIGINAL LIBRARY". All content should remain unchanged.
- Modify a playlist or favorite in the legacy UI, return to the Compose preview and inspect status. Room should refresh or safely fall back; no missing songs.
- Test same URL with conflicting titles in different playlists. The Room preview must refuse parity rather than silently substitute a title.
- Confirm guest backup export/import still functions. Import must invalidate any prior Room preview and trigger a new check.
- Exercise corrupt or missing checkpoint, Room transaction failure and low-storage conditions only on a disposable test profile. Never intentionally corrupt real preferences.
- Lock screen during track playback and verify Bluetooth/media controls.

## Next release gate

Only after Phases 1–3C compile and device tests pass: implement a single-author, conflict-safe transactional write cutover; decide how to retain per-playlist metadata; preserve a reversible snapshot; verify crash/restart/low-storage handling. **Google sign-in and cloud recovery remain later phases.**
