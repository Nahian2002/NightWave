# NW60 — Offline decoder-error recovery (source patch)

**Parent:** `feature/nightwave-nw59-integrated-build-gates`  
**Branch:** `feature/nightwave-nw60-offline-playback-recovery`  
**Status:** Source implemented; **full Android/JUnit compilation and actual phone decoding are not yet verified.** No APK or public release was published.

## Confirmed bug in the existing service

`PlaybackService.onPlayerError()` immediately returned for **every offline Media3 error**. Even when the listener had a legitimate active local queue with another song, the app made no attempt to recover. Manual Next/Jump could additionally leave Media3 in `STATE_IDLE` after a failed decoder, because the previous code sought and played without always calling `prepare()`.

This is a meaningful code-path defect, not evidence of any particular Samsung codec failure in an installed NW59 APK.

## NW60 changes

1. On an actual decoder error for the currently selected `offline:<private-indexed-id>`, mark the ID as failed for the current selection. Never trust raw file paths, external URLs or a mismatched/stale media item.
2. Post recovery out of the Media3 callback. Cancel the posted action if the user changed playback generation, left offline playback, or switched to a different media ID in the meantime.
3. **Sequential playback:** attempt a later *distinct* unfailed entry from the existing offline queue; wrap only if Repeat All is selected. **Shuffle:** use only Media3's real next entry, never fabricate a random song or replay an already-failed ID. If no valid candidate remains, stop automatic recovery (no loop).
4. Reprepare the existing Media3 timeline if the decoder error left it `STATE_IDLE`, then play the chosen local entry. For the legacy one-item timeline, reuse the existing `playOfflineId` validation path. User-initiated Next, Previous, Jump or a new selection explicitly clear failure history so users can deliberately retry.
5. Do **not** delete/import/repair audio files, mutate the saved library, silently search the internet, override permission restrictions, cache YouTube sources, or expose local file names in logs.
6. Streaming/public-media recovery continues through its original separate `PlaybackRecoveryPolicy`. No changes to online queue semantics.

## Tests and limitations

- `OfflineDecodeRecoveryPolicyTest` covers selected-ID ownership, untried queue traversal, Repeat All wrapping, one-item stop, shuffle order and a maximum queue bound.
- Isolated `kotlinc` compilation of the pure policy and 11 independent assertions passed locally. This is **not** a full Android/Media3 execution.
- `tools/verify_nw60_offline_recovery.py --self-test` adds source contracts and six broken-code fixtures. It is called by GitHub CI (which has previously failed before runner allocation).
- Windows' existing `tools/check-nw59-integration.ps1` now includes NW60 without adding another startup-heavy script. `build-windows.cmd` invokes that checker before Gradle, after the older NW52 checks.
- Physical device and Android tests remain mandatory. Media3's decoder state/seek semantics vary with codec/container/device.

## Device acceptance matrix

1. With supported authorized audio files stored locally, turn off Wi-Fi and cellular data. Play an MP3 then Next/Previous, seek, pause/continue and close/reopen NightWave.
2. With a **disposable** deliberately damaged/unsupported sample (not personal audio), place it between two playable tracks. Confirm that a decoder failure skips at most to the next existing saved entry, never inserts suggestions and never runs indefinitely. Confirm the bad file remains listed under **Issues**; no automatic deletion.
3. Repeat the above with Repeat Off, Repeat All and Shuffle. One bad file with Repeat All must not retry itself indefinitely; repeatable decoding failures across all local files must terminate cleanly.
4. After an error, tap a different valid song or Next/Jump. It must recover from idle with the new track and preserve the user's queue.
5. Lock screen, Bluetooth pause/resume, notification controls and normal online YouTube playback must remain unchanged.
6. If Google sign-in still returns **internal error 8**, copy the credential-free diagnostic from You → Google setup report and compare installed APK package/SHA-1 with Google Cloud OAuth, Drive API activation and test-user status. NW60 **does not** claim to repair server-side OAuth registration.

**Public release:** still NO-GO pending full compilation, app signing, offline/Bluetooth/device regressions, API 36 Play compatibility, privacy and media-rights verification.
