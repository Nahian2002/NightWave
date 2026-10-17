# NightWave 2.0 · Phase 5O — Swipe between pages

**Version:** 2.0.0-alpha5o · **Build:** 31  
**Branch:** `feature/nightwave-v2-phase5o-swipe-tab-pager`  
**Status:** Source implementation; not yet a compiled, verified Android APK.

## What changed

The old premium Home/Search/Library/You content used a Crossfade that changed pages only when the bottom bar was tapped. The new `HorizontalPager` provides native horizontal drag tracking and settling. A single `rememberPagerState` controls both pager and bottom navigation: no duplicate selected-page state is maintained.

- Swipe **left**: Home → Search → Library → You.
- Swipe **right**: You → Library → Search → Home.
- Bottom icons, premium Settings, Home search shortcuts, Library links and the system Back handler navigate through that same pager.
- Programmatic tab clicks use an animated pager transition, except when **Reduced Motion** is chosen; then the tab changes without programmatic animation.
- Pager position is saveable across Android configuration changes; each page has a stable key and adjacent pages are kept composed.
- Leaving a tab clears text focus and hides the soft keyboard so it doesn't obscure another screen.
- **Vertical scrolling** stays inside its LazyColumn. Profile's horizontal avatar-chip scroller remains a nested horizontal child.
- **Full-screen Now Playing**, artwork left/right next/previous and its downward dismissal are outside the pager. When Now Playing or the Up Next sheet is open, the background pager's swipe is disabled.
- The existing mini-player's swipe **up to expand** / **down to hide** continues to work independently. This phase does **not** yet add queue swipe-to-remove, which requires an Undo-backed queue mutation, not a UI-only gesture.
- Bottom tabs expose selected-state semantics and deterministic test tags for Android accessibility/automation.
- No changes to imported audio, favorites, queue storage, playback service, user profile, signing identity or app ID.

## QA added

`tools/verify_v2_phase5o.py` checks source wiring for pager navigation, selected-state tabs, overlay gesture isolation, reduced motion, existing storage protections and alpha5o/code31.

`NightWaveSwipeTabsInstrumentedTest.kt` contains two Android Compose UI tests:
1. Swipe Home → Search → Library → You, then one page backward. Assert the bottom selection follows the page each time.
2. Swipe vertically on Home, assert the selected tab stays Home; tap You, swipe back to Library, then tap Home. Verify bottom navigation and gestures never diverge.

These tests have **not** been executed here; Android Gradle, SDK, emulator and phone are unavailable in the authoring environment. Static source checks alone cannot prove smoothness, scrolling arbitration, animation quality or accessibility.

## Windows and physical device acceptance

1. Download and extract the Phase 5O ZIP to a fresh directory.
2. Run `build-windows.cmd` and require `dist/NightWave-build-status.txt` to show SUCCESS, code 31, `2.0.0-alpha5o`, valid signer and APK SHA256.
3. With one authorized Android phone, use `check-device-update.cmd` to check signing continuity and downgrade safety. Never uninstall or clear saved music to circumvent a failure.
4. On an emulator/disposable profile, execute `connectedDebugAndroidTest`; include the new two swipe tests and existing launcher/You tests.
5. Swipe slowly and quickly through all tabs. Verify **only one page** settles after each natural flick, selected icons match, and vertical Home/Search/Library/You scrolling still feels natural.
6. On Search, focus the keyboard then swipe to Library; the keyboard must close. Return to Search and confirm its text/query state is not wiped.
7. Tap premium Settings from each page; it should open You. Use Back to return Home, while dismissing an open queue or expanded player first.
8. While a song plays, swipe its cover artwork left/right to skip tracks, not change app tabs; swipe the expanded player down to dismiss; swipe mini-player up to expand/down to hide.
9. Scroll the horizontal avatar-style chips in You → Theme without accidentally changing pages.
10. Enable **Reduced Motion** and tap bottom tabs: programmatic transitions should jump immediately. Re-test text size 1.3× and 1.5×, both night/AMOLED and Pearl themes, and rotation.
11. Test offline session resume, playlists, background playback, lock-screen notification, Bluetooth controls and previously saved queue after reopening.

## Next steps

**5P:** independent safe queue gestures: swipe-to-remove only a future Up Next entry with **Undo**, never erase any original audio file or saved playlist. Improve mini-player affordances without changing its working swipe implementation.

**5Q:** investigate real public/native search failures seen on-device.

**5R:** refine motion/accessibility and require a real Android build/phone playback/crash acceptance run before release.

Leave this PR **DRAFT** until Android compilation, instrumentation, and phone verification succeed. Stable `main` remains untouched.
