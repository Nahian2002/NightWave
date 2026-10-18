# NightWave Phase 6W5: device-aware audio controls

## Implemented in this source branch
- Android session-scoped probe publishes band count, center frequencies (Hz), min/max millibel gain and device-defined preset labels.
- You-tab controls are generated from capability values; no hard-coded 5-band or ±15 dB assumptions in the new manual dialog.
- One `ACTION_EQ_BANDS_APPLY` command transports the complete gain array to the service, which validates session, count, bounds and gain inputs before applying the profile.
- Audio-session changes rebind effects through the pre-existing Media3 listener, with fail-open playback behavior.
- Equalizer objects are explicitly released; probe resources are released unless they are already service-owned.
- Off remains the default. No NightWave server required.

## Source audit performed
Checked the latest branch files contain the new service actions, capability APIs, audio-session listener, UI dispatch and cleanup; fixed excess UI formatting and limited advertised presets to the 20 service-supported choices.

## Outstanding issues / not verified
- No Gradle/Android SDK compilation, unit-test run, device execution, battery measurement or Bluetooth route regression is claimed. Github accepting source is NOT proof that the APK builds.
- A complete band array is dispatched atomically at the command level, not a guaranteed glitch-free audio DSP transition. It is applied to one new effect before enabling.
- Status is a latest service receipt stored in local preferences; device effect-enabled status is not proof of audible processing. A future sequence-tagged response/broadcast is needed for strict command acknowledgement.
- Effects and capability metadata are session-specific. With no active audio session, controls should report unavailable until music is started.
- Legacy single-band command remains for compatibility; the new UI uses batch mode.
- Range-failure and unsupported route are fail-open; no user-data migration or persistent manual EQ profile has been added.

## Build/device test checklist
1. In a clean extracted project, run `build-windows.cmd` with Gradle assembleDebug/testDebugUnitTest.
2. Validate new Kotlin classes and Android audiofx APIs compile; inspect full Kotlin compiler messages on failure.
3. Play local MP3 and public tracks. Open You > Appearance & playback > Audio equalizer. Confirm actual labels/frequencies/range and Off.
4. Apply neutral settings, min/max settings, switch presets, repeat, seek, next/previous, sleep timer and pause/resume.
5. Switch speaker and Bluetooth during playback; verify session-owned effect cleanup and no volume jumps.
6. Test unsupported Equalizer device, empty/no audio session, fewer than five bands, atypical band counts and large fonts.
7. Confirm saved playlists, offline vault, Drive backups and APK signing remain untouched.
