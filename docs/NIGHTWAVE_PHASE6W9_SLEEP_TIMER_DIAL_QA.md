# Phase 6W9 — Sleep Timer duration dial

## Why
The prior Now Playing sleep timer had a tall dropdown with only 15, 30, 45 and 60 minutes. The user requested a premium dial and any desired minute value.

## Implemented
- A new Compose Dialog with circular touch/drag dial covering 1–120 minutes.
- Exact numeric minute input, accepting every whole minute from 1 through 1440 (24 hours).
- Existing 15/30/45/60 minute quick selections.
- Live duration text in the dial, active-timer label, explicit Cancel timer and Back.
- Scrollable, rounded material dialog; cancel action separated from Start for narrow screens and large text.
- Service ownership and monotonic elapsedRealtime scheduling unchanged. No server, account, new permissions or media/player replacement.
- SleepTimerPolicy now validates arbitrary 1..1440 durations; rejects zero, negatives, >1440 and overflow.
- Updated historical Phase 6K source gate to recognize the new dialog and ensure original stop/cancel/service safeguards remain.
- Expanded unit tests for minute boundaries, dial quadrants and geometry validation.

## Source review
12 targeted source consistency checks passed: new duration bounds, service monotonic policy usage, dial input, visible cancellation, preset retention, dialog connection, updated tests/source gate and brace consistency.

## Important limitation
Source checks do not prove Android compilation or visual correctness. No Gradle compile, on-device dial gesture, background timer, battery/Doze, or signer compatibility was run in this session.

## Device acceptance checklist
1. Build from branch with build-windows.cmd; require BUILD SUCCESS. Do not install from failed build.
2. Install over same signed app; never uninstall/delete saved data for a signing mismatch.
3. Open Now Playing > Sleep timer. Verify dialog is readable in portrait, landscape and enlarged font.
4. Tap/drag dial to 1, 37, 120. Verify selected minutes update; no timer should start before confirmation.
5. Type exact values 7, 61, 121, 240 and 1440; press Start; verify "Sleep in ... min" and preserved playback/queue.
6. Reject blank, 0 and 1441 without arming or cancelling an existing timer.
7. Start a 1-minute timer; lock phone, confirm pause at expiration, no unintended next-track action, and manual resume still works.
8. Cancel an active timer and confirm music continues beyond the previous deadline.
9. Re-open after app backgrounding; service restart must not silently re-arm an expired or lost timer.
