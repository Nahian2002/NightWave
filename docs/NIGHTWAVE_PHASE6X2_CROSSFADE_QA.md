# NightWave Phase 6X2 — Adjustable local crossfade

## Base / scope
The branch builds on `feature/nightwave-v2-phase6x1-offline-gapless`, which already carries the equalizer visibility, Sleep Timer dial, rapid repeat, and Media3 local gapless changes. This phase does **not** introduce a NightWave server, online audio preloading, signed URL caching, cloud storage changes, app ID changes, or migrations.

## Implemented
- **OFF by default**: saved locally in `CrossfadeSettingsStore`. The normal gapless Media3 playlist is unchanged when crossfade is Off.
- **Adjustable 1–10 seconds**: Now Playing includes a user-facing Crossfade dialog and duration slider from Off to 10 sec, with explicit Apply/Cancel.
- **True overlapping audio**, not a misleading fade-out/fade-in gap: shortly before a verified private offline track ends, a secondary ExoPlayer opens the next validated local MediaItem. The old track fades down while the next starts playing and fades up.
- The ramps use bounded, complementary linear gains: `old + new = ListeningLevelPolicy.gain` at each step, without automatic volume boosts. Two active decoders are present only during the overlap.
- **MediaSession remains singular**. Media3 `MediaSession.setPlayer` takes over the already-playing next deck on the main looper near the end of overlap; the old deck is released. Now Playing, notification controls, offline ID checkpoints, listening history, and equalizer ownership follow the new session player.
- No crossfade for public streams, single-track queues, queues that fail 6X1 gapless validation, last track, streams at non-1x speed, or tracks too short for the overlap. Such cases use the previous gapless/fallback path.
- Sleep Timer expiration, selection/Next/Previous, seeking outside the overlap window, audio errors, stopping playback, and service destruction all cancel/release the incoming deck and restore the original volume.
- Second deck uses local wake mode and leaves Android audio focus with the session-owned deck until promotion; session transfer fences synchronous callbacks.
- No high-frequency checks when crossfade is Off.

## Source/test results
- Pure `CrossfadePolicy.kt` compiled with local `kotlinc`. 13 executable cases passed (bounds, inputs, eligibility, progress and complementary output gain).
- `CrossfadePolicyTest.kt` adds four JUnit tests to the Gradle source set.
- `tools/verify_v2_phase6x2.py` adds source wiring, safety and earlier-feature preservation guards and is invoked by the existing Android CI workflow.
- 23 initial GitHub source consistency checks passed. Android Gradle / instrumentation / physical audio checks are **still pending**; source checks are not release certification.

## Mandatory device tests after a confirmed successful Windows Gradle build
1. Before installation, make a backup. Install only if the APK signing certificate matches the installed NightWave. Do **not** uninstall or clear app data.
2. In Now Playing, open Crossfade and confirm **Off** is initially selected. Check 1, 4, and 10 seconds on the slider; Cancel must not change the setting. Apply 4 seconds.
3. Import three 30–90 second compatible local MP3s; add them to one local playback queue. Let A play into B, then B into C; listen for both songs playing during approximately 4 seconds of overlap. Confirm correct title, selected queue row, current position and lock-screen controls after each handoff.
4. Switch Crossfade to Off. Play the same sequence: no overlapping audio; the Phase 6X1 gapless transition must remain.
5. While actively overlapping, press Pause, Previous, Next, and seek far backwards in separate runs. Expect immediate stop of the secondary audio, correct queue selection and no volume sticking at 0%.
6. Start a 1-minute Sleep Timer that expires near a transition: **all audio must stop**. Resume manually; the timer must not auto-play.
7. Play the last track and a single-song queue: no next song should unexpectedly begin, and there must be no dual audio.
8. Set offline speed to a non-1x value: Crossfade must not run. Return to 1x and verify normal mode resumes.
9. Try a queue with more than 64 tracks and a missing private file: old playback path remains available, and no automatic deletion of private audio, ID index or playlists occurs.
10. Switch between Bluetooth/earpiece/speaker and lock screen; watch for sudden loudness changes, playback interrupts, unexpected media-session disconnects and audio focus changes.
11. Test public streaming while Crossfade is set: **no crossfade**; signed URLs still resolved just in time, repeat should remain instantaneous, and EQ/volume controls should retain their prior behavior.
12. Relaunch app; selected crossfade seconds should persist, but the app must not automatically start playback.

## Known limits / caveats
- Overlap requires two audio decoders during the transition. On constrained devices, the secondary decoder may not be ready, in which case it is released and Media3 gapless plays instead.
- Mixed sample rates/codecs, embedded encoder silence, certain DSP/equalizer implementations, device audio focus and output routing may affect audible results. This is not a promise of mathematically sample-perfect mixing.
- The current phase deliberately excludes crossfade on public online tracks, as they use non-cached, just-in-time expiring stream URLs.
- Avoid treating this branch as an approved production APK until Gradle and actual device testing pass.
