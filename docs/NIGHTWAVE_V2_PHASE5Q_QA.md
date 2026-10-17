# NightWave 2.0 · Phase 5Q — Search Recovery

**Version:** 2.0.0-alpha5q · **versionCode:** 33  
**Branch:** `feature/nightwave-v2-phase5q-search-recovery`  
**Status:** Draft source implementation, **not** a verified APK or guarantee of live public search availability.

## Why the phone displayed “Search needs a different route”

The native search first uses NewPipeExtractor to query public YouTube results, then the app tries several independent public Invidious community instances. Those endpoints can individually become blocked, move, stop serving valid JSON, rate-limit requests or simply return no playable public result. When all failed, the old UI showed a very large error surface and a duplicate YouTube search button, even if the song was already in the local saved library.

This phase fixes the degraded experience **without suggesting that any public search provider is guaranteed to work**.

## Implemented

1. **Device-saved results, independent of online search.** Search now checks liked songs, recent public-video tracks and up to 25 saved playlists, deduplicating by the saved public-video URL and sorting exact/prefix matches first. A bounded list of at most 24 matching tracks appears under **In your library** while public results load, succeed or fail. Local saved matches stay on the device; they are never sent to a search endpoint.
2. **Compact online recovery.** Instead of the large **Search needs a different route** card plus another browser CTA, failures show a small **Online search is unavailable** panel with **Retry** and **YouTube ↗** actions. If local matches exist, they remain playable immediately. An official YouTube search remains available when providers fail.
3. **Previous public results survive temporary retry failures.** If the same query is repeated, existing successful public results are retained and clearly labeled as earlier results if refreshing fails. Results and errors from an older query do not appear under a newly typed query. The new query can be submitted while a different one is loading.
4. **Faster provider failover after a hard extractor exception.** The ALL filter uses the same underlying upstream backend as VIDEOS. After a transport/extractor exception, NightWave now switches directly to an independent community provider instead of trying another near-identical request. This cannot repair a dead community endpoint or guarantee a particular result.
5. **Pasted public YouTube watch links.** A strictly normalized YouTube watch/shorts/live video URL goes directly to the existing Media3 playback route; clearly malformed/unsupported links are rejected before a provider request. No private/DRM video bypass or direct streaming URL reuse is added.
6. **Provider parsing tests.** New MockWebServer cases verify that malformed HTML challenge pages and empty JSON responses do not stop attempts at another independent endpoint.

## Validation

- Six pure JUnit tests were authored for device-saved matching: empty input, case-insensitive title/artist, all-term matching, ranking, duplicate URLs and bounded results.
- Two additional MockWebServer JUnit cases were authored for provider failover; the previous two remain.
- `tools/verify_v2_phase5q.py` is the source-only gate for saved matching, error UI, valid/invalid watch links, failover, previous results and release version.
- Kotlin/JVM isolated test compilation can check the SavedTrackSearch helper without the Android SDK, but **full Android Gradle, repository JUnit/MockWebServer, WebView/UI instrumentation, signed APK and phone testing have not run in this environment**.
- Online search depends on third-party availability and extraction compatibility; no provider uptime or public result availability is asserted.

## Windows and S24 Ultra QA

1. Extract a fresh source ZIP, run `build-windows.cmd`, and require a signed SUCCESS for **2.0.0-alpha5q / code 33**. If it fails, send `dist/NightWave-build-errors.txt` and the relevant compiler output. Do not install an old APK by mistake.
2. Run `check-device-update.cmd` on one authorized phone to check signing and version compatibility before an in-place install. **Never uninstall or clear saved music** to bypass a signing mismatch.
3. Search for a previously liked or recently played public song (e.g. a song saved in a playlist) while Wi-Fi/mobile data is disconnected. It should appear under **In your library** and can request playback through the existing service when streaming is available.
4. Search for an unfamiliar public song with internet. When a provider responds, verify results are relevant and playable through the existing public-media resolver. A result list is not proof that a track's audio stream is available.
5. Test airplane mode, provider outages, retrying the same query, entering a new query while the old one loads and clearing the input. Old public results must never be mislabeled as results for new text.
6. Tap **YouTube ↗** on failure and confirm the built-in official search opens. Test the alternate browser option if YouTube cannot load in WebView.
7. Paste a valid YouTube watch URL: it should use the player, not keyword search. Paste an unsupported/non-YouTube URL: it must not launch playback or a native provider request.
8. Check long Bengali/English search strings, Android Search keyboard, page swipes, portrait rotation, reduced motion, 1.3×/1.5× font scaling, offline imported music Library and currently playing music.
9. Confirm previous Up Next swipe/remove/Undo, background audio, mini-player and Saved Queue are unaffected.

**Release gate:** Keep the PR DRAFT. A verified build plus Android/phone playback and search behavior are required before merging to main.
