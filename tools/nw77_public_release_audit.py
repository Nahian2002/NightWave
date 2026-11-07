#!/usr/bin/env python3
"""NW77 public release readiness audit. Read-only; no music, tokens or network access.

A passing debug build is NOT a public content-use authorization. This audit
intentionally reports release NO-GO while prohibited or unreviewed source paths
remain. It never deletes/changes existing app data or modifies Android source.
"""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path
import tempfile

SOURCE_FILES = {
    "gateway": "app/src/main/java/com/nightwave/player/net/YouTubeGateway.kt",
    "playback": "app/src/main/java/com/nightwave/player/playback/PlaybackService.kt",
    "search_worker": "app/src/main/java/com/nightwave/player/offline/SearchTrackSaveWorker.kt",
    "search_queue": "app/src/main/java/com/nightwave/player/offline/SearchTrackSaveQueue.kt",
    "search_policy": "app/src/main/java/com/nightwave/player/offline/SearchTrackSavePolicy.kt",
    "direct_policy": "app/src/main/java/com/nightwave/player/offline/PermittedAudioDownloadPolicy.kt",
    "catalog": "app/src/main/java/com/nightwave/player/offline/FreeAudioCatalogPolicy.kt",
    "cloud": "app/src/main/java/com/nightwave/player/v2/cloud/NightWaveCloudAuthorization.kt",
}
BOUNDARY_FILES = {
    "release_policy": "app/src/main/java/com/nightwave/player/core/YouTubeReleasePolicy.kt",
    "transport": "app/src/main/java/com/nightwave/player/net/ExtractorDownloader.kt",
    "community": "app/src/main/java/com/nightwave/player/net/CommunityVideoSearch.kt",
    "official": "app/src/main/java/com/nightwave/player/ui/OfficialYouTube.kt",
    "startup": "app/src/main/java/com/nightwave/player/NightWaveApp.kt",
}
POLICY_URLS = [
    "https://developers.google.com/youtube/terms/developer-policies",
    "https://developers.google.com/youtube/terms/developer-policies-guide",
    "https://developers.google.com/workspace/drive/api/guides/api-specific-auth",
]

def _finding(code: str, severity: str, detail: str, files: list[str]) -> dict:
    return {"code": code, "severity": severity, "finding": detail, "files": files}

def private_search_enabled(project: Path) -> bool:
    path = project / BOUNDARY_FILES["release_policy"]
    return path.is_file() and bool(re.search(r"(?m)^    const val UNOFFICIAL_SEARCH_ENABLED = true$",
                                            path.read_text(encoding="utf-8")))


def offline_boundary(project: Path, texts: dict) -> bool:
    boundary = {name: (project / path).read_text(encoding="utf-8") if (project / path).is_file() else ""
                for name, path in BOUNDARY_FILES.items()}
    gateway = texts["gateway"] or ""
    guards = (
        re.search(r"(?m)^    const val OFFICIAL_ONLY = true$", boundary["release_policy"]),
        'check(!OFFICIAL_ONLY)' in boundary["release_policy"],
        re.search(r"fun resolveAudioOnlyForOffline\(url: String\): PlayableAudio \{\s*YouTubeReleasePolicy.requireLegacyAccess\(\)", gateway),
        "if (YouTubeReleasePolicy.OFFICIAL_ONLY) return fail(YouTubeReleasePolicy.OFFLINE_UNAVAILABLE)\n        return try" in (texts["search_worker"] or ""),
        "check(!YouTubeReleasePolicy.OFFICIAL_ONLY) { YouTubeReleasePolicy.OFFLINE_UNAVAILABLE }" in (texts["search_queue"] or ""),
        "fun safeAudioEndpoint(raw: String): Boolean {\n        if (YouTubeReleasePolicy.OFFICIAL_ONLY) return false" in (texts["search_policy"] or ""),
        "Intent(Intent.ACTION_VIEW, Uri.parse(url))" in boundary["official"],
        "VideoLink.normalize(url)?.let { open(activity, it) }" in boundary["official"],
    )
    return all(guards)


def owner_playback_boundary(texts: dict) -> bool:
    return ("if (!YouTubeReleasePolicy.ownerPlaybackEnabled) {\n            generation++" in (texts["playback"] or "") and
            "check(YouTubeReleasePolicy.ownerPlaybackEnabled)" in (texts["gateway"] or ""))


