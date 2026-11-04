# Code40 product review — 4 November 2026

Plan: review the full source feature inventory and available private connected-phone evidence, inspect further reachable read-only screens, grade observed presentation/usability with explicit confidence, and persist an actionable new roadmap. Do not alter app code, library/account data or system settings, install another APK, publish binaries, merge or activate the feed. Preserve original audits, scores, roadmap IDs and pending acceptance gates. Private phone content stays outside Git.

The review is tied to code40/source3674B2BE and the verified installed APK. A source review can inventory every exposed capability; it cannot prove every runtime path, animation frame, theme/font/OS combination or subjective audio quality. Unknown critical dimensions stay unscored. New ratings are provisional product-design judgments, not acceptance certificates or automatic rewards for test counts.

## Review result and evidence limits

**Provisional observed presentation/usability index: 7.83/10 across 92/100 original rubric weight.** Full product overall rating is **NOT_ASSESSED**. These are subjective design judgments at a premium music-app standard, not measured competitor scores. The original100-weight functional rubric is not fully executable from this session: lyrics and recovery are unscored, and other numbers assess visible presentation rather than untested operational correctness. Do not compare the index numerically with the historical6.7 or recording-only8.8 as if they were the same experiment.

Candidate code40, source3674B2BE, APK7BACDEE3; actual installed identity and scoped smoke are recorded in [phone evidence](../NW145_SAMSUNG_CODE40_SMOKE.md). Phone reports Samsung SM-S928B/Android17/API37,1440×3120,density560,font0.8,AMOLED. Private captures include Home01/23/31, Search14/16, Library15, You05/28, offline03(screenshot only), player21/24, queue25, Help09, unavailable-update11 and Sound Studio29(screenshot only). Raw personal content remains outside Git. Only read-only additional screens were inspected; no preset, queue, library/account or theme changes were applied. Phone returned Home/paused.

Sound Studio screenshot succeeded but UI hierarchy dump exited137 on two scoped attempts; the cause is not established. Do not call it an app crash, a TalkBack failure or a pass. The earlier stale-dump issue was excluded and fresh unique snapshots used elsewhere. Continuous animation/frame traces, all-theme rendered captures, large-type and other-device tests were not collected. A screenshot cannot grade transition smoothness.

## Detailed presentation scorecard

| Area | Provisional score | Evidence coverage | Main gap |
| --- | --- | --- | --- |
| Visual identity and coherence | 8.3 | Observed AMOLED; other palettes source-only | Good matte identity; component families still differ |
| Compactness and useful screen space | 7.5 | Home/player/queue/You/offline/Sound screenshots | Oversized secondary cards, repeated padding and explanatory copy |
| Typography and visual readability | 8.1 | Observed font0.8 only | Clear hierarchy; long native labels and all-font acceptance remain |
| Navigation and user control | 8.2 | Scoped actual tab/Back/background checks | More route families and gesture/picker paths need coverage |
| Home usefulness and discovery | 8.0 | Actual mature-history Home | Useful resume/history; large mix cards and long lower sections |
| Search presentation | 8.0 | Empty/saved-result UI observed; live relevance unscored | Source/version decisions exist; real query latency/recall not established |
| Music identity and artwork | 6.8 | Actual enlarged player art | Visible low-resolution softness; source-correct sharper art needed |
| Now Playing presentation | 8.2 | Actual mini/full controls and Back | Strong controls; secondary content and art quality reduce finish |
| Library presentation | 7.7 | Actual empty saved Library; advanced operations source-only | Entry paths exist; populated/bulk/conflict states not exercised |
| Queue presentation | 7.4 | Actual24-upcoming sheet; no edits | PLAYING label conflicts with paused player; bulky row/header spacing |
| Offline/transfer presentation | 7.4 | Actual offline screenshot; workers source-only | Style/density differs; throughput/integrity operational grade unknown |
| Lyrics | Not scored | Source/import policies and player entry only | No real LRC import/sync/editor session; numerical experience score withheld |
| Sound Studio presentation | 7.8 | Actual screenshot; hierarchy dump exited137 | Truthful capabilities; large readback/copy cards; no audible-quality grade |
| Account and recovery | Not scored | You summary observed; backup/restore source-only | No live recovery/account-switch experiment; score withheld |
| Personalization presentation | 8.0 | AMOLED/profile UI observed; four palettes source reviewed | Other themes and preference changes not physically reviewed |
| Copy and state clarity | 7.6 | Actual queue/update/help/settings | Paused/PLAYING contradiction, generic unavailable-update recovery, lengthy technical copy |

