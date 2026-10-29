# FINISH02 — everyday offline clarity

29 October 2026. **LOCAL_VERIFIED / DEVICE_NOT_RUN** on `fix/nightwave-everyday-finish`, [draft PR #156](https://github.com/Nahian2002/NightWave/pull/156), stacked on OFFLINE_FINISH #155. Protected parent `c932daaa1b3ac2e3da87730b3d726659f1e74171`; pre-code plan `78a57c0db60bd8d8b88fa88e30c560e7f003c0e6`; implementation `2718895ce0aaec837aa18781d37da00165f14049`. [Revised schedule](UPCOMING_UPDATES.md), [latest recorded review](../audits/2026-10-29_RECORDING_230548_REVIEW.md), [eight phone cases](FINISH02_ACCEPTANCE.csv).

## User-visible changes

- Clear saved-song search is a separate 48dp action while a query exists. A filtered empty library offers **Show all saved songs**, resetting only query/availability filters and paging. Sorting, history, files, selection and all routes remain. The recorded 0-of-1 Blue result was filtering, not lost audio.
- Resume, options, recovery/removal dialogs and accessibility descriptions reuse known conservative display identity. Completed download headings and creator use a generation-fenced background snapshot; optional identity failure never changes file readiness. Legacy/imported items retain a truthful fallback; original records, IDs, URLs, editions and data formats remain.
- Verified successful guarded transfers retain numeric wire-byte and monotonic elapsed receipts. Example fixture: 2,704,719 bytes / 507ms displays **Transfer 0.51 s · 42.68 Mbps avg**. This is a formatter regression fixture based on the owner's prior recording, not a new network measurement. The interval ends before import/verification and includes connection, resume checks and retry waits; it excludes queue/source lookup. Received bytes exclude retained bytes; retries can include re-received bytes. Invalid, legacy and already-saved reuse receipts are omitted. A later dialog cannot age the frozen receipt. Only an actual available-file Ready state shows it; no diagnostic-text parsing or readiness inference from speed.

ART02 handles older cached artwork separately; SEARCH02 handles public-search latency. No palette rewrite or transfer/source/bitrate algorithm change. Every existing feature, theme, package/signer, manifest/dependency and original persistence format remains.

## Verification

| Evidence | Result / limit |
| --- | --- |
| Windows builder | Android/Compose/APK and instrumentation sources PASS |
| JVM | **1,754 tests / 217 suites**, zero failures/errors/skips; seven new tests |
| Source checks | **74 PASS**, including new broken-caller fixtures; not runtime proof |
| Lint | **0 errors / 317 warnings / 17 hints**, exact issue inventory unchanged; no suppression/baseline |
| Preservation | **904** prior canonical inputs byte-identical; ten intended existing edits and three added inputs; nine callback inventories retained |
| Network/source safety | Existing range engine, Gateway, quality selection and public-address checks unchanged; downloader differs only by observational receipt getter/marks |
| Secrets | All-ref Gitleaks, **3,539 commits**, no leaks; existing reviewed exclusions retained |
| Physical acceptance | Eight new cases and all prior device/upgrade/data/audio/account/accessibility/performance/release gates remain **NOT_RUN** |
| Delivery | Owner-only Drive permission readback and streamed **33,844,842-byte** SHA256 match; no install/reset/public APK/merge/Actions change |

Build **NW-debug-9457716D1AC24E2A**. Canonical source SHA256 **9457716D1AC24E2AB3D8E1761124CA9DB34A65658120375654EBABDF1D0F8EF6**, **917 inputs**. APK SHA256 **53BCF27946D7DC8938DD6A1FD093ECE0E51C1AFD4072D2CD821B9C0FD2F71C01**. Signer **EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394**, matching prior FD80578F. Package `com.nightwave.player`, `2.0.0-alpha5s` / code35, min26/target36 unchanged.

Build provenance records plan revision `78a57c0d` with the implementation working tree; the subsequently committed implementation has independently matching canonical source bytes. Git revision context alone is not the source identity. A private checkpoint retains baseline/final bundles, exact working-source ZIP with CRC/canonical readback, raw JVM/lint/source/build/signer/delivery receipts and superseded marker-check failure. The original background availability call was retained, so historical readiness guards still pass without weakening them.

No new rating follows from compilation. Latest recorded demonstrated usability remains about **8.4**, visual rubric **8.8**, offline journey **9.0**; this candidate needs fresh phone evidence. **ART02 is next**, then SEARCH02 and NW137–NW145. The every-six-hour heartbeat reads actual backlog state and runs one eligible batch at a time.
