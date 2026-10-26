# NW93 — Queue Persistence & Playback Recovery Hardening

**26 October 2026** — cumulative branch `feature/nightwave-nw93-queue-recovery`, parent NW92 `a28266e6501329145f4eaeffcd8a005f7f34e22f`.

## Confirmed defect

`PlaybackQueue` tracked shuffle Previous, forward retrace and songs already visited exclusively as in-memory integer indices. `QueueStateStore` persisted the selected public track, shuffle/repeat settings and Play Next priorities, but not those histories. After process/service death, Previous could lose its trail and Next could repeat songs previously heard in the same shuffle cycle.

## NW93 correction

- Stores visited/retrace/seen histories as **canonical public track IDs**, not transient queue indices or stream URLs; restores them after validating the exact selected queue and shuffle mode.
- New `ShuffleRecoveryPolicy` rejects foreign/unavailable URLs, duplicate seen-cycle entries and arrays longer than 100; corrupt history loses *only* transient history, leaving the selected song and saved queue intact.
- Legacy snapshots with no `shuffle_history` field continue to restore normally. New JSON fields are additive; no preference namespace, database schema or file migration is performed.
- `PlaybackQueue` rehydrates histories after setting shuffle and repeat, before Play Next priorities; queue transitions already call `savedQueue.save(queue)` for Next/Previous/Jump/mode changes.
- Original `shuffle` reset and explicit order semantics, offline playback, API/OAuth, media source handling and installed app data are unchanged.

## Evidence

- **10/10** locally executed pure Kotlin/JVM policy regression fixtures with the system compiler (good history, absent/legacy state, wrong selection, untrusted ID, oversized arrays, duplicate seen, repeated true visits, forward trail, full-cycle retention).
- **Six** added Android Gradle/JUnit integration tests for real `PlaybackQueue` history export/import behavior, written but **not executed** in this session.
- New Python structural wiring check and CI Kotlin smoke tasks.
- Current GitHub runners may still fail with zero executed steps. Do not infer Kotlin compile success from that infrastructure failure.
- Full Android/JUnit instrumentation, device restart, signing/upgrade continuity and the NW90 139-case acceptance matrix remain **NOT VERIFIED**.

## Residual limitations

- A crash before the last successful preference commit can still lose recent queue transitions; there is no guarantee of instant crash durability after a failed disk commit.
- History is bounded to 100 entries; restoring untrusted or malformed history intentionally discards it without deleting persisted queue data. The queue remains capped at 100 songs.
- Playback does not automatically start after process creation: user transport remains the authority.
- Public release remains NO-GO pending build, device, content licensing, target API and release/privacy acceptance.

## Safe owner-side steps

Download NW93 in a fresh folder and run `run-nw91-final.cmd` for the cumulative Windows source/JUnit/APK gate. Use `check-device-update.cmd` before installing any APK. Never uninstall or reset existing offline songs to overcome a signer mismatch.

Optional Kotlin smoke if installed:

```
kotlinc app/src/main/java/com/nightwave/player/playback/ShuffleRecoveryPolicy.kt tools/NW93ShuffleRecoverySmoke.kt -include-runtime -d nw93-smoke.jar
java -jar nw93-smoke.jar
```
