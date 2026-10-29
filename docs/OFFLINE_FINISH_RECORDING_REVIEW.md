# Offline finish — 29 October 2026 recording review and repair plan

## Pre-change rating and evidence

Protected parent `981c6655577162fcfaf3612d36b51621891703d4`, NW136 PR #154. Owner explicitly asks to rate first, explain the route to 9.5, and apply the fixes. This is a recording-driven NW136 follow-up; NW137 is not silently marked complete.

Reviewed recording 195906: 83,586,920 bytes, SHA256 `c477ff44d6196cce0b346afe5da3ed2b39ecde464706247d79f6df59d3a185c5`, 1080×2340, all 9,094 frames decoded sequentially through 211.5769s, zero backwards timestamps, 212 one-second samples/eight contact sheets and full-size detail stills. Encoding cadence is not measured app frame rate. Private media and frames stay outside Git. The exact APK ID is not exposed, although the newly added KiB/s/Mbps/details UI is visible.

Main-screen visual rubric remains **8.8/10** (identity 8.5, colors 9.0, hierarchy 9.0, observed coherence 9.0, perceived readability 8.5). Theme appearance: AMOLED 9.2, Midnight 9.0, Pearl 8.8, Aurora 8.7. These are subjective visual grades, not contrast measurements or certification. The original 6.7 whole-product audit remains frozen.

| Recorded aspect | Pre-fix grade / 10 | Evidence / route to 9.5 |
| --- | ---: | --- |
| Main visual identity and colors | 8.8 | Cohesive themes; retain them rather than restarting a broad redesign. |
| Home / mood cards | 8.8 | Clear compartments, no persistent decorative clipping in inspected settled frames; retain original actions and readable thumbnails. |
| Search | 8.6 | Empty query remains empty during navigation; improve task continuity, preserve all results/source identity. |
| Main Library | 8.8 | Saved/history distinction and collections present; device persistence and bulk acceptance still needed. |
| Online player layout | 9.0 | Strong hierarchy; source cover remains soft. Buffering shows a Play glyph despite pending playback: check desired-state control wiring. |
| You / settings | 8.4 | Long option lists remain; preserve all appearance, sound, account and recovery paths. |
| Offline navigation / compactness | 6.5 | Source tabs plus duplicate destination rows push the actual music down. Single-page pagination also spends space without useful navigation. |
| Download feedback | 6.5 | Global text still says Resume always restarts from zero, contradicting NW136. Early ETA jumps from about 25 minutes; visible rates vary substantially. |
| Observed transfer task | 5.0 | A roughly 2.8 MB file reaches Ready around 162s after a request near 83s: approximate end-to-end rate around 0.3 Mbps, not a certified 1 Mbps minimum. Diagnostics were never opened, so no limiting hop is established. |
| Saved-song identity / artwork | 4.5 | Online Play Date/Melanie artwork becomes raw filename, generic “Offline · NightWave” artist and placeholder art. Keep proven source identity and cached legitimate artwork. |
| Offline playback / removal | 5.0 | Position advances after removal around 189–190s; the removed offline selection remains Playing around 193–202s. Reconcile indexed deletion with active/preloaded playback. Audio/airplane-mode correctness is not proven by the video. |
| Empty / completion states | 7.0 | Ready is shown after completion, but technical copy and unused chrome crowd small libraries. Make verification explicit before readiness. |
| Queue / lyrics / Sound Studio | Not demonstrated | Entry points are visible; full functionality and accuracy cannot be freshly rated from this recording. Preserve NW137–NW145 acceptance. |
| Recovery / accessibility / battery / security | Not demonstrated | No restore, assistive technology, long soak or new security audit occurs. These are certification gates, not assumed failures. |

**Recorded offline journey: 7.5/10, provisional expert judgment, not a weighted whole-product re-rating.** A single completed transfer is meaningful progress, but cannot prove sustained throughput, sound quality, all features or 9.5 overall.

## Repair plan committed before implementation

