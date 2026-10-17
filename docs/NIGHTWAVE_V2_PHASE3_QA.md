# NightWave 2.0 Phase 3 — Safe guest archive milestone

**State:** Source committed. Full Android Gradle compile, on-device export/import and the new unit tests have **not** been executed here.

## Implemented
- Guest exports use Android Storage Access Framework CreateDocument (no broad file-system permission).
- Guest imports use OpenDocument with bounded streaming (2 MB maximum) and an explicit confirmation before replacement.
- Format v1 contains display name, avatar, theme, liked tracks and ordered playlists. Recent listening history stays device-only.
- Canonical public-video URLs only; unknown fields, invalid themes, duplicate playlist names and invalid video IDs are rejected.
- Exact-payload SHA-256 integrity check, payload size and playlist/song count limits.
- Replacement stores an independent pre-restore library snapshot before committing an entire new library in one preference transaction. Restores profile after the library commit; failed profile save attempts immediate library rollback.
- No OAuth, Google account sync or fake account recovery claims. Backups are local JSON metadata, **not encrypted**. Users must store backup files securely.
- Includes eight unit tests for round-trip, integrity, size, invalid media, unknown formats and duplicates.
- Version 2.0.0-alpha3 (code 10), still on an opt-in branch based on the unverified Phase 2 Compose preview.

## Not completed
Room and DataStore replacement of legacy preferences, robust interrupted-import recovery UI, Google sign-in and Drive backup, and multi-device sync are future stages. This milestone does **not** automatically restore anything after uninstall.

## Testing required
1. Extract branch ZIP into a new folder; run `build-windows.cmd`. Do not uninstall the current working version if build fails.
2. Open Home → Explore the new premium interface → You (Profile).
3. Create a playlist with multiple tracks, like songs and edit profile.
4. Export to JSON via file picker. Keep backup somewhere outside the app's private storage.
5. Change name/theme and library; import JSON, review summary, choose Replace. Verify earlier playlist order and favorites return.
6. Test Cancel, empty file, malformed JSON, tampered checksum, oversized file, invalid URLs and low-storage conditions. No partial mutations should be accepted.
7. Reopen app and confirm playlists, avatar and theme persist. Verify screen-off playback still works.
8. Check export file contains NO access tokens, passwords, signed audio URLs or listening history.

**Do not merge to main before Phase 1/2/3 compile and device regression checks.**
