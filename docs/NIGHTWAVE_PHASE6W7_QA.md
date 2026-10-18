# Phase 6W7 — EQ command receipt stabilization

## Implemented
- Unique UUID command tokens sent with preset and atomic band-array changes.
- Service echoes matching token to the latest EQ state receipt after processing the command.
- UI polls for a matching receipt and reports unavailable instead of treating service start as proof of success.
- Invalid preset explicitly rejected.
- Android audio effect remains optional and OFF by default; no backend.

## Verified source checks (6/6)
- Shared command constant present
- Receipt persisted with token
- UI compares exact token
- Invalid preset handling present
- Batch validates exact audio session
- No fixed 5-band dialog

## Still not verified
- Android Gradle assembleDebug and testDebugUnitTest.
- Hardware device, headset, speaker and Bluetooth switch testing.
- End-to-end API behavior and installed APK signer continuity.
- Strong synchronization guarantee when two commands overlap: local preferences store only the latest token and may overwrite an earlier receipt. UI may show unavailable for an earlier request.

## Required release test
1. Windows build-windows.cmd compilation and tests.
2. Existing debug APK signing compatibility without uninstall/data reset.
3. EQ preset and manual controls with local/streaming tracks, play/pause, next, sleep timer.
4. Bluetooth route switching after capabilities discovery.
5. Rapid successive commands verify only matching receipt shown.
6. Unsupported preset/no-session proves fail-open audio and no crash.
