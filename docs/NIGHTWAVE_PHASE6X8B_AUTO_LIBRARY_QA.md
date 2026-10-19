# NightWave Phase 6X8B — Android Auto browse & Android build stabilization

## Base / safety
Branch based directly on `feature/nightwave-v2-phase6x8-android-widget-device-controls`. All prior NightWave music/playlist/backup/lyrics/playback code stays in place, with no version-code, signing, database, private-audio index, backup schema or cloud-API changes.

## Implementation
- Migrates the *same* `PlaybackService` from `MediaSessionService` to `MediaLibraryService`, retaining its existing ExoPlayer (including 6X2 crossfade handoff), Media3 notification/player session, playback recovery, Sleep Timer, widget custom commands, Bluetooth noisy-route handling and data protections. Does not register a second media service.
- Advertises `androidx.media3.session.MediaLibraryService` and `android.media.browse.MediaBrowserService` on the same manifest service, plus Google's `automotive_app_desc.xml` media descriptor. Source: Android Media3 `Serve content with a MediaLibraryService` documentation.
- Exposes a root with **Imported music**, **Liked songs**, **Recently played**, and **My playlists**. Existing playlist names and song metadata are browsed from the current local indexes; no network request is made to browse. Media3 items correctly distinguish browse folders from playable entries.
- Car search is strictly local over bounded imported/saved metadata. Pagination is zero-indexed, maximum 100 entries per page. Offline-only browse/search requests do not offer streaming tracks.
- Car media IDs are bounded encoded references. Selection requires exact current membership in indexed private audio (validated with `fileFor`) or a current MusicLibrary collection (validated canonical video watch URLs). Unknown/URI-only media requests produce no items and **do not auto-play**.
- Play selection is dispatched back through NightWave's existing `playOfflineId` or public `resolveAndPlay` and queue state store. Public signed stream links are never represented as playable direct URLs in the browser catalog.
- Keeps the 6X8 widget's session connection; MediaLibrarySession is also a MediaSession. The widget's custom commands and prior headset media button handlers remain.
- Legacy `tools/verify_source.py` and `tools/verify_v2_phase6x8.py` gates are updated to accept the single-service Media3 migration rather than assume the retired manifest action/session builder.

## Verification evidence
- Local isolated `kotlinc` policy smoke compilation completed, with **20/20 executed assertions** for encoded playlist IDs, public catalog IDs, offline path restrictions, invalid input, UTF-8 and zero-based paging. This compiles a matching copy of the authored pure Kotlin policy; it is NOT full Android project compilation.
- New `AutoBrowsePolicyTest.kt`: nine JUnit4 methods, to be run by `:app:testDebugUnitTest`.
- New `NightWaveAutoBrowserIntegrationTest.kt`: two AndroidJUnit4 methods, for service type and connected MediaBrowser browsing. The browser test uses the application thread for all MediaBrowser API calls and compares playback state before/after browsing.
- New static gate `tools/verify_v2_phase6x8b.py`: parses actual manifest/automotive XML and checks existing widget and service/source invariants. Included in Android CI with historic checks.
- Draft diagnostic PR #79 triggered actual GitHub Actions Android verification. Two jobs reported **failure**, with no returned step metadata, artifact or downloadable log (job log retrieval returned BlobNotFound). These do NOT establish a Kotlin compiler error nor successful compilation. A known user-side `build-windows.cmd` or restored CI log is still required before APK installation.
- No successful complete Android SDK Gradle compile, physical Android Auto emulator playback, Samsung phone browsing, Bluetooth test, or Android UI screenshot has been observed. Do not call this a release-ready APK.

## Required device/CI gates
1. GitHub Actions or the Windows builder must report BUILD SUCCESS for `:app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin`. Save the complete diagnostic if it fails.
2. Keep installed app data. Compare APK certificate to the installed NightWave; **never uninstall or clear app storage** to resolve signing conflicts.
3. Use Android Auto Desktop Head Unit (DHU) or Android Automotive OS emulator with a test phone/emulator (not while driving). Verify NightWave appears once as a media app.
4. Browse Imported, Liked, Recent and named playlists; pagination and search must remain responsive, and browsing alone must not start playback or open a network connection.
5. Choose three offline files from a test library and check play/next/previous/seek, repeat/shuffle, gapless, crossfade and lock-screen controls. Return to Browse without interrupting playback.
6. Choose one previously saved public YouTube watch URL; verify it goes through NightWave's existing resolver, not direct ExoPlayer playback of the webpage URL. If the source is unavailable, show a recoverable failure, no crash or unintended playback of an older song.
7. Try malformed media IDs, unknown indexed local tracks, empty playlists, offline-only browser requests and interrupted network. No app-private file path or signed audio URL should be exposed to car controllers.
8. Headset/Bluetooth disconnect must pause or safely honor audio focus; reconnect must not force auto-play.
9. Test widget Pause/Next/Previous, media notifications, main Activity reconnect, service recreation, Sleep Timer expiry and all previous offline files/library backups.
10. Attempt Gradle instrumentation tests on an emulator. Do not run destructive tests on an existing media library.

## Open blockers
- PR CI ran but supplied no usable steps/logs, so **APK build status is unknown/failing**. Do not install from this branch yet.
- The first car/MediaBrowser playback handoff has been implemented but not tested in an Android Auto host; it must not be declared feature-complete until browsing AND selection work on an emulator.
- Android Auto browse views and car search are system-controlled UI, not custom graphics.
