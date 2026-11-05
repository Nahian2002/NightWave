# NightWave final review

The design and everyday usability earn **8.5/10** as an editorial assessment. This judgment draws on the version 48 S24 Ultra walkthrough and earlier documented checks of individual features. A full release score remains pending because several functional acceptance gates have no current device evidence.

| Area | Rating | Evidence and limit |
| --- | ---: | --- |
| Visual identity and color | 8.8 | The AMOLED screen has a coherent teal and black palette and clear hierarchy. Branded video thumbnails make the overall presentation less consistent. |
| Layout and compactness | 8.3 | Quick actions and the tab bar are easy to scan. The resume card and persistent mini-player consume substantial vertical space. |
| Typography and copy | 8.5 | Titles, secondary text and actions are easy to distinguish on the S24. Large text and the full Bangla experience need current coverage. |
| Themes and personalization | 8.6 | Four choices give the app a range of looks. AMOLED was observed on version 48, and other themes have earlier scoped coverage. |
| Navigation and accessibility | 8.8 for navigation | Home, Search, Library and You opened on the S24. Screen-reader, large-font and older-device coverage remain incomplete. |
| Home and discovery | 8.3 | Resume, history and mixes give Home clear purpose. The mixes visible in the test use familiar tracks. Discovery relevance remains unmeasured. |
| Search and library | 8.5 for UI | Clear search entry and library routes. Search relevance and latency, large collections and conflict handling need broader tests. |
| Artwork and music identity | 7.6 | Artwork is large enough to identify tracks. Soft images and promotional graphics are visible even on the same Home screen. |
| Now Playing and queue | 8.6 for UI | Accessible controls and a clear paused state in scoped checks. Long playback, interruption and audio route handling remain open. |
| Offline and transfers | 8.2 for UI | Offline access is prominent. Sustained throughput and integrity across networks have no current device grade. |
| Lyrics and Sound Studio | 8.2 for controls | Scoped reviews covered presentation. Lyrics timing, presets and audible quality need dedicated tests. |
| Manual update check | 9.0 | The S24 recovered after the feed fix. Both version 48 entry points displayed the authenticated paused state. |
| Automatic updates and installation | Pending | A complete signed release offer, download, seven-day prompt and installer flow still need device evidence. |
| Data protection and recovery | Pending | The S24 upgrade preserved all 33 collected files before first launch. Disposable restore and account-switch tests remain open. |
| Performance and reliability | Pending | The scoped S24 smoke test recorded no fatal exception. Frame timing, battery use, two-hour playback and older Android remain open. |

The [S24 results](NW153_S24_UPDATE_CHECK.md) document the installed version 48 and live feed. The version 49 patch releases a download connection if the HTTP response fails or redirects. The signed live feed currently advertises no APK.

Version 49 is build `NW-debug-B751A234428BB70C` from `4c2f4a83aa5f1a27f46f71904b9e5eb05845c489`. The APK is 28,122,602 bytes with SHA-256 `1D91E9DFCC1E63DCAB387CCA6B9FFD2AFC4D4D2DB5DCA3A392550A5C5B33C8FB`. The signing certificate matches the version 48 APK pulled from the S24. The Windows build, 1,867 JVM tests and 94 current source checks passed. Android instrumentation sources compiled. The phone evidence in this review applies to version 48.

Lint passed with zero errors and the same 317 warnings and 17 hints as version 48. The owner-only Drive copy matched the built APK in size and SHA-256. [Draft PR #180](https://github.com/Nahian2002/NightWave/pull/180)
