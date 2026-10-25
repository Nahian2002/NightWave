# NW58 — Now Playing overlay opacity during motion

**Parent:** `feature/nightwave-nw57-home-first-fold`  
**Branch:** `feature/nightwave-nw58-player-overlay-polish`  
**Status:** Source updated, phone animation QA pending.

## Problem from 25 October recording
Expanded Now Playing briefly showed faint Home content behind it around the start of the player animation. Previous code applied fade-in and fade-out to the *entire* full-screen player, including its supposed opaque background. The underlay could therefore remain visible during fade.

## Fix
- Compose `MutableTransitionState` keeps track of actual enter/exit animation lifetime.
- A separate solid `colors.background` surface is shown from the first requested expand frame until the exit animation finishes. The foreground player retains NW50's capped short slide-and-fade animation.
- Target state is set inside `LaunchedEffect(expanded)`, not while composing; backdrop visibility also checks the immediate `expanded` request to avoid first-frame transparency.
- Playback, audio queue, lyrics, gestures, reduced-motion mode and navigation callbacks are unchanged.

## Evidence
- `NightWavePlayerOverlayPolicyTest` pure tests cover first entry, intermediate frames and exit completion.
- `tools/verify_nw58_overlay.py --self-test` guards real Compose wiring, foreground animation durations and the opaque surface, with negative fixtures.
- Full Kotlin/Compose compilation and actual animation smoothness are **not verified** by source inspection. Compare another recording on the same Samsung after the integrated build.

## After this batch
NW53–NW58 are stacked in this branch. Before installation, run the original Windows `build-windows.cmd`, collect real compile/test evidence and verify APK signing continuity. Google error 8 and actual offline playback are still device acceptance blockers.
