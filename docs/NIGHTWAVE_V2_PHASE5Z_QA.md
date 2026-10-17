# NightWave Phase 5Z — Safe Cross-Device Library Merge

## What is implemented (source-only draft)

Phase 5Z introduces **manual compare and non-destructive merge** from a chosen NightWave snapshot in the *same authorized Google account's* private Drive appDataFolder. This is an incremental foundation for multi-device support. **It is not yet automatic live synchronization, Spotify Connect, or cross-device playback handoff.**

After connecting Google Drive via the existing optional 5U controls, open **You > Library & backup > Compare & merge devices**. Choose one of the up to 20 most recent matching cloud snapshots. The app validates and reads the selected snapshot, calculates a local comparison, and shows exactly how many liked tracks and playlist copies would be added. No import happens until the user explicitly confirms **Merge into this device**.

Merge rules:
- Liked songs: append remote liked videos that are missing locally, retaining local order and metadata.
- New playlist name: add the remote playlist *as a separate intact collection*.
- Same playlist name and same ordered URL list: do nothing. Do not replace local metadata.
- Same playlist name but different songs/order: preserve original local list and import remote list as `Name (Drive)`, `Name (Drive 2)`, etc. Existing imported identical copies are recognized to prevent repeated imports.
- Local guest profile, theme, avatar, recent history, offline audio files, stream URLs, bookmarks and current playback queue are not changed by the merge.
- Fail closed rather than truncating at the existing strict archive limits: at most 500 liked songs, 50 playlists, 300 tracks per playlist, 2 MB encoded archive. No cloud snapshots are deleted or overwritten.
- A library changed since the comparison dialog was created is rejected by the **atomic local snapshot comparison**, so a concurrent local playlist edit is never overwritten by a stale merge. Existing manual restore-with-rollback is reused under the same in-process transaction lock.
- An account change aborts the merge flow. Cloud Drive authorization remains narrow (`drive.appdata`); no additional permissions or new remote write APIs.
- No automatic upload after applying a manual merge. Users can run **Back up now** when satisfied; their previously enabled daily backup schedule remains independently configured. Before using two devices regularly, note that 5U snapshots are append-only: the current UI lists the recent 20 and does not implement retention/pruning.

### Constraints

This is a **manual additive merge**, not a three-way sync engine. It intentionally never interprets one device's deletion as a command to delete music on another. Conflicting playlist changes become alternate copies. It does not merge listening history automatically or synchronize positions/live player ownership; those need separately designed version vectors, device identities, conflict handling and cancellation.

## Verification that must be performed later (not executed in this phase)

1. Run `tools/verify_v2_phase5z.py` and historical source gates.
2. Run `CloudLibraryMergerTest` JVM tests: empty set, identical libraries, favorites union, new playlists, same-name conflicts, re-import idempotence, second changed snapshot, remote invalid URLs, and archive size/capacity failures.
3. Run `build-windows.cmd`; require JVM tests, `:app:assembleDebug`, and `:app:compileDebugAndroidTestKotlin` to pass. Record compiler diagnostics, code/version, SHA and APK signing fingerprint before any install.
4. On two disposable Android profiles with the **same authorized Google account**: save different favorites and playlists, back up one phone, compare on the other, cancel, merge, and confirm both originals remain. Repeat merge of same snapshot: no duplicate (Drive) playlist should appear.
5. Modify a playlist while merge dialog is open. Confirmation **must reject** the stale comparison and require a new preview. Check profile/history/offline imports/playback queue remain unchanged.
6. Test near 500 liked/50 playlist/300-song limits: show the refusal without changing any local collection. Check invalid/oversized Drive archive is rejected before confirmation.
7. Test two different Google accounts, permission cancellation, network disconnect, malformed snapshot, and background worker timing without silent imports.
8. Verify 5Y personal mixes, same-song Spotlight resume, 5T search, queue controls, manual v1 backup/restore, rollback and 5U automatic backup remain intact.
9. Run `check-device-update.cmd` before installing over the current app. Never uninstall or clear app data to bypass a signature mismatch.

## Current status

Source committed on draft PR only. No Android build, Gradle/JUnit execution, cloud OAuth setup, real-device synchronization or recovery QA is claimed yet. Stable `main` remains unchanged.

Next optional stage: **Phase 5Z2** — real multi-device sync design with device IDs, per-collection revision/conflict records, opt-in periodic incoming checks and user-controlled playback-position handoff, after 5Z/5U passes production QA.
