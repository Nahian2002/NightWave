# NightWave — repository navigation

**Current development wave:** NW119 lyrics and cumulative build/organization recovery, on `update/nightwave-cleanup-nw119-lyrics`. This index is a navigation aid, not a release certificate.

## Start here

| Purpose | Source |
| --- | --- |
| Project overview and limitations | [README](../README.md) |
| **Active phase plan and true development gates** | [NW111–NW125 roadmap](NW111_NW125_SPOTIFY_QUALITY_ROADMAP.md) |
| Windows build, Android signing and release procedure | [Build and release](BUILD_AND_RELEASE.md) |
| Release risks and blockers | [Release readiness](../RELEASE_READINESS.md) |
| Exact test status and unrun acceptance cases | [Test report](../TEST_REPORT.md), [Verification matrix](VERIFICATION_MATRIX.md) |
| Responsible vulnerability reporting | [Security](../SECURITY.md) |
| Contribution rules | [Contributing](../CONTRIBUTING.md) |
| Privacy wording still requiring review | [Privacy policy draft](../PRIVACY_POLICY_DRAFT.md) |
| Repository cleanliness / tracked-file audit | [NW118 hygiene audit](NW118_REPOSITORY_HYGIENE_AUDIT.md) |
| Windows commands and historical smoke checks | [Tools navigation](../tools/README.md) |

## Source of truth

- **Source:** `app/src/main/java/` (production app); `app/src/main/res/` (Android resources); `app/src/main/AndroidManifest.xml` (permissions/components).
- **JVM tests:** `app/src/test/`; **device tests:** `app/src/androidTest/`. Pure CLI smoke scripts are useful preflights, never substitutes for these suites.
- **Build rules:** root `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, and `app/build.gradle.kts`; **CI:** `.github/workflows/`. Do not delete or alter SDK/signing checks for cosmetic cleanup.
- **Room schema/migrations and settings:** Treat all as migration evidence; never delete without a verified backwards-compatibility and restore audit.
- **Current source wave:** NW119 update branch, based on exact NW118 parent `64160f1388b6dcef6439f384602d1a7c9a6c63d8`. Branches are incremental. Do not assume `main` contains any of these updates.

Current automated workflow: `.github/workflows/current-android.yml`; `android.yml` remains a manual historical diagnostic. See [historic check inventory](HISTORICAL_CI_SOURCE_CHECKS_20261027.md).

## Historical material — preserve, don't mistake for current status

- `docs/CURRENT_CUMULATIVE_ROADMAP.md`: historical NW76–NW90 plan. Despite its old filename, it is **not** the current roadmap.
- `docs/README_LEGACY_PHASE_NOTES.md`: intentionally retained pre-audit phase notes; old branch/ZIP claims are **not** proof of the current APK.
- `docs/NIGHTWAVE_*.md`, `docs/NW*.md`, and `docs/NIGHTWAVE_V2_*.md`: historic per-phase designs, failure reports, test receipts and implementation notes. Preserve until referential-dependency and audit-retention checks are complete.
- `tools/verify_*.py`, `tools/nw*.py`, `tools/*Smoke.kt`, and `tools/check-*.ps1`: historical source/regression checks. Some are referenced by CI and Windows launchers; **never batch-delete by age/name**.
- `run-nw90-final.cmd`, `run-nw91-final.cmd`: preserved historical Windows acceptance entrypoints. For the next release use the current build guide and review these tools' exact SHA compatibility.

## Safe housekeeping conventions

Keep generated Android `build/`, local `.gradle/`, IDE settings, archives, diagnostic logs/dumps, local secrets, APKs and signing keys **outside Git**. `.gitignore` prevents future accidental adds but does not untrack history. Do not store account credentials, OAuth tokens, private playlists or media files in GitHub.

**Build status:** current Android/JVM build passes (1,421 tests). See [NW119 evidence](NW119_CLEANUP_AND_LYRICS_EVIDENCE.md). **Release status:** NO-GO pending physical-device validation, installed signer, Drive read-back/restore, music-source rights and privacy checks. A committed PR and passing source string checks are not runtime QA.
