# DESIGN04 — source-aware artwork, readable identity and deliberate gestures

29 October 2026. Owner-authorized refinement of artwork framing, metadata, Home hierarchy and gesture consistency. [Draft PR #148](https://github.com/Nahian2002/NightWave/pull/148), branch `design/nightwave-editorial-finish`; protected DESIGN03 parent `e7890f957fbb96c0b70a8d5dd69ea498eb9b37c6`. See [current verification](../../TEST_REPORT.md) and [24-case manual supplement](EDITORIAL_FINISH_ACCEPTANCE.csv). Earlier audits/acceptance remain; NW134 stays the next feature batch.

## Recorded baseline and limits

The preceding `Screen_Recording_20261029_002945.mp4` review supplies a **subjective 8.3/10 visible-UI score** on the same five equal dimensions: identity 8.5, color 9, hierarchy 8, observed coherence 8, perceived readability 8. The 91.12904-second file is 51,641,597 bytes; SHA256 `a825527e2fdf417d5bfbeca10f7ee928981daebd0a9fd29ad55ec523810fa09a`, 1080×2340, 5,679 frames at reported 62.31822 fps. Those are media properties, not app frame-time measurements. The prior review decoded 90 one-second samples at 0–89 seconds (90–91s unavailable), a three-second overview and details at 3, 12, 15, 20–23, 33, 58–62, 72, 78 and 84 seconds. No exact inspected binary identity is exposed.

The recording shows larger readable art and complete mood illustrations. Wide sources still sit inside oversized square backplates; promotional pipe-delimited captions dominate Now Playing. Home has several equally emphasized, similar history mixes. Partial Home/Search pages occur around 00:20–00:23 and recover; touches are visible, so those frames cannot establish an idle freeze or complete navigation certification. This update addresses those composition/ownership gaps without claiming a post-change 9.5–10 score.

## Artwork framing

The player frame reads only the successfully resolved current source's intrinsic ratio, already protected by the NW133 source/track identity gate. Landscape art shortens the frame proportionally up to a bounded 1.9:1 ratio; square/portrait art keeps safe square containment. Unknown public art starts at a 4:3 placeholder; unknown local art stays square. Fit remains authoritative, so complete image edges remain visible and native-size stamps do not return. Extreme/invalid dimensions cannot create zero/negative/NaN layout sizes.

Home recent/next-listen artwork uses a deliberate 1.5:1 rail frame. Original sources and same-video candidate/cache/fallback behavior remain. This does not remove bands embedded in the source file or recover missing image detail; it removes unnecessary square UI padding around wide art. Player swipe, buffering, reduced-motion transition, loaded-art ambience and all controls remain.

## Display headlines and complete originals

`NightWaveEditorialPresentationPolicy` wraps the unchanged core MusicIdentity for music UI only. It shortens a multi-part spaced-pipe caption only with explicit promotional evidence, and carries Live/Remix/Acoustic/Cover/Karaoke/Instrumental/Remaster/Version/Edit and Bangla edition segments into the visible headline. Ambiguous/genuine pipe titles and long initial segments keep the existing conservative display. Creator attribution remains unchanged; no artist is inferred.

A visible **Details** action uses the existing player status row. Its selectable, scrollable dialog exposes the full original title, artist/creator and original public source, or a meaningful on-device source label. It resets when the selected track changes. Videos-mode results retain original provider captions. Stored VideoPreview fields, source URLs, playback/notification metadata, lyrics identity, search indexing and backups are not rewritten. The core MusicIdentity file is byte-identical to the parent.

## Home hierarchy

Only the first mix has the emphasized container; every other mix still appears with its original four thumbnails, selection, explanation, track preview and Play actions. Each mix gains a purpose grounded in its actual source membership, such as liked songs, beyond-recent tracks or distinct creators. Compact factual summaries count unique track URLs and real creator labels; total duration appears only when every unique track has a valid known duration. Partial/unknown durations never become a purported full length. Existing membership reasons and original mixes/queues remain authoritative.

The change makes the first card the entry point and alternatives easier to compare, without removing or merging existing collections, inventing recommendations or inferring tastes. Large text retains the existing stacking/reflow and minimum targets.

## Gesture ownership

Nested horizontal leftovers were already isolated, but a gesture can be acquired by the outer pager before a shelf has scrolled. Each shelf now observes its first pointer down without consuming the child's events. A transient owner token disables only outer horizontal page dragging until that shelf's touch ends. The child shelf and vertical parent scrolling remain available; ordinary page swipes outside shelves remain enabled.

Tokens are idempotent and independently released by pointer-up, cancellation/finally and disposal. Multi-owner state prevents a nested or second touch from releasing someone else's ownership. The idle pager recovery also waits for shelf touches, so it cannot recenter underneath a held finger. Theme recomposition does not reset navigation, selected content, Search drafts or storage. Existing settled-page selection and selected-tab recentering remain.

## Verification contract

New JVM regressions cover conservative metadata/edition/Bangla handling, honest mix facts, landscape/square/portrait/invalid geometry, distinct mix purposes and balanced touch ownership. Existing policies, source guards and safety assertions remain. Existing Android player acceptance now opens Details and reads exact originals, checks the aspect-aware frame and then exercises Save/seek/lyrics/queue as before. The actual pager case checks press-time ownership and release before edge flings/swipes/theme changes. The 13-method × 36-configuration matrix remains **468 cases compiled only, NOT_RUN**; stronger assertions do not become executed phone cases.

`verify_editorial_finish.py --self-test` adds ten broken caller fixtures for source dimensions/frame wiring, raw original access, root ownership gating, token release, single lead mix and factual metadata. Prior guards remain; the existing recovery guard follows its expanded shelf-touch input instead of deleting fixtures. No lint suppression/baseline is added.

## Preserved boundaries and device acceptance

No provider/dependency/package/signing/schema/permission change, no storage/account/consent operation, no source rewrite, no feature removal. Four palettes, complete mood art/caption lanes, normal/error roles, native/classic choice, Search modes/history/Undo/prefill ownership, all six shortcuts, queues, playlists, library and recovery paths remain.

Phone acceptance must cover four themes, wide/square/portrait/local/missing art, track changes during loading, long Bangla/version captions, Details while playing, all mix actions, held/released/cancelled/multi-touch shelf gestures, vertical scrolling, rapid/reselected tabs, theme/rotation/window/font changes, TalkBack/keyboard and prior data/audio/performance/release gates. Source/compilation success cannot certify those outcomes. The 9.5–10 target requires fresh rendered/task evidence. Actions stays disabled; public APK distribution remains unapproved. Candidate delivery to the owner is private and separate.
