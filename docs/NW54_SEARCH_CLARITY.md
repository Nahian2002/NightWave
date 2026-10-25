# NW54 — Search clarity and consumer UX

**Parent:** `feature/nightwave-nw53-google-recovery-ux`  
**Branch:** `feature/nightwave-nw54-search-clarity`  
**Status:** Source changed; Kotlin/Compose compilation and on-device Search validation remain pending.

## Changes
- Online results heading now says **Songs & music** / **Videos** and an accurate online result count without surfacing the upstream community search-provider domain in the main UI.
- **Best matches** uses **Closest to your search** and **More results** uses **More recordings, covers & remixes**. This improves readability without falsely promising verified artist profiles, search ranking certification or rights to source audio.
- Saved creators remain explicitly based on saved metadata (names can differ from official artist pages). No new Google/Spotify source is implied.
- Loading text tells the truth when real local matches are available; network failures now present a simpler recovery with the existing Retry / YouTube fallback buttons.
- Kept `SearchResultPresentationPolicy.matches`, dedicated single-result playback queues, Songs/Videos mode, local search, history management, offline search, and on-device search text unchanged.

## Tests
- Three pure JVM cases in `SearchConsumerCopyPolicyTest` cover count grammar, nontechnical provider copy, accurate local match descriptions and creator attribution.
- `tools/verify_nw54_search_copy.py --self-test` contains four broken-code fixtures for disclosure, missing results, wrong player queue and absent tests.
- Android workflow wired to verify new Search gate and all previous NW53 guards before Kotlin/JUnit test runs.

## Pending phone acceptance
Open Search and test Bangla/English song, video, saved and nonsense queries, provider failure, local zero matches and a slow network. Compare card density and bottom navigation against the NW52 recording. A source copy change alone does not improve network speed or establish no-result accuracy.

**Next:** NW55 — unify Offline and Download screens under NightWave's dark/light visual language.
