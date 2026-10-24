#!/usr/bin/env python3
"""NW26 read-only cross-file source guard. Not an Android build certificate."""
from __future__ import annotations
import argparse
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FILES = {
    "errors": "app/src/main/java/com/nightwave/player/v2/cloud/DriveHttpFailurePolicy.kt",
    "consent": "app/src/main/java/com/nightwave/player/v2/cloud/CloudBackupConsentPolicy.kt",
    "state": "app/src/main/java/com/nightwave/player/v2/cloud/CloudBackupState.kt",
    "worker": "app/src/main/java/com/nightwave/player/v2/cloud/NightWaveCloudWorker.kt",
    "client": "app/src/main/java/com/nightwave/player/v2/cloud/NightWaveDriveClient.kt",
    "activity": "app/src/main/java/com/nightwave/player/v2/ui/NightWaveStudioActivity.kt",
    "authorization": "app/src/main/java/com/nightwave/player/v2/cloud/NightWaveCloudAuthorization.kt",
    "rank": "app/src/main/java/com/nightwave/player/offline/OfflineSearchRanking.kt",
    "offline": "app/src/main/java/com/nightwave/player/offline/OfflineTrackSearch.kt",
    "suggestions": "app/src/main/java/com/nightwave/player/net/LocalSearchSuggestions.kt",
    "lyrics": "app/src/main/java/com/nightwave/player/v2/lyrics/PrivateLyricsStore.kt",
    "libraryUi": "app/src/main/java/com/nightwave/player/offline/OfflineLibraryActivity.kt",
    "smoke": "tools/kotlin/NW26ReliabilitySmoke.kt",
    "runner": "tools/nw26_reliability_smoke.py",
    "workflow": ".github/workflows/android.yml",
    "manifest": "app/src/main/AndroidManifest.xml",
}
def check(src: dict[str, str]) -> list[str]:
    bugs: list[str] = []
    def need(ok: bool, why: str) -> None:
        if not ok: bugs.append(why)
    need("code == 401 || code == 403" in src["errors"] and
         "code == 408 || code == 429 || code in 500..599" in src["errors"],
         "OAuth and retryable Drive HTTP statuses must be distinct")
    need("throw DriveHttpFailure(response.code)" in src["client"],
         "Drive HTTP failures must retain status without a raw response body")
    need('"Bearer $accessToken"' in src["client"] and
         ".followRedirects(false)" in src["client"],
         "Drive must preserve request-local token and redirect protection")
    need("same(expected: CloudBackupConsent, current: CloudBackupConsent)" in src["consent"] and
         "expected.wifiOnly == current.wifiOnly" in src["consent"],
         "Full backup privacy consent snapshot must be compared")
    need("fun CloudBackupStatus.consentSnapshot()" in src["state"] and
         '"nightwave_cloud_backup_v1"' in src["state"],
         "Current cloud preference keys and consent bridge must be preserved")
    need("beforeUpload.consentSnapshot()" in src["worker"] and
         "store.read().consentSnapshot()" in src["worker"] and
         "DriveHttpFailurePolicy.mayRetry(code)" in src["worker"],
         "Worker must validate before and after upload and classify retries")
    need("val operationConsent = cloudStore.read().consentSnapshot()" in src["activity"] and
         "DriveHttpFailurePolicy.message(code)" in src["activity"],
         "Manual sign-in must expose safe error and guard backup consent")
    need("CloudBackupConsentPolicy.same(operationConsent," in src["activity"] and
         "return@onSuccess" in src["activity"],
         "Manual backup must not certify stale privacy choices")
    need("https://www.googleapis.com/auth/drive.appdata" in src["authorization"] and
         "https://www.googleapis.com/auth/drive.file" not in src["authorization"],
         "Cloud backup must not silently widen Drive OAuth scope")
    need("OfflineSearchRanking.score" in src["offline"] and
         "OfflineSearchRanking.terms" in src["offline"],
         "Offline Library needs Unicode-safe ranked search")
    need("take(5_000)" in src["suggestions"] and "take(400)" not in src["suggestions"],
         "Saved music suggestions cannot stop after first 400 tracks")
    need("offlineMatches.take(3)" in src["activity"] and
         "OfflineTrackSearch.match(query, offlineSongs).take(60)" in src["activity"],
         "Search screen must display saved offline matches")
    need("savedMatches.isEmpty() &&" in src["activity"] and
         "offlineMatches.isEmpty() && !searching" in src["activity"],
         "Do not claim no results while offline matches exist")
    need("hasImportedLrc(" in src["lyrics"] and
         '"lrc:" + id' in src["lyrics"] and
         "lyrics.hasImportedLrc(song.id)" in src["libraryUi"],
         "Offline Library must show only actually imported LRC")
    need("TimedLyricsCodec.parse(bytes)" in src["lyrics"] and
         '"nightwave_private_lrc_v1"' in src["lyrics"],
         "NW26 must retain existing verified, private LRC format")
    need("32 exact-production Kotlin Drive/search assertions" in src["runner"] and
         'println("PASS: NW26 " + passed' in src["smoke"],
         "Exact source JVM smoke proof missing")
    need("tools/verify_nw26_cloud_search.py" in src["workflow"] and
         "tools/nw26_reliability_smoke.py" in src["workflow"] and
         "feature/nightwave-v2-nw26-google-search-lyrics" in src["workflow"],
         "CI NW26 checks and branch trigger missing")
    need('android:allowBackup="false"' in src["manifest"] and
         "android.permission.INTERNET" in src["manifest"],
         "Android private-data and network policy changed")
    for key in ("errors", "consent", "worker", "client", "activity",
                "rank", "offline", "suggestions", "lyrics", "libraryUi"):
        imports = re.findall(r"^import\s+(\S+)", src[key], re.M)
        need(len(imports) == len(set(imports)), "Duplicate imports: " + key)
    return bugs

def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--self-test", action="store_true")
    args = ap.parse_args()
    if args.self_test:
        empty = {key: "" for key in FILES}
        assert len(check(empty)) >= 15
        print("PASS: NW26 regression gate rejects missing source")
        return 0
    try:
        source = {key: (ROOT / path).read_text(encoding="utf-8")
                  for key, path in FILES.items()}
    except (OSError, UnicodeError):
        print("FAIL: NW26 full source checkout unavailable")
        return 1
    bugs = check(source)
    for issue in bugs: print("FAIL:", issue)
    if bugs: return 1
    print("PASS: NW26 cloud consent, OAuth diagnostics, local search and LRC source contracts")
    print("UNVERIFIED: Google account picker, real Drive requests, Android UI and full Gradle build")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