## Feature-by-feature inventory and review

O = observed current phone presentation/scoped behavior; S = source reviewed; U = full runtime acceptance unknown. Ratings above apply only to the stated presentation scope, not a blanket grade for every feature below.

| Capability group | Included features reviewed | Coverage / assessment / next work |
| --- | --- | --- |
| Guest/profile | Guest access, name/avatar, local identity | O/S; no forced sign-in observed; edit/persistence U |
| Home | Continue, recent shelf/full history, mixes, mood/query shortcuts, overflow/removal/Undo | O/S; reduce repeated visual weight; no history edits performed |
| Statistics | Local insights/trend/memory, privacy controls | S; require real empty/mature/disabled state and truthful time windows |
| Search | Saved/local corpus, public songs/videos, query modes, grouping, source/version evidence, recent queries, clear/retry/browser fallback | O/S; empty-return passed; live query recall/latency/cancellation U |
| Incoming links | Shared text/public link parsing, unsupported link handling | S; preserve no auto-playing incoming playlist; provider roundtrips U |
| Library | Liked, playlists, create/rename/pin/folders/filter/sort, selection/bulk copy/remove/Undo/reorder | O(empty)/S; populated and stale-state tasks U |
| Playlist exchange | Text share/export preview, bounded30/first500, canonical public URLs, UTF8 strict import, duplicate-name suffix | S; preserve code39 separator fix; real destination/provider/conflict/cancel paths U |
| Track options | Play Next, source details, save/offline options and collection actions | S; reachability and no wrong-recording actions need real tasks |
| Mini-player | Artwork/identity, play/pause, Next, hide/restore, expand/gestures | O; compact launch/restore and control states observed; long absence U |
| Full player | Artwork/ambient backdrop, progress/seek, previous/next, shuffle/repeat, like, details | O/S; soft artwork confirmed; seek/queue edits/shuffle persistence U |
| Queue | Current/upcoming, manual Play Next, remove/Undo, reorder/clear, shuffle/earlier sections | O/S; paused row labeled PLAYING confirmed; do not erase queue to review it |
| Lyrics | Owned LRC chooser/review, source binding, timed follow, editor, replace/delete | S; no service catalog; import/timing/editor runtime U |
| Sound | Device EQ capability/readback, presets, advanced bands/curves, active/draft Apply/Discard | O/S; screenshot reports5 bands/10 presets on this phone only; sound quality U |
| Local playback controls | Speed, crossfade/gapless, sleep timer, listening level/attenuation | S; route/crossfade/speed interactions and loudness accuracy U |
| Offline library | SAF/import, original source identity, availability/issues/probes, resume/filter/sort | O/S; native styling is heavier than main shell; decoder/airplane-mode tests U |
| Downloads/catalog/audio URL | Source eligibility, bounded range/ETag resume, pause/cancel/retry, file verification/import, throughput receipt | S; no bandwidth guarantee; controlled/source transfer and write/commit timing U |
| Recovery | Guest export/import/undo, optional Drive backup/readback/preview/merge/restore, history/preferences/LRC choices | S; existing privacy/account boundaries retained; live disposable-account acceptance U |
| Account consent | Optional Google sign-in/scopes, resolution/revoke/change account, busy/cancel paths | S; no account action exercised; no legal/security certification |
| External controls | MediaSession notification, widget, headset/Bluetooth, car browse/default clients | S; some play/pause UI observed, external surfaces/route correctness U |
| Themes | Midnight navy/cyan, Aurora purple/rose, Pearl warm light/blue, AMOLED black/mint | AMOLED O(8.2 visual); other3 S only, no fabricated rendered scores |
| Accessibility/language | English/Bangla resources, locale digits/Unicode, headings/actions, touch targets, keyboard/insets | S/O limited; TalkBack/Switch Access/large fonts, complete native translation U |
| Motion | Press scale, nav/player/surface/artwork transitions, reduced motion, haptics and seek-thumb policy | S; specification8.0 provisional, frame pacing/smoothness unscored |
| Updates | Launch/resume weekly policy, manual check, signed metadata/minSdk, bounded download/hash/package/signer and Android installer approval | O/S; code40 install continuity observed; manual request unavailable, feed activation not done; seven-day/new-release flow U |
| Diagnostics/support | Build details, copy/report preview, private crash summary, playback/storage recovery | O/S; no clipboard/account export invoked; reduction of technical copy still needed |
| Classic fallback | Retained native UI and advanced tools | S; platform auto-size applies26+, API23 fallback needs design work; no feature deletion proposed |

