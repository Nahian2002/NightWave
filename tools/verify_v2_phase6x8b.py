#!/usr/bin/env python3
"""Phase 6X8B manifest, car-browse privacy and playback continuity gate.

Parses real Android resources. This does NOT substitute for Gradle or car tests.
"""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
def read(path):
    return (root / path).read_text(encoding="utf-8")
app = "app/src/main/"
src = app + "java/com/nightwave/player/"
A = "{http://schemas.android.com/apk/res/android}"
manifest = ET.fromstring(read(app + "AndroidManifest.xml"))
descriptor = ET.fromstring(read(app + "res/xml/automotive_app_desc.xml"))
assert descriptor.tag == "automotiveApp"
assert any(e.get("name") == "media" for e in descriptor.findall("uses"))
application = manifest.find("application")
assert application is not None
metadata = [e for e in application.findall("meta-data")
            if e.get(A+"name") == "com.google.android.gms.car.application"]
assert len(metadata) == 1
assert metadata[0].get(A+"resource") == "@xml/automotive_app_desc"
services = [e for e in application.findall("service")
            if e.get(A+"name") == ".playback.PlaybackService"]
assert len(services) == 1, "Android Auto must not create a second playback service"
service_xml = services[0]
assert service_xml.get(A+"foregroundServiceType") == "mediaPlayback"
assert service_xml.get(A+"exported") == "true"
actions = [e.get(A+"name") for e in service_xml.findall("intent-filter/action")]
assert "androidx.media3.session.MediaLibraryService" in actions
assert "android.media.browse.MediaBrowserService" in actions
assert "androidx.media3.session.MediaSessionService" not in actions

policy = read(src + "auto/AutoBrowsePolicy.kt")
catalog = read(src + "auto/AutoBrowseCatalog.kt")
service = read(src + "playback/PlaybackService.kt")
widget = read(src + "widget/NightWaveWidgetController.kt")
old_gate = read("tools/verify_v2_phase6x8.py")
baseline_gate = read("tools/verify_source.py")
tests = read("app/src/test/java/com/nightwave/player/auto/AutoBrowsePolicyTest.kt")
instrumented = read("app/src/androidTest/java/com/nightwave/player/auto/NightWaveAutoBrowserIntegrationTest.kt")
workflow = read(".github/workflows/android.yml")
assert "MAX_PAGE_SIZE = 100" in policy
assert "MAX_RESULTS = 150" in policy
assert "Base64.getUrlEncoder().withoutPadding()" in policy
assert "fun offlineTarget(" in policy
assert "fun publicTarget(" in policy
assert "fun <T> page(" in policy
assert "page.toLong() * pageSize" in policy
assert "fun children(parent: String): List<MediaItem>?" in catalog
assert "AutoBrowsePolicy.ROOT -> listOf(" in catalog
assert "AutoBrowsePolicy.IMPORTED -> localSongs().map(::localItem)" in catalog
assert "VideoLink.normalize(it.url) == it.url" in catalog
assert "if (local.fileFor(song) == null) return null" in catalog
assert "val chosen = songs.indexOfFirst { it.url == url }" in catalog
assert "fun search(query: String)" in catalog
assert "sources.distinctBy { it.mediaId }" in catalog
assert "MediaLibrarySession.Builder(this, player," in service
assert "class PlaybackService : MediaLibraryService()" in service
assert "override fun onGetSession(" in service
assert "override fun onGetLibraryRoot(" in service
assert "override fun onGetChildren(" in service
assert "override fun onGetItem(" in service
assert "override fun onSearch(" in service
assert "override fun onGetSearchResult(" in service
assert "override fun onAddMediaItems(" in service
assert "AutoPlaySelection.Offline" in service
assert "AutoPlaySelection.Streaming" in service
assert "queue.useExplicitDisplayOrder()" in service
assert "playOfflineId(selection.songId, local)" in service
assert "queue.current?.let { resolveAndPlay(it) }" in service
assert "Futures.immediateFuture(emptyList())" in service
assert "override fun onMediaButtonEvent(" in service
assert "MediaLibrarySession? = mediaSession" in service
assert "NightWaveWidgetController.COMMAND_NEXT" in service
assert "setHandleAudioBecomingNoisy(true)" in service
assert "MediaLibraryService" in old_gate
assert "MediaLibraryService" in baseline_gate
assert "SessionToken(app, ComponentName(app, PlaybackService::class.java))" in widget
assert tests.count("@Test fun ") >= 9
assert instrumented.count("@Test fun ") >= 2
assert "feature/nightwave-v2-phase6x8b-auto-library-build-hardening" in workflow
assert "python3 tools/verify_v2_phase6x8b.py" in workflow
assert "python3 tools/verify_v2_phase6x8.py" in workflow
assert ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin" in workflow
for title, value in (("policy",policy),("catalog",catalog),
                     ("service",service),("tests",tests),("android",instrumented)):
    assert value.count("{") == value.count("}"), title + " broken Kotlin braces"
    imports = [x for x in value.splitlines() if x.startswith("import ")]
    assert len(imports) == len(set(imports)), title + " duplicate imports"
print("PASS: Android Auto MediaLibraryService single-player browse/selection source contracts")
print("PENDING: complete Android Gradle build and AAOS/Android Auto emulator playback verification")
