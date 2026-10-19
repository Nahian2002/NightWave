# NightWave Phase 6X8C — Media3 compilation safety and build evidence

## Motivation
The 6X8B Android Auto development branch introduced `MediaLibraryService` and callback error results using `SessionError.ERROR_BAD_VALUE`. Android Media3 marks that error constant as `@UnstableApi`; official AndroidX sample callbacks explicitly opt in. Kotlin consumers therefore must opt in to avoid an opt-in compiler error when compiler checks enforce Media3's API annotations.

## Applied changes
- The existing `PlaybackService` is now annotated `@OptIn(UnstableApi::class)`, with `androidx.media3.common.util.UnstableApi` imported. This does **not** change the Media3 1.6.1 dependency, playback engine, state transitions, session IDs, player controls or Android Auto catalog results.
- Added `tools/verify_v2_phase6x8c.py` and wired it into the existing cumulative GitHub Actions source checks. The gate parses the real Android manifest and automotive descriptor, checks for exactly one playback service, enforces explicit Media3 API opt-in and confirms the existing widget, queue, crossfade, normalization, Bluetooth and Sleep Timer entrypoints still exist.
- No database migration, cloud/Drive backup, app ID, signing or user-library files are touched. No server introduced.

## Verification status
- **Code change committed.** Confirmed the GitHub source contains the opt-in import and annotation.
- Android documentation and AndroidX's official MediaLibrarySession callback samples support the opt-in requirement.
- Source-gate source committed; **the gate itself has not been executed against a complete local checkout**: this environment has no Android SDK, the GitHub workspace cannot be fetched via container networking, and no Android build log was available through the GitHub connector.
- 6X8B's earlier two GitHub PR workflow jobs concluded failure without job-step data or downloadable logs. These failures were not proven to be caused by the missing opt-in. No evidence of a successful Android compilation, JUnit run, device test, Android Auto emulator run, or safe-to-install APK exists for 6X8C.
- At the time of inspection, commit statuses exposed no completed checks for this 6X8C branch.

## Release gate — mandatory before installation
On Windows, use the checked-in `build-windows.cmd` and require a fresh **BUILD SUCCESSFUL** in the full log (Gradle tasks: `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:compileDebugAndroidTestKotlin`). If a failure occurs, inspect the generated `dist/NightWave-build-errors.txt`, `dist/NightWave-build-diagnostic.txt`, `dist/NightWave-build-status.txt`, and `dist/NightWave-build-full.log`. These are the evidence needed for a bounded patch, not guesses.

**Never uninstall NightWave, clear app data, or install an APK with a mismatched signing certificate.** Debug APKs built by GitHub may have a different signer from your installed app.

After compilation passes, test media notification Play/Pause/Next/Previous, offline queue/Repeat/Shuffle, widgets, Bluetooth headphone-disconnect pause and Android Auto browser/search/select/stream playback on a DHU/AAOS emulator while parked or at a desk. Imported music, playlists, lyrics, Google Drive backups and Sleep Timer must remain intact.

## Next planned Spotify-style improvements after a verified build
1. Unified saved-music library that can search both imported local audio and saved streaming tracks.
2. Non-destructive offline artist/album groups backed by extracted ID3/MediaMetadata instead of guessed labels.
3. Higher-quality listening handoff and richer downloadable/cached lyrics, respecting source rights and local-only constraints.
