# NightWave Phase 6X1 — gapless playback for verified offline queues

## Base / integrations preserved
Built on `fix/nightwave-v2-phase6w10-instant-repeat`, which already incorporates the 6W8 equalizer options visibility fix, the 6W9 Sleep Timer dial and the 6W10 instant-repeat controls. No server, package-id change, app signing change, migration or library rewrite is involved.

## What changed
- A queue of **2–64 distinct locally imported tracks** is inspected through `OfflineAudioLibrary` and `fileFor(song)` so every prepared file is still an app-private, indexed, readable owned file.
- If all entries pass and a selected ID matches, the service passes the validated `MediaItem` list to the **existing single Media3/ExoPlayer** through `setMediaItems(...)` once.
- Media3 owns automatic transitions in that playlist, avoiding the earlier `STATE_ENDED → playOfflineId → setMediaItem → prepare` sequence between eligible tracks.
- `onMediaItemTransition` reconciles Media3 automatic/seek transitions back to the existing ID-only `OfflinePlaybackQueue` and `OfflineSessionStore` checkpoint. Pause/resume and Now Playing metadata continue using the existing MediaController.
- Explicit Next, Previous and Jump use timeline seeks instead of recreating an offline source when the gapless timeline is active.
- If the entire offline queue cannot be validated or has more than 64 entries, NightWave uses the old per-song fallback. Missing files are never silently dropped from the user's queue.
- Streaming still resolves each URL immediately before use. **No attempt to pre-cache signed stream URLs, no server, and no claim of gapless public playback**.
- Repeat/shuffle remain unchanged for local tracks: they are still unavailable in the present offline UI. Crossfade is NOT part of this phase.
- Encoder delay/padding and decoder/format compatibility determine actual audible gaplessness. The service cannot guarantee no silence embedded in a user's file.

## Source/test evidence
- `OfflineGaplessPolicy.kt` compiled using the available local `kotlinc` runtime and passed **10 pure Kotlin executable cases** for size bounds, unsafe IDs, selected index, auto next, manual backward/forward seeks and public URL rejection.
- Added `OfflineGaplessPolicyTest.kt` to the Android unit-test source set with six targeted JUnit methods (must still execute via Gradle).
- Service wiring has been reviewed for playback transitions, selected track checkpoint, private-file validation, explicit next/previous, and public-stream fallback.
- **No full Android SDK/Gradle `assembleDebug`, `testDebugUnitTest` or `compileDebugAndroidTestKotlin` run was available in this environment. No physical-device audition performed.**

## Mandatory release gate
1. In a fresh 6X1 checkout, run `build-windows.cmd` and require BUILD SUCCESS before installing any APK.
2. Import 3 locally owned, compatible MP3s with **no inherent leading/trailing silence**. Add all to one offline queue and start from the first.
3. Let a short track end naturally; verify the next starts without a pause or player spinner. Watch queue title, artwork and selected index.
4. Manually press Next, Previous and Jump to the third song. Confirm selection and resume bookmark reflect the current track.
5. Pause, background, lock screen, relaunch and manually resume. Confirm the last selected offline ID and position are restored without auto-play on service creation.
6. Pause at the end of the last song. Ensure there is **no automatic wraparound**, no public URL resolution and no ghost extra song.
7. Verify a queue of one still works. Verify queue of 65+ tracks and a missing private file use prior non-gapless behavior without deleting data.
8. Switch offline → public playback → offline. Confirm public speed returns to 1x and no old offline MediaItems remain.
9. Test Sleep Timer expiry across a local track boundary, Equalizer routing with Bluetooth, instant-repeat on public music, and ensure pre-existing library files, lyrics and Drive backup remain intact.
10. Verify no unsupported claims: this mode is not crossfade and cannot eliminate silence encoded in a track. Gapless timing should be measured using matching music files and the phone audio route.

## Known limits
- A local playlist is prepared on the service/main thread, up to 64 verified files; an unusually slow/private-storage device can introduce selection latency. Revisit with bounded background preflight if reported by real phones.
- If an indexed file is deleted *after* playlist validation, Media3 may error rather than silently skipping it. Existing retry/recovery rules still apply.
- No Android APK has been compiled or approved for installation in this phase.
