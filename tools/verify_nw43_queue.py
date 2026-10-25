#!/usr/bin/env python3
"""NW43 queue-source integration gate, not a Kotlin compiler or device test."""
import argparse
from pathlib import Path
import tempfile

ROOT = Path(__file__).resolve().parents[1]
SRC = "app/src/main/java/com/nightwave/player/"
PATHS = {
    "ui": SRC + "v2/ui/NightWaveStudioActivity.kt",
    "queue": SRC + "playback/PlaybackQueue.kt",
    "service": SRC + "playback/PlaybackService.kt",
    "bridge": SRC + "v2/playback/StudioPlaybackBridge.kt",
    "storage": SRC + "playback/QueueStateStore.kt",
    "sheet": SRC + "v2/ui/StudioPlayerScreens.kt",
    "unit": "app/src/test/java/com/nightwave/player/playback/PlaybackQueuePlayNextTest.kt",
    "android": "app/src/androidTest/java/com/nightwave/player/v2/ui/NightWaveQueueSectionsInstrumentedTest.kt",
    "persist": "app/src/androidTest/java/com/nightwave/player/playback/QueuePlayNextPersistenceInstrumentedTest.kt",
}
BAD_SEARCH_COLLECTIONS = (
    "onPlay(song, state.videos)",
    "onPlay(song, savedMatches)",
    "onPlay(song, savedTracks)",
    "onPlay(song, selectedCreatorGroup.tracks)",
)
NEEDS = {
    "queue": [
        "fun insertNext(entry: QueueEntry): Boolean",
        "fun priorityNextUrls(): List<String>",
        "fun restoreNextPriorities(urls: List<String>)",
        "manualNext.addFirst(entry.url)",
        "queuedNext != null ->",
    ],
    "service": [
        'const val ACTION_PLAY_NEXT = "com.nightwave.player.PLAY_NEXT"',
        "ACTION_ENQUEUE, ACTION_PLAY_NEXT ->",
        "QueueRestorePolicy.isCanonicalPublicWatchUrl(url)",
        "if (!offlinePlaying)",
        "queue.insertNext(entry)",
    ],
    "bridge": [
        "fun playNext(video: VideoPreview) = sendQueueAddition(",
        "PlaybackService.ACTION_PLAY_NEXT",
        "VideoLink.normalize(video.url)",
    ],
    "storage": [
        '"manual_next"',
        "queue.priorityNextUrls()",
        "queue.restoreNextPriorities(snapshot.manualNextUrls)",
    ],
    "sheet": [
        '"PLAYING NOW"',
        '"UP NEXT"',
        '"SHUFFLE POOL',
        '"EARLIER IN THIS QUEUE"',
        "upcoming.isEmpty()",
    ],
    "ui": [
        'onPlay(song, listOf(song))',
        'requestPublicQueueAction(video, playNext = true)',
        'requestPublicQueueAction(song, playNext = true)',
    ],
}

def audit(src):
    errors = []
    for key, needles in NEEDS.items():
        for needle in needles:
            if needle not in src.get(key, ""):
                errors.append(key + ": missing " + repr(needle))
    ui = src.get("ui", "")
    try:
        search = ui.split("private fun StudioSearch(", 1)[1].split(
            "private fun StudioLibrary(", 1)[0]
    except IndexError:
        errors.append("StudioSearch boundaries missing")
        search = ui
    for needle in BAD_SEARCH_COLLECTIONS:
        if needle in search:
            errors.append("Search result illegally queues a collection: " + needle)
    if search.count("onPlay(song, listOf(song))") != 4:
        errors.append("Exactly four Search item-tap paths must be single-song")
    if src.get("unit", "").count("@Test fun ") < 14:
        errors.append("Queue Play Next pure tests incomplete")
    if src.get("android", "").count("@Test fun ") < 3:
        errors.append("Queue grouping Android tests incomplete")
    if src.get("persist", "").count("@Test fun ") < 2:
        errors.append("Play Next persistence Android tests incomplete")
    return errors

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--self-test", action="store_true")
    parser.add_argument("--root", type=Path, default=ROOT)
    args = parser.parse_args()
    if args.self_test:
        fake = {k: "\n".join(v) for k, v in NEEDS.items()}
        fake["ui"] = (
            "private fun StudioSearch(\n" +
            "onPlay(song, listOf(song))\n" * 4 +
            "\nprivate fun StudioLibrary(\n" + fake["ui"]
        )
        fake["unit"] = "@Test fun x() {}\n" * 14
        fake["android"] = "@Test fun x() {}\n" * 3
        fake["persist"] = "@Test fun x() {}\n" * 2
        assert not audit(fake), audit(fake)
        bad = dict(fake)
        bad["ui"] = bad["ui"].replace(
            "onPlay(song, listOf(song))", "onPlay(song, state.videos)", 1)
        assert audit(bad)
        print("PASS: NW43 source gate detects accidental Search queue pollution")
        return 0
    missing = [v for v in PATHS.values() if not (args.root / v).is_file()]
    if missing:
        print("FAIL: missing files", *missing, sep="\n")
        return 1
    src = {k: (args.root / v).read_text(encoding="utf-8")
           for k, v in PATHS.items()}
    problems = audit(src)
    for item in problems:
        print("FAIL:", item)
    if problems:
        return 1
    print("PASS: NW43 source queue contracts, 14 JVM + 5 Android test declarations")
    print("NOT VERIFIED: Kotlin compiler, Android UI, APK signing or phone playback")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
