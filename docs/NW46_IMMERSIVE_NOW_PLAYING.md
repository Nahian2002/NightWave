# NW46 — Immersive Now Playing (source implementation)

**Parent:** `feature/nightwave-nw45-google-drive-recovery`  
**Branch:** `feature/nightwave-nw46-immersive-now-playing`  
**Status:** SOURCE IMPLEMENTED. Source-contract review passed. Android build, JUnit execution, Android instrumentation and phone QA remain **UNVERIFIED**.

## User-facing changes

- Art-forward, responsive Now Playing canvas with deterministic local ambient light, calmer borders, real thumbnail fallback, restrained shadow and larger artwork on ordinary portrait phones.
- New pure `NightWaveNowPlayingLayoutPolicy`: bounded art sizes for normal phones, compact/short windows, landscape and tablets; protects enlarged text and invalid view measurements.
- Refined NightWave wordmark hierarchy and source-aware label, larger play/pause emphasis with 48dp surrounding controls, stronger song title typography.
- Clearer offline-library call to action, an Up Next/Queue entry point, and visible gesture hint only when skipping is possible.
- Lyrics moved above advanced audio controls so frequent actions surface before uncommon settings. All existing audio controls remain accessible below lyrics.
- Stable Compose tags for artwork, transport, offline action and queue, enabling later device UI tests.

## Preservation & behavior limits

Only the Now Playing Compose presentation and its standalone sizing policy were modified. **Do not infer improvements to stream search, download eligibility, offline file rights, Google OAuth, Media3 playback, audio DSP or lyrics availability from this batch.**

Preserved: safe track-bound seek `seekToForTrack`, swipe gesture rules, play/pause, next/previous, repeat/shuffle, liked state, save options, queue, local LRC editing/timing, crossfade restrictions, sound controls, sleep timer, player health and retry feedback.

Animation honors reduced-motion for artwork spring, transition and shadow emphasis. Actual artwork requests still use Coil's existing fallback without invented art or costly real-time blur.

## Verification & test matrix

- **Source guard review: PASS** (2026-10-25): playback callbacks and accessibility semantics kept, new tags present, safe sizing policy present, queue → lyrics → audio hierarchy correct.
- **Exact-file Kotlin policy smoke: PASS**: compiled `NightWaveNowPlayingLayoutPolicy.kt` locally with `kotlinc`; `git hash-object` matches GitHub blob `cb7332657e3287f82d63ad20423043966cea7f97`. Geometry smoke covers normal phone, compact/split screen, large text, tablet and invalid measurements. A floating-point edge in the split-screen assertion was fixed with a 1dp test tolerance. **This is not an Android UI compilation.**
- New JVM test `NightWaveNowPlayingLayoutPolicyTest` covers portrait 390dp, compact 320dp, 300dp split-screen, large text, wide tablet/landscape and NaN/infinite/negative geometry.
- `tools/verify_nw46_player.py --self-test` supplies four negative fixtures; workflow calls source self-test, regular source test, and existing Gradle gates.
- **No successful Android build confirmed.** GitHub Actions run 40196021059 for the NW46 workflow commit failed within seconds and exposed no executed job steps; no compiler or unit-test result can be inferred.
- **Not run:** Gradle compile, JUnit, Compose instrumentation, rotation/split-screen touch review, actual S24 Ultra device acceptance. Do not treat source success as release-ready.

## Deferred combined-APK checks

1. After all planned batches, run `build-windows.cmd`, verify a new local APK and pass the regular Gradle tests. Do not uninstall or clear data in order to upgrade.
2. Portrait/landscape/split-screen/large-text: art must fit, all controls scroll into reach, text truncates legibly, no clipped corners or overlapping mini-player.
3. Tap pause/play, next/previous, shuffle/repeat and like, then swipe art in both directions. Ensure manual `Play Next` from NW43 still outranks automatic queue suggestions.
4. Scrub while changing tracks quickly: stale seek must never apply to a different track.
5. Open Queue, Save for offline, Lyrics, Edit LRC, and Sound settings; close/return without losing playback.
6. Test with no thumbnail, no track, buffering/error, empty offline library, reduce-motion enabled and talk-back.

**Next:** NW47 Home, Library & You redesign. NW45 Google OAuth still requires successful on-device authorization and backup round-trip before public release.
