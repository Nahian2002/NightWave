# NW68 — Equalizer ownership and crossfade handoff reliability

**Parent:** `feature/nightwave-nw67-playback-transition-reliability`  
**Branch:** `feature/nightwave-nw68-audio-session-crossfade-safety`  
**Status:** Source changes committed and read back; **581/581 isolated Kotlin/JVM policy assertions PASS** using the exact GitHub blob (SHA-1 `ac91c00a45d33f83f9da645bbe8bfe859774460c`). The committed JUnit tests are **not yet executed with Gradle**. Android compilation, hardware audio effects and Samsung playback QA remain **unverified**. Public release: **NO-GO**.

## Source problems addressed

- `PlaybackService.applyEqualizer()` published a `sessionReady` capability snapshot **before checking `Equalizer.hasControl()`**. Another effect may own Android's audio engine, so Sound Studio could show band controls even when this session had no control. A previously active effect on the same session was also reused unconditionally, without confirming `enabled` and `hasControl` again.
- `ACTION_EQ_DISCOVER` published capabilities from an unowned probing effect. Device capability availability and actual effect ownership must not be confused.
- `finishCrossfade()` checked general offline state and incoming ID, but did not require the old and new decks to still be playing, that the current outgoing media matched the selected offline ID, or that the target was exactly the next timeline slot. A stale or paused handoff can substitute the wrong player for the MediaSession.

## Changes

1. **EqualizerSessionPolicy:** reusable effect must still own the exact active session and remain enabled. Release and attempt a bounded rebuild when Android revoked control. The service still fails safely if no effect is available; it does not claim that an output route necessarily applies Android audio effects.
2. **EqualizerCapabilities.publish:** only reports `sessionReady` after verifying a positive session ID, actual effect control, nonempty reasonable band count and ordered platform range. Invalid ownership clears a previous stale snapshot.
3. **ACTION_EQ_DISCOVER:** no longer advertises unowned capabilities and explains when another audio effect controls the device session.
4. **CrossfadeHandoffPolicy:** handoff only when both decks are playing; offline/gapless mode is active; Shuffle, Repeat One and sleep-timer pause do not conflict; target is the immediate next timeline slot; and current/next private IDs match the outgoing/incoming media IDs. On rejection, existing `cancelCrossfade()` returns to the original Media3 gapless playback.
5. The existing 0–10s crossfade setting, equalizer gain/band settings, hardware limits, audio normalization, Media3 timeline, queue/shuffle modes, lyrics, downloaded music, app/package identity and OAuth scope are unchanged.

## Verification status

- `AudioEngineOwnershipPolicyTest.kt` added with ownership/session, range, effect-enabled, stale media/queue index, stopped second deck, Shuffle, Repeat One and sleep-timer permutations. The exact committed pure Kotlin policy (verified with `git hash-object`) was compiled in isolation using `kotlinc` and passed **581/581** local logic assertions covering combinations of session control and handoff conditions. This does not run Android classes or hardware effects.
- Cumulative Windows source guard updated for the ownership checks and NW67–NW66 invariants; its presence is **not proof the guard or Gradle ran**.
- GitHub Android workflow run `40229270373` completed **failure** almost immediately with no returned step details. The reason could not be determined from the accessible workflow logs; **this is not evidence of an Android compiler error or a successful build**.
- **No completed Android/JVM Gradle run or Samsung hardware audio measurement is available** from this phase. Do not rate equalizer audibility or actual crossfade perfection as 10/10.

## Acceptance after an independently verified complete build

1. Play local audio, open Sound Studio and change supported presets and individual device-supported manual bands; confirm command acknowledgement and the actual reported band levels. Try headphones, Bluetooth and speaker; *some outputs may bypass Android FX*.
2. Switch audio output/route, disconnect a Bluetooth headset, and re-open Sound Studio. An effect with lost session control must no longer appear Ready/Active; a subsequent supported effect should be discoverable after starting audio.
3. Use a 3–5 s crossfade with two authorized local files, then pause in the overlap, skip to another song, enable Shuffle/Repeat One and change speed. No wrong-track handoff or second hidden deck should continue after cancellation.
4. Confirm crossfade OFF still uses normal eligible Media3 gapless playback and does not change existing queue/shuffle or offline files.
5. Verify the saved library and NW62–NW67 downloads/recovery, background/lock screen controls and Google OAuth diagnostics remain unchanged. Never uninstall or clear app data to bypass signing errors.

**Still blocked:** complete Android compilation, on-phone EQ/crossfade QA, Google OAuth error 8, offline rights/source eligibility, Play/privacy review. **Public release: NO-GO.**
