# NW107 — Reversible Synchronized Lyrics Follow and TalkBack Semantics

Date: 27 October 2026 · Parent `53695ea82319fdf9ccfa8090b42e2d29219d7ea1`
Branch: `feature/nightwave-nw107-lyrics-follow-accessibility`

## Confirmed user-facing accessibility defect

In `StudioPlayerScreens.kt`, immersive offline lyrics had a **Follow** button with `enabled = !lyricsAutoFollow`. When auto-follow was ON, the control was disabled; users could only leave following mode by physically swiping the lyric list. That meant TalkBack/switch-control users lacked an explicit accessible Pause Follow action. The label and spoken action also did not communicate current follow state.

## Narrow implementation

- Keep `lyricsAutoFollow` scoped to the selected track and reuse existing cue scroll logic.
- Replace the disabled Follow control with a reversible **Pause / Follow** button. Pause turns auto-follow OFF; Follow turns it ON and the existing `LaunchedEffect` scrolls to the current cue. There is no playback seek or file mutation.
- Set explicit accessible action and state descriptions: `Pause automatic lyric scrolling` / `Resume automatic lyric scrolling`, and `Automatic lyric follow on` / `off`.
- Reuse existing minimum 48dp touch target and `lyrics-follow-toggle` test tag. Keep button captions short so the immersive row remains usable on compact displays.
- Preserve direct lyric-list drag disabling auto-follow, reduced-motion scroll behavior, and the existing exact-song check before tap-to-seek. No online lyric extraction or catalog added.

## Verification

- `LyricsReadingPolicyTest.kt` extends the original four JUnit tests by four, retaining Bangladesh-language line narration and adding reversible action/state/hint contracts. The standalone `tools/nw107_lyrics_junit_gate.py` compiles the original Kotlin JUnit test file with the actual production `LyricsReadingPolicy.kt`, `LyricsSyncPolicy.kt` and `TimedLyrics.kt` plus a **temporary JUnit-compatible shim** from NW102. It does not alter the Gradle/JUnit source or APK.
- `tools/NW107LyricsFollowSmoke.kt` executes 17 deterministic pure Kotlin checks.
- `tools/verify_nw107_lyrics_follow.py` checks source/UI/CI wiring and retention of swipe behavior, exact user action and readable button semantics.
- CI adds both policy test routes and an explicitly **no Android approval** evidence receipt. The existing full Android unit/instrumentation gates remain mandatory.

## Limits / release status

The 8 Kotlin JUnit-source cases and the smoke tests are **not** Compose UI, TalkBack service, switch-access or physical Android acceptance tests. Screen-reader focus order, button speech, imported Bangla lyric rendering and application background playback require final on-device checks. GitHub hosted Android and minimal runner canary have failed pre-step in prior waves; no successful Android compile can be asserted without new evidence.

NW106's public queue JUnit six cases are still pending independent complete runtime verification, and the NW102 full 53-method regression suite has not been certified in a single run. Signing continuity, production OAuth/Drive backup, API 36 compliance and music-source rights review remain.

No package identity, signing keys, local files, library, playback source, saved LRC or authentication changed. **Public release NO-GO.**
