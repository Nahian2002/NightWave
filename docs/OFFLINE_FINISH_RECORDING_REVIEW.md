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

Pending implementation. No post-fix score is awarded before reviewing the resulting app.

### Owner-provided transfer evidence, before concurrency implementation

The owner supplies a second download without background music: bounded ranges, HTTP/1.1 / 206, three requests, 2,948,520 received bytes, zero retained-start bytes; DNS 9ms, connect 1,072ms (including TLS 54ms), headers 300ms, receive 52,685ms, write 5ms, sync 20ms. This is owner-supplied diagnostic text, not an independently captured phone trace. Receive-only average is approximately **0.448 Mbps**. Storage and DNS are not the dominant measured phases; background playback is not necessary to reproduce this run. Per-connection versus aggregate source pacing remains unproven.

Plan extension: test **at most four parallel 512 KiB ranges after a validated 64 KiB probe**, retaining strong-validator/Content-Range/type/byte checks, original quality, storage bounds and ordered durable commit. Buffer at most 2 MiB across the batch, cancel only this transfer's active calls, validate every part before combining, retain only synced receipt-backed segments, and safely restart if the source changes or ignores Range. Record a matched controlled server comparison; phone/source minimum speed remains unverified. This experiment is justified by the new receive-bound HTTP/1.1 evidence, not a promise that concurrency defeats every server limit.