1. Replace contradictory global resume guidance with source-aware language. Use measured, smoothed recent-byte speed/ETA with a warm-up period; show completed-byte totals and explicit verification rather than interpreting every instantaneous jump as a stable line rate. Keep privacy-safe details and all safety checks.
2. Consolidate repeated offline destination controls through the existing shared routes; preserve every source, import, history, filter, recovery and playback action. Keep independent 48dp targets and reflow. Do not display single-page paging controls as wasted chrome.
3. Preserve known source title/creator/artwork for saved searched tracks using bounded, validated, optional metadata. Do not invent album/artist identity, persist signed media URLs, require network access for playback, or break legacy/imported/library/cloud IDs. Read existing source links where safe for backward-compatible display.
4. Reconcile library removals with current and prepared offline playback; stop the removed selection, evict removed prepared items and retain unrelated public queues/data. Test stale asynchronous results and current/queued removals. Preserve file/index safety and cleanup semantics.
5. Make Pause available when playback is requested but buffering. Inspect pager settling against the recording; repair only a demonstrated or source-reproducible fault. Retain original navigation, queue and all themes.
6. Test meaningful positive/negative cases, full Android/JVM/source/lint/signer/provenance, source/callback preservation and private recovery. Save exact remaining phone checks and a cumulative draft PR, deliver an owner-only APK with hash readback. No automatic install, data reset, public binary, merge or Actions change.

## Results and remaining acceptance

