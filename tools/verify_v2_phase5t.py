#!/usr/bin/env python3
"""Phase 5T official-first search source contract. NOT an Android build."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
def read(path):
    return (root / path).read_text(encoding="utf-8")

activity = read("app/src/main/java/com/nightwave/player/v2/ui/NightWaveStudioActivity.kt")
browser = read("app/src/main/java/com/nightwave/player/ui/YouTubeSearchBrowser.kt")
saved = read("app/src/main/java/com/nightwave/player/net/SavedTrackSearch.kt")
gateway = read("app/src/main/java/com/nightwave/player/net/YouTubeGateway.kt")
workflow = read(".github/workflows/android.yml")
manifest = read("app/src/main/AndroidManifest.xml")
gradle = read("app/build.gradle.kts")

assert 'private fun primarySearch(query: String, mode: SearchMode)' in activity
primary = activity.split("private fun primarySearch(", 1)[1].split("private fun prepareSearch(", 1)[0]
assert "SearchInputResolver.resolve(clean)" in primary
assert "is SearchInput.VideoUrl, SearchInput.BadLink -> nativeSearch(clean, mode)" in primary
assert "is SearchInput.Keywords -> {" in primary
assert "prepareSearch(clean, mode)" in primary
assert "openOfficialSearch(clean)" in primary
assert "nativeGateway.search(" not in primary
assert 'private fun openOfficialSearch(query: String)' in activity
assert 'YouTubeSearchBrowser(this).show(clean)' in activity
assert 'onPrimarySearch = { query, mode -> primarySearch(query, mode) }' in activity
assert 'onPrepareSearch = { query, mode -> prepareSearch(query, mode) }' in activity
assert 'onWebSearch = { query -> openOfficialSearch(query) }' in activity

search = activity.split("private fun StudioSearch(", 1)[1].split("private fun StudioLibrary(", 1)[0]
assert 'val submit = { if (query.isNotBlank()) onPrimarySearch(query.trim(), mode) }' in search
assert 'keyboardActions = KeyboardActions(onSearch = { submit() })' in search
assert 'Text("Find music on YouTube")' in search
assert 'onNativeSearch(query.trim(), mode)' in search
assert 'Text(if (searching) "Checking quick results…" else "Try quick in-app results")' in search
assert 'onPrimarySearch(shortcut.query, SearchMode.SONGS)' in search
assert 'SavedTrackSearch.match(query, savedTracks)' in search
assert 'onPlay(song, savedMatches)' in search
assert "Quick results aren't available right now" in search
assert 'Text("Retry quick results")' in search
assert 'Text("Online search is unavailable")' not in search
assert "state.query.isNotBlank()" in search

# Legacy routes and offline library remain available and user data is unchanged.
assert "fun search(query: String, mode: SearchMode)" in gateway
assert "object SavedTrackSearch" in saved
assert "WebSearchRoute.searchUrl(query)" in browser
assert "WebSearchRoute.selectedVideo" in browser
assert "settings.allowFileAccess = false" in browser
assert 'android.intent.category.LAUNCHER' in manifest
assert 'applicationId = "com.nightwave.player"' in gradle
assert 'versionCode = 35' in gradle
assert 'versionName = "2.0.0-alpha5s"' in gradle

# This is a stacked development branch, NOT a proven Android APK.
assert "tools/verify_v2_phase5t.py" in workflow
assert "feature/nightwave-v2-phase5s-build-recovery" in workflow
assert ":app:compileDebugAndroidTestKotlin" in workflow
assert "UNVERIFIED-FOR-DEVICE" in workflow
assert activity.count("{") == activity.count("}")
print("PASS: Phase 5T official-first primary search, local saved matches, optional quick results and URL routing")
print("PENDING: Android Gradle, physical WebView/YouTube availability and device playback")
