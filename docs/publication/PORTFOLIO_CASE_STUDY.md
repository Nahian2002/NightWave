# NightWave — Android music-player engineering

NightWave is an independent Android player with an original midnight interface, local audio, saved public-media references, playlists, background playback, imported synchronized lyrics, Sound Studio and optional Google Drive recovery.

## Engineering work visible in the source

- Kotlin and Jetpack Compose surfaces backed by an existing Media3 playback service; single-song search commands and explicit collection queues keep user intent distinct.
- Full supported local search snapshots, background metadata preparation, cancellation and query/revision checks, Unicode/Bangla matching and original source identity.
- Local import/download readiness, bounded recovery workflows and checks that distinguish a saved URL from a verified offline audio file.
- Optional Google authorization with separate backup/file scopes, account/consent revision boundaries and recovery previews.
- Reproducible source identity embedded in private validation builds, independent APK/source checks and preserved signing/data boundaries.
- Repository publication controls, immutable CI action references, redacted secret scanning and documented unresolved dependency/provider risks.

## Verified evidence and limits

The NW130 validation build passed 1,546 JVM tests across 195 suites, Android production/Compose and instrumentation-source compilation, debug assembly and 57 source checks. Its lint result was zero errors, with 313 warnings and 17 hints retained. The frozen 36-case synthetic public-search corpus had zero top-result errors; it does not establish live-provider recall or general catalog accuracy.

Instrumentation sources compile, but physical-device acceptance, audible playback timing, recovery/OAuth and accessibility certification remain pending. Host-JVM timing is not Android input/render performance. See [current test evidence](../../TEST_REPORT.md), [the active roadmap](../PRODUCT_QUALITY_ROADMAP.md) and [publication status](../../SOURCE_PUBLICATION.md).

## Portfolio presentation

When a reviewed public source branch is approved, pin it to the maintainer's GitHub profile and link this case study from the profile README. Demonstrate real tasks with redacted captures from an attributable build and original or authorized media; do not publish the private owner recording, account identifiers or private playlists. Describe the actual implementation decisions, validation boundaries and known limitations. Avoid catalog-size, verified-artist, platform-approval or finished-product claims without evidence.

This is a source-project case study, not an APK release or an endorsement by Spotify, Google or YouTube. Source-only publication does not require Play Store submission or a perfect product score. Licensing/privacy and GitHub hosting obligations remain separate. The current repository is private and the app is unchanged by publication preparation.
