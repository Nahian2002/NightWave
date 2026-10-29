# Current verification — recorded offline journey finish

29 October 2026. [Draft PR #155](https://github.com/Nahian2002/NightWave/pull/155), `fix/nightwave-offline-finish`, stacked on NW136 #154. Protected parent `981c6655577162fcfaf3612d36b51621891703d4`; pre-code review/plan `c8f65557`, timing extension `54e02eeb`, implementation `b4dc9486`, final built revision `336dc8c38aa0fdf6079e38bfc81d0ad606b69457`. [Detailed ratings, plan and results](docs/OFFLINE_FINISH_RECORDING_REVIEW.md), [15-case phone supplement](docs/quality/OFFLINE_FINISH_ACCEPTANCE.csv), [previous verification](https://github.com/Nahian2002/NightWave/blob/981c6655577162fcfaf3612d36b51621891703d4/TEST_REPORT.md). **NW137 is next.**

Pre-fix main visual rubric **8.8**, recorded offline journey **7.5**; no post-fix 9.5 claim. Owner-supplied second-run timings identify receive wait (~0.448 Mbps), not disk, as dominant. The candidate adds guarded four-part transfer after a validated probe, optional known identity/existing cached art, indexed-removal playback reconciliation, buffering Pause, measured ETA and compact retained routes. Original source/quality, all themes/features, signing and original data formats remain.

| Evidence | Final result and practical limit |
| --- | --- |
| Complete Windows builder | Android/Compose/APK and instrumentation-source compilation PASS |
| JVM execution | **1,747 tests / 216 suites**, zero failures/errors/skips; 21 new methods |
| Source fences | **73 PASS**, including broken-caller fixtures; not runtime phone proof |
| Lint | **0 errors / 317 warnings / 17 hints**; three added style/localization notices triaged without suppression |
| Controlled HTTP comparison | Identical 2,097,171-byte file: ~2.12 Mbps serial / ~8.83 Mbps parallel under per-response pacing; not phone/provider speed |
| Source preservation | 226 protected files identical; fourteen intended existing source changes and nine callback inventories retained |
| Package/dependencies/quality | Manifest/build inputs/Gateway/public-address function remain identical |
| Signing/provenance | Same signer as previously delivered NW136; canonical embedded source/bytes verified |
| Android/theme/runtime | Existing test sources compile only, including 612 theme cases; no connected phone/emulator |
| Phone acceptance | Fifteen new cases plus all prior physical/data/recovery/audio/accessibility/resource/release gates NOT_RUN |
| Recovery/delivery | Private verified source ZIP/bundles, raw tests/lint/provenance/signature and owner-only streamed-hash delivery |
| Hosted checks | Actions stays disabled; no hosted pass or merge |

Build **NW-debug-FD80578F4DE7C202**, canonical source SHA256 **FD80578F4DE7C20251247DBEB81EAAAE298C8289CD02233680C8F9509C39D29D**, **914 inputs**. APK SHA256 **1451AFEF92B6DFDA50914602E2F0F3C8BF3F18C32DA9DDAECB165B4D4FC81F28**. Signer **EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394**; package/version `com.nightwave.player` / `2.0.0-alpha5s` / code35, min26/target36 unchanged.

Cached artwork/identity are optional local additions, not a cloud schema migration; cache misses never make audio unavailable. Four connections can improve per-connection pacing but cannot promise throughput against an aggregate source/network limit. Concurrent read timings are cumulative; elapsed wall time is separate. Validate same-source quality, completed bytes, Wi-Fi/mobile and background playback, Pause/Cancel/process/storage, selected/queued deletion, four-theme rendering, offline audio/seek and resource soak on the actual phone. The original whole-product 6.7 baseline remains historical; no fresh overall certification is inferred.
