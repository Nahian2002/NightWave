#!/usr/bin/env python3
"""NW21 source-preflight only. Complete Gradle + Android device QA still required."""
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
def text(path):
    return (root / path).read_text(encoding="utf-8")

catalog = text("app/src/main/java/com/nightwave/player/offline/FreeAudioCatalogClient.kt")
policy = text("app/src/main/java/com/nightwave/player/offline/FreeAudioCatalogPolicy.kt")
integrity = text("app/src/main/java/com/nightwave/player/offline/FreeAudioIntegrity.kt")
engine = text("app/src/main/java/com/nightwave/player/offline/PermittedAudioDownloader.kt")
screen = text("app/src/main/java/com/nightwave/player/offline/FreeAudioCatalogActivity.kt")
offline = text("app/src/main/java/com/nightwave/player/offline/OfflineLibraryActivity.kt")
chrome = text("app/src/main/java/com/nightwave/player/v2/ui/NightWaveDiscoveryChrome.kt")
manifest = text("app/src/main/AndroidManifest.xml")
tests = text("app/src/test/java/com/nightwave/player/offline/FreeAudioIntegrityTest.kt")
catalog_tests = text("app/src/test/java/com/nightwave/player/offline/FreeAudioCatalogClientTest.kt")

assert 'iiprop", "url|size|mime|extmetadata|sha1"' in catalog
assert 'isValidSourceHash(sha1)' in catalog
assert 'sha1Base36: String' in policy
assert 'MessageDigest.getInstance("SHA-1")' in integrity
assert "sourceSha1Base36(file) == expected" in integrity
assert "expectedSourceSha1: String? = null" in engine
assert "expectedSourceBytes: Long? = null" in engine
assert "FreeAudioIntegrity.sourceMatches(pending, expectedSourceSha1)" in engine
assert "pending.length() == expectedSourceBytes" in engine
assert "inspectAudioOnly(pending)" in engine
assert "job.download(source, entry.sha1Base36, entry.bytes)" in screen
assert "PlaybackService.ACTION_PLAY_OFFLINE" in screen
assert "savedId = it.id" in screen
assert "screenWidthDp > 780" in screen
assert "dp(52)" in screen and "card.animate()" in screen
assert "FreeAudioCatalogActivity::class.java" in offline
assert "FreeAudioCatalogActivity::class.java" in chrome
assert 'nightwave-save-offline-search' in chrome
assert re.search(
    r'<activity\\s+android:name="\\.offline\\.FreeAudioCatalogActivity"'
    r'(?=[^>]*android:exported="false")[^>]*/>', manifest, flags=re.S
)
assert tests.count("@Test") >= 3 and catalog_tests.count("@Test") >= 4
for name, content in {
    "catalog": catalog, "policy": policy, "integrity": integrity,
    "engine": engine, "screen": screen, "offline": offline,
    "chrome": chrome
}.items():
    imports = [line for line in content.splitlines() if line.startswith("import ")]
    assert len(imports) == len(set(imports)), f"Duplicate import in {name}"
print("PASS: NW21 source contracts, licensed discovery, integrity and responsive download flow")
print("UNVERIFIED: Android Gradle compilation, live Commons API and phone tests")
