# NightWave — Release Readiness

**Status: NO-GO for public APK / Google Play. GitHub repository remains private.**
**Prepared on:** 25 October 2026

The existing Android app is **unchanged** in this release-preparation branch. Nothing in this repository documentation proves that NightWave can legally or technically store every YouTube song for offline playback.

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
