# NW154 artwork, Home and mini-player

The Home shelves use a consistent 16:9 crop and the existing higher-resolution, same-video artwork candidates. Small covers in Continue Listening and mixes can try one sharper still when the public thumbnail query and canonical track agree. The exact original remains the fallback. Now Playing keeps its existing full-image framing.

The standard-text Continue Listening minimum drops from 96 dp to 80 dp. The mini-player row drops from 52 dp to 48 dp and its outer vertical padding drops by 2 dp in total. Play/Pause, Next and Hide keep their existing 48 dp buttons. Large text retains its previous layout allowance. Recent-history help is available with the expanded shelf.

Among mixes already selected for Home, those opening with more songs outside the six initially visible recent tracks now lead. Their track lists and playback order are preserved. Captions distinguish liked songs, recent plays, playlists and tracks outside recent history. Short histories can still show their available mixes, and a one-track artwork tile fills its cell.

Version 48 provided the baseline Home and Offline screens on the S24 Ultra. Both local backup pickers opened and could be cancelled, after which the guest profile matched its prior hash and Home was visible with playback paused.

The owner approved the in-place install of version 50. All 33 backed-up files matched after installation and before the first launch. On the same S24 profile at 3.5 px per dp, the Recently Played heading moved up 56 px (16 dp) and the Mixes heading moved up 175 px (50 dp). The mini-player's Play control retained its 168 px (48 dp) bounds. The first mix changed, its preview opened and the Play mix action remained available. Now Playing, Offline, Search, Library and both manual update routes passed the scoped screen checks.

The S24 comparison exposed black bars in some small covers. For the same public track, the downloaded 480×360 source had 90 nearly black full rows and the 320×180 source had zero. Version 51 chooses the widescreen source for eligible small covers, rejects known low-resolution placeholders and keeps the exact original as fallback. The version 50 S24 screen result rates **8.8/10 for design and everyday usability**, up from 8.5. The compactness gain is visible, while provider artwork and mix preview placement still affect polish. Version 51 needs its own rendered check before claiming 9.

Older or smaller device behavior, playback interruptions, transfer recovery, offline playback integrity, actual backup restore and the complete signed update flow remain separate acceptance cases.

| Changed area | Previous review | S24 version 50 | Assessment |
| --- | ---: | ---: | --- |
| Layout and compactness | 8.3 | 9.0 | More of the mix shelf fits above the mini-player at the measured text size. |
| Home and discovery | 8.3 | 8.7 | The lead mix is distinct from the first recent tracks. Reaching its preview still requires scrolling. |
| Artwork | 7.6 | 8.1 | Recent cards fill the same frame. Promotional graphics and baked bars in small covers still stand out in version 50. |

Version 50 passed the Windows build, 1,869 JVM tests and 94 source checks. Android instrumentation sources compiled. The signer matched the APK pulled from the S24. The 219-case fixture pack was generated for this build and awaits device execution. Build `NW-debug-F6C17509C71754C2` was made from `4cb918ef13515c702f7cf43400bfe1051db5d782`. Its 28,141,040-byte APK has SHA-256 `FA355DE13761430D2FF24584481CF8912D0D42E0F4D4B9B76171DF0600DC4C7A`.

Lint passed with zero errors, 319 warnings and 17 hints. The two added warnings are for the retained translations of the old resume and mix helper lines. The owner-only Drive copy matched the APK in size and SHA-256. [Draft PR #181](https://github.com/Nahian2002/NightWave/pull/181)

Version 51 is the follow-up candidate. The Windows build, 1,869 JVM tests and 94 source checks passed. Lint returned to the version 49 inventory of 0 errors, 317 warnings and 17 hints after removing the two unused translation entries in both locales. The signer matched the APK pulled from the S24 running version 50. The owner-only Drive readback matched the complete 28,121,587-byte APK. Its SHA-256 is `3F61FE8CCC9841700E2619125D894FB73999FABAC9600FFE8BA832FA2A13E215`. Build `NW-debug-BF1D2D63955839B2` came from `235b080b0d68abd5600da720dabfc88837f91269`.
