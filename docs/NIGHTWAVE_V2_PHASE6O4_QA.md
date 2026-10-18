# NightWave Phase 6O4 — Library Quick Actions & Navigation

Stacked draft on 6O3. Stable main and existing installed app data remain untouched.

## Implemented
- The Library root gains two compact user-initiated actions: New playlist (reuses legacy single-writer synchronous commit and naming validation) and Find music (navigates to existing Search, without starting a network request itself).
- Nonempty Liked Songs and saved playlist detail views gain Play list and Shuffle play. Both use the existing StudioPlaybackBridge and one Media3 PlaybackService; no new player, playback worker or queue persistence is added.
- Play list respects the selected collection's currently visible sorted/filtered order. Shuffle play randomizes that same view **once**, using a fresh monotonic click seed; it does not change the existing service shuffle mode, stored song order or user preferences.
- Pure LibraryQuickPlayPolicy only accepts exact canonical public YouTube watch URLs, deduplicates by URL and caps to 100 items, matching the PlaybackService queue limit. Long views show a notice that only up to 100 visible tracks are queued; all original saved tracks remain untouched.
- Both actions are explicit: no automatic streaming occurs during mere tab navigation, filtering, process start or viewing a playlist. Existing share/import, offline, favorites, playlist management, song options and saved order remain.
- 16 JVM test methods authored for queue capacity, saved-order preservation, seed-stable shuffle, dedup, invalid links/offline items and original playback identity. Phase source-check registered in Android workflow.

## Verification
Source-level inspection is not compilation. Android Gradle/JVM tests, actual UI on small devices, stream continuity and debug-signing compatibility remain **unverified** until the runner and later phone validation.

## Phone release test ideas
- Tap New playlist and validate duplicate name/blank rejection, data survives restart. Tap Find music and back navigation.
- In Liked Songs and playlists with 1, 100 and 300 songs, Play list and Shuffle play. Confirm the tapped selection resolves real video audio; previous/next and real Media3 queue contain at most 100 entries.
- Change filter and sort before quick play, ensure playback order follows that view and original saved order is retained. Confirm share/import still work and offline files never enter a public queue.
- Check large font and small width for two compact actions, screen rotation and Back navigation. Do not install a differently signed debug APK over the currently installed signed app.
