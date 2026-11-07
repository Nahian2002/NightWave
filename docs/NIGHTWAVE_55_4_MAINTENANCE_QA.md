# NightWave 55.4 maintenance QA

Routine feature development is paused. The public update remains 55.4, Android code 63. The maintenance patch was tested on the S24 Ultra and is available for source review separately from the public release.

## Fixes

- Empty Sources shows provider controls without an unrelated prompt to search local music. Your Music keeps its own prompt, and All Sources still includes matching local results after a query.
- Lint tasks now depend on the task that generates the APK source fingerprint. A combined assembly and lint run that previously failed now succeeds.
- Offline volume balancing checks the Android version before reading the PCM encoding key introduced in API 24. API 23 uses the existing 16-bit default.
- The default branch documentation now points to the actual 55.4 release, source tag, support status and verification evidence. [Documentation PR](https://github.com/Nahian2002/NightWave/pull/200)

## Verification

| Item | Result |
| --- | --- |
| Source | `1bf763cf5c477bc4301f19704bdbc45517219b34` |
| Maintenance APK | `NW-debug-5596276985CB99D0`, SHA-256 `4E8AB988F5D94CF8951E9AD0C1D7703CA9C581AED266F1B8A8CC22181DFCFBC6` |
| Package and signer | `com.nightwave.player` 55.4, code 63, signer `EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394` |
| Build and automated checks | Assembly, JVM and Android instrumentation source compilation, 95 source checks, 1,904 JVM tests, and lint passed. Lint reported 0 errors, 299 warnings and 17 hints. The JVM suite ran under Linux with the current compiled classes. |
| Upgrade in place | The S24 installed the exact same-signer APK over public 55.4. All 17 backed-up app files matched before first launch. |
| Home and library | Home, mix preview, Library, Offline music and Downloads rendered. The profile editor opened and was dismissed. |
| Search | Empty Sources and Your Music showed their intended content. The four provider filters were present. A live catalog query returned a matching result, and NewPipe found “ek paye nupur amar” inside NightWave. Saved Links and the source guide opened. |
| Offline and player | Imported and played a generated FLAC, paused it, opened the full player and queue, and toggled the lyrics panel. Both the imported copy and the picker fixture were removed. |
| Settings and help | You, appearance, Sound Studio, listening, App details, Help and privacy, and backup management were observed. Sound Studio rendered. Android's UI hierarchy command stopped with exit 137 while it was open and recovered after closing it. |
| Themes | Midnight, Aurora, Pearl and AMOLED rendered in the theme chooser. The original AMOLED theme was restored and the test fixture was absent from Home. |
| Update check | The S24 showed the current-version response. The signed canonical manifest still advertises the exact public code 63 APK. The maintenance APK is outside the stable feed. |
| Public documentation | 70 local links checked across 14 repository entry documents. No missing targets were found. |

The S24 run covered one handset. Instrumentation sources compiled, and the full instrumentation suite remains unexecuted. Older Android and small-screen devices, TalkBack, Bluetooth and Android Auto, crossfade and volume balancing across audio routes, long transfers, third-party provider playback, LRC import and synchronization, Google Drive restore and account switching, support email sending, and an older phone's installer handoff remain open for end-to-end testing. No account or sound preferences were changed for this pass.
