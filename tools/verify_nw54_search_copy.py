#!/usr/bin/env python3
"""NW54 read-only Search consumer-copy regression gate."""
from pathlib import Path
import argparse

ROOT = Path(__file__).resolve().parents[1]
UI = ROOT / "app/src/main/java/com/nightwave/player/v2/ui"
TEST = ROOT / "app/src/test/java/com/nightwave/player/v2/ui"


def verify(copy: str, studio: str, tests: str) -> None:
    for token in (
        "fun resultsTitle(mode: Mode)",
        "fun resultCount(count: Int)",
        "fun bestMatchesSubtitle()",
        "fun moreResultsSubtitle()",
        "fun savedCreatorNotice()",
        "fun loadingHint(hasLocal: Boolean)",
        "fun fallbackHint(hasLocal: Boolean)",
        "may differ from official artist pages",
    ):
        if token not in copy:
            raise AssertionError("Search copy missing: " + token)
    for token in (
        "SearchConsumerCopyPolicy.resultsTitle(",
        "SearchConsumerCopyPolicy.resultCount(state.videos.size)",
        "SearchConsumerCopyPolicy.bestMatchesSubtitle()",
        "SearchConsumerCopyPolicy.moreResultsSubtitle()",
        "SearchConsumerCopyPolicy.loadingHint(",
        "SearchConsumerCopyPolicy.fallbackHint(",
        "SearchConsumerCopyPolicy.savedCreatorNotice()",
        "onNativeSearch(entered, mode)",
        "onWebSearch(query.trim())",
        "onRecordSearch(entered)",
        "onPlay(selected, listOf(selected))",
        "SearchResultPresentationPolicy.matches(",
    ):
        if token not in studio:
            raise AssertionError("Search wiring missing: " + token)
    block_start = studio.index('item(key = "nw54-public-results-heading")')
    block_end = studio.index('if (state.error.isNotBlank()) {', block_start)
    if "state.provider" in studio[block_start:block_end]:
        raise AssertionError("Technical provider ID leaked into results heading")
    if "not verified artist identities" in studio:
        raise AssertionError("Developer copy leaked into normal Search results")
    for test in (
        "labelsStayReadableAndDoNotPresentSourceDomains",
        "localLibraryHintDoesNotClaimMissingSongsExist",
        "creatorMetadataCaveatRemainsHonestWithoutTechnicalLanguage",
    ):
        if test not in tests:
            raise AssertionError("Missing JVM regression " + test)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    docs = [
        (UI / "SearchConsumerCopyPolicy.kt").read_text(encoding="utf-8"),
        (UI / "NightWaveStudioActivity.kt").read_text(encoding="utf-8"),
        (TEST / "SearchConsumerCopyPolicyTest.kt").read_text(encoding="utf-8"),
    ]
    verify(*docs)
    if args.self_test:
        for index, old, new in (
            (0, "may differ from official artist pages", "are official artist pages"),
            (1, "SearchConsumerCopyPolicy.resultCount(state.videos.size)",
             'state.videos.size.toString() + state.provider'),
            (1, "onPlay(selected, listOf(selected))", "onPlay(selected, allResults)"),
            (2, "localLibraryHintDoesNotClaimMissingSongsExist", "neverAssertLocalResults"),
        ):
            broken = docs[:]
            assert old in broken[index]
            broken[index] = broken[index].replace(old, new, 1)
            try:
                verify(*broken)
            except AssertionError:
                continue
            raise AssertionError("NW54 guard missed regression: " + old)
        print("NW54 Search negative fixtures: PASS")
    print("NW54 Search UI source contracts: PASS")
    print("UNVERIFIED: actual provider latency, search relevance, image rendering.")


if __name__ == "__main__":
    main()
