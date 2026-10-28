# DESIGN06 - player continuity and readable listening surfaces

29 October 2026. Owner-authorized fixes following the latest recording review. [Draft PR #150](https://github.com/Nahian2002/NightWave/pull/150), branch `design/nightwave-continuity-finish`; protected DESIGN05 parent `b6c6b066cfb9052c74d83b63a5e3233e4cd92392`. See [current verification](../../TEST_REPORT.md), [24-case supplement](CONTINUITY_FINISH_ACCEPTANCE.csv). NW134 remains the next feature batch.

## Recorded baseline and evidence boundary

`Screen_Recording_20261029_023412.mp4` is 87,684,653 bytes, SHA256 `7ec3583aee62cf31102b48b98400f367c4d0819929b5f6657d54c41d878bf26b`, 1080x2340. Sequential decoding read all 11,577 frames with monotonic presentation timestamps through 198.2001 seconds; visual review used 199 one-second samples, four-second overviews and detailed frames. Encoding rate is not an Android performance measurement. Exact inspected binary identity is not exposed.

The fixed five-dimension subjective visible-UI score is **8.7/10**: identity 8.5, color 9, hierarchy 9, observed coherence 8.5, perceived legibility 8.5. Main title cleanup, concise summaries, useful Library and quieter You are visible. Theme-appearance diagnostics: AMOLED 9.1, Midnight 9.0, Pearl 8.7; Aurora lacks sufficient current evidence and is not freshly rated. No new whole-product rating replaces the original 6.7 audit.

An empty viewport sample around 01:50 lies between settings at 01:49 and the player at 01:51. It is real in sequential samples, but those samples alone do not establish exact duration, root cause or a persistent freeze. Source inspection identifies a matching transition gap: backdrop visibility followed the immediate request, while content waited for a LaunchedEffect to target a separate MutableTransitionState and then faded from zero alpha. This correction addresses that verified source gap; physical reproduction remains required.

## One transition owns the player

A direct `updateTransition(targetState = expanded)` now drives both content and backdrop lifetime. Player entry/exit use existing bounded translation and durations, without a full-screen alpha fade. Reduced motion retains immediate transitions. No effect-delayed content target leaves an opaque but empty first viewport.

The original backdrop policy also observes transition running state, covering enter reversed before current/target ever settle true. It remains opaque through interrupted exit and disappears with the completed transition. Home must not show through the animated player. All metadata, art identity/fallback, queue, gestures, seek, lyrics, offline, Sound, controls and source Details remain.

## Readable actual listening statistics

`NightWaveListeningPresentationPolicy` formats real durations, concise UTC dates/periods and measured metric columns. Positive sub-minute time displays less than one minute rather than zero; long time uses hours/minutes. Cross-year periods retain both years. The UI retains explicit UTC context and real 7/30-day grouping from the unchanged ListeningTrendEngine.

Metric cards stack at narrow widths/large system type. Trend labels have their own row above flat comparison bars; slider-like stop dots no longer imply an input control. Each row exposes the full UTC date/range and recorded time to accessibility services. Consent, Refresh, Erase & turn off and both windows retain independent targets and callbacks.

Copy uses recorded time, sessions (30s+) and different songs, with a concise explanation of active playback, 30-second qualification, local privacy and incomplete observations. No microphone, cloud transfer, all-time coverage or full-play claim is introduced. Recorded song labels use the same conservative display identity while original records stay intact. Recent-list insights share measured cards and explain different songs versus play counts. No consent default, recording logic, pruning, storage/schema, exports or counts change.

## More distinct real Home mixes and covers

Daily selection, Another direction, Creator blend and Listening history have distinct labels and truthful purposes grounded in actual keys/membership. A history label requires all displayed tracks to belong to the actual history set. Mix membership, order, ranking, playback, source URLs, reasons and preview/Play actions are unchanged.

Mosaics choose up to four distinct real tracks, preferring existing artwork and creator variety with deterministic mix-key rotation. They remain stable for unchanged inputs and do not reorder queues. Every cover is from that mix's tracks; missing art retains real-source fallback. This reduces repeated leading covers without generated art, fabricated albums or new providers. It cannot recover detail absent from source images or remove embedded promotional graphics.

## Shared settings rhythm

Settings headings use sentence case and readable label size. Header/divider/row insets align at 12dp; decorative icons are 36dp while rows retain at least 64dp interactive height and large-type growth. The storage subtitle describes the user outcome. All settings, account/consent/recovery, themes/avatar, memory, diagnostics, native/classic and data paths remain available.

## Verification and remaining acceptance

New JVM regressions cover interrupted backdrop lifetime, readable duration/UTC/calendar/reflow, representative cover stability/source membership/creator variety/missing art and honest distinct Home roles. Existing edition/source identity tests remain. NW58's original negative fixtures are retained and expanded for alpha/delay; the current source runner includes the updated guard plus a continuity caller guard. The cumulative NW59 preflight now requires the single synchronous owner and running-state retention instead of the superseded effect-delayed source pattern; its earlier fixtures remain and six player negatives were added. The first build stopped at this old preflight before Gradle; the updated guard is verified before a fresh build attempt.

Two added Android methods provide a clock-controlled first-frame/exit-reversal pixel assertion and actual 7/30-day, consent, Refresh and Erase callback checks at the 36 configurations. The matrix is **16 x 36 = 576 compiled cases only**, pending execution. No rendering proof is inferred from compilation or source strings.

Phone acceptance must check first/open/close/reversal/reduced-motion/theme changes, actual four-theme layout and missing/wide artwork, readable small/large type, TalkBack/keyboard, all stats/mix/settings actions, shelf versus page ownership and prior audio/data/recovery/performance gates. No new 9.5-10 rating is claimed. Public source and private owner APK delivery remain separate; Actions stays disabled and public release remains unapproved.