def public_search_boundary(project: Path, texts: dict) -> bool:
    transport = (project / BOUNDARY_FILES["transport"]).read_text(encoding="utf-8")
    community = (project / BOUNDARY_FILES["community"]).read_text(encoding="utf-8")
    startup = (project / BOUNDARY_FILES["startup"]).read_text(encoding="utf-8")
    gateway = texts["gateway"] or ""
    return (not private_search_enabled(project) and
            'onPhase: (PublicSearchPhase) -> Unit = {}): VideoSearchResults {\n        YouTubeReleasePolicy.requireLegacyAccess()' in gateway and
            'override fun execute(request: Request): Response {\n        YouTubeReleasePolicy.requireLegacyAccess()' in transport and
            'private val instances: List<String> = emptyList()' in community and
            'if (YouTubeReleasePolicy.OFFICIAL_ONLY) return\n        try {\n            NewPipe.init' in startup)


def private_search_boundary(project: Path, texts: dict) -> bool:
    transport = (project / BOUNDARY_FILES["transport"]).read_text(encoding="utf-8")
    gateway = texts["gateway"] or ""
    return (private_search_enabled(project) and
            "YouTubeReleasePolicy.requireUnofficialSearch()" in gateway and
            "YouTubeReleasePolicy.requireUnofficialSearch()" in transport and
            "PublicSearchRequest.current.get() != null && allowsNewPipeSearch(url)" in transport and
            ".followRedirects(false)" in transport and ".followSslRedirects(false)" in transport)



