# Phase 6W4 — experimental manual five-band equalizer

- Extends 6W3 with service-owned `ACTION_EQ_BAND_SET` and five in-app sliders, initially centered at neutral.
- Inputs limited to indices 0..4 and ±1500 millibels. Active Android Equalizer also validates against real band count and gain range before changing each gain.
- Manual mode represented internally as preset 21. Every update releases any prior effect and rebinds to the current Media3 audio session. No server, microphone, new permission, song-file modification or volume amplification beyond Android effect capabilities.
- Unsupported devices, audio-session changes and absent playback audio session must leave normal playback available.

## Important limitations
- Actual device preset names and band frequencies are not yet queried for UI. The generic numbered controls are NOT proof of exactly five available Android bands.
- Android equalizer gain range varies. Requested slider levels may not be available and can be silently rejected. This is an experimental interface, not a verified audiophile equalizer.
- Audio effect is not persisted; state resets after playback service recreation.
- Applying five bands uses five sequential explicit service commands rather than one atomic update; transitions between intermediate settings may be audible.
- No complete Android Gradle build, physical-device test, audio-route testing or release signature verification has been performed for this branch.

## Required build and regression
1. Run local build-windows.cmd and testDebugUnitTest; fix any Kotlin or Android API errors.
2. Exercise play/pause, next, repeat, public streams, offline files, sleep timer and Bluetooth switching while changing EQ.
3. Test on devices with fewer than five bands, narrower gain ranges, unsupported Equalizer implementation, and no active audio session.
4. Confirm no crashes, leaked audio effects, accidentally boosted audio, or unexpected queue changes.
5. Replace generic controls with actual band frequencies/ranges, capability acknowledgements and atomic command handling in the following hardening iteration.
