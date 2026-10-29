# NightWave readiness

**30 October 2026 — development candidate; app release NO-GO.** Integrated source is [NW132 plus maintenance](https://github.com/Nahian2002/NightWave/tree/main). Current cumulative NW137 is on `update/nightwave-nw137-owned-lyrics`, [draft PR #159](https://github.com/Nahian2002/NightWave/pull/159), stacked on #158, locally verified with physical acceptance pending. [NW138 is next in the revised sequence](docs/quality/UPCOMING_UPDATES.md); six-hour follow-ups do not waive release gates.

Exact local JVM, Android/Compose, instrumentation-source, assembly, source, lint and artifact/signing evidence: [TEST_REPORT](TEST_REPORT.md). Compiled screen tests are not executed phone acceptance. Hosted runner allocation is reported separately in the review.

| Open gate | Required evidence |
| --- | --- |
| Installed update/data | Match installed signer; explicitly permitted in-place update preserves library, profiles, queue, owned audio and account boundaries. Never uninstall/reset to force compatibility. |
| Product/accessibility | Actual tasks, captures, contrast, large text, TalkBack/Switch Access, adaptive layouts and failure recovery. |
| Playback/performance | Audible queue/route/background behavior, measured Android startup/search/jank and matched playback soaks. Host timings remain separate. |
| Recovery/accounts | Disposable actual backup/readback/preview/restore/merge with stale-account/consent and storage/network failure checks. |
| Privacy/rights/security | Applicable source/provider/media rights, privacy disclosures, dependency advisories and distribution/security review. |

The [active roadmap](docs/PRODUCT_QUALITY_ROADMAP.md), [backlog](docs/quality/NW126_NW145_BACKLOG.json), [original 139 cases](docs/NW90_EXPANDED_139_CASE_MATRIX.csv) and [80 added cases](docs/quality/NW126_NW145_ACCEPTANCE.csv) retain pending work. The score remains provisional 6.7/10.

Source visibility is separate from distributing an APK or submitting to a store. Source-only publication has no product-score or Play-submission prerequisite. Follow [SOURCE_PUBLICATION](SOURCE_PUBLICATION.md), [operator checks](docs/publication/OPERATOR_CHECKLIST.md), [dependency review](docs/publication/DEPENDENCY_REVIEW.md) and [source-use notes](TERMS_AND_SOURCE_USE.md). This maintenance changes no visibility, account settings or publication approval.

Historical readiness narratives remain in dated batch reports and Git history. Use this page for current gates and the test report for current measurements.
