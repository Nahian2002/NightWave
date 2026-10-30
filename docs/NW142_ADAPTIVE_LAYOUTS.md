# NW142 adaptive layouts and input

## Committed implementation plan — 30 October 2026

Cumulative parent `e400a8b15cefcd8d56ef8e99dbd225edeec9d680` (NW141 draft #163). Branch `update/nightwave-nw142-adaptive`. Verified private baseline bundle and exact tracked working-byte ZIP: `20261030-nw142-adaptive`, outside Git. Existing physical gates remain NOT_RUN.

1. Inspect actual shell/player/queue/dialog constraints at narrow, landscape, tablet, split-screen and 1x–3x text. Replace fixed-height text-bearing action rows with growing minimum-height/reflow layouts. Keep every control/callback and existing theme/data/source/account authority.
2. Give tablet dialogs a bounded readable width and ensure short-window headers/footers cannot consume the entire scrolling body. Make queue summary/help/actions participate in scrolling, preserving edit snapshots, independent rows, move/remove/Undo and shuffle semantics.
3. Dismiss obsolete input focus on a completed tab change without clearing the query, destination or selected track. Keep platform IME/native input handling and the existing safeDrawing inset owner; avoid duplicate keyboard padding or global key interception. Preserve retained picker/import state and Back precedence.
4. Add pure malformed-constraint/budget regressions plus real component profile/input tests. Prepare physical keyboard/picker/rotation and all-theme profile cases; compilation is not execution. Run current source checks, the full Windows builder, then serial lint and signer/source/recovery checks before a cumulative draft PR and owner-only streamed-hash-verified APK.
5. Keep original acceptance and full profile/input matrix open without actual device evidence. Advance to NW143 only after complete local verification. If the weekly reserve is reached, preserve work and truthful pending checks, delete the automation, and do not deliver an unfinished build. No automatic install/reset/uninstall, app account action, merge, public binary or Actions/settings changes.

This batch keeps the full NW142 acceptance scope. Source improvements and profile-test readiness do not prove actual keyboard/system-picker/rotation, physical font/contrast/focus or tablet behavior. References: [Android insets](https://developer.android.com/develop/ui/compose/system/insets), [keyboard handling](https://developer.android.com/develop/ui/compose/touch-input/keyboard-input/commands).
