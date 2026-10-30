# NW142 adaptive layouts and input

## Committed implementation plan — 30 October 2026

Cumulative parent `e400a8b15cefcd8d56ef8e99dbd225edeec9d680` (NW141 draft #163). Branch `update/nightwave-nw142-adaptive`. Verified private baseline bundle and exact tracked working-byte ZIP: `20261030-nw142-adaptive`, outside Git. Existing physical gates remain NOT_RUN.

1. Inspect actual shell/player/queue/dialog constraints at narrow, landscape, tablet, split-screen and 1x–3x text. Replace fixed-height text-bearing action rows with growing minimum-height/reflow layouts. Keep every control/callback and existing theme/data/source/account authority.
2. Give tablet dialogs a bounded readable width and ensure short-window headers/footers cannot consume the entire scrolling body. Make queue summary/help/actions participate in scrolling, preserving edit snapshots, independent rows, move/remove/Undo and shuffle semantics.
3. Dismiss obsolete input focus on a completed tab change without clearing the query, destination or selected track. Keep platform IME/native input handling and the existing safeDrawing inset owner; avoid duplicate keyboard padding or global key interception. Preserve retained picker/import state and Back precedence.
4. Add pure malformed-constraint/budget regressions plus real component profile/input tests. Prepare physical keyboard/picker/rotation and all-theme profile cases; compilation is not execution. Run current source checks, the full Windows builder, then serial lint and signer/source/recovery checks before a cumulative draft PR and owner-only streamed-hash-verified APK.
5. Keep original acceptance and full profile/input matrix open without actual device evidence. Advance to NW143 only after complete local verification. If the weekly reserve is reached, preserve work and truthful pending checks, delete the automation, and do not deliver an unfinished build. No automatic install/reset/uninstall, app account action, merge, public binary or Actions/settings changes.

This batch keeps the full NW142 acceptance scope. Source improvements and profile-test readiness do not prove actual keyboard/system-picker/rotation, physical font/contrast/focus or tablet behavior. References: [Android insets](https://developer.android.com/develop/ui/compose/system/insets), [keyboard handling](https://developer.android.com/develop/ui/compose/touch-input/keyboard-input/commands).

## Weekly reserve stop — 30 October 2026

Hourly automation `nightwave-sequential-upgrades` was **deleted** when the verified weekly window reached **10% remaining**. Do not restart after reset without a new owner request. Preserve and resume the unfinished NW142 batch before advancing: branch `update/nightwave-nw142-adaptive`, plan `7007a50da9b822cd7ca96a5568e40b55130a50d1`, implementation `1556d0d610800e8e85aab0d3eb5e9c770e73385c`. Private recovery/evidence: `20261030-nw142-adaptive` outside Git. No NW142 APK, PR, push, merge or delivery was performed.

Initial JVM/Android compilation passed (1,812 JVM / 224 suites, zero failures/errors/skips), before the final queue test was added. The initial current-source runner **FAILED one of 83 checks**: `check-nw52-integration.ps1`, “restorable mini-player tap targets” marker, following the fixed-height-to-minimum-height change. This is not repaired or re-verified. The final queue test has not compiled. Full Windows builder, final source checks, sequential lint, APK/source/signer/delivery verification and all physical/input/profile cases are **NOT_RUN / pending**. No new rating or acceptance is earned. Last fully verified delivery remains NW141, draft #163.

### Saved implementation and required continuation

- Player text-bearing actions and mini-player row use growing minimum heights; the audio details header stacks on narrow/large-type windows rather than shrinking labels.
- Sound Studio has a 600dp maximum width; actual dialog constraints cap independently scrollable header/footer areas and leave the settings body a share of the height. Account header follows the same measured-height policy.
- Queue heading/summary/edit/help is an item in the same lazy viewport as actual rows; snapshot/selection/move/remove/Undo callers remain. Existing safeDrawing IME ownership, settled-tab focus dismissal, native editor handling and retained picker state are preserved, not duplicated.
- Four pure viewport tests passed. Two parameterized real-component methods span 72 width/height/font/theme combinations (144 compile-only cases) in the initial successful compile. A separate actual long-title/3x queue-scroll test was added afterward and is NOT_COMPILED. No Android case executed.
- Next authorized run must inspect this checkpoint, repair/strengthen the existing NW52 guard for the actual minimum-height callback path, compile the final tests, run all current checks, the complete Windows builder and then serial lint. Compare notices, source/data/callback boundaries, real previous APK signer and final artifact identity. Prepare/attach the cumulative draft PR and verify owner-only Drive streamed bytes/hash only after successful validation.
- Ten supplemental cases in `quality/NW142_ADAPTIVE_ACCEPTANCE.csv` retain the full original profile, IME/picker/rotation/Back/hardware-input/data scope. They and every prior physical gate stay NOT_RUN.

##31 October owner override result

Preserved NW142 source is now cumulatively host-verified with the [weekly updater](WEEKLY_UPDATER.md) (see TEST_REPORT.md):1817 JVM/226 suites,84 source checks,full Windows builder and serial lint PASS. All physical cases remain NOT_RUN. Updater stable feed disabled pending explicit public-release/main approval. The one-time allowance override does not restart hourly automation. Next independent roadmap source batch NW143.
