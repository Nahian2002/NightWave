# NW77 — Music-source architecture and public-release safety gate

**Cumulative branch:** `feature/nightwave-nw77-source-safety-release-gate`  
**Parent:** `feature/nightwave-nw76-build-certification` (the Windows/phone build reported successful by the owner).  
**Scope:** read-only public-release audit and workflow reporting; **no Android production audio/UI/Google source changes**, no data migrations, no music deletion, no signing changes, no server or subscription.

## What changed

- `tools/nw77_public_release_audit.py`: an offline, standard-library-only source audit and deterministic synthetic test. It inventories identifiable music-origin pathways and stores a JSON report in `dist/NightWave-NW77-public-release-audit.json`.
- `tools/nw77-release-check.ps1` and `check-public-release.cmd`: a Windows one-click source audit (PowerShell 5.1; no Python required). Run it optionally to create `dist/NightWave-NW77-public-release-report.txt`. Its exit status is nonzero by design until public release is safe, **not** a broken development build.
- `.github/workflows/android.yml`: include the new branch and run both synthetic checks plus the source release audit. Attach the JSON NO-GO/review report separately while allowing debug compilation/JUnit to proceed. Hosted CI debug artifacts do not certify Android update compatibility.
- `README.md`: identifies newest source branch and distinguishes connected Google account from proven backup/restore.

## Source inventory and current red flags

| Path | Current evidence | Public release |
|---|---|---|
| User imported local audio | Private library and verified local playback | Retain |
| Direct HTTPS authorized audio | `PermittedAudioDownloadPolicy` excludes YouTube/googlevideo hosts; files verified before offline indexing | Rights and end-to-end QA still required |
| Public-domain/CC0 Commons catalog | `FreeAudioCatalogPolicy.permittedLicense` allows CC0/public domain, with host guard | Validate individual metadata and licenses |
| YouTube search reference | `YouTubeGateway` extracts/searches YouTube references, with community fallback | Needs provider-policy review |
| Background audio from YouTube | `YouTubeGateway.resolve` extracts progressive audio; `PlaybackService` places audio URL in independent Media3 player | **Public release NO-GO without proper rights/authorization or compliant redesign** |
| YouTube-derived offline copy | `SearchTrackSaveWorker` uses `resolveAudioOnlyForOffline` + `downloadResolvedSearchAudio` | **Public release NO-GO without prior written approval/rights or removal** |
| Google Drive optional backup | `NightWaveCloudAuthorization` uses only `drive.appdata` | Test account connected; actual backup/readback and production OAuth status unverified |

**No assertion is made that the user's specific songs are licensed.** Direct-link HTTPS validation is a security property, not a grant of permission. This gate is a source audit, not a legal determination.

Relevant official rules (verified October 2026):
- https://developers.google.com/youtube/terms/developer-policies
- https://developers.google.com/youtube/terms/developer-policies-guide
- https://developers.google.com/workspace/drive/api/guides/api-specific-auth

YouTube's API Services rules restrict downloads/offline copies of audiovisual content and separate/background audio unless appropriately approved. The proper public-release path is to use genuinely permitted or licensed audio for independent player/offline use, and provide authorized compliant YouTube viewing if needed.

## Why the audit cannot say GO even if a path disappears

A code-pattern scan cannot prove content licenses, platform approval, accessibility, Google sign-in production availability, Drive backup integrity or user-device behavior. Therefore results have only **NO_GO** (known blockers) and **REVIEW_REQUIRED** (no known blockers, manual checks still outstanding). It never produces a false success/public APK.

The audit emits only filenames, fixed labels and non-sensitive source features; no music URLs, personal tracks, credentials or tokens. Its `--self-test` verifies known violations are caught, missing source files fail closed, and even clean synthetic code stays `REVIEW_REQUIRED`.

## How to use safely

1. Keep using your successfully built NW76 Android app during private development; NW77 does not require installing a different APK to obtain its report.
2. Optional: download the NW77 cumulative branch ZIP, extract and run `check-public-release.cmd` **without** running an app installation. The command intentionally shows **NO-GO** for the existing prohibited source paths and writes the report. A nonzero exit code means the release check blocked a public APK, not a failed audio transfer.
3. You may run `build-windows.cmd` if you want source parity, but **NW77's build has not been independently tested** in the assistant environment.
4. To finish backing up your Google account safely, on the installed app choose **Back up now**, confirm a truthful verified backup date/status, then **Preview restore** and inspect the preview. Avoid replacing existing library data while only testing.
5. Follow up on reported ~35–37 KB/s permitted audio downloads with controlled same-file browser/source comparison. The source has no intentional 37 KB/s hard cap; the cause is not yet established.
6. Before publishing: redesign/authorize YouTube-derived audio paths; gather rights evidence for third-party media; verify OAuth production status and backup readback; complete release signing/device QA.

## Exit criteria and evidence

**NW77 infrastructure implementation:** source report and Windows/CI guard committed; synthetic code inspections completed, execution of Windows PowerShell, Gradle and device tests still required.

**Public distribution:** NO-GO, independent of debug-build success or individual developer Google authorization. Further source and policy changes are required before public approval. Preserve old users' music and data in any later migration; never silently delete files.

**Next technical product work:** after the source-safety foundation, prioritize preserving an offline-first licensed/direct-audio experience, debugging slow transfers and polishing search metadata. The unresolved public-release blockers must not be hidden by UI improvements.
