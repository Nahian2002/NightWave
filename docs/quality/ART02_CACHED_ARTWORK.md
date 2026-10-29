# ART02 — existing saved artwork continuity

30 October 2026. **LOCAL_VERIFIED / DEVICE_NOT_RUN**. Protected parent `941922ed7b6b86d992db2a2d8469bd46af427c5f`; branch `update/nightwave-art02-cached-artwork`, [draft PR #157](https://github.com/Nahian2002/NightWave/pull/157), stacked on FINISH02 #156. Private checkpoint `20261030-art02` contains the pre-change recovery bundle. SEARCH02 follows this bounded batch.

## Committed implementation plan

1. Recover optional identity and artwork only for an existing, readable, exactly matching offline song. Accept authoritative existing sidecar metadata or an unambiguous canonical source link corroborated by stored public queue metadata. Never infer an album/artist from filename text or choose between conflicting links.
2. Check existing Coil disk snapshots only, with the existing per-image/type/dimension/path/quota checks and same-video thumbnail variants. Skip known tiny missing-image placeholders. No new HTTP, credentials, signed-media URL persistence, audio/index/cloud schema mutation or required network. Missing cached art remains an honest placeholder.
3. Stage at most one image at a time off the UI thread, in batches of at most 24 candidates with a cooperative time budget. Recheck exact index membership, file health, metadata/link freshness and lifecycle before atomic publication under the existing index monitor; serialize sidecar removal/publication so deleting a song cannot resurrect its cover or metadata. Do not hold the index monitor while copying/decoding an image.
4. Schedule visible/selected saved items through bounded lifecycle-owned background work. Publish one optional sidecar revision after a changed identity/cover. Refresh offline queue display and selected Media3 metadata only behind generation/index/media/revision fences, preserving media ID, URI, transport, position and queue. Keep service workers/listeners owned and released. Audio readiness must not depend on artwork success.
5. Test ambiguous links, bounded selection, missing/corrupt cache and stale/deleted/replaced identity fences, source-preserving metadata updates and playback publication fences. Retain all prior regression tests and broken-caller fixtures. Run current source checks, full Windows builder, then lint; verify signer/canonical bytes, private recovery and owner-only APK hash readback. Save a stacked draft PR.
6. Keep phone cache/rendering, offline notification/current/queued art, deletion races, process/rotation, large-font/four-theme, airplane-mode and playback-soak cases NOT_RUN until actually executed. No new product rating is inferred from compilation.

Media3 permits metadata-only playlist replacement when playback-relevant properties are unchanged; see [official playlist modification guidance](https://developer.android.com/media/media3/exoplayer/playlists#modifying_the_playlist). Actual audible continuity and notification image loading still require device evidence.


## Results and limits

Committed plan `8abf0d1f`; implementation `1d7cb436f5d9a93e99150aeda12b974e9fb56a39`. Recover visible/selected existing songs through an unambiguous source link with matching stored queue metadata, or authoritative optional sidecar metadata. Same-video cached variants preserve the original known source. Conflicting links, credential-bearing keys and tiny missing-large-image placeholders are rejected. Metadata and art failures remain optional; a corrupt/missing/unavailable image cache cannot prevent known identity capture or change audio readiness. Unknown source or evicted artwork remains a placeholder; this batch does not recover art that no longer exists locally.

At most 24 candidates per call with a cooperative 2-second budget and one staged image at a time. Existing 1 MiB/image, 64 MiB total, 4096px/type/path/symlink bounds remain. Decode/copy/fsync run outside the index monitor. Exact membership, readable file, sidecar/source link and lifecycle are rechecked before atomic publication under the existing index monitor. Removal uses the same short commit monitor and does not wait for image copying. Failed/cancelled stages clean only their owned temporary image.

The Library schedules selected/current-page candidates separately from its read-only health scan. Pending work coalesces and closes with the screen; attempted IDs are bounded to the existing library capacity. The service has a separate bounded worker so artwork cannot evict index-removal reconciliation. Current offline metadata refreshes only when media ID, selected item, generation, raw index and optional sidecar revision still match. Metadata-only item replacement preserves the same media ID/URI/configuration; there is no artwork-triggered prepare, seek, play or pause. Queue display snapshots invalidate on a sidecar revision without changing queue IDs/order. Actual notification loading, audible continuity and rendering remain phone checks.

| Verification | Result / practical limit |
| --- | --- |
| Windows builder | Full Android/Compose/APK and instrumentation-source build PASS |
| JVM | **1,762 tests / 218 suites**, zero failures/errors/skips; eight added policy tests |
| Source checks | **75 PASS**, including broken-caller fixtures; not phone execution |
| Lint | **0 errors / 317 warnings / 17 hints**. Exact issue inventory unchanged; no suppression/baseline. |
| Android runtime additions | Six isolated fixture/cache/metadata test methods compile only; **NOT_RUN** |
| Preservation | **912** prior canonical inputs byte-identical; five intended existing edits, four new inputs; nine callback inventories retained |
| Source/data/feature boundaries | Audio index/cloud schemas, network/range/quality/Gateway, package/dependencies and all themes unchanged |
| Secrets | All-ref implementation-stage Gitleaks **3,544 commits**, no leaks; existing reviewed exclusions retained |
| Device/release | Eight new physical cases plus previous gates remain **NOT_RUN**; no install/reset/merge/public APK/Actions change |
| Delivery/recovery | Owner-only Drive permission and streamed **33,844,842-byte** SHA readback; private working-byte source ZIP/bundles and raw evidence |

Build **NW-debug-EF876AADD31F4B90**; canonical source SHA256 **EF876AADD31F4B90F884BEEFEB492F01C2638B639AD1724C874D4406A7EC3D3A**, **921 inputs**. APK SHA256 **C6D9EE03BC6F7D94D619DF3B6EF51646B2E36814A769C112117D6D3F0B1E7AF9**. Signer **EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394**, matching prior FINISH02. Package/version `com.nightwave.player` / `2.0.0-alpha5s` / code35, min26/target36 unchanged. Canonical source matches the implementation commit above.

Private checkpoint retains the initial Unit-return compiler failure and superseded build rejected by the source-freeze gate; only the final frozen build is delivery evidence. An interrupted lint attempt is retained separately; final sequential lint completed successfully. Previous 219-case pack is prepared again for this exact APK, without executing phone/account tasks. Latest recorded usability/visual/offline scores remain 8.4/8.8/9.0, with exact recorded APK attribution unproven; this source batch awards no new rating. **SEARCH02 is next**, then NW137–NW145. The existing hourly heartbeat remains active and runs one eligible batch at a time.
