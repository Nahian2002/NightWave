# NW154 artwork, Home and mini-player

The Home shelves use a consistent 16:9 crop and the existing higher-resolution, same-video artwork candidates. Small covers in Continue Listening and mixes can try one sharper still when the public thumbnail query and canonical track agree. The exact original remains the fallback. Now Playing keeps its existing full-image framing.

The standard-text Continue Listening minimum drops from 96 dp to 80 dp. The mini-player row drops from 52 dp to 48 dp and its outer vertical padding drops by 2 dp in total. Play/Pause, Next and Hide keep their existing 48 dp buttons. Large text retains its previous layout allowance. Recent-history help is available with the expanded shelf.

Among mixes already selected for Home, those opening with more songs outside the six initially visible recent tracks now lead. Their track lists and playback order are preserved. Captions distinguish liked songs, recent plays, playlists and tracks outside recent history. Short histories can still show their available mixes, and a one-track artwork tile fills its cell.

The S24 Ultra still has version 48. Its baseline Home and Offline screens were captured. Both local backup pickers opened and could be cancelled, after which the guest profile matched its prior hash and Home was visible with playback paused. No backup destination or restore source was selected.

Version 50 needs a same-track before-and-after review on the S24 and a smaller device before revising the 8.5/10 design and usability rating. Playback interruptions, transfer recovery, offline playback integrity, actual backup restore and the complete signed update flow remain separate acceptance cases.

Version 50 passed the Windows build, 1,869 JVM tests and 94 source checks. Android instrumentation sources compiled. The signer matched the APK pulled from the S24. The 219-case fixture pack was generated for this build and awaits device execution. Build `NW-debug-F6C17509C71754C2` was made from `4cb918ef13515c702f7cf43400bfe1051db5d782`. Its 28,141,040-byte APK has SHA-256 `FA355DE13761430D2FF24584481CF8912D0D42E0F4D4B9B76171DF0600DC4C7A`.

Lint passed with zero errors, 319 warnings and 17 hints. The two added warnings are for the retained translations of the old resume and mix helper lines. The owner-only Drive copy matched the APK in size and SHA-256. [Draft PR #181](https://github.com/Nahian2002/NightWave/pull/181)
