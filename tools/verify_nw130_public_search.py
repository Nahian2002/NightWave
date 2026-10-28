"""NW130 result evidence and per-search transport cancellation wiring; device QA is separate."""
from pathlib import Path
import argparse

ROOT = Path(__file__).resolve().parents[1]
BASE = "app/src/main/java/com/nightwave/player/"


def verify(src):
    activity = src["v2/ui/NightWaveStudioActivity.kt"]
    screen = activity.split("internal fun StudioSearch(", 1)[1].split("private fun StudioLibrary(", 1)[0]
    public = screen.split('} else if (responseMatchesQuery && state.videos.isNotEmpty())', 1)[1]
    assert public.count("publicEvidence = publicEvidence[song.url]") == 3
    for marker in ("PublicMusicResultEvidence.forResults(state.videos, mode)", "nw130-cancel-search",
                   "if (it != query && state.loading) onCancelSearch()", "Text(state.phase.label",
                   "onPlay(selected, listOf(selected))"):
        assert marker in screen, marker
    for marker in ("PublicMusicSearchRunner.run", "request == nativeSearchGeneration",
                   "request != nativeSearchGeneration", "catch (timeout: TimeoutCancellationException)",
                   "failure is PublicSearchDeadlineException", "onCancelSearch = { cancelNativeSearch() }"):
        assert marker in activity, marker
    runner = src["net/PublicMusicSearchRunner.kt"]
    for marker in ("withTimeout(timeoutMillis)", "withContext(Dispatchers.IO)", "request.scoped",
                   "finally { request.cancel() }", "calls.toList().forEach { it.cancel() }",
                   "ThreadLocal<PublicSearchRequest?>()", "finally { current.set(previous) }",
                   "call.execute().use", "calls.remove(call)", "TIMEOUT_MS = 20_000L"):
        assert marker in runner, marker
    assert ".cancelAll(" not in runner
    for name in ("net/ExtractorDownloader.kt", "net/CommunityVideoSearch.kt"):
        assert "PublicSearchRequest.executeHttp" in src[name], name
    gateway = src["net/YouTubeGateway.kt"]
    assert gateway.count("request?.checkActive()") >= 5
    assert "onPhase(PublicSearchPhase.COMMUNITY)" in gateway
    assert "if (mode == SearchMode.SONGS) sortMusicMatches" in gateway
    row = src["v2/ui/StudioComponents.kt"]
    for marker in ("publicEvidence.sourceAndDuration", "publicEvidence.titleLabels",
                   "publicEvidence.duplicateReference", "publicEvidence?.uploader",
                   "if (publicEvidence != null) Int.MAX_VALUE"):
        assert marker in row, marker
    evidence = src["net/PublicMusicResultEvidence.kt"]
    for marker in ("Duration unavailable", "Uploader:", "Title labels:", "Video ID:"):
        assert marker in evidence, marker
    for bad in (".edit()", "playback.play(", "CatalogDownloadQueue.enqueue"):
        assert bad not in runner and bad not in evidence


if __name__ == "__main__":
    parser = argparse.ArgumentParser(); parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    names = ("v2/ui/NightWaveStudioActivity.kt", "v2/ui/StudioComponents.kt",
             "net/PublicMusicResultEvidence.kt", "net/PublicMusicSearchRunner.kt",
             "net/ExtractorDownloader.kt", "net/CommunityVideoSearch.kt", "net/YouTubeGateway.kt")
    src = {name: (ROOT / BASE / name).read_text(encoding="utf-8") for name in names}
    verify(src)
    if args.self_test:
        for name, old, new in (
            (names[0], "publicEvidence = publicEvidence[song.url]", "publicEvidence = null"),
            (names[0], "onPlay(selected, listOf(selected))", "onPlay(selected, state.videos)"),
            (names[3], "finally { request.cancel() }", "finally { }"),
            (names[3], "calls.toList().forEach { it.cancel() }", "calls.clear()"),
            (names[4], "PublicSearchRequest.executeHttp", "unscopedHttp"),
            (names[5], "PublicSearchRequest.executeHttp", "unscopedHttp"),
            (names[6], "request?.checkActive()", "// skipped cancellation"),
        ):
            broken = dict(src); assert old in broken[name]
            broken[name] = broken[name].replace(old, new)
            try: verify(broken)
            except AssertionError: continue
            raise AssertionError("Accepted broken NW130 fixture: " + old)
    print("PASS NW130 public evidence, original selection, phases/deadline and request-scoped HTTP cancellation; device QA NOT_RUN")
