"""NW133 presentation/image/seek caller contracts; Android execution remains separate."""
from pathlib import Path
import argparse

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / "app/src/main/java/com/nightwave/player/v2/ui"
NAMES = ("StudioPlayerScreens.kt", "NightWaveArtworkPolicy.kt", "NightWaveNowPlayingPresentationPolicy.kt", "NightWaveSeekTimeline.kt")

def verify(src):
    ui, art, policy, seek = (src[n] for n in NAMES)
    for marker in ("requestCandidates(url, large)", ".size(pixels, pixels)", "CachePolicy.ENABLED",
                   "isUnavailableLargeStill(displayUrl", "candidateIndex == attempt", "trackIdentity = artwork.trackId",
                   "large = true", "stateDescription = statusLabel", "playbackStatus(playerHealth)",
                   "NightWaveNowPlayingPresentationPolicy.canSeek(", "NightWaveNowPlayingPresentationPolicy.seekPosition(",
                   "actions.seekToForTrack(originatingTrack, it)", '"nightwave-now-playing-lyrics-action"',
                   '"nightwave-now-playing-queue-action"'):
        assert marker in ui, marker
    assert "actions.seekTo(" not in ui
    for marker in ("uri.rawUserInfo != null", "uri.rawQuery != null", "uri.rawFragment != null",
                   '"maxresdefault.jpg", "sddefault.jpg", "hqdefault.jpg"', "coerceIn(48, 1280)", "width in 1..120"):
        assert marker in art, marker
    for marker in ("expected != current", "!resolving", "error.isNullOrBlank()", "PlayerProgressRules.requestedPosition"):
        assert marker in policy, marker
    for marker in ("remember(trackIdentity)", "pointerInput(enabled, trackIdentity)", "if (!enabled) disabled()",
                   "commitCallback(trackIdentity, mapped)", "positionDescription?.let"):
        assert marker in seek, marker
    assert ui.count("playbackStatus(") == 2
    for bad in ("Http", ".edit()", "enqueue("):
        assert bad not in art and bad not in policy

if __name__ == "__main__":
    parser = argparse.ArgumentParser(); parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args(); src = {n: (BASE / n).read_text(encoding="utf-8") for n in NAMES}; verify(src)
    if args.self_test:
        for name, marker in ((NAMES[0], ".size(pixels, pixels)"), (NAMES[0], "candidateIndex == attempt"),
                             (NAMES[0], "actions.seekToForTrack(originatingTrack, it)"), (NAMES[0], "stateDescription = statusLabel"),
                             (NAMES[1], "uri.rawQuery != null"), (NAMES[2], "expected != current"),
                             (NAMES[3], "pointerInput(enabled, trackIdentity)")):
            broken = dict(src); broken[name] = broken[name].replace(marker, "BROKEN_FIXTURE")
            try: verify(broken)
            except AssertionError: continue
            raise AssertionError("Accepted broken fixture: " + marker)
    print("PASS NW133 source-bound artwork, fallback, status and identity-scoped seek wiring; device NOT_RUN")
