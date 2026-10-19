# NightWave Phase 6X3 — Safe Offline Volume Balance

## Parent / preserved features
Base: `feature/nightwave-v2-phase6x2-adjustable-crossfade` (includes 6X1 local gapless, 6X2 adjustable local crossfade, the prior EQ dialog visibility hotfix, Sleep Timer dial, and instantaneous repeat).
No app ID, package signing, library file, database migration, permission, server, online extractor, or Google Drive backup change.

## Implementation
- **Off by default.** In offline Now Playing, a "Volume balance" switch turns local, per-track attenuation on or off.
- The service selects the current private-library song and the next local song using their registered IDs, validates them through `OfflineAudioLibrary.fileFor()`, and submits bounded decode work to a 1-worker/1-pending-job executor.
- Android MediaExtractor + MediaCodec decode up to the first 12 seconds of the verified private file (maximum 1.5M PCM samples, ~3 seconds processing budget). Supports PCM16 and floating-point output when available. Unsupported, unreadable or undecodable formats are not normalized; ordinary music playback is preserved.
- Computes approximate PCM RMS and measured sample peak, selects a conservative 0.25–1.0 gain capped by target RMS 0.13 and sample peak 0.98. Never requests gain greater than unity or modifies Android system volume.
- The measured attenuation is cached with the audio ID and byte length in an app-private preference store. **Audio bytes and paths are never written to this cache.** Corrupt/missing cache entries fall back to analysis or normal listening level.
- The existing Listening Level (100 / 85 / 70%) remains the user's master setting. Normalized output uses `ListeningLevel × track attenuation`. Each crossfade deck independently uses its song's attenuation in the overlap; the total remains bounded by the master setting.
- Offline gapless automatic track transitions update volume and pre-analyze the following track. Returning to public streaming immediately restores normal Listening Level, with no offline attenuation.
- Disabling the switch immediately restores the normal master level. The setting is local and is loaded after normal activity/service restarts; no automatic playback on startup.
- No claims of Spotify-equivalent EBU R128 / LUFS mastering, true-peak safety, or volume boosting. This is an **RMS/peak-informed, non-boosting local loud-track reducer**; quiet tracks cannot be amplified with the existing 0–1 Media3 volume API.

## Verification performed
- 13 standalone Kotlin behavior checks passed for the mirror of `OfflineNormalizationPolicy`, including loud/quiet tracks, peak limitation, disabled behavior, clamping, nonfinite values, and master-gain multiplication.
- 8 JUnit cases added to `OfflineNormalizationPolicyTest.kt` (Gradle execution pending).
- 25/25 GitHub source-consistency checks passed after the integration edits; one cache-default validation fix followed these checks.
- `tools/verify_v2_phase6x3.py` added and wired into Android CI; 6N3 legacy source gate updated to recognize the shared gain owner rather than expect an old direct volume assignment.
- Full Android Gradle build, instrumentation, native decode on device, and real audio volume matching **not yet verified**.

## Mandatory tests after a verified Android build
1. Use `build-windows.cmd`; install only after `BUILD SUCCESS` and certificate compatibility. Never uninstall or clear NightWave's library to resolve signing conflicts.
2. Offline Now Playing: check Volume balance is Off by default, enable it and verify it remains enabled after leaving and reopening the screen.
3. Import several personally owned MP3/AAC/FLAC songs with noticeably different mastered loudness. Play the louder track; after initial local analysis, its volume should be reduced. Quiet tracks should remain unchanged, not amplified.
4. Switch the feature Off while playing: normal Listening Level should return. Set Listening Level to 70% and verify normalized mode never overrides that user-set ceiling.
5. Use 6X2 crossfade with 4 seconds: transitions must not spike in loudness; note any audible steps while a first-time analysis completes. Return to Off for 6X1 gapless-only transitions.
6. Change current local song, skip multiple songs, seek, pause, lock phone, play using notification controls, and change Bluetooth output while normalization is enabled. No clicks, stuck silent tracks, crashes or service restarts.
7. Play a streaming track immediately after a normalized offline one: ordinary streaming volume should be restored, and online track behavior should not use a private-file gain.
8. Test one unsupported/invalid local encoding only if safely available; failed analysis must not delete, rewrite or render the song unplayable.
9. Turn On, force stop/relaunch through ordinary app lifecycle (do not clear app data), verify the saved toggle and that music never automatically starts on service creation.
10. Verify EQ, Sleep Timer, offline speed, repeat modes, playlists, lyrics and Drive backups still behave as before.

## Limits
- First playback may initially be at normal volume until the bounded analysis finishes; a detectable downward change is possible. Cached subsequent playback avoids repeated decoding.
- RMS of a short sample is not integrated perceived loudness (LUFS), and cannot guarantee equal perceived loudness for every track.
- Only app-private offline files are analyzed. Online tracks and protected streams are not scanned or downloaded.
- This branch is **development source**, not a confirmed Android APK.
