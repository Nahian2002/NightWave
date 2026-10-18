# Phase 6W3 — equalizer reliability and status

## Changes
- Fixed leaked temporary Android Equalizer object when a preset index was unsupported or setup failed.
- Release now independently attempts disable and release without retaining a stale active object.
- Service publishes a local status receipt: requested preset, Android enabled flag and a concise result.
- You-tab dialog shows the most recent receipt and re-reads after sending a command.
- Status is reset to OFF when service starts; manual device presets are still not enumerated, and numeric preset choices remain experimental.

## Caveats
- SharedPreferences.apply() is asynchronous; a 400 ms UI read is a convenience, **not a guaranteed command acknowledgement**.
- Android saying Equalizer.enabled == true does NOT establish audible processing on a particular output route.
- Device preset names and actual supported preset count are not exposed to UI yet.
- No microphone, audio recording, login, cloud processing, or NightWave server is introduced.

## Required verification
1. Build assembleDebug and testDebugUnitTest on Windows with the project's build-windows.cmd.
2. Confirm Kotlin compile, particularly Android Equalizer and Media3 audio-session callback APIs.
3. Exercise OFF / each available preset / unsupported preset on speaker and Bluetooth.
4. Change audio route during playback; verify old effect release and reattachment if possible.
5. Check service restart resets settings and status. Keep existing offline and public queues unchanged.
6. Stress next/previous, repeat, sleep timer, and offline speed alongside EQ.
7. Check that message feedback is not misleading when the service is stopped or a new command supersedes an older one.

**Status:** source committed, no completed Android compilation or device acceptance testing. Not a release certification. Full dynamic preset enumeration and manual band controls are still future tasks.
