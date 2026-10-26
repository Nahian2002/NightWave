# NW72 — Exact Google account-session recovery for Drive preview, merge and restore

**Parent:** `feature/nightwave-nw71-smart-library-discovery`  
**Integrated source:** `feature/nightwave-nw72-cloud-session-restore-safety`  
**Release:** **NO-GO**. Google OAuth **internal status 8 remains unresolved** pending real APK signer/configuration and Play services verification.

## Source defect and risk

Existing CloudBackupState already increments a persisted **consent revision** after an account connect, disconnect or backup/privacy setting change. Background upload workers protect their results with this revision. Interactive **Drive restore/merge previews**, however, used just the account email to check current identity. A pending confirmation from a previous authorization could therefore look current after disconnecting and reconnecting the *same* Google account, even though it belonged to an older session. A local-archive checkpoint protects against intervening library edits but does not prove the Google session itself is current.

## NW72 changes

- Introduced pure Kotlin `CloudAccountSessionPolicy` requiring **a nonblank, case-insensitively matching Google account and exact consent revision**. Session equality does not depend on a completed backup flag, because that flag may change when an upload finishes without rotating the account session.
- After Drive verification, the current `CloudBackupConsent` is carried **in memory** through the snapshot list, snapshot download, Compare & Merge confirmation, Restore/Restore + History confirmation and optional preferences/LRC restore prompt. An outdated account receipt cancels the action and requests another preview.
- Confirmation buttons block while another cloud action is busy. Background execution rechecks the exact session receipt **before starting local modifications**. A reconnect of the same email or a changed backup privacy setting invalidates old previews.
- No new Google authorization scopes, login webviews, server, client secrets, tokens in preferences, Drive deletion/overwrites, snapshot format changes, local song changes, backup uploads, or profile migration were introduced.
- Drive backups still require `drive.appdata` only and keep original copy-on-write/checksum-readback behavior. Offline audio cannot be reconstructed from Drive metadata backups.

## Verification actually performed

- **Exact committed production policy** `CloudAccountSessionPolicy.kt` compiled together with the existing `CloudBackupConsentPolicy.kt` under local `kotlinc`. The compiled Git blob SHA-1 was `673c91e59030ccce1e86dac83284e1056ed4e9f4` (existing consent class SHA-1 `c4d71ab2ca51e10c4057346ea29e8eec74ebc30a`).
- **6,013/6,013 isolated Kotlin/JVM assertions PASS**: account matching, blank/changed account rejection, same-email reconnect revision change, busy request, 2,001 revision values with independent state and confirmation checks. Reproduction: `tools/NW72CloudSessionSmoke.kt` (blob `ce18c76aca4900d3f54719ee81349ae115fa69b3`).
- Added JUnit source `CloudAccountSessionPolicyTest.kt`; Android Gradle/JUnit execution **not verified**.
- Updated the cumulative Windows PowerShell source gate to require NW72 session protection while retaining prior NW53–NW71 safeguards. Source text matches are not full Android integration tests.
- **Not performed:** Google Cloud client inspection, OAuth status-8 resolution, live Drive backup/restore, instrumentation, full APK build or signer-compatible Samsung app installation.

## Google error 8 — what actually needs verifying

Internal status **8** is Google Play services' reported error, **not proof** of a particular root cause. Seeing the Google account chooser does **not** establish Drive scope consent, account identity verification or a successful cloud backup. Do not repeatedly grant broader scopes, disable signing checks, or bypass Google's native authorization UI.

At final Windows/device QA, perform these steps:

1. Build the **NW72 cumulative source** in a clean Windows folder with `build-windows.cmd`. Only after a complete success run `check-google-oauth.cmd` against the verified newly built APK.
2. Compare the package name (`com.nightwave.player`) and certificate SHA-1 to **Google Auth Platform > Clients > Android OAuth client** in the same Google Cloud project that has the **Google Drive API enabled**. In Testing mode, ensure the intended Google account is an allowed test user.
3. Run `check-device-update.cmd` before upgrading the installed app to preserve its original signer and app-private offline music. Do **not** uninstall/clear data to work around OAuth 8.
4. On the phone, open You > Account & backup > **Copy Google setup report**; compare the phone's **installed APK** package/SHA-1, Play Services status and last failure stage/code with the Windows APK and Google Cloud project. No token or personal email should be included in the shared diagnostic.
5. Test a **small, consented metadata-only backup**; verify the Drive snapshot readback and a safe preview. Use a disposable library profile for real restore tests. A remembered account-picker selection alone is insufficient.
6. While a cloud restore/merge/optional-data preview dialog is open, disconnect/reconnect the same account or change a backup privacy option on another supported surface, then confirm. A stale preview must refuse; a newly verified preview must succeed. Verify that existing offline music survives.

**Remaining:** full Android compilation and instrumented QA; actual OAuth account connection and Drive round-trip; any Google Cloud client fix; public-release privacy/licensing/Play review. No claims that Google status 8, unrestricted offline song saving or Spotify catalog parity are fixed by this source-only batch.
