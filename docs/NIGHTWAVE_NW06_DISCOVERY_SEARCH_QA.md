# NightWave NW-06 — Search & Discovery Refinement

**Date:** 19 October 2026  
**Base:** NW-05 integration commit `ef9a0b3ee427affa3ee0892c711c91e533728f4e`  
**Branch:** `feature/nightwave-v2-nw06-discovery-search`  
**Status:** Development code committed. **No new passing Android build, network acceptance or phone test has been demonstrated.**

## Purpose

NightWave's pre-existing Search screen had two separate public search routes:
an official YouTube in-app browser, and optional quick results powered by
NewPipe/community provider APIs. The quick result action was buried under
More search options; switching Songs/Videos could display results from the
previous filter, and a provider returning no items could look identical to
a listener having no songs saved.

This update improves the actual search workflow **without assuming a licensed
Spotify music catalogue, creating a backend or silently uploading saved music.**

## Included implementation

### In-app-first search

- **Search NightWave** is now the main query action and Android keyboard Search
  submits to the existing optional public-video gateway.
- **YouTube** remains a clearly labeled adjacent choice, opening official
  search when community public-video providers are slow/unavailable.
- Direct public YouTube video URLs remain handled by the existing URL policy.
  No private/DRM/region-bypass behavior was added.
- More search options still exposes the **Songs** and **Videos** filters.
  Changing filter no longer presents stale results from the other mode.

### Saved-library suggestions

- Typing a nonblank query offers up to six locally derived title/creator chips
  when corresponding saved public-video references exist.
- Suggestions use only already-saved track metadata through
  `SavedTrackSearch` and `MusicIdentity`.
- Duplicate song titles are deduplicated in the suggestion display and
  frequency ranking comes only from distinct saved public URLs.
- Tap suggestions **only prefill the local search field**. They do not
  automatically submit anything to an external service.
- A saved creator is labeled **Creator** and is not represented as a verified
  artist/account page.

### Search-result correctness and failure messaging

- `SearchResultPresentationPolicy.matches()` requires both a normalized
  query and the exact Songs/Videos mode to match before any public result
  is shown.
- Explicit provider-empty results have an informative message instead of
  an apparent empty local library.
- Existing successful result cache, failure fallback, request generation,
  coroutine cancellation and recovery of earlier same-query results remain.
- No provider metadata is silently modified into an invented canonical song.

## Regression coverage added

- Six JVM tests for bounded deterministic local suggestions, duplicates,
  genuinely saved creator labels, and preserving original metadata.
- Four JVM tests for query/mode comparison, Unicode normalization and
  distinct empty-provider messaging.
- The `verify_nw06_discovery_search.py` source gate and Android CI branch
  trigger are included, preserving NW-01 through NW-05 checks and requiring
  `:app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin`.

**The tests are written, not confirmed as passing.** A source check does
not prove that NewPipe or any independent community provider is available.

## Known limitations

1. Community search reliability depends on unowned public network services.
   Some queries can still return an error, empty list or limited metadata.
2. Official YouTube search is a browser-based fallback, not a licensed
   Spotify-scale in-app catalogue.
3. Local suggestions cover saved **public-video references**, not every
   imported MP3 or unidentified file. They do not infer artist identities.
4. No online lyric retrieval, Google account identity or cross-device history
   synchronization is included in NW-06.
5. Private songs, sign-in tokens, local user metadata and history are not
   uploaded to search providers by the suggestion feature.

## Deferred combined phone acceptance

1. Build a clean NW-06 ZIP; require a full `BUILD SUCCESSFUL` and all
   automated tests, including any older regression gate.
2. Verify application ID, version and signing certificate compatibility;
   do not uninstall/erase the current NightWave app to force an update.
3. Save a small public playlist, then type portions of titles and creator
   names. Local suggestions must be relevant, labeled and responsive without
   internet. Tap a suggestion and confirm it only prefills text.
4. Search a common song name using **Search NightWave** and compare **Songs**
   versus **Videos**. Old mode's results must disappear on filter change.
5. Type a new query while the old search is running; results from the old
   query must not surface under the new query.
6. Simulate no internet and provider-empty cases. The screen should show
   an honest error/empty message and preserve saved-library results.
7. Tap the YouTube alternative and confirm it opens the official search
   without triggering an unrelated track or deleting saved music.
8. Regress NW-01 Shuffle, NW-02 Sound Studio, NW-03 account/Drive, NW-04
   seek/artwork layout and NW-05 queue/identity fixes before release.

**Next recommended milestone:** NW-07 Build & Release Gate — full compilation,
JVM suites, Android instrumentation smoke tests, data-preserving in-place
upgrade, app identity/signer checks, accessibility and real device crash review.
No release should be called stable until these are complete.
