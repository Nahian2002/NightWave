#!/usr/bin/env python3
"""NW77 public release readiness audit. Read-only; no music, tokens or network access.

A passing debug build is NOT a public content-use authorization. This audit
intentionally reports release NO-GO while prohibited or unreviewed source paths
remain. It never deletes/changes existing app data or modifies Android source.
"""
from __future__ import annotations

import argparse
import json
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
POLICY_URLS = [
    "https://developers.google.com/youtube/terms/developer-policies",
    "https://developers.google.com/youtube/terms/developer-policies-guide",
    "https://developers.google.com/workspace/drive/api/guides/api-specific-auth",
]

def _finding(code: str, severity: str, detail: str, files: list[str]) -> dict:
    return {"code": code, "severity": severity, "finding": detail, "files": files}

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

    if ("NewPipe.getService(\"YouTube\")" in gateway
            and "fun resolve(url: String)" in gateway
            and "info.audioStreams" in gateway
            and "gateway.resolve(entry.url)" in playback
            and "setUri(playable.audioUrl)" in playback):
        issues.append(_finding(
            "NW77-YOUTUBE-SEPARATE-BACKGROUND-AUDIO", "BLOCKER",
            "PlaybackService resolves extracted YouTube audio into the standalone media player. "
            "Needs compliant playback design or provider authorization before public distribution.",
            [SOURCE_FILES["gateway"], SOURCE_FILES["playback"]]))
    if ("resolveAudioOnlyForOffline(canonical)" in worker
            and "downloadResolvedSearchAudio(" in worker
            and "SearchTrackSaveWorker" in queue):
        issues.append(_finding(
            "NW77-YOUTUBE-OFFLINE-COPY", "BLOCKER",
            "Search-track worker resolves a YouTube audio stream and saves it to a private file. "
            "YouTube API policies prohibit this without prior written approval.",
            [SOURCE_FILES["search_worker"], SOURCE_FILES["search_queue"]]))
    if ("googlevideo.com" in search_policy
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