def audit(project: Path) -> dict:
    texts = {}
    for name, rel in SOURCE_FILES.items():
        path = project / rel
        if not path.is_file():
            texts[name] = None
        else:
            texts[name] = path.read_text(encoding="utf-8")
    issues = []
    missing = [SOURCE_FILES[n] for n, text in texts.items() if text is None]
    if missing:
        issues.append(_finding(
            "NW77-MISSING-SOURCE", "BLOCKER",
            "Mandatory source paths unavailable; cannot certify a public release.", missing))
    # Unknown implementations cannot be treated as compliant. These patterns
    # identify observed, concrete source paths, not the rights of any one song.
    gateway = texts["gateway"] or ""
    worker = texts["search_worker"] or ""
    queue = texts["search_queue"] or ""
    playback = texts["playback"] or ""
    search_policy = texts["search_policy"] or ""
    direct = texts["direct_policy"] or ""
    catalog = texts["catalog"] or ""
    cloud = texts["cloud"] or ""

    offline_fenced = offline_boundary(project, texts)
    online_fenced = owner_playback_boundary(texts)
    owner_preview_supported = "ownerPlaybackEnabled" in gateway
    if owner_preview_supported:
        issues.append(_finding(
            "NW77-OWNER-YOUTUBE-PLAYBACK-PREVIEW", "BLOCKER",
            "This source supports a debug-only owner preview of separate YouTube audio. Keep preview APKs private.",
            [BOUNDARY_FILES["release_policy"], SOURCE_FILES["gateway"]]))
    if private_search_enabled(project):
        issues.append(_finding(
            "NW77-PRIVATE-UNOFFICIAL-SEARCH", "BLOCKER",
            "The owner preview enables unofficial YouTube metadata search. Keep the APK private.",
            [BOUNDARY_FILES["release_policy"], SOURCE_FILES["gateway"]]))
    elif (project / BOUNDARY_FILES["release_policy"]).is_file() and not public_search_boundary(project, texts):
        issues.append(_finding(
            "NW77-UNVERIFIED-SEARCH-BOUNDARY", "BLOCKER",
            "The public search boundary could not be verified.", [SOURCE_FILES["gateway"]]))
    if (not (online_fenced and owner_preview_supported) and "NewPipe.getService(\"YouTube\")" in gateway
            and "fun resolve(url: String)" in gateway
            and "info.audioStreams" in gateway
            and "gateway.resolve(entry.url)" in playback
            and "setUri(playable.audioUrl)" in playback):
        issues.append(_finding(
            "NW77-YOUTUBE-SEPARATE-BACKGROUND-AUDIO", "BLOCKER",
            "PlaybackService resolves extracted YouTube audio into the standalone media player. "
            "Needs compliant playback design or provider authorization before public distribution.",
            [SOURCE_FILES["gateway"], SOURCE_FILES["playback"]]))
    if (not offline_fenced and "resolveAudioOnlyForOffline(canonical)" in worker
            and "downloadResolvedSearchAudio(" in worker
            and "SearchTrackSaveWorker" in queue):
        issues.append(_finding(
            "NW77-YOUTUBE-OFFLINE-COPY", "BLOCKER",
            "Search-track worker resolves a YouTube audio stream and saves it to a private file. "
            "YouTube API policies prohibit this without prior written approval.",
            [SOURCE_FILES["search_worker"], SOURCE_FILES["search_queue"]]))
    if (not offline_fenced and "googlevideo.com" in search_policy
            and "safeAudioEndpoint(" in search_policy):
        issues.append(_finding(
            "NW77-YOUTUBE-CDN-AUDIO-ENDPOINT", "BLOCKER",
            "Searched-audio save policy accepts Googlevideo audio URLs; URI validation "
            "does not itself grant content usage rights.",
            [SOURCE_FILES["search_policy"]]))
    # Positive source guardrails, but neither confirms source licensing.
    if ("forbiddenHosts" not in direct or
            '"youtube.com"' not in direct or
            '"googlevideo.com"' not in direct):
        issues.append(_finding(
            "NW77-DIRECT-ORIGIN-SAFETY", "BLOCKER",
            "Direct download source excludes neither YouTube nor Googlevideo origins.",
            [SOURCE_FILES["direct_policy"]]))
    if ("permittedLicense(shortName: String)" not in catalog or
            '"cc0"' not in catalog or
            '"public domain"' not in catalog):
        issues.append(_finding(
            "NW77-CATALOG-LICENSE-GATE", "BLOCKER",
            "Public-domain catalog no longer enforces its declared license allowlist.",
            [SOURCE_FILES["catalog"]]))
    if ('const val SCOPE = "https://www.googleapis.com/auth/drive.appdata"' not in cloud):
        issues.append(_finding(
            "NW77-CLOUD-SCOPE", "BLOCKER",
            "Backup no longer declares only the app-data Drive permission.",
            [SOURCE_FILES["cloud"]]))

    # Even a clean static scan cannot certify third-party rights, privacy policy,
    # Play review, Google OAuth production status, or real-device acceptance.
    blockers = sum(issue["severity"] == "BLOCKER" for issue in issues)
    return {
        "phase": "NW77",
        "public_release": "NO_GO" if blockers else "REVIEW_REQUIRED",
        "release_approved": False,
        "blocking_findings": blockers,
        "findings": issues,
        "verified_safeguards": {
            "youtube_media_extraction_remains_blocked": offline_fenced and
                "fun resolve(url: String): PlayableAudio {\n        YouTubeReleasePolicy.requireLegacyAccess()" in gateway and
                "if (YouTubeReleasePolicy.OFFICIAL_ONLY) {\n            generation++" in playback,
            "youtube_offline_saving_remains_blocked": offline_fenced,
            "direct_audio_excludes_youtube": '"youtube.com"' in direct and
                '"googlevideo.com"' in direct and "forbiddenHosts" in direct,
            "commons_cc0_or_public_domain_filter": all(x in catalog for x in
                ("permittedLicense(shortName: String)", '"cc0"', '"public domain"')),
            "google_backup_narrow_scope": 'const val SCOPE = "https://www.googleapis.com/auth/drive.appdata"' in cloud,
        },
        "manual_gates": [
            "Provide documented content rights and source agreements for public audio features.",
            "Replace or obtain authorization for YouTube-derived standalone and offline audio paths.",
            "Verify Google OAuth production status and real Drive backup/readback/restore preview.",
            "Pass signed release APK, platform policy, accessibility and device acceptance.",
        ],
        "standards": POLICY_URLS,
        "note": "Static evidence only. Debug builds and existing local media are untouched.",
    }

