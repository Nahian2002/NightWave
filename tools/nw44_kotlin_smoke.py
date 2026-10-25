#!/usr/bin/env python3
"""Compile genuine NW44 pure Kotlin ranking files against synthetic model stubs.

This is a local policy smoke, NOT Android/Gradle/provider/device validation.
No network access or user data is used. Kotlin and Java are optional on CI;
if absent, return code 2 (SKIP), not a false pass.
"""
import argparse
from pathlib import Path
import re
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / "app/src/main/java/com/nightwave/player/net"
SOURCES = [
    BASE / "MusicIdentity.kt",
    BASE / "SearchRelevancePolicy.kt",
    BASE / "SearchMusicIntentPolicy.kt",
    BASE / "SearchResultPresentationPolicy.kt",
    BASE / "SavedTrackSearch.kt",
]
TESTS = ROOT / "app/src/test/java/com/nightwave/player/net/SearchMusicIntentPolicyTest.kt"
TRIPLE = re.compile(r'Triple\("([^"]+)", "([^"]+)", "([^"]+)"\)')


def fixture_lines():
    text = TESTS.read_text(encoding="utf-8")
    cases = TRIPLE.findall(text)
    if len(cases) != 40:
        raise ValueError(f"Expected 40 music search fixture triples, found {len(cases)}")
    # Source fixture literal strings are already quoted Kotlin strings. Only
    # extract completely matched triples from the checked-in JUnit test.
    return TRIPLE.findall(text)


def test_program(cases):
    def quote(value):
        return '"' + value.replace("\\", "\\\\").replace('"', '\\"') + '"'
    rows = ",\n".join("    Triple(" + ", ".join(quote(item) for item in case) + ")"
                      for case in cases)
    return r'''package com.nightwave.player.net

data class VideoPreview(
    val title: String, val author: String, val url: String,
    val thumbnail: String, val durationSeconds: Long
)
enum class SearchMode { SONGS, VIDEOS }

fun main() {
    val examples = listOf(
''' + rows + r'''
    )
    check(examples.size == 40)
    examples.forEachIndexed { i, (query, preferred, other) ->
        val good = VideoPreview(preferred, "Uploader", "https://www.youtube.com/watch?v=" +
            (1000 + i * 2).toString().padStart(11, '0'), "", 215)
        val decoy = VideoPreview(other, "Uploader", "https://www.youtube.com/watch?v=" +
            (1001 + i * 2).toString().padStart(11, '0'), "", 215)
        val ranked = SearchRelevancePolicy.rankPublic(query, listOf(decoy, good))
        check(ranked.first() == good) { "Ranking mismatch for $query" }
        check(ranked.toSet() == setOf(good, decoy))
    }
    check(SearchMusicIntentPolicy.canonical("Alone, Pt. II") == "alone 2")
    check(SearchMusicIntentPolicy.canonical("We Don't Talk Anymore") ==
        "we dont talk anymore")
    check(SearchMusicIntentPolicy.canonical("I Love You") == "i love you")
    val x = VideoPreview("Alone Pt II", "Uploader",
        "https://www.youtube.com/watch?v=12345678901", "", 210)
    val y = VideoPreview("Home Alone 2 Movie Trailer", "Uploader",
        "https://www.youtube.com/watch?v=12345678902", "", 210)
    val grouped = SearchResultPresentationPolicy.groupMusic("alone 2",
        SearchMode.SONGS, listOf(y, x))
    check((grouped.bestMatches + grouped.moreResults).toSet() == setOf(x, y))
    check(grouped.bestMatches.first() == x)
    val videos = SearchResultPresentationPolicy.groupMusic("alone 2",
        SearchMode.VIDEOS, listOf(y, x))
    check(videos.bestMatches.isEmpty() && videos.moreResults == listOf(y, x))
    println("PASS: 40 synthetic query comparisons + alias/grouping invariants")
    println("NOT VERIFIED: real providers, Android compile, Media3 playback")
}
'''


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    try:
        cases = fixture_lines()
    except (OSError, ValueError) as err:
        print("FAIL:", err)
        return 1
    program = test_program(cases)
    if args.self_test:
        assert "examples.size == 40" in program
        assert len(SOURCES) == 5
        print("PASS: NW44 Kotlin smoke fixture extraction self-test")
        return 0
    compiler, java = shutil.which("kotlinc"), shutil.which("java")
    if not compiler or not java:
        print("SKIP: standalone kotlinc/Java unavailable; Gradle build still required")
        return 2
    for source in SOURCES:
        if not source.is_file():
            print("FAIL: Missing production source", source)
            return 1
    with tempfile.TemporaryDirectory(prefix="nightwave-nw44-") as folder:
        path = Path(folder)
        harness = path / "NW44Smoke.kt"
        jar = path / "nw44.jar"
        harness.write_text(program, encoding="utf-8")
        for argv in [
            [compiler, *map(str, SOURCES), str(harness),
                "-include-runtime", "-d", str(jar)],
            [java, "-jar", str(jar)],
        ]:
            result = subprocess.run(argv, cwd=ROOT, capture_output=True,
                                    text=True, timeout=120)
            if result.returncode:
                print((result.stdout + result.stderr)[-6000:])
                return result.returncode
            if result.stdout.strip():
                print(result.stdout.strip())
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
