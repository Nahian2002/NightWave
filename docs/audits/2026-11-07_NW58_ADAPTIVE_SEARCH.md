# NW58 adaptive Search routing and relevance

The direct and four configured community search hosts are reused. The first community attempt prefers the host that answered the S24 probes. Later searches try the most recently successful community host first. When community search recovers a direct parser or transport failure, the gateway prefers it for 60 seconds. The direct route remains a recovery option and regains priority after the window. Requests stay serial. Route health stores no queries and is held in memory. Cache and explicit refresh behavior retain their existing scope.

Literal video matches outrank alias or single-edit matches. View counts break relevance ties. Songs places credible music above broad one-word matches without music evidence and retains provider results in the full list.

## S24 Ultra live probes

The same public Minecraft query was submitted first in Videos and then in Songs. The app diagnostics measured live provider work. The displayed-result column includes automated screen polling. Each lookup returned 20 online results. These are individual measurements on one device.

| Build and mode | App diagnostics | HTTP calls | Displayed results observed |
| --- | ---: | ---: | ---: |
| Previous build, first Videos lookup | 6,078 ms | 4 | 8.45 s |
| Previous build, next Songs lookup | 1,497 ms | 4 | 5.83 s |
| Adaptive routing only, first Videos lookup | 6,792 ms | 4 | 11.59 s |
| Adaptive routing only, next Songs lookup | 407 ms | 1 | 3.41 s |
| Final build, first Videos lookup | 1,395 ms | 1 | 6.12 s |
| Final build, next Songs lookup | 1,571 ms | 1 | 6.25 s |

The direct extractor returned no usable rows in these probes. The community endpoint supplied the rows. On the adaptive build, Queen’s “Bohemian Rhapsody” appeared in the first visible Best matches rows for the title and artist query. The final build uses the same ranking logic and starts with the same community host that served that result. The phone’s search history matched.

## Verification

- Galaxy S24 Ultra, Android 17 / API 37. The installed APK readback matched the candidate. All 32 backed-up files matched before first launch.
- Version 58 (`2.0.0-alpha5s`), package `com.nightwave.player`, build `NW-debug-99DE616675BB7939`.
- Build source revision `3b70cde741042a439381f5f69f3fd5944d257a04`. Embedded source SHA-256 `99DE616675BB79392EE7C77A9E519F89DB87C443B0C7715266B098A357F9230E`.
- APK SHA-256 `0BF4AED577367FCC104F085D72D5FAFD3AD145D143B4E4EE6A631F6020B9C624`, 30,598,708 bytes. Signer SHA-256 `EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394`.
- Windows builder, 1,899 JVM tests, 94 source checks and sequential lint passed. Lint returned zero errors, 319 warnings and 17 hints. Android instrumentation sources compiled.
- More provider, query, device and network combinations remain open. Poco visual acceptance is tracked separately.

[Draft review #195](https://github.com/Nahian2002/NightWave/pull/195)
