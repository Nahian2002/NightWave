# NW82 — Search Precision & Alternate Recordings

**Official roadmap:** [NW76–NW90](CURRENT_CUMULATIVE_ROADMAP.md) · Wave 2, phase NW82  
**Cumulative parent:** `feature/nightwave-nw81-music-title-intelligence`  
**Current branch:** `feature/nightwave-nw82-search-precision`  
**Status:** Focused source work, regression fixtures, exact-source Kotlin smoke harness and legacy preflight updates committed. Android compile/runtime and real provider outcomes are **not** marked verified.

## Product behavior

1. **Accurate titles and creators:** Short terms (fewer than four characters) no longer match arbitrary prefixes or substrings of longer words. Thus **"Alo Alo" does not match "Alone"**, while an actual "Alo Alo" title keeps high exact-title priority. Exact word/title/artist evidence ranks above safe long-prefix evidence.
2. **Repeated tokens require repeated evidence:** A query containing the same word twice cannot be satisfied by a single saved word through doubled-up clean/raw metadata fields. The match engine tracks consumed word positions.
3. **Limited typo tolerance:** Public ranking and *last-resort* local saved-library recovery support **one ASCII Latin character edit or adjacent transposition** for query words at least five characters long. Strict saved matches win first. Do not fuzzily merge Bangla or other non-Latin vowel spelling; "মালা" and "মেলা" remain distinct. Typo candidates are heuristics, **not** evidence of verified music identity.
4. **More precise public ranking:** Exact numbers remain exact ("Alone 2" isn't an accidental prefix of "2024"), and strong song/artist boosts require complete token coverage. Existing single-song queue behavior, saved URLs and video metadata are unchanged.
5. **Remix/live/cover discovery:** Songs results optionally show **Other versions** when a version is an explicit suffix of the requested song title (for example "Moonlight Remix" or "Moonlight (Live)"). These entries keep their separate original URL and stay playable via the exact same one-song callback. **Requested variants remain primary**, and Videos mode preserves its original provider ordering. There is no provider identity merge, deduplication of distinct recordings, or invented artist attribution.
6. **Saved and offline matching:** Saved references search local metadata with exact/prefix priority, then optional English typo fallback only when there are no strict matches; Offline Library's existing title/extension search now observes short-term word boundaries; local creator suggestions also avoid false short-substring matches. Local/offline suggestions remain locally derived, with no network request while typing.

## Regression coverage

- New `SearchTokenMatchingPolicyTest`: word boundaries, Bangla marks, Latin edit/transposition limits, repeated-term word counts.
- Extended `SearchRelevancePolicyTest`: "Alo Alo" vs "Alone", title and version priorities, saved typo fallback, Bangla vowel stability, unchanged source URLs.
- Extended `OfflineSearchRankingTest`: offline "Alo" vs "Alone", exact "Alo Alo", distinct Bangla vowel signs.
- Extended `SearchResultPresentationPolicyTest`: alternate versions surfaced without losing individual entries; explicitly requested remixes remain primary; Videos mode untouched.
- Updated `SearchMusicIntentPolicyTest`: every source result must remain in one of Best, Other versions or More. All existing 40-query source-metadata ranking cases remain.
- Updated NW43/NW44 source preflights for **three** public result groups while preserving the single selected-song playback constraint.
- `tools/NW82SearchSmoke.kt` compiles as a pure JVM smoke with a test-only `VideoPreview` stub when run against the exact production Kotlin policies; no application sources are replaced. The exact compilation command is in its header.
- `tools/verify_nw82_search.py` audits source-only acceptance invariants. It is **not** a substitute for the Kotlin/Gradle tests.

## Scope and limitations

- NightWave has no privileged licensed metadata provider. This search only ranks and displays actual third-party source results and local references; it cannot promise perfect exact-song identification across all catalogs.
- This batch does **not** add a paid API, NightWave server, background music crawler, external credentials or new public media rights.
- Provider limits, search network timeouts and site availability remain outside this ranking patch; higher resource budgets do not eliminate unavailable or restricted source tracks.
- **Not yet run:** full Kotlin/JUnit/Android instrumented tests, current clean Windows Gradle build, signed APK, real query accuracy and Samsung playback. The owner has explicitly deferred the single comprehensive device validation to NW90.
- NW77's unresolved public-release audio-source rights review remains **NO-GO**.

## NW90 acceptance examples

Queries: "Alo Alo" vs "Alone", "Moonlight" vs "Moonlight Remix" and "Moonlight (Live)", "Midnight" vs "midnigth", "Alone 2" vs "Home Alone 2", Bangla "মালা" vs "মেলা", accent-folded "Beyoncé Halo"; search saved/unsaved/local/offline; exact vs requested cover/live remix; tap one result and confirm **one track in queue**. Verify URL/source identity is unmodified before and after title display.
