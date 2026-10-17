#!/usr/bin/env python3
"""NightWave Phase 5O source-only pager/navigation gate.

Verifies wiring, not Compose compilation, drag physics or phone operation.
"""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
read = lambda p: (root / p).read_text(encoding="utf-8")
base = "app/src/main/java/com/nightwave/player/"
ui = read(base + "v2/ui/NightWaveStudioActivity.kt")
player = read(base + "v2/ui/StudioPlayerScreens.kt")
service = read(base + "playback/PlaybackService.kt")
offline = read(base + "offline/OfflineAudioLibrary.kt")
gradle = read("app/build.gradle.kts")
builder = read("tools/build-windows.ps1")
device = read("tools/check-device-update.ps1")
tests = read("app/src/androidTest/java/com/nightwave/player/v2/ui/NightWaveSwipeTabsInstrumentedTest.kt")
manifest = read("app/src/main/AndroidManifest.xml")

assert 'versionCode = 34' in gradle
assert 'versionName = "2.0.0-alpha5r"' in gradle
assert "-VersionName '2.0.0-alpha5r' -VersionCode '34'" in builder
assert "$versionName = '2.0.0-alpha5r'" in device
assert '$versionCode = 34' in device
assert '$reportedVersion -ne $versionName' in device

# One stable page position for click/swipe/back/settings and restoration.
assert 'import androidx.compose.foundation.pager.HorizontalPager' in ui
assert 'import androidx.compose.foundation.pager.rememberPagerState' in ui
assert 'val pagerState = rememberPagerState(' in ui
assert 'StudioTab.entries.size' in ui
assert 'val tab = StudioTab.entries[pagerState.currentPage]' in ui
assert 'HorizontalPager(' in ui
assert 'state = pagerState' in ui
assert 'key = { StudioTab.entries[it].name }' in ui
assert 'beyondViewportPageCount = 1' in ui
assert 'userScrollEnabled = !expanded && !showQueue' in ui
assert 'pageSpacing = 0.dp' in ui
assert 'StudioTab.HOME -> StudioHome(' in ui
assert 'StudioTab.SEARCH -> StudioSearch(' in ui
assert 'StudioTab.LIBRARY -> StudioLibrary(' in ui
assert 'StudioTab.PROFILE -> StudioProfile(' in ui
assert 'modifier = Modifier.fillMaxSize().padding(safe).testTag("nightwave-tab-pager")' in ui
assert 'Crossfade(targetState = tab' not in ui
assert 'var tab by rememberSaveable' not in ui

# Gestures and bottom tabs must never diverge.
assert '.clickable { navigateToTab(item) }' in ui
assert 'val selected = tab == item' in ui
assert '.semantics { this.selected = selected }' in ui
assert '.testTag("nightwave-tab-" + item.name.lowercase())' in ui
assert 'IconButton(onClick = { navigateToTab(StudioTab.PROFILE) })' in ui
assert 'navigateToTab(StudioTab.SEARCH)' in ui
assert 'navigateToTab(StudioTab.LIBRARY)' in ui
assert 'tab != StudioTab.HOME -> navigateToTab(StudioTab.HOME)' in ui
assert 'showQueue -> showQueue = false' in ui
assert 'expanded -> expanded = false' in ui
assert 'tab == StudioTab.LIBRARY && selectedPlaylist != null -> selectedPlaylist = null' in ui

# Reduced motion skips programmatic animation, and focused input cannot keep
# the soft keyboard over a different tab.
assert 'if (profile.motion == MotionPreference.REDUCED)' in ui
assert 'pagerState.scrollToPage(destination.ordinal)' in ui
assert 'pagerState.animateScrollToPage(destination.ordinal)' in ui
assert 'focus.clearFocus()' in ui and 'keyboard?.hide()' in ui
assert 'if (searchNavigationToken > 0) pagerState.scrollToPage(StudioTab.SEARCH.ordinal)' in ui
assert 'searchNavigationToken' in ui

# Child and overlay gestures remain separate from the page pager.
assert 'detectVerticalDragGestures(' in player
assert 'detectHorizontalDragGestures(' in player
assert 'PlayerVerticalSwipe.MINIMIZE -> onHide()' in player
assert 'PlayerVerticalSwipe.EXPAND -> onExpand()' in player
assert 'PlayerVerticalSwipe.MINIMIZE) onDismiss()' in player
assert 'StudioNowPlayingOverlay(' in ui
assert 'if (showQueue) StudioQueueSheet(' in ui
assert 'onOpenClassic = onOpenClassic' in ui

# Real Android input regressions are authored for an emulator.
assert tests.count('@Test') == 2
assert 'swipesChangeTheSelectedBottomTab' in tests
assert 'bottomTabClicksAndVerticalScrollingShareOnePageState' in tests
assert 'performTouchInput { swipeLeft() }' in tests
assert 'performTouchInput { swipeRight() }' in tests
assert 'performTouchInput { swipeUp() }' in tests
assert 'assertIsSelected()' in tests

# No storage, service, package or permission changes in this phase.
assert 'nightwave_offline_v1' in offline
assert 'private fun resumeOfflineSession()' in service
assert 'NightWaveStudioActivity::class.java' in service
assert manifest.count('android.intent.category.LAUNCHER') == 1
assert 'android:allowBackup="false"' in manifest
ET.parse(root / "app/src/main/AndroidManifest.xml")
assert ui.count('{') == ui.count('}')
imports = [s for s in ui.splitlines() if s.startswith('import ')]
assert len(imports) == len(set(imports))

print('PASS: Phase 5O swipe pager, bottom tabs, back/settings and reduced-motion source wiring')
print('PENDING: Gradle/Compose build, instrumented gesture tests and real phone QA')
