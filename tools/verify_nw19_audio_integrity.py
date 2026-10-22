#!/usr/bin/env python3
"""NW19 source-only regression gate. Does NOT replace Android device QA."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
def read(path):
    return (root / path).read_text(encoding="utf-8")

frames = read("app/src/main/java/com/nightwave/player/offline/AudioFrameSignature.kt")
policy = read("app/src/main/java/com/nightwave/player/offline/PermittedAudioDownloadPolicy.kt")
downloader = read("app/src/main/java/com/nightwave/player/offline/PermittedAudioDownloader.kt")
library = read("app/src/main/java/com/nightwave/player/offline/OfflineAudioLibrary.kt")
screen = read("app/src/main/java/com/nightwave/player/offline/PermittedAudioDownloadActivity.kt")
unit = read("app/src/test/java/com/nightwave/player/offline/PermittedAudioDownloadPolicyTest.kt")
instrumented = read("app/src/androidTest/java/com/nightwave/player/offline/AuthorizedAudioImportIntegrityInstrumentedTest.kt")

assert "fun isMp3Frame(prefix: ByteArray)" in frames
assert "fun isAacAdts(prefix: ByteArray)" in frames
assert "bitrateIndex in 1..14" in frames
assert "frameBytes >= headerBytes" in frames
assert "AudioFrameSignature.isMp3Frame(firstBytes)" in policy
assert "AudioFrameSignature.isAacAdts(firstBytes)" in policy
assert "detectAudioExtension(" in policy
assert "uri.scheme.equals(\"https\", true)" in policy
assert "inspectAudioOnly(pending)" in downloader
assert "importPermittedStagedAudio(pending, fileName)" in downloader
assert "private fun audioSha256(file: File)" in library
assert "MessageDigest.isEqual(expectedSha256, audioSha256(pending))" in library
assert "MessageDigest.isEqual(expectedSha256, audioSha256(file))" in library
assert "return@synchronized existing" in library
assert "pending.renameTo(finalFile)" in library
assert 'onSuccess = { "Offline audio ready:' in screen
assert unit.count("@Test") >= 19
assert "rawMp3FrameIsNeverMisclassifiedAsAac" in unit
assert "aacAdtsIsNeverMisclassifiedAsMp3" in unit
assert "malformedAndTruncatedMpegHeadersAreRejected" in unit
assert instrumented.count("@Test") >= 2
assert "identicalDownloadedBytesReuseExistingSong" in instrumented
assert "sameSizeButDifferentBytesAreNotDeduplicated" in instrumented

for name, source in (
    ("signatures", frames), ("policy", policy), ("engine", downloader),
    ("library", library), ("unit", unit), ("instrumented", instrumented)
):
    imports = [line for line in source.splitlines() if line.startswith("import ")]
    assert len(imports) == len(set(imports)), "Duplicate imports in " + name

print("PASS: NW19 source-only audio signature, SHA-256 copy, dedup and tests")
print("UNVERIFIED: complete Android Gradle build, real Android downloader and device QA")
