# NW74 — UI-state consistency and bounded dialog callbacks

**Parent:** `feature/nightwave-nw73-backup-freshness-error8-guidance`  
**Cumulative branch:** `feature/nightwave-nw74-ui-state-crash-guards`  
**Status:** Source implemented. Exact committed pure Kotlin safety policy compiled and passed **7,509/7,509** standalone deterministic/seeded assertions, with matching Git blob SHA-1 `70c87759e48ad0608e0e92a49f1f5cd84aa75b33`. **Full Android Gradle/JUnit and Samsung end-to-end UI/crash testing remain UNVERIFIED. Public release: NO-GO.**

## Source-level risks found

1. **Library detail could outlive its collection.** A `rememberSaveable` selected playlist/Smart Library ID persisted after rotation and could refer to a deleted/renamed playlist or a smart collection that disappeared after the library changed. The UI would show a ghost detail with no real backing collection.
2. **Android native dialog result indexes were trusted.** Several lists (`Copy to playlist`, `Save to playlist`, playlist folder choice, folder management, and Drive snapshot selection) were indexed using `names[index]` or `options[index]` without bounds checking. A stale/recreated dialog or inconsistent selection could throw. These are defensive source risks, not a claim of reproducible phone crashes.
3. **Shared playlist import assumed a nonempty last row.** The success branch used `outcome.updated.playlists.last().first`, which throws on an unexpected empty snapshot and can report the wrong name if the new playlist is not the last row.
4. **Stale offline Search index:** Compose `produceState` retains the old value after a keyed `libraryRevision` change until its background job finishes. Search could briefly show files that were removed or superseded while a new index read was in flight.

## Implemented changes

- Added pure, Android-free `StudioUiStateSafetyPolicy` with `resolveCollection`, generic bounds-checked `itemAt`, and `createdPlaylistName` that safely identifies a new playlist by snapshot difference rather than assuming a last row.
- The main Compose shell resolves the selected Library item against current saved playlist names, active Smart collection IDs and built-in Liked/Recent collection IDs. An invalid saved selection is cleared via `LaunchedEffect`, and the detail screen immediately renders the Library overview instead of a ghost collection. A legitimate selection is preserved.
- The offline Search `produceState(libraryRevision)` explicitly clears the previous index before beginning the asynchronous IO reload. The stored offline files and underlying index are **not** changed.
- Native dialogs use `itemAt` or `indices` checks and display safe guidance if an option has disappeared. The Drive snapshot picker retains NW72's account-consent revision guard; invalid selection cannot start cloud work.
- A successful shared-playlist import reports the newly created collection name if provable, or a generic import success if the name can't be identified. **Existing playlist names, songs and URLs are not rewritten or deleted.**

## Verification actually completed

- `StudioUiStateSafetyPolicy.kt` from GitHub is byte-identical to the compiled local production Kotlin file (`git hash-object` = `70c87759e48ad0608e0e92a49f1f5cd84aa75b33`).
- `kotlinc-jvm 1.9.0` compiled the exact production policy together with an isolated test harness, which passed **7,509/7,509** checks: valid/invalid Library selection, dynamic Smart ID invalidation, removed playlist IDs, negative/oversized dialog indexes, empty/new import results, and 2,500 deterministic randomized combinations of selection/index/created-name logic.
- Added `StudioUiStateSafetyPolicyTest.kt` JUnit source and `tools/NW74UiStateSmoke.kt` reproducible CLI smoke. These **have not been executed by Android Gradle/JUnit**.
- Extended `tools/check-nw59-integration.ps1` with the NW74 UI/selection/source contracts, retaining earlier cumulative safeguards. Source-guard execution on Windows remains pending.

Reproduce from the project root with installed `kotlinc` and JDK:

```sh
kotlinc app/src/main/java/com/nightwave/player/v2/ui/StudioUiStateSafetyPolicy.kt tools/NW74UiStateSmoke.kt -include-runtime -d nw74-ui.jar
java -jar nw74-ui.jar
```

## Samsung acceptance after a successful compatible APK build

1. Save a new playlist, open it, rotate/recreate the Activity, then delete/rename that playlist from an allowed surface. Reopen Library: no ghost detail or crash; the overview appears safely.
2. Open a Smart collection and modify the underlying favorites/Recently Played so the collection disappears. The selected detail must return to Library overview, not show stale or invented songs.
3. Open Save to playlist, Copy to playlist, playlist-folder chooser or Drive snapshot picker, then change the underlying list through a supported secondary surface before choosing. Invalid/stale options must be rejected with a message rather than modifying an unrelated item.
4. Import a shared playlist when other playlists already exist. Confirm the reported name corresponds to the actually created playlist; no originals should change.
5. Delete a disposable offline song while Search is open and trigger a library refresh. The old local file index should disappear until the fresh background scan completes.
6. Recheck Home, Search, Library, Account, Now Playing, Up Next, lyrics and notification controls on a Samsung S24 Ultra. Confirm no overlapping UI states, crashes, lost playback or unintended navigation.
7. Run a **complete successful** `build-windows.cmd` / Gradle/JUnit process before installing; verify the signer using `check-device-update.cmd` and Google setup with `check-google-oauth.cmd`. **Do not uninstall/clear NightWave to resolve OAuth or signing issues.**

**Not claimed:** every Android crash fixed, all UI components 10/10, a successful Android compilation, Google OAuth error 8 resolved, complete offline catalog eligibility, or public-release readiness. **Release: NO-GO.**
