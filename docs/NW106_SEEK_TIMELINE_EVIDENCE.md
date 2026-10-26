# NW106 — Now Playing Seek and Accessibility Reliability

Date: 27 October 2026 · Parent commit `c865b63680eb83498135ac08fa92ed7155dacc37`
Branch: `feature/nightwave-nw106-seek-timeline-reliability`

## Confirmed source discrepancy

`NightWaveSeekTimeline` draws its thumb with a clamped visual edge (`NightWaveSeekGesturePolicy.thumbX`), but mapped taps and drags with `touchFraction`, which used `(width - 2 * edge).coerceAtLeast(1)` even when its actual track had no usable horizontal space. A narrow split-screen or transient zero-width layout could therefore turn a tap or drag into an unintended seek to the beginning. The Compose accessibility `setProgress` path also always returned success, including a non-finite requested fraction that the player later rejected.

## Fix

- Add `touchFractionOrNull`, which returns no seek coordinate for invalid/non-finite or zero-usable-width geometry. Keep `touchFraction` as a presentation-only legacy fallback.
- Both tap and drag use nullable mapping. If a drag was never valid or invalidated while moving, its end calls `onCancel` instead of committing stale `dragged` state.
- `accessibilityFractionOrNull` accepts finite values with explicit [0,1] clamping. Accessibility `setProgress` returns false and does not commit when values are NaN or infinite; retains a single commit for valid values.
- Avoid an extra Seek command, playback resolution, event loop, or Media3 state store; no user media changed.

## Regression evidence and limits

- Five additional Kotlin JUnit policy tests, preserving four existing tests (9 total), cover narrow geometry, malformed input, accessible progress, visual inverse mapping and clamped valid touches.
- `tools/NW106SeekTimelineSmoke.kt` tests 24 deterministic pure Kotlin scenarios against the actual production gesture policy. Source wiring verifier checks end-to-end Compose call sites.
- CI checks are configured but GitHub runners have failed before step execution on prior branches. Compiling pure Kotlin policies **does not certify Compose, Android Media3, JVM test suite, or on-phone gesture behavior**.
- The standalone public queue 6-test gate and full Android build remain outstanding. NW105 reported 47 of 53 JUnit methods verified in independent runs, not a full 53-test run.
- Source rights, signer identity, Drive OAuth production authorization, API target and device acceptance are separate release gates.

No Android package, signing, database, music source, offline file, lyrics provider or account change. **Public release NO-GO.**
