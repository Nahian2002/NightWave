#!/usr/bin/env python3
"""NW87 read-only source contracts; does not compile/execute Android code."""
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
def read(name):
    return (ROOT / name).read_text(encoding="utf-8")

base = "app/src/main/java/com/nightwave/player/offline/"
downloads = read(base + "CatalogDownloadsActivity.kt")
offline = read(base + "OfflineLibraryActivity.kt")
dashboard = read(base + "DownloadCenterDashboardPolicy.kt")
recovery = read(base + "OfflineLibraryRecoveryGuide.kt")
for marker in (
    "DownloadCenterDashboardPolicy.overview(",
    "DownloadCenterDashboardPolicy.tab(",
    "DownloadCenterDashboardPolicy.helpLine()",
    "filterLabels[filter]?.apply",
    "DownloadFileVerificationPolicy.ready(",
    "DownloadHistoryPagePolicy.normalizedPage(",
):
    assert marker in downloads, marker
for marker in (
    "OfflineLibraryRecoveryGuide.recheckActionLabel()",
    "runCatching { library.healthReport() }",
    "checked.getOrNull()?.indexReadable == true",
    "OfflineLibraryRecoveryGuide.emptyIssuesMessage(",
    "OfflineLibraryRecoveryGuide.title(health)",
    "OfflineLibraryRecoveryGuide.help(health)",
    '.setNeutralButton("Import trusted audio")',
):
    assert marker in offline, marker
assert "Resume restarts from zero" in dashboard
assert '"$name · …"' in dashboard
assert "Do not delete or automatically rebuild it" in recovery
assert "NW87_TRANSFER_OFFLINE_PRESENTATION_SMOKE_PASS" in read(
    "tools/NW87TransferSmoke.kt")
print("NW87_TRANSFER_OFFLINE_SOURCE_CONTRACT_PASS")
print("UNVERIFIED: Gradle, WorkManager and real device tests")
