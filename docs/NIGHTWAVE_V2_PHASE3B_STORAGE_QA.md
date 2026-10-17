# NightWave 2.0 — Phase 3B Room/DataStore migration

**Status: isolated source branch; Windows Gradle + on-phone QA not yet run.**
Version: 2.0.0-alpha3b / versionCode 11. Based on Phase 3A branch. Do not merge to production yet.

## What Phase 3B actually does

- Adds a normalized Room database with saved tracks, ordered playlists, playlist-entry links, liked songs, recent history and atomic migration checkpoints.
- Adds a Preferences DataStore copy of guest display name, avatar, theme, motion, audio, history privacy and other display settings.
- Opens the new storage on the **optional NightWave 2.0 preview**, asynchronously and off the main thread. Profile shows synchronization status and a Recheck action.
- Parses legacy \`nightwave_library_v1\` SharedPreferences strictly. If invalid JSON or unexpected links exist, leaves both source and existing Room snapshot unchanged and surfaces an error.
- Writes Room rows and checkpoint in one SQLite transaction; a crash rolls back the entire attempted snapshot.
- On repeated preview open / change to legacy preferences, computes a source digest and only re-imports when data changes.
- Never deletes or mutates the original music preferences. The Phase 1/2 media player and Phase 3A export/import continue to use original storage.
- Does NOT invoke Android's SharedPreferencesMigration for guest settings: that built-in migration may remove migrated keys. Instead, DataStore copies normalized values and retains the original file while it is the active source of truth.
- Does NOT enable Google login, Drive storage or auto cloud recovery.

**Important:** This is a protected **shadow migration**, not the final storage cutover. Changing favorites in the legacy player remains authoritative. Room/DataStore are still secondary mirrors, to be promoted after migration tests and an explicit dual-writer/cutover plan.

## Stored data

Room: only public video URLs and metadata, ordered playlist membership, favorite ordering and local recent history. No signed playback URLs, audio files, passwords or Google tokens.

DataStore: local guest profile ID, name, avatar, theme, motion, quality, privacy, crossfade and artwork/awake preferences, plus a migration digest. This is local storage and is deleted by uninstalling unless manually backed up using the Phase 3A backup.

## Build validation (free, on your Windows PC)

1. Download the Phase 3B source ZIP and extract it in a **fresh folder**.
2. Run \`build-windows.cmd\`. It runs Gradle \`:app:testDebugUnitTest\` and \`:app:assembleDebug\`, including KSP generation. It does not need GitHub Actions billing.
3. Optionally run \`python tools/verify_v2_phase3b.py\` to check source wiring.
4. For Room/DataStore device instrumented tests, connect an Android phone with USB debugging and run the Gradle \`:app:connectedDebugAndroidTest\` task from an environment with Android SDK configured (e.g. Android Studio). Do not assume that \`build-windows.cmd\` performs these tests.
5. If any Kotlin/Room/KSP task fails, send the **first** compiler error. Do not replace or uninstall the existing app.

## Phone acceptance tests

- Install Alpha 3B **over** the previous debug build only after successful compilation. Don't uninstall or clear app data.
- Open legacy Home → Explore the new premium interface → You.
- Wait for the local storage migration status. Expect a nonzero liked-song and playlist count matching the current library.
- Tap "RECHECK NEW STORAGE"; reopening should not discard favorites or reset theme/avatar.
- Add and reorder items in existing Library; reopen preview. Mirror statistics should reflect legacy changes. The original library should remain playable even if the mirror errors.
- Export a Phase 3A guest backup, import it after explicit confirmation, and recheck the Room mirror.
- Simulate malformed JSON and transaction interruption only in disposable test builds, not with real data.

## Automated regression coverage added

JVM: eight tests covering legacy ordering, stable digests, mutations, invalid URLs, malformed JSON, duplicate playlist names/URLs, and empty profiles.

Android instrumentation: five Room tests for ordering, idempotence, corrupted legacy rejection, SQLite rollback on synthetic crash, and updated favorites; one DataStore test for guest settings persistence without source deletion.

None of these tests were executed in this environment. They must pass locally.

## Release gate before Room becomes canonical

1. All Phase 1/2 Compose and playback code compiles cleanly and passes phone QA.
2. Room KSP schema JSON exported to \`app/schemas\`, version-controlled; no destructive migrations.
3. Repeated legacy→Room reconciliations match favorites, playlist membership/order and history for multiple realistic datasets.
4. An actual rollback and low-storage/error exercise passes.
5. Implement transaction-safe writes to BOTH active legacy and Room or a verified one-time cutover; prevent concurrent old/new editors from overwriting changes.
6. Switch Compose UI reads/writes only after all checks above; keep a pre-cutover export.
7. Google identity and account backup are separate, later phases with user-consented scopes.

**No deletion of legacy preferences is planned for this milestone.**
