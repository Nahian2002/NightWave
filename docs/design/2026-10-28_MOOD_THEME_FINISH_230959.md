# DESIGN02 mood artwork and final composition review

28 October 2026. Existing [draft PR #146](https://github.com/Nahian2002/NightWave/pull/146), `design/nightwave-expressive-themes`. Protected prior review `70f12cdbd606fb91c6aca8a77da1f9585a4bbf40`; source implementation `ac3a8866` with caller-guard follow-up `931143f6`. [Current exact verification](../../TEST_REPORT.md). Prior [composition/Search review](2026-10-28_THEME_POLISH_211137.md) and older appearance audits remain. This is an owner-requested visual correction; NW134 remains next.

## Evidence and rating

Owner media `Screen_Recording_20261028_230959.mp4`: 48,546,598 bytes; SHA256 `2d97679c817854d5ea75745b66ff0564378f0b6669ba6acbbf2c04363f57a87f`. Reported 1080×2340, 5,505 frames at approximately 62.11435 fps, frame-count duration 88.62687 seconds. These are media properties, not app frame-time evidence. One-second timestamp seeks decoded 86 samples at 0–85 seconds; 86–88 seconds failed. A four-second overview and detail frames at 20, 32, 48, 64, 72, 76, 80 and 84 seconds were inspected. This is visual sampling, not every-frame playback or audio review. The inspected frames do not expose a unique APK/source identity, so attribution to a particular delivered binary remains unverified.

AMOLED is explicitly selected at about 00:32 and has broad Home/Search/Library/You/player coverage. Aurora appears on Home/Library/You around 00:36–00:56; Pearl on Home/Library/You and player around 01:00–01:12 and You again near 01:20. A navy/cyan player is shown near 01:16, with more limited context. These observations do not establish an independent complete score for each theme. The flat rail and quieter separators are visible, but formal large-text/accessibility/native acceptance remains separate.

Using the retained five equally weighted appearance dimensions for the recorded screen sample:

| Dimension | Score / 10 | Evidence |
| --- | ---: | --- |
| Identity | 8 | Wave identity and distinct palette choices remain coherent. |
| Color character | 9 | Black/teal, plum/lavender, warm-white/cobalt and navy/cyan now have clear character and recognizable primary actions. |
| Tonal hierarchy | 8 | The flat navigation rail improves separation; mood art is faint or partly erased and repeated indicators add emphasis. |
| Observed cross-screen coherence | 8 | Main screens and player use coordinated roles; incomplete theme/device coverage limits confidence. |
| Perceived legibility | 8 | Text remains legible at the recorded scale; some supporting copy is still dense. No rendered contrast certificate is inferred from video. |
| Equal-weight mean | **8.2** | Subjective observed appearance only; not a new whole-product score. |

Color identity averages **8.5/10** across identity/character; layout/coherence is around **8/10**. The underlying five-dimension rubric is unchanged, but the set of demonstrated screens differs from older samples, so this is not a controlled A/B effect measurement. Overall frozen provisional product score stays **6.7/10**. A 9.5–10 appearance result remains the target; passing source/tests cannot assign that result to the new candidate.

## Confirmed mood-card defect

At about **00:20**, Unplugged/Momentum artwork is largely missing or cut; at **00:48** and **01:04**, the After hours rings run beyond the top/right edges and other motifs appear truncated. Source corroborates two causes: a full-card Canvas uses the entire card's changing height, while an opaque caption is drawn above its lower portion; circles centered at 91% width with radii up to 34% width extend outside the card. This is a real internal composition defect, separate from normal clipping of neighboring cards at a horizontally scrolled shelf viewport.

## Applied correction

- Each mood card has an independently measured **72dp artwork header**, then a content-sized caption. The canvas is no longer behind the caption, and font scaling grows the text area rather than stretching or obscuring the illustration.
- A normalized motif policy defines six distinct complete illustrations: concentric rings, focus lines, pulse bars, acoustic strings, rising energy bars and a morning sun/horizon. A centered square viewport plus **4dp stroke padding** keeps all geometry inside its own paint region, including round line caps.
- Decorative contrast increases from 20%/42% accent to 38%/76%; text keeps the existing opaque foreground roles. All four palette values remain unchanged. These are vector motifs for search shortcuts, not fabricated album art or music statistics.
- The decorative glyph becomes Search, correctly communicating that a mood opens an explicit search draft. The same title, subtitle, source query, callback and horizontal shelf remain.
- Navigation uses one animated selection capsule instead of a capsule plus a second top bar. Labels, selected semantics, full tab touch bounds, haptic/motion settings, and all destinations remain.

No feature is hidden/removed. Search ownership fix, original URLs, playback, queue, native/classic choice, library/history, account/consent, stored themes/data, signing/package identity and dependencies remain. A partly off-screen neighboring shelf card is still a normal horizontal-scroll preview, not an internal artwork failure.

## Verification and required device review

Six JVM regressions cover six distinct motifs, complete line/circle geometry, stroke padding across narrow/wide paint regions, invalid/tiny bounds, device-locale stability and safe fallback. The existing 36-configuration Android mood method now iterates all six actual shortcuts and checks artwork within the header, caption below the header, caption within the card and each original search callback. The ten-method matrix remains **360 compiled cases, NOT_RUN**; checking six moods inside a method does not turn it into six executed phone cases. The source runner retains 62 checks, with 14 expressive negative fixtures including header removal and viewport wiring. Earlier guards remain.

Phone confirmation must cover all six motifs in each theme, narrow/large type, shelf scrolling, full labels and actions, reduced motion and TalkBack. New app screenshots are needed to judge the finished composition; pure geometry and source checks alone do not certify rendered polish. Prior Search page-recreation/native/system-bar/data/audio/performance/release gates carry forward. New-candidate device acceptance remains NOT_RUN; NW134 remains next.
