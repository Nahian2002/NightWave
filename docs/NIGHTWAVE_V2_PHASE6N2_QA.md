# NightWave 6N2 — Reliable end-of-track handoff

Stacked draft on 6N1; original main and device data are untouched.

## Implemented
- Fence a deferred Media3 STATE_ENDED event by the exact media ID, source mode (public vs imported offline), selection generation, current selected entry, player END state, sleep-timer pause state and one-shot completion.
- A posted callback from a previous song cannot skip a newer manual Next, queue Jump, offline selection or resuming track.
- Prevent duplicate end callbacks from advancing multiple entries. When playback genuinely resumes, the completion marker resets so manual replay and Repeat One can end normally again.
- Existing repeat, shuffle, queue, and explicit-resume behavior remains owned by the service and original queues; no extra player/timer/worker is created.
- Offline transition skips unavailable app-owned files with a single reusable library facade, validating every file before playback. No private paths or streams are persisted.
- Extra guard prevents a delayed public error retry from restarting audio after the service selection generation has changed.

## Source and authored tests
- 12 new pure JVM test methods covering normal online/offline end, stale generation, changed selected or loaded ID, source-mode crossover, non-ended state, sleep-timer expiry, duplicate callbacks and missing IDs.
- Source integrity gate: tools/verify_v2_phase6n2.py, layered after previous phases.
- Android Gradle/JUnit/AndroidTest compilation is still unverified if the GitHub Actions run fails without acquiring a runner.
- User phone QA deferred. When requested, test rapid Next/Previous and Jump at song boundaries, repeat-one, shuffle, missing offline files, changing offline speed, sleep-timer expiry, Bluetooth, queue restore and background audio.

## Scope
This is **transition reliability**, not sample-perfect gapless or crossfade. NightWave currently uses one Media3 media item at a time and resolves public streams just-in-time; real prebuffered seamless transitions require separately reviewed queue/source architecture and codec-device verification. Do not promise gapless to users.