def self_test() -> None:
    with tempfile.TemporaryDirectory(prefix="nightwave-nw77-") as tmp:
        root = Path(tmp)
        for rel in SOURCE_FILES.values():
            file = root / rel
            file.parent.mkdir(parents=True, exist_ok=True)
            file.write_text("", encoding="utf-8")
        (root / SOURCE_FILES["gateway"]).write_text(
            'NewPipe.getService("YouTube")\nfun resolve(url: String)\ninfo.audioStreams')
        (root / SOURCE_FILES["playback"]).write_text(
            "gateway.resolve(entry.url)\nsetUri(playable.audioUrl)")
        (root / SOURCE_FILES["search_worker"]).write_text(
            "resolveAudioOnlyForOffline(canonical)\ndownloadResolvedSearchAudio(")
        (root / SOURCE_FILES["search_queue"]).write_text("SearchTrackSaveWorker")
        (root / SOURCE_FILES["search_policy"]).write_text(
            "googlevideo.com\nsafeAudioEndpoint(")
        (root / SOURCE_FILES["direct_policy"]).write_text(
            'forbiddenHosts = setOf("youtube.com", "googlevideo.com")')
        (root / SOURCE_FILES["catalog"]).write_text(
            'fun permittedLicense(shortName: String) = "cc0" == "public domain"')
        (root / SOURCE_FILES["cloud"]).write_text(
            'const val SCOPE = "https://www.googleapis.com/auth/drive.appdata"')
        result = audit(root)
        actual = {x["code"] for x in result["findings"]}
        required = {
            "NW77-YOUTUBE-SEPARATE-BACKGROUND-AUDIO",
            "NW77-YOUTUBE-OFFLINE-COPY",
            "NW77-YOUTUBE-CDN-AUDIO-ENDPOINT",
        }
        assert required.issubset(actual) and result["public_release"] == "NO_GO"
        # Removing detected risky source code does NOT automatically approve
        # publication. Manual licensing/provider review is still necessary.
        for item in ("gateway", "playback", "search_worker", "search_policy"):
            (root / SOURCE_FILES[item]).write_text("", encoding="utf-8")
        clean = audit(root)
        assert clean["public_release"] == "REVIEW_REQUIRED"
        assert not clean["release_approved"]
        (root / SOURCE_FILES["cloud"]).unlink()
        missing = audit(root)
        assert missing["public_release"] == "NO_GO"
        assert any(x["code"] == "NW77-MISSING-SOURCE" for x in missing["findings"])
        assert any(x["code"] == "NW77-CLOUD-SCOPE" for x in missing["findings"])
    checkout = Path(__file__).resolve().parents[1]
    if (checkout / BOUNDARY_FILES["release_policy"]).exists():
        sources = {name: (checkout / path).read_text(encoding="utf-8") for name, path in SOURCE_FILES.items()}
        assert private_search_boundary(checkout, sources), "Private search exceeds its metadata boundary"
        current = audit(checkout)
        assert current["public_release"] == "NO_GO"
        assert {issue["code"] for issue in current["findings"]} == {
            "NW77-PRIVATE-UNOFFICIAL-SEARCH", "NW77-OWNER-YOUTUBE-PLAYBACK-PREVIEW"}
        assert current["verified_safeguards"]["youtube_offline_saving_remains_blocked"]
        assert not current["verified_safeguards"]["youtube_media_extraction_remains_blocked"]
        unguarded = {**sources, "playback": sources["playback"].replace(
            "if (!YouTubeReleasePolicy.ownerPlaybackEnabled) {", "BROKEN_GUARD", 1)}
        assert owner_playback_boundary(sources)
        assert not owner_playback_boundary(unguarded)
        for name, marker in (
            ("gateway", "YouTubeReleasePolicy.requireLegacyAccess()"),
            ("search_worker", "if (YouTubeReleasePolicy.OFFICIAL_ONLY) return fail"),
            ("search_policy", "if (YouTubeReleasePolicy.OFFICIAL_ONLY) return false"),
            ("search_queue", "check(!YouTubeReleasePolicy.OFFICIAL_ONLY)"),
        ):
            modified = {**sources, name: sources[name].replace(marker, "BROKEN_GUARD", 1)}
            assert not offline_boundary(checkout, modified), name
    print("NW77 public-release audit self-test PASS: detected prohibited paths, manual review, missing files.")

def main() -> int:
    parser = argparse.ArgumentParser(description="Read-only NW77 public-release gate")
    parser.add_argument("--project-root", default=".")
    parser.add_argument("--report", default="dist/NightWave-NW77-public-release-audit.json")
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    if args.self_test:
        self_test()
        return 0
    project = Path(args.project_root).resolve()
    report = audit(project)
    target = Path(args.report)
    if not target.is_absolute():
        target = project / target
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print("NW77 public release: " + report["public_release"])
    for issue in report["findings"]:
        print("  " + issue["code"] + ": " + issue["finding"])
    print("Report: " + str(target))
    print("Debug builds remain permitted for private testing. No APK was published.")
    return 2 if report["public_release"] == "NO_GO" else 3

if __name__ == "__main__":
    raise SystemExit(main())
