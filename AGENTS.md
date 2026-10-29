# NightWave continuity instructions

Before choosing or implementing an upgrade, read:

1. `docs/PRODUCT_QUALITY_ROADMAP.md` — active product plan and next batch.
2. `docs/quality/NW126_NW145_BACKLOG.json` — batch status, dependencies and acceptance criteria.
3. `docs/audits/2026-10-28_PRODUCT_QUALITY_AUDIT.md` and `TEST_REPORT.md` — baseline findings and verification limits.

Current cumulative review branch is `update/nightwave-nw135-offline-journey`, draft PR #152 stacked on NW134 #151 and DESIGN06 #150 (earlier #144 -> #145 -> #146 -> #147 -> #148 -> #149 remain). Read docs/NW135_OFFLINE_IMPORT_READINESS.md and TEST_REPORT.md. NW135 probes actual audio track/sample before owned import rename/index commit, exposes bounded import stages/results, simplifies only truly empty readable Library, adds read-only Check audio/recovery and explicit source choices, and refreshes index changes across recreation. All formats/features/themes, origins, schemas, provider restrictions, data and prior queue/Search/transition/consent boundaries remain. Build 2D35C8B6 passes 1,688 JVM tests and 70 source checks; five new Android cases and the existing 612-case theme matrix compile only. Actual SAF picker, airplane-mode playback/seek/restart, storage/grant/decoder/device acceptance and prior gates remain NOT_RUN. A track/sample probe is not a full decode or integrity certificate; metadata-only availability is not a new audio check. Latest recorded visual score remains 8.8 from 030542; original 6.7 whole-product audit stays frozen.

When the owner says "next", select the first eligible incomplete source batch in the active backlog. **NW136 is next** on a new cumulative branch based on this reviewed tip; NW126-NW135 are locally verified with physical acceptance pending. Keep the original NW134/NW135 physical rows NOT_RUN until actual evidence exists. Check checkout, remote and latest ledger first; do not restart historical batches or another broad redesign. Main stays the canonical integrated baseline.

Preserve package/signing identity, installed data, guest profiles, Google consent/account boundaries, playlists, history, local music, original URLs and migration evidence. Inspect caller wiring as well as pure policies; a large synthetic benchmark does not prove the UI supplies the whole library. Do not remove safeguards or tests to improve a score.

For every batch, update its status, findings addressed, exact commit/build evidence, remaining device checks and the next batch in the backlog and roadmap. Keep implementation, JVM/Android compilation, device verification and release approval separate. Independent work may proceed with documented device checks pending; final 9.5 certification requires the actual acceptance evidence.

Ratings are a subjective product-review rubric. Code changes, passing string checks and test counts cannot automatically increase the rating. Re-rate against the same rubric using current screenshots, task outcomes and device results. Preserve the original NightWave identity and declared local-first scope; do not claim Spotify's catalog, device network or service scale.

Use isolated checkpoints and a reviewable branch. Keep private recordings, extracted frames, accounts, credentials, APKs and generated logs outside Git. Retain historical roadmaps and the original 139-case matrix as evidence; the active plan extends them.

## Maintenance continuity

Current source includes maintenance on `main`, based on NW132 `aa6a56fd553a297d5aa790f6049a1f4634a11b63`. Read TEST_REPORT.md and docs/REPOSITORY_MAINTENANCE_20261028.md before more cleanup. Retain historical evidence and useful incremental caches; project housekeeping does not authorize deleting personal folders. NW133 is locally verified on its review branch; NW136 is next; NW134/NW135 physical acceptance remains pending.

## Completed source-only publication

Source is public on canonical `main` after explicit owner authorization. Read SOURCE_PUBLICATION.md and .github/security/source-publication-result.json. Publication preserved NW132 maintenance app bytes; later NW133 product changes are separately authorized on their review branch. Actions is deliberately disabled; public secret scanning/push protection, private reporting and main review protections were read back. Owner admin bypass is retained; hosted required checks are not configured. Do not re-enable Actions, publish APKs, or change dependency/provider behavior incidentally. NW133 is locally verified on its review branch; NW136 is next; NW134/NW135 physical acceptance remains pending.

## Historical repository publication preparation

The owner explicitly requires no app changes for public-repository preparation. On `audit/nightwave-public-source-preparation`, preserve all `app/`, `tools/`, Gradle/build inputs, package/signing identity and installed data at NW130 parent `fb8f0455c6903249ab85299a76566153a2b8d2a7`. Read [SOURCE_PUBLICATION.md](SOURCE_PUBLICATION.md). Work is limited to repository workflows, documentation and publication controls. Dependency advisories and provider/rights issues must be recorded without silently changing features or libraries. Do not switch visibility, delete/rewrite history or publish binaries as an incidental cleanup step. The preparation ended unchanged at `e476a979491dcb09fa4734e2763c0f357b6c2cd9`. Separately authorized NW131–NW132 product changes follow on their own branches; the active next source batch is NW136 after locally verified NW135; physical acceptance remains pending.
