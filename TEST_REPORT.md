# Current verification — NW129 complete local search

28 October 2026. Branch `update/nightwave-nw129-complete-local-search`, protected NW128 parent `684efcfbb3fd3eacf2f8a2ad4d94aff4e8592092` (PR #131).

- Complete final Windows builder: **BUILD SUCCESS**; Android production Kotlin/Compose, instrumentation-source compilation and debug APK assembly PASS.
- **1,532 JVM tests / 192 suites**, zero failures/errors/skips. Seventeen new cases cover complete parsed 10k/playlist26/>350 saved/>400 offline/5k offline boundaries, paging membership, source provenance, later duplicate metadata, Unicode/ranking parity, edits/removal, cancellation/join, failures/refresh and original streamed digest parity.
- **56/56 current source checks PASS**. Guards that encoded old Search function visibility, per-query work and caller/suggestion limits now check complete preparation and synchronous query/index/revision acceptance. No autoqueue, media/account/data, recovery, source and packaging safeguards were removed. A new positive/negative wiring guard runs in the current runner.
- Three actual-screen preference/parser/index/tap Android cases **compile only**. Their stores are unique synthetic fixture files; no real library is cleared. No device execution, actual screenshot, Android dispatcher/heap trace or phone keystroke/render distribution is claimed.
- Final sequential lint: **0 errors, 313 warnings, 17 hints**, matching NW128 totals. Two new paging-state allocation hints were corrected; no warning suppression or baseline was added.
- Exact packaged-source gate: **PASS_EMBEDDED_SOURCE_MATCH**, **826 inputs**. Build ID `NW-debug-CE8EF36E8FCD696F`; canonical source SHA256 `CE8EF36E8FCD696F0AE0FE0191D90F107C2E0950741E02A8432EA4B2F959FD1C`.
- APK SHA256: `6961547D81A2959A3F8BE6112678372BF367EDB90AC2305AA2E725B470F7CFCA`.
- Retained NW08 manifest fingerprint: `C0CC48925D7325A75D08FED4CA53F15C3A784D6DF8F64C7BEAB8A09ABD2D028E`. It uses the existing different ordering/encoding contract from the canonical source digest.
- Package `com.nightwave.player`, version `2.0.0-alpha5s`, code35, compile/target36/min26 unchanged. Signer SHA256 `EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394` independently matches the protected NW128 APK. Installed-phone comparison remains NOT_RUN.

The actual Search caller now uses complete observed snapshots, prepared background metadata, provenance labels and result paging. Strict source errors are explicit unavailable states; they do not imply an empty or repaired library. The original snapshot digest format is preserved with streaming allocation; independent old-algorithm comparisons and existing parity/recovery tests pass. The debug-only Compose test-host dependency is included; release manifest, workers and playback engine are unchanged. See [NW129 scope/contract](docs/NW129_COMPLETE_LOCAL_SEARCH.md).

Two final fresh JVM runs on 10,000 unique public / 10,600 public references / 34 playlists plus 5,000 offline metadata entries recorded 20 samples for each of seven production typing queries. Query p95 ranged **138.4–153.4ms including 120ms debounce**. Cold parse/preparation samples were approximately 1,032.2/1,145.0ms; warm samples 161.3–303.8ms. Output digests agree and all recorded measured-source hashes match. These timings exclude Android preference disk IO, IME, rendering, ART, heap/GC/jank and device acceptance; they are not an end-to-end phone result or a comparable before/after app score.

`adb devices -l` returned no connected device. No installation, account action, real download/import, user-library edit or migration occurred. All four NW129 acceptance rows are **PARTIAL**, device status **NOT_RUN**. Actual screen boundary/edit/observer tests, Android traces and 20-run input/render distributions, large type/Bangla/TalkBack, installed signer and data-preservation acceptance remain required. A new exact-APK/source-bound 219-case fixture plan is prepared, not executed.

The original 139-case matrix and frozen audit are unchanged. Broader Home/Library/Smart collection work still needs later profiling/organization; this does not certify the entire app free of main-thread work. NW126–NW128 physical blockers carry forward. The provisional score stays **6.7/10** and public release **NO-GO**. Hosted CI status is separate from these local results.

Next independent source batch: **NW130 — decidable public music results**, following the [active roadmap](docs/PRODUCT_QUALITY_ROADMAP.md) and [backlog](docs/quality/NW126_NW145_BACKLOG.json).
