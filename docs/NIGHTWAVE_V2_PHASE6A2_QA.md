# NightWave Phase 6A2 — Mini-player dismissal and responsive layout

## Source delivered (draft)

This phase directly addresses the screenshots from the current NightWave Android build.

### A — Close or swipe down to hide Now Playing mini-player
- Added a dedicated **48dp X button** with accessible description **"Hide mini-player; music keeps playing"**. This invokes the existing `onHide` Compose callback only (`compactHidden = true`); it does not pause, stop, clear a queue, seek, close the service or restart a stream.
- Preserved the existing swipe-down hide and swipe-up expand gesture. Added a subtle centered grab-handle hint in the mini-player.
- Preserved the smaller **"Now playing · Tap to show"** restore pill when hidden, with the old `compactHidden = false` callback; the player remains available and Android's playback notification continues independently.
- On narrow screens, mini-player always retains the playback and X controls while omitting the secondary Next button below a **365dp available-width** threshold. **Next** remains in full Now Playing and the device notification/Media3 controls. On wider screens, the mini-player still displays Next.
- The row gives song artwork and title their own clickable expand area; tapping X or playback actions is not an expand-card action.

### B — Bottom-of-page safe padding
The app uses a Material3 `Scaffold` with a dynamic bottom bar. Scaffold already subtracts mini-player and bottom-nav height from the content viewport. Adding 150dp to every list would produce excess blank space; instead 6A2 applies final item **scroll breathing space only**:
- Home 36dp
- Search 34dp
- Library 40dp
- You/Profile 52dp

This protects the final interactive items without adding double insets or masking navigation.

### C — Home spotlight artwork/title collision
The supplied screenshot showed **Ed Sheeran's Shape of You** title extending into the cover. The old card placed a 65%-width title column and a fixed 145dp cover in one overlapping Box. The 6A2 implementation gives them separately measured **Row** positions, with a smaller 96dp image in very narrow cards and a 124dp image otherwise. Long titles use 2-line ellipsis rather than drawing over the actual artwork. The original Last Played song and **Play again** callback (including Phase 5Y safe resume) are unchanged.

### Privacy, stability and other features
No changes to audio sources, Media3 player ownership, queue state, Google Drive sync, library files, local schema, notification controls, Android package/version, permissions, sign-in or playlist contents. New Android Compose UI tests are authored for a disposable emulator and will be executed only when verification resumes.

## Deferred acceptance checks

1. Build from this exact branch with `build-windows.cmd`. Run the full JVM and Android test-source compilation gates and produce signing/identity diagnostics. **The code is not verified to compile yet.**
2. Play a track, press mini-player **X**: music keeps playing, elapsed time advances, Android notification remains. The restore pill appears; tapping it shows mini-player without seeking or restarting extraction.
3. Pause and press **X**: music stays paused (the UI does not accidentally resume). Unhide and press playback once; it should behave normally.
4. Swipe mini-player **down**: hide only. Swipe **up**: expand Now Playing. Check gesture conflicts at 320dp, 360dp, 412dp, large phone and tablet widths; test in both themes and with reduced motion.
5. On narrow screens confirm **X** and Play/Pause remain accessible, title truncates reasonably, and Next is available after expanding. On wider screens confirm Next remains visible in the mini-player.
6. Home spotlight test with a long video title and artwork: title never draws over the image at 320/360/412dp, including accessibility font scaling up to 1.8x. Verify clicking **Play again** uses the already implemented safe resume logic.
7. Scroll Home, Search, Library and You to their last item with mini-player showing, hidden and no track: no essential button clipped. Ensure no 150dp blank gap or permanently obscured final card.
8. Inspect Now Playing gestures, Up Next queue and Undo, local audio playback, favorites, playlists, personal mixes, native and official search, You/Google Drive controls. **Do not execute destructive cloud restores on the primary phone.**
9. Before installing a new APK, compare signing identity with `check-device-update.cmd`. Do not uninstall or clear user data to work around a signature conflict.

## Release state

GitHub source only, stacked draft branch on Phase 6A. Android Gradle/JUnit/instrumented tests, phone screenshots, signing and cloud OAuth remain unverified while testing is deferred. **Do not publish or merge to stable main.** Suggested subsequent 6A2B work: search empty-state hierarchy, richer Home mix details and responsive typography based on real phone screenshots.
