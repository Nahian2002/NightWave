# NightWave Phase 5U — Google Drive backup (draft, device verification required)

## Scope and safety

A new optional Google authorization uses only `https://www.googleapis.com/auth/drive.appdata`. Google Drive's `appDataFolder` is hidden and available only to NightWave with this permission, not a user-visible Google Drive folder. The user can revoke authorization or remove app data via Google account/Drive controls. Never request full Drive or Gmail access for this feature.

NightWave requests account permission *inside the app*, never through the user's ChatGPT Google Drive connector. The Android application's package and signing fingerprints must be registered with Google OAuth. No client secret, refresh token, access token or user music files are stored in the backup archive or Git repository.

## Implemented in the Phase 5U source

- **You > Library & backup:** Connect Google Drive; Back up now; Preview cloud restore; opt-in listening history (default OFF); Wi-Fi-only (default ON); Disconnect.
- **Deliberate first-run choice:** Granting permission **does not** overwrite or upload a library. A user restoring on a new/empty phone can preview old backups first. Scheduling is enabled **only after** a user-triggered snapshot uploads and is read back byte-for-byte.
- **Server storage:** append-only, uniquely named `nightwave-cloud-v2-<timestamp>-<random>.json` under Drive `appDataFolder`. No automatic deletion/overwrite in 5U. Storage use may increase over time until retention controls are designed. Previous snapshots remain available in Drive; the UI lets the user choose among up to 20 most recent matching snapshots before downloading and validating the selected one.
- **Backup data:** v1 guest archive (profile display name, theme, avatar, liked public-video URLs and playlists), plus at most 100 validated recent public tracks *only if opted in*. Existing manual export format v1 is unchanged. No raw offline audio, local audio paths, searches, login credentials, signed stream URLs or player session cache. Other profile preferences are not yet part of cloud restore.
- **Restore:** cloud snapshot must pass size, fields, URL constraints, nested v1 archive and SHA-256 checks before a preview. The user confirms replacing local favorites/playlists; a second explicit option optionally replaces local recent history. The pre-import recovery snapshot remains. Never silently overwrite a library or auto-restore on Google connection.
- **Scheduled backup:** WorkManager unique 24h periodic task with unmetered network requirement by default, dynamic setting, account-pinned authorization and no background permission prompts. Android can delay or skip a run under battery/network restrictions; a listed last-success time reflects only verified upload + reread.
- **Disconnect:** cancels future WorkManager tasks and clears local account association. Previous Drive snapshots remain; revoking Google's scope is a separate Google account control. Previously uploaded history stays in old snapshots even if future history opt-in is disabled.
- **Google account verification:** code pins the account for background requests; if Google cannot identify/reauthorize the same account, uploads stop safely until the user reconnects.
- **Not in Phase 5U:** automatic cloud playlist merge across two changing phones, server-based multi-device live sync, global search history, full playback-position sync, Google Drive file deletion/retention, distributed conflict resolution.

## Google Cloud Console setup (required before live Drive QA)

1. In **Google Cloud Console**, create/select a project and enable **Google Drive API**.
2. In **Google Auth Platform**, configure branding/consent information. Add the `.../auth/drive.appdata` scope. If the publishing status is Testing, register the intended Google test users. Verify the scope classification and project publishing requirements with Google.
3. Create an **Android OAuth client** with package `com.nightwave.player` and the signing **SHA-1** of the actual APK being run. Register the release signing certificate separately when it exists. A debug APK built on a different machine/CI usually has a *different* SHA-1: OAuth may fail even if Gradle compiles.
4. Do not hardcode a cloud secret/API key or commit `google-services.json` to satisfy this flow. The Google Play services Authorization API can request the narrow scope based on the registered Android application.
5. Run `build-windows.cmd` on the Phase 5U branch and require Gradle **BUILD SUCCESSFUL**, including `:app:testDebugUnitTest`, `:app:assembleDebug` and `:app:compileDebugAndroidTestKotlin`. Then run `check-device-update.cmd` and verify signing compatibility before an in-place install. Never uninstall/clear local music to get around a signing mismatch.

## On-device acceptance checklist (first use a disposable test account/device)

1. Start with a populated local test library and no Drive authorization. Confirm the Google controls are opt-in, the original manual Export/Restore/Undo remain, and no background Drive task is registered.
2. Tap **Connect Google Drive**; exercise account picker, consent accepted/declined, missing Google Play services and a two-account phone. No data should upload on consent alone. A different account must not receive the prior account's backups.
3. On the selected account, tap **Back up now**. Confirm Drive appDataFolder snapshot creation, server read-back equality, last verified time, and scheduling on Wi-Fi. Repeat backup: a second snapshot should exist; earlier one should be unchanged.
4. Toggle listening history OFF/ON and create new backups; decode exported byte payloads under controlled QA to ensure history is absent/present as chosen. No media content or secrets should be present.
5. Toggle Wi-Fi-only OFF/ON; inspect WorkManager constraints. Disable connectivity and grant/revoke OAuth; automatic retries must not launch a permission prompt or upload to a different account.
6. On a second disposable install using the same Google account, **preview restore before backup**; choose an older snapshot, verify counts, cancel with no change, restore library only (preserve local recent), and restore library + history only after separate selection. Test corrupt snapshot failure leaves local data unchanged.
7. Confirm favorites, playlists, profile basics, classic view, offline library, search, Up Next swipes, device audio playback and original recovery snapshot all remain correct.
8. Disconnect: scheduled work cancelled, existing Drive snapshots not deleted, no further uploads. Reconnect is explicit. Note that old history still exists in earlier snapshots.
9. Check Android 12–16 devices after background restriction/battery saver: do not promise exact daily execution.
10. Do not merge draft or publish until build + Android device OAuth/Drive end-to-end QA passes. Source-only checks are not proof of working sync.

## Useful reference documentation

- https://developers.google.com/workspace/drive/api/guides/appdata
- https://developers.google.com/android/reference/com/google/android/gms/auth/api/identity/AuthorizationClient
- https://developers.google.com/workspace/drive/api/guides/manage-uploads
- https://developer.android.com/topic/libraries/architecture/workmanager
