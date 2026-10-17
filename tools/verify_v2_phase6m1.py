#!/usr/bin/env python3
"""6M1 source and preservation checks, not Android compile/runtime."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
def src(p): return (root / p).read_text(encoding="utf-8")
base = "app/src/main/java/com/nightwave/player/"
share = src(base + "v2/social/PlaylistShareFormatter.kt")
activity = src(base + "v2/ui/NightWaveStudioActivity.kt")
tests = src("app/src/test/java/com/nightwave/player/v2/social/PlaylistShareFormatterTest.kt")
workflow = src(".github/workflows/android.yml")
gradle = src("app/build.gradle.kts")
assert "VideoLink.normalize(video.url) != video.url" in share
assert "tracks.take(MAX_INPUT_TRACKS)" in share
assert "MAX_LINKS = 30" in share
assert ".distinctBy { it.url }.take(MAX_LINKS)" in share
assert "if (shareable.isEmpty()) return null" in share
assert "onClick = { onSharePlaylist(selectedPlaylist, tracks) }" in activity
assert "selectedPlaylist != LIKED_COLLECTION && tracks.isNotEmpty()" in activity
assert "Intent.createChooser(Intent(Intent.ACTION_SEND)" in activity
assert "PlaylistShareFormatter.format(name, tracks)" in activity
assert "fun showCloudMergePreview(" in activity
assert 'applicationId = "com.nightwave.player"' in gradle
assert "versionCode = 35" in gradle
assert "tools/verify_v2_phase6m1.py" in workflow
assert "feature/nightwave-v2-phase6l-diverse-personal-mixes" in workflow
assert tests.count("@Test") >= 10
for name, text in (("share", share), ("activity", activity), ("tests", tests)):
    assert text.count("{") == text.count("}"), name
    imports = [ln for ln in text.splitlines() if ln.startswith("import ")]
    assert len(imports) == len(set(imports)), name
print("PASS: explicit, bounded playlist sharing; local/private sources excluded")
print("PENDING: Android compile/JVM suite and device Sharesheet verification")
