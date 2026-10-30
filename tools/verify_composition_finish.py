"""DESIGN05 caller contracts; source verification does not certify rendered/device quality."""
from pathlib import Path
import argparse

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / "app/src/main/java/com/nightwave/player/v2/ui"
FILES = ("NightWaveStudioActivity.kt", "NightWaveEditorialPresentationPolicy.kt",
         "NightWaveVisualLanguage.kt", "NightWaveLibraryOverview.kt", "NightWavePremiumChrome.kt")


def verify(src):
    activity, policy, visual, overview, chrome = (src[n] for n in FILES)
    library = activity.split("internal fun StudioLibrary(", 1)[1].split("private fun StudioProfile(", 1)[0]
    profile = activity.split("private fun StudioProfile(", 1)[1].split("private fun StudioProfileGroup(", 1)[0]
    for marker in ("NightWaveLibraryOverviewHeader(overview.summary)", "NightWaveLibraryStartCard(hasHistory = overview.historyEntries > 0, onFindMusic = onSearch)",
                   "var automaticCollectionsOpen by rememberSaveable { mutableStateOf(true) }",
                   "if (automaticCollectionsOpen) items(smartCollections", "onSelectPlaylist(collection.id)"):
        assert marker in library, marker
    assert "Text(overview.summary" not in library, "Duplicate summary outside header"
    for marker in ('NightWaveQuickActionPill(strings.get(R.string.nw141_account_backup_008a1)', 'NightWaveQuickActionPill(strings.get(R.string.nw141_offline_music_a8f6a)',
                   'Icons.Outlined.GraphicEq, onEqualizer,\n                    modifier = Modifier.testTag("nw47-you-sound")',
                   'Icons.Outlined.LibraryMusic, onManageHistory,\n                    modifier = Modifier.testTag("nw47-you-history")'):
        assert marker in profile, marker
    assert profile.count("NightWaveQuickActionPill(") == 2, "Duplicate You destinations"
    assert 'Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))' in profile
    assert 'testTag("composition-library-find")' in overview and "onClick = onFindMusic" in overview
    for marker in ("parts.size >= 4", "creatorKey(parts.last()) == creatorKey(base.artistOrCreator)",
                   "!promotion.containsMatchIn(title) && !sourceCredits", "edition.containsMatchIn(it)",
                   "completeDuration(tracks)", "durationSeconds in 1..604800"):
        assert marker in policy, marker
    assert "NightWaveEditorialPresentationPolicy.mixSummary(mix)" in visual
    assert "NightWaveEditorialPresentationPolicy.mixFacts(mix)" not in visual
    assert "contentDescription = trackLabel" in visual
    assert "NightWaveEditorialPresentationPolicy.identity(it.title, it.author)" in chrome
    assert "maxLines = if (fontScale >= 1.3f) Int.MAX_VALUE else 2" in chrome


if __name__ == "__main__":
    parser = argparse.ArgumentParser(); parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args(); src = {n: (BASE / n).read_text(encoding="utf-8") for n in FILES}
    verify(src)
    if args.self_test:
        for name, marker in ((FILES[0], "NightWaveLibraryOverviewHeader(overview.summary)"),
                             (FILES[0], "NightWaveLibraryStartCard(hasHistory = overview.historyEntries > 0, onFindMusic = onSearch)"),
                             (FILES[0], "var automaticCollectionsOpen by rememberSaveable { mutableStateOf(true) }"),
                             (FILES[0], 'Modifier.testTag("nw47-you-sound")'),
                             (FILES[0], 'Modifier.testTag("nw47-you-history")'),
                             (FILES[1], "creatorKey(parts.last()) == creatorKey(base.artistOrCreator)"),
                             (FILES[1], "edition.containsMatchIn(it)"),
                             (FILES[2], "NightWaveEditorialPresentationPolicy.mixSummary(mix)"),
                             (FILES[3], "onClick = onFindMusic"),
                             (FILES[4], "maxLines = if (fontScale >= 1.3f) Int.MAX_VALUE else 2")):
            broken = dict(src); assert marker in broken[name]
            broken[name] = broken[name].replace(marker, "BROKEN_FIXTURE")
            try: verify(broken)
            except AssertionError: continue
            raise AssertionError("Accepted broken composition caller: " + marker)
    print("PASS composition title/edition safety, concise mix facts, sparse Library routes and consolidated You actions; device NOT_RUN")
