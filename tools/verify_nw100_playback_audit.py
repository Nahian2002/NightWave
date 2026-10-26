#!/usr/bin/env python3
"""NW100 cumulative playback source gate, not an Android build certificate."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
def read(p): return (root/p).read_text(encoding="utf-8")
public=read("app/src/main/java/com/nightwave/player/playback/PlaybackQueue.kt")
offline=read("app/src/main/java/com/nightwave/player/offline/OfflinePlaybackQueue.kt")
svc=read("app/src/main/java/com/nightwave/player/playback/PlaybackService.kt")
ci=read(".github/workflows/android.yml")
assert "fun hasNext(): Boolean {" in public
assert "fun hasNext(): Boolean {" in offline
assert "if (priorityNextUrls().isNotEmpty()) return true" in public
assert "forwardVisits.lastOrNull()" in public
assert "it != index && it !in shuffledSeen" in public
assert "repeat == Repeat.ALL" in public
assert "return unplayed.any { it in ids.indices && it != index }" in offline
assert "repeat == PlaybackQueue.Repeat.ALL" in offline
assert "if (action == NightWaveWidgetController.COMMAND_NEXT &&" in svc
assert "!hasManualNext()" in svc
assert "private fun hasManualNext(): Boolean {" in svc
assert "if (!offlinePlaying) return queue.hasNext()" in svc
assert "if (!offlineGaplessActive) return offlineQueue.hasNext()" in svc
assert "player.nextMediaItemIndex" in svc
assert 'player.getMediaItemAt(next).mediaId == "offline:$id"' in svc
assert "SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED)" in svc
assert "NW100QueueAvailabilitySmoke.kt" in ci
assert "verify_nw100_playback_audit.py" in ci
assert "verify_nw99_media_session.py" in ci
assert "verify_nw98_background_intent.py" in ci
assert "verify_nw97_offline_transitions.py" in ci
assert "verify_nw96_equalizer.py" in ci
assert "verify_nw95_audio_route.py" in ci
assert "verify_nw94_playback.py" in ci
assert "verify_nw93_queue_recovery.py" in ci
assert "verify_nw92_storage.py" in ci
print("NW100_CUMULATIVE_PLAYBACK_WIRING_PASS")
