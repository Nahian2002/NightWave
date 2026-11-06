# NightWave

An independent Android music player for your own audio and saved music references. Built in Kotlin with Jetpack Compose and Media3.

**[Download for Android](https://github.com/Nahian2002/NightWave/releases/download/nightwave-v55/NightWave-v55.apk)** · [Release notes](https://github.com/Nahian2002/NightWave/releases/tag/nightwave-v55) · [Source for this release](https://github.com/Nahian2002/NightWave/tree/nightwave-v55) · [Build guide](docs/BUILD_AND_RELEASE.md)

## Get NightWave

The current public release is **version 55** and requires **Android 6.0 or newer**. If your installed copy has the updater, open **You → Check for updates**. Android asks you to confirm the install. For a first install or a build without that button, use the release APK above. Upgrade in place with the matching signer to keep your data.

## What you can do

| Area | Features |
| --- | --- |
| Listen | Background audio, queue controls, shuffle, repeat, mini-player and Now Playing |
| Organize | Import your own audio, including FLAC, build playlists, save tracks and browse listening history |
| Discover | Browse local mixes and search for public references where sources are available |
| Customize | Four themes, synchronized lyrics from your own LRC files, and Sound Studio |
| Back up | Export library metadata locally or use optional Google Drive recovery |

Saved public links are references. Offline files and third-party media require the appropriate rights.

## Development

**Version 58 is the current review build.** It adds Openverse music discovery and clearer update version labels. The S24 passed scoped update and Search checks. Wider compatibility, playback and recovery testing remain open. [Draft review](https://github.com/Nahian2002/NightWave/pull/195) · [Test evidence](docs/MULTISOURCE_NW58_IMPLEMENTATION.md)

Build a fresh checkout with `build-windows.cmd`. Run `check-device-update.cmd` before installing over an existing build. For the public APK's exact source, use the release tag. The default branch hosts the project entry points and stable update feed.

[Repository guide](docs/REPOSITORY_INDEX.md) · [Contributing](CONTRIBUTING.md) · [Security](SECURITY.md) · [Privacy draft](PRIVACY_POLICY_DRAFT.md) · [License](LICENSE)
