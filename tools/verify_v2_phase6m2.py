#!/usr/bin/env python3
"""6M2 reviewed playlist-import source gate; NOT Android compilation."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
def src(relative):
    return (root / relative).read_text(encoding="utf-8")

base = "app/src/main/java/com/nightwave/player/"
parser = src(base + "v2/social/PlaylistShareParser.kt")
formatter = src(base + "v2/social/PlaylistShareFormatter.kt")
activity = src(base + "v2/ui/NightWaveStudioActivity.kt")
mutations = src(base + "library/LibraryMutationPlan.kt")
facade = src(base + "library/MusicLibrary.kt")
lane = src(base + "library/LegacyLibraryTransaction.kt")
parser_tests = src("app/src/test/java/com/nightwave/player/v2/social/PlaylistShareParserTest.kt")
mutation_tests = src("app/src/test/java/com/nightwave/player/library/SharedPlaylistImportTest.kt")
workflow = src(".github/workflows/android.yml")
gradle = src("app/build.gradle.kts")

assert 'const val PREFIX = "NightWave playlist: "' in parser
assert "const val MAX_TEXT_CHARS = 12_000" in parser
assert "VideoLink.normalize(url) != url" in parser
assert "if (rows.last() != FOOTER) return null" in parser
assert "if (shareable.isEmpty()) return null" in formatter
assert "raw.trimStart().startsWith(PlaylistShareParser.PREFIX)" in activity
assert "else showSharedPlaylistPreview(incoming)" in activity
assert "private fun promptSharedPlaylistPaste()" in activity
assert 'Text("Import shared playlist")' in activity
assert '.setPositiveButton("Import")' in activity
assert "library.importSharedPlaylist(incoming.name, incoming.tracks)" in activity
assert "LegacyLibraryTransaction.mutate(app, LibraryChange.ImportSharedPlaylist(name, tracks))" in facade
assert "data class ImportSharedPlaylist" in mutations
assert "operation.tracks.size !in 1..30" in mutations
assert "if (normalized.url != raw.url) return rejected()" in mutations
assert "clean.map { it.url }.toSet().size != clean.size" in mutations
assert "if (old.playlists.size >= 50) return rejected()" in mutations
assert "old.copy(playlists = old.playlists + (chosen to clean))" in mutations
assert "if (!editor.commit())" in lane and "if (!saved)" in lane
assert "tools/verify_v2_phase6m2.py" in workflow
assert "feature/nightwave-v2-phase6m1-safe-playlist-sharing" in workflow
assert 'applicationId = "com.nightwave.player"' in gradle
assert "versionCode = 35" in gradle
assert parser_tests.count("@Test") >= 12
assert mutation_tests.count("@Test") >= 10

for name, body in (("parser", parser), ("activity", activity), ("mutations", mutations),
                   ("parser-tests", parser_tests), ("mutation-tests", mutation_tests)):
    assert body.count("{") == body.count("}"), name
    imports = [ln for ln in body.splitlines() if ln.startswith("import ")]
    assert len(imports) == len(set(imports)), name

print("PASS: Phase 6M2 preview, strict parser and one-commit atomic playlist import preserved")
print("PENDING: full Android Gradle/JVM build, UI and physical-device QA")
