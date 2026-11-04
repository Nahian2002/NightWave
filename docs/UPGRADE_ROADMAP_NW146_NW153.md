# Upcoming upgrades NW146–NW153 — code40 review

4 November2026. Active successor to the locally verified NW126–NW144/COMPAT01 source work. Those batches are historical implementation milestones, not proof that the app is perfect. [Current review](audits/2026-11-04_CODE40_PRODUCT_REVIEW.md), [scorecard](quality/CODE40_REVIEW_SCORECARD.json), [structured backlog](quality/NW146_NW153_BACKLOG.json).

Provisional presentation/usability index7.83/10 across92/100 original rubric weight; complete product rating unavailable. Preserve all features/themes, source URLs/quality, data formats/installed data, package/signer and account/consent boundaries. Improve the established design rather than restart a whole-app theme project.

| Order | Batch | Priority | Concrete outcome |
| --- | --- | --- | --- |
| 1 | NW146 | P0 | Truthful playback and update states: Current/Playing/Paused/Buffering/Error labels agree across mini/full/queue and accessibility descriptions. |
| 2 | NW147 | P1 | Compact secondary UI without losing controls: Reduce repeated header/card/copy weight in Home,You,queue and Sound Studio using measured viewport budgets. |
| 3 | NW148 | P1 | Source-correct artwork quality: Measure requested/cached/decoded/displayed art sizes; recover sharper legitimate same-source imagery where available. |
| 4 | NW149 | P1 | Four-theme and motion consistency: Review every theme at Home/Search/Library/You/player/queue/offline/dialog states with measured contrast. |
| 5 | NW150 | P1 | Search offline and lyrics journey finish: Exercise live public and saved/local queries, versions/cancellation/stale results and exact source identity. |
| 6 | NW151 | P1 | Older Android language and accessibility finish: Backport classic text sizing on API23–25 without removing the native fallback; validate bounded text/wrapping. |
| 7 | NW152 | P1 | Recovery sharing and release readiness: Verify share/file-provider/picker/rotation/cancel/name-conflict and data-preserving receipts with disposable fixtures. |
| 8 | NW153 | P0 | Final measured acceptance and re-rating: Close original219 plus supplemental/new cases with exact source/APK/signer and device artifacts; repair demonstrated critical/high failures. |

## Execution rules and evidence

NW146 is LOCAL_VERIFIED (code41); see docs/NW146_STATE_TRUTH.md and the structured evidence receipt. NW147 is LOCAL_VERIFIED (code42); see docs/NW147_COMPACT_SECONDARY.md and its structured receipt. Start NW148 next. Run one bounded source batch at a time from the cumulative review tip with a private recovery checkpoint and committed plan. Use meaningful regressions, full current checks/Windows builder and sequential lint for affected app changes; verify source/APK/signer/private delivery and create a draft review. Passing checks establish local readiness, not a score. Source-only work may continue with explicit phone gaps pending; device/account actions must respect the owner's exact authorization.

No schedule was created or restarted. Do not install a new APK, reset data, change accounts, activate the public feed, publish a binary, merge or change repository settings incidentally. Owner separately approved code40 and code41 installation with non-destructive connected-phone checks. Later installation needs its own explicit scope. NW145 remains the cross-cutting acceptance record; no original matrix is silently marked PASS.

Target9.5+ is conditional: measure actual tasks, complete the original full rubric with no core category below9, close unknown critical gates, and keep release approval separate. Reference-specific A/B screenshots, contrast/action bounds and raw performance/audio/power evidence are required where relevant. Source and unknown functional areas are not assigned optimistic operational grades. Spotify-scale catalogs, remote handoff, collaborative/social/AI services are outside these batches unless separately scoped; add no nonfunctional controls.

## Current practical priorities

First correct the paused/PLAYING contradiction and update-state messaging. Then reduce secondary panel weight and improve same-source artwork. Follow with all-theme/motion and offline/language/accessibility work. Recovery/release and full measured acceptance finish the sequence. For R06, investigate tool/OS/UI semantics evidence before calling hierarchy dump137 an app defect. Private screenshots and personal content remain outside Git.

[Draft review #171](https://github.com/Nahian2002/NightWave/pull/171), stacked on phone evidence #170; unmerged.
