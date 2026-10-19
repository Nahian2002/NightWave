# NW-02 — NightWave Sound Studio

**Branch:** `feature/nightwave-v2-nw02-sound-studio`  
**Base:** `52283da7bc292ecc7ed21934583d611f317f8b88` (NW-01 shuffle/EQ/Google recovery)  
**State:** Source changes committed; **fresh full Android compilation and device audio testing pending**.

## Goal

Replace the old Android system-style Equalizer modal with a polished, accessible in-app sound-control interface while fixing a real service bug: preset changes could be ignored if an EQ effect already existed on the same audio session.

This is not a streaming-catalog or licensed-music feature and does not require a NightWave backend.

## Implemented

- **You → Sound Studio:** Branded Compose modal instead of nested `AlertDialog.Builder`/system `SeekBar` dialogs.
- **Playback-dependent capabilities:** Discover bands, names, preset availability and dB limits from Android's active audio session. No fixed assumption of a 5-band equalizer.
- **Device presets:** Horizontal selection chips; supported preset count is capped and verified on the playback-service side. An Off action is always available.
- **Manual studio:** Individually labeled Hz/kHz bands with signed gain readouts, range-clamped Compose sliders, a flat reset and an explicit Apply action. The player never stores these as user audio-file edits.
- **Response curve:** Shows the actual device-reported gains after preset application, or the current unsaved manual draft. Explicitly **not** live-spectrum extraction or visualization of played PCM samples.
- **Service receipt:** Each command uses a unique request ID. The studio waits for the Media3-owned service status, distinguishing enabling a platform effect from an audible result on a particular route.
- **Audio-session failures:** Clear pending/unavailable messages for missing sessions and unsupported or externally controlled effects.
- **Fixed preset-change bug:** Preset selection on an already active session releases and reinitializes the Android effect, rather than returning early with the previous preset unchanged.
- **Restore options after Off:** Discover capabilities again when the effect was released; users needn't close/reopen the studio to see presets.
- **Safety:** Empty/invalid cached device ranges cannot crash draft initialization; manual slider disabled if no usable range; no broad permissions or account changes.
- **Regression tests:** Seven pure JVM tests for safe draft creation, updating ranges without mutating the caller, preset bounds and readable labels. Existing NW-01 checks remain.

## Known limitations

1. Android's `Equalizer` may report enabled while speaker/headphone/Bluetooth routing produces no audible effect. This is not a promise of lossless processing or effect support on all devices.
2. EQ preference persistence across service restarts is **not** claimed. No new saved audio-processing state was introduced.
3. Frequency-response curve renders levels reported by the Android API, **not** real-time measured output.
4. Crossfade, replay gain, search, queue and Google Drive are unchanged by this phase.
5. User-installed builds remain version `2.0.0-alpha5s` / code `35`; signing certificate must match before installing over personal music data.

## Deferred acceptance for when the user is ready

1. Obtain **BUILD SUCCESSFUL** in `build-windows.cmd`, with `:app:testDebugUnitTest`, `:app:assembleDebug` and `:app:compileDebugAndroidTestKotlin`; confirm the build-status file.
2. Launch the app on Samsung Android and open **You → Sound Studio** while an imported local song plays.
3. Confirm the modal displays supported frequency bands, presets and valid dB range. Select two different presets and check that both UI state and audible effect change if Android supports it.
4. Turn EQ Off; verify presets stay discoverable. Try manual bands with meaningful positive/negative gains and Apply, then Reset flat and reapply.
5. Compare on speaker vs headphones/Bluetooth; record status and failures without exposing private music.
6. Open Sound Studio with no song; the interface must show an honest unavailable state and must not crash. Start music, Refresh device and retry.
7. Repeat Google Drive connect and rapid public-video Shuffle tests from [NW-01](NIGHTWAVE_NW01_FIELD_FAILURES_QA.md) to detect regressions.
8. Confirm UI fits narrow portrait, scrolls to all bands, closes with Android Back and preserves existing playlist and lyric data.

**Verification distinction:** New tests and source checks were added, not claimed passed. GitHub CI on NW-01 previously failed at runner startup without executed steps. Successful Android compilation is still required to mark NW-02 tested.
