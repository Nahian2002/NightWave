# NW49 — Audio & Lyrics Studio

**Parent:** `feature/nightwave-nw48-downloads-offline2`  
**Branch:** `feature/nightwave-nw49-audio-lyrics-studio`  
**Status:** SOURCE IMPLEMENTED and inspected. Android/Gradle compilation, JVM execution, Media3 device behavior and audible EQ QA remain **UNVERIFIED**.

## 1. Genuine Sound Studio tuning

- NW49 adds conservative manual curves: **Flat**, **Warm bass**, **Clear vocals**, and **Airy detail**. Curves are computed from the *actual center frequencies reported by Android*; there is no fabricated fixed 5-band panel.
- Requests are generated in millibels, with targets bounded between -1.0 dB and +2.5 dB and clamped again to the hardware's advertised/manual-safe range. Devices exposing 1–32 bands can receive correctly shaped drafts. Stale/inconsistent capabilities are rejected.
- Presets modify an explicit UI **draft only**. They are not audible until the user presses **Apply manual EQ**, which calls the existing Media3-owning service `ACTION_EQ_BANDS_APPLY`. The existing status receipt/command-ID acknowledgement remains authoritative.
- The current UI displays **Not applied** when a draft differs from the reported device levels; manually changing a slider clears the chosen guided-curve chip. All existing device presets and explicit **Turn off**, capability refresh, error and active-session feedback remain.
- EQ depends on Android's real audio session and selected hardware/Bluetooth route. **Enabled does not guarantee audible EQ on all devices.** The default remains off.

## 2. Playback-session equalizer safety

- The legacy per-band command previously accepted only indices `0..4`, even when a device exposed a different number of bands. It now validates the index and gain against the *current* `EqualizerCapabilities` snapshot, exact audio session and supported manual range.
- Invalid/stale per-band commands leave the currently playing EQ intact and return an explicit rejection. The service now checks for negative band indices before applying manual settings and acknowledges legacy band operations when a caller supplies a command ID.
- This patch does **not** attempt to force audio effects onto unsupported output routes, and does not change the raw Android audio-processing API.

## 3. Gapless and crossfade clarity

- Existing verified app-private Media3 gapless queues and opt-in dual-deck crossfade are retained. The default crossfade remains off; no public streaming track is prebuffered for crossfade.
- The Now Playing sound panel now explains crossfade eligibility: Shuffle, Repeat One, non-1× speed and streaming are situations where crossfade is inactive/unsupported. A requested duration is explicitly described as **requested**, not already active.
- Crossfade depends on the next verified local recording being ready; the player falls back to its normal Media3 transition if prebuffering is impossible. This batch does not claim a new codec, true universal gapless, or guaranteed sample-accurate handoff.
- Existing volume normalization, listening level, sleep timer, offline queue ordering and speed controls are preserved.

## 4. More reliable synchronized lyrics

- Full-screen LRC tap-to-seek now uses `actions.seekToForTrack(cueOriginTrack, ...)` rather than the unfenced `actions.seekTo(...)` path. The original track identity is captured when rendering the cue. If the user rapidly changes tracks, a stale tap cannot seek the new song.
- Tapping a line re-enables automatic following. Touch-drag still intentionally pauses auto-follow; the Follow button restores it. Existing 50ms/250ms offsets, editing/import/replacement/removal, line progress and accessibility remain.
- **No automatic extraction of copyrighted or streaming-site lyrics** is implemented. Lyrics still require user-supplied synchronized offline LRC, which can be timed/edited locally. We do not claim word-level karaoke timing.

## 5. Tests and build gates

- `SoundStudioTuningPolicyTest`: four JVM test cases for real device frequency mapping, bounded gains, invalid/stale capabilities and draft-vs-applied state.
- `AudioTransitionUiPolicyTest`: four JVM cases ensuring streaming, off, shuffled/repeating/changed-speed and requested durations are described correctly.
- `tools/verify_nw49_audio_lyrics.py --self-test`: source contract gate and five deliberate broken-code fixtures; it checks the EQ backend, track-bound lyrics, existing crossfade/gapless and unchanged important user actions.
- Workflow `.github/workflows/android.yml` now invokes NW49 gates before the ordinary Gradle compile, JUnit and Android test-source checks. Earlier stacked branches' CI jobs terminated without any reported executed steps. A fresh, successful build is required to claim Kotlin compatibility.

## Deferred combined-APK acceptance (after NW52)

1. Compile stacked branch in Windows using the prescribed toolchain; require **green Kotlin compilation, unit tests and instrumented test-source compilation** before packaging final APK.
2. On S24 Ultra, play an imported track, open Sound Studio, refresh capabilities, select Warm bass, press Apply, and check real gain receipts. Test Clear vocals, Airy detail, Flat, Turn off, device preset, manual slider and unsupported-device feedback on speaker and Bluetooth. Do not assume every output route honors Android effects.
3. Skip/replace tracks while Sound Studio is open; stale session/band requests must be rejected without muting/stopping the player.
4. Play adjacent valid local audio in a prepared queue: verify default transition, opt-in 2–5s crossfade, repeat one, Shuffle, adjusted playback speed, seek-backward during fade, pause during fade and headset route changes. Verify no double-next, gaps beyond codec boundaries, overlapping audio left after exit or misleading labels.
5. Import a genuine LRC, open full-screen lyrics, tap cues to seek, drag to disable following and restore Follow, calibrate ±50ms/±250ms, then rapidly skip tracks during cue taps. No old cue should seek the new recording.
6. Re-test existing NW43 queue order, NW45 Google, NW48 downloads and ordinary background playback. Confirm persisted local music, database and permissions are unchanged. Never uninstall or clear the existing app data for a debug build.

**Next:** NW50 Premium Motion System; after NW52 the integrated build/device QA remains a hard release blocker.
