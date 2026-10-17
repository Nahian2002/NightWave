# NightWave Phase 6O1 — Search & Discovery 3.0: local matching and music relevance

Stacked as a draft on 6N3. The stable main branch, application ID, signatures, favorites/playlists and real public-video URL identities are unchanged.

## What changed
- Saved local reference search supports diacritic/accent-insensitive matching, punctuation/word-separator tolerant queries and matching split terms across original title, uploader and conservative display-only labels.
- Ranking is deterministic and stable: exact song title, exact creator, title prefix, creator prefix, title phrase, creator phrase, partial terms. Duplicate saved URLs remain collapsed while the original VideoPreview object/URL is returned unchanged.
- Optional quick search in SONGS mode now shares normalization/scoring and demotes reaction, tutorial, review, commentary and podcast rows when the listener searches for a song. If a listener explicitly searches "reaction" that result is not penalized.
- Optional VIDEOS mode retains provider ordering. No video rows are filtered, no sources are fabricated and no new endpoints, providers, credentials or internet traffic is introduced by relevance ranking.
- Primary official YouTube search stays available as before. Saved music is always locally searchable even if the network search fails.

## Authored automated checks
- 16 pure JVM test methods for accented content, punctuation, uploader/artist matching, exact/prefix scores, stable ties, dedup, 24-result bounds, source URL preservation, negative matches, public music relevance and reaction-intent exceptions.
- Source-preservation gate at tools/verify_v2_phase6o1.py and stacked CI target.
- This does not prove Kotlin/Android compilation or device usability; GitHub Actions previously did not allocate a runner. Verify at release gate before installing onto a phone.

## Deferred device QA
- Type song/creator queries using punctuation and accents; verify the library section updates without a network request. Check keyboard Submit still opens the official YouTube browser, while quick results remain optional.
- Confirm public Videos vs Songs ordering differences are deliberate, search result tapping plays real canonical public video URLs, and liked/recent/playlist media stays unchanged.
- Verify text scaling, compact screen, swipe navigation, app lifecycle and network provider fallbacks.

## Not claimed
This is local relevance and public-result reordering, not verified artist/album identities, a licensed music catalog, generative recommendation intelligence, universal typo correction or Spotify-style semantic search.
