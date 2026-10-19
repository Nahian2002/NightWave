#!/usr/bin/env python3
"""Phase 6X8 source, Android resource XML and security integration verification.

This is a *real XML parse and source assertion* gate, but does not replace
Gradle Kotlin compilation or actual launcher/Bluetooth/headset device tests.
"""
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
APP = "app/src/main/"
JAVA = APP + "java/com/nightwave/player/"
A = "{http://schemas.android.com/apk/res/android}"

def read(path):
    return (ROOT / path).read_text(encoding="utf-8")
def xml(path):
    return ET.fromstring(read(path))

manifest = xml(APP + "AndroidManifest.xml")
widget_info = xml(APP + "res/xml/nightwave_widget_info.xml")
layout = xml(APP + "res/layout/nightwave_playback_widget.xml")
xml(APP + "res/drawable/nightwave_widget_surface.xml")
for name in ("previous", "next", "play", "pause"):
    root = xml(APP + f"res/drawable/ic_nw_widget_{name}.xml")
    assert root.tag == "vector"

strings = xml(APP + "res/values/strings.xml")
assert any(tag.get("name") == "widget_description" for tag in strings)

provider = read(JAVA + "widget/NightWaveWidgetProvider.kt")
receiver = read(JAVA + "widget/NightWaveWidgetActionReceiver.kt")
controller = read(JAVA + "widget/NightWaveWidgetController.kt")
policy = read(JAVA + "widget/NowPlayingWidgetPolicy.kt")
store = read(JAVA + "widget/NowPlayingWidgetStore.kt")
service = read(JAVA + "playback/PlaybackService.kt")
build = read("app/build.gradle.kts")
unit = read("app/src/test/java/com/nightwave/player/widget/NowPlayingWidgetPolicyTest.kt")
device = read("app/src/androidTest/java/com/nightwave/player/widget/NightWaveWidgetIntegrationTest.kt")
ci = read(".github/workflows/android.yml")

app = manifest.find("application")
assert app is not None
widget = next(x for x in app.findall("receiver")
              if x.get(A+"name") == ".widget.NightWaveWidgetProvider")
controls = next(x for x in app.findall("receiver")
                if x.get(A+"name") == ".widget.NightWaveWidgetActionReceiver")
assert widget.get(A+"exported") == "true"
assert controls.get(A+"exported") == "false"
assert widget.find("intent-filter/action").get(A+"name") ==        "android.appwidget.action.APPWIDGET_UPDATE"
assert widget.find("meta-data").get(A+"resource") == "@xml/nightwave_widget_info"
assert widget_info.get(A+"initialLayout") == "@layout/nightwave_playback_widget"
assert widget_info.get(A+"updatePeriodMillis") == "0"
assert int(widget_info.get(A+"targetCellHeight")) >= 3
ids = {item.get(A+"id") for item in layout.iter() if item.get(A+"id")}
required = {"@+id/widget_open", "@+id/widget_track", "@+id/widget_artist",
            "@+id/widget_previous", "@+id/widget_play_pause", "@+id/widget_next"}
assert required <= ids, required - ids

assert "NowPlayingWidgetPolicy.normalized" in provider
assert "NowPlayingWidgetStore(app).write(safe)" in provider
assert "if (ids.isEmpty()) return" in provider
assert "NightWaveWidgetActionReceiver::class.java" in provider
assert "PendingIntent.FLAG_IMMUTABLE" in provider
assert "Intent.FLAG_ACTIVITY_NEW_TASK" in provider
assert "fun onReceive(" not in provider  # exported provider must be display-only
assert "goAsync()" in receiver
assert "NightWaveWidgetController.perform(" in receiver
assert "private" in receiver or "NightWave" in receiver

assert "MediaController.Builder(app, token).buildAsync()" in controller
assert "SessionToken(app, ComponentName(app, PlaybackService::class.java))" in controller
assert "7_000L" in controller
assert "AtomicBoolean(false)" in controller
assert "controller?.release()" in controller
assert "runCatching { finished() }" in controller
assert "connected.currentMediaItem != null" in controller
assert "sendCustomCommand(" in controller
assert "context.startForegroundService" not in controller
assert "startService(" not in controller

assert "MAX_TITLE = 96" in policy and "MAX_ARTIST = 80" in policy
assert "Character.isISOControl" in policy
assert 'getSharedPreferences(' in store
assert 'nightwave_widget_state_v1' in store
for forbidden in ("audioUrl", "MediaItem", "password", "token", "filepath", "filePath"):
    assert forbidden not in store

assert "NightWaveWidgetController.COMMAND_NEXT" in service
assert "NightWaveWidgetController.COMMAND_PREVIOUS" in service
assert "controller.packageName != packageName" in service
assert "playNext(false)" in service and "playPrevious()" in service
assert "override fun onCustomCommand(" in service
assert "override fun onMediaButtonEvent(" in service
assert service.count("setHandleAudioBecomingNoisy(true)") >= 2
assert "publishWidgetState()" in service
assert "onMediaMetadataChanged(" in service
assert "override fun onIsPlayingChanged(" in service
assert "override fun onMediaItemTransition(" in service
assert "NowPlayingWidgetProvider.publish(this, WidgetPlaybackSnapshot())" in service
assert "MediaSession.Builder(this, player)" in service  # existing session preserved
assert "class PlaybackService : MediaSessionService()" in service

assert "com.google.guava:guava:" in build
assert "androidx.media3:media3-session:1.6.1" in build
assert unit.count("@Test fun ") >= 8
assert device.count("@Test fun ") >= 3
assert "python3 tools/verify_v2_phase6x8.py" in ci
assert "feature/nightwave-v2-phase6x8-android-widget-device-controls" in ci
assert "python3 tools/verify_v2_phase6x7.py" in ci
for title, source in (("provider", provider), ("receiver",receiver),
        ("controller",controller), ("policy",policy), ("store",store),
        ("service",service),("unit",unit),("device",device)):
    assert source.count("{") == source.count("}"), title + " unbalanced braces"
    imports = [ln for ln in source.splitlines() if ln.startswith("import ")]
    assert len(imports) == len(set(imports)), title + " duplicate imports"

print("PASS: 6X8 XML, protected receiver, session transport, widget state and no-auto-start gates")
print("UNVERIFIED: Gradle assembleDebug/testDebugUnitTest/compileDebugAndroidTestKotlin; launcher, Bluetooth and car hardware")
