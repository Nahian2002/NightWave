#!/usr/bin/env python3
"""NW60 — fail-closed source checks for Media3 local decode-error recovery.

Read-only and does not call Media3, scan/delete files, access the network,
or claim successful Android compilation or actual offline decoding.
"""
from __future__ import annotations

import argparse
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SERVICE = ROOT / "app/src/main/java/com/nightwave/player/playback/PlaybackService.kt"
POLICY = ROOT / "app/src/main/java/com/nightwave/player/playback/OfflineDecodeRecoveryPolicy.kt"
TEST = ROOT / "app/src/test/java/com/nightwave/player/playback/OfflineDecodeRecoveryPolicyTest.kt"


def verify(service: str, policy: str, tests: str) -> None:
    markers = {
        "service": (
            "override fun onPlayerError(error: PlaybackException)",
            "if (offlinePlaying) {",
            "OfflineDecodeRecoveryPolicy.belongsToSelection(",
            "offlineDecodeFailedIds.add(selectedId!!)",
            "val atErrorGeneration = generation",
            "generation != atErrorGeneration",
            "OfflineDecodeRecoveryPolicy.safeShuffledIndex(",
            "OfflineDecodeRecoveryPolicy.nextLinearIndex(",
            "if (targetIndex == null)",
            "playerNow.seekTo(targetIndex, 0L)",
            "if (playerNow.playbackState == Player.STATE_IDLE)",
            "playerNow.prepare()",
            "offlineQueue.jump(targetIndex)",
            "playOfflineId(nextId)",
            "ACTION_PLAY_OFFLINE -> {",
            "ACTION_RESUME_OFFLINE -> {",
            "ACTION_JUMP_OFFLINE -> if (offlinePlaying)",
            "offlineDecodeFailedIds.clear()",
            "if (!automatic) offlineDecodeFailedIds.clear()",
            "cancelPendingResolution()",
            "PlaybackRecoveryPolicy.mayAutoRetry(",
        ),
        "policy": (
            'private const val PREFIX = "offline:"',
            "queueIds.size in 1..MAX_QUEUE_SIZE",
            "selectedId !in alreadyFailed",
            "fun nextLinearIndex(",
            "if (!repeatAll && candidate >= ids.size) return null",
            "ids[at] !in alreadyFailed",
            "fun safeShuffledIndex(",
            "it != currentIndex",
            "ids[it] !in alreadyFailed",
        ),
        "tests": (
            "onlyTheActualSelectedPrivateTrackCanTriggerRecovery",
            "linearRecoverySkipsBrokenTracksAndNeverRevisitsFailedIds",
            "repeatAllCanWrapButNeverReplayBrokenSource",
            "shuffleOnlyUsesMedia3OwnedNextSlot",
            "maliciousOrOverlargeQueueFailsClosed",
        ),
    }
    for name, data in (("service", service), ("policy", policy), ("tests", tests)):
        for token in markers[name]:
            if token not in data:
                raise AssertionError(name + ": missing " + token)

    handler = service.split(
        "override fun onPlayerError(error: PlaybackException)", 1
    )[1].split("if (queue.current?.url != video) return", 1)[0]
    for forbidden in (
        "library.delete(", "File.delete(", "getAuthorizationResultFromIntent",
        "resolveAndPlay(", "YouTubeGateway(", "startDownload(", "download(",
    ):
        if forbidden in handler:
            raise AssertionError("Decode-error recovery must never perform " + forbidden)
    if "if (offlinePlaying || queue.current?.url != video) return" in service:
        raise AssertionError("Offline decoding errors are ignored again")
    if "if (!offlinePlaying || generation != atErrorGeneration ||" not in handler:
        raise AssertionError("Posted recovery must be fenced against track changes")

    jump = service.split("ACTION_JUMP_OFFLINE -> if (offlinePlaying)", 1)[1].split(
        "ACTION_RESUME_SAVED ->", 1
    )[0]
    if "if (player.playbackState == Player.STATE_IDLE) player.prepare()" not in jump:
        raise AssertionError("User jump after a decoder failure may stay IDLE")

    manual = service.split("private fun advanceOffline(", 1)[1].split(
        "private fun playNext(", 1
    )[0]
    if "if (!automatic) offlineDecodeFailedIds.clear()" not in manual:
        raise AssertionError("Manual Next/Previous must allow a deliberate retry")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    files = [
        SERVICE.read_text(encoding="utf-8"),
        POLICY.read_text(encoding="utf-8"),
        TEST.read_text(encoding="utf-8"),
    ]
    verify(*files)
    if args.self_test:
        for index, old, new in (
            (0, "offlineDecodeFailedIds.add(selectedId!!)",
             "offlineDecodeFailedIds.clear()"),
            (0, "generation != atErrorGeneration",
             "generation == atErrorGeneration"),
            (0, "if (!automatic) offlineDecodeFailedIds.clear()",
             "// no explicit user retry"),
            (1, "ids[at] !in alreadyFailed", "true"),
            (1, "it != currentIndex", "true"),
            (2, "shuffleOnlyUsesMedia3OwnedNextSlot", "noShuffleTest"),
        ):
            broken = files[:]
            assert old in broken[index], old
            broken[index] = broken[index].replace(old, new, 1)
            try:
                verify(*broken)
            except AssertionError:
                continue
            raise AssertionError("Unsafe NW60 regression accepted: " + old)
        print("NW60 negative source fixtures: PASS")
    print("NW60 bounded offline decode/Next/Jump source contracts: PASS")
    print("UNVERIFIED: full Android build and local file decoder behavior.")


if __name__ == "__main__":
    main()
