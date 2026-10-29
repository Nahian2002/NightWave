# NightWave continuity instructions

Before choosing or implementing an upgrade, read:

1. `docs/PRODUCT_QUALITY_ROADMAP.md` — active product plan and next batch.
2. `docs/quality/NW126_NW145_BACKLOG.json` — batch status, dependencies and acceptance criteria.
3. `docs/audits/2026-10-28_PRODUCT_QUALITY_AUDIT.md` and `TEST_REPORT.md` — baseline findings and verification limits.

Current cumulative review branch is `fix/nightwave-offline-finish`, OFFLINE_FINISH [draft PR #155](https://github.com/Nahian2002/NightWave/pull/155) stacked on NW136 #154. Read docs/OFFLINE_FINISH_RECORDING_REVIEW.md and TEST_REPORT.md. Owner recording 195906 shows completed save, stale resume help, crowding, lost offline identity/art and playback after removal. Main visual rubric remains 8.8; recorded offline journey pre-fix 7.5; no new score is inferred. Owner-provided second-run receive timings give about 0.448 Mbps, with negligible writes/sync; no aggregate source limit is proven. Transfers now validate a 64 KiB probe then at most four 512 KiB ranges, buffer at most 2 MiB, commit receipt-backed bytes in order, and cancel only owned calls. Controlled paced host comparison: 2.12 vs 8.83 Mbps; not phone certification. Optional metadata keeps known creator/title and existing cached art only; no new image HTTP request. Source/quality, account boundaries, package/signer and original data formats remain. Build FD80578F: 1,747 JVM tests / 216 suites, 73 source checks, lint 0 errors / 317 warnings / 17 hints. Three added style/localization warnings are triaged without suppression. Fifteen new phone cases and all prior gates remain NOT_RUN. NW137 stays next; do not restart a broad redesign or claim 9.5/1 Mbps acceptance from host tests.

When the owner says "next", select the first eligible incomplete source batch in the active backlog. **NW137 is next** on a new cumulative branch based on this reviewed tip; NW126–NW136 are locally verified with physical acceptance pending. Keep the original NW134/NW135/NW136 physical rows NOT_RUN until actual evidence exists. Check checkout, remote and latest ledger first; do not restart historical batches or another broad redesign. Main stays the canonical integrated baseline.

Preserve package/signing identity, installed data, guest profiles, Google consent/account boundaries, playlists, history, local music, original URLs and migration evidence. Inspect caller wiring as well as pure policies; a large synthetic benchmark does not prove the UI supplies the whole library. Do not remove safeguards or tests to improve a score.

For every batch, update its status, findings addressed, exact commit/build evidence, remaining device checks and the next batch in the backlog and roadmap. Keep implementation, JVM/Android compilation, device verification and release approval separate. Independent work may proceed with documented device checks pending; final 9.5 certification requires the actual acceptance evidence.

Ratings are a subjective product-review rubric. Code changes, passing string checks and test counts cannot automatically increase the rating. Re-rate against the same rubric using current screenshots, task outcomes and device results. Preserve the original NightWave identity and declared local-first scope; do not claim Spotify's catalog, device network or service scale.

Use isolated checkpoints and a reviewable branch. Keep private recordings, extracted frames, accounts, credentials, APKs and generated logs outside Git. Retain historical roadmaps and the original 139-case matrix as evidence; the active plan extends them.

## Maintenance continuity

Current source includes maintenance on `main`, based on NW132 `aa6a56fd553a297d5aa790f6049a1f4634a11b63`. Read TEST_REPORT.md and docs/REPOSITORY_MAINTENANCE_20261028.md before more cleanup. Retain historical evidence and useful incremental caches; project housekeeping does not authorize deleting personal folders. NW133 is locally verified on its review branch; NW137 is next; NW134/NW135 physical acceptance remains pending.

## Completed source-only publication

Source is public on canonical `main` after explicit owner authorization. Read SOURCE_PUBLICATION.md and .github/security/source-publication-result.json. Publication preserved NW132 maintenance app bytes; later NW133 product changes are separately authorized on their review branch. Actions is deliberately disabled; public secret scanning/push protection, private reporting and main review protections were read back. Owner admin bypass is retained; hosted required checks are not configured. Do not re-enable Actions, publish APKs, or change dependency/provider behavior incidentally. NW133 is locally verified on its review branch; NW137 is next; NW134/NW135 physical acceptance remains pending.

## Historical repository publication preparation

The owner explicitly requires no app changes for public-repository preparation. On `audit/nightwave-public-source-preparation`, preserve all `app/`, `tools/`, Gradle/build inputs, package/signing identity and installed data at NW130 parent `fb8f0455c6903249ab85299a76566153a2b8d2a7`. Read [SOURCE_PUBLICATION.md](SOURCE_PUBLICATION.md). Work is limited to repository workflows, documentation and publication controls. Dependency advisories and provider/rights issues must be recorded without silently changing features or libraries. Do not switch visibility, delete/rewrite history or publish binaries as an incidental cleanup step. The preparation ended unchanged at `e476a979491dcb09fa4734e2763c0f357b6c2cd9`. Separately authorized NW131–NW132 product changes follow on their own branches; the active next source batch is NW137 after locally verified NW136; physical acceptance remains pending.
