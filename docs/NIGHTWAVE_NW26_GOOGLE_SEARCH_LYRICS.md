# NightWave NW26 — Google Drive authorization diagnostics, reliable backup, search and LRC discoverability

**Date:** 24 October 2026. **Status:** DRAFT / not Android-release-certified.
**Parent:** NW25. **Branch:** `feature/nightwave-v2-nw26-google-search-lyrics`.

## Delivered

### Google Drive connection and backup

- Preserved the **least-privilege** `drive.appdata` OAuth scope for metadata/history and user-imported LRC backup. There is still no broad Drive or Gmail permission request, server, login credential storage or access token persistence.
- Kept Google Play services' interactive account selector and token-based account verification using `GET /drive/v3/about?fields=user(emailAddress)`. A chooser returning a token **does not guarantee** Drive API access, so the app now distinguishes **HTTP 401/403** (authorization/API/account setup problems) from **408/429/5xx** transient failures, with redacted user-facing messages. This clarifies the previously observed “account chooser opens but never connects” symptom, but **cannot itself prove OAuth registration is correct**.
- Background WorkManager backup now stops retrying permanent 401/403 failures blindly. Transient responses remain subject to bounded retry; no OAuth token, email, HTTP body, or full authorization URL is included in errors.
- Added a pure `CloudBackupConsentPolicy` and immutable snapshot of account, cloud enabled state, listening history/lyrics/preferences opt-ins and Wi-Fi-only state. Both manual **Back up now** and periodic worker compare before and after network upload. If any consent/account choice changes, the old upload cannot be certified as the *current* verified backup. Existing remote snapshots remain untouched. A remote transfer cannot be rolled back, but UI and status are truthful.
- Existing Drive snapshots remain append-only and verified by byte-for-byte download readback, not just HTTP upload success. No automatic restore, overwrite or deletion.

### Device-local search

- Introduced `OfflineSearchRanking` with case-folding, Latin diacritic matching (e.g. “Beyonce” finds “Beyoncé”) while preserving Bangla vowel marks and script text (e.g. “ফিরে দেখা”).
- Ranks exact titles before title prefixes, substrings, all-term matches and extension-only matches; preserves stable saved-order tie handling; does not rewrite audio metadata.
- In Search, shows up to **60** matching offline tracks (previously 16) and up to three direct offline suggestion chips. A typed query with offline matches no longer triggers “no results.”
- `LocalSearchSuggestions` now considers up to **5,000** saved video references instead of the first 400; all suggestions come from real saved metadata, with no network lookup while typing.

### Imported synchronized lyrics

- Offline Library displays **LRC lyrics** next to tracks that have a saved, app-private synchronized LRC entry.
- This remains an imported/user-provided LRC feature. **Live online lyric extraction, automatic copyrighted-lyrics scraping, sound-to-lyric transcription, and cloud-published lyrics are NOT implemented by NW26.**
- The `nightwave_private_lrc_v1` schema, strict LRC parser/size limits, track-ID attachment and per-account optional lyrics backup consent are preserved. No song/lyric file cleanup or index migration.

## Actual execution evidence

The exact GitHub source blobs were matched and compiled locally via kotlinc:
- `DriveHttpFailurePolicy.kt`: `902d1e79ec091b86fd0b7f2fab948b52b54d9764`
- `CloudBackupConsentPolicy.kt`: `8a48ec71ea4b828a007e8b7669e8648d090fa5c2`
- `OfflineSearchRanking.kt`: `040e6284244cc719a2dff25da758dd91e2790a59`
- `NW26ReliabilitySmoke.kt`: `c41fdba4ac1a983db390ae34bacdf7096e910e41`

`kotlinc` generated an executable JVM jar and `java -jar` printed:

```
PASS: NW26 32 exact-production Kotlin Drive/search assertions
LIMIT: Android OAuth, actual Drive upload and UI lyrics not tested
```

JUnit regression suites `DriveHttpFailurePolicyTest`, `CloudBackupConsentPolicyTest`, `OfflineSearchRankingTest`, plus a read-only GitHub source integration preflight `verify_nw26_cloud_search.py` were authored and connected to CI but **not run on Android Gradle**.

## Remaining release checks and external prerequisites

1. GitHub hosted Actions has often failed *before any step*. Check each run's job/step metadata; do not claim this means Kotlin compilation failed or passed.
2. Verify the Android **package name**, **release/debug signing-certificate SHA-1/SHA-256**, **OAuth Android client** and Drive API enablement in Google Cloud Console. These are account-specific external settings and cannot be guaranteed from GitHub source alone. Google OAuth consent test users and any publishing/verification status may matter. **Never send private keys or OAuth tokens**.
3. Test Google account chooser → actual connected status → Back up now → remote download readback → preview restore → reconnect, using the exact APK signer compatible with the installed NightWave app.
4. Confirm toggling history/lyrics/preferences/Wi-Fi or disconnecting during a long backup does not mark a stale snapshot current.
5. Verify local search for English accented and Bangla titles, saved references past the first 400, downloaded tracks past the first 16, and imported LRC badges on device.
6. Test app restart, background WorkManager transfers, font/display size and playback in the consolidated Android device QA round; no unverified APK installation is recommended.

**No NightWave private music, account, personal library, cloud backups or existing installed app was changed in this development session.** This is source development only; full Android Gradle and device certification remain outstanding.
