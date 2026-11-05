# NW155 direct mix preview

Version 52 opens a mix in a preview sheet above Home. The sheet starts with the mix name, reason, Play mix and Close. The first three tracks are visible through the existing track rows, with the existing See all control for the rest. Playback receives the original mix order. Song options leave the preview before opening the existing menu. Three-cover mosaics use a full-height lead beside two stacked tiles.

On the same S24 Ultra, the first tap on version 51 left Play mix offscreen. On version 52, Play mix was visible on the first tap. Close and Android Back returned to the same card bounds. See all tracks and the handoff to song options passed. The in-place upgrade preserved all 33 backed-up files before first launch. Playback remained paused during the preview check.

The scoped S24 design and everyday usability rating rises from 8.9 to **9.1/10**. The biggest improvement is discovery usability, because the action and tracks are available immediately. Provider cover quality still varies. The rating is editorial and specific to the tested S24 profile. Smaller-screen, screen reader, full playback, offline reliability, backup recovery and full signed update acceptance remain open.

The Windows builder, 1,869 JVM tests and 94 source checks passed. The two updated Compose tests compiled. Build `NW-debug-9F998499DCA7E988` came from `c62ccfeaff651d5b49b72ede3145160ce84ef569`. The 28,138,453-byte APK has SHA-256 `DBD7C1536D94DBC6CEAD6991BBDAB9493288AC712D991D6D356E237AEAA56A63`. Its certificate matched the version 51 APK pulled from the S24. The owner-only download matched the APK by size and hash.

Lint reported zero errors and the same 317 warnings and 17 hints as version 51. The S24's two manual update checks correctly reported no newer compatible build while the live feed advertised version 51. The guest profile and local library matched their preinstall hashes after the checks. [Draft PR #183](https://github.com/Nahian2002/NightWave/pull/183)
