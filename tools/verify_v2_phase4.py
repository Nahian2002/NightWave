#!/usr/bin/env python3
"""NightWave Phase 4 static wiring gate; does not replace Android Gradle/device tests."""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
code = root / "app/src/main/java/com/nightwave/player"
read = lambda relative: (code / relative).read_text(encoding="utf-8")

settings = (root / "app/build.gradle.kts").read_text()
manifest = ET.parse(root / "app/src/main/AndroidManifest.xml").getroot()
playback = read("playback/PlaybackService.kt")
queue = read("playback/QueueStateStore.kt")
bridge = read("v2/playback/StudioPlaybackBridge.kt")
studio = read("v2/ui/NightWaveStudioActivity.kt")
player = read("v2/ui/StudioPlayerScreens.kt")
components = read("v2/ui/StudioComponents.kt")
design = read("v2/model/NightWaveDesign.kt")
classic = read("ui/MainActivity.kt")

assert 'versionCode = 24' in settings
assert 'versionName = "2.0.0-alpha5h"' in settings
assert 'applicationId = "com.nightwave.player"' in settings
android_name = "{http://schemas.android.com/apk/res/android}name"
activities = {x.attrib.get(android_name): x for x in manifest.iter("activity")}
assert ".ui.MainActivity" in activities
assert ".v2.ui.NightWaveStudioActivity" in activities
def intents(activity):
    return [
        {item.attrib.get(android_name) for item in intent.iter("action")} |
        {item.attrib.get(android_name) for item in intent.iter("category")}
        for intent in activity.findall("intent-filter")
    ]
# Runtime hotfix: known-functional classic UI launches first, Compose remains opt-in.
assert any("android.intent.action.MAIN" in intent and
           "android.intent.category.LAUNCHER" in intent
           for intent in intents(activities[".ui.MainActivity"]))
assert not any("android.intent.category.LAUNCHER" in intent
               for intent in intents(activities[".v2.ui.NightWaveStudioActivity"]))
assert "NightWaveStudioActivity::class.java" in classic
assert "Intent(this, MainActivity::class.java)" in playback
assert any("android.intent.action.SEND" in intent
           for intent in intents(activities[".ui.MainActivity"]))

for string in ("saveBookmark(", "fun bookmark(", "bookmark_position_ms",
               "bookmark_duration_ms"):
    assert string in queue
assert 'savedQueue.restore(queue)' in playback
assert 'autoPlay = false' in playback and 'resumePositionMs = savedQueue.bookmark' in playback
assert 'ACTION_RESUME_SAVED' in playback
assert 'main.postDelayed(progressWriter, 2500L)' in playback
assert 'main.removeCallbacks(progressWriter)' in playback
assert 'persistProgress()' in playback
assert 'playWhenReady = autoPlay' in playback
assert 'setSessionActivity(openApp)' in playback
assert 'NightWaveStudioActivity::class.java' in playback
assert 'savedTrack?.url.orEmpty()' in bridge
assert 'savedBookmark?.positionMs' in bridge
assert 'PlaybackService.ACTION_RESUME_SAVED' in bridge
assert 'PlaybackService.ACTION_RESUME_SAVED' in classic

assert '"NightWave"' in components and '"nightwave."' not in components
assert 'height(164.dp)' in components
assert 'Modifier.size(52.dp)' in components
assert 'height(58.dp)' in player and 'height(2.dp)' in player
assert 'onHide: () -> Unit' in player
assert 'if (travel > 70f) onHide()' in player
assert 'if (dragged > 65f) onDismiss()' in player
assert 'maxHeight * .34f, 330.dp' in player
# Two Material3 Surface calls in the compact player/nav require the import:
# without it Kotlin reports unresolved Surface and cascading @Composable errors.
assert studio.count("Surface(") >= 2
assert "import androidx.compose.material3.Surface" in studio
assert 'height(60.dp)' in studio
assert 'Now playing · Tap to show' in studio
assert 'showAdvanced' in studio
assert 'BackHandler(enabled = showQueue || expanded)' in studio
assert 'onClose = {' in studio and 'MainActivity::class.java' in studio
assert 'ROOM_PRIMARY_WRITES_ENABLED = false' in read("library/LibraryStoragePolicy.kt")
assert 'MINI_PLAYER_HEIGHT_DP = 60' in design

tests = (root / "app/src/androidTest/java/com/nightwave/player/playback/QueueStateBookmarkInstrumentedTest.kt").read_text()
assert tests.count("@Test") >= 5

for title, source in (("service", playback), ("queue", queue), ("bridge", bridge),
                      ("studio", studio), ("player", player), ("components", components)):
    assert source.count("{") == source.count("}"), f"{title}: mismatched braces"
    imports = [line for line in source.splitlines() if line.startswith("import ")]
    assert len(imports) == len(set(imports)), f"{title}: repeated import"

print("PASS: safe classic launcher, opt-in Compose preview, compact player/nav, gestures,")
print("      explicit classic fallback, queued session/bookmark resume and tests")
print("NOT VERIFIED: Gradle/Compose build, instrumental tests or phone playback")
