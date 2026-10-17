# NightWave Phase 6J — Music Identity & Local Discovery

**Draft development branch:** `feature/nightwave-v2-phase6j-music-identity-discovery`, stacked on user's checked 6I UI branch. This phase improves *display formatting and locally discoverable music*, not access to Spotify's licensed music catalog.

## Delivered

### 1. Conservative song presentation, never a metadata migration
- Added `net/MusicIdentity.kt` with `MusicIdentity.display(title, uploader)`. It hides **explicit, trailing** video-marketing tags such as `(Official Music Video)`, `[Official Video]`, `(Official Audio)`, `(Lyric Video)` and `[Lyrics]`, including up to two nested trailing labels.
- A clearly structured `Artist - Song` title only becomes the shorter **Song** display label if the independent uploader name corroborates the artist spelling after normalizing punctuation/spacing or an explicit `VEVO`/`- Topic` suffix. Otherwise the entire credited title and source uploader stay visible. **This does not verify a recording/album or prove an artist identity**: public video metadata may still be incorrect or misleading.
- Important distinctions such as `Live`, `Acoustic`, `Remix`, `Cover`, `Karaoke` and other versions are not stripped by broad heuristics.
- The display-only label is used in Home's last-played Spotlight, public track list rows, mini-player and expanded Now Playing. The `VideoPreview` model, title/author stored on disk, URL, selected queue identity, playlist/sharing actions, actual Media3 stream and cloud backup format **are not rewritten**.

### 2. Search finds human-readable local labels
- `SavedTrackSearch.match` matches both the raw source title/uploader and the derived display title/creator. For example, an EllieGouldingVEVO upload properly credited `Ellie Goulding - Love Me Like You Do` can now match `ellie goulding` as two separate words, even though the channel name doesn't contain a space.
- Source videos are returned **unchanged** and still deduplicated by their exact public URL, ordered and capped as before. Offline imported audio data is not exposed to providers.
- Search now explicitly clarifies that saving a public video in the NightWave library **does not download its audio** or make streaming work without an internet connection.

### 3. Artists & creators from your own music
- With known saved/recent/playlist videos, the Search empty state shows a horizontally scrollable **Artists & creators from your music** shelf.
- The identity index counts **distinct saved/recent public video URLs**, not listening sessions or complete artist discographies, filters generic placeholders such as `YouTube creator`, and offers at most eight creator chips.
- Selecting a creator chip **only fills the local Search query**, revealing matching tracks already in NightWave. It does not automatically send the name to YouTube/Invidious, play a song, create a playlist, read imported audio file paths, or enable listening-history tracking. The user can separately press **Find music on YouTube** to search publicly.
- No fake album art, discographies, release dates, verified artist badges, offline streaming rights, Spotify synchronization, or new server is introduced.

## Source and test changes
- New `app/src/main/java/com/nightwave/player/net/MusicIdentity.kt`.
- `net/SavedTrackSearch.kt` returns the same public videos but uses derived labels for local matching/ranking.
- `v2/ui/StudioComponents.kt` and `StudioPlayerScreens.kt` use display-only identity; `NightWaveStudioActivity.kt` adds local creator chips to Search.
- Ten authored JVM/JUnit test cases in `MusicIdentityTest.kt` cover source identity corroboration, conservative suffix removal, preserving meaningful versions, unknown uploader, URL deduplication, creator ranking, nonmutation of playback source, and matches despite VEVO channel spacing.
- `tools/verify_v2_phase6j.py` is a fail-closed source-only check for the display/persistence boundary, local-only chips, prior playback contracts and application identity. The stacked GitHub workflow now includes the gate and triggers on PRs targeting Phase 6I.
- A **local Kotlin/JVM isolated equivalent** of the display policy compiled and passed seven representative conditions. This was not a Gradle build of the GitHub repository or execution of the new JUnit suite.

## Android/device QA still required

1. Run `build-windows.cmd` from the complete 6J ZIP: `:app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin` must all pass; capture actionable build errors with `send-build-report.cmd`.
2. On the phone, compare original vs displayed track labels for Ellie Goulding, Charlie Puth, Ed Sheeran, video titles with no artist credit, and videos containing meaningful Live/Remix/Acoustic labels. Verify correct artwork and full metadata remain visible in options/share as appropriate.
3. Under Search with a known library, swipe **Artists & creators from your music**, tap each chip, verify only saved results appear without an unsolicited network call; then separately use the explicit online search action. With no saved/recent tracks, no invented creators should appear.
4. Confirm clicking any cleaned track, the Home Spotlight, next/previous, queue, repeat, favorites, playlist edits and backup/export uses the exact original source URL and preserves original saved title/uploader metadata.
5. Test the search matches after entering the original raw video title, original uploader spelling, and cleaned artist spelling. Verify no duplicates and that stale online results are not shown under a new local creator query.
6. Regress 6I compact Home and Library layouts at 320/360/412dp, tablet, large fonts, reduced motion, TalkBack, AMOLED/light theme, mini-player X/swipe, local imported offline audio and opted-in private listening-memory erase.
7. Before installing a newly built APK over the existing phone, run the read-only `check-device-update.cmd` comparison. Do not uninstall or clear data to bypass signer mismatch.

## Scope decision

Artist pages based on catalog IDs, trustworthy album grouping, licensed offline downloads, AI recommendation infrastructure, Spotify Connect and cross-device sync are **not implemented** because video titles/uploader labels alone cannot support them reliably. Next Phase 6K should focus on reachable audio/playback controls rather than claiming unsupported catalog features.

**Status:** GitHub source implementation and unit-test definitions committed. Full Android compilation, emulator and on-device tests **not yet verified on this 6J branch**. Keep as draft until actual acceptance.
