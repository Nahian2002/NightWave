> **NW125 final candidate update — 27 October 2026:** The complete NW111–NW124 implementation is on `release/nightwave-nw125-final-candidate`; the old `main` still does not contain it. No new NW125 Windows/Android build or phone tests have been executed here. Latest compiled build/test evidence is NW122; the status remains **NO-GO for public APK/Play** until source rights, device/signing continuity, OAuth/backup and security gates are evidenced. The historical release-readiness narrative below refers to earlier branches and may contain superseded version/SDK statements; the current source is compile/target API 36, code 35, `2.0.0-alpha5s`. See [NW125 final candidate](docs/NW125_FINAL_CANDIDATE.md).

> NW122 update — 27 October 2026: measured local search/ranking and library sort improvements preserve ordered results; 1,464 JVM tests and 50 current source checks pass. Host-JVM receipts are separate from Android startup, input-to-results, playback-next, jank, battery and 30-minute soak acceptance, which remain pending. See [NW122 measurement/evidence](docs/NW122_PERFORMANCE_MEASUREMENT.md) and [current test report](TEST_REPORT.md). Physical/account/installed-signer/rights gates remain open; public release remains NO-GO.

> NW121 update — 27 October 2026: premium account/recovery UI and access-state corrections compile; all 1,453 JVM tests and 49 current source checks pass. The real Drive client/codecs pass an isolated synthetic roundtrip; this is not live Google OAuth or phone restore proof. Android production/instrumentation sources, debug assembly and local NW120 signer comparison pass; lint has zero errors. See [current test report](TEST_REPORT.md) and [NW121 evidence](docs/NW121_PREMIUM_ACCOUNT_RECOVERY_EVIDENCE.md). Physical UI/account/signer/Drive and public-release gates remain open.

> NW120 build update — 27 October 2026: complete Windows build, 1,433 JVM tests, 49 structural checks, Android production/instrumentation-source compilation, debug assembly and local NW119 signer comparison pass. Audio-session identity/readback and crossfade EQ corrections are implemented; audible output-route and installed-phone acceptance remain pending. See [current test report](TEST_REPORT.md) and [NW120 evidence](docs/NW120_AUDIO_SESSION_EVIDENCE.md). Public release remains NO-GO. Previous milestone counts below are historical.

> Current build update — 27 October 2026: NW119 locally compiles Android app and instrumentation sources, assembles a debug APK and passes all 1,421 JVM tests. Current structural checks pass 49/49; lint has zero errors (315 warnings remain). See [current test report](TEST_REPORT.md) and [NW119 evidence](docs/NW119_CLEANUP_AND_LYRICS_EVIDENCE.md). Installed-phone signer, device, OAuth/Drive, privacy and rights acceptance remain pending. Public release is still NO-GO. Older statements below are historical unless explicitly reconfirmed.

# NightWave — Release Readiness

**Status: NO-GO for public APK / Google Play. GitHub repository remains private.**  
**Updated on:** 25 October 2026  
**Current integrated development branch:** `feature/nightwave-nw52-final-integration-release-gates` (NW43–NW52), **not** default `main`.

The 10-source-batch roadmap is implemented as stacked code branches, **not** a compiled/released app. The current Android build remains API 35 and version `2.0.0-alpha5s`; a tested Android API 36 migration is mandatory for a 2026 Play phone submission. No green Gradle/JVM/Compose build, real Google authorization, production AAB/signing, external privacy disclosure, historical secret scan, or public-source/media rights approval is confirmed. The app has not been published. Nothing proves arbitrary YouTube songs may legally or technically be stored offline.

**Build this exact branch only for private validation** using [NW52 Final Integration & Acceptance](docs/NW52_FINAL_INTEGRATION.md). Older NW34 release-preparation documents are historical, not proof of NW52 completion.

## Where to begin

- [Public Release Execution Plan](docs/PUBLIC_RELEASE_EXECUTION_PLAN.md) — what is prepared, what's blocked, and safe next actions
- [Evidence-based Release Audit](docs/PUBLIC_RELEASE_READINESS_AUDIT_20261025.md) — detailed risks and source findings
- [Public Data-Flow Inventory](docs/PUBLIC_DATA_FLOW_INVENTORY.md) — local, YouTube, Google Drive, web search and third-party flows
- [Privacy Policy Draft](PRIVACY_POLICY_DRAFT.md) — source-informed only; final legal and runtime verification needed
- [Security Policy](SECURITY.md) — private vulnerability-reporting procedure
- [Dependencies and Rights](docs/THIRD_PARTY_REVIEW.md) — software and media provenance gate
- [Contributing](CONTRIBUTING.md) — data-safe contribution guidelines

## Important distinction

**GitHub source publication** may become reasonable after confidential Git-history review, licensed source review, and cleanup of misleading release claims.

**Public APK/Play release** has separate unresolved gates: YouTube platform/recording permissions, Android API 36, production signing, user-data/privacy disclosure, Google OAuth validation, release build and physical-device tests.

Neither the GPL license nor a user pressing "I have permission" provides YouTube's written approval.

## Local checks (not executed in this connected GitHub edit session)

From a complete checkout:

```powershell
python tools/public_release_preflight.py --self-test
python tools/public_release_preflight.py --json
python tools/release_dependency_inventory.py --self-test
python tools/release_dependency_inventory.py --json
```

On a full git clone with all refs and installed Gitleaks:

```powershell
python tools/public_release_preflight.py --history --json
```

The history scan may write a redacted private report into `dist/`, which is ignored by Git. Keep it private and rotate/remediate any exposed credentials. This scan, even if clean, cannot certify all images/code rights or third-party compliance.

No branch merge, visibility switch, Play submission, APK installation, source app modification or user-data deletion is authorized by this documentation.
