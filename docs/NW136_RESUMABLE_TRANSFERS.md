# NW136 — measured, resumable audio transfers

## Plan recorded before implementation, 29 October 2026

Protected parent: `2f8b757c2141269245fa97389bbd7d4ff53caf8d` (NETWORK01, PR #153).
Branch: `update/nightwave-nw136-resumable-transfers`. A verified baseline Git bundle is retained privately.

The owner's recording demonstrates roughly 11–43 displayed KB/s, then deliberate cancellation. It does not identify DNS, TLS, server pacing or storage as the cause. NETWORK01 reused connections and buffered writes but still used full GET requests. The next change addresses that missing transfer strategy rather than promising an ISP line rate.

### Research and decisions

- [RFC 9110, If-Range](https://www.rfc-editor.org/rfc/rfc9110.html#section-13.1.5) and [combining parts](https://www.rfc-editor.org/rfc/rfc9110.html#section-15.3.7.3): combine only matching representations with a strong validator; validate every Content-Range. A changed validator/full 200 must never append to retained bytes. Weak ETags cannot authorize resume.
- [NewPipe's native download mission](https://github.com/TeamNewPipe/NewPipe/blob/dev/app/src/main/java/us/shandian/giga/get/DownloadMission.java) uses bounded requests; NightWave currently uses Extractor, not that downloader. Implement an independent engine, without copying its implementation or changing source/bitrate selection.
- [OkHttp 4.12 EventListener](https://github.com/square/okhttp/blob/parent-4.12.0/okhttp/src/main/kotlin/okhttp3/EventListener.kt): measure connection/header/receive/write phases without logging URLs, tokens, addresses or response bodies. Reused connections legitimately omit connection phases.
- [WorkManager lifecycle](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work): network constraints, cancellation and process stop differ. Keep durable checkpoints for interruption/Pause, remove on deliberate Cancel, and re-resolve expiring searched URLs at execution.

### Implementation order

1. Add a bounded sequential range engine (1 MiB requests), strong ETag or conservatively strong HTTP-date validation, exact range/length/type checks, identity encoding and a safe full-transfer fallback. No speculative parallel streams, provider bypass, lower bitrate or source substitution.
2. Retain only synced, hashed segments in app-private checkpoints. Bound size/count/age; validate restored bytes, canonical paths and source identity; truncate uncommitted tails. Serialize each source's writers. Checkpoints never count as Ready audio. Signed URLs are never persisted by the new store.
3. Wire all three existing workers into automatic recovery; preserve explicit searched-track Pause/Resume and distinguish deliberate Cancel. Keep original catalog digest, Android audio-only inspection, storage limits and atomic library import. Every redirect and DNS lookup retains existing restrictions.
4. Add privacy-safe transfer details, truthful resume wording and unambiguous speed units in Downloads. Preserve routes, actions, all four themes and every app feature.
5. Exercise real HTTP/file transfers in JVM tests: valid ranges, interruption/resume, corruption, changed validators, ignored Range, malformed/short/oversized responses, weak/no validators, backoff and cancellation. Run a controlled host throughput check separately from phone/provider acceptance.
6. Run the full Windows builder, JVM suites, current source gates, Android test-source compilation, lint, signing/provenance and protected-file comparison. Save exact evidence, private recovery files, owner-only APK and cumulative draft PR. Update the active backlog and next batch.

### Acceptance boundaries

The useful target is **at least 1 Mbps = 125,000 bytes/s**, not 1 MB/s. Report actual receive speed; never manufacture a minimum or claim the engine can override source capacity. Physical Wi-Fi/mobile comparisons need the exact APK, same source, completed file and airplane-mode playback. Those remain NOT_RUN without a phone.

This session implements NW136 and associated Downloads diagnosis/polish. The existing NW137–NW145 lyrics, sound, recovery, accessibility, adaptive layout and cross-device acceptance plan remains active. No new 9.5 rating is awarded from code or synthetic tests; the last visual rubric and original product baseline remain intact.

## Results

**LOCAL_VERIFIED / DEVICE_NOT_RUN.** [Draft PR #154](https://github.com/Nahian2002/NightWave/pull/154), stacked on #153. Plan commit `cb752250`; implementation `a0057c72f15f0fd3aafe4a56646fe9a5b4ed9f00`; compatibility/final built revision `d04d514f51a8b2a2554c5dc7c316302c4a656959`.

Implemented the six planned steps. The original guarded full-GET path remains available when Range is ignored, no strong validator exists, or checkpoint capacity is unavailable. A changed representation is reset; malformed ranges fail safely. MIME/magic, catalog SHA-1, Android audio-only sample inspection and verified private import remain prerequisites for Ready. Source selection/quality is byte-identical; no parallelism, provider workaround, cookie or access change was introduced.

Private checkpoints have a 250 MiB per-file limit, conservative 512 MiB aggregate reservation and eight slots. Each completed segment is flushed/synced before an atomic metadata replacement; restore checks its SHA-256 and truncates any uncommitted tail. Source identity and selected representation are hashed; signed searched URLs are freshly resolved. Resume validity is 24 hours; expired owned files are lazily removed at the next transfer, not by a background timer. Pause keeps checked segments; active deliberate Cancel discards them. API 31+ stop reasons are guarded; API 26–30 checks persisted WorkManager cancellation on the transfer thread with a two-second bound and fails closed on an unreadable result.

Manual searched-track Resume retains the existing confirmation/queue flow and creates a new WorkRequest UUID for the same unique source identity. Automatic retry/process recovery retains the original job identity. Direct/catalog transfers automatically recover eligible checkpoints on retry; their original source and permission flows remain. The final incomplete segment may be downloaded again; no unverified bytes are promoted.

Downloads shows measured KiB/s plus Mbps and omits restored-byte jumps from speed estimates. Transfer details contain fixed states and numeric protocol/status/request/byte/timing values only. Receive, write and sync are separate from audio verification/import. Timing phases are cumulative; connection includes TLS and pooled connections may show zero connection time.

### Exact local evidence

- Full Windows builder, Android/Compose/APK and instrumentation-source compilation PASS; **1,726 JVM tests / 214 suites, 72 source checks, lint 0 errors / 314 warnings / 17 hints**, zero JVM failures/errors/skips. No lint suppression or baseline. An initial candidate was superseded when lint caught unguarded API-31 stop reasons; its failed report is retained privately, and all final checks reran after the fix.
- **29 new JVM methods** run real loopback HTTP and filesystem transfers: bounded exact-byte completion, resume after interruption/Pause, corruption/invalid metadata, wrong/weak/missing validators, ignored/malformed/short/encoded responses, repeated changes, server backoff, expiry, storage, cancellation, slot capacity and lazy cleanup. Corruption recovery and interrupted tails are checked against exact original bytes. The Pause test verifies actual resumed offset, not just the existence of a metadata file.
- Controlled loopback receipt: `NW136_HOST_RECEIPT bytes=2097923 nanos=43896200 mbps=382.342526232339 scope=loopback-not-phone`. This is a small host fixture with a complete-byte comparison, not a phone/YouTube/ISP throughput result or a guaranteed minimum.
- Source preservation: **223 protected files byte-identical**, eleven intended existing offline files changed, all nine callback inventories retained. Playback, source/bitrate selection, four themes, package/build dependencies, account/storage schemas and DNS-public-address body unchanged. Installed data remains NOT_RUN.
- Build **NW-debug-CBD2E4D4E07D9BB4**; canonical source SHA256 **CBD2E4D4E07D9BB46E34E20F0929226C5355CBB984291259C55C4ECBE250225D**, **908 inputs**. APK SHA256 **2FDF1084FCF3C64E5DC94FCDC9FD3EC4B7C13DCF5DA2A16BD42ABA5A29586B07**, signer **EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394**, matching the previously delivered owner APK. No installation or account operation.
- Private checkpoint holds baseline/final Git bundles, exact working-source ZIP/readback, raw JUnit, lint, source gates, provenance/signature, original failed candidate evidence and owner-only streamed-hash delivery receipt. Public repo contains source/docs only; no binary, private media, merge or Actions change.

### Remaining physical acceptance

All original four NW136 rows and [fourteen supplemental phone cases](quality/NW136_TRANSPORT_ACCEPTANCE.csv) remain **NOT_RUN**, alongside prior gates. No phone/emulator is connected. Compare three completed same-source runs on Wi-Fi and mobile; include a controlled eligible endpoint, original problematic source, same quality, exact bytes and elapsed time, diagnostics, interruption/process/storage/cancellation, four-theme reflow, concurrent playback and airplane-mode play/seek/skip. Record failures as well as successes. The target is 1 Mbps (125,000 bytes/s); a source can still deliver less.

**Next: NW137 user-owned lyrics**, then the existing sound, recovery, performance, accessibility/adaptive and certification batches. The last recorded visual rubric remains 8.8; the original 6.7 whole-product baseline is frozen. This transport change does not establish 9.5 overall.
