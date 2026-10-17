# NightWave Phase 6I — Compact Editorial UI

**Draft branch:** `feature/nightwave-v2-phase6i-compact-editorial-ui` stacked on the complete 6H2 compiler hotfix. This phase is restricted to Compose presentation and source regression checks. The stable `main`, package ID, APK signing, installed music files, playlists, backups, offline audio, Media3 service and private listening-history preferences remain unchanged.

## User-facing improvements

1. **Compact player chrome.** The mini-player keeps its close X, 48dp play/pause and 48dp dismiss tap regions, progress bar, album cover, long-title truncation, whole-card vertical swipe-down hide and swipe-up expand. The decorative handle shrinks from 16dp to 9dp, content row from 58dp to 54dp, cover from 44dp to 40dp. The bottom navigation row shrinks 62dp to 58dp and top branding header 60dp to 56dp. Insets remain from `WindowInsets.safeDrawing`; Scaffold continues to deduct its actual bottom-bar height rather than adding fake scroll padding.
2. **Six distinct moods, one shelf.** Home replaces three large two-column rows with a horizontal, scrollable row of six 178dp tiles. Each tile still launches the exact original named music search; none invents artist data or starts audio automatically. Visuals now have differentiated lightweight Canvas motifs for After hours (orbital night), Deep focus (measured lines), Desi pulse (beat waveform), Unplugged (strings), Momentum (rising bars), Slow mornings (sunrise lines). They use no extra network requests, bitmap resources, stock art or paid textures.
3. **Library overview density.** The two non-interactive count cards are replaced by one concise count line. The actual Offline music, Liked songs, named playlists, collection filters/sorting, track options, playlist management and offline resumption remain fully accessible.
4. **More navigable You.** Recent listening, optional local listening memory and playback health panels are hidden behind clear **View/Hide** rows by default, while the opt-in state is still visible as **On / Off**. Tracking never turns on automatically. A disconnected/errored player shows its health controls automatically even if the panel wasn't expanded. Settings, theme, reduced-motion toggle, library backups, Google Drive compare/merge, advanced storage, crash diagnostics and classic interface stay.
5. **Existing look, more music.** Retains AMOLED black, teal accents, real artwork, known recent music, existing Spotlight safety and named NightWave branding. Changes intentionally focus on content density rather than mimicking Spotify or claiming a licensed music catalog.

## Compatibility safeguards

- Does not modify `PlaybackService.kt`, Media3's queue, resolver, downloaded or imported audio, lyrics, library backup schema, Cloud Drive snapshots, permissions, versionCode or applicationId.
- `tools/verify_v2_phase6a2.py` is updated only to expect the smaller decorative swipe handle, retaining explicit assertions for correct close semantics, saved history, 48dp buttons, swipe gestures and bottom-scaffold safe padding.
- New `tools/verify_v2_phase6i.py` checks mode count/preservation, genuine onSearch actions, correct UI panel state, safe mini-player interaction, library accessibility and unchanged app identity. The Android workflow is staged to run the source contract and full Gradle/JUnit + Android test-source compile on PRs targeting the 6H2 branch when runners are available.

## Acceptance tests not yet performed

1. Run all inherited source gates, `:app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin` and resolve exact compiler/test failures. Previous 6H2 fixes were reported and runtime screenshots from the user show a NightWave app running, but **this Phase 6I branch is not yet built or device-verified**.
2. On a disposable emulator, test 320dp, 360dp, 412dp, landscape, tablets and accessibility font scales 1.0 to 1.8. Long music titles, 48dp playback controls, reduced motion and swipe gestures must remain usable without overlap.
3. Explore all six moods in Home by scrolling sideways; each must dispatch its original distinct query without starting audio, jumping tabs unexpectedly or losing vertical Home scroll.
4. Inspect the six mood visual motifs in AMOLED, dark and light themes. Their color contrast and text legibility matter more than automated source checks.
5. Hide/restore the mini-player using X and vertical swipe while audio is actually playing. Playback must continue unaffected. Check Search, You and Library scrolled to their final controls with the bottom bar visible.
6. Open/close Recent listening, Listening statistics, health panels and backup options. Confirm progress and privacy state survive page swipe/recomposition/rotation as appropriate. User must deliberately enable recording; erase and confirmation still work.
7. Open imported offline library, play audio, open liked tracks and a named playlist, sort/filter, edit playlists, retry playback and inspect Google Drive state; no data may disappear as a result of cosmetic UI changes.
8. Before installing any newer APK onto the existing phone, run the read-only `check-device-update.cmd` certificate/signing comparison. **Never uninstall or clear data because of a debug-signing mismatch.**

## Scope boundary

Spotify-level licensed music catalog, catalog artist/album pages, server-backed streaming, Spotify Connect, cross-device live sync and AI recommendations are **not** part of 6I. Phase 6J can separately explore accurate metadata and discovery from permitted sources after the 6I layout is verified. No claims of those external features are made.

**Verification status:** source changes committed and static checks staged; Android Gradle build, UI screenshot comparison, accessibility and on-device regressions remain pending.
