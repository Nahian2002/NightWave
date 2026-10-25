#!/usr/bin/env python3
"""NW49: pure source regression/negative-fixture gate, NOT device audio QA."""
import argparse
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PLAYBACK = ROOT / "app/src/main/java/com/nightwave/player/playback"
UI = ROOT / "app/src/main/java/com/nightwave/player/v2/ui"
TESTS = ROOT / "app/src/test/java/com/nightwave/player/playback"


def verify(files: dict[str, str]) -> None:
    required = {
        "SoundStudioTuningPolicy.kt": (
            "object SoundStudioTuningPolicy",
            "enum class Shape { FLAT, WARM, VOCALS, AIRY }",
            "SoundStudioPolicy.safeManualRange(",
            "centersHz.size !in 1..32",
            "centersHz.any { it <= 0 }",
            "targetMb(shape, hz).coerceIn(range.first, range.last)",
            'edited -> "Not applied"',
        ),
        "NightWaveSoundStudio.kt": (
            "SoundStudioTuningPolicy.draft(",
            "SoundStudioTuningPolicy.Shape.entries",
            "selectedShape = null",
            "SoundStudioTuningPolicy.hasChanged(",
            "SoundStudioTuningPolicy.statusLabel(",
            'Text("Guided tuning"',
            "draftLevels = updated",
            "issueCommand(PlaybackService.ACTION_EQ_BANDS_APPLY)",
            "latest.commandId == requestId",
            "SoundStudioPolicy.safeManualRange(",
            "capabilities.centersHz[index]",
        ),
        "PlaybackService.kt": (
            "band in caps.centersHz.indices",
            "caps.audioSessionId == session",
            "supported != null && level in supported",
            "Rejected: unsupported band or stale audio session",
            "index !in 0 until candidate.numberOfBands.toInt()",
            "ACTION_EQ_BAND_SET",
            "CrossfadePolicy.canPrepare(",
            "if (!offlinePlaying || !offlineGaplessActive",
            "new.currentMediaItem?.mediaId != \"offline:$id\"",
            "OfflineGaplessPolicy.transitionIndex(",
            "normalizeSelectedAndNext()",
        ),
        "AudioTransitionUiPolicy.kt": (
            "object AudioTransitionUiPolicy",
            "fun description(",
            "shuffled ->",
            "repeatOne ->",
            "!normalSpeed ->",
            "only when the next verified offline track is ready.",
            "eligible local playlists",
        ),
        "StudioPlayerScreens.kt": (
            "val cueOriginTrack = state.videoUrl",
            "actions.seekToForTrack(cueOriginTrack,",
            "lyricsAutoFollow = true",
            "cueOriginTrack.isNotBlank()",
            "LyricsSyncPolicy.seekPosition(",
            "AudioTransitionUiPolicy.description(",
            "AudioTransitionUiPolicy.dialogDetail(crossfadeDraft)",
            "crossfade-seconds-slider",
            "lyrics-follow-toggle",
            "lyrics-fine-timing-toggle",
            "onImportLyrics",
            "onEditLyrics",
            "onRemoveLyrics",
        ),
        "SoundStudioTuningPolicyTest.kt": (
            "generatedCurvesRespectActualDeviceBandCenters",
            "neverRequestsOutOfRangeOrOverlyAggressiveBoosts",
            "rejectsStaleOrUnsafeCapabilities",
            "draftMustBeAppliedAndIsNotAClaimOfAudibleOutput",
        ),
        "AudioTransitionUiPolicyTest.kt": (
            "streamingNeverPromisesCrossfadeOrGapless",
            "offOnlyMentionsGaplessAsEligibilityNotGuarantee",
            "incompatiblePlaybackModesNeverClaimActiveTransition",
            "requestedOverlapIsBoundedAndNeedsNextVerifiedFile",
        ),
    }
    for file, tokens in required.items():
        missing = [token for token in tokens if token not in files[file]]
        if missing:
            raise AssertionError(file + ": missing " + repr(missing))
    service = files["PlaybackService.kt"]
    if "if (band in 0..4 && level in -1500..1500)" in service:
        raise AssertionError("Legacy fixed five-band EQ command was restored")
    lyric = files["StudioPlayerScreens.kt"]
    start = lyric.index('itemsIndexed(lyrics.cues,')
    end = lyric.index('                    Surface(\n                        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),', start)
    if "actions.seekTo(" in lyric[start:end]:
        raise AssertionError("Unfenced lyrics tap-to-seek was restored")
    if "Android audio effects could not be enabled" not in service:
        raise AssertionError("Platform acknowledgement on EQ loss was removed")


def main() -> None:
    cli = argparse.ArgumentParser()
    cli.add_argument("--self-test", action="store_true")
    args = cli.parse_args()
    sources = {
        "SoundStudioTuningPolicy.kt": PLAYBACK / "SoundStudioTuningPolicy.kt",
        "NightWaveSoundStudio.kt": UI / "NightWaveSoundStudio.kt",
        "PlaybackService.kt": PLAYBACK / "PlaybackService.kt",
        "AudioTransitionUiPolicy.kt": PLAYBACK / "AudioTransitionUiPolicy.kt",
        "StudioPlayerScreens.kt": UI / "StudioPlayerScreens.kt",
        "SoundStudioTuningPolicyTest.kt": TESTS / "SoundStudioTuningPolicyTest.kt",
        "AudioTransitionUiPolicyTest.kt": TESTS / "AudioTransitionUiPolicyTest.kt",
    }
    files = {k: v.read_text(encoding="utf-8") for k, v in sources.items()}
    verify(files)
    if args.self_test:
        def must_reject(file: str, old: str, replacement: str) -> None:
            if old not in files[file]:
                raise AssertionError("Fixture source token missing: " + old)
            broken = dict(files)
            broken[file] = broken[file].replace(old, replacement, 1)
            try:
                verify(broken)
            except AssertionError:
                return
            raise AssertionError("NW49 rejected a broken regression fixture")
        must_reject("StudioPlayerScreens.kt",
                    "actions.seekToForTrack(cueOriginTrack,", "actions.seekTo(")
        must_reject("PlaybackService.kt", "band in caps.centersHz.indices",
                    "band in 0..4")
        must_reject("SoundStudioTuningPolicy.kt",
                    "targetMb(shape, hz).coerceIn(range.first, range.last)",
                    "targetMb(shape, hz)")
        must_reject("NightWaveSoundStudio.kt",
                    "draftLevels = updated", "preview = updated")
        must_reject("AudioTransitionUiPolicy.kt",
                    "shuffled ->", "false ->")
        print("NW49 negative fixtures: PASS")
    print("NW49 guarded audio/lyrics source contracts: PASS")
    print("UNVERIFIED: Android compilation, JVM execution, audible EQ, phone lyrics.")


if __name__ == "__main__":
    main()
