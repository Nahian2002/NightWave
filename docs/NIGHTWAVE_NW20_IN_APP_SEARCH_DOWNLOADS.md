# NW20 — Search and save openly licensed audio inside NightWave

**Source:** NW19 audio-integrity development branch. **Status: experimental / NO-GO**, no full Android build or device test verified.

## Implemented in source

- An in-app music search screen backed by Wikimedia Commons' public MediaWiki API.
- The result filter only offers supported direct audio files whose Commons metadata identifies CC0 or public-domain licensing.
- User sees the recording title, Commons source/license link, license label, file size and a Save Offline action. This does not require a pasted URL or external file import.
- The existing NW19 verified downloader is reused: MIME/signature checks, Android audio validation, private staging, SHA-256 copy check, duplicate-content reuse and offline library storage.
- The catalog is reachable from Offline Library -> Download permitted audio -> Search free music. Existing YouTube streaming remains unchanged.

## Major limits

- **Not a YouTube-to-MP3 feature**. Existing YouTube search results are not directly downloadable through this provider. Most mainstream copyrighted music may not appear in the Commons catalog.
- Commons licensing metadata is subject to possible errors; verify the source/rights for each item.
- Supported audio containers only. Formats are not transcoded to MP3. Some Commons audio may fail Android decoder validation.
- No background downloading, no universal song availability guarantee, and no offline copy until an actual transfer succeeds.
- The full Android Gradle/JUnit build, APK signing compatibility and Galaxy on-device tests have not passed.

## Release gate

Build complete branch with `build-windows.cmd` and test actual catalog results, original-file download, airplane-mode playback, 404/503 responses, inaccurate metadata, cancellation and app restarts. Preserve the installed app and its user library until signing compatibility is verified.
