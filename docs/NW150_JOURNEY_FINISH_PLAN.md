# NW150 — Search, offline and lyrics journey finish

5 November 2026. Parent `3cde02622c3425b6d92315838fe5fdd21e32e8ba`, cumulative review branch `update/nightwave-nw150-journey-finish`. Private verified recovery bundle: `20261105-nw150/before.bundle` outside Git. Clean checkout and remote parent agree; no other batch is active.

Admission: ordinary usage allowed, five-hour remaining97%, weekly59%. The previously recorded conservative35-point full-completion estimate plus9 reserve fits. Recheck during implementation and before validation/delivery. No reset credit or schedule action.

## Inspection and intended changes

1. Preserve exact on-device search identity. Current Search passes only the title into On device; duplicate titles can show different files. Carry an optional stable entry ID through the private destination, retain it across recreation, clear it on deliberate query editing, and never substitute another file or auto-play. Refresh destination filters and show actionable missing-entry guidance.
2. Distinguish explicitly cancelled/background-paused public search from provider failure. Keep generation/socket cancellation, existing results, explicit retry/refresh, all version URLs, local search and YouTube fallback. Add a neutral stopped state with an accessible retry action; avoid an error claim for a deliberate stop.
3. Clarify owned-LRC follow versus playback controls, preserve import/review/editor/export/removal and binding checks, and make narrow/large-font follow actions wrap rather than crowd. Validate fixture-only import/editor/follow guidance; do not import into personal data or fetch unauthorized lyrics/audio.
4. Inspect offline readiness/import/error and transfer-phase presentation. Preserve distinct receive/write/sync evidence and supported sources; no artificial throughput or universal speed claim. Add fixes only for demonstrated gaps and retain existing retry/recovery controls.

## Device control

The connected physical Samsung SM-S928B reports Android17/API37 and installed code43. Normal launch and a fresh in-app hierarchy succeed; an initial launcher dump could not get idle state. Verify a uniquely observed in-app navigation tap before choosing further automation. Existing app navigation intents remain available. If Samsung input works, defer the unneeded MIUI instrumentation pilot; do not add a production control endpoint or install a helper. No new app/test APK installation is authorized by this plan or the connection request. Prepare exact verified candidate and draft review/private delivery before asking for the required version-specific installation approval.

## Verification and completion

Add meaningful duplicate-title/missing-ID/recreation and stopped-search regressions, preserve existing request cancellation/source/version tests, and compile fixture-only UI cases for changed controls. Run all current source checks, full Windows builder then sequential lint; compare warning inventory, exact source/APK/signature/native entries. Keep raw phone data and logs private. Record exact identity and NOT_RUN device gaps, deliver only through verified owner-only Drive with complete streamed byte/hash readback, create and attach a draft PR. Update active roadmap/backlog/test report with next NW151. No merge, release/feed activation, binary publication, settings/account/audio/library mutation, reset/uninstall or automatic rating increase.
