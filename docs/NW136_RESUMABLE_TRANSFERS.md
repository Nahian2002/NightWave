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

Implementation and final validation pending.
