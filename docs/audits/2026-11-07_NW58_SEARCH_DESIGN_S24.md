# NW58 Search, suggestions and visual follow-up

## Changes

- Retain public view counts in memory and use them to order equally relevant results. Compare title and artist evidence first. Songs keeps alternative versions and the full provider list. Best matches requires stronger music evidence for broad one-word searches and generic long clips.
- Start fallback sooner when the direct route stalls. The direct phase has a 5-second budget, each community call a 4-second limit, and the overall request retains its 20-second limit. Community responses are bounded before parsing.
- Now Playing suggests up to three related tracks from the last Songs result set and liked songs. It favors the same artist, suppresses duplicates and offers an explicit artist search shortcut.
- The Sources description and actions have more separation. The Videos action and local match counts use clearer English and Bangla labels.
- Midnight uses cobalt, Aurora uses violet, Pearl uses warm white and blue, and AMOLED uses mint on black. The chooser includes swatches. The waveform has legacy, adaptive and Android 13 monochrome resources. [Android adaptive icons](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive)

## Samsung evidence

Galaxy S24 Ultra (SM-S928B), Android 17 / API 37. The installed APK was pulled back and matched the verified hash. All 32 backed-up files matched before first launch.

The final Minecraft probe showed 20 online results in Songs after approximately 6.1 seconds and 20 online results in Videos after approximately 8.3 seconds. The preceding baseline displayed an unavailable-provider state. The candidate used the existing community fallback. UI observation includes accessibility polling and provider response time. This is one query on one phone. The known gameplay clips stayed in the full list, and no Best matches section was shown for this query. Automated fixtures also preserve eligible long songs and soundtracks.

The phone rendered all four palettes during the visual pass and returned to AMOLED. Samsung's app drawer and App info rendered the new waveform. The final build carries the same theme and icon resources. Sources spacing and the player section were checked. A same-artist search produced visible related rows during the visual pass, and the final build's artist shortcut prefilled Search without playing a track. Search history matched, and the final check returned to paused Home without a fatal crash in the current app process.

## Build and remaining checks

- Application version 58 (`2.0.0-alpha5s`), `com.nightwave.player`.
- Source revision `347eb0b2db2644652e2d40d65226a2083b60026f` and build ID `NW-debug-511D7F34157127D7`.
- Embedded source SHA-256 `511D7F34157127D772272BA42D113F194480243B32988C6C64E4B9579FA94E2F`.
- APK SHA-256 `D1E1DC9140A749A9CB56AE9928FED05177D4418C40F416F180072016DB7247D6`, 30,598,708 bytes.
- Signer SHA-256 `EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394`.
- Windows builder, 1,894 JVM tests, 94 source checks and sequential lint passed. Android instrumentation sources compiled. Lint reported zero errors, 319 warnings and 17 hints. The legacy adaptive resource has a monochrome lint warning. The packaged API 33 resource supplies the monochrome layer.
- Next device pass is Poco launcher masking, text scale and narrow layouts across Search, Now Playing, Sources and the theme chooser. Direct YouTube extraction returned no usable rows on the S24 probes. Broader relevance, playback and cross-device acceptance remain open.

[Draft review #195](https://github.com/Nahian2002/NightWave/pull/195) tracks the source. The public release is [v55](https://github.com/Nahian2002/NightWave/releases/tag/nightwave-v55).
