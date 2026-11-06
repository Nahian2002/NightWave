# NW58 open music and update reliability

## Stable update

The public v55 APK is now offered through **You → Check for updates** on compatible older builds that have the updater. Manual checks run on demand. Automatic checks keep their seven-day cadence. Android asks the user to confirm installation. Builds already on v55 or higher receive no downgrade.

The stable change is separate from the candidate. It references the existing release tag, original APK and established offline feed key. The canonical URL returned the exact signed v55 envelope after GitHub cache propagation. A fresh full APK download through the client’s allowed redirect hosts matched 28,182,584 bytes and SHA-256 `DD0EE037E8949C7325323DE3D2BA797F50703937E5B73494F92CD7065BEFCEDE`. The v51 and v55 client trust anchors match. The v55 APK has package `com.nightwave.player`, code 55, min SDK 23, and the trusted app signer below. The release page, README and About now agree with the feed. [Stable feed change](https://github.com/Nahian2002/NightWave/pull/194) · [v55 release](https://github.com/Nahian2002/NightWave/releases/tag/nightwave-v55)

For future approved releases, verify the published APK’s package, higher version code, signer, exact bytes and digest. Sign and review the stable manifest with the existing offline key, then read back the canonical feed and release asset after activation. Keep review builds off the stable channel.

## Candidate behavior

- Search adds **Openverse** and includes it in **All permitted**. Requests occur only after **Search sources** is submitted. The anonymous API limits results to music and the supported license and provider filters. Existing searches, playback and local data paths remain available.
- Accept only non-mature music with supported license markers and HTTPS Jamendo track or Commons file-description pages. Results show the provider, creator where returned and license label. They open on the original site. They do not add a native stream, offline download or catalog-wide license claim.
- The live Windows piano query returned 20 records. The first Jamendo page resolved to HTTP 200. Commons decoding is covered by a fixture. The sampled live Commons music queries yielded no matching records. Direct Jamendo API activation remains separate.
- The first S24 build hit the shared six-second timeout. A phone-side HTTP probe received its first byte after 8.67 seconds. The revised build limits Openverse to 14 seconds with a 12-second read timeout. Other provider limits, cancellation and quota isolation are unchanged.
- The update checker asks the network to revalidate feed metadata and displays the release code alongside the version name. The signature, compatible version, signer and download checks remain required.

[Openverse API project](https://github.com/WordPress/openverse) · [Openverse terms](https://docs.openverse.org/terms_of_service) · [YouTube developer policies](https://developers.google.com/youtube/terms/developer-policies)

## Verification

| Item | Result |
| --- | --- |
| Build source | `73b4542a3290a13469e48989c7ad25eba362798a` |
| Build identity | `NW-debug-E216BC421C7715CF` |
| Canonical source SHA-256 | `E216BC421C7715CF24643DB24A07E01D9AB4E53CF4CA01468583DA787390749B` |
| APK | Code 58, `2.0.0-alpha5s`, 29,536,222 bytes, SHA-256 `5C7C0B5C2EBC0BECE18C8AF730F6EFD9B07B73E126637931B1751178FDB4EAE2` |
| APK signer | `EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394` |
| Windows builder | PASS, 1,888 JVM tests in 238 suites, zero failures/errors/skips |
| Current source checks | PASS, 94 |
| Android lint | PASS, 0 errors, 318 warnings, 17 hints |
| Private APK delivery | Owner-only permissions and full Drive readback matched the exact APK bytes and SHA-256. |
| Android instrumentation | Compiled, NOT_RUN |
| Physical S24 acceptance | Scoped PASS. Both in-place installs preserved 34 backed-up files before first launch. Manual update check, Library, Openverse results and paused Home passed on the revised build. |
| Public v58 release | Pending acceptance and release review |

The Windows preflight wrapper initially treated the successful ADB pull progress line as an error. The separate signature verifier compared the directly pulled v57 APK against the candidate, and the second install compared the actual first v58 build against the revised APK. Installed APKs matched the expected hashes. The revised build returned live Openverse results with license labels and the provider-page action. Search history matched byte for byte. The phone ended on Home with playback paused and zero fatal exceptions for the current process.

Pending checks include full per-provider end-to-end playback, older Android, accessibility, restore and performance/soak. An eligible older phone is still needed to observe the v55 offer, APK download and Android confirmation end to end. No quality rating change is inferred from host results.

[Draft PR #195](https://github.com/Nahian2002/NightWave/pull/195)
