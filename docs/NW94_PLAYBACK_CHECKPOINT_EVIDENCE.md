# NW94 — Public Playback Checkpoint Reliability

**26 October 2026** · base NW93 commit `1c1554ffff38bcb986139fa1562ea3a3d8a3b9d6` · branch `feature/nightwave-nw94-playback-checkpoint-reliability`.

## Confirmed defects

1. `PlaybackService.persistProgress(flush=true)` passed no durability argument to `QueueStateStore.saveBookmark`, so shutdown/pause still called `SharedPreferences.apply()` asynchronously.
2. After a new online queue selection, the outgoing Media3 item can briefly remain loaded. A delayed pause callback could persist the old item's playhead after the new song's zero-position bookmark, displacing the new resume location.
3. Mode acknowledgment logic read the persisted queue to confirm an operation, but the `save()` method hid its failed `SharedPreferences.commit()` return. NW94 does not acknowledge shuffle/repeat persistence unless that specific write succeeds.

## NW94 changes

- Add a **strict identity fence**: only a currently selected canonical public watch URL may update public bookmark state. Offline sessions use their pre-existing separate position store.
- Flush the outgoing selected song synchronously **before** replacing a different public queue selection; prevent late old-media callbacks from writing newer queue bookmarks.
- `QueueStateStore.saveBookmark` still defaults to non-blocking `apply()` for the ordinary 2.5-second ticker, but honors `flush=true` with a synchronous `commit()` for pause/shutdown.
- `QueueStateStore.save` returns the actual atomic preference transaction result; mode request receipts require the write's success plus existing readback guard.
- No change to preference keys, queue snapshot schema, app/Google sign-in identity, stored media files, signing, or serverless architecture.

## Test evidence

- New standalone Kotlin smoke exercises **11** normal/negative bookmark-identity cases. It can be compiled without the Android SDK using the repository's real pure-Kotlin VideoLink, QueueRestorePolicy, PlaybackQueue and ShuffleRecoveryPolicy source.
- New **six JUnit** tests test the same production policy, but require full Gradle execution to certify.
- New `tools/verify_nw94_playback.py` structural assertions and CI wiring.
- GitHub hosted runner may remain blocked before checkout. Full APK, complete JUnit suite, Samsung pause/process-death scenarios, 139-case acceptance, device signer and Music Vault remain **NOT VERIFIED** until physical/device/Windows build evidence is available.

## Residual limitations

- Android lifecycle can terminate a process without `onDestroy`; the previous asynchronous progress tick can be lost in a hard kill despite the pause/destroy durability improvement. Do not claim power-loss-proof progress.
- A write failure leaves the in-memory queue usable, but the older last successful checkpoint may be restored after process death. NW94 now avoids false receipt acknowledgements; transactional rollback of failed queue writes needs a broader state-machine review.
- Public media licensing/rights and Play target API migration still block public release.

## Owner-side verification

Use the fresh NW94 ZIP, `run-nw91-final.cmd` for cumulative Windows debug build and JUnit checks, and `check-device-update.cmd` **before** updating the installed app. No uninstall/reset to bypass signing mismatch. New pure-Kotlin smoke is in CI as `tools/NW94PublicBookmarkSmoke.kt`.

**Disposition: SOURCE IMPLEMENTATION + targeted checks, NOT a fully certified Android release.**
