# NW70 — Bangla-safe local search, ranked saved references and responsive typing

**Parent:** `feature/nightwave-nw69-queue-transition-consistency`  
**Cumulative branch:** `feature/nightwave-nw70-local-search-language-performance`  
**Status:** Source implementation committed. Exact `SearchScriptNormalization.kt` compiled under local Kotlin/JVM and passed **2,424/2,424 standalone assertions**, with the compiled source Git blob SHA-1 `079803f36ae11a51ff82f366d0598539780cfca7`. Full Android Gradle/JUnit compilation, Compose integration and Samsung UI/performance testing remain **UNVERIFIED**. **Public release: NO-GO.**

## Source defects and user-visible consequences

1. **Bangla characters lost significant vowel marks.** The shared saved/public `SearchRelevancePolicy.normalize()` stripped every Unicode `\p{M}` mark, not just Latin accents. Two different Bengali spellings could collapse to the same key, resulting in incorrect saved-track matching and stale online-search comparison. The new `SearchScriptNormalization` strips only combining marks after Latin letters, leaves Bengali marks intact, then recomposes text with NFC because NFKD decomposes Bengali `ো` into separate vowel marks.
2. **Local searches repeatedly scored saved tracks.** `SavedTrackSearch.match()` filtered on one full normalization and then rescored titles on another pass. It also deduplicated by URL **before** relevance sorting, so an earlier weak/placeholder title could hide a better later saved metadata record for the same original video.
3. **Typing performed library ranking on the Compose main thread.** `StudioSearch()` synchronously searched saved references and scanned/sorted up to 5,000 offline song titles each time query changed. NW70 moves saved-track ranking, offline matching and local suggestions into one `produceState` coroutine using `Dispatchers.Default`, with a 120 ms typing debounce. A new query or library snapshot cancels the previous calculation and initially clears stale suggestions. **Explicit online Search/Find music is not delayed.**
4. **Old saved creators were invisible.** `LocalSearchSuggestions` supplied up to 5,000 references to `MusicIdentity.creatorsFrom()`, but that helper silently processed only 350. An optional, bounded `scanLimit` keeps Home's original 350-reference budget and lets background local Search examine up to 5,000 references. Titles, URLs and creator attribution are still source metadata, not verified artist claims.

## Preserved boundaries

- Original `VideoPreview` titles, creator labels, URLs, saved music, offline private IDs, download records and user playlists are **not rewritten**. Ranked duplicates only change which *visible local saved reference* is shown when the same URL has multiple metadata records.
- Search does not import local files, download protected audio, add songs to playlists, change online provider order in Videos mode or grant new media licenses.
- Offline-library `OfflineTrackSearch` already preserved Bengali vowel signs; its ranking and file access are unchanged. Only its invocation from Studio Search moves off the UI thread.
- Online provider queries retain their existing coroutine/cancellation path. Offline suggestions remain device-only; no network calls or external backend were added.

## Verification performed

- The exact committed `SearchScriptNormalization.kt` passed local `kotlinc` compilation and **2,424/2,424** standalone deterministic and randomized Unicode assertions, including Bangla vowel preservation, Latin accent folding, mixed-script input, normalization idempotency and bounded output. `tools/NW70SearchSmoke.kt` reproduces these checks on any machine with Kotlin CLI. Its Git blob is `a7880c011b7c4c4f0d17ff4a94df622c36f4bb15`.
- A first executed smoke caught NFKD's decomposition of Bangla `ো`. The helper was corrected to return NFC-normalized search keys and retested to 2,424/2,424 PASS.
- Added/extended JUnit source regression coverage in `SearchRelevancePolicyTest.kt`, `SavedTrackSearchTest.kt`, `LocalSearchSuggestionsTest.kt` and `SearchResultPresentationPolicyTest.kt`. They protect distinct Bangla vowel spellings, best metadata for duplicate saved URLs, nonpositive result limits, creator matches past entry 350 and stale result visibility. **JUnit under Android Gradle has not been executed here.**
- The cumulative Windows `tools/check-nw59-integration.ps1` now requires these source contracts and fixes a stale pre-NW67 check of a removed decoder-recovery generation expression. **The PowerShell gate itself has not been executed here.**
- A standalone Unicode helper test does **not** establish app-wide compilation, actual Search UX speed, network provider reliability or availability of every requested song.

Run the isolated smoke from the repository root (JDK and `kotlinc` required):

```sh
kotlinc app/src/main/java/com/nightwave/player/net/SearchScriptNormalization.kt tools/NW70SearchSmoke.kt -include-runtime -d nw70-search.jar
java -jar nw70-search.jar
```

## Deferred device acceptance after a clean, signer-compatible Android build

1. Search saved **public** references for `ভালোবাসা`, `মালা`, `মেলা`, accented Latin names such as `Beyoncé`, mixed Bangla/English titles and punctuation-separated titles. Similar Bengali spellings must stay distinguishable.
2. Search the *offline* library with 500–5,000 local files. Rapidly enter and delete query text. The keyboard and UI should stay responsive; outdated suggestions must not appear for newly typed text.
3. Verify older saved creators beyond saved item #350 appear as local suggestions without creating or guessing new artist records.
4. Verify a saved public URL with two metadata variants appears once with the higher-relevance title, while playback always uses the original same URL.
5. Confirm selecting Songs/Videos maintains mode-specific public search behavior, official YouTube fallback and network error notices; no stale online results for an older Bangla spelling.
6. Verify NW62–NW69 downloaded music, playback, queue, EQ, crossfade, OAuth diagnostics and background services. Run `build-windows.cmd`, `check-google-oauth.cmd`, and `check-device-update.cmd` before installing over the existing Samsung app. **Never uninstall NightWave or clear app data to fix signing/OAuth.**

**Remaining blockers:** complete Android Gradle/JUnit build, physical-device UX/performance and download tests, Google OAuth error 8, source-rights/availability and privacy/Play compliance. **Not public-release ready.**
