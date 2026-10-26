# NW99 — Notification Controls and MediaSession Queue Routing

Date: 27 October 2026 · Parent `4af2b8e958c35ad747a9d726e3432c1300a18537` · Branch `feature/nightwave-nw99-notification-session-commands`

## Root cause

NightWave correctly routed hardware headset key codes and private launcher-widget commands into `playNext()` / `playPrevious()`, but the public ExoPlayer timeline contains only one resolved public video at a time. Media3's generic "seek next/previous media item" therefore cannot represent all of NightWave's separate 100-item public queue in the notification. No queue-aware media button preferences were advertised to the notification controller.

Also, the custom command handler used to return `RESULT_SUCCESS` for NEXT and PREVIOUS even when no playable queue selection existed. A stale widget command could appear acknowledged despite doing nothing.

## Changes

- Add stable Media3 1.6.1 **media-button preferences** for Previous and Next, using `CommandButton.Builder(ICON_PREVIOUS / ICON_NEXT)` and the same existing `NightWaveWidgetController.COMMAND_*` session commands. The notification surfaces queue-level actions rather than assuming ExoPlayer owns an entire public playlist.
- Permit those commands for NightWave's verified own-package controller and Media3's actual `isMediaNotificationController`; preserve default `super.onConnect` behavior for all other clients. Hardware media-key interception and Android Auto browsing remain unchanged.
- `onCustomCommand` checks exact supported action AND selection in the active mode before calling the same existing `playNext(false)` / `playPrevious()`. Reject unknown/empty queue with `RESULT_ERROR_NOT_SUPPORTED` and unauthorized controllers with `RESULT_ERROR_PERMISSION_DENIED`.
- The pure `MediaSessionQueueControlPolicy` has no Android dependencies. No signed stream URLs, app-private file IDs or path parameters flow into these commands.
- No second MediaSession, foreground-service change or independent audio-focus implementation. Android Media3 continues to own Play/Pause and lifecycle.

## Verification / limitations

- 18 deterministic standalone Kotlin cases for controller trust, command identity, selected public/offline queue and rejection of cold or wrong-mode commands.
- Eight Android-module JUnit cases added but not executed without Android Gradle.
- Source integration script checks actual notification-button declaration and handler wiring. Workflow branch and canary updated for NW99.
- **Physical notification rendering, notification tap routing, Bluetooth/car controller interoperability and Android build remain unverified.** UI/system-level layouts may vary by Android release, and custom buttons can be disabled by a controller.
- Only source-level test evidence should be interpreted as passing; GitHub Actions has repeatedly failed without launching runner steps.

No change to app package, user data, Google auth, external accounts, downloaded audio, signing identity, manifest or backend.

**Public release NO-GO** until signed-update continuity, current Android build/unit+device acceptance, OAuth/backup smoke, source-rights review and target API checks are independently confirmed.

Media3 API references consulted:
https://developer.android.com/reference/androidx/media3/session/MediaSession.Builder
https://developer.android.com/reference/androidx/media3/session/MediaSession
https://developer.android.com/reference/androidx/media3/session/CommandButton.Builder
