# NightWave — repository navigation

**Current implementation:** NETWORK01 on [`fix/nightwave-network-throughput`](https://github.com/Nahian2002/NightWave/tree/fix/nightwave-network-throughput), [draft PR #153](https://github.com/Nahian2002/NightWave/pull/153) stacked on NW135 #152. [Transport evidence](NETWORK_TRANSPORT_THROUGHPUT.md), [ten-case supplement](quality/NETWORK_THROUGHPUT_ACCEPTANCE.csv), [verification](../TEST_REPORT.md). Build 3733A8B0 phone throughput/audio acceptance NOT_RUN. **NW136 remains next.** Earlier reviews/audits are retained.

## Start here

| Purpose | Source |
| --- | --- |
| Source-publication preparation; app unchanged | [Publication status](../SOURCE_PUBLICATION.md), [source-use information](../TERMS_AND_SOURCE_USE.md), [dependency review](publication/DEPENDENCY_REVIEW.md) |
| Latest cleanup, navigation repair and deleted-file inventory | [Maintenance report](REPOSITORY_MAINTENANCE_20261028.md) |
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
| Four-theme matte design and acceptance | [DESIGN01](design/MATTE_DESIGN_SYSTEM.md), [336-case ledger](design/MATTE_DESIGN_ACCEPTANCE.csv) |
| Media transport investigation | [NETWORK01 evidence](NETWORK_TRANSPORT_THROUGHPUT.md) |
| Owned import and offline readiness | [NW135 evidence](NW135_OFFLINE_IMPORT_READINESS.md) |
| Queue/transport and UI finishing evidence | [NW134 evidence](NW134_QUEUE_TRANSPORT.md) |
| Player artwork, state and seek evidence | [NW133 evidence](NW133_PLAYER_ARTWORK_METADATA.md) |
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
- **Current source wave:** NETWORK01 follows locally verified NW135; NW136 remains planned. Phone bandwidth/audio/resource and original gates remain pending.

Current automated workflow: `.github/workflows/current-android.yml`; `android.yml` remains a manual historical diagnostic. See [historic check inventory](HISTORICAL_CI_SOURCE_CHECKS_20261027.md).

## Historical material — preserve, don't mistake for current status

- `docs/CURRENT_CUMULATIVE_ROADMAP.md`: historical NW76–NW90 plan. Despite its old filename, it is **not** the current roadmap.
- `docs/README_LEGACY_PHASE_NOTES.md`: intentionally retained pre-audit phase notes; old branch/ZIP claims are **not** proof of the current APK.
- `docs/NIGHTWAVE_*.md`, `docs/NW*.md`, and `docs/NIGHTWAVE_V2_*.md`: historic per-phase designs, failure reports, test receipts and implementation notes. Preserve until referential-dependency and audit-retention checks are complete.
- `tools/verify_*.py`, `tools/nw*.py`, `tools/*Smoke.kt`, and `tools/check-*.ps1`: historical source/regression checks. Some are referenced by CI and Windows launchers; **never batch-delete by age/name**.
- `run-nw90-final.cmd`, `run-nw91-final.cmd`: preserved historical Windows acceptance entrypoints. For the next release use the current build guide and review these tools' exact SHA compatibility.

## Safe housekeeping conventions

Keep generated Android `build/`, local `.gradle/`, IDE settings, archives, diagnostic logs/dumps, local secrets, APKs and signing keys **outside Git**. `.gitignore` prevents future accidental adds but does not untrack history. Do not store account credentials, OAuth tokens, private playlists or media files in GitHub.

**Build status:** exact current results and hashes are in TEST_REPORT.md; the NW132 baseline has 1,573 executed JVM tests and 59 checks. Screen-test compilation is separate from phone execution. See [current verification](../TEST_REPORT.md). **Release status:** NO-GO pending physical-device validation, installed signer, live restore, source rights and privacy checks. A short recording or passing source checks do not certify the full app.

Latest follow-up: [four-theme ratings, chrome polish and Search input ownership](design/2026-10-28_THEME_POLISH_211137.md).

Latest visual correction: [mood artwork containment and 8.2 appearance review](design/2026-10-28_MOOD_THEME_FINISH_230959.md); device confirmation remains pending.
