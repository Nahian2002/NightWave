# NightWave continuity instructions

Before choosing or implementing an upgrade, read:

1. `docs/PRODUCT_QUALITY_ROADMAP.md` — active product plan and next batch.
2. `docs/quality/NW126_NW145_BACKLOG.json` — batch status, dependencies and acceptance criteria.
3. `docs/audits/2026-10-28_PRODUCT_QUALITY_AUDIT.md` and `TEST_REPORT.md` — baseline findings and verification limits.

Current cumulative review branch is `update/nightwave-nw138-sound-studio`, stacked on NW137 draft PR #159. Read docs/NW138_SOUND_STUDIO.md, docs/quality/UPCOMING_UPDATES.md and TEST_REPORT.md. Build NW-debug-B4B205C1BB2D17BA: 1,793 JVM / 221 suites, 78 source checks, lint 0 errors / 322 warnings / 17 hints with unchanged notices. Four new Compose methods compile only; eight new physical cases and all prior gates remain NOT_RUN. Original EQ engine/data/transition/theme/source/package/signer contracts remain unchanged. No new rating or audible verification is inferred.

When the owner says "next", select the first eligible incomplete source batch in backlog execution_order. **NW139 is next**, then NW140–NW145, on a cumulative branch based on the newest reviewed tip. NW138 is locally verified; keep device/runtime/release acceptance separate. Check checkout, remote and latest ledger first; preserve original physical rows/history. Main remains the integrated baseline. Do not overlap another active batch or restart a broad redesign.

Preserve package/signing identity, installed data, guest profiles, Google consent/account boundaries, playlists, history, local music, original URLs and migration evidence. Inspect caller wiring as well as pure policies; a large synthetic benchmark does not prove the UI supplies the whole library. Do not remove safeguards or tests to improve a score.

For every batch, update its status, findings addressed, exact commit/build evidence, remaining device checks and the next batch in the backlog and roadmap. Keep implementation, JVM/Android compilation, device verification and release approval separate. Independent work may proceed with documented device checks pending; final 9.5 certification requires the actual acceptance evidence.

Ratings are a subjective product-review rubric. Code changes, passing string checks and test counts cannot automatically increase the rating. Re-rate against the same rubric using current screenshots, task outcomes and device results. Preserve the original NightWave identity and declared local-first scope; do not claim Spotify's catalog, device network or service scale.

Use isolated checkpoints and a reviewable branch. Keep private recordings, extracted frames, accounts, credentials, APKs and generated logs outside Git. Retain historical roadmaps and the original 139-case matrix as evidence; the active plan extends them.

## Scheduled five-hour completion budget

Before admitting a scheduled batch, read both Codex windows and conservatively estimate the entire remaining update: inspection, implementation, tests and repairs, sequential build/lint, recovery, review and private delivery. Add a reserve of at least 5 percentage points or 25% of estimated work, whichever is larger. Until comparable evidence supports a better estimate, require at least 30% five-hour allowance remaining for a normal full batch, and more for complex or uncertain work. This is a project planning floor, not an OpenAI product limit or a completion guarantee.

Start only when the full completion envelope fits. Insufficient five-hour allowance means skip this run and keep the hourly schedule active for a fresh check after reset; never weaken scope or validation to fit. If either window is unreadable or ordinaryUsageAllowed is false, skip work. Recheck both windows after implementation, before expensive validation/delivery and at least every 10 minutes; re-estimate remaining work and safely checkpoint if it no longer fits. Shared-account changes are observations, not exact per-batch costs. The weekly reserve below still permanently stops the automation at 10% remaining.

## Scheduled weekly usage reserve

Before scheduled upgrades, use get_usage_limits and identify the seven-day Codex window, preferring rateLimitsByLimitId.codex. usedPercent is consumed: stop at usedPercent >= 90 (remaining <= 10). Preserve work, mark incomplete checks honestly, delete automation nightwave-sequential-upgrades and notify; never automatically resume after reset. Recheck after implementation, before validation/publication and at least every 10 minutes of long work. If weekly usage is unavailable, skip scheduled upgrades. This is a periodic shared-account guard, not an instantaneous spending cap. See docs/quality/UPCOMING_UPDATES.md.

## Revised execution schedule — 29 October 2026

Read docs/quality/UPCOMING_UPDATES.md and backlog execution_order first. Latest recorded review is docs/audits/2026-10-29_RECORDING_230548_REVIEW.md: about 8.4 demonstrated usability, 8.8 main visual, 9.0 offline journey; one 2,704,719-byte transfer reports 507ms (~42.678 Mbps), not a universal guarantee. FINISH02 on fix/nightwave-everyday-finish is locally verified from c932daaa (#155); NW139 is next. Hourly Asia/Dhaka thread heartbeat nightwave-sequential-upgrades runs at most one eligible batch and must not overlap unfinished work. Original NW137–NW145 and all physical gates remain. Update actual evidence before advancing; consult execution_order instead of historical next-batch notes below.

## Maintenance continuity

Current source includes maintenance on `main`, based on NW132 `aa6a56fd553a297d5aa790f6049a1f4634a11b63`. Read TEST_REPORT.md and docs/REPOSITORY_MAINTENANCE_20261028.md before more cleanup. Retain historical evidence and useful incremental caches; project housekeeping does not authorize deleting personal folders. NW133 is locally verified on its review branch; NW139 is next; NW134/NW135 physical acceptance remains pending.

## Completed source-only publication

Source is public on canonical `main` after explicit owner authorization. Read SOURCE_PUBLICATION.md and .github/security/source-publication-result.json. Publication preserved NW132 maintenance app bytes; later NW133 product changes are separately authorized on their review branch. Actions is deliberately disabled; public secret scanning/push protection, private reporting and main review protections were read back. Owner admin bypass is retained; hosted required checks are not configured. Do not re-enable Actions, publish APKs, or change dependency/provider behavior incidentally. NW133 is locally verified on its review branch; NW139 is next; NW134/NW135 physical acceptance remains pending.

## Historical repository publication preparation

The owner explicitly requires no app changes for public-repository preparation. On `audit/nightwave-public-source-preparation`, preserve all `app/`, `tools/`, Gradle/build inputs, package/signing identity and installed data at NW130 parent `fb8f0455c6903249ab85299a76566153a2b8d2a7`. Read [SOURCE_PUBLICATION.md](SOURCE_PUBLICATION.md). Work is limited to repository workflows, documentation and publication controls. Dependency advisories and provider/rights issues must be recorded without silently changing features or libraries. Do not switch visibility, delete/rewrite history or publish binaries as an incidental cleanup step. The preparation ended unchanged at `e476a979491dcb09fa4734e2763c0f357b6c2cd9`. Separately authorized NW131–NW132 product changes follow on their own branches; the active next source batch is NW139 after locally verified NW138; physical acceptance remains pending.
