> **29 October acceptance correction:** the [new owner recording](2026-10-29_ARTWORK_PAGER_REVIEW.md) exposes tiny native-size artwork and split pages. The current correction supersedes Inside with proportional Fit, uses larger balanced mosaics and adds shelf isolation, settled selection, tab reselect and idle pager repair. Read TEST_REPORT.md for the current candidate; original intentions below are retained as history.

# DESIGN03 — music-first content and presentation finish

Owner-authorized single-session implementation of all six recommendations from the `Screen_Recording_20261028_232750.mp4` review. Branch `design/nightwave-content-finish`, protected DESIGN02 parent `6d664ba4111cbd87c9adf28e6600ca0fef25f982`. See [current exact verification](../../TEST_REPORT.md) and [acceptance supplement](CONTENT_FINISH_ACCEPTANCE.csv). [Draft PR #147](https://github.com/Nahian2002/NightWave/pull/147) is a separate review on top of DESIGN02; NW134 remains the next feature batch.

## Recorded baseline

The prior review sampled the approximately 92.93911-second owner recording: 50,378,437 bytes, SHA256 `e6eb4f476395fadbcee6537797c83997d44428f29ad830b2bf34c77aca6b6d44`, 1080×2340, 5,943 frames at reported 63.94509 fps. 92 one-second seeks decoded at 0–91 seconds; 92s failed. A three-second overview and detail frames at 6, 12, 18, 21, 27, 30, 36, 42, 54, 60, 63, 69 and 78 seconds were reviewed. Media rate is not app performance. Exact APK identity is not exposed in inspected frames.

Observed appearance/UI score **8.4/10**, using the same five equal dimensions: identity 8.5, color 9, hierarchy 8, screen consistency 8.5, perceived readability 8. The six recommendations below are drawn from that sample; no new-candidate score is awarded from code/tests. The original whole-product audit remains frozen at provisional 6.7 and is not a current complete runtime re-evaluation. A 9.5–10 result requires rendered and task acceptance.

## All six recommendations implemented

| Recommendation | Actual change | Preserved behavior |
| --- | --- | --- |
| Artwork presentation | Home/recent/next-listen/collection artwork shares the existing source-bound CoverArt renderer. Large previews retain the entire source with ContentScale.Inside, avoiding crop zoom and enlargement beyond loaded pixels. Existing bounded same-video candidates, cache, missing-image fallback and identity-checked player ambience remain. | Original track/art URLs, third-party/signed/local URI handling, bounded fallback and all playback/options actions. No generated covers, provider expansion or new dependency. |
| Home hierarchy | Continue remains the single primary action. Four-image mix mosaics become bounded 72dp accents instead of full-width square walls; featured mix art also reduces to 72dp. Titles and counts lead, while the existing preview keeps complete membership reasons. | Every mix, all four original thumbnails, selection, explanation, Play mix, more tracks, recent expansion/removal/history/Undo paths. |
| Search music first | Empty-query saved starter music moves before history, offline/mood and creator browsing. The existing four mood filters use a compact snapping row; offline browsing becomes an inline action. | All starter rows, exact public/offline matches and paging, history/remove/Undo, modes, creator filters/all-creators list, public/official alternatives and explicit Search prefill ownership. |
| Shorter copy | Home/Library summaries remove repeated qualifiers, duplicate equal song/history counts and the redundant Automatic collection phrase under its existing heading. Distinct repeated-play and membership counts remain. Profile group spacing tightens without shrinking row targets. | Counts, origins, history versus Save distinction, manual confirmations, feature descriptions and local-first/privacy/account disclosures. |
| Consistent text colors | Normal Library content explicitly uses onSurface. A distinct, opaque tinted error container prevents the former surface/errorContainer color alias from resolving ordinary implicit content to error ink. Real error text retains tested contrast. | All four palette values and stored identities; genuine error/selection/health roles stay meaningful. |
| Spacing and interactions | Home recent/next/mood shelves snap to item starts using the already installed Foundation API. Shared gutters align header/mini-player/navigation; secondary actions use 14dp corners. Standard player art has a 360dp cap and a smaller height share, with tighter noncompact header/metadata spacing. | Every tab label/target/selected state, haptics, reduced motion, swipe/Back, large-type reflow and reachable secondary actions. |

The source cannot create detail absent from a low-resolution image. The quality change is honest containment and best available same-source artwork, not a claim of recovered pixels. A partially visible neighboring shelf item is an intentional preview; settling behavior is now snapped.

## Architecture and regression coverage

Presentation changes stay in UI files. `NightWaveContentPresentationPolicy` contains the distinct error-container blend and concise count/membership formatting. It writes no storage, sends no request and changes no schema. Shared renderer/caller wiring keeps the existing NW133 source-bound artwork and NW131 Home policies authoritative.

New JVM tests cover error-surface separation from ordinary surfaces, actual danger-text contrast in every theme, distinct unique-song/repeated-play meaning, empty/inconsistent counts and retained membership evidence. The prior tablet art-bound assertion is updated to the deliberate 360dp cap; existing narrow/large-text/transport safety assertions remain.

Two actual UI methods extend the existing 36-configuration matrix to **12 × 36 = 432 planned cases, compile only**. They verify music visible before browsing and retained browse/play callbacks, plus generated owner-fixture image edges/native-size containment without a provider request. The existing theme method checks Material contentColorFor(surface) and a distinct error container. Compiled tests do not mean phone execution.

`verify_content_finish.py --self-test` adds ten broken caller fixtures for ordering, all three snap rails, aspect/native-size treatment, source identity, normal content colors, error-role wiring and bounded mosaics. Existing checks/negative fixtures remain; no assertions or lint baseline are removed.

## Required device acceptance

Use the new build ID from TEST_REPORT.md. Review four themes at 320/360/412dp and 1×/2×/3× type; play/select/open options from every affected shelf; verify all six mood cards settle readably; inspect missing, low-resolution, wide, square and local artwork; confirm Search music order, typed/cleared draft retention, creator browsing and all provider/offline alternatives. Check normal versus error text and disabled controls, native/system bars, TalkBack/keyboard, frame pacing/cache behavior and installed-data continuity. No install/reset/account/data operation is included in local verification. Prior audio/performance/recovery/release gates carry forward. Actions stays disabled and public APK distribution remains unapproved.
