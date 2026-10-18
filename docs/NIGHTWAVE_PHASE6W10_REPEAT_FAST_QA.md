# NightWave — Instant Repeat button hotfix (6W10)

## User-observed bug
While music was playing, the Repeat control felt delayed or required repeated presses. The earlier UI waited for service Intent handling and a saved-queue reconciliation, while Media3 event/ticker updates could repaint an old OFF/ALL/ONE state.

## Implemented
- StudioPlaybackBridge updates Compose state immediately on every press.
- Repeat cycles OFF -> ALL -> ONE -> OFF, including three rapid taps without waiting for callbacks.
- Each tap sends the **desired absolute mode**, not an ambiguous "toggle" intent. Commands arriving late cannot reinterpret the user's intended mode.
- The service keeps legacy toggle behavior for older callers, validates explicit enum names, commits its saved queue, and acknowledges a mode only when the saved queue reflects it.
- Bridge guards an optimistic mode against old Media3/ticker/queue refreshes while awaiting the **matching command ID** (bounded 2.5 seconds), then reconciles with service state.
- Repeat icon includes readable Off / All / One labels and accessibility descriptions.
- Player queue identity, track resolution, music files, volume, EQ and sleep timer logic remain untouched.
- Repeat remains intentionally unavailable for offline tracks, matching the existing player behavior. Offline repeat needs a separate feature implementation.

## Checks performed
11/11 targeted source checks passed on the GitHub branch, including explicit service commands, receipts, three-state logic, UI and legacy compatibility. Pure repeat policy unit tests were added.

## Still required before release
- **Not yet Android built or tested on a phone.** Run `build-windows.cmd` and require BUILD SUCCESS.
- With a public streaming track playing, tap repeat three times quickly. Visually expect All -> One -> Off without waiting for a second or music restarting.
- Restart the app: current repeat mode should match the last service-confirmed mode.
- Test at a track boundary, repeat-one behavior and repeat-all queue wrap.
- While music is paused and resumed, repeat changes must not cause player errors or reset the queue.
- Check offline track: repeat remains disabled (known scope). Confirm that limitation is clearly communicated.
- Check transition from public to offline and back, and rapid presses while buffering.

No NightWave-owned server required.
