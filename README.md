# NightWave

An independent Android music player for your own audio and saved music references. Built in Kotlin with Jetpack Compose and Media3.

**[Download NightWave 55.5 for Android](https://github.com/Nahian2002/NightWave/releases/download/nightwave-v55.5/NightWave-v55.5.apk)** · [Release notes](https://github.com/Nahian2002/NightWave/releases/tag/nightwave-v55.5) · [Matching source](https://github.com/Nahian2002/NightWave/tree/nightwave-v55.5) · [Build guide](docs/BUILD_AND_RELEASE.md)

## Get NightWave

NightWave 55.5 (Android build 64) requires **Android 6.0 or newer**. Download the APK above for a first install or to upgrade manually. Install over an existing copy with the matching signing certificate to keep your library and settings. Android asks you to confirm the installation.

**You → Check for updates** uses the signed in-app channel, which currently offers [NightWave 55.5](https://github.com/Nahian2002/NightWave/releases/tag/nightwave-v55.5).

## What’s new in 55.5

- The empty Sources view shows its provider controls. Your Music keeps the local search prompt.
- Library copy explains how to keep songs handy with likes and playlists.
- Offline volume balancing uses a compatible PCM default on Android 6.0.
- Windows assembly and lint can run in the same Gradle invocation.

## What you can do

| Area | Features |
| --- | --- |
| Listen | Background playback for local audio, queue controls, shuffle, repeat, mini-player and Now Playing |
| Organize | Import your own audio, build playlists, save tracks and browse listening history |
| Discover | Browse local mixes and search for public references where sources are available |
| Customize | Four themes, synchronized lyrics from your own LRC files, and Sound Studio |
| Back up | Export library metadata locally or use optional Google Drive recovery |

Saved public links are references. Offline files and third-party media require the appropriate rights.

## Project status

Routine feature development is paused. The release and test evidence below describe the current public build.

[Multi-source roadmap archive](docs/MULTISOURCE_NW56_NW58_MASTER_PLAN.md) · [Acceptance record](docs/quality/MULTISOURCE_NW56_NW58_ACCEPTANCE.md) · [Device test runbook](docs/MULTISOURCE_S24U_DEVICE_QA.md)

The `nightwave-v55.5` tag identifies the exact released source. The default branch hosts the signed stable update feed.

## Development

The [maintenance QA report](https://github.com/Nahian2002/NightWave/blob/nightwave-v55.5/docs/NIGHTWAVE_55_4_MAINTENANCE_QA.md) records the fixes and device coverage. [Release verification](TEST_REPORT.md)

Build a fresh checkout with `build-windows.cmd`. Run `check-device-update.cmd` before installing over an existing build. For the public APK's exact source, use the release tag. The default branch hosts the project entry points and stable update feed.

[Repository guide](docs/REPOSITORY_INDEX.md) · [Contributing](CONTRIBUTING.md) · [Security](SECURITY.md) · [Android privacy information](PRIVACY.md) · [License](LICENSE)
