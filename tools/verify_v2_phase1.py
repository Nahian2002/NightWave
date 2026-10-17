#!/usr/bin/env python3
"""Static phase-1 gate. Does not claim Android compilation or runtime behavior."""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
app = root / "app/src/main"
java = app / "java/com/nightwave/player"
manifest = ET.parse(app / "AndroidManifest.xml").getroot()
android_name = "{http://schemas.android.com/apk/res/android}name"
activities = [x.attrib.get(android_name) for x in manifest.iter("activity")]
assert ".ui.MainActivity" in activities
assert ".v2.ui.NightWaveStudioActivity" in activities
assert any(x.attrib.get(android_name) == ".playback.PlaybackService" for x in manifest.iter("service"))
assert (app / "res/layout/activity_main.xml").exists()

settings = (root / "app/build.gradle.kts").read_text(encoding="utf-8")
root_build = (root / "build.gradle.kts").read_text(encoding="utf-8")
for token in ("compose = true", "compose-bom:2024.12.01", "activity-compose:1.9.3",
              "coil-compose:2.7.0", "material3", "org.jetbrains.kotlin.plugin.compose"):
    assert token in (settings + root_build), token
assert 'applicationId = "com.nightwave.player"' in settings

profile = (java / "v2/model/ProfileModel.kt").read_text(encoding="utf-8")
design = (java / "v2/model/NightWaveDesign.kt").read_text(encoding="utf-8")
studio = (java / "v2/ui/NightWaveStudioActivity.kt").read_text(encoding="utf-8")
store = (java / "v2/data/GuestProfileStore.kt").read_text(encoding="utf-8")
old = (java / "ui/MainActivity.kt").read_text(encoding="utf-8")
service = (java / "playback/PlaybackService.kt").read_text(encoding="utf-8")
for c in ("MIDNIGHT", "AURORA", "PEARL", "AMOLED"):
    assert c in profile and c in design
for text in ("Good to see you", "What are we playing?", "Your music.",
             "Make it yours.", "onSearch", "onPlay", "onLegacyPlayer"):
    assert text in studio, text
assert "GuestProfileStore(this)" in studio
assert "nightwave_v2_guest_profile_1" in store
assert "nightwave_web_search" in old
assert "Return to NightWave" in old
assert "MediaSessionService" in service and "onGetSession" in service
assert "Player.STATE_ENDED" in service
assert "Cloud recovery" in studio
assert "GoogleSignInClient" not in studio
assert "signInWithCredential" not in studio
for secret in ("access_token", "refresh_token", "password", "id_token", "session_cookie"):
    assert secret in profile and secret not in store

for name, body in (("Studio", studio), ("Theme", (java / "v2/ui/NightWaveTheme.kt").read_text()),
                   ("Components", (java / "v2/ui/StudioComponents.kt").read_text())):
    # Low-cost structural sanity check; Kotlin parser/Gradle is the authoritative check.
    assert body.count("{") == body.count("}"), f"{name}: unmatched brace count"

print("PASS: opt-in v2 activity, existing playback service, Compose dependencies,")
print("      four themes, guest persistence, data migration isolation, route handoff")
print("      and no fabricated Google sign-in or leaked secret fields")
print("PENDING: full Gradle Kotlin/Compose build and device layout tests")
