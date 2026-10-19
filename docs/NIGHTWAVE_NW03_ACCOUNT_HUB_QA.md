# NightWave NW-03 — Local Account & Google Drive Backup Hub

**Date:** 19 October 2026  
**Base:** NW-02 Sound Studio commit `4cbbceb4a378316089e02cb2ccfaa384d15bbaa6`  
**Branch:** `feature/nightwave-v2-nw03-account-backup-hub`  
**State:** Source committed. **Not a verified Android release; fresh Gradle build, Google authorization and phone acceptance remain pending.**

## Delivered

### User experience

- **You → Account & backup:** A dedicated, theme-aware Compose dialog with local guest identity, optional Google Drive connection, backup status, backup/restore/merge actions and backup preferences.
- **Three distinct backup states:** `LOCAL`, `DRIVE READY`, `BACKED UP`. A Drive authorization with no successful remote verification is not reported as a completed backup.
- **Guest protection:** Connecting, changing or disconnecting Google Drive does not sign out the local guest or replace imported music, playlists and lyrics.
- **Account privacy:** Drive account email is masked on the account hub and legacy You-page backup details; persisted account identity is preserved for scoped authorization.
- **Confirmation before account changes:** Disconnection and switching to another Drive account clearly explain what happens to local music and existing backups.
- **Safe native handoff:** Local export, cloud restore preview and comparison flows close the account hub before opening Android file pickers or native dialogs, avoiding stacked modal windows.
- **Clear optional controls:** History, private LRC, preference and Wi-Fi-only automatic backup settings remain disabled without a connected Drive account, and preserve their existing default states.

### Implementation and safety boundaries

- Uses the existing Google Play services authorization flow with `drive.appdata` only; **no new OAuth scopes**, API keys or dedicated NightWave server.
- This is **not full Google identity sign-in** and does not create a Spotify-like online account. The guest profile stays local. A Drive connection only grants private backup authorization.
- Backup metadata is not identical to uploading the underlying music files. The existing personal audio locker has a separate user-controlled workflow.
- Google authorization requires valid app signing SHA/OAuth registration and Google services. No success is claimed without device verification.
- Old NW-01 Shuffle fixes and NW-02 Sound Studio stay in this branch unchanged.

### Automated coverage added

Five pure JVM unit tests assert: disconnected/local classification, connected-not-yet-backed-up classification, verified backup classification, display-only account masking, and correct connection/busy gating for backup actions.

Source preflight `tools/verify_nw03_account_hub.py` checks wiring and authorization scope; the Android CI workflow includes the new branch and source check.

**Those tests are authored, not confirmed passing as part of a fresh full Gradle build.** GitHub CI may fail before a runner starts; this must not be called a source or test failure without executed job evidence.

## Deferred phone acceptance

When the user is ready for the combined NW-01–NW-03 test:

1. Build a fresh extracted NW-03 ZIP using `build-windows.cmd`; require `BUILD SUCCESSFUL` for `:app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin`.
2. Confirm version, SHA-256 and APK signer compatibility **before** any in-place update. Never uninstall the existing app to work around a signing mismatch.
3. Open **You → Account & backup** without Google connection. Verify **LOCAL** status, guest profile name and disabled backup-only controls. Export a test library.
4. Tap **Connect Google Drive** and observe the Google consent flow or exact status code. No unverified success message should appear.
5. If connected, check **DRIVE READY** (not BACKED UP) before first verified backup. Create a backup using a small test library; only a verified upload should show **BACKED UP**.
6. Preview a restore and merge; confirm no accidental overwrite or duplication. Test cancellation and disconnected/error states.
7. Tap **Change Drive account**, cancel once, then review confirmation without switching in a production library. If testing actual switch with a secondary test account, verify the previous account's last verified backup is not attributed to the new account.
8. Tap **Disconnect Drive**, cancel, then confirm on an isolated test library. Confirm local songs, playlists, profile and settings survive; old Drive backups remain in the respective account.
9. Retest NW-01 rapid public Shuffle, NW-02 Sound Studio, UI size and background playback for regressions.

## Next work

**NW-04** should address the visibly malformed seek bar, blurry artwork, Now Playing control hierarchy, compact mini-player and narrow-screen bottom navigation. Full Google identity/account sign-in requires a separate Google Identity/Credential Manager implementation and associated OAuth project/client configuration; it must not be represented as completed by this phase.
