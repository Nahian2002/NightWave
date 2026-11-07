# NightWave

An independent Android music player for your own audio and saved music references. Built in Kotlin with Jetpack Compose and Media3.

**[Download NightWave 55.3 for Android](https://github.com/Nahian2002/NightWave/releases/download/nightwave-v55.3/NightWave-v55.3.apk)** · [Release notes](https://github.com/Nahian2002/NightWave/releases/tag/nightwave-v55.3) · [Matching source](https://github.com/Nahian2002/NightWave/tree/nightwave-v55.3) · [Build guide](docs/BUILD_AND_RELEASE.md)

## Get NightWave

NightWave 55.3 (Android build 62) requires **Android 6.0 or newer**. Download the APK above for a first install or to upgrade manually. Install over an existing copy with the matching signing certificate to keep your library and settings. Android asks you to confirm the installation.

**You → Check for updates** uses the signed in-app channel, which currently offers [NightWave 55.3](https://github.com/Nahian2002/NightWave/releases/tag/nightwave-v55.3).

## What’s new in 55.3

- A fresh Home shows six mood searches, with shortcuts to Search and Offline music.
- **Find music** shows results inside NightWave from your library, Audius, PeerTube and Openverse. Saved matches and word suggestions stay on-device.
- **Search sources** separately searches Audius, PeerTube, Radio Browser and Openverse.
- First-time Google Drive backup works from a fresh profile. Offline music supports importing FLAC files.
- Search stays in NightWave. Saved track links play when a verified local copy is available. Offline audio can be imported or obtained from supported sources.

## What you can do

| Area | Features |
| --- | --- |
| Listen | Background playback for local audio, queue controls, shuffle, repeat, mini-player and Now Playing |
| Organize | Import your own audio, build playlists, save tracks and browse listening history |
| Discover | Browse local mixes and search for public references where sources are available |
| Customize | Four themes, synchronized lyrics from your own LRC files, and Sound Studio |
| Back up | Export library metadata locally or use optional Google Drive recovery |

Saved public links are references. Offline files and third-party media require the appropriate rights.

## Project roadmap

[Multi-source roadmap](docs/MULTISOURCE_NW56_NW58_MASTER_PLAN.md) · [Acceptance record](docs/quality/MULTISOURCE_NW56_NW58_ACCEPTANCE.md) · [Device test runbook](docs/MULTISOURCE_S24U_DEVICE_QA.md)

The `nightwave-v55.3` tag identifies the exact released source. The default branch hosts the signed stable update feed.

## Development

Version 55 passed the Windows build, 1,870 automated tests and 94 source checks. The in-place upgrade, support draft, App details and Search completion were checked on an S24 Ultra. Broader device coverage remains open. [Release verification](TEST_REPORT.md)

Build a fresh checkout with `build-windows.cmd`. Run `check-device-update.cmd` before installing over an existing build. For the public APK's exact source, use the release tag. The default branch hosts the project entry points and stable update feed.

[Repository guide](docs/REPOSITORY_INDEX.md) · [Contributing](CONTRIBUTING.md) · [Security](SECURITY.md) · [Android privacy information](PRIVACY.md) · [License](LICENSE)
