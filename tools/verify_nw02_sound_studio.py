#!/usr/bin/env python3
"""NW-02 source integrity: Sound Studio/Media3 equalizer integration.

This is not a successful Android build, unit-test run or audible-device test.
"""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
def source(path: str) -> str:
    return (root / path).read_text(encoding="utf-8")

screen = source("app/src/main/java/com/nightwave/player/v2/ui/NightWaveSoundStudio.kt")
activity = source("app/src/main/java/com/nightwave/player/v2/ui/NightWaveStudioActivity.kt")
service = source("app/src/main/java/com/nightwave/player/playback/PlaybackService.kt")
policy = source("app/src/main/java/com/nightwave/player/playback/SoundStudioPolicy.kt")
capabilities = source("app/src/main/java/com/nightwave/player/playback/EqualizerCapabilities.kt")
tests = source("app/src/test/java/com/nightwave/player/playback/SoundStudioPolicyTest.kt")
workflow = source(".github/workflows/android.yml")

assert "fun NightWaveSoundStudioDialog(onDismiss: () -> Unit)" in screen
assert "DialogProperties(usePlatformDefaultWidth = false)" in screen
assert "Audio engine could not be reached" in screen
assert "latest.levelsMb" in screen
assert "resetDraft = action == PlaybackService.ACTION_EQ_PRESET_SET" in screen
assert "Sound Studio" in activity
assert "Icons.Outlined.GraphicEq" in activity
assert "val levelsMb: List<Int>" in capabilities
assert 'effect.getBandLevel(i.toShort()).toInt()' in capabilities
assert "eqCapabilities.publish(candidate, sessionId)" in service
assert "Sound Studio" in screen and "Frequency response" in screen
assert "Visual guide for the manual bands; not a live audio spectrum." in screen
assert "Device presets" in screen and "Apply manual EQ" in source("app/src/main/java/com/nightwave/player/v2/ui/SoundStudioApplyBar.kt")
assert "capabilities.centersHz.size" in screen
assert "SoundStudioPolicy.updateLevel" in screen
assert "SoundStudioPolicy.validLevels" in screen
assert "statusStore.read()" in screen
assert "latest.commandId == requestId" in screen
assert "ACTION_EQ_PRESET_SET" in screen and "ACTION_EQ_BANDS_APPLY" in screen
assert "ACTION_EQ_DISCOVER" in screen and "delay(300L)" in screen

assert "onEqualizer = { showSoundStudio = true }" in activity
assert "if (showSoundStudio) NightWaveSoundStudioDialog(" in activity
assert "var showSoundStudio by rememberSaveable" in activity
assert "AlertDialog.Builder(context).setTitle(\"Manual equalizer\")" not in activity
assert "sendEqCommand(actionName:" not in activity
assert "requestedEqPreset = preset" in service
assert "val availablePresets = eqCapabilities.read()" in service
assert "releaseEqualizer()\n                    applyEqualizer(sessionId)" in service
assert "Rejected: start playback and refresh supported presets." in service
assert "fun flatLevels(" in policy and "fun validLevels(" in policy
assert "if (minimumMb > maximumMb) emptyList()" in policy
assert tests.count("@Test") >= 7

assert "feature/nightwave-v2-nw02-sound-studio" in workflow
assert "python3 tools/verify_nw02_sound_studio.py" in workflow
assert ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin" in workflow

for name, text in (("screen", screen), ("activity", activity), ("service", service)):
    imports = [line for line in text.splitlines() if line.startswith("import ")]
    assert len(imports) == len(set(imports)), name + ": duplicate Kotlin imports"

print("PASS: NW-02 Compose Sound Studio and Media3 effect source contracts")
print("UNVERIFIED: Gradle compilation, new JVM tests, output effect, signer, phone QA")
