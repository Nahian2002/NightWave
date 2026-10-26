#!/usr/bin/env python3
"""NW108 exact wire audit for deterministic playlist backup identity."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
read=lambda path:(root/path).read_text(encoding="utf-8")
policy=read("app/src/main/java/com/nightwave/player/v2/data/PlaylistNameIdentity.kt")
codec=read("app/src/main/java/com/nightwave/player/v2/data/LibraryBackupCodec.kt")
merger=read("app/src/main/java/com/nightwave/player/v2/cloud/CloudLibraryMerger.kt")
recovery=read("app/src/main/java/com/nightwave/player/v2/cloud/CloudRecoveryPolicy.kt")
test=read("app/src/test/java/com/nightwave/player/v2/data/PlaylistNameIdentityTest.kt")
ci=read(".github/workflows/android.yml")
assert "first.equals(second, ignoreCase = true)" in policy
assert "fun contains(names: Iterable<String>, candidate: String)" in policy
assert "fun same(first: String, second: String)" in policy
assert "fun allDistinct(names: List<String>)" in policy
assert codec.count("PlaylistNameIdentity.allDistinct(")==2
assert not codec.count("it.first.lowercase()")
assert merger.count("PlaylistNameIdentity.contains(")>=2
assert merger.count("PlaylistNameIdentity.same(")>=2
assert "val names = local.playlists.map { it.first }.toMutableList()" in merger
assert "PlaylistNameIdentity.same(it.first, name)" in recovery
assert "remotePlaylists.firstOrNull" in recovery
assert "lowercase(Locale.ROOT)" not in recovery
assert "PlaylistNameIdentity.key(" not in merger
assert test.count("@Test fun ")==6
assert "tools/NW108PlaylistNameSmoke.kt" in ci
assert "python3 tools/nw108_name_junit_gate.py" in ci
assert "python3 tools/verify_nw108_playlist_identity.py" in ci
assert "NW102 compile and run original Kotlin JUnit source tests" in ci
print("NW108_BACKUP_PLAYLIST_IDENTITY_WIRING_PASS checks=16")