**LOCAL_VERIFIED / DEVICE_NOT_RUN**, [draft PR #155](https://github.com/Nahian2002/NightWave/pull/155), final built revision `336dc8c38aa0fdf6079e38bfc81d0ad606b69457`. Both the original repair plan and the later owner-timing concurrency extension were committed before their corresponding implementation. No post-fix score is awarded before reviewing the resulting app.

- Implemented validated probe/four-part transfer, ordered durable hashing/checkpoint commit, bounded memory and isolated cancellation; changed/unsupported sources restart safely. Per-call metric clocks and synchronized totals prevent concurrent timing corruption; elapsed wall time is shown separately from summed receive time. Source quality and every existing URL/DNS/redirect/format/storage/integrity check remain.
- Implemented measured rolling-byte speed, a three-second speed warm-up and ten-second/64 KiB ETA eligibility, excluding restored bytes. Byte completion shows Finishing/Checking rather than Downloading 100%. Global help now distinguishes supported continuation and safe restart.
- Consolidated duplicate source destinations through shared tabs, retaining duplicate destinations in native recovery. Import/more tools share a row, storage/recheck share a row, one-page paging is omitted, and saved rows have readable identity plus a 56dp playable artwork action. All source, history, recovery, upload, filter and playback routes remain reachable. No theme palette was changed without an observed color fault.
- Added an optional bounded identity sidecar. Known creator/title use the existing conservative display formatter; imported IDs/audio index/cloud format remain unchanged. Cover images are copied only from existing Coil disk cache, never fetched by the sidecar: 1 MiB/image, 64 MiB total, checked dimensions/type/path, atomic copy. A cache miss or metadata failure never changes audio readiness. Existing links can use matching public queue metadata for display; absent evidence remains Saved audio. This local sidecar is not a new cloud-art backup feature.
- Added lifecycle-owned, bounded background library reconciliation with index/generation/media/queue freshness fences. Removing the selected offline item stops/releases it and clears its checkpoint, without auto-starting another source or erasing public queues. Removing upcoming files evicts prepared items and remaps queue/shuffle history. Buffering controls retain Pause based on requested transport while audible-playing state remains separate.
- Inspected pager recovery and sampled swipe transitions: no persistent stopped offset is established. Existing idle recovery remains; no unsupported claim of a new pager defect or fix.

### Validation and exact identity

**1,747 JVM tests / 216 suites, 73 source checks, lint 0 errors / 317 warnings / 17 hints**, zero JVM failures/errors/skips; full Android/Compose/APK and instrumentation-source build PASS. Twenty-one added JVM methods cover range ordering/fallback/backoff/cancel/recovery and controlled throughput, optional metadata validation/Unicode, rate warm-up/stall/restored bytes, buffering intent and offline queue removal/history. Seventy-three current gates include broken-caller fixtures. Final lint passes; two added UseKtx preference-style notices and one short Recheck label localization notice are triaged for existing conventions/NW141, without suppression or baseline. Android/source compilation does not execute the new runtime on a phone.

Matched exact-byte controlled fixture (64 KiB per 250ms per response):

```
OFFLINE_FINISH_CONTROLLED threads=1 bytes=2097171 nanos=7901540800 mbps=2.1233033435706616 scope=per-connection-paced-loopback-not-phone
OFFLINE_FINISH_CONTROLLED threads=4 bytes=2097171 nanos=1899911100 mbps=8.830606863657989 scope=per-connection-paced-loopback-not-phone
```

This demonstrates the benefit under per-connection pacing, not a source's aggregate limit, Wi-Fi/mobile speed, battery result or guaranteed minimum.

Build **NW-debug-FD80578F4DE7C202**; canonical source SHA256 **FD80578F4DE7C20251247DBEB81EAAAE298C8289CD02233680C8F9509C39D29D**, **914 inputs**. APK SHA256 **1451AFEF92B6DFDA50914602E2F0F3C8BF3F18C32DA9DDAECB165B4D4FC81F28**, signer **EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394**, matching the last delivered NW136 APK. Package `com.nightwave.player`, version `2.0.0-alpha5s` / code35, min26/target36 and dependencies remain unchanged. Source readback: 226 protected files identical, fourteen intentional existing files changed, all nine callback inventories retained; the protected inventory explicitly includes the added NW136 transfer files and two player UI files. Gateway/source/bitrate and public-address body are unchanged.

Private checkpoint keeps original recording samples, baseline/final bundles, exact working-source ZIP/CRC/canonical readback, raw JVM and lint/source/provenance/signature receipts and owner-only APK/hash delivery. Initial compile errors and an obsolete marker preflight are retained as superseded evidence; no failed candidate is presented as final proof.

### Phone/release gates still open

All [fifteen new cases](quality/OFFLINE_FINISH_ACCEPTANCE.csv) and prior physical gates remain NOT_RUN. The recording supports one saved-file Ready/playback-state/removal observation, not exact APK attribution, airplane-mode sound, multi-device reliability, TalkBack, contrast, large-font reflow, live source speed, account restore or resource-soak acceptance. No device was connected, installed or reset. Source is on a cumulative draft PR; no public APK, merge or Actions change. **NW137 remains the next source batch.** The route to a defensible 9.5 still includes measured phone tasks and NW137–NW145; this repair does not self-award that score.

### Owner-provided transfer evidence, before concurrency implementation

The owner supplies a second download without background music: bounded ranges, HTTP/1.1 / 206, three requests, 2,948,520 received bytes, zero retained-start bytes; DNS 9ms, connect 1,072ms (including TLS 54ms), headers 300ms, receive 52,685ms, write 5ms, sync 20ms. This is owner-supplied diagnostic text, not an independently captured phone trace. Receive-only average is approximately **0.448 Mbps**. Storage and DNS are not the dominant measured phases; background playback is not necessary to reproduce this run. Per-connection versus aggregate source pacing remains unproven.

Plan extension: test **at most four parallel 512 KiB ranges after a validated 64 KiB probe**, retaining strong-validator/Content-Range/type/byte checks, original quality, storage bounds and ordered durable commit. Buffer at most 2 MiB across the batch, cancel only this transfer's active calls, validate every part before combining, retain only synced receipt-backed segments, and safely restart if the source changes or ignores Range. Record a matched controlled server comparison; phone/source minimum speed remains unverified. This experiment is justified by the new receive-bound HTTP/1.1 evidence, not a promise that concurrency defeats every server limit.
