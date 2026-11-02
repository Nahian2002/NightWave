# NightWave 2.0 Alpha 3 — Local data safety and recovery QA
Date: 2026-10-17

**Status: source staged; FULL ANDROID BUILD, local JVM tests, Room instrumentation and phone import tests still PENDING.**

## Phase 3 accomplishments
- **Manual guest export:** Android Storage Access Framework creates a user-chosen \`NightWave-Guest-Backup-YYYYMMDD.json\`. The backup contains favorite/playlist metadata, display name, avatar and app preferences, but not Google credentials, stream URLs, audio files or a stable device identifier. History is excluded until the user opts in.
- **Strict format:** v1 schema, 10 MiB maximum, SHA-256 checksum, known-field allowlists, bounded song and playlist counts, unique canonical YouTube links, and HTTPS thumbnail host validation. The checksum identifies accidental corruption; it is NOT encryption or cryptographic authentication. Exported files are readable JSON—store them safely.
- **Safe import:** Preview counts, explicit merge/replace/merge-music-only decisions, final confirmation, capacity-conflict rejection, keep local device identity, preserve local history when the incoming backup omitted it.
- **Library restoration:** One synchronous SharedPreferences editor commit for all collection records, plus profile settings save with recovery copy and rollback on errors. A private rescue journal remains available if the app/process dies mid-import; auto-recovery occurs on the next Studio launch. Failure or incomplete cleanup is not reported as success.
- **New persistence staging:** Room v1 database mirrors the legacy favorites/history/playlist records with transactional read-after-write comparison; DataStore mirrors guest preferences. Neither mirror takes over playback or overwrites existing preferences in this phase.
- **UI performance:** Imports, checksums, exported-file writes, Room mirrors, and DataStore staging run on Dispatchers.IO rather than Android UI thread.
- **Tests uploaded:** 12 JUnit backup/merge unit cases, 2 Android instrumented Room mirror tests, a Phase 3 static checker. They have not been executed by Android Gradle in this environment.

## Why Room is only a staging copy
Both legacy and Phase 2 player interfaces still read and write \`nightwave_library_v1\` and \`nightwave_queue_v1\`. Cutting over only some writers to Room would produce two contradictory copies and risk losing song edits. Phase 3 prepares, verifies and snapshots Room. Replacing the active source of truth will require coordinated migration of all readers/writers and physical phone QA. Existing libraries are not deleted.

## Windows build and sanity checks
1. [Download this phase branch ZIP](https://github.com/Nahian2002/NightWave/archive/refs/heads/feature/nightwave-v2-phase3-data-recovery.zip). Extract to a new folder.
2. Optional Python validation: \`python tools/verify_v2_phase1.py\`, \`python tools/verify_v2_phase2.py\`, \`python tools/verify_v2_phase3.py\`.
3. Run \`build-windows.cmd\`. It runs \`:app:testDebugUnitTest\` and \`:app:assembleDebug\` on local Windows with no paid GitHub runner. Version name \`2.0.0-alpha3\`, version code 10.
4. Do NOT uninstall the installed NightWave APK and do not clear storage. If Gradle fails, send the **first Kotlin compile error** and leave the old APK installed.
5. After BUILD SUCCESS, install \`dist\NightWave-debug.apk\` over the app with the same debug signing key. If signing differs, **do not uninstall** as a workaround; valuable guest data would be deleted.

## Guest backup acceptance
1. Open **Home → Explore the new premium interface → You / Profile**.
2. Set a display name/theme, like a few songs and create a playlist. Choose **Export Guest Backup**.
3. Save the JSON to a location outside the app, such as Documents or Drive. Check that the file is visible and nonzero.
4. Change the display name and/or create a TEST playlist, then choose **Import Saved Backup**. Inspect counts, choose Merge, confirm. Verify no duplicates and original playlists remain.
5. If testing Replace, first preserve the original export and use expendable test data. Confirm explicit second warning.
6. With history privacy off, inspect that recent history is absent from the exported payload (even if listening history exists locally). With it on, only then should history appear.
7. Try a corrupted JSON export or a hand-tampered checksum. It must be rejected before changing saved music.
8. Restart the app and confirm all restored favorites and profile settings survived. If pending recovery reports failure, do not clear storage; retain the original backup and send the report.

## Room and DataStore checks
- On an Android emulator/device run \`:app:connectedDebugAndroidTest\` and check the two Room migration tests. (Not part of default local build.)
- Crash/relaunch, low storage, import cancellation, schema conflict and concurrent editing are separate device acceptance cases.
- Guest export is MANUAL. After uninstall, it can be restored only by importing the file again. It is not Google authentication or automatic backup.

## Security and disclosure
- The exported JSON is NOT encrypted. Treat it as personal data, particularly if history is included.
- Checksum protects integrity against accidental corruption, not a malicious replacement by someone who can recompute it.
- Do not include OAuth tokens, signed media URLs, passwords or personal account identifiers.
- Private rescue copies may temporarily include local history, inside the app sandbox. They are removed after a successful restore.
- Third-party YouTube search/retrieval limitations from Phases 1–2 remain; this phase does not fix unavailable search providers.

## Next phase
Phase 4: improve the Media3 service with interruption handling, sleep timer, playback settings and background reliability. Google sign-in and appDataFolder recovery remain later work and require OAuth setup/verification.

**DO NOT MERGE THIS BRANCH INTO MAIN UNTIL THE ANDROID BUILD AND REAL RESTORE TESTS PASS.**
