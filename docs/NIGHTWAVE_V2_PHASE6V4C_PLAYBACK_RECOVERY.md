# NightWave Phase 6V4C — Safe playback recovery and foreground reconnect

## Scope
Builds on Phase 6V4B offline file-health hardening and 6V4A identity-safe
queue persistence. Does **not** add server-side streaming, new download
permissions, invasive background loops, account access, or automatic replay
after a service/process restart. Does not mutate saved playlists, cloud
backups or offline audio during recovery.

## Failure modes addressed
1. A transient network-error callback may be posted shortly before a user
   pauses the player or a sleep timer expires. Delayed callbacks must not
   undo the explicit user pause.
2. A queued network retry may become stale after Next/Jump/queue change,
   switching to app-owned offline files or starting another resolution.
3. A provider must not replace the selected public video with metadata
   for a different video, even if the request-generation fence passes.
4. A failed Media3 controller future previously needed another foreground
   lifecycle entry or manual Reconnect. A fresh foreground attempt is now
   allowed once, with no reconnect storm.
5. Events from a disconnected/stale controller must not overwrite the active
   replacement controller's state or claim that music is playing.
6. An extractor timeout or overloaded resolver must leave the playback
   intent paused and an actionable error, rather than retaining a ghost
   play-when-ready state with no actual MediaItem.

## Code
- `playback/PlaybackRecoveryPolicy.kt`: pure scheduling, dispatch and
  returned-media identity predicates.
- `PlaybackService.onPlayerError()`: checks playback intent before scheduling
  any automatic refresh, then revalidates the exact selected/loaded public
  song, generation, previous retry marker, no pending lookup, offline mode,
  sleep timer and current playback intent on the main-thread callback.
- `resolveAndPlay()`: discards a success callback whose resolved video URL
  differs from the selected canonical public URL. Timeout and worker
  rejection stop play-when-ready and report explicit Retry to the user.
- `v2/playback/MediaReconnectPolicy.kt`: pure one-shot foreground reconnect
  eligibility gate, no request is made when a controller/future already exists.
- `StudioPlaybackBridge`: removes the obsolete event listener, discards late
  callbacks and schedules a single foreground reconnect on connection
  failure or disconnection. No automatic Play command is issued.

## Regression tests (added, not yet run through Gradle)
`PlaybackRecoveryPolicyTest`
- Allows exactly the normal transient error path.
- User pause before an error and after scheduling prevent auto-play.
- Sleep-timer expiry and offline mode block retries.
- Stale generation, changed selected/loaded URL, exhausted retry budget
  and concurrent extractor requests reject the delayed callback.
- A resolver returning a different public video or signed media link
  cannot be accepted for the selected track.

`MediaReconnectPolicyTest`
- One reconnect attempt is allowed while visible.
- Paused or released Activity, exhausted one-shot budget, existing
  controller and pending connection are all rejected.

## Verification status
- Changes are isolated to a fresh 6V4C feature branch.
- Source review only; **full Android/Gradle compilation and tests have NOT
  yet been run for this branch**.
- No APK is certified or installed by this phase.

## Integrated 6V4D release gate
1. Run all JVM tests and `compileDebugAndroidTestKotlin`, then package debug
   APK only if they pass. Do not bypass failed tests.
2. On an isolated Android emulator, simulate pause immediately after a
   transient stream error and confirm no automatic resumed playback.
3. Start a lookup, then switch songs/offline audio and reject stale results.
4. Simulate service disconnection and one foreground reconnect; verify no
   background retry storm, no stale status, and no automatic play.
5. After deliberate process termination, verify Home retains only
   explicit-resume state; paused music must not restart automatically.
6. Check lock-screen and headset controls, queue previous/next and
   sleep-timer cancellation behavior.
7. Confirm that 6V4A saved queues and 6V4B local file-health warnings remain
   intact, along with Google Drive transfer paths.
8. Verify APK signing/upgrade compatibility before personal device install.
