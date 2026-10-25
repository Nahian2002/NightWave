# NW44 — Smart Search 2.0: Relevance, Intent and Cleaner Discovery

**Base:** `feature/nightwave-nw43-queue-correctness`  
**Branch:** `feature/nightwave-nw44-smart-search-v2`  
**Status:** CODE COMMITTED. **Full Android Gradle compile, JUnit execution and physical-device results deferred** by user's request. This documentation does not claim a release-ready APK.

## Problem observed on the phone

Searching `Alone 2` surfaced unrelated `Home Alone 2` videos before the Alan Walker track, and the Search page presented two primary search controls, extra advanced/filter panels, verbose explanations and unspecific result ordering. NW43 corrected unwanted playback of entire search batches; NW44 improves which results appear **first** without reintroducing that queue problem.

## Changes

1. **`SearchMusicIntentPolicy`** — new deterministic pure Kotlin helper for ranking provider metadata. It treats `Pt. II` and `Part Two` as the number 2 when associated with `pt` or `part`; handles a few common contraction variations (`don't` / `dont`). It only normalizes for scoring, not when saving/displaying public metadata. Roman numeral `I` in ordinary sentences is deliberately not changed.
2. **Possible title credit split** — `Artist - Song` titles supply a scoring-only `Song` candidate. This is **not** a verified recording/performer identity or permission to download.
3. **Variant relevance** — unsolicited cover/remix, karaoke, slowed/sped-up, nightcore, live, 8D, instrumental, reactions, tutorials, clips and movie trailers can rank lower than matching main-song versions. If someone **explicitly searches** for a remix, cover or other version, that cue is not penalized.
4. **`SearchRelevancePolicy`** — hooks scoring hints into Songs mode and protects numeric token boundaries. Tie scores continue to preserve original provider order; all public results are retained, with no modified URL, title, uploader or thumbnail.
5. **`SearchResultPresentationPolicy.Groups`** — up to five high-confidence *likely metadata* matches grouped above other results; remaining videos stay accessible. If confidence is insufficient, show all under ordinary public results. Videos mode remains in original provider order and is never automatically marked as music.
6. **Discovery layout** — removes duplicated `More search options` panel and generic technical text; direct Songs/Videos chips near the search field trigger a fresh search for that mode; richer nonblocking loading state; shorter offline/error hints; `Best matches` and `More results` sections remain clear that uploader metadata is not verified.
7. **NW43 compatibility** — the two visible public-result groups both call one shared `playSinglePublic(song)` callback. Search still sends **one selected track**, never all provider results. The three saved/creator single-song tap paths and the distinct collection playback action are preserved.

## Regression tests and evidence

- `SearchMusicIntentPolicyTest.kt` contains **40 synthetic query-pair fixtures**, plus six JUnit tests addressing Roman numeral specificity, requested cover/remix versions, explicit movie-title searches, stable Videos-mode provider order, preservation of all results and empty/mismatched responses.
- `tools/nw44_kotlin_smoke.py` uses those **exact 40 committed fixture pairs** to compile five real, platform-independent production Kotlin files against synthetic `VideoPreview`/`SearchMode` stubs and run assertions **when `kotlinc` and Java are available**. It is wired into GitHub Actions conditionally; no app compilation is implied.
- `tools/verify_nw44_search.py` checks the ranking/section wiring and fails if any Search row accidentally re-adds all provider results to the queue. NW43's existing Python/Windows queue gates were updated to support both public visual groups.
- **Source audit:** 20/20 explicit NW44 wiring invariants passed; zero duplicate Kotlin imports across audited changed sources. An independently written JavaScript model replayed all 40 synthetic comparisons and found 40/40 intended pairwise rankings, but it **did not execute Kotlin/JUnit** and is not a provider precision study.
- **Unverified:** actual GitHub runner availability, Gradle, Android test-source compilation, runtime YouTube response quality, cross-language song-title accuracy, and real device layout.

## Limitations and acceptance criteria

No metadata heuristic can guarantee the right recording or remove every unrelated result. The ranking uses only already-returned public titles, creator labels and duration. Search Videos mode continues to prioritize the provider's original sequence. The existing streaming provider, DRM/source eligibility, playback engine, queue, library, offline storage, signing identity, serverless architecture and Google OAuth are unchanged.

For final testing, use the exact final branch and verify:
1. `build-windows.cmd`: actual `:app:testDebugUnitTest`, `:app:assembleDebug`, and `:app:compileDebugAndroidTestKotlin` must pass.
2. Search `Alone 2`, `Alone Pt II`, `we dont talk anymore`, `moonlight remix`, movie titles and diverse artist names. Review the top five results, version labels, and public-source availability.
3. Confirm Videos mode preserves provider order; choosing Songs/Videos updates results rather than showing stale-mode content. Test no results, network failure, loading, keyboard Search, recent history and local/offline matches.
4. Confirm a Search row still plays **one** song (NW43) while a named playlist intentionally starts its collection.
5. Inspect Search on 360dp and 412dp screens with large text, both NightWave themes and Reduced Motion. No clipping or unnecessary duplicate panels.
6. Validate user data/signature continuity before updating the installed APK. Do not uninstall or clear audio for testing.

## Next batch

**NW45 — Google account/Drive reliability**: resolve the real Google authorization error, distinguish status 8 from configuration errors and validate non-destructive backup/recovery end to end. This cannot be called complete without a configured Google Cloud project and a real connected-device test.
