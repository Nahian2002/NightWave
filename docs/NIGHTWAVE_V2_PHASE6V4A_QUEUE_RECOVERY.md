# NightWave Phase 6V4A — Identity-safe queue recovery

## Scope
Improve persistence and restart behavior of the **public YouTube queue** without
changing media playback, the app-owned offline library, Drive audio, OAuth,
playlist databases or existing user preferences.

## Changes
- Persist `selected_url` alongside the queue's selected index.
- Use the unfiltered saved row at the original index to restore the *same*
  song after bad rows or duplicate URLs have been discarded.
- Continue accepting legacy v1 snapshots with no `selected_url` field.
- If the selected song cannot be verified, restore an empty queue rather than
  silently switching to another item. Do **not** erase the raw old snapshot.
- Only canonical public YouTube video watch URLs are written into queue
  snapshots, playback bookmarks, lookup markers or failure labels; never
  retain signed audio links or untrusted HTTP URLs.
- Cap persisted title/artist/artwork fields, serialized queue length and
  restored snapshot size.
- Synchronously commit queue changes (not high-frequency playhead bookmarks)
  to reduce losses after abrupt process termination.
- Playback on app/service startup still requires an explicit user action.
  No network lookup or autoplay is performed by restoration.

## Automated regression coverage
**JVM tests** — `QueueRestorePolicyTest`
- Invalid row before selected song; remap to correct index.
- Duplicate row before selected song; retain exact selection.
- Legacy index-only queue compatibility.
- Mismatched persisted selected URL fails closed.
- Invalid or missing selection fails closed.
- Reject noncanonical links, including signed-link query parameters.

**Android instrumentation tests** — `QueueStateBookmarkInstrumentedTest`
- Restore the same selected track when a legacy snapshot has a corrupt row.
- Keep original snapshot bytes unchanged after a failed identity check.
- Save and restore explicit selection identity.
- Never write signed links in upcoming queue entries.
- An unsafe selected entry cannot overwrite the prior safe snapshot.
- Signed query parameters on watch URLs cannot leak into bookmarks or lookup/failure metadata.
- Existing bookmark/queue regression tests remain enabled.

## Verification status
- Source changes committed to a separate feature branch.
- Static source review only in the assistant environment.
- A complete Windows Gradle build, JVM tests, Android test compilation and
  real-device recovery validation are **not yet verified for this branch**.

## Windows validation at integrated milestone
1. Extract a fresh full branch ZIP to a short new path.
2. Run `build-windows.cmd`; require BUILD SUCCESS, green JVM tests and
   an APK from the current build. Do not install a stale previous APK.
3. Run `check-device-update.cmd` and verify signing compatibility.
4. Update NightWave in place without clearing the installed app's data.
5. Play a public track, save a queue with several items and background the
   app; close/relaunch, then verify the same track and next/previous order.
6. Test repeat/shuffle restoration, Bluetooth controls and screen-off
   listening after explicitly resuming playback.
7. Test the separate offline library and Google Drive music vault for
   unchanged playback and data safety.
8. If Gradle fails, use `send-build-report.cmd` and send only the redacted
   diagnostic.

## Remaining Phase 6V4 increments
- 6V4B: offline index/file reconciliation with no destructive automatic repair.
- 6V4C: bounded playback recovery and restart acceptance checks.
- 6V4D: integrated regression gate for local music, queue and Drive paths.
