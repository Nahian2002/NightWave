# NightWave — repository navigation

**Current implementation baseline:** cumulative NW132 clear Library on [`update/nightwave-nw132-clear-library`](https://github.com/Nahian2002/NightWave/tree/update/nightwave-nw132-clear-library). See [NW132 evidence and device protocol](NW132_CLEAR_LIBRARY.md). **Next source batch: NW133.** GitHub default `main` is an older app; NW126–NW132 physical acceptance remains open.

## Start here

| Purpose | Source |
| --- | --- |
| Source-publication preparation; app unchanged | [Publication status](../SOURCE_PUBLICATION.md), [source-use information](../TERMS_AND_SOURCE_USE.md), [dependency review](publication/DEPENDENCY_REVIEW.md) |
| Project overview and limitations | [README](../README.md) |
| **Final Windows build & phone test** | [NW125 final candidate guide](NW125_FINAL_CANDIDATE.md) |
| **Active phase plan and true development gates** | [NW126–NW145 roadmap](PRODUCT_QUALITY_ROADMAP.md), [structured backlog](quality/NW126_NW145_BACKLOG.json), [acceptance cases](quality/NW126_NW145_ACCEPTANCE.csv) |
| Current build identity and acceptance baseline | [NW126 evidence](NW126_BUILD_IDENTITY_BASELINE.md) |
| Latest compact hierarchy and build evidence | [NW127 evidence](NW127_COMPACT_VISUAL_HIERARCHY.md) |
| Shared offline shell and latest build evidence | [NW128 evidence](NW128_UNIFIED_OFFLINE_SHELL.md) |
| Complete local search and latest build evidence | [NW129 evidence](NW129_COMPLETE_LOCAL_SEARCH.md) |
| Decidable public results and latest evidence | [NW130 evidence](NW130_DECIDABLE_PUBLIC_RESULTS.md) |
| Purposeful Home and latest evidence | [NW131 evidence](NW131_PURPOSEFUL_HOME.md) |
| Clear Library identity and latest evidence | [NW132 evidence](NW132_CLEAR_LIBRARY.md) |
| Detailed current product rating and source/video findings | [28 October audit](audits/2026-10-28_PRODUCT_QUALITY_AUDIT.md) |
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
- **Current source wave:** NW132 clear Library follows NW131 and completed repository-only publication preparation. NW126–NW132 are locally verified with physical acceptance pending; `main` does not contain these waves. NW133 is next. Consult the structured backlog and current test report.

Current automated workflow: `.github/workflows/current-android.yml`; `android.yml` remains a manual historical diagnostic. See [historic check inventory](HISTORICAL_CI_SOURCE_CHECKS_20261027.md).

## Historical material — preserve, don't mistake for current status

- `docs/CURRENT_CUMULATIVE_ROADMAP.md`: historical NW76–NW90 plan. Despite its old filename, it is **not** the current roadmap.
- `docs/README_LEGACY_PHASE_NOTES.md`: intentionally retained pre-audit phase notes; old branch/ZIP claims are **not** proof of the current APK.
- `docs/NIGHTWAVE_*.md`, `docs/NW*.md`, and `docs/NIGHTWAVE_V2_*.md`: historic per-phase designs, failure reports, test receipts and implementation notes. Preserve until referential-dependency and audit-retention checks are complete.
- `tools/verify_*.py`, `tools/nw*.py`, `tools/*Smoke.kt`, and `tools/check-*.ps1`: historical source/regression checks. Some are referenced by CI and Windows launchers; **never batch-delete by age/name**.
- `run-nw90-final.cmd`, `run-nw91-final.cmd`: preserved historical Windows acceptance entrypoints. For the next release use the current build guide and review these tools' exact SHA compatibility.

## Safe housekeeping conventions

Keep generated Android `build/`, local `.gradle/`, IDE settings, archives, diagnostic logs/dumps, local secrets, APKs and signing keys **outside Git**. `.gitignore` prevents future accidental adds but does not untrack history. Do not store account credentials, OAuth tokens, private playlists or media files in GitHub.

**Build status:** cumulative app build and all 1,546 JVM tests pass; current runner passes 57 checks including the packaged build identity verifier and baseline-fixture tests. See [current verification](../TEST_REPORT.md). **Release status:** NO-GO pending physical-device validation, installed signer, live restore, source rights and privacy checks. A short recording or passing source checks do not certify the full app.
