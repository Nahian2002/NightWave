# NightWave 6N3 — Safe listening-level controls

Stacked on 6N2. Stable main untouched. True device-EQ/crossfade and sample-accurate gapless remain separate, unverified features.

## Implemented
- Expanded Now Playing gains **Listening level** with three explicit choices: Full (100%), Comfortable (85%), Quiet (70%).
- The Media3 PlaybackService applies an explicit output volume multiplier between 0.7 and 1.0, rather than changing the phone's hardware/system volume or applying an untested audio-effects engine. These presets attenuate only; **never amplify**.
- One private bounded integer persists per app, read safely even from malformed stored preference types; malformed values fall back to 100%.
- A selected preset is applied to the only ExoPlayer in the service, for both the app's public streaming mode and private offline audio. The same level is applied on service recreation, but service startup still does not autoplay music.
- Preferences don't contain song URLs, private files, player state, history, tokens or accounts.
- Existing offline-speed feature (6N1) remains independent. Changing listening level does not seek, stop, replay or modify the queue, and does not affect the sleep timer.

## Verification
- 10 pure JVM test methods authored for all presets, labels, safe fallback, lack of boost and rejection of unknown values.
- Phase 6N3 Python source-regression gate registered in GitHub's stacked Android workflow.
- If GitHub Actions fails before acquiring a runner, Android compile and on-device behavior remain **unverified**.

## Device QA deferred at user's request
- On a playing public song and owned offline file, change 100↔85↔70%; compare amplitude, pause/resume and next track. Check wired/Bluetooth audio, lock screen, speakers, Android audio focus and DSP compatibility.
- Restart service without autoplay and manually resume. Verify the saved listening level, imported-audio speed choice, queued songs, favorites, backups and listening privacy settings.
- Check low-height devices, larger text settings and TalkBack menu usability. Verify exact signing identity before installing over a previously signed NightWave build.

## Not implemented
This is attenuation, not an equalizer, compressor, loudness normalization or crossfade. Android has an optional third-party/vendor system audio-effects panel, but it is not universally available and can depend on a valid audio-session ID. NightWave does not misrepresent a missing vendor panel as a built-in EQ.
