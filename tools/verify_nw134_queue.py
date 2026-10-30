"""NW134 caller/command fences. JVM behavior and phone transport acceptance are separate."""
import argparse
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / "app/src/main/java/com/nightwave/player"
FILES = ("playback/PlaybackQueue.kt", "playback/PlaybackService.kt",
         "v2/playback/StudioPlaybackBridge.kt", "v2/ui/StudioPlayerScreens.kt",
         "v2/ui/NightWaveStudioActivity.kt")

def verify(src):
    queue, service, bridge, ui, activity = (src[x] for x in FILES)
    for marker in ("editSnapshot() != expected", "from <= index", "to <= index",
                   "entries[from].url in priorities", "entries[to].url in priorities",
                   "entries.subList(index + 1, entries.size).clear()",
                   "entries.map { it.url } != removed.remainingUrls", "restoreNextPriorities(removed.priorityBefore)"):
        assert marker in queue, marker
    handler = service.split("ACTION_MOVE_UPCOMING, ACTION_CLEAR_UPCOMING -> {",1)[1].split("ACTION_REMOVE_UPCOMING ->",1)[0]
    for marker in ("!offlinePlaying && !queue.shuffle", "QueueRestorePolicy::isCanonicalPublicWatchUrl",
                   "queue.moveUpcoming(", "queue.clearUpcoming(expected)", "if (changed) savedQueue.save(queue)"):
        assert marker in handler, marker
    for forbidden in ("resolveAndPlay", "exoPlayer", "queue.replace", "offlineQueue.reset"):
        assert forbidden not in handler, forbidden
    for marker in ("actual != expected", "state.offline || state.shuffle", "scheduleQueueRefresh()",
                   "EXTRA_QUEUE_EDIT_PRIORITIES", "EXTRA_QUEUE_EDIT_PLAYING"):
        assert marker in bridge, marker
    for marker in ('testTag("nw134-confirm-clear")', "clearRequest = editSnapshot",
                   "onClearUpcoming?.invoke(reviewed)", "onMoveUpcoming?.invoke(from, to, editSnapshot)",
                   "requestRemove(index, video.url)", 'Text(strings.get(R.string.nw141_move_earlier_73661))', 'Text(strings.get(R.string.nw141_move_later_d6e85))'):
        assert marker in ui, marker
    assert "playerActions.moveUpcoming(from, to, expected)" in activity
    assert "playerActions.clearUpcoming(expected)" in activity

if __name__ == "__main__":
    parser=argparse.ArgumentParser(); parser.add_argument("--self-test",action="store_true"); args=parser.parse_args()
    src={name:(BASE/name).read_text(encoding="utf-8") for name in FILES}; verify(src)
    if args.self_test:
        for name, marker in ((FILES[0],"editSnapshot() != expected"),
                             (FILES[0],"restoreNextPriorities(removed.priorityBefore)"),
                             (FILES[1],"!offlinePlaying && !queue.shuffle"),
                             (FILES[1],"if (changed) savedQueue.save(queue)"),
                             (FILES[2],"actual != expected"),
                             (FILES[3],"onClearUpcoming?.invoke(reviewed)"),
                             (FILES[4],"playerActions.moveUpcoming(from, to, expected)")):
            broken=dict(src); broken[name]=broken[name].replace(marker,"BROKEN_FIXTURE")
            try: verify(broken)
            except (AssertionError,IndexError): continue
            raise AssertionError("Accepted broken caller: "+marker)
    print("PASS NW134 snapshot-bound queue edits, priority Undo, service ownership and confirmation; device NOT_RUN")
