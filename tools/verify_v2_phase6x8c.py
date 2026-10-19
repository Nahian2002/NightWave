#!/usr/bin/env python3
"""6X8C: Media3 library contract and Android Auto compile-preflight.

Uses actual XML parsing and source contracts. This does not run Kotlin or Gradle,
and must never be reported as a successful Android build.
"""
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "app/src/main"
A = "{http://schemas.android.com/apk/res/android}"

def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")

svc = read("app/src/main/java/com/nightwave/player/playback/PlaybackService.kt")
catalog = read("app/src/main/java/com/nightwave/player/auto/AutoBrowseCatalog.kt")
policy = read("app/src/main/java/com/nightwave/player/auto/AutoBrowsePolicy.kt")
manifest = ET.parse(MAIN / "AndroidManifest.xml").getroot()
car = ET.parse(MAIN / "res/xml/automotive_app_desc.xml").getroot()
workflow = read(".github/workflows/android.yml")

assert "import androidx.media3.common.util.UnstableApi" in svc
assert "@UnstableApi\nclass PlaybackService : MediaLibraryService()" in svc, \
    "Media3 unstable APIs must be marked on the service directly"
assert "@OptIn(UnstableApi::class)" not in svc, \
    "Media3 UnstableApi is not a Kotlin RequiresOptIn marker"
assert "MediaLibrarySession.Builder(this, player," in svc
assert "override fun onGetLibraryRoot(" in svc
assert "override fun onGetChildren(" in svc
assert "override fun onGetItem(" in svc
assert "override fun onGetSearchResult(" in svc
assert "override fun onSearch(" in svc
assert "override fun onAddMediaItems(" in svc
assert "override fun onGetSession(" in svc
assert "onMediaButtonEvent(" in svc
assert "SessionError.ERROR_BAD_VALUE" in svc
assert "MediaLibrarySession? = mediaSession" in svc
assert "private var mediaSession: MediaLibrarySession? = null" in svc
assert "NightWaveWidgetController.COMMAND_NEXT" in svc
assert "setHandleAudioBecomingNoisy(true)" in svc
assert "SleepTimerPolicy.deadlineMs(" in svc
assert "CrossfadePolicy.gains(" in svc
assert "OfflineNormalizationPolicy.gain(" in svc

app = manifest.find("application")
assert app is not None
services = [x for x in app.findall("service")
            if x.get(A + "name") == ".playback.PlaybackService"]
assert len(services) == 1
service = services[0]
assert service.get(A + "exported") == "true"
assert service.get(A + "foregroundServiceType") == "mediaPlayback"
actions = [x.get(A + "name") for x in service.findall("intent-filter/action")]
assert "androidx.media3.session.MediaLibraryService" in actions
assert "android.media.browse.MediaBrowserService" in actions
assert car.tag == "automotiveApp"
assert any(x.get("name") == "media" for x in car.findall("uses"))

assert "fun selection(id: String): AutoPlaySelection?" in catalog
assert "if (local.fileFor(song) == null) return null" in catalog
assert "VideoLink.normalize(it.url) == it.url" in catalog
assert "fun search(query: String)" in catalog
assert "MAX_PAGE_SIZE = 100" in policy
assert "MAX_RESULTS = 150" in policy

# Prevent an old source-level result being mistaken for Android build success.
assert ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin" in workflow
assert "python3 tools/verify_v2_phase6x8b.py" in workflow
assert "python3 tools/verify_v2_phase6x8c.py" in workflow
assert "feature/nightwave-v2-phase6x8c-media3-compile-hardening" in workflow

for name, source in (("service", svc), ("catalog", catalog), ("policy", policy)):
    assert source.count("{") == source.count("}"), name + " unmatched braces"
    imports = [line for line in source.splitlines() if line.startswith("import ")]
    assert len(imports) == len(set(imports)), name + " duplicate imports"

print("PASS: 6X8C source/XML Media3 unstable-API annotation and one-player safety contract")
print("NOT VERIFIED: Android Gradle compile, Android Auto host, Play/Pause/Next, Bluetooth and signing")
