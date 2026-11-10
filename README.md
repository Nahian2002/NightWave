# NightWave

**Your music, your way.** NightWave is an independent Android listening app for your own audio library, playlists, and saved music references, built with Kotlin, Jetpack Compose, and Android Media3.

[**Download public APK — NightWave 55.5**](https://github.com/Nahian2002/NightWave/releases/download/nightwave-v55.5/NightWave-v55.5.apk) · [Release notes](https://github.com/Nahian2002/NightWave/releases/tag/nightwave-v55.5) · [Exact release source](https://github.com/Nahian2002/NightWave/tree/nightwave-v55.5) · [Changelog](CHANGELOG.md)

> **Release channels are different.** The documented **public download is 55.5 (build 64)**. The signed update feed on `main` now identifies **55.7 (build 66)**, but that APK is a **restricted, owner-only GitHub asset**—not a public download. The current `main` app source is a separate development line; build and install from the matching release tag if you need 55.5. See [release and update status](#release-and-update-status).

## Why NightWave?

NightWave keeps your everyday listening essentials together without requiring a NightWave-hosted music server:

| Experience | Available capabilities |
| --- | --- |
| **Play** | Local audio playback in the background, Now Playing and mini-player, queue management, shuffle and repeat |
| **Library** | Import personal audio, liked/saved tracks, playlists, listening history, and locally organized mixes |
| **Find music** | On-device library search, public music-reference search, and optional source-specific discovery where available |
| **Offline** | Listen to imported files offline; import/download directly linked audio only when you have permission and the source permits it |
| **Make it yours** | Four visual themes, local synchronized `.lrc` lyrics, and Sound Studio audio controls |
| **Back up** | Local library-metadata export and optional Google Drive backup/recovery with user authorization |

**Important:** A public search result or saved link is **not** an owned audio file, an offline download, or permission to copy music. Availability and playback of third-party content depend on the provider and applicable rights. NightWave does not operate a centralized playback/search server.

## Install on Android

The **public NightWave 55.5 APK** is **Android build 64**, supports **Android 6.0 (API 23) and newer**, and uses package ID `com.nightwave.player`.

1. Download the [verified public GitHub release APK](https://github.com/Nahian2002/NightWave/releases/download/nightwave-v55.5/NightWave-v55.5.apk). Review its [release notes](https://github.com/Nahian2002/NightWave/releases/tag/nightwave-v55.5).
2. Open the APK on your phone and approve Android's installation prompt, including the per-app permission to install from that source if requested.
3. If updating an existing installation, **verify the signing certificate matches**. A different signature will not be a safe in-place update; never uninstall or clear app data just to force an upgrade.
4. Open NightWave, import audio you own or are allowed to use, and configure optional services only if you want them.

### Release and update status

| Channel | Version | Audience / meaning |
| --- | --- | --- |
| [**Public GitHub APK**](https://github.com/Nahian2002/NightWave/releases/tag/nightwave-v55.5) | **55.5 · code 64** | Documented public installation and reproducible release source |
| Signed [`updates/stable.json`](updates/stable.json) | **55.7 · code 66** | Points to a **private owner-only asset** requiring authorized GitHub access; **not a publicly available APK** |
| [`main` development source](app/build.gradle.kts) | **2.0.0-alpha5s · code 35** | In-progress source configuration, **not** the public 55.5 release or a safe version-code upgrade over it |

The app's **You → Check for updates** flow uses signed metadata and checks APK integrity/signing before Android installation. The 55.7 restricted flow needs authorized access; older installations may require a one-time manual bootstrap. **Do not assume the private update can be downloaded by everyone.**

For the exact public 55.5 build, use the [`nightwave-v55.5` source tag](https://github.com/Nahian2002/NightWave/tree/nightwave-v55.5), **not an arbitrary checkout of `main`**.

## What's in public 55.5?

- Clearer empty-state controls for **Sources** and search guidance in **Your Music**.
- Better library guidance for saving songs and organizing playlists.
- An Android 6.0-compatible fallback for offline volume-balancing PCM encoding.
- A Windows build configuration that supports assembly and lint in the same Gradle invocation.

The preceding [55.4 release](https://github.com/Nahian2002/NightWave/releases/tag/nightwave-v55.4) restored in-app YouTube metadata search via NewPipe with an independent Invidious fallback and added clearer search privacy messaging. See [CHANGELOG.md](CHANGELOG.md) for older releases.

## Privacy, sources, and offline rights

- **On device:** Imported files, local library data, playlists, settings, lyrics, and listening history are managed on your phone. Clearing NightWave app data or uninstalling may delete app-private content.
- **Search:** Submitting an online query contacts the selected third-party provider. Search terms, IP addresses, and request metadata may be visible to that provider. Typing suggestions can use local library/history.
- **Google Drive:** Optional, permission-based backup and recovery; Google account access is not required for basic local listening. Disconnecting an account does not remove previously uploaded Drive files.
- **Downloads:** Only save files you own or have permission to download. No blanket rights to YouTube audio, copyrighted recordings, or DRM-protected streams are implied.

Read the full [privacy information](PRIVACY.md) and [security policy](SECURITY.md). Report security concerns privately rather than posting logs, personal music, or credentials to public issues.

## Build from source

The repository includes Android app code, tests, local Windows build helpers, and documentation. The source uses **Kotlin, Jetpack Compose, Android Media3/ExoPlayer, Room, DataStore, WorkManager, OkHttp, and NewPipeExtractor**. See [the app configuration](app/build.gradle.kts) for the checked-out branch's exact versions and Android requirements.

**Windows development workflow** (from the repository root):

```powershell
# Review source contracts without installing or changing a phone.
python tools/verify_current.py

# Build and run the repository's local build checks.
.\build-windows.cmd

# Read-only APK-versus-installed-app signing/version compatibility check.
.\check-device-update.cmd
```

Use the project's configured Android SDK/JDK and the [full build and release guide](docs/BUILD_AND_RELEASE.md). `build-windows.cmd` writes reports and build output under `dist/`; its success **does not prove** an APK has the same signer as the app already installed on your phone. Run the read-only compatibility preflight before considering an in-place upgrade, and keep user data intact.

**For released 55.5 source:** check out `nightwave-v55.5` before building. **For current development:** `main` is an independent alpha source configuration (currently minimum API 26), so don't label a local `main` build as public 55.5 or install it over a higher-version-code release without an explicit, verified migration and signing plan.

[Repository navigation](docs/REPOSITORY_INDEX.md) · [Build tools](tools/README.md) · [Test evidence](TEST_REPORT.md) · [Contributing](CONTRIBUTING.md)

## Project status and future work

Routine feature development is **paused**; best-effort maintenance and release-channel work can still occur. The **NW56–NW58 multi-source documents** record an earlier zero-cost development/acceptance plan and are **not a promise of features in public 55.5**. Experimental provider work and draft pull requests must pass separate source, device, privacy, and release checks before public availability.

[Multi-source plan](docs/MULTISOURCE_NW56_NW58_MASTER_PLAN.md) · [Acceptance checklist](docs/quality/MULTISOURCE_NW56_NW58_ACCEPTANCE.md) · [S24 Ultra test runbook](docs/MULTISOURCE_S24U_DEVICE_QA.md) · [Verification summary](TEST_REPORT.md)

## Contributing and license

Bug reports and narrow fixes are welcome; see [CONTRIBUTING.md](CONTRIBUTING.md). Please redact private paths, accounts, tokens, music, and device logs. NightWave is published under the [GNU GPL v3](LICENSE); third-party services and media retain their own terms and rights.
