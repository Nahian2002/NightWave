# NightWave 6W6 — Equalizer reliability audit

## Changes committed
- Capabilities now carry the exact Android audio-session identifier.
- On audio-session transitions, stale metadata is invalidated before EQ reattachment.
- Manual batch updates are refused unless the current Media3 audio session matches the probed session.
- Invalid, stale or out-of-range batch requests preserve the actual requested/active EQ state instead of falsely reporting manual mode as selected.
- Existing library, queue, playback, backups and audio files were not intentionally modified.

## Source checks completed
Read back updated service and capability source from GitHub and verified the matching publish signatures, session check and route-change invalidation are present.

## Regression test matrix to run on Android
1. Gradle compile: run build-windows.cmd, ensure assembleDebug and testDebugUnitTest pass. Review Kotlin errors.
2. Start playback, discover EQ capabilities, then switch Bluetooth/speaker routes before pressing Apply: stale input must be rejected.
3. Probe caps twice on the same audio session; verify consistent labels/ranges.
4. Send malformed array, array of wrong count, and out-of-range gain: current sound and active status must not be reset.
5. Repeat play/pause, skip, background playback, offline audio, sleep timer, reboot and resume.
6. Test session restart, unsupported effect devices, large fonts, five-or-fewer/more-than-five bands, and service stop.
7. Test effect status wording against real audible behavior; do not treat the Android enabled flag as proof of sound processing.

## Known remaining issues
- No full Android build, automated Gradle test execution, instrumented audio test, or phone verification was completed in this session.
- UI still uses a short delayed read of preferences, not strict command-ID acknowledgments.
- Some Android routes may not support output DSP. Truly reliable UI feedback needs per-command sequencing, preferably a binder/controller response.
- An atomic command does not guarantee artifact-free DSP transition.
- Phase 6W6 should remain on this development branch until successful build and device QA.
