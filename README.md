# NightWave

An independent Android music player for your own audio and saved music references. Built in Kotlin with Jetpack Compose and Media3.

**[Download for Android](https://github.com/Nahian2002/NightWave/releases/download/nightwave-v51/NightWave-v51.apk)** · [Release notes](https://github.com/Nahian2002/NightWave/releases/tag/nightwave-v51) · [Source for this release](https://github.com/Nahian2002/NightWave/tree/nightwave-v51) · [Build guide](docs/BUILD_AND_RELEASE.md)

## Get NightWave

The current public release is **version 51** and requires **Android 6.0 or newer**. If your installed copy has the updater, open **You → Check for updates**. Android asks you to confirm the install. For a first install or a build without that button, use the release APK above. Upgrade in place with the matching signer to keep your data.

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

**Version 52 is the latest device-tested review build.** Its mix previews open in one tap and return to the same Home position when closed. The S24 Ultra review rated design and everyday usability **9.1/10**. Broader compatibility, playback and recovery testing remain open. [Review PR](https://github.com/Nahian2002/NightWave/pull/183) · [Test evidence](https://github.com/Nahian2002/NightWave/blob/28d9d9861650363853584c3afa93c4cd30d4b3bf/docs/NW155_MIX_PREVIEW.md)

Build a fresh checkout with `build-windows.cmd`. Run `check-device-update.cmd` before installing over an existing build. For the public APK's exact source, use the release tag. The default branch hosts the project entry points and stable update feed.

[Repository guide](docs/REPOSITORY_INDEX.md) · [Contributing](CONTRIBUTING.md) · [Security](SECURITY.md) · [Privacy draft](PRIVACY_POLICY_DRAFT.md) · [License](LICENSE)
