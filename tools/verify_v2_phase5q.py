#!/usr/bin/env python3
"""NightWave Phase 5Q: search source/wiring and offline fallback assertions.

This checks checked-in files, not public-provider uptime, Compose compilation
or an Android-device build.
"""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
read = lambda path: (root / path).read_text(encoding="utf-8")
base = "app/src/main/java/com/nightwave/player/"
gradle = read("app/build.gradle.kts")
studio = read(base + "v2/ui/NightWaveStudioActivity.kt")
gateway = read(base + "net/YouTubeGateway.kt")
fallback = read(base + "net/CommunityVideoSearch.kt")
saved = read(base + "net/SavedTrackSearch.kt")
input_parser = read(base + "core/SearchInputResolver.kt")
browser = read(base + "ui/YouTubeSearchBrowser.kt")
saved_tests = read("app/src/test/java/com/nightwave/player/net/SavedTrackSearchTest.kt")
fallback_tests = read("app/src/test/java/com/nightwave/player/net/CommunityVideoSearchTest.kt")
service = read(base + "playback/PlaybackService.kt")
offline = read(base + "offline/OfflineAudioLibrary.kt")
manifest = read("app/src/main/AndroidManifest.xml")
builder = read("tools/build-windows.ps1")
device = read("tools/check-device-update.ps1")

assert 'versionCode = 35' in gradle
assert 'versionName = "2.0.0-alpha5s"' in gradle
assert "-VersionName '2.0.0-alpha5s' -VersionCode '35'" in builder
assert "$versionName = '2.0.0-alpha5s'" in device
assert '$versionCode = 35' in device

# Saved results must be available without external network search.
assert 'object SavedTrackSearch' in saved
assert 'fun match(query: String, tracks: List<VideoPreview>' in saved
assert 'terms.all { term -> haystack.contains(term) }' in saved
assert '.distinctBy { it.value.url }' in saved
assert '.take(limit.coerceIn(1, 24))' in saved
assert 'SavedTrackSearch.match(query, savedTracks)' in studio
assert 'SectionHeading("In your library"' in studio
assert 'playlistNames.take(25).flatMap(getPlaylist)' in studio
assert '(favorites + recent +' in studio
assert 'onPlay(song, savedMatches)' in studio

# Failure state is smaller, still actionable, and preserves same-query results.
assert 'Text("Online search is unavailable"' in studio
assert 'Text("Retry")' in studio
assert 'Text("YouTube ↗")' in studio
assert '(!responseMatchesQuery || state.error.isBlank()) && query.isNotBlank()' in studio
assert 'val responseMatchesQuery = state.query.isNotBlank()' in studio
assert 'val searching = responseMatchesQuery && state.loading' in studio
assert 'Native results are unavailable right now' not in studio
assert 'EmptyMusicCard("Search needs a different route"' not in studio
assert 'val previous = nativeSearchState.takeIf {' in studio
assert 'videos = previous?.videos.orEmpty()' in studio
assert 'Showing earlier results for this search.' in studio

# Typed public URL is handled directly, not sent as keyword search. Invalid
# link is rejected without contacting NewPipe, and the parser validates it.
assert 'SearchInputResolver.resolve(clean)' in studio
assert 'is SearchInput.VideoUrl -> {' in studio
assert 'SearchInput.BadLink -> {' in studio
assert 'playback.play(VideoPreview("Loading track…"' in studio
assert 'fun resolve(raw: String): SearchInput' in input_parser

# Direct extractor failure should skip a duplicate request to same backend;
# independent community results continue to be verified via mock webservers.
assert 'Direct extractor unavailable; trying independent public provider' in gateway
assert 'break' in gateway.split('Direct extractor unavailable; trying independent public provider')[1].split('}', 1)[0]
assert 'val alternateResult = runCatching { alternate.search(text) }' in gateway
assert 'class CommunityVideoSearch' in fallback
assert 'failsOverAfterMalformedJsonChallenge' in fallback_tests
assert 'emptyPublicResultsTryNextEndpoint' in fallback_tests
assert 'WebSearchRoute.searchUrl(query)' in browser
assert 'Search on YouTube ↗' in studio

assert saved_tests.count('@Test') == 6
assert fallback_tests.count('@Test') == 4
assert 'nightwave_offline_v1' in offline
assert 'private fun resumeOfflineSession()' in service
assert manifest.count('android.intent.category.LAUNCHER') == 1
ET.parse(root / "app/src/main/AndroidManifest.xml")
for name, source in (('search UI', studio), ('gateway', gateway),
                     ('saved search', saved), ('fallback', fallback)):
    assert source.count('{') == source.count('}'), name + ' brace balance'
    imports = [line for line in source.splitlines() if line.startswith('import ')]
    assert len(imports) == len(set(imports)), name + ' duplicate imports'

print('PASS: Phase 5Q device-local saved matches, provider fallback and clear search retry UX')
print('PENDING: Gradle/Android build, WebView/Compose runtime, and live provider/device QA')
