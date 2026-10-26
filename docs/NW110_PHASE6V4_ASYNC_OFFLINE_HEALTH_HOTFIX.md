# NW110 Phase 6V4D Hotfix — Async Offline Health

Date: 27 October 2026. Cumulative parent `78d18834f91b99c496d816c3e277d4684249524e`.

## Windows build failure reproduced from uploaded evidence
The user's `C:\NWF\run-nw91-final.cmd` passed the NW44 corrected Search preflight and subsequent 6P/6V integration checks. Phase 6V4D then failed before Gradle: `OfflineLibraryActivity.kt : read-only cached or refreshed health report must appear in UI`. No new APK was verified.

## Cause
The old Windows source check demanded `val report = cachedReport ?: library.healthReport().also { cachedReport = it }` in UI code. NW90 deliberately moved read-only `healthReport()` out of synchronous `refresh()` into a background worker. The Activity now schedules `worker.execute { val result = runCatching { library.healthReport() } }`, caches `cachedReport = result.getOrNull()`, then the UI reads `val report = cachedReport ?: run { requestHealthCheck(); return }` and displays `report.attentionCount`. An independent Python preflight also expected the removed synchronous pattern.

## Fix
- Updated `tools/check-phase6v4-integration.ps1` to check the current asynchronous UI/worker/cached-data contract and reject a main-thread scan. Added a self-test with one positive and four negative scenarios.
- Updated `tools/verify_v2_phase6v4d.py` to require the same async pattern, reject reintroduced synchronous scans and execute a negative fixture in its self-test.
- CI wires the Windows and Python checks on the hotfix branch.
- Production Kotlin, library files, Google Drive, downloaded audio and signing remain unchanged.

## Verification boundary
Static tests and negative source fixtures can catch stale/insecure source patterns, but they do not establish Android compilation, physical UI behavior or APK signing. This fix is **not** a successful Gradle build. The previous log explicitly states Gradle never started.

Use the complete branch ZIP with a short fresh extraction path such as `C:\NWG`, then run `run-nw91-final.cmd`. Keep installed music and app data intact. **Public release: NO-GO.**
