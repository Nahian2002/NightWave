# NightWave continuity instructions

Before choosing or implementing an upgrade, read:

1. `docs/PRODUCT_QUALITY_ROADMAP.md` — active product plan and next batch.
2. `docs/quality/NW126_NW145_BACKLOG.json` — batch status, dependencies and acceptance criteria.
3. `docs/audits/2026-10-28_PRODUCT_QUALITY_AUDIT.md` and `TEST_REPORT.md` — baseline findings and verification limits.

Current cumulative review branch is `fix/nightwave-everyday-finish`, FINISH02 [draft PR #156](https://github.com/Nahian2002/NightWave/pull/156) stacked on #155. Read docs/quality/FINISH02_EVERYDAY_CLARITY.md, docs/quality/UPCOMING_UPDATES.md and TEST_REPORT.md. Build 9457716D: 1,754 JVM tests / 217 suites, 74 source checks, lint 0 errors / 317 warnings / 17 hints. All eight new phone cases and prior physical gates remain NOT_RUN. Latest recorded usability is about 8.4, main visual 8.8 and offline journey 9.0; no new score is inferred from source checks.

When the owner says "next", select the first eligible incomplete source batch in backlog execution_order. **ART02 is next**, then SEARCH02 and NW137–NW145, on a cumulative branch based on the newest reviewed tip. FINISH02 is locally verified. Check checkout, remote and latest ledger first; retain original physical rows and historical evidence. Main remains the integrated baseline. Do not overlap another active batch or restart a broad redesign.

Preserve package/signing identity, installed data, guest profiles, Google consent/account boundaries, playlists, history, local music, original URLs and migration evidence. Inspect caller wiring as well as pure policies; a large synthetic benchmark does not prove the UI supplies the whole library. Do not remove safeguards or tests to improve a score.

For every batch, update its status, findings addressed, exact commit/build evidence, remaining device checks and the next batch in the backlog and roadmap. Keep implementation, JVM/Android compilation, device verification and release approval separate. Independent work may proceed with documented device checks pending; final 9.5 certification requires the actual acceptance evidence.

Ratings are a subjective product-review rubric. Code changes, passing string checks and test counts cannot automatically increase the rating. Re-rate against the same rubric using current screenshots, task outcomes and device results. Preserve the original NightWave identity and declared local-first scope; do not claim Spotify's catalog, device network or service scale.

Use isolated checkpoints and a reviewable branch. Keep private recordings, extracted frames, accounts, credentials, APKs and generated logs outside Git. Retain historical roadmaps and the original 139-case matrix as evidence; the active plan extends them.

## Revised execution schedule — 29 October 2026

Read docs/quality/UPCOMING_UPDATES.md and backlog execution_order first. Latest recorded review is docs/audits/2026-10-29_RECORDING_230548_REVIEW.md: about 8.4 demonstrated usability, 8.8 main visual, 9.0 offline journey; one 2,704,719-byte transfer reports 507ms (~42.678 Mbps), not a universal guarantee. FINISH02 on fix/nightwave-everyday-finish is locally verified from c932daaa (#155); ART02 is next. Hourly Asia/Dhaka thread heartbeat nightwave-sequential-upgrades runs at most one eligible batch and must not overlap unfinished work. Original NW137–NW145 and all physical gates remain. Update actual evidence before advancing; consult execution_order instead of historical next-batch notes below.

## Maintenance continuity

Current source includes maintenance on `main`, based on NW132 `aa6a56fd553a297d5aa790f6049a1f4634a11b63`. Read TEST_REPORT.md and docs/REPOSITORY_MAINTENANCE_20261028.md before more cleanup. Retain historical evidence and useful incremental caches; project housekeeping does not authorize deleting personal folders. NW133 is locally verified on its review branch; ART02 is next; NW134/NW135 physical acceptance remains pending.

## Completed source-only publication

Source is public on canonical `main` after explicit owner authorization. Read SOURCE_PUBLICATION.md and .github/security/source-publication-result.json. Publication preserved NW132 maintenance app bytes; later NW133 product changes are separately authorized on their review branch. Actions is deliberately disabled; public secret scanning/push protection, private reporting and main review protections were read back. Owner admin bypass is retained; hosted required checks are not configured. Do not re-enable Actions, publish APKs, or change dependency/provider behavior incidentally. NW133 is locally verified on its review branch; ART02 is next; NW134/NW135 physical acceptance remains pending.

## Historical repository publication preparation

The owner explicitly requires no app changes for public-repository preparation. On `audit/nightwave-public-source-preparation`, preserve all `app/`, `tools/`, Gradle/build inputs, package/signing identity and installed data at NW130 parent `fb8f0455c6903249ab85299a76566153a2b8d2a7`. Read [SOURCE_PUBLICATION.md](SOURCE_PUBLICATION.md). Work is limited to repository workflows, documentation and publication controls. Dependency advisories and provider/rights issues must be recorded without silently changing features or libraries. Do not switch visibility, delete/rewrite history or publish binaries as an incidental cleanup step. The preparation ended unchanged at `e476a979491dcb09fa4734e2763c0f357b6c2cd9`. Separately authorized NW131–NW132 product changes follow on their own branches; the active next source batch is ART02 after locally verified NW136; physical acceptance remains pending.
