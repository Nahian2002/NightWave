# Current verification — NW127 compact visual hierarchy

28 October 2026. Branch `update/nightwave-nw127-compact-hierarchy`, based on protected NW126 parent `0b21056456fb48017545909caf53c757d7b918c5` (PR #129).

- Complete Windows builder: **BUILD SUCCESS**. Production Kotlin/Compose, Android instrumentation-source compilation and debug APK assembly pass.
- **1,502 JVM tests / 189 suites**, zero failures/errors/skips. Six new methods cover narrow/large-text action reflow, malformed measurements, bounded shelf growth, Continue minimums, opaque-caption palette contrast and preserved text/touch sizes. Existing adaptive tests retain their assertions against the revised minimums.
- **54/54 current source checks PASS**. NW52 now checks disclosed menu callbacks, separate Play, caller removal wiring and its confirmation. NW59 still checks music-before-discovery ordering and rejects its missing-action fixture. Existing account, playback, recovery, integrity and provenance guards remain.
- **180 new parameterized Android component cases compile only**: five cases across 320/360/412dp, 1×/2×/3× text and four themes. They cover navigation bounds/labels/callbacks, recent disclosure, section reflow, mix captions and Continue/Settings. Existing recent/Search instrumentation sources were updated for the new flow. No device execution or generated screenshot is claimed.
- Android lint: **0 errors, 313 warnings, 17 hints**, unchanged totals; no new suppression or baseline.
- Independent packaged-source gate: **PASS_EMBEDDED_SOURCE_MATCH**, **811 inputs**. Build ID `NW-debug-343760FC90345D51`; canonical source SHA256 `343760FC90345D518CBC1D978231391CC28883FF929A45F0DB47DF802224893B`.
- APK SHA256: `4F9B232F42A8CEC025A0A623A40BDE954F872C34422D9E5F598B4F2AB9B999D6`.
- Retained NW08 source-manifest fingerprint: `A8A902FD558D36E0A5FEFE10E4319C6EF39D62C9350574724A6C515FB1272CBD`. This manifest-file hash and the canonical digest have different documented ordering/encoding contracts.
- Package `com.nightwave.player`, version `2.0.0-alpha5s`, code35, compile/target36/min26 preserved. Certificate SHA256 `EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394` matches the saved prior APK. Installed-phone comparison remains NOT_RUN.

The delivered UI consolidates Home Continue, removes redundant Home summaries and Library shortcut rows, preserves a stable Create action, moves recent removal behind overflow, separates mix/mood captions from artwork and shares page spacing/type/reflow/chrome rules. Minimum touch targets and system font scaling remain. See the [NW127 contract and before/after description](docs/NW127_COMPACT_VISUAL_HIERARCHY.md).

`adb devices -l` returned no connected devices. All four NW127 acceptance rows are **PARTIAL** for local delivery with missing physical proof; device status is **NOT_RUN**. Matched full Home/Search/Library/You screenshots, rendered contrast, actual touch bounds, large-text overflow, TalkBack and selected-song/callback acceptance remain required. The synthetic component matrix cannot replace those checks.

NW126 [identity/baseline evidence](docs/NW126_BUILD_IDENTITY_BASELINE.md) and pending installed attribution/signer/data-preservation checks carry forward. No installation, account operation or user-library mutation occurred. The original 139-case acceptance matrix remains unchanged; the extended backlog remains a separate 80-case ledger.

The original [product score](docs/audits/2026-10-28_PRODUCT_QUALITY_AUDIT.md) remains **6.7/10** pending comparable fresh product evidence. Performance, actual restore, offline/route/soak, accessibility and applicable release approvals remain open; public release stays **NO-GO**. Hosted CI status is recorded separately on the PR and must not be inferred from the local PASS.

Next independent source batch: **NW128 — unified shell and offline navigation**, retaining NW126/NW127 physical blockers. See the [active roadmap](docs/PRODUCT_QUALITY_ROADMAP.md) and [backlog](docs/quality/NW126_NW145_BACKLOG.json).
