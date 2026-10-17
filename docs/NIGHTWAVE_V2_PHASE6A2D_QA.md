# NightWave Phase 6A2D — Compact personalized Home and useful Search start

**Branch:** `feature/nightwave-v2-phase6a2d-discovery-home-density` (stacked on Phase 6A2). This is a GitHub source change, **not a device-verified APK**. Android Gradle, instrumentation tests and screen captures remain deferred as requested.

## Home: personalized mixes with tighter information density

- Phase 5Y's on-device, deterministic, authentic favorites/recent/playlist mix engine is **unchanged**. Only real saved public tracks can appear. No fabricated music, third-party AI models, tracking, or new network calls.
- Home content vertical spacing is reduced from 16dp to 12dp; mix cards are 106dp for a single row item and 122dp when paired. A single mix fills the available row width instead of looking like a large, half-empty editorial billboard.
- Tapping a mix card still **previews** it without starting playback or replacing a queue.
- The expanded mix now has a deliberate **Play mix** button, starts the first authentic saved track only when pressed, previews only the first three songs by default, and offers **See all N tracks** / **Show fewer tracks** for larger collections (up to existing 12-track mix limit).
- Track tiles retain explicit play and options actions. Changing which mix is open collapses the prior "show all" choice. The reduced card backgrounds retain the NightWave palette rather than artificial album art.
- Mood art height is reduced slightly from 126dp to 118dp; six search shortcuts remain available. Song metadata, playlist contents and actual playback media are not modified.

## Search: valuable idle state and optional extra tools

- With a **blank query**, display four existing curated mood queries plus up to **three genuinely saved** liked/recent/playlist video tracks under **Already in your library**. Selecting one uses the existing saved-track playback route. Empty libraries show truthful guidance instead of fake recommendations.
- The **Find music on YouTube** button is shown only when the user has typed or selected a query, rather than occupying a disabled 49dp row in idle state. Keyboard search still triggers the same primary discovery callback.
- Native provider filters (Songs / Videos) and **Try quick in-app results** are now behind the accessible **More search options** disclosure. The old quick result/provider errors still become visible if a quick search is active or returned a result/failure. Core YouTube discovery remains primary.
- Saved matching still works locally as the user types, regardless of network. Stale native results are matched to the same query only. Clear search returns to the idle discovery and saved-music screen.
- Removed repetitive "No matches yet" and constant provider-disclaimer copy from the blank screen, replacing it with shorter query-specific guidance. This makes the Search tab less visually empty or dominated by optional-failure controls.

## Preserved safety boundaries

No change to `com.nightwave.player` application identity, alpha5s/build35, existing Media3 service, Phase 5Y safe resume logic, Phase 6A2 mini-player X/swipe-down/restore pill, imported offline audio, playlists, Google OAuth/backups/merge, user profile, Android permissions, or signing.

## QA deferred until requested

1. Run the source gate `tools/verify_v2_phase6a2d.py` together with inherited gates, compile full Gradle JVM and `:app:compileDebugAndroidTestKotlin` test sources, and assemble/debug APK on Windows.
2. On 320dp, 360dp, 412dp phone sizes and tablet widths, with regular/large fonts and AMOLED/light themes, verify single/paired mix cards, headings, Play mix CTA, optional track disclosure and all six mood tiles. Inspect layout for text clipped by icon/edges or below mini-player.
3. With a library containing 0, 1, 2, 4 and 12 eligible songs, check that cards never imply fake catalog suggestions, repeat songs, or misreport track count; opening a card does not interrupt audio.
4. Press **Play mix** on an expanded card. Confirm a single deliberate queue play starts the expected first track; no duplicate extraction due to recomposition. Test the per-track options.
5. With blank Search, verify four shortcut chips and up to three real saved tracks. Test no-saved-track guidance, select a saved track without a network request, then type a query.
6. Test Search with offline connectivity, provider errors, rapid query changes, keyboard submission, and a valid/malformed pasted YouTube link. Extra quick options must remain optional and its errors must not prevent the primary search.
7. Regress Phase 6A2 X/swipe-to-hide mini-player (music must keep playing), Home spotlight title/cover collision, Up Next swipes, offline imports, profile cloud controls, playlist changes, and saved/last-played history.
8. Compare APK signing identity with the currently installed app using `check-device-update.cmd`. Never clear existing user data or uninstall the current app just to update a debug build.

**Status:** Source committed and UI-test cases authored; no real device screenshot inspection, Android build, instrumented execution or Google synchronization check performed. Keep this phase in draft until verified.
