# NW122 — measured search and library performance

27 October 2026. Parent: verified NW121 `10ea25315d9746ebbde463ef05067b50798c9614`. Branch: `update/nightwave-nw122-measured-performance`.

## Implemented behavior

- Saved search prepares display/Unicode identities once per invocation, reusing them across strict, curated-alias and last-resort typo passes. It never retains a global library/query cache. An edited or removed song is evaluated from the new snapshot immediately.
- Token evidence selects the best unused occurrence in one scan without temporary singleton lists or three repeated grade scans. Exact, safe prefix and optional single-edit Latin priority, first-equal occurrence and repeated-query-word requirements remain unchanged. Bangla vowel marks stay meaningful.
- Public-result ranking computes each song's score once before sorting. Original provider metadata, stable ties, duplicate entries and URLs remain intact.
- Library title/creator sorting prepares sort keys once per matched row. Saved order, descending-title stable ties, creator/title ties, view filtering and underlying playlist order remain unchanged.
- Search suggestions reuse that query's saved matches through 5,000 rows. Larger libraries still search all saved songs for the main results but independently use the first 5,000 for suggestions, retaining the previous boundary. Search stays on `Dispatchers.Default`, with existing 120ms typing debounce, cancellation and clear-before-suspend behavior.
- Current preflights now check invocation-local preparation, same-query suggestion reuse and both sides of the 5,000-row boundary. No safeguard, test floor, Unicode/alias fixture or URL-deduplication guard was removed.

No library, music, history, backup, signing, account, media-service or database schema migration is introduced. No tracked file was proven unwanted by this wave. Existing NW119–NW121 fixes are retained.

## Measurement protocol

`NW122SearchBenchmark` compiles against the actual production Kotlin classes through the existing Android Gradle unit-test runtime. It is a separate opt-in JavaExec entry point, never a timing assertion in the correctness suite. No synthetic reimplementation of the optimized algorithms is timed.

The deterministic dataset contains 5,000 public-reference labels across English/Bangla, 40 creators and five title forms. Six saved queries cover direct English/Bangla, repeated words, curated Romanization, typo and no-match fallback. Public ranking uses 150 entries; two library scenarios sort 5,000 entries. Every scenario performs 12 untimed warmups and 40 timed samples per fresh JVM. Three independent JVMs per version run sequentially with `-Xms512m -Xmx1024m` on the same Windows host/JDK. Result hashing and source/harness hashes are outside timed sections.

The baseline is a separate detached checkout of the exact NW121 parent, with only the identical benchmark harness/init script copied in. Production source is unchanged there. Candidate and baseline receipts record actual source-file hashes, runtime, heap, dataset, ordered-output hash and individual median/p95/max. The comparison rejects changed environment, harness, source within a group, scenario/sample/result counts, invalid timings or ordered-output differences. Timing thresholds are deliberately not correctness gates.

The summary uses the **median of the three process p95 values**, not a pooled p95. This host-JVM microbenchmark excludes app startup, Compose frames, the debounce, input-to-display latency, actual network/provider calls, audio decoding, Android ART and thermal/battery conditions. Small changes may be noise; it cannot establish device speed or Spotify parity. Allocation reductions are implementation observations, not measured total heap/battery savings.

### Recorded host-JVM result

Windows 11 amd64, OpenJDK 21.0.8+9-LTS, 32 reported processors. Three fresh JVMs per version; 120 timed samples per scenario/version. Values below are the median of three process p95s in milliseconds.

| Scenario | NW121 | NW122 |
| --- | ---: | ---: |
| saved:moonlight | 24.17 | 26.14 |
| saved:ভালোবাসা | 22.86 | 23.03 |
| saved:alo alo | 23.06 | 24.09 |
| saved:valobasha | 66.04 | 32.11 |
| saved:midnigth | 57.81 | 27.81 |
| saved:missingword | 49.66 | 28.13 |
| public:moonlight | 29.66 | 6.13 |
| library:title | 124.91 | 11.37 |
| library:creator | 187.94 | 14.83 |

Fallback/alias search, public ranking and both large-library sorts improve materially in these runs. Direct English/Bangla/repeated-word search p95 is slightly higher in the candidate (up to about 2ms here); no direct-search improvement or blanket performance win is claimed. Device acceptance determines real listener impact.

Ordered URL/title output SHA256: `9e5882625f577b18ae5c2ea81548bde5809d5566bbac56f226f2291681b339c2`; identical across all six processes. Harness SHA256: `4e9d6df36d3454b8c4641b6f2d82845075b28da656b711703961d748540cbc57`.

