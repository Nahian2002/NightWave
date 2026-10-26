#!/usr/bin/env python3
"""NW99 committed-source integration guard; no Android environment claims."""
from pathlib import Path

root=Path(__file__).resolve().parents[1]
def read(name):return (root/name).read_text(encoding="utf-8")
svc=read("app/src/main/java/com/nightwave/player/playback/PlaybackService.kt")
policy=read("app/src/main/java/com/nightwave/player/playback/MediaSessionQueueControlPolicy.kt")
tests=read("app/src/test/java/com/nightwave/player/playback/MediaSessionQueueControlPolicyTest.kt")
ci=read(".github/workflows/android.yml")
widget=read("app/src/main/java/com/nightwave/player/widget/NightWaveWidgetController.kt")
assert "import androidx.media3.session.CommandButton" in svc
assert ".setMediaButtonPreferences(listOf(" in svc
assert "CommandButton.Builder(CommandButton.ICON_PREVIOUS)" in svc
assert "CommandButton.Builder(CommandButton.ICON_NEXT)" in svc
assert "NightWaveWidgetController.COMMAND_PREVIOUS, Bundle.EMPTY" in svc
assert "NightWaveWidgetController.COMMAND_NEXT, Bundle.EMPTY" in svc
assert "session.isMediaNotificationController(controller)" in svc
assert svc.count("MediaSessionQueueControlPolicy.trustedController(")>=2
assert "MediaSessionQueueControlPolicy.knownAction(" in svc
assert "MediaSessionQueueControlPolicy.canNavigate(" in svc
assert "offlineSelected = offlineQueue.current != null" in svc
assert "publicSelected = queue.current != null" in svc
assert "return super.onConnect(session, controller)" in svc
assert "SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED)" in svc
assert "NightWaveWidgetController.COMMAND_NEXT -> playNext(false)" in svc
assert "NightWaveWidgetController.COMMAND_PREVIOUS -> playPrevious()" in svc
assert "playWhenReady = connected.playWhenReady" in widget
assert "sameAppPackage || mediaNotification" in policy
assert "if (offlinePlaying) offlineSelected else publicSelected" in policy
assert tests.count("@Test fun ")>=8
assert "python3 tools/verify_nw99_media_session.py" in ci
assert "tools/NW99MediaSessionQueueSmoke.kt" in ci
print("NW99_MEDIA_SESSION_SOURCE_WIRING_PASS")
