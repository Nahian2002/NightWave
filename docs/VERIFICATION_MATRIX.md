# Current verification guide

**30 October 2026.** Current cumulative NW137 is locally verified; NW138 next. Exact evidence: [TEST_REPORT](../TEST_REPORT.md), [new eight-case supplement](quality/NW137_OWNED_LYRICS_ACCEPTANCE.csv).

| Evidence | Meaning | Limit |
| --- | --- | --- |
| Current source checks | Required structural and broken-fixture checks execute | Not a compiler or runtime test |
| JVM tests | Gradle unit tests execute | Synthetic host behavior; not Android UI/storage/audio |
| Android and test compilation | Production/Compose and instrumentation sources compile; APK assembles | Screen tests are not executed |
| APK/source/certificate | Built bytes match source inputs and protected candidate certificate | Installed signer requires a phone comparison |
| Device acceptance | NOT_RUN for current batches | No inferred rendering, picker, account recovery or audible behavior |
| Hosted CI | Exact final-head annotations in the review | Actions intentionally disabled; no hosted pass claimed |

The [original 139 cases](NW90_EXPANDED_139_CASE_MATRIX.csv) are unchanged. [80 additional cases](quality/NW126_NW145_ACCEPTANCE.csv) record partial local evidence and unrun phone work. Prepared 219-case packs are not executed acceptance runs and do not populate the installed app.

Follow the [active backlog](quality/NW126_NW145_BACKLOG.json), [roadmap](PRODUCT_QUALITY_ROADMAP.md), [readiness gates](../RELEASE_READINESS.md) and [build instructions](BUILD_AND_RELEASE.md). App release remains NO-GO; source-only publication is separate.

The [archived verification narrative](VERIFICATION_MATRIX_HISTORY_20261028.md) preserves the original 19 October failed-build evidence. Its old branch, SDK and test counts are historical.
