# NW148 — Code43 phone findings and repair checkpoint

4 November 2026. Owner explicitly approved **“Install version43 and test.”** The connected Samsung SM-S928B reports API37,1440×3120,density560 override,font0.8,AMOLED. Actual installed code42 was pulled and matched, signer continuity verified, then code43 installed in place. Pulled installed APK matches **04FD53AE47B19B858212D07B80ED10C5FEC3541944D2EBDF4541B170B9BA9A7B**,28,101,622 bytes. See [exact source identity](quality/NW148_SOURCE_EVIDENCE.json).

All **29 collected preference/database hashes were unchanged** immediately before/after installation, before first launch, with the app stopped. This is a scoped snapshot, not complete-file/migration certification. Normal launch can update runtime state. First cold launch reported1,196ms in one ADB sample, not a fully-drawn percentile.

## What the actual phone established

- Rectangular sizing works: stable full-player measurement/request **1073×600**; an earlier pre-aspect frame measured/requested1073×805. Debug reports show actual bitmap **168×94**, not an enlarged decoder output. Disk, memory-cache and a later network load of the existing source all produced this small original.
- The stored thumbnail is on the expected public image host and its video ID matches the canonical track. It contains the public thumbnail metadata query keys sqp/rs. Current policy correctly preserves queried source addresses, so it does not try the existing unsigned large-still candidates for this case.
- A bounded, same-video public-image probe confirmed **1280×720**,640×480 and480×360 originals are available. Raw URLs, video identity, image bytes, hashes, screenshots and queue records remain private. This disproves a blanket claim that no sharper legitimate source exists. **The intended sharpness outcome is not complete.**
- Same selected artwork was compared before/after installation. Code43 screenshots initially showed only the ambient full-player background, while fresh UI trees contained controls at normal bounds. Back returned to a normal Home screen. One normal force-stop/relaunch and reopen produced a normal full player with controls. Cause unknown; the owner had not physically checked the earlier blank state. Do not call this a crash, permanent regression, resolved defect or accessibility pass. Retain the anomaly for controlled lifecycle/frame review.
- The current-PID crash buffer and final paused state are recorded separately in private evidence. No sound/library/account/system preference was changed, no cache or installed data cleared, no test APK installed and no source/recording selected by the test was changed.

The earlier code42 UI-tree bounds1057×591 include visual scaling; they are not a direct measurement of the old decode request. For the new measured1073×600 viewport, the former square policy would request600×600. Old decoded pixels were not measured. This corrects the source-only checkpoint's591×591 inference from rendered bounds.

## Required bounded repair before NW149

1. Add a separate canonical-track-bound artwork plan. Only a strict canonical public watch ID matching the known HTTPS thumbnail host/path may authorize same-video unsigned still candidates for public sqp/rs thumbnail metadata. Preserve the stored signed URL exactly as fallback; do not strip credentials or reinterpret arbitrary queries, mismatched IDs, local/offline or third-party sources. Keep attempts bounded and every audio URL/quality/data format unchanged.
2. Add negative identity/query/host/path tests and an actual synthetic Coil case showing the bound high-resolution source is selected. Retain existing queried-address preservation tests for calls without explicit canonical binding.
3. Repeat complete current checks, full Windows builder then sequential lint, signer/source/APK/private-delivery verification and scoped device receipt. A new APK requires its own explicit installation approval; code43 approval does not cover a new build/version.
4. Repeat player open/Back/reopen observations with settled screenshots and frame/lifecycle evidence. Diagnose the intermittent capture anomaly before changing animations, blur or layering; physical confirmation may still be needed. Do not apply speculative settings or declare9.5.

## Budget and honest status

At the repair admission check, **22% five-hour /73% weekly remained**. Conservative complete remaining repair estimate18 points plus5 reserve requires23; it does not fit. This is a project estimate under the owner's saved completion-budget rule, not an OpenAI technical minimum. No reset credit was consumed and no automation restarted. Preserve the review candidate, raw evidence and committed repair plan; do not begin underfunded implementation or omit validation. Re-read both windows and re-estimate on resume, increasing for unresolved rendering complexity.

Code43 host checks remain verified:1,828 JVM/228 suites,90 source checks, full builder and sequential lint0 errors/327 warnings/17 hints. They were not rerun for this documentation-only evidence. Four Android artwork cases remain COMPILED_NOT_RUN; original219 matrices stay unpromoted. **NW148 remains NEEDS_REPAIR; next source batch is NW148 repair, then NW149.** No rating increase or release approval. Phone left **Home, existing selection paused**; approval covers code43 only.

[Draft review #174](https://github.com/Nahian2002/NightWave/pull/174) remains unmerged and is not a release-ready certificate.
