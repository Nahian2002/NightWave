# NightWave continuity instructions

Before choosing or implementing an upgrade, read:

1. `docs/PRODUCT_QUALITY_ROADMAP.md` — active product plan and next batch.
2. `docs/quality/NW126_NW145_BACKLOG.json` — batch status, dependencies and acceptance criteria.
3. `docs/audits/2026-10-28_PRODUCT_QUALITY_AUDIT.md` and `TEST_REPORT.md` — baseline findings and verification limits.

Current review branch `design/nightwave-expressive-themes` adds DESIGN02 to protected DESIGN01 parent `1413f0f3838df497b6c696ccd4243b5a6285916b`. Read docs/design/2026-10-28_THEME_RECORDING_REVIEW.md and docs/design/EXPRESSIVE_THEME_FINISH.md: the owner judged the matte result dull, so theme roles are now navy/cyan, plum/lavender/rose, warm-white/cobalt and black/teal. Do not silently restore the superseded near-gray palette or white-only primary fills. Recorded old Midnight is rated 6.0/10; the attributable DESIGN02 D5 recording is rated 7.6/10 on the same theme-only rubric. Read docs/design/2026-10-28_THEME_VALIDATION_202916.md. All four choices are observed with limited coverage; no overall re-rating or formal device pass is claimed. The container/system-bar follow-up is locally verified; new B017 phone acceptance is pending. NW134 remains next; use the newest cumulative review branch, while `main` remains the canonical reviewed baseline. Preserve older audits/acceptance records and PR dependencies #144 → #145 → #146.

When the owner says "next", select the first eligible incomplete batch in the active backlog. The next source batch is **NW134**; NW126–NW133 are locally verified with physical checks pending. Check the actual checkout, remote updates and the latest ledger first. Do not restart NW111–NW125 simply because historical documents contain old next-step text.

Preserve package/signing identity, installed data, guest profiles, Google consent/account boundaries, playlists, history, local music, original URLs and migration evidence. Inspect caller wiring as well as pure policies; a large synthetic benchmark does not prove the UI supplies the whole library. Do not remove safeguards or tests to improve a score.

For every batch, update its status, findings addressed, exact commit/build evidence, remaining device checks and the next batch in the backlog and roadmap. Keep implementation, JVM/Android compilation, device verification and release approval separate. Independent work may proceed with documented device checks pending; final 9.5 certification requires the actual acceptance evidence.

Ratings are a subjective product-review rubric. Code changes, passing string checks and test counts cannot automatically increase the rating. Re-rate against the same rubric using current screenshots, task outcomes and device results. Preserve the original NightWave identity and declared local-first scope; do not claim Spotify's catalog, device network or service scale.

Use isolated checkpoints and a reviewable branch. Keep private recordings, extracted frames, accounts, credentials, APKs and generated logs outside Git. Retain historical roadmaps and the original 139-case matrix as evidence; the active plan extends them.

## Maintenance continuity

Current source includes maintenance on `main`, based on NW132 `aa6a56fd553a297d5aa790f6049a1f4634a11b63`. Read TEST_REPORT.md and docs/REPOSITORY_MAINTENANCE_20261028.md before more cleanup. Retain historical evidence and useful incremental caches; project housekeeping does not authorize deleting personal folders. NW133 is locally verified on its review branch; NW134 is next.

## Completed source-only publication

Source is public on canonical `main` after explicit owner authorization. Read SOURCE_PUBLICATION.md and .github/security/source-publication-result.json. Publication preserved NW132 maintenance app bytes; later NW133 product changes are separately authorized on their review branch. Actions is deliberately disabled; public secret scanning/push protection, private reporting and main review protections were read back. Owner admin bypass is retained; hosted required checks are not configured. Do not re-enable Actions, publish APKs, or change dependency/provider behavior incidentally. NW133 is locally verified on its review branch; NW134 is next.

## Historical repository publication preparation

The owner explicitly requires no app changes for public-repository preparation. On `audit/nightwave-public-source-preparation`, preserve all `app/`, `tools/`, Gradle/build inputs, package/signing identity and installed data at NW130 parent `fb8f0455c6903249ab85299a76566153a2b8d2a7`. Read [SOURCE_PUBLICATION.md](SOURCE_PUBLICATION.md). Work is limited to repository workflows, documentation and publication controls. Dependency advisories and provider/rights issues must be recorded without silently changing features or libraries. Do not switch visibility, delete/rewrite history or publish binaries as an incidental cleanup step. The preparation ended unchanged at `e476a979491dcb09fa4734e2763c0f357b6c2cd9`. Separately authorized NW131–NW132 product changes follow on their own branches; the active next batch is NW134 after locally verified NW133.
