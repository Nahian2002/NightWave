# NW98 — Background Playback Intent and Widget Controls

**26 October 2026**. Source branch `feature/nightwave-nw98-background-playback-intent`; parent `d73ef0afc625cf51894dd057aba78b8b3b7202bf`.

## Reproduced source defects

- During a public audio lookup, the user may pause the pending Media3 Play intent before the resolver finishes. The service's same-queue fast path returned without reasserting Play when the same song was selected again via an explicit PLAY_QUEUE command. The subsequent resolved track could remain paused despite a new user Play request.
- The home-screen widget issued Play whenever `isPlaying=false`. On a loaded buffering item, `playWhenReady=true` but `isPlaying=false`, so tapping to cancel the pending Play instead reissued Play. The widget icon and action disagreed while buffering.
- A change in playWhenReady without changing isPlaying did not itself refresh the widget.

## Cumulative fixes

- Add a pure pending-explicit-play policy: only the same, still-resolving selected song and an explicit Play Queue command can resume a paused lookup. The existing extractor is reused. A queue-only replacement cannot revive playback, and stale/loaded state cannot invoke the pending fast path.
- Add `WidgetPlaybackIntentPolicy` to select Pause when loaded media has Play intent, Play when loaded media is paused, or no action without loaded media/controller command. The displayed widget play/pause affordance uses the identical policy.
- `PlaybackService` observes `onPlayWhenReadyChanged` to update widget state even when no audible-status transition occurs. Background listening metrics still count only actual playback; no change to audio focus or foreground service policy.
- Existing no-auto-resume-after-process-death behavior is retained.

## Targeted evidence and limitations

- 22 deterministic pure Kotlin smoke decisions cover buffering, paused, missing-media and disabled-command scenarios, matching icon behavior, same-queue reassertion, and stale extraction.
- Three new resolution JUnit tests + five new widget JUnit tests; Android Gradle execution not certified.
- New source-integration verifier and CI branch/runner checks.
- **Full Android compilation, real device widget controls, background kills, Bluetooth, 139-case acceptance and signed update remain NOT VERIFIED.** GitHub runners have been failing before execution.

No queue schema changes, offline audio deletions, API keys, Play signing changes, app package modifications or external service dependencies. **Release NO-GO** pending independent Android/device checks.