Raw receipts: [before 1](measurements/nw122/nw122-before-1.json), [before 2](measurements/nw122/nw122-before-2.json), [before 3](measurements/nw122/nw122-before-3.json), [after 1](measurements/nw122/nw122-after-1.json), [after 2](measurements/nw122/nw122-after-2.json), [after 3](measurements/nw122/nw122-after-3.json). The [comparison](measurements/nw122/comparison.json) retains each process-p95 range and median-of-medians; individual source hashes are in each raw receipt.

These small committed JSON files contain only synthetic benchmark evidence. Build logs/APKs/caches stay outside Git.

### Reproduce

In each complete checkout with the same JDK/SDK and harness, run the installed Gradle 8.11.1 executable (or a compatible configured Gradle):

```powershell
gradle --no-daemon --console=plain -I tools/nw122-benchmark.init.gradle :app:benchmarkSearch '-PperformanceReceipt=build/nw122-before'
# Candidate: use build/nw122-after as the receipt prefix.
python tools/compare_nw122_performance.py --self-test
python tools/compare_nw122_performance.py --before build/nw122-before-1.json build/nw122-before-2.json build/nw122-before-3.json --after build/nw122-after-1.json build/nw122-after-2.json build/nw122-after-3.json --output build/nw122-comparison.json
```

Keep other builds/profilers idle during measurements. Preserve each raw JSON and log; do not substitute initial exploratory runs for the final matching-harness comparison.

## Android verification and artifact identity

- Complete one-click Windows builder: **BUILD SUCCESS**. Real production Kotlin/Compose, Android instrumentation-source compilation, debug assembly and **1,464 JVM tests in 184 suites** pass with zero failures/errors/skips.
- Final Android lint: **PASS — zero errors, 313 warnings, 0 hints**. No new suppression or warning baseline was introduced.
- **50/50 current source/negative/Windows checks pass**, including the new comparator self-test. Existing Android account-store tests compile only; no device instrumentation execution is claimed.
- Independent source attestation: **795 build-affecting files match**; fingerprint SHA256 `6AB4E8527E42D9D9221E27368E094390737ECE7D18F64A69558AC5537A63D1E7`.
- Debug APK SHA256 `AEC2B9599974381DDA588E1E38B661BE273B4677098E9E3AAA63EF764A662E0D`.
- Certificate SHA256 `EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394`; independent comparison against the saved NW121 APK: **MATCH**. Installed-phone continuity remains untested.
- Package `com.nightwave.player`, version `2.0.0-alpha5s`, code `35`, compile/target API 36. App identity/key, account/library formats, music, Room schema and existing consent/recovery guards remain unchanged.
- Working checkout: `C:\Users\jayed\NightWave-work`. Checkpoint: `C:\Users\jayed\NightWave-checkpoints\20261027-nw122-performance`. A baseline Git bundle was saved before edits. The full source ZIP preserves actual working-tree bytes with per-file SHA256/CRC verification; Git's LF-normalized archive bytes would not match the Windows compilation receipt.

## Phone acceptance still required

NW122 remains device-pending. No cold/warm startup, playback-next latency, Android allocations, frame jank, battery metric or 30-minute playback soak was collected by this update. Do not fill these with host-JVM numbers.

1. Use a disposable validation profile and preserve the installed signer/data. Record phone model, OS, app/source hash, library size, storage, route, power mode and temperature for both builds. Never uninstall/clear a listener's app to create a baseline.
2. Capture at least 20 cold launches and 20 warm resumes separately. A cold launch must begin with the validation app stopped; do not interrupt active listening. Record full-draw time as well as Android launch timing; `am start -W` alone cannot prove the music library is ready.
3. Replay the same local search queries against 0/500/5,000/10,000-row disposable libraries; record keystroke-to-correct-results p50/p95 and trace main-thread frames. Include queries changed rapidly, removal/rename during typing, rotation and app backgrounding. Confirm there is no stale-query result and suggestions retain their bound.
4. Measure at least 100 explicit Next actions from intent to audible start on permitted local audio, separating cache/warm decode and output route. Confirm queue order/history/Undo, EQ, lyrics, notification and headset controls still follow the selected song. Provider/network time belongs in a separate scenario.
5. Capture an Android Studio/Perfetto trace for startup, large-library browsing and rapid search/skip. Record allocations/GC, main-thread blocking, frame-time/jank distributions and retained memory after closing search. Do not infer jank from host timings.
6. Run a 30-minute screen-off playback soak, then a separate foreground interaction run. Record interruptions, crashes/ANRs, wrong-track state, memory trend, battery/CPU and thermal conditions. Compare equivalent routes/volume/power/temperature; retain raw evidence and explicit PASS/FAIL per acceptance case.

The next ordered source wave is **NW123 accessibility**. Physical performance and earlier Google/restore/signer/rights/release gates remain open. Public release remains NO-GO.
