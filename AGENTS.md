# Current owner request: NightWave post-v55 multi-source roadmap (2026-11-06)

**PRIORITY OVERRIDE FOR THIS FEATURE TRACK:** The owner has requested **THREE mega-batches only**, `NW56`, `NW57`, `NW58`, implementing zero-billing multi-source discovery, unified playback/library experience, and S24 Ultra/release QA. **These are not the historical NW56–NW58 internal batches.** This roadmap has been **documented only, not coded or tested**.

Before starting *any* new multi-source work read, in order:
1. [Master execution contract](docs/MULTISOURCE_NW56_NW58_MASTER_PLAN.md) — exact v55 source tag, allowed provider capabilities, zero-cost terms and merged scopes.
2. [46-case acceptance matrix](docs/quality/MULTISOURCE_NW56_NW58_ACCEPTANCE.md) — blocking evidence requirements.
3. [Windows + connected S24 Ultra device runbook](docs/MULTISOURCE_S24U_DEVICE_QA.md) — real ADB discovery, safe signing/data preflight, build, instrumentation and regression.
4. [Machine-readable batch ledger](docs/quality/MULTISOURCE_NW56_NW58_STATUS.json) — batch state, blockers, source proof and next work.

**Critical lineage:** Public release `nightwave-v55` source is commit `c569de64cdd618fb6e5593e9b6bc778903a7588e`. The default `main` history diverges from v55; NEVER implement on `main` merely because it is default, or apply an older historical NW56–58 document. Recheck for a newer authorized release, then base feature implementation on verified v55 or a verified descendant, on a reviewable branch. Do not overwrite/revert the current public release.

**When owner says "start/next" for the multi-source project:** start the first PLANNED eligible mega-batch (`NW56` first), complete the whole batch with host tests and device gates, and record actual evidence. A connected S24 Ultra is a testing opportunity **not** a claim of connected/authorized device or permission to wipe, sign with a different key, alter accounts or install before verified backup. Keep all required infrastructure **৳0/month**. No paid API/CI, no prohibited scraping, no false in-app/offline playback claims, no regression, no public release or update-feed edit without explicit release go-ahead. Block incompatible providers honestly.

The historical product-quality roadmap and NW133 directions below are preserved as evidence for a **different, earlier internal track**; they do not supersede this explicit owner request.

---

# NightWave continuity instructions

Before choosing or implementing an upgrade, read:

1. `docs/PRODUCT_QUALITY_ROADMAP.md` â€” active product plan and next batch.
2. `docs/quality/NW126_NW145_BACKLOG.json` â€” batch status, dependencies and acceptance criteria.
3. `docs/audits/2026-10-28_PRODUCT_QUALITY_AUDIT.md` and `TEST_REPORT.md` â€” baseline findings and verification limits.

The current cumulative implementation includes NW132 clear Library, based on protected NW131 parent `e80378f1e5c0bc1f81970afd5d4a70c9ce42f2d9`. Read the latest backlog plus NW126â€“NW132 evidence; physical acceptance is pending. `main` is now the canonical reviewed cumulative source. Do not merge an old v0.8 tag or historical branch over it.

When the owner says "next", select the first eligible incomplete batch in the active backlog. The next source batch is **NW133**; NW126â€“NW132 are locally verified with physical checks pending. Check the actual checkout, remote updates and the latest ledger first. Do not restart NW111â€“NW125 simply because historical documents contain old next-step text.

Preserve package/signing identity, installed data, guest profiles, Google consent/account boundaries, playlists, history, local music, original URLs and migration evidence. Inspect caller wiring as well as pure policies; a large synthetic benchmark does not prove the UI supplies the whole library. Do not remove safeguards or tests to improve a score.

For every batch, update its status, findings addressed, exact commit/build evidence, remaining device checks and the next batch in the backlog and roadmap. Keep implementation, JVM/Android compilation, device verification and release approval separate. Independent work may proceed with documented device checks pending; final 9.5 certification requires the actual acceptance evidence.

Ratings are a subjective product-review rubric. Code changes, passing string checks and test counts cannot automatically increase the rating. Re-rate against the same rubric using current screenshots, task outcomes and device results. Preserve the original NightWave identity and declared local-first scope; do not claim Spotify's catalog, device network or service scale.

Use isolated checkpoints and a reviewable branch. Keep private recordings, extracted frames, accounts, credentials, APKs and generated logs outside Git. Retain historical roadmaps and the original 139-case matrix as evidence; the active plan extends them.

## Maintenance continuity

Current source includes maintenance on `main`, based on NW132 `aa6a56fd553a297d5aa790f6049a1f4634a11b63`. Read TEST_REPORT.md and docs/REPOSITORY_MAINTENANCE_20261028.md before more cleanup. Retain historical evidence and useful incremental caches; project housekeeping does not authorize deleting personal folders. NW133 remains next.

## Completed source-only publication

Source is public on canonical `main` after explicit owner authorization. Read SOURCE_PUBLICATION.md and .github/security/source-publication-result.json. App bytes remain NW132 maintenance. Actions is deliberately disabled; public secret scanning/push protection, private reporting and main review protections were read back. Owner admin bypass is retained; hosted required checks are not configured. Do not re-enable Actions, publish APKs, or change dependency/provider behavior incidentally. NW133 remains next.

## Historical repository publication preparation

The owner explicitly requires no app changes for public-repository preparation. On `audit/nightwave-public-source-preparation`, preserve all `app/`, `tools/`, Gradle/build inputs, package/signing identity and installed data at NW130 parent `fb8f0455c6903249ab85299a76566153a2b8d2a7`. Read [SOURCE_PUBLICATION.md](SOURCE_PUBLICATION.md). Work is limited to repository workflows, documentation and publication controls. Dependency advisories and provider/rights issues must be recorded without silently changing features or libraries. Do not switch visibility, delete/rewrite history or publish binaries as an incidental cleanup step. The preparation ended unchanged at `e476a979491dcb09fa4734e2763c0f357b6c2cd9`. Separately authorized NW131â€“NW132 product changes follow on their own branches; the next product batch is NW133.
