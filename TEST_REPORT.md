# Current verification — artwork sizing and pager regression correction

29 October 2026. Existing [draft PR #147](https://github.com/Nahian2002/NightWave/pull/147), `design/nightwave-content-finish`, stacked on #146. Protected previous candidate review `67c35cdca371d805d59284aa77b8ec6729efaf3c`; correction implementation `ff5000124bb80abe9735fbbaf52e965ededba342`. [Owner recording, full scorecard, source gaps and correction](docs/design/2026-10-29_ARTWORK_PAGER_REVIEW.md). [Prior 4041 verification](https://github.com/Nahian2002/NightWave/blob/67c35cdca371d805d59284aa77b8ec6729efaf3c/TEST_REPORT.md) and earlier audits/acceptance remain historical evidence.

The new approximately 127.4956-second owner recording supplies 127 decoded one-second samples (0–126s, 127s failed), overview and detailed inspection. It clearly shows tiny previews in large frames and split destinations across more than one theme. No unique inspected binary identity is exposed. On the same five-dimension rubric the visible UI is **7.8/10** (identity 8.5, color 9, hierarchy 6, coherence 7.5, perceived legibility 8). Diagnostic art/navigation scores are around 4/5; mood composition and music-first Search remain around 8.5. These are recorded subjective appearance judgments, not a current whole-product or new-candidate score. Original 6.7 whole-product audit remains frozen.

The prior Inside/native-size decision was too strict and is superseded by **ContentScale.Fit** for large previews: proportional frame filling with complete image edges, while retaining existing bounded same-source candidates/cache/fallback/identity checks. This can enlarge source pixels and does not recreate absent detail. Featured mosaics are 88dp; paired art uses a bounded 1.6:1 banner. All four thumbnails, sources and actions remain.

Horizontal shelf boundaries consume only leftover horizontal scroll/fling deltas. The pager has a shared 100ms idle fractional-offset recovery that excludes active drag/animation and cancels stale work. Navigation selection/focus follows settledPage; selected-tab taps recenter, and the early return cannot ignore a partial/moving page. Normal page swipes outside shelves, vertical scrolling, all destinations, motion/haptics and overlay behavior remain. Exact device gesture causality is not traced; these source gaps are consistent with the recording and need physical validation.

| Executed or compiled check | Result and limit |
| --- | --- |
| Complete Windows builder | PASS: production Android/Compose, debug APK and instrumentation-source compilation. |
| JVM suite | **1,632 tests / 204 suites**, zero failures/errors/skips; seven new idle/drag/restoration/reselect/navigation regressions. |
| Current source runner | **64/64 PASS**. Ten new recovery broken fixtures; prior 63 checks retained. Existing content fixtures update explicitly superseded Inside/72dp design expectations to Fit/1.6:1 without removing cases. |
| Android theme/layout matrix | **13 × 36 = 468 compiled cases, NOT_RUN**. New method covers nested shelf-edge flings, normal page swipe, forced partial state and theme recomposition. Existing wide-art method now checks proportional filling and complete edges rather than tiny native-size presentation. |
| Sequential lint | **0 errors / 313 warnings / 17 hints**, unchanged; no new suppression/baseline. |
| Feature preservation | **234 protected nonpresentation source/schema/build files byte-identical**; nine callback inventories and exact six shortcut definitions retained. Installed-data acceptance remains separate. |
| Signer | PASS against protected delivered 4041 APK, same retained certificate. No agent-connected installed comparison or install/reset. |
| Acceptance | Fresh 219-case source/APK-bound fixture pack PREPARED_NOT_EXECUTED; updated 136-case content supplement NOT_RUN. |
| Device/publication | No Android device/emulator listed by current adb inventory. No agent install/reset/account/data operation. Actions disabled; no public APK or hosted CI pass. |

| Artifact identity | Value |
| --- | --- |
| Build ID | `NW-debug-006E0C6D2D365492` |
| Canonical source SHA256 | `006E0C6D2D365492ED91B403D60791D0A98B2ED89CA2B1E3C0EFA94878C22AD5` |
| Source inputs | `871` |
| APK SHA256 | `BA93943B89D5390ACDBF2F8B78B3B12586E773851897E5CDBAAC8A7ED1BB06E0` |
| Embedded revision context | `ff5000124bb80abe9735fbbaf52e965ededba342` |
| Separate NW08 manifest SHA256 | `2C98476FB2194167D1E9200D084A9656BF1C2FC057FECD2588858E1363B0BD78`; retained independent encoding/ordering contract |
| App identity | `com.nightwave.player`, `2.0.0-alpha5s`, code35; min26/compile-target36 unchanged |
| Signing certificate SHA256 | `EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394` |

Private checkpoint `C:\Users\jayed\NightWave-checkpoints\20261029-ui-review-000718` retains recording samples/metadata, protected recovery, exact receipts and source/APK evidence; older checkpoints remain. Owner-only candidate delivery is separate. No private media/accounts/credentials/binaries are committed or publicly published.

All four palette values, every feature and collection/action, Search draft ownership, complete mood art/caption separation, normal/error roles, original URLs, account/consent, queue/library/storage, native/classic choice, package/signing and dependencies remain. New **006E** phone validation must cover readable artwork and shelf edges/rapid taps/reselect/interrupted swipes/theme/recreation/window/font combinations, plus all prior accessibility/audio/account/recovery/data/performance/release gates. No 9.5–10 is inferred from code tests. New-candidate device acceptance **NOT_RUN**, release **NO-GO**. **NW134 remains next.**
