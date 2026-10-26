# NW62 — verified backup guard and post-authorization checks

Source branch: `feature/nightwave-nw62-backup-verification-guard` (based on NW61).

## Change
- Refuse to mark a Google Drive library backup as verified when the success timestamp is zero or negative, in both legacy interactive and consent-bound worker success paths.
- Add isolated Android instrumentation regression for invalid timestamps. It uses sandbox preferences and no real Google account.
- Keep package identity, Drive OAuth scopes, account picker, backup format, playlists and offline files unchanged.

## What account connection means
A successful Google sign-in only authorizes Drive access. It **does not** prove your library was uploaded, that a backup is current, or that restore works.

## Owner acceptance
1. Build the NW62 branch locally and install without clearing app data. Match the signing identity of your current installation.
2. Open NightWave's Account & backup settings. Confirm Google remains connected.
3. Trigger the app's **manual backup** and verify the UI reports **Backup verified** or **Already backed up; library unchanged**, with a nonzero last-success time.
4. Add a disposable playlist or favorite, back up again, and confirm the success timestamp advances.
5. Use the app's restore preview to inspect intended changes. Do **not** restore over valuable data merely as a smoke test; use a safe test profile/device when available.
6. Run the instrumented `CloudFirstBackupConsentInstrumentedTest` test class on Android.

## Evidence and release status
The source guard and regression were committed to GitHub. No Android compile, instrumentation run, Drive round-trip, physical-device check, or public-release approval is claimed.
