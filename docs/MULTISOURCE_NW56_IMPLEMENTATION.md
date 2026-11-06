# NW56 implementation checkpoint

Baseline is the exact public v55 tag. The feature branch uses the four roadmap files from main and keeps the existing app and update lineage separate from main. The connected S24 Ultra is on v55. A fresh v55 build and source check run is in progress in the baseline checkout.

Add a separate, opt-in Sources view to Search. It will send only an explicitly submitted query to the selected providers and render results in source-specific sections. Use documented, anonymous, read-only HTTPS routes for Audius, PeerTube via Sepia Search, and Radio Browser. Validate canonical destinations before presenting external actions. In-app provider playback needs separate route and rights verification. Never expose third-party offline controls in this batch. Keep Jamendo blocked until the owner's noncommercial use, registration and app eligibility are established.

Keep state and network work scoped to the view, reject stale responses, enforce bounded transport and timeouts, and test failure isolation. Verify signed in-place upgrade and app-only S24 journeys before advancing the roadmap.

## Provider contract ledger · 6 November 2026

| Source | NW56 route and evidence | Playback, offline and cost |
| --- | --- | --- |
| Audius | Official `api.audius.co/v1/tracks/search` with a query and NightWave app name. Anonymous Windows request returned HTTP 200 and 20 tracks for `piano`. Cards name Audius and link to its track page. | The public API documents anonymous read access at lower limits. No key, bearer token or account used. Stream flags alone do not enable an in-app action. Offline unavailable. [API reference](https://docs.audius.co/api/) · [Access restrictions](https://docs.audius.co/developers/guides/gate-release-access/) |
| PeerTube | Framasoft's Sepia Search JSON endpoint returned HTTP 200 and 20 public, non-NSFW videos for `piano`. Show the publishing instance and preserve its exact HTTPS watch link. Two distinct instances are covered by fixtures. | No credentials or payments. Playback follows the originating instance in the browser or provider app. Embedding and download capabilities remain unverified per instance and recording. [PeerTube search](https://docs.joinpeertube.org/use/search) · [Sepia Search](https://sepiasearch.org/) · [Instance playback and embedding](https://docs.joinpeertube.org/use/watch-video) |
| Radio Browser | The documented `de1.api.radio-browser.info` directory returned HTTP 200 and 20 stations for `classical`. Ten had HTTPS station sites. Cards distinguish live radio from tracks. Only the station site opens. | Free directory API. Send a descriptive user agent. Do not retrieve or record station streams. One live HTTPS directory host was confirmed via DNS. Unavailability is explicit if it fails. [Directory and mirror guidance](https://api.radio-browser.info/) |
| Jamendo | Blocked. No credential configured or API call made. | Free use is limited to qualifying noncommercial apps and requires app registration. Campaign intent and eligibility are unresolved. The API restricts offline-focused use. [API terms](https://devportal.jamendo.com/api_terms_of_use) |
| Local and existing YouTube paths | The new screen does not call or merge data from those paths. | Existing behavior remains in its own Search path. No new extraction, player, download or cache. [YouTube developer policies](https://developers.google.com/youtube/terms/developer-policies) |

Discovery issues one bounded read per explicitly selected source and does not follow HTTP redirects. Results appear independently. The view keeps query and results in memory. Rate limiting is source-specific and no account, paid backend, CI, or provider subscription was added. Link-outs are not evidence of in-app playback.

## Baseline

The unmodified public tag `c569de64cdd618fb6e5593e9b6bc778903a7588e` passed a fresh Windows build, 1,870 JVM tests and 94 source checks. Android test sources compiled. The local baseline rebuild had SHA-256 `0049D410B0A3FFDAFCBAFA78A3D9C0B0A9D2447C78BFC96712BABFE559DB5D02`. The published and installed v55 APK remains the separately verified `DD0EE037E8949C7325323DE3D2BA797F50703937E5B73494F92CD7065BEFCEDE`.

## Candidate verification

- Source revision `bd307a3756ea9d5e4010757943046e56b3d54b15` descends from the public tag. Canonical source SHA-256 `06465B1773326A1581C65FEA20CDE1E877848AE9F8C7A9BF74283496C9A6B97C`.
- Build `NW-debug-06465B1773326A15`, version code 56, version name `2.0.0-alpha5s`, package `com.nightwave.player`. APK 28,241,296 bytes, SHA-256 `76D4C7897E9B1F23BBDEC7D126E0D6505EC61E977D37266543E60CD684B4C00F`. Owner-only Drive permissions and full byte and hash readback passed.
- Windows builder, 1,878 JVM tests across 236 suites, 94 source checks and sequential lint passed. Lint inventory matches v55 with 0 errors, 317 warnings and 17 hints. Android instrumentation sources compiled. No test APK installed. A targeted APK scan found no credential files or supported private-key, GitHub-token, Google-refresh-token or AWS-ID signatures.
- Device update compatibility passed. The installed v55 APK matched the published hash. S24 Ultra SM-S928B, API 37, upgraded to the exact v56 APK with the same signer `EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394`. All 33 backed-up files matched before first launch.
- The S24 showed genuine Audius tracks, PeerTube videos from two publishing instances and Radio Browser station results. Each provider's link opened an external activity. Typing without a selected provider kept submission disabled. The original local Search returned matches. Home, Search, Library, You and the paused mini-player were observed. Search history matched byte for byte. No fatal crash was observed for the test process.
- Physical coverage is scoped. Existing local import, active playback and original public search require a separate device regression. Per-provider playback, compact and large-text testing, and Android instrumentation execution remain open. NW57 and NW58 are still gated by the acceptance matrix. The GitHub release is v55 and the signed in-app feed offers v51.
