# NightWave

**Your music, your space.** NightWave is an independent, local-first Android music app for listening to your own audio, organizing a personal library, and discovering public music references. Built with **Kotlin**, **Jetpack Compose**, and **AndroidX Media3**.

**Public release: 55.5** · Android **6.0+** (API 23+) · Package `com.nightwave.player` · **GPL-3.0-or-later**

[**Download NightWave 55.5 APK**](https://github.com/Nahian2002/NightWave/releases/download/nightwave-v55.5/NightWave-v55.5.apk) · [Release notes](https://github.com/Nahian2002/NightWave/releases/tag/nightwave-v55.5) · [Exact released source](https://github.com/Nahian2002/NightWave/tree/nightwave-v55.5) · [Verification](TEST_REPORT.md)

## Get started

1. Download the **public 55.5 APK** from the link above (Android version code **64**, target API **36**).
2. Open it on your Android device and approve the installation through Android's installer.
3. Import audio you own or are permitted to use, then explore **Home**, **Search**, **Library**, **Offline music**, **Now Playing**, and **You**.

**Upgrading an existing installation?** Preserve your library and settings. Install over the existing app **only if the signing certificates match**; do not uninstall, clear app data, or force a different-signer APK over your music. The Windows [read-only device compatibility checker](check-device-update.cmd) is available when building locally.

### Public download vs. in-app updates

- **55.5 (build 64)** is the latest **public APK documented here**, with a matching [source tag](https://github.com/Nahian2002/NightWave/tree/nightwave-v55.5).
- As of **10 November 2026**, the repository's signed [update manifest](updates/stable.json) points to **55.7 (build 66)**, an **owner-only/private** asset. It is *not* a public 55.7 download, and a public installation may not be authorized to retrieve it.
- For a reliable public download, use the **55.5 GitHub release** above rather than assuming **You → Check for updates** will offer a publicly accessible APK. Updates require verified metadata, matching APK bytes, and compatible signing identity.

## Features in the public release

| Experience | What NightWave offers |
| --- | --- |
| **Playback** | Background playback for local audio, Now Playing and mini-player controls, queue, shuffle, and repeat |
| **Personal library** | Import your own audio; organize playlists, liked/saved items, listening history, and local mixes |
| **Discovery** | Search for public music references, including YouTube metadata search using NewPipe with an independent fallback; availability depends on third-party services |
| **Offline music** | Keep imported audio on-device; use permitted local or direct-link audio workflows only where you have the necessary rights |
| **Sound & lyrics** | Sound Studio and synchronized lyrics from your own imported LRC files |
| **Appearance** | Four themes: **Midnight**, **Aurora**, **Pearl**, and **AMOLED** |
| **Backup & recovery** | Local library-metadata export and optional, permission-based Google Drive backup/recovery |

NightWave is **not a licensed music-streaming catalog**. Search results and saved public links are references, not a license to download, copy, or redistribute third-party recordings. NightWave is not affiliated with Spotify, YouTube, Google, or other media providers. See [media and source-use information](TERMS_AND_SOURCE_USE.md).

## What's new in 55.5

- Improved the empty-state guidance for **Sources** and local Library search, and clarified how likes and playlists keep music easy to find.
- Preserved a compatible **Android 6.0** PCM default for offline volume balancing.
- Repaired the Windows Gradle configuration so assembly and lint can run together.

For the complete release history, see [CHANGELOG.md](CHANGELOG.md) and the [55.5 release notes](https://github.com/Nahian2002/NightWave/releases/tag/nightwave-v55.5).

## Build from source

To reproduce the **public 55.5 build**, use its **matching release tag**, not an arbitrary default-branch checkout. The `main` branch also hosts release/update metadata and may contain different development configuration.

**Windows:** use a fresh, writable checkout with the Android SDK, a suitable JDK (Java 17 target), and the repository's build tooling.

~~~powershell
git clone --branch nightwave-v55.5 --depth 1 https://github.com/Nahian2002/NightWave.git
cd NightWave
python tools/verify_current.py
.\build-windows.cmd
~~~

After building, examine the generated `dist/NightWave-build-status.txt`, test receipts, APK identity and signature. Before installing a locally built APK over an existing app, connect one authorized phone and run `.\check-device-update.cmd`. **A locally built/debug-signed APK is not automatically an in-place upgrade for the publicly signed release.**

For prerequisites, build troubleshooting, release provenance, and test expectations, see the [build guide](docs/BUILD_AND_RELEASE.md), [repository guide](docs/REPOSITORY_INDEX.md), and [verification guide](docs/VERIFICATION_MATRIX.md).

## Quality and current status

The public release has documented build, JVM/source/lint checks, and **scoped Samsung Galaxy S24 Ultra observations**. These do **not** establish exhaustive testing across Android versions, screen sizes, audio routes, accessibility modes, long-duration playback, Google Drive recovery, or every third-party endpoint.

- [Test evidence and remaining coverage](TEST_REPORT.md)
- [Release readiness and open checks](RELEASE_READINESS.md)
- [Maintenance QA record in the 55.5 source tag](https://github.com/Nahian2002/NightWave/blob/nightwave-v55.5/docs/NIGHTWAVE_55_4_MAINTENANCE_QA.md)

**Routine feature development is paused.** The [post-v55 multi-source plan](docs/MULTISOURCE_NW56_NW58_MASTER_PLAN.md), [acceptance plan](docs/quality/MULTISOURCE_NW56_NW58_ACCEPTANCE.md), and separate experimental pull requests describe ongoing or planned work; they **are not features promised by the public 55.5 APK**. In particular, do not assume Audius, PeerTube, Radio Browser, or Openverse experiments are included in this release.

## Privacy, support, and contributing

NightWave stores imported audio and library information on your device. Submitted online searches, optional source links, update checks, and Google Drive features contact their respective third-party services; they may receive request information. Google Drive connection is optional and requires consent. Read the [Android privacy information](PRIVACY.md).

If something breaks, include the **NightWave version**, Android version, a short reproduction, and **redacted** logs; never post personal audio, account tokens, private Drive contents, or signing keys publicly.

[Support & contribution guide](CONTRIBUTING.md) · [Private vulnerability reporting](SECURITY.md) · [Source and media rights](TERMS_AND_SOURCE_USE.md) · [License](LICENSE) · [Repository navigation](docs/REPOSITORY_INDEX.md)

---

*NightWave is an independent project. The GPL license covers the applicable software source, not third-party music, artwork, lyrics, or service permissions.*
