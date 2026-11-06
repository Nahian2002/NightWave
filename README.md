# NightWave

An independent Android music player for your own audio and saved music references. Built in Kotlin with Jetpack Compose and Media3.

**[Download NightWave v55 for Android](https://github.com/Nahian2002/NightWave/releases/download/nightwave-v55/NightWave-v55.apk)** · [Release notes](https://github.com/Nahian2002/NightWave/releases/tag/nightwave-v55) · [Matching source](https://github.com/Nahian2002/NightWave/tree/nightwave-v55) · [Build guide](docs/BUILD_AND_RELEASE.md)

## Get NightWave

Version 55 requires **Android 6.0 or newer**. Download the APK above for a first install or to upgrade manually. Install over an existing copy with the matching signing certificate to keep your library and settings. Android asks you to confirm the installation.

**You → Check for updates** uses the signed in-app channel, which currently offers [version 51](https://github.com/Nahian2002/NightWave/releases/tag/nightwave-v51).

## What’s new in v55

- Find music and YouTube are easier to reach in Search. Recent matches use on-device history.
- The You page groups settings, and App details puts the version and common actions first.
- **You → App & support → Email support** opens a problem report draft you can review before sending.
- Mix previews open directly from Home and preserve your place when you close them.

## What you can do

| Area | Features |
| --- | --- |
| Listen | Background audio, queue controls, shuffle, repeat, mini-player and Now Playing |
| Organize | Import your own audio, build playlists, save tracks and browse listening history |
| Discover | Browse local mixes and search for public references where sources are available |
| Customize | Four themes, synchronized lyrics from your own LRC files, and Sound Studio |
| Back up | Export library metadata locally or use optional Google Drive recovery |

Saved public links are references. Offline files and third-party media require the appropriate rights.

## Development

Version 55 passed the Windows build, 1,870 automated tests and 94 source checks. The in-place upgrade, support draft, App details and Search completion were checked on an S24 Ultra. Broader device coverage remains open. [Release verification](TEST_REPORT.md)

Build a fresh checkout with `build-windows.cmd`. Run `check-device-update.cmd` before installing over an existing build. For the public APK's exact source, use the release tag. The default branch hosts the project entry points and stable update feed.

[Repository guide](docs/REPOSITORY_INDEX.md) · [Contributing](CONTRIBUTING.md) · [Security](SECURITY.md) · [Privacy draft](PRIVACY_POLICY_DRAFT.md) · [License](LICENSE)
