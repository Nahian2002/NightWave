# NightWave Phase 6X4 — Offline Repeat & Shuffle

## Base
`feature/nightwave-v2-phase6x3-offline-volume-normalization`, including the prior Equalizer UI hotfix, Sleep Timer dial, instant streaming repeat, offline gapless, adjustable local crossfade, and opt-in non-boosting offline normalization.

## Implemented
- Repeat cycles **OFF → ALL → ONE → OFF** for imported/offline songs, immediately on tap, just as for public tracks.
- Shuffle toggles **On/Off** with an explicit visual label and updates immediately.
- The repeat/shuffle settings live in a private offline-only preferences store, separate from public stream queue persistence. No local music file, lyrics, cloud backup, playlist or signing metadata changes.
- Service accepts absolute repeat and shuffle commands, persists them, and applies Media3 playlist `shuffleModeEnabled` and `repeatMode`. Rapid presses never depend on delayed service toggles to determine the requested setting.
- Fully verified, prepared 6X1 Media3 offline playlists automatically follow shuffled order or Repeat All/One. Transition reconciliation stays ID-validated and saves the new selected local ID / position checkpoint.
- Fallback queues (e.g. >64 songs or an unavailable item) use bounded, non-repeating shuffle cycles, Repeat All and automatic Repeat One, plus explicit Previous history.
- Manual Next ignores automatic Repeat One; previous-track behavior retains the restart-after-3-seconds convention.
- Crossfade is deliberately suspended while shuffle or Repeat One is enabled, avoiding next-index mismatches and overlapping a song with itself. Standard local gapless/normal playback continues.
- When switching to public streaming, Media3 shuffle/repeat are cleared; the separate public queue and its repeat/shuffle controls stay authoritative.
- No NightWave server, network preload, private song upload, permission, application ID/version or database migration is introduced.

## Checks
- 23 isolated Kotlin behavior assertions **executed successfully**, compiling the offline queue/gapless policies with Kotlin 1.9.0 and a minimal PlaybackQueue.Repeat enum fixture. This is not a full Android build.
- One Kotlin compatibility error (`ArrayDeque.addLast`) was found, fixed to `add`, then compilation and all 23 isolated assertions passed.
- 8 JUnit queue-mode cases and 4 JUnit gapless/shuffle-transition cases added to the Android unit-test source set (execution through Gradle pending).
- `tools/verify_v2_phase6x4.py` is added and wired into CI on this new branch. Earlier source gates remain in the workflow.
- Full Gradle debug compile, JUnit Android project run, device crossfade and notification/lock-screen QA are **not yet confirmed**.

## Real-device test plan (deferred until user's testing session)
1. Build via `build-windows.cmd` and require BUILD SUCCESS. Only install over the existing application with a matching signing certificate—never uninstall or clear the library to resolve signer mismatches.
2. Play an offline queue of 3–5 imported music files. Check Shuffle On/Off changes immediately, works at natural end, and never repeats already-played songs before exhausting the first shuffled cycle.
3. Press Previous under 3 seconds after moving through shuffled tracks; it should follow real playback history. Above 3 seconds, Previous first restarts the current song.
4. Tap Repeat three times quickly while playing; expected: All → One → Off, with visual labels responding instantly.
5. Repeat One: finish a short imported track and confirm only that song loops. Press manual Next and confirm it advances rather than trapping the user.
6. Repeat All: play through last local song and verify proper wrap; repeat Off should stop after the final track with no ghost next-song playback.
7. Turn crossfade on (e.g. 4s). In Shuffle On or Repeat One, verify ordinary non-overlapping playback, no wrong tracks or double audio. Turn both off and verify 6X2 crossfade works again.
8. With normalization enabled, verify volume stays within Listening Level across shuffled/looped songs; no volume stuck at zero and no clipping spikes.
9. Test a 70-track queue or otherwise deliberately unsupported prepared timeline in an isolated copy. The old per-track fallback should shuffle/repeat and use Previous history.
10. Lock phone, use notification/headset Next and Previous, reopen Now Playing, resume after app interruption and reboot. Ensure offline preferences persist without auto-playing on app/service startup.
11. Switch to public streaming music and confirm old public shuffle/repeat state is not overwritten by offline settings; streaming still uses just-in-time resolution.
12. Confirm all imported songs, playlists, saved lyrics and Google Drive backups remain unchanged.

## Caveats
- The built-in Media3 shuffle order for prepared queues may differ from the fallback random order, but both are bounded to the selected local queue.
- Shuffle and crossfade are mutually restricted for this phase. Supporting both simultaneously would require mapping the actual Media3 next-item order into a dual-deck audio preloader.
- Offline repeat/shuffle are not available when no local track is selected; controls display per the current Now Playing screen.
- No APK is certified until the Android build and device tests pass.
