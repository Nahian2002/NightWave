#!/usr/bin/env python3
"""NW44 deterministic Smart Search source wiring (NOT a Kotlin/Android compile)."""
import argparse
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
NET = "app/src/main/java/com/nightwave/player/net/"
UI = "app/src/main/java/com/nightwave/player/v2/ui/"
FILES = {
    "intent": NET + "SearchMusicIntentPolicy.kt",
    "rank": NET + "SearchRelevancePolicy.kt",
    "group": NET + "SearchResultPresentationPolicy.kt",
    "gateway": NET + "YouTubeGateway.kt",
    "ui": UI + "NightWaveStudioActivity.kt",
    "tests": "app/src/test/java/com/nightwave/player/net/SearchMusicIntentPolicyTest.kt",
    "queue": "tools/verify_nw43_queue.py",
}

REQUIRED = {
    "intent": [
        "object SearchMusicIntentPolicy",
        "fun canonical(raw: String)",
        "fun titleCandidates(track: VideoPreview)",
        "fun variantPenalty(query: String, track: VideoPreview)",
        "fun titleSignal(query: String, track: VideoPreview)",
    ],
    "rank": [
        "SearchMusicIntentPolicy.canonical(query.text)",
        "SearchMusicIntentPolicy.titleSignal(wanted, track)",
        "SearchMusicIntentPolicy.variantPenalty(wanted, track)",
        ".thenBy { it.index }",
    ],
    "group": [
        "data class Groups(",
        "fun groupMusic(query: String, mode: SearchMode, videos: List<VideoPreview>)",
        "mode != SearchMode.SONGS",
        "ranked.drop(best.size)",
    ],
    "gateway": [
        "if (mode == SearchMode.SONGS) sortMusicMatches(videos, text) else videos",
        "SearchRelevancePolicy.rankPublic(query, videos)",
    ],
    "ui": [
        "SearchResultPresentationPolicy.groupMusic(",
        "publicGroups.bestMatches",
        "publicGroups.moreResults",
        'key = "public:" + it.url',
        "onPlay(selected, listOf(selected))",
        "playSinglePublic(song)",
        'label = { Text("Songs") }',
        'label = { Text("Videos") }',
    ],
    "queue": [
        "search.count(\"onPlay(selected, listOf(selected))\") != 1",
        "search.count(\"playSinglePublic(song)\") != 3",
    ],
    "tests": [
        "assertEquals(40, cases.size)",
        "partNumberAliasesAreContextualNotBroadRomanNumeralSubstitution",
        "explicitVariantIntentDoesNotPenalizeTheRequestedVersion",
        "resultsRemainPublicMetadataUnalteredAndNoneAreHidden",
    ]
}

def audit(files):
    issues = []
    for key, tokens in REQUIRED.items():
        raw = files.get(key, "")
        for token in tokens:
            if token not in raw:
                issues.append(f"{key}: missing {token!r}")
    ui = files.get("ui", "")
    start = ui.find("private fun StudioSearch(")
    end = ui.find("private fun StudioLibrary(", start)
    search = ui[start:end] if start >= 0 and end > start else ""
    if not search:
        issues.append("Search function boundary missing")
    for bad in (
        "onPlay(song, state.videos)", "onPlay(song, savedMatches)",
        "onPlay(song, savedTracks)", "onPlay(song, selectedCreatorGroup.tracks)"
    ):
        if bad in search:
            issues.append("Search bulk playback regression: " + bad)
    if search.count("onPlay(song, listOf(song))") != 3:
        issues.append("Three local/creator Search tap paths must remain single song")
    if search.count("onPlay(selected, listOf(selected))") != 1:
        issues.append("Public Search must use one single-song playback callback")
    if search.count("playSinglePublic(song)") != 3:
        issues.append("All three public groups must use the same single-song callback")
    if "quickOptionsExpanded" in search:
        issues.append("Old duplicate advanced search controls returned")
    if files.get("tests", "").count("Triple(") < 40:
        issues.append("40-query fixture set is missing")
    return issues

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    if args.self_test:
        fake = {k: "\n".join(v) for k, v in REQUIRED.items()}
        fake["ui"] = (
            "private fun StudioSearch(\n" +
            "onPlay(song, listOf(song))\n" * 3 +
            "onPlay(selected, listOf(selected))\n" +
            "playSinglePublic(song)\n" * 3 +
            "\nprivate fun StudioLibrary(\n" + fake["ui"]
        )
        fake["tests"] += "\nTriple(\"q\", \"a\", \"b\")" * 40
        assert not audit(fake), audit(fake)
        bad = dict(fake)
        bad["ui"] = bad["ui"].replace("playSinglePublic(song)",
            "onPlay(song, state.videos)", 1)
        assert audit(bad)
        print("PASS: NW44 source gate rejects bulk queue regression")
        return 0
    files = {}
    for key, rel in FILES.items():
        path = args.root / rel
        if not path.is_file():
            print("FAIL: missing", rel)
            return 1
        files[key] = path.read_text(encoding="utf-8")
    issues = audit(files)
    for item in issues:
        print("FAIL:", item)
    if issues:
        return 1
    print("PASS: NW44 deterministic ranking, result grouping and NW43 queue guard")
    print("NOT VERIFIED: Kotlin/Gradle execution or real provider relevance")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
