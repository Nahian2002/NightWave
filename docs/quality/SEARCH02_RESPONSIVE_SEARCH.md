# SEARCH02 — responsive public search

30 October 2026. **LOCAL_VERIFIED / DEVICE_NOT_RUN** on `update/nightwave-search02-responsive`, [draft PR #158](https://github.com/Nahian2002/NightWave/pull/158), stacked on ART02 #157. Protected parent `633b6f4f1a715378eb47a17c00ea0e9127dbe8c4`. Private checkpoint `20261030-search02` retains the baseline bundle. NW137 follows this bounded batch.

## Committed implementation plan

The existing gateway already caches twelve successful queries for ten minutes, but cache freshness is invisible and retry can reuse it. The screen retains previous results in state yet hides those rows behind its loading branch. The twenty-second overall search deadline can be spent almost entirely on the direct provider, leaving little fallback time. The latest recording's roughly seven-to-eight-second public lookup is one observation, not a performance distribution.

1. Keep cache per gateway/activity in memory, with query/mode identity and preserved provider/source metadata and original selectable URLs. Extract a tested bounded LRU: twelve entries, 55 results per entry, explicit fresh/stale windows and a total text budget. Reject oversized cache admission without truncating the live result. Never cache private/local matches, failed/empty searches, credentials, signed playback URLs or account state; no disk/shared-account cache.
2. Present only exact-query/mode cached or previous results while refreshing. Label recent results and refreshing state, preserve all result/version actions, and provide an explicit Refresh path through the existing callback that bypasses a fresh cache. Failure/cancellation retain only matching prior results; a new query cannot inherit stale results or search text.
3. Reserve fallback opportunity through an eight-second direct-provider phase within the existing twenty-second whole-search bound. Keep existing endpoint ordering and explicit browser fallback; no parallel endpoint fan-out, source/quality change or timeout change to unscoped playback/download extraction. Cancellation closes only the owning search calls and cannot publish/cache late results.
4. Capture frozen numeric diagnostics for elapsed/direct/community wall time, HTTP count/time and cache use. Show these only in optional Search details, without query strings, source URLs, IPs, cookies or bodies. Add meaningful fake-clock cache/freshness tests and real controlled HTTP tests for phase failover, cancellation and scope isolation. Preserve prior source guards and regressions.
5. Run full Windows JVM/Android/APK/instrumentation compilation, current source checks and sequential lint, exact source/signer/provenance/preservation checks and private recovery. Save a stacked draft PR and verified owner-only APK. Phone query latency, source/version selection, local/private isolation, four themes/large text and network/background/rotation acceptance remain NOT_RUN; do not infer a 9.5 score or universal search speed.


## Results and practical limits

Plan `8ca040c5`; initial implementation `d5b3820a5c0e8c095dace257be28b62cb4671af1`; final built correction `21d2b18ae1a23ecc23990caf44b43287d95e9351`. An existing preflight rejected exposing raw provider infrastructure in the normal header. That copy was removed, the original guard retained, and the superseded failure preserved privately. Provider metadata remains attached to cached results internally; all existing source/duration/uploader/version evidence and original URL selection remain visible. No provider endpoints, ordering, browser behavior, playback/download extraction or source quality changed.

- Matching previous/cached public rows stay usable while refresh progress remains visible. Query/mode/generation gates prevent another query's rows from becoming current; Home prefill and existing private/local search ownership are unchanged. Explicit repeated Find music, Refresh or Retry with visible results bypasses a fresh cache. A new query can reuse its own recent entry. Failure/cancellation preserve only matching carried results and retain explicit browser fallback; no background search fan-out.
- Public-only per-gateway memory cache: twelve-entry LRU, 55 results per entry, 50,000 text units per entry and 250,000 total, fresh below two minutes, stale continuation below ten minutes. Expired/reversed-clock entries evict. Empty/failure/oversized/noncanonical playback addresses are not admitted. Defensive snapshots preserve raw title, uploader, duration, original URL, artwork and provider/fallback metadata. Oversized live results are returned intact. No persistent/shared-profile/local-corpus cache was introduced.
- Scoped direct provider has an eight-second phase budget inside the unchanged twenty-second overall deadline; community fallback uses the remaining whole-search time and its existing endpoint timeouts. The controlled socket test uses a smaller 250ms direct budget and proves a healthy fallback remains reachable. Unscoped extraction retains its original timeout and transport. Cancellation closes only owned calls, and the guarded cache commit cannot occur after request cancellation. Provider/extraction work remains cooperative; no universal cold-search latency is promised.
- Optional Search details freezes total/direct/community wall time, completed HTTP count/work and recent-cache status. Phase times include extraction/parsing; HTTP work includes connection/read/processing and is not a network-only number. No query, URL, account, IP, headers or body appears in this diagnostic. Large-font Refresh/details buttons share width and retain independent minimum 48dp targets; actual reflow remains a phone case.

| Verification | Result / limit |
| --- | --- |
| Windows builder | Android/Compose/APK and instrumentation-source compilation PASS |
| JVM | **1,772 tests / 219 suites**, zero failures/errors/skips; ten new methods |
| New host coverage | Fresh/stale/expiry/clock, query/mode/repeated-word/Bangla identity, LRU/text bounds, source/version snapshots, isolated instances, numeric timing, actual HTTP phase failover and cancelled commit |
| Source checks | **76 PASS**, including new caller-negative fixtures and strengthened original NW130 visibility fence |
| Lint | **0 errors / 317 warnings / 17 hints**; exact issue inventory unchanged; no suppression/baseline |
| Compiler warning | Existing unchanged Library selectedPlaylist-null branch is always false in its enclosing non-null path; surfaced on recompiling the screen, no new behavior inferred |
| Android additions | Two new real-screen methods compile only: visible/selectable refresh rows and explicit Refresh/details; **NOT_RUN** |
| Preservation | **915** prior canonical inputs identical; six intended existing edits, four additions; nine callback inventories retained |
| Source/data boundaries | Audio index/cloud/account formats, theme tokens, endpoint lists and unscoped extractor, resolve/quality/package/dependencies unchanged |
| Secrets | Implementation-stage all-ref Gitleaks **3,549 commits**, no leaks; reviewed exclusions unchanged |
| Physical/release | Eight new cases and all prior gates remain **NOT_RUN**; no install/reset/merge/public APK/Actions change |
| Delivery/recovery | Owner-only Drive permission and **33,844,842-byte** streamed SHA match; private working-byte source ZIP/bundles and raw receipts |

Build **NW-debug-5ACF2492268C8B8E**, source SHA256 **5ACF2492268C8B8E30ACB1701398885CD3805E164C79D8BBBDCFE4EDC6A878DE**, **925 inputs**. APK SHA256 **C51A79FCC9A5EE0007A8F07F570FF9025794299E85F4534ED349A367CF3A496F**. Signer **EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394**, matching ART02. Package/version `com.nightwave.player` / `2.0.0-alpha5s` / code35, min26/target36 unchanged. Canonical source matches the built correction above. The 219-case baseline pack is prepared for this APK, not executed.

Latest recorded usability/visual/offline scores remain 8.4/8.8/9.0, with exact recorded APK attribution unproven; this batch awards no score increase. **NW137 guided user-owned lyrics is next**, then NW138–NW145. Hourly follow-ups remain active, one eligible batch at a time.
