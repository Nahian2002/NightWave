# NW150 — Search, offline and lyrics journey finish

5 November 2026. Code46 is **LOCAL_VERIFIED**, device acceptance **NOT_RUN**. This cumulative update retains every existing feature/theme, source URL/quality, data format, package/signer and account/consent boundary. [Committed plan](NW150_JOURNEY_FINISH_PLAN.md), [exact evidence](quality/NW150_SOURCE_EVIDENCE.json).

Search previously passed only an on-device song title to its destination. It now passes the stable entry ID as well: duplicate titles resolve to the selected entry, renamed titles keep identity, and a missing entry never falls back to another recording. Selection survives Activity recreation, including a deliberately cleared selection; editing the query returns to normal full-library matching. Reused destinations reset their availability filter/page. Readiness remains a separate current file-health check; opening the entry never auto-plays or edits the index.

Explicit cancellation and background suspension now have neutral, distinct messages with Retry and YouTube fallback. Existing result/version URLs and diagnostics stay intact; query/mode and request-generation/socket guards still prevent stale results from becoming current. Actual provider failures retain their error presentation. No service, URL, ranking, cache, timeout or transfer behavior changed.

Owned-LRC immersive controls now say **Pause scrolling / Follow lyrics**, separated from playback. The explanatory hint has its own line and action buttons wrap at narrow/large-text widths. Import/review/explicit replacement/editor/export/remove, follow after drag and timing/source-binding behavior remain. English/Bangla resources match. Fixture regressions exercise UTF-8 review/edit/round-trip, conflicting metadata and existing positive-offset semantics. No lyrics provider, transcription, unauthorized audio or personal import was added.

Offline import, missing/size-changed/unreadable readiness, retry and network/receive/write/sync separation were inspected and retained; existing regression suites run in the full builder. No fake speed floor or universal Mbps promise is introduced. Physical transfers/import and full seek/decode outcomes remain pending.

## Exact verification

- Final Windows builder PASS; **1,854 JVM tests /233 suites**, zero failures/errors/skips. Thirteen new JVM cases. Nine new parameterized/exact-entry Android cases and updated existing fixtures **COMPILED_NOT_RUN**, not executed on personal data.
- **92 current source checks PASS_SOURCE_ONLY**. Sequential lint0 errors/327 warnings/17 hints; complete ID/severity/message/path inventory unchanged from45. Publication-control self-test PASS.
- Gitleaks final implementation scans3,655 commits/all refs/full history: zero unreviewed findings. One immutable fixed UI fixture marker was reviewed as non-secret and added to the existing exact-fingerprint ledger; no file/rule exclusion or history rewrite. First build failed on a missing resource import, repaired before final validation; an intermediate passing build predates the retained web-fallback refinement and is not delivered.
- Build `NW-debug-D3D3F6A4CD0C40D5`, built revision `1bd431d5a235e055fe076b83a32346e033ecbb1b`. Canonical source `D3D3F6A4CD0C40D5420D8D1AFA310E00D49B11F0534B949BB005A771E8387C7D` /1008 inputs. APK `11258AE163BCBE357D7E890C6093FB6D70AEF4C0ED92D4BF810BBA7953FE9BC0` /29,311,242 bytes. Signer `EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394` matches freshly pulled actual Samsung43. Eight native entries and one other asset unchanged from45; APK ZIP inventory unchanged.
- Owner-only Drive permissions and complete streamed byte/hash readback PASS. Public repository contains source/evidence only. APK/private captures/data/logs remain outside Git.

## Device and remaining acceptance

Connected Samsung SM-S928B reports Android17/API37. Existing installed43 was freshly pulled and matched; normal launch, uniquely observed Home→Search tap and an empty search field succeeded. Launcher hierarchy initially could not get idle state, but the in-app hierarchy succeeded. This is code43 capability evidence only. The earlier MIUI pilot is deferred because supported normal Samsung input works; no helper/test APK or production automation endpoint was installed or added.

Code46 has **not been installed**; saved installation rules require a version-specific approval. [Prepared scoped test plan](NW150_SAMSUNG_CODE46_TEST_PLAN.md). No original219 case, full NW145 gate, older-device/soak/listener study, physical gesture or new rating is promoted. Next independent source batch **NW151 — older Android language/accessibility**. No main merge, release/feed activation, public binary, account/system-setting action or automation restart.

[Draft review #176](https://github.com/Nahian2002/NightWave/pull/176), stacked on NW149 draft175; unmerged.