## Animation and theme assessment

Source: NightWaveMotionPolicy and NightWaveMattePolicy. Adjacent navigation220ms; surface240ms; content280ms; cover190ms; full-player enter260ms/exit180ms; general cap450ms. Press scale0.95/100ms, reduced-motion durations0. These are reasonable specifications (8.0 provisional design intent), not a smoothness/performance measurement. Verify gesture cancellation, settled selected tab, keyboard/picker return and reduced-motion behavior under frame traces before tuning durations. Keep accessible focus/touch-exploration seek-thumb visibility.

AMOLED is coherent but broad black gaps, many similarly weighted rounded rectangles and mint repeated on most actions reduce hierarchy. Midnight/Aurora/Pearl palette definitions are intentional; their complete rendered contrast and state behavior are unscored. No liquid-glass redesign or feature removal is recommended. Improve hierarchy/imagery/spacing within every existing theme.

## Confirmed findings versus investigations

- R01 CONFIRMED: queue25 says PLAYING while player24 behind it is paused; StudioPlayerScreens uses unconditional current-row/header wording. Use Current/Selected when paused, Playing only for real playing state; include buffering/error/disconnected semantics.
- R02 CONFIRMED VISUAL: player21/24 visibly enlarges soft video artwork. Audit requested/decode/display dimensions and reuse legitimate same-source sharper art; never silently change the audio recording or invent album art.
- R03 CONFIRMED VISUAL: queue header/current row, Sound readback/explanations and You rows are large relative to task content. Compact secondary information while retaining48dp actions, all controls and large-text expansion. This is a layout judgment, not measured tap efficiency.
- R04 CONFIRMED OBSERVATION: manual update shows generic failure/retry; feed remains inactive. Distinguish authenticated Disabled/NoCompatible/Available from unavailable/offline/error; do not treat an unsigned404 as authoritative disabled metadata. Delivery activation remains an owner approval gate.
- R05 SOURCE/LIMITATION: native/advanced copy remains English in places; API23 classic auto-size fallback is incomplete. No complete-app translation or older-phone UI grade is justified.
- R06 INVESTIGATE: Sound Studio hierarchy dump exit137; obtain compatible tool/accessibility evidence before diagnosing the app. Screenshot shows dense explanation near a switch, but actual overlap bounds were not established.
- R07 UNVERIFIED: long-absence/process-death, transfer speed, restoration, route interruptions, accessibility and battery. Unknown is not synonymous with broken; plan controlled evidence before changing engines.

## Upgrade direction

Use the new [NW146–NW153 roadmap](../UPGRADE_ROADMAP_NW146_NW153.md) and [structured backlog](../quality/NW146_NW153_BACKLOG.json). Original audits, source-batch history and NW145 critical gates remain evidence. No score increases automatically when a source batch compiles.
