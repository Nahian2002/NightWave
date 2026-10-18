# Phase 6W2 — audio settings + bug audit

## Scope
- You > Appearance & playback > Audio equalizer opens the opt-in system preset dialog.
- OFF is the default every time the playback service starts.
- Presets are deliberately generic *device preset N* labels; Android devices do not expose a universal set of named presets.
- Selecting a preset sends an explicit service Intent. The service rejects out-of-range values, and releases the effect on audio-session change/service exit.
- Existing EQ foundation and all player/library data stay intact.

## Known limitations (must not claim complete or verified)
- Dialog selection is a command, not an acknowledgment of active effect. Android may reject an unsupported preset silently; no active-state display is promised.
- No manual EQ bands, per-route capability reporting, preset persistence, volume normalization, gapless, or crossfade.
- No Android Gradle build, instrumented tests, or physical-device QA completed here.
- Potential Android-specific compile and API compatibility must be checked using build-windows.cmd.

## Safety / regression checks
1. Run Gradle assembleDebug and testDebugUnitTest on Windows builder.
2. Install without uninstalling or clearing app data; confirm signing compatibility.
3. Open You > Appearance & playback > Audio equalizer; choose Off, then preset 1; exit/reopen app, and confirm no crash.
4. Test local MP3, streaming audio, Bluetooth route switch, next/previous, lock screen, offline speed, and sleep timer.
5. Verify unsupported audio effects never stop playback; inspect logcat for Equalizer failures or SecurityException.
6. Check landscape, minimum supported Android version, large fonts, and screen reader focus.
7. Add service-to-UI state/capability query and real device preset labels before declaring complete audio settings.

## Audit notes
- Do not claim that a successful startService proves an equalizer is active.
- Do not assume system preset #1 is identical across phones.
- Avoid changing player ownership and queue transition paths for cosmetic features.
