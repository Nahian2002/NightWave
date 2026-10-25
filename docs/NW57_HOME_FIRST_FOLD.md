# NW57 — Home first-fold hierarchy

**Parent:** `feature/nightwave-nw56-theme-contrast`  
**Branch:** `feature/nightwave-nw57-home-first-fold`  
**Status:** Source implemented; visual/device verification pending.

## Fix from latest NW52 recording
The Home screen displayed the last-played artwork in the premium greeting hero, repeated the same track in a 181dp **ListeningSpotlight** card, and offered a third **Continue** shortcut. The duplicated spotlight consumed the first screen and made the app feel repetitive.

- Keep personalized greeting and real listening artwork in `NightWaveHomeHero`.
- Keep the user's real **Continue** action and all quick shortcuts.
- Remove the duplicate 181dp ListeningSpotlight from Home; its only repeat/resume action remains available in Continue, Recently Played or the mini-player.
- Put the Recently Played heading and actual horizontal song shelf immediately after quick access. Move the liked/history statistics below real music.
- Empty history still shows its original honest `EmptyMusicCard` and **Discover music** destination. Keep private daily mixes, mood discovery, liked Library links and all playlists untouched.
- No new networking, artwork requests, saved data changes, continuous animation or automatic/random song queueing introduced.

## Tests
`tools/verify_nw57_home.py --self-test` protects actual playback, remove/options, real Search/Offline/Liked destinations and above-the-fold section order with three broken fixtures. CI runs it following NW53–NW56 gates.

## Pending QA
Compare the Home first fold against the previous NW52 video with a currently played track and an empty history. Test 320/360/390dp, large fonts and dynamic artwork; ensure there is no clipping and that the same track resumes correctly from every action. A source-level layout change does not establish pixel-perfect rendering or smoothness.

**Next:** NW58 integrated regression and device-validation batch, after which run the combined Windows diagnostic build again.
