# NW86 — Library, Playlists, Recently Played & You

**Roadmap:** [NW76–NW90 cumulative quality program](CURRENT_CUMULATIVE_ROADMAP.md) · Wave 3, NW86  
**Parent:** `feature/nightwave-nw85-now-playing-redesign`  
**Current branch:** `feature/nightwave-nw86-library-playlists-you`  
**Status:** Code committed, exact-hash Kotlin selection smoke passed and regression source tests added. Full current-branch Gradle/JVM/Android and Samsung acceptance deferred to NW90.

## Library usability and organization

1. **Precise playlist search:** Playlist-name filtering now uses the same NW82 normalized whole-word/long-prefix token policy as song Search. `Alo` no longer matches `Alone`, repeated queries such as `Alo Alo` require two real terms, and Bangla vowel marks keep creator/title spellings distinct. A playlist may still be found via a matching song when the existing contained-song option is enabled. Sorting/filtering does not write or reorder playlists.
2. **Visible selection and safe bulk edit:** Existing bulk playlist actions still use the single-writer atomic transaction with a hard **20-track maximum**. The UI can **Select visible** or **Deselect visible**, retain selections from other filtered views, display their count explicitly (including hidden selected entries), and **Clear** the set. Selection is submitted in underlying saved playlist order even if the view is sorted differently, guarding stale/filtered operations. Unknown rows are never allowed in the selected URL set. No bulk delete or copy can bypass `LibraryMutationPlan` expected-snapshot checks.
3. **Recently Played:** The collection now has **Newest first** (original source order) and **Oldest first** (temporary reversed view). **Clear listening history** requires a confirmation explicitly stating that Liked, playlists and downloaded audio remain. Leaving the collection dismisses a pending confirmation. Existing per-row remove action and ordinary playback remain unchanged.
4. **Folders, pins and recovery:** Existing folder create/rename/reassignment and playlist pinning continue to use metadata-only stores. `Show all playlists` now clears an active folder filter along with playlist text/sort/empty filters. Previously, an empty folder could stay selected after resetting filters and still hide every playlist.
5. **Compact You profile:** Google Drive's connected/disconnected status appears as a compact **Backup connection** row; existing account quick access, Profile editing, optional Drive consent, **Manage backups** (verified backup, restore preview, compare/merge), Sound Studio, Recently Played, listening memory preferences and privacy controls remain available. The duplicate explanatory text no longer expands the default You view unnecessarily.

## Focused tests

- `LibraryBulkSelectionPolicyTest.kt`: four new NW86 cases for selecting across filters, hidden-selection reporting, deselecting visible only, removing stale/unknown references and retaining the guarded 20-song limit.
- `LibraryBrowsePolicyTest.kt`: four NW86 cases covering short partial-word false positives, repeated terms, Bangla vowel differences and read-only oldest-first history.
- `tools/NW86LibrarySmoke.kt`: standalone pure Kotlin smoke for the exact production `LibraryBulkSelectionPolicy`, with a compile-only `VideoPreview` declaration under `tools` instead of the Android source tree.
- `tools/verify_nw86_library.py`: read-only source contract checking the existing single-writer cap, folder/pin metadata safety, history confirmation UI, retained account controls and test presence.

## Verified evidence (26 October 2026)

**Executed:** The exact GitHub text for production `LibraryBulkSelectionPolicy.kt` and `tools/NW86LibrarySmoke.kt` was reproduced locally. Their `git hash-object` values matched the connected repository's Git blob SHAs `a0752e193c30786ca16ee0ea1cb2a6cf047aa2ff` and `e8ac65673739e471a38c0c58b44358fa6a966841`, respectively. `kotlinc` compiled both, and `java` returned **NW86_LIBRARY_SELECTION_POLICY_SMOKE_PASS**. That isolated smoke covers selection only; it is not a full Android/JUnit build.

**Source audit:** GitHub readback confirmed the altered Library/You UI callbacks and policy values. The source-only contract and newly added JUnit files have **not** yet run as part of the full project. No live playlist data or installed APK was modified.

## NW90 acceptance

Confirm no data loss and no accidental destructive action across live Library and Room preview, 1/20/21/300-song playlists, pin/unpin/rename/move folders, view sort vs persisted order, filtered selected hidden entries with Copy/Remove/Undo, network-saved playlist URL identity, guest/Google connected profiles, Share and Restore, Bangla search, TalkBack for single-row delete, large text/landscape, and history clearing without affecting favorites or offline files. Full Gradle/JVM/instrumentation and Samsung QA are still mandatory before rating the release.

**Release remains NO-GO** until the NW77 public music-source rights issue, NW90 tests and the owner's final acceptance are resolved.
