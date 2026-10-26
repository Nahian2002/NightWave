# NW79 — Download & Offline Reliability

**Roadmap:** [Owner-approved NW76–NW90 quality program](CURRENT_CUMULATIVE_ROADMAP.md)  
**Parent:** NW78 cumulative Google backup reliability branch  
**Status:** Source implementation and targeted Kotlin policy smoke complete; Android build, instrumented tests, device downloads and final quality acceptance deferred to NW90.

## Improvements committed

1. **Slow-source timeout:** The authorized, size-limited direct audio downloader previously used a 25-minute whole-call timeout, causing a valid slow stream to fail even while bytes continued arriving. This is now **180 minutes**. The 30-second idle-read timeout, 250 MiB cap, storage reserves, HTTPS/DNS/redirect safety, cancellation, audio-only signature and MediaExtractor validation, checksum checks for licensed catalog media, and atomic private-library import all remain intact. This is **not** a download acceleration mechanism.
2. **Backoff for transient HTTP statuses:** HTTP 408, 425, 429, 500, 502, 503 and 504 trigger a privacy-safe `IOException` containing only the numeric HTTP code. The downloader does **not** immediately repeat such responses: it exits to WorkManager's existing exponential backoff. Permanent denials remain non-retryable. No Retry-After response header, token, URL or cookie is persisted.
3. **Truthful network-complete vs offline-ready state:** The downloader now calls `onVerifying()` only after network bytes finish, before file/hash checks and atomic library import. Catalog, direct-link, and selected searched-audio WorkManager workers persist a boolean `KEY_VERIFYING` with the current byte counts, and their foreground notifications switch to **Checking audio before saving**.
4. **Downloads view:** While a running transfer is in the verifying stage, the Download Center displays **Checking audio · Saving to Offline Library (not ready yet)**, hides transfer throughput/ETA and shows an indeterminate progress indicator. WorkManager `SUCCEEDED` by itself still does not confer `Ready`: the existing file-health check must independently find readable indexed offline audio. Verified local file checks and missing-file recovery remain unchanged.
5. **Safe end-user errors:** WorkManager's final status for transient response codes now shows bounded guidance, including rate limit, temporary source outage or network interruption. The UI never interpolates raw exception text, signed playback URLs, headers or local paths.

## Existing controls retained

- Search-track **Pause** records a user-requested stop and only shows **Paused** after WorkManager reports `CANCELLED`. **Resume restarts from byte zero**; byte-range checkpoints are not supported for these sources.
- Catalog/direct transfer **Cancel** remains available; failed transfers direct users to permitted source selection. There is **no claim of resumable partial downloads** for catalog/direct tracks.
- Transfer attempts/history remain WorkManager-managed. A completed WorkManager task is not treated as playable if its private audio file is missing, corrupt or unindexed.
- No changes to the Google OAuth scope, login, Drive backup, library data format, permissions, signing, application package or multimedia licensing.

## Regression coverage added or expanded

- New `AudioDownloadNetworkPolicyTest`: transient vs permanent HTTP status matrix, privacy-safe exception, bounded long transfer.
- Extended `DownloadCenterViewPolicyTest`: verifying cannot be conflated with offline-ready.
- Extended `DownloadRecoveryPolicyTest`: 408/425/429/5xx statuses map to safe, idempotent user guidance.
- Extended `SearchOfflineFailurePolicyTest`: temporary source failures never expose URL secrets.
- New `tools/NW79DownloadSmoke.kt`: standalone Kotlin smoke over exact pure source policies.

## Evidence

- New production `AudioDownloadNetworkPolicy.kt` matched its GitHub blob hash `cd32346f05e8d9fbc52cd640983af814e5a77b22` in a local file copy and compiled with available `kotlinc`; the standalone retry-classification smoke returned **NW79_NETWORK_POLICY_SMOKE_PASS**.
- The full project's Gradle/JUnit/Android instrumentation, Windows preflight, signing, long permitted-source transfers and Samsung acceptance have **not** been run or claimed for this branch.
- The previous reported approximately **35–37 KB/s** download rate is an observation, not a proven app throughput cap. Root cause cannot be established without a controlled same-source network test. NW79 prevents one premature timeout and improves retry semantics; it does not certify faster bytes per second.
- The public-release rights issue flagged by NW77 remains **NO-GO**. These fixes do not approve YouTube-derived offline copies.

## Final NW90 acceptance (owner-requested single test session)

Use the new cumulative NW79+ source once the remaining NW80–89 upgrades are stacked. Run current clean Gradle/JVM/Android tests, update-signature checks and device validation: interrupted/cancelled transfers, WorkManager delayed retry, HTTP simulated statuses, >25-minute progressing permitted files where practical, unknown content length, mismatched checksums, low storage, malicious redirects, finalization states, dead/missing files, flight-mode playback of previously verified permitted audio, and no deleted playlists or Google account regression.

NW79 is **source-implemented**; it is **not** a completed device-certified quality gate.
