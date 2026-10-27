# NW127 — compact visual system and hierarchy

28 October 2026. Branch `update/nightwave-nw127-compact-hierarchy`, protected parent `0b21056456fb48017545909caf53c757d7b918c5` (NW126, PR #129). This is the next stacked source batch; the old default `main` must not replace this chain.

## Delivered behavior

| Surface | NW126 baseline | NW127 implementation |
| --- | --- | --- |
| Home | A 140dp default hero minimum, a repeated Continue pill, counters and another Library summary/action. | A 96dp minimum Continue row that grows with content; one Continue callback; existing Offline/Liked/Search shortcuts; music shelves and mood choices without the repeated counters/footer. |
| Recent shelf | Permanent 48dp options/removal row beneath every cover. | An overflow alongside the caption. Play and overflow have separate, nonoverlapping layout regions. Removal requires opening the menu; the original caller confirmation and recovery remain. |
| Mix cards | Text layered over partially visible artwork and a stretched empty area. | A bounded 72/88dp artwork region plus an opaque themed caption region. Titles, provenance and View tracks/Collapse grow intrinsically. |
| Mood cards | White captions over variable editorial gradients. | Decorative art stays above a themed, opaque caption. Required title/subtitle text uses the palette's tested surface colors. |
| Search | Repeated discovery eyebrow and promotional heading beside Offline. | A direct Search title; Offline moves below the title in narrow/large-type layouts, retaining its query and destination. |
| Library | Import, a scrolling Offline/Liked/History/New strip, real collection entries, and repeated New/Find buttons. | A visible New playlist/Find music group before the real collection entries; Import remains below those entries. Folder/filter/bulk/playlist actions retain their existing callbacks. |
| You | A repeated personalization eyebrow and Library/Sound/Backup subtitle. | A You heading with the existing truthful account badge; it reflows at narrow widths and large type. Account, sound, backup, recovery and App details remain available. |
| Shared shell | Fixed 56dp header; fixed navigation height with one-line labels. | A 52dp header minimum, a named compact brand mark when space is tight, and a 48dp Settings target. Navigation retains every label and tab semantic while its height can grow with wrapped text. |

These are source-level before/after facts. They are not measured screenshot pixel savings or a certified visual rating. A few cards deliberately give captions more space; the density gain comes chiefly from removing duplication and management rows.

## Shared layout contract

- `NightWavePremiumDesignTokens`: page top 8dp / bottom 24dp; page list gaps 8dp; card caption padding 12dp; caption gaps 4dp; section title 20sp/27sp. Body text and system font scaling remain intact. Existing shared 18dp card, 24dp hero/navigation and 12dp artwork radii remain.
- `NightWaveActionGroup` and `NightWaveSectionActions` inspect their actual available width using `BoxWithConstraints`. Below 360dp or at 1.3× text and above, related actions use a separate line. Unknown width chooses reflow.
- Shelves use 156dp tiles by default and expand to 156–260dp for larger text, retaining a next-item cue at the declared phone widths. Captions grow; no fixed text container height is introduced.
- Continue minimums are 96/132/180dp across the existing font-scale bands. They use `heightIn`, so these are lower bounds rather than clipping ceilings.
- Navigation uses intrinsic height plus its existing accessibility minimums. Each of four tabs retains its explicit full accessibility name, selected state and repeat-tap suppression.
- New controls allocate at least 48dp. Existing fixed 44/46dp Library and You action heights become 48dp minimums. Music metadata can still use intentional bounded title lines; action labels and the changed section/mix/mood captions wrap.
- Home, mix and mood required captions use opaque surfaces. Foreground, supporting and accent colors are checked against that exact surface in all four palettes at the normal-text 4.5:1 threshold. The palette math cannot certify every rendered screen or unknown provider artwork.

Android's [intrinsic measurement guidance](https://developer.android.com/develop/ui/compose/layouts/intrinsic-measurements) explains content-dependent row height; [touch-target guidance](https://support.google.com/accessibility/android/answer/7101858) explains allocating at least 48dp. Device rendering and assistive-technology checks remain separate.

## Regression protection

Six new JVM cases cover reflow at 320/360/412dp and 1×/2×/3× text, malformed width/scale inputs, bounded growing shelves, monotonic Continue minimums, every opaque-caption palette pairing and preserved text/touch sizes. The existing adaptive minimum assertions and standalone smoke expectations now reflect the new contract.

`NightWaveCompactHierarchyInstrumentedTest` supplies five component tests across 36 width/type/theme combinations (180 parameterized cases): navigation labels/bounds/callbacks, recent disclosure/removal isolation, section/action wrapping, mix caption overflow, and Continue/Settings callbacks. The cases save synthetic component PNGs to the test APK's cache when actually run. They do not launch the music service, use accounts, contact an artwork provider or modify a listener's library. The existing recent test now opens the actual disclosure before exercising Track options and removal; the Search header test expects the new direct label. Compilation does not mean these cases executed.

Two active Windows source guards were updated for the deliberately removed counter and permanent X row. NW59 still requires the order Continue → shortcuts → recent music → discovery and rejects a missing discovery fixture. NW52 now requires separate menu callbacks, the artwork Play callback, caller removal wiring and its existing confirmation. Playback, account, recovery, history and packaging safeguards are retained. Historical optional Python audits that encode old labels/layout markers remain dated evidence rather than current appearance certification.

## Pending physical acceptance

No connected device was available for this batch. There is no newly captured matched before/after set, executed Android matrix, rendered contrast report, TalkBack acceptance, or new product rating. NW126 installed-source/signer/data-preservation gates also remain open.

1. Use the protected NW126 APK/checkpoint and this candidate, first comparing the installed signer through the existing read-only procedure. Any authorized installation must be an in-place compatible update; never uninstall/reset to make a test pass.
2. Capture full Home, Search, Library and You with matched disposable content, visible selected-song state, scroll position, window/insets, theme, 320/360/412dp widths and 1×/2×/3× type. Keep actual hardware and build receipts with the images. The synthetic component matrix is additional evidence, not a replacement for those real screens.
3. Run the parameterized component class on a disposable authorized test target; export its `nw127-synthetic-*` test-cache directories with the exact installed APK/source receipt. Check every failure and screenshot. Do not interpret the existence of test source as a PASS.
4. Verify full action labels, nonoverlapping 48dp targets, menu dismissal/Back, correct Play/Continue/Track options/remove confirmation/Undo, visible Create, selected-track persistence while navigating and all source routes. Exercise TalkBack as well as touch. Measure required rendered text/control contrast in every theme, including bright and failed artwork.
5. Record the original 139-case and new 80-case matrix outcomes without erasing NW126 blockers. Re-rate only after comparable current product evidence exists. Public release remains NO-GO and the audit rating stays 6.7/10.

The next independent source batch is **NW128: unified shell and offline navigation**. NW131 retains the deeper Home empty-state/curation work; NW141/NW142 retain full accessibility/localization/adaptive acceptance. This batch does not claim those later outcomes.

## Executed local evidence

- Complete Windows builder: BUILD SUCCESS; production Kotlin/Compose, Android instrumentation-source compilation and debug assembly PASS.
- JVM: **1,502 tests / 189 suites**, zero failures, errors or skips; six new NW127 methods plus existing adapted regressions.
- Current source checks: **54/54 PASS**, including updated NW52/NW59 checks and existing packaging/provenance safeguards.
- Lint: **0 errors, 313 warnings, 17 hints**; unchanged totals, no new suppression or baseline.
- Embedded build ID: `NW-debug-343760FC90345D51`. Canonical source SHA256: `343760FC90345D518CBC1D978231391CC28883FF929A45F0DB47DF802224893B`, **811 inputs**.
- APK SHA256: `4F9B232F42A8CEC025A0A623A40BDE954F872C34422D9E5F598B4F2AB9B999D6`. Retained NW08 manifest fingerprint: `A8A902FD558D36E0A5FEFE10E4319C6EF39D62C9350574724A6C515FB1272CBD` (different documented ordering/encoding contract).
- Package `com.nightwave.player`, version `2.0.0-alpha5s`, code35, compile/target36/min26 preserved. Signer SHA256 `EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394` matches the saved prior APK; actual installed-phone comparison NOT_RUN.
- `adb devices -l`: no connected device. No app installation, account operation or user-library mutation occurred. All four NW127 acceptance rows are PARTIAL for source delivery with their physical evidence explicitly pending; device status NOT_RUN.

The private checkpoint retains build/source/signature/test/lint receipts, source ZIP and recovery Git bundles. Generated APKs, logs, captures and private recordings are not committed to Git. GitHub hosted execution is recorded separately on the PR; a local PASS cannot imply hosted CI success.
