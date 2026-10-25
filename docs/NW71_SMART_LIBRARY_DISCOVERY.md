# NW71 — Smart Library fairness, Recently Played rediscovery, playlist song discovery

**Parent:** `feature/nightwave-nw70-local-search-language-performance`  
**Cumulative branch:** `feature/nightwave-nw71-smart-library-discovery`  
**Status:** Source committed and exact production `SmartDiscoveryPoolPolicy.kt` compiled using local Kotlin/JVM. **12,010/12,010 standalone assertions PASS**, with compiled-source Git blob SHA-1 `a547fd017ac922ac070a7d2d030425615262e3e1`. Full Android Gradle/JUnit compilation, complete Smart Library integration, and Samsung UI/runtime checks remain **UNVERIFIED**. Public release: **NO-GO**.

## Source issues resolved

1. **Large Favorites / Recently Played shelves starved saved playlists.** `SmartLibraryPolicy` previously flattened all supplied playlists and then took the first 600 records from favorites + recent + playlist tracks. Because favorites and recent were first, a sufficiently large library could hide playlist-only tracks. NW71's pure `SmartDiscoveryPoolPolicy` reserves at most 200 distinct favorites, 150 recent and 250 playlist tracks. The playlist scan is lazy and bounded by 60 playlists and up to 200 tracks each. Song IDs/URLs remain unique and no source collection is mutated.
2. **Rediscover your likes included recently heard songs.** Reversing all saved favorites did not guarantee rediscovery. It now selects only eligible liked URLs not present in the first 100 distinct eligible Recently Played references. If none are eligible, it omits the rediscovery shelf rather than inventing tracks.
3. **Smart Library admitted malformed public links.** Dynamic shelves now use the existing canonical `VideoLink.normalize(song.url) == song.url` rule. Invalid, non-YouTube, local/private or noncanonical URLs are excluded *from generated Smart Library views only*. The underlying saved metadata, playlists and original videos are neither rewritten nor deleted.
4. **Playlist search found names only.** `LibraryBrowsePolicy.playlists()` adds an opt-in `matchContainedSongs` flag, false by default to preserve existing callers. The Library UI now enables it, so searching a song title or saved creator can reveal a containing playlist even when the playlist name does not match. Each playlist checks at most 100 contained songs, and query terms must match the same song. The input hint now accurately reads **Search playlists or their songs**.
5. **Recently Played ordering remains authentic.** The generated `Recent rotation` keeps the user's source order, collapses duplicate canonical URLs and limits to 100 eligible entries. No invented play count or server history is used.

## Preservation and boundaries

- No playlists, public video references, app-private offline IDs, downloads, lyrics, profiles, or music files were modified or migrated.
- Daily Mix 1–3 and Personal Mix engines remain unchanged; NW71 improves the **dynamic Smart Library** collection pools and user-controlled playlist finding. No new streaming provider or AI recommendation backend was added.
- Titles and creator names are based on saved public metadata, **not verified music publisher/artist identity**.
- Noncanonical saved references are still present in their original collections; only Smart Library dynamic views exclude them for safer playback.
- No edit, delete, reorder, upload, or background cleanup is initiated by these read-only policies.

## Verification performed and pending

- The exact production `SmartDiscoveryPoolPolicy.kt` was copied to an isolated Kotlin/JVM test and its Git blob matched `a547fd017ac922ac070a7d2d030425615262e3e1`. It compiled successfully and passed **12,010/12,010** local deterministic and seeded-stress assertions across fair source quotas, stable deduplication, empty sets, preserving only source keys, max limits, and liked-but-not-recent rediscovery. The standalone test harness initially had a wrong input type for generic key sets, corrected before the passing run; this did not require a production policy change.
- Added `SmartDiscoveryPoolPolicyTest.kt` with pure JVM JUnit fixture cases and `tools/NW71SmartDiscoverySmoke.kt` for reproducible CLI testing. Added Android project unit-test cases to `SmartLibraryPolicyTest.kt` and `LibraryBrowsePolicyTest.kt` for genuine recently unplayed likes, playlist-only groups, invalid links, contained song/creator search and no cross-song term mixing. **These JUnit suites have NOT been run under Gradle.**
- Expanded `tools/check-nw59-integration.ps1` with the NW71 source contracts and retained NW53–NW70 checks; the PowerShell gate has **not been run** in this environment.
- No assertion here proves the full app compiles, the actual 100-track playlist scan is smooth on a Samsung, or dynamic collection behavior survives process death. Those require actual Android/device testing.

Reproduce the pure Kotlin smoke from a checkout:

```sh
kotlinc app/src/main/java/com/nightwave/player/v2/library/SmartDiscoveryPoolPolicy.kt tools/NW71SmartDiscoverySmoke.kt -include-runtime -d nw71-discovery.jar
java -jar nw71-discovery.jar
```

## Samsung integration checklist after a successful signed build

1. Populate a test profile with 300+ liked songs and 150+ Recent entries, plus playlist-only recordings. Confirm Smart Library shows eligible playlist-only creators and tracks rather than exhausting all 600 slots on Favorites.
2. Mark a liked song as recently played; it must disappear from **Rediscover your likes** while remaining saved in the original Likes/Recently Played collections. Unliked and recently heard tracks must not be presented as forgotten favorites.
3. Introduce a disposable invalid public reference. Confirm generated Smart Library views reject it without deleting its original saved record.
4. In Library, search for a contained song title, uploader or Bangla text when the playlist name is different. The correct playlist should appear, but the search itself must not reorder, add or delete songs. Search two words appearing only in different songs; that playlist should not falsely match.
5. Verify the original playlist-only-name search semantics for older callers, and that manual sort/pin/folder filters still work with the optional song match.
6. Recheck NW62–NW70 offline downloads, queues, OAuth diagnostics and search. Before upgrading an existing Samsung install, require `build-windows.cmd` SUCCESS, then `check-google-oauth.cmd` and `check-device-update.cmd`. Do **not** uninstall NightWave or clear stored audio to bypass signing/OAuth problems.

**Remaining blockers:** complete Android Gradle/JUnit/instrumentation tests, Samsung collection/search performance, Google OAuth error 8, source licensing/provider availability, release/Play policy. **Public APK/Play: NO-GO.**
