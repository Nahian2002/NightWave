# NightWave 2.0 — Alpha 5E Discovery and Library Polish

**Version:** 2.0.0-alpha5e (code 21)  
**Branch:** `feature/nightwave-v2-phase5e-discovery-polish`  
**Status:** Draft source implementation. **No Android build or physical-device UI testing has run here.**

## New experience

- **Home:** Replaces the abstract discovery banner with an actionable spotlight. It uses the last *actual* public video from local listening history, including its real title, artist and artwork, or a clearly generic discover state when the history is empty. “Play again” selects the existing track using its genuine recent-list queue; it is **not** exact position restoration for that public track.
- **Discovery:** Six carefully named genres/moods arranged in compact 2-column tiles, from late-night music to Bangla/Hindi tracks. Each tile is just a public search query, **not** a guarantee or fabrication of recommendations, content availability, or playable streams.
- **Search:** Four one-tap shortcut chips issue the same native public Songs search used by manual queries. Search result list, YouTube website fallback, song options and existing failure behavior stay intact.
- **Library:** Likes and playlists are compact artwork-backed collection rows showing **real saved first-track thumbnails** and actual counts. Long playlist names truncate. Open a playlist/liked-songs collection to search within the current saved titles or artists; if the filter produces zero matches, Clear filter works. In the Library overview, the first five liked songs are shown as quick access; opening Liked Songs reveals the full collection.
- **Playback highlighting:** The currently playing public track receives a subtle themed background and label. Missing thumbnails show a consistent illustration rather than a broken/empty square.
- **Navigation:** More coherent rounded bottom bar with low-motion animated active state, while respecting Reduced Animations (zero-duration transition). Android Back closes queue/full player, then an opened playlist, then navigates back to Home before leaving NightWave.
- **Safety:** All new changes are in the premium preview UI, its presentation components, and the pure discovery-shortcut model. Public YouTube extraction, Media3 service, offline session restore, local storage namespaces, app identity, and classic launcher are unchanged.

## Validation accomplished

- The pure `DiscoveryShortcuts` implementation was compiled using Kotlin CLI in an isolated JVM smoke workspace; seven independent assertions passed (six entries, unique queries/keys, expected mappings and unknown-key handling). This is **not** Android compilation or execution of the repository's JUnit suite.
- Three JUnit cases authored in `DiscoveryShortcutsTest`; new `tools/verify_v2_phase5e.py` structural gate.
- Remote source inspection of modified Kotlin files found balanced braces, no duplicate imports, and the expected action-to-existing-search wiring. These checks cannot prove Kotlin/Compose compilation or visual layout.

## Windows and S24 Ultra acceptance

1. Download/extract the complete source ZIP for this branch to a new directory. Run `build-windows.cmd`; it must report **BUILD SUCCESS** and produce `dist/NightWave-debug.apk`. If it fails, supply `dist/NightWave-build-errors.txt` and relevant `dist/NightWave-build-full.log` lines. Do not uninstall or clear the installed app.
2. Optional source-only checks: `python tools/verify_v2_phase5e.py`. This cannot replace the Gradle build.
3. Install the debug APK **only if** the build succeeds and its signing key matches the existing install. Keep the old app if Android rejects the signature.
4. Test Home with empty history and with actual recently played songs. Confirm real artwork when available, sensible truncation of long titles, and that Play again plays the correct queue track.
5. Test all six Home shortcuts and at least two Search shortcut chips. Confirm no fake content is shown and the embedded YouTube browser fallback is still accessible when native search fails.
6. Open Library with no likes/playlists, then with long named playlists and many likes. Verify thumbnail fallback, correct counts, full collection access, artist/title filtering, and the Clear filter empty state.
7. Switch across all four tabs; press Android Back from a nested playlist, Search, full player, and Up Next. Verify Back navigates inside NightWave rather than unexpectedly exiting.
8. Test reduced-motion setting and four theme palettes. On S24 Ultra check portrait, gesture navigation, large font/accessibility scaling, screen insets, card cropping, thumbnail errors and UI scrolling.
9. Recheck online playback, background media notification and Bluetooth/lock-screen controls; repeat with imported offline music and explicit Resume. Nothing in this phase is intended to change those playback engines.
10. If Android warns about recurring crashes, use `capture-nightwave-crash.cmd` and share reviewed/redacted `FATAL EXCEPTION` or ANR evidence.

## Release gate

The PR stays **DRAFT**; stable `main` remains untouched. Do not claim a functional, fully tested APK until the Windows Android build and phone acceptance succeed. No automatic cloud sync, YouTube downloads or fake streaming recommendations were added.
