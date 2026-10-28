# DESIGN03 owner regression review — artwork and split pages

29 October 2026. Existing [draft PR #147](https://github.com/Nahian2002/NightWave/pull/147), `design/nightwave-content-finish`. Protected parent `67c35cdca371d805d59284aa77b8ec6729efaf3c`; correction implementation `ff500012`. [Current exact verification](../../TEST_REPORT.md). This is a correction to the prior content finish, not NW134 or a new feature batch.

## Evidence

`Screen_Recording_20261029_000718.mp4`: 83,454,881 bytes; SHA256 `de44e5e213fb27e7ba298af17c14500562f31df03118d710097a6b0c4ad79aa6`. Reported 1080×2340, 9,008 frames at approximately 70.65342 fps, frame-count duration 127.4956 seconds. These describe media, not app performance. One-second timestamp seeks decoded 127 samples at 0–126s; 127s failed. Four-second overview and detail frames at 16, 17, 40, 44, 64, 68, 84 and 96 seconds were inspected. This is visual sampling, not every-frame playback/audio review. No unique inspected APK identity is exposed; source/appearance correspondence does not prove binary attribution.

Tiny art is clearly visible on Home around 00:04–00:16 and full player around 01:24 and 01:36. The complete frame is large but the source appears as a tiny center stamp. Split Home/Search pages appear around 00:16–00:18, 00:44 and 01:04–01:08; Library/You is also partial near 00:40. Thus this is not confined to a single theme or destination. The recording establishes visible failures, not their precise internal gesture/event sequence.

## Honest recorded rating

Same five equal appearance dimensions, with navigation breakage treated as a blocking UI issue:

| Dimension | Score / 10 | Evidence |
| --- | ---: | --- |
| Identity | 8.5 | NightWave branding and four distinct choices remain recognizable. |
| Color character | 9 | Teal/black, navy/cyan, plum/lavender and Pearl/cobalt roles remain strong. |
| Tonal hierarchy | 6 | Tiny previews inside large vacant frames undermine the intended visual hierarchy. |
| Observed coherence | 7.5 | Split destination layouts break the otherwise coordinated shell. |
| Perceived legibility | 8 | Settled text remains readable; current type/contrast evidence is limited to the recording. |
| Equal-weight mean | **7.8** | Subjective visible UI/appearance, below the prior 8.4 sample. Not a whole-product score. |

Additional aspect judgments: mood illustration composition about 8.5 and music-first Search organization about 8.5; artwork presentation around 4 and navigation stability around 5 because of the observed failures. These diagnostic sub-scores are not extra weights in the five-dimension mean. Motion smoothness, real accessibility and complete runtime reliability are not numerically certified from this video. No new-candidate 9.5–10 score is assigned from passing code checks.

## Root causes and correction

**Artwork:** the prior no-upscaling decision was too strict. ContentScale.Inside retains native drawable size; small/density-adjusted sources can remain tiny even within a large measured frame. Replace it with ContentScale.Fit for large previews. This fills the limiting dimension proportionally, retains all image edges and avoids square crop zoom. It may enlarge low-resolution sources; it does not reconstruct missing detail. Existing same-source candidate selection, fallback, cache and identity-bound ambient art remain unchanged.

Mix artwork also receives a more balanced size: the featured mosaic is 88dp and paired cards use a bounded 1.6:1 artwork banner, rather than the previous 72dp stamp or the superseded full-width square wall. All four real thumbnails and every preview/play action remain.

**Pager:** the source has no idle-offset repair, tab selection follows the nearest current page during a drag, selected tabs have no recenter action, and nested horizontal shelf leftovers can reach the outer pager. These are concrete source gaps consistent with the recording; an exact on-device event trace is still unavailable.

- Horizontal shelves consume only unconsumed horizontal scroll/fling deltas at their boundary. Vertical scrolling remains available; normal page swipes outside shelves remain enabled.
- A shared recovery observes pager state, waits 100ms after an idle fractional offset and recenters to the nearest current page. Active drags/animations are excluded, and collectLatest cancels stale recovery when state changes.
- Navigation selection/focus side effects use settledPage. Tapping an already selected tab calls the existing navigation path, and a fractional/moving page cannot be discarded by the former early return.
- No theme preference, selected song, queue, Search draft, account state or stored library is reset. All destinations, page swipes, shelf scrolling, reduced motion, haptics and overlays remain.

## Regression proof and remaining acceptance

Seven JVM methods test idle offsets, centered/rounding-only states, active drag/animation exclusion, invalid inputs, restored indices, selected-tab recentering and destination navigation. The existing wide-source Android method now verifies frame filling plus both source edges rather than enforcing native-size stamps. One additional actual pager method tests shelf-edge flings, a normal page swipe, forced partial offset and theme recomposition. The four-theme/width/font matrix becomes **13 × 36 = 468 compiled cases, NOT_RUN**.

Existing content caller guards retain their tests and update the two explicitly superseded design expectations (Inside→Fit, 72dp stamp→1.6:1 banner). A new recovery guard adds ten broken caller fixtures. Existing error-surface, source protection, history/count, original URL and input/seek/feature checks remain. Physical confirmation is required across four themes, shelf edges, rapid/repeated tab taps, partial swipe interruptions, rotation/theme switches, overlays, font/window sizes and all source artwork types.

Prior 8.4/8.2/7.6/6.0 reviews, the 6.7 original whole-product audit and their acceptance limits remain historical evidence. New-candidate score/device acceptance is pending; NW134 remains next.
