# NW121 — premium Account & backup, precise access and recovery state

27 October 2026. Parent: verified NW120 `eb2722002fdd047bc9f0bff0a1d0e8be83eb167e`. Branch: `update/nightwave-nw121-premium-account-recovery`.

## User-facing result

Account & backup is implemented as real Jetpack Compose UI in the APK. It keeps NightWave's original palette and identity while improving hierarchy, restraint and actionable state:

- Gradient local-profile hero, bounded 600dp dialog and scrollable recovery sections.
- One contextual primary action: Connect, Reconnect, Create first backup or Back up now. Restore preview, merge, local export, account change and disconnect remain available through existing guarded callbacks.
- Distinct saved-account/access and historical verified-backup messages, masked email, real last-check/backup timestamps and an explicit access-attention chip. The You header cannot label unknown or failed access connected/backed up.
- Compact-width and large-font spacing, wrapping profile text and minimum 48–54dp actions. Preference rows provide one grouped Switch action with a minimum 64dp row rather than an unlabeled small toggle alone.
- Connection help also appears for a saved account with failures. Setup fingerprints, reports and the OAuth console are behind an explicit Connection details disclosure. No developer setup details replace the main recovery actions.
- Scope copy states that library metadata/selected optional data are backed up; audio files are separate and restoration starts with a review.

No emulator/phone render, visual comparison or TalkBack session was executed. Pure layout decisions and Android compilation are verified; pixel/layout acceptance is pending. No Spotify logo, copied interface, licensed catalog, premium score or parity claim is introduced.

## Concrete correctness repairs

Previously, saved account identity and a historical backup receipt could still appear connected after current authorization failed, while setup help was hidden for connected accounts. New `CloudAccessState` and last-check metadata are separate from backup verification. Cancellation preserves the previous state; permission/configuration issues need review, and transient failures do not claim a revoked grant. Verified backup timestamps/privacy receipts are preserved when access needs attention.

Access receipts have a separate monotonic revision. A worker captures its own receipt and may update a negative result only if no newer access proof replaced it. The same-account/same-consent/same-millisecond stale-failure case is covered by an Android test declaration. Existing consent revisions still fence account switches, reconnects and privacy changes. Foreground SharedPreferences observation publishes worker results, detaches on stop/destroy and ignores late callbacks after closure.

`Tasks.await` wraps Google failures. The worker now inspects at most eight cause nodes with identity-based cycle detection to retain typed Google/Drive status without logging exception messages or credentials. Configuration errors cannot accidentally enter the generic transient retry branch merely because of an ExecutionException wrapper.

A read-back-verified Drive copy whose local receipt cannot be saved is not reported as a successful backup job. Interactive feedback distinguishes that failure from an account/privacy change. Disconnect feedback checks persistence and stops local scheduling. Default Drive calls now have a 60-second overall deadline as well as existing connect/read/write and byte bounds.

The NW59 Windows preflight was adjusted for the new access-aware badge wrapper while retaining checks for the exhaustive backup-stage mapper. No test floor or safety guard was removed.

## Test evidence

- **1,453 JVM tests in 183 suites: zero failures/errors/skips.** Twenty new methods cover access semantics, compact badges, bounded/cyclic wrapped failures, responsive layout/action choice and five real-client isolated roundtrip scenarios.
- **49/49 current source/negative and Windows preflights pass.**
- Complete one-click Windows builder: **BUILD SUCCESS**; production Kotlin/Compose, JVM suite, Android instrumentation-source compilation and debug APK assembly pass.
- Final Android lint: **PASS — zero errors, 313 warnings, 17 hints.** No new suppression or warning baseline was added.
- Four new Android store tests compile for access/first-backup separation, consent/account fencing, preservation of backup/privacy receipts and newer same-millisecond access proof. They were **not executed** on a device.

### Exact boundary of the isolated roundtrip

The actual `NightWaveDriveClient`, multipart serialization, listing/download/readback and cloud/guest codecs run through an injected in-memory HTTP interceptor. All requests still target the expected Google HTTPS endpoints and appDataFolder; the interceptor returns synthetic responses before any network connection. It uses only a fixed test token and disposable guest metadata.

The tests verify upload/readback/archive decode equality, unchanged-copy reuse without another POST, corrupt readback rejection, retained HTTP 401 on denied readback, and network/invalid-input failure without a verified outcome. No PATCH/DELETE is sent. Production construction keeps the pinned Google endpoints and default client; the injectable client is internal for tests.

This does not validate live Google OAuth, production account switching, API setup, device cancellation, the actual library restore transaction or Drive access from the installed app. No real account, credentials, music, private library or external Drive data was read/written by these tests.

## Final artifacts and identities

- Package `com.nightwave.player`; version `2.0.0-alpha5s`, code `35`, compile/target API 36. Package, signing key, backup formats, Room schema, OAuth scopes and existing music files were not changed.
- APK SHA256: `E451254EF8006378DD981EA319EB088980A694CAAF60FF66F23BAB97FFAC6CE4`.
- Working-tree source fingerprint SHA256: `0654CB86836BDB180BFF05819C61C678E21E8ABF8520BD9529ED4323196C2151`, **792 build-affecting files**. Independent NW101 attestation matches.
- Certificate SHA256: `EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394`. Independent comparison against the saved NW120 APK: **MATCH**. Installed-phone signer continuity is untested.
- Workspace: `C:\Users\jayed\NightWave-work`. Recovery folder: `C:\Users\jayed\NightWave-checkpoints\20261027-nw121-account-recovery`. The original NW120 Git bundle was saved before edits. No tracked files were deleted for NW121.

## Consolidated phone/disposable-profile acceptance — pending

1. Inspect 320/360/412dp, landscape/tablet widths, long/Bangla names and 1x/2x fonts; all actions remain readable/reachable without clipping. Run TalkBack on status chips and full-row switches.
2. Guest-only launch: profile/local export work without Google, and no account/backup success is fabricated.
3. Connect a disposable account: Account checked appears only after Drive identity verification; no first-backup success or automatic schedule is implied yet.
4. Cancel the chooser, switch accounts, reconnect the same account and rotate during permission UI. Existing local music, guest identity, privacy decisions and prior remote backups survive.
5. Trigger supported expired/refused/configuration cases and a transient network failure. Saved-account help remains reachable; old successful backups remain historical receipts, not current access proof.
6. Run the four new isolated Android store cases and verify an older worker cannot overwrite a newer checked access receipt or changed consent.
7. Upload, read back, preview and restore a disposable library with the actual signed APK. Verify selected optional history/lyrics/preferences only, and no silent overwrite or restoration before explicit confirmation.
8. Change local library or consent while a preview is open. Existing stale-local and stale-account guards must reject old confirmation.
9. Check slow network, deadline, low storage/failed local receipt, app stop/restart and WorkManager cancellation. Do not promote an incomplete upload or claim a saved disconnection that failed.
10. Disconnect during permitted background work; local scheduling stops, app library remains, remote snapshots are retained and late results do not relabel the current account.

Public release remains **NO-GO** until live device/OAuth/restore, installed signer, rights/privacy and earlier-wave acceptance pass. Next ordered wave: **NW122 measured performance**. The checkpoint contains a full CRC/per-file-SHA-verified source ZIP, debug candidate, changed-file hashes, complete test/lint/build receipts, baseline/update bundles and resume instructions. Extract/restore into a new directory; do not reset an existing library-bearing app or populated checkout.
