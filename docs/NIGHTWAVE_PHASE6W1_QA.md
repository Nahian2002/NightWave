# NightWave Phase 6W1 — safe platform equalizer foundation

Development branch only; built on Phase 6V7B. The installed app and `main` are untouched.

## Implemented

- Opt-in Android platform equalizer tied to the Media3 audio-session ID.
- Service command `ACTION_EQ_PRESET_SET` with integer `EXTRA_EQ_PRESET`: 0 = disabled, 1..20 = device preset number 0..19 (only available device presets are accepted).
- Defaults OFF on every service start. No new permissions, network requests, file alterations, volume increases, queue changes, or persistent audio settings.
- Old effect released before attaching to a changed playback session, and on shutdown.
- Unsupported devices/presets fail open: audio playback proceeds unchanged.

## Not yet implemented

A user-facing settings screen, live preset enumeration, ten-band manual EQ, bass booster, gapless/crossfade, and loudness normalization. Do not describe this foundation as a complete equalizer release. Preset numbers are internal for now and must not be exposed before we can enumerate their actual names.

## Acceptance gates (still required)

1. Compile `assembleDebug` and `testDebugUnitTest` with Android SDK.
2. Test on real phone (speaker, wired if available, Bluetooth). Ensure default unchanged.
3. Switch a valid preset while audio plays, then disable; verify no crashes, no queue reset and no leaked effect after player exit.
4. Test unsupported preset, invalid negative value, unsupported equalizer implementation and repeated audio-session changes.
5. Confirm sleep timer, playback speed, local-file playback, public playback, media button controls, and app restart still behave as before.
6. Only then add a compact UI with actual device preset names and availability/fallback states.

No full Android compile or device QA is claimed for this commit.
