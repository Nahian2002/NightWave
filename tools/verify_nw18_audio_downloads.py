#!/usr/bin/env python3
"""NW18 source-only safety gate. Not a replacement for Android Gradle/device QA."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
def read(path: str) -> str:
    return (root / path).read_text(encoding="utf-8")

policy = read("app/src/main/java/com/nightwave/player/offline/PermittedAudioDownloadPolicy.kt")
engine = read("app/src/main/java/com/nightwave/player/offline/PermittedAudioDownloader.kt")
ui = read("app/src/main/java/com/nightwave/player/offline/PermittedAudioDownloadActivity.kt")
tests = read("app/src/test/java/com/nightwave/player/offline/PermittedAudioDownloadPolicyTest.kt")
library = read("app/src/main/java/com/nightwave/player/offline/OfflineAudioLibrary.kt")

assert 'uri.scheme.equals("https", true)' in policy
assert 'uri.userInfo == null' in policy and 'uri.rawFragment == null' in policy
assert all(host in policy for host in ("youtube.com", "googlevideo.com", "spotify.com"))
assert 'manifestExtensions' in policy and 'detectAudioExtension(' in policy
assert 'MAX_AUDIO_BYTES = 250L * 1024L * 1024L' in policy
assert 'audioSignatureMatches(' in policy and 'supportedContentType(' in policy
assert '.followRedirects(false)' in engine and '.followSslRedirects(false)' in engine
assert 'resolved.all(::isPublicAddress)' in engine
assert 'URI(url).resolve(target)' in engine
assert 'require(redirects < 4' in engine
assert 'repeat(3) { attempt ->' in engine
assert 'return@repeat' in engine and 'pending.delete()' in engine
assert 'inspectAudioOnly(pending)' in engine
assert 'MediaExtractor()' in engine and 'mime.startsWith("video/")' in engine
assert 'require(audioTracks > 0)' in engine
assert 'response.code == 200' in engine
assert 'received <= PermittedAudioDownloadPolicy.MAX_BYTES' in engine
assert 'importPermittedStagedAudio(pending, fileName)' in engine
assert 'synchronized(OfflineLibraryIndexLock.monitor)' in library
assert 'pending.renameTo(finalFile)' in library
assert 'onDestroy()' in ui and 'transfer?.cancel()' in ui
assert tests.count('@Test') >= 16
assert 'refusesToRelabelAnM4aAsMp3' in tests
assert 'extensionlessCdnLinkIsValidatedAfterTransfer' in tests

for name, data in (("policy", policy), ("engine", engine), ("ui", ui)):
    imports = [line for line in data.splitlines() if line.startswith('import ')]
    assert len(imports) == len(set(imports)), f"Duplicate imports in {name}"

print('PASS: NW18 bounded direct-audio source contracts and JUnit wiring')
print('UNVERIFIED: full Android compile, actual HTTPS transfer and Samsung playback')
