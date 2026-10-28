# Matte design refresh — DESIGN01

Owner-requested visual work after NW133, 28 October 2026. Branch `design/nightwave-matte-system`, protected parent `f5e706b2a30fb2f629d0f1b0d6e223531f8f0c02` (NW133 PR #144). Implementation through `b30b7a2e4e9318ac2a91d7edb2418488c649cd03`. This is a separate design task; NW134 queue/transport implementation remains next. [Draft PR #145](https://github.com/Nahian2002/NightWave/pull/145), stacked on PR #144. Exact evidence: [current report](../../TEST_REPORT.md).

## Visual system

Matte, structural surfaces, full-color source artwork, clear typography and restrained selection accents. No liquid glass, metallic chrome, neon lime, new font download or invented recommendation service. Existing theme names and saved choices stay MIDNIGHT, AURORA, PEARL and AMOLED; no settings migration/reset occurs.

| Role | Midnight | Aurora | Pearl | AMOLED |
| --- | --- | --- | --- | --- |
| Canvas | #0A0A0A | #100F16 | #F9F7F2 | #000000 |
| Surface | #121212 | #17151F | #F0EDE7 | #0E0E0E |
| Raised | #1A1A1A | #22202B | #E5E0D8 | #181818 |
| Primary text | #FFFFFF | #FAF8FF | #24221F | #FFFFFF |
| Secondary text, fully opaque | #8E8E93 | #B3ADBF | #5F5A55 | #A0A0A5 |
| Restrained selected accent | #A1D6C9 | #CEC0EC | #61527F | #B1CEC5 |

Shared cards use 20dp corners; expanded art uses 16dp. The spacing rhythm remains 8dp, with 4dp internal caption gaps and 16dp card padding. Existing responsive page gutters, minimum 48dp touch bounds, system font scaling and the larger navigation/transport reflows remain. Decorative 5% strokes are separate from contrast-tested outlines and selected-state signals. Theme surface tint and tinted elevation are removed from the shared system; the native recovery/widget surfaces receive matching flat dark resource styling.

Button and icon-action wrappers use each real Material interaction source. A 100ms ease-out press changes drawn content to 95%, with no new gesture recognizer and no reduced touch bounds. Disabled states, callbacks and cancellation stay with Material. Reduce Motion makes the feedback immediate and unscaled. Existing card/row behavior and user navigation motion preferences remain.

## Screen architecture and preserved features

- Home: real Continue/empty entry, existing shortcuts, Recently Played, saved next-listen rail, a full-width existing mix, then measured paired mix cells. All mixes remain; narrow/large-text cells stack in the same order. Mosaics contain up to four actual source thumbnails, not generated album covers. Every mix still opens its explanation/tracks before explicit Play. Recent expansion/history/clear confirmation, moods/search shortcuts, offline and library destinations remain.
- Search and Library retain compact lists and complete existing source/search/bulk/document/Undo behavior. The system changes their palette, shared shapes and action feedback; it adds no feature disclosure or new hidden destination.
- Mini/full player retain selected metadata, hide-without-stopping, Next/Previous, modes, Save/Like, queue, lyrics and all existing secondary tools. Full Play is a flat monochrome circle, with inverse light-theme treatment. Metadata and status keep their prior identity/health policies.
- Expanded artwork atmosphere reuses the successful selected CoverArt drawable. It creates no second request/provider. The existing bounded candidate/cache policy remains; large requests allow software decoding for a bounded 96px background copy. Android31+ uses a static 60dp blur, clipped viewport and rectangular edge treatment at at most 15% opacity (8% in Pearl). Android26–30, missing art and copy failures use the solid matte background. Source/identity changes clear the backdrop; outgoing frames cannot set the current image. Actual image/cache/GPU cost and visual appearance remain phone acceptance tasks.
- Seek keeps its 48dp interaction area and 4dp monochrome drawing. The thumb appears during touch/drag, keyboard focus or touch exploration. Left/Right/Home/End, accessibility progress and touch still commit through the existing track-bound seek guard. Touch-exploration listeners are removed on disposal.
- Native Offline, supported Catalog, Downloads and Drive Locker adapt text, surfaces, warnings and bright action pairs to the chosen palette. Startup reads the saved theme without creating a profile or writing preferences. Native dialogs/system bars also follow light/dark mode. Existing workers, file/picker paths and account/consent/recovery operations are unchanged. Classic recovery remains available with its coordinated flat dark resources; it is not a new four-theme classic UI.
- You, Account, Sound Studio, sleep/audio/lyrics tools and diagnostics retain their actions and existing disclosures. Shared theme/button styling and flattened headers apply without changing EQ, recovery, consent or effect state.

## Implementation seams

`NightWaveDesign` owns the four ARGB palettes. `NightWavePremiumDesignTokens` and `NightWaveMattePolicy` define shared geometry/feedback/layout rules. `NightWaveTheme` supplies Material roles, flat shapes and platform appearance. `NightWaveMatteComponents` supplies stable interaction-source wrappers, the measured Bento pair and selected-art backdrop. Native adapters map existing explicit text/surface roles without writing preferences. Source action names and handler wiring remain at their original callers.

The NW59 source preflight now checks the approved Pearl #61527F accent instead of the former #815154 literal; actual color contrast remains required by JVM tests. Existing layout tests adopt the approved 16dp player-art radius while retaining every size/reflow/transport assertion. No assertion or failing suite is removed. One narrowly scoped Kotlin deprecation suppression documents required Android26–34 bar-color setters; no lint baseline, lint suppression or broad warning suppression is added.

## Acceptance evidence and limits

Fourteen new executed JVM methods cover all four palettes on opaque surfaces, monochrome transport, meaningful outlines, worst permitted artwork backgrounds, native text/warning/action pairs, invalid and real measured Bento inputs, API gating, Reduce Motion and input-mode thumb visibility. Existing regressions remain.

Eight new Android methods parameterize 320/360/412dp × 1×/2×/3× text × four themes: 288 planned cases. They use actual Home, Search, Library, player, navigation, mix cards and button/backdrop components with synthetic in-memory callbacks and blank/provided local bitmap art. Tests include source-preserving explicit mix playback dispatch, exact query submission, Create/import dispatch, Save/queue/lyrics/scoped seek, independent targets, press cancellation and API-specific backdrop rendering. Their screenshot files are created only when Android execution actually occurs. Compilation is not a rendered pass.

The [design acceptance ledger](MATTE_DESIGN_ACCEPTANCE.csv) keeps these 288 cases plus 48 manual theme-specific checks NOT_RUN until actual execution. It supplements, and does not rewrite or certify, the original 139+80 acceptance cases. The 219-case artifact-bound pack is preparation only. No phone/emulator was attached or installed; no actual screenshot, native dialog/picker, notification, acoustic, restart, upgrade or GPU/battery acceptance is claimed.

Private checkpoint: `C:\Users\jayed\NightWave-checkpoints\20261028-matte-redesign`. Keep exact APK/provenance/source ZIP, Git bundle, raw JUnit/lint/source/feature-preservation receipts and case pack there. Actions remains disabled; no APK is uploaded. Source-only publication is separate from app distribution. The provisional 6.7/10 score and release NO-GO remain until real acceptance/re-rating. Next source batch: NW134.
