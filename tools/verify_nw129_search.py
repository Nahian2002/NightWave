"""Current full-corpus/cancellation wiring guard. Device timing is separate."""
from pathlib import Path
import argparse

ROOT = Path(__file__).resolve().parents[1]
BASE = "app/src/main/java/com/nightwave/player/"


def verify(src):
    activity = src["v2/ui/NightWaveStudioActivity.kt"]
    caller = activity.split("StudioTab.SEARCH -> {", 1)[1].split("StudioTab.LIBRARY ->", 1)[0]
    for bad in (".take(350)", ".take(400)", ".take(25)", ".distinctBy"):
        assert bad not in caller, "Search caller drops membership: " + bad
    assert "rememberNightWaveLocalCorpus(" in caller and "corpus = localCorpus" in caller
    screen = activity.split("internal fun StudioSearch(", 1)[1].split("internal fun StudioLibrary(", 1)[0]
    for marker in ("LocalMusicQueryRunner.run(index, query)", "it.accepts(query, index)",
                   "savedMatches.take(savedVisible)", "offlineMatches.take(offlineVisible)",
                   "nw129-local-loading", "nw129-local-error", "sourceLabel = index?.origin(song.url)?.label()"):
        assert marker in screen, marker
    for bad in ("SavedTrackSearch.match(", "SavedCreatorBrowser.groups(", "MusicIdentity.creatorsFrom("):
        assert bad not in screen, "UI thread performs local preparation: " + bad
    corpus = src["v2/ui/NightWaveLocalSearchCorpus.kt"]
    for marker in ("withContext(Dispatchers.IO)", "MusicLibrary(app).searchSnapshot()",
                   "OfflineAudioLibrary(app).songs()", "registerOnSharedPreferenceChangeListener(listener)",
                   "unregisterOnSharedPreferenceChangeListener(listener)", "controller.invalidate()",
                   "val current = if (state.refresh == refresh)", "Lifecycle.State.STARTED"):
        assert marker in corpus, marker
    control = src["net/LocalMusicSearchController.kt"]
    for marker in ("requests.collectLatest", "withContext(Dispatchers.Default)", "task.ensureActive()",
                   "if (requests.value == request)", "mutable.value = LocalMusicCorpusState(next.revision",
                   "delay(120)", "fun close() { job.cancel() }"):
        assert marker in control, marker
    index = src["net/LocalMusicSearchIndex.kt"]
    assert "all.add(track)" in index and "saved.all(query, checkpoint)" in index
    assert "MAX_PUBLIC_REFERENCES = 15_600" in index
    assert "this.query == query && this.index === index" in index
    for body in (corpus, control, index):
        assert "startService(" not in body and ".edit()" not in body and "CatalogDownloadQueue.enqueue" not in body
    library = src["library/MusicLibrary.kt"]
    assert "prefs.all.filterKeys" in library and "LegacyLibrarySnapshotParser.parse(values)" in library


if __name__ == "__main__":
    parser = argparse.ArgumentParser(); parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    names = ("v2/ui/NightWaveStudioActivity.kt", "v2/ui/NightWaveLocalSearchCorpus.kt",
             "net/LocalMusicSearchController.kt", "net/LocalMusicSearchIndex.kt", "library/MusicLibrary.kt")
    src = {name: (ROOT / BASE / name).read_text(encoding="utf-8") for name in names}
    verify(src)
    if args.self_test:
        for name, old, bad in (
            (names[0], "corpus = localCorpus", "corpus = localCorpus.take(350)"),
            (names[0], "it.accepts(query, index)", "true"),
            (names[1], "unregisterOnSharedPreferenceChangeListener(listener)", "keepOldListener(listener)"),
            (names[2], "if (requests.value == request)", "if (true)"),
            (names[3], "all.add(track)", "all.addAll(listOf(track).distinctBy { it.url }) // .edit()"),
        ):
            broken = dict(src); assert old in broken[name]
            broken[name] = broken[name].replace(old, bad)
            try: verify(broken)
            except AssertionError: continue
            raise AssertionError("Accepted a broken NW129 fixture: " + old)
    print("PASS NW129 complete caller, observed source, cancellation/revision and paging wiring; device proof NOT_RUN")
