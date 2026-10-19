# NightWave 6X8C2 — Phase 6P crossfade-aware build preflight hotfix

## User-reported failure
The Windows build stopped at **Phase 6P integration source preflight**, printing:
`PlaybackService must retain exactly one ExoPlayer builder`.

The earlier 6X8C1 duplicate import failure is fixed: the latest screenshot shows Kotlin source/import preflight PASS over 251 files. **Gradle did not start** for the reported 6X8C1 run.

## Root cause
Phase 6P predated Phase 6X2's opt-in offline crossfade and checked for exactly one `ExoPlayer.Builder(`. The current service intentionally has **two builder sites**:
1. A service-owned, session-connected main ExoPlayer.
2. A temporary, validated local offline-audio crossfade decoder.

The incoming deck is prepared without requesting audio focus, can be cancelled/released, and is transferred into the single MediaLibrarySession only after a valid crossfade handoff. Removing this second player would eliminate actual overlapping audio.

## Fixes
- `tools/check-phase6p-integration.ps1`: Require exactly two builder sites, explicit primary/secondary construction, candidate cleanup on exceptions, cancellation cleanup, session handoff and outgoing deck release. Reject zero/one/three or absent cleanup; do **not** disable the safety gate.
- `tools/verify_v2_phase6p.py`: Match the two-builder lifecycle and account for the additional Android Auto streaming dispatch in the existing explicit queue-order path. No hardcoded user media IDs are introduced.
- `tools/verify_v2_phase6x2.py`: Accept the temporary decoder's `setHandleAudioBecomingNoisy(true)` configuration before `.build()`. Continue to insist on failure and cancellation release paths.
- CI: Push trigger added for `fix/nightwave-v2-phase6x8c2-crossfade-preflight`. All previous source and Android Gradle tasks retained.

## Verification actually performed
- Compared the latest GitHub service with the original Windows 6P script and Python contracts.
- 14/14 targeted dual-player lifecycle, import, source and CI assertions passed.
- 34/34 independent source-contract checks corresponding to the cumulative Phase 6P Python gate passed.
- Reviewed the subsequent 13 Windows preflight scripts and all 70 CI Python script sources for old one-player/session assumptions. Found and corrected the extra Phase 6X2 stale builder-line assertion.
- These are **source inspections/assertions**. Windows PowerShell 5.1 preflight, Gradle Android compilation, JUnit execution and device playback have **not** run in this environment. Do not report an APK as built.

## Next local build
1. Extract the complete 6X8C2 branch ZIP to a fresh folder.
2. Run `build-windows.cmd`.
3. Confirm Kotlin compatibility preflight, Phase 6P integration preflight, the subsequent cumulative preflights, and **BUILD SUCCESSFUL** after Gradle.
4. If it fails, upload the new `dist/NightWave-build-errors.txt`, `dist/NightWave-build-diagnostic.txt`, `dist/NightWave-build-status.txt`, and `dist/NightWave-build-full.log`.
5. Do not uninstall the installed NightWave app, clear its storage, or install a newly built APK without validating that the signing certificate matches the installed app. Existing private songs, lyrics and Google Drive metadata must remain undisturbed.
