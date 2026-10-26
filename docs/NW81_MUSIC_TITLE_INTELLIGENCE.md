# NW81 — Music Title Intelligence

**Wave 2 begins:** Original NightWave NW76–NW90 quality roadmap.  
**Parent:** `feature/nightwave-nw80-playback-engine-perfection`  
**Current cumulative source:** `feature/nightwave-nw81-music-title-intelligence`  
**Status:** Source implementation and regression test sources committed; final Android build/device quality acceptance scheduled for NW90.

## Music-first title presentation, not rewritten music metadata

NightWave receives **video titles and uploader/channel names**, not verified label-supplied song/artist identities. NW81 improves how existing entries **look** without replacing their `VideoPreview`, saved/favorited references, playlists, queues, underlying watch URLs, offline song IDs or cloud backups.

- Song cards, Home/recent tiles, mini-player, Now Playing and **Up Next** share the existing `MusicIdentity.display()` pathway. Up Next previously rendered raw queue titles, so it now uses the same display-only intelligence.
- Bounded, suffix-anchored cleanup removes only explicit `[Official Video]`, `[Official Music Video]`, `(Official Audio)`, lyric-video/lyrics, and official/unqualified visualizer/visualiser **presentation badges**. Repeated suffixes and dangling separators are handled without broader arbitrary word replacement.
- Labels that identify different recordings — **Live, Acoustic, Remix, Cover, Instrumental, Karaoke, Radio Edit, featured artists**, etc. — stay intact. An official-sounding phrase **in the middle** of a title is not stripped.
- An artist prefix `Artist - Track` is shown as the separate artist name only if the independent uploader name corroborates the prefix. The existing VEVO and `- Topic` matching also supports Unicode dash variants; no new artist name is guessed from a title when the uploader differs.
- **Bangla combining vowel signs remain part of the comparison key**. For example, `মালা` and `মেলা` are different creators and must not be merged by dropping vowel marks. This is display-identity hygiene, not a full Bangla language model.
- **Videos** search explicitly retains the provider's original caption using `MusicIdentity.videoDisplay()`; **Songs** search may use conservative music presentation. This choice is carried by the shared track tile while preserving identical tap, Like, queue, options and search result IDs.

## Regression coverage

- Extended `MusicIdentityTest.kt` with five NW81 test groups for multiple trailing video labels, necessary version identifiers, Bangla vowel differences, original Videos caption and unchanged source `VideoPreview`.
- `tools/NW81TitleSmoke.kt` contains a pure-Kotlin smoke that can compile with `MusicIdentity.kt` outside the Android runtime (the test-only `VideoPreview` stub is in `tools`, never in application source).
- `tools/verify_nw81_titles.py` contains a source-only contract check for the display/scope/UI routing, Unicode logic and tests.

## Evidence and limitations

- GitHub source readback confirmed: exactly **two** public search-result groups pass `rawVideoCaption = mode == SearchMode.VIDEOS`; the shared tile uses original captions in Videos and clean display in Songs; Up Next uses the same display; all five NW81 JUnit groups exist; the source-only checker and Kotlin smoke are present.
- All sampled changed Kotlin source files had balanced braces. **This is a narrow structural check, not a Kotlin compiler pass.**
- No Gradle/JUnit/Android instrumentation/real Samsung test has been run for NW81 in this session. The smoke is provided for execution from a full source checkout; do not mark it green before running it.
- This is not an LLM or licensed metadata catalog. Album, label, ownership and verified artist credentials **cannot** be inferred reliably from video channels; ambiguous captions deliberately remain unchanged.
- NW82 is responsible for **search ranking and typo precision**; NW83–85 are responsible for larger visual design changes. NW81 does not prematurely rewrite search, download or playback ranking logic.
- NW77's unresolved source-rights and public-release gates remain **NO-GO**.

## Final NW90 device checkpoints

Inspect Songs vs Videos for raw-caption preservation, same queue URL before/after title cleanup, Bangla artist/title pairs, genuine remix/live/cover distinctions, screen-reader labels, queue selections, favorites/playlists and cloud backup identity. Run all Kotlin/JUnit/Android tests and current build before any publication decision.
