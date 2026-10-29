# Current verification — NW136 resumable transfers

29 October 2026. [Draft PR #154](https://github.com/Nahian2002/NightWave/pull/154), `update/nightwave-nw136-resumable-transfers`, stacked on NETWORK01 #153. Protected parent `2f8b757c2141269245fa97389bbd7d4ff53caf8d`; researched plan `cb752250`; implementation `a0057c72f15f0fd3aafe4a56646fe9a5b4ed9f00`; final built revision `d04d514f51a8b2a2554c5dc7c316302c4a656959`. [Plan/architecture/results](docs/NW136_RESUMABLE_TRANSFERS.md), [physical supplement](docs/quality/NW136_TRANSPORT_ACCEPTANCE.csv), [previous verification](https://github.com/Nahian2002/NightWave/blob/2f8b757c2141269245fa97389bbd7d4ff53caf8d/TEST_REPORT.md). **NW137 is next.**

Eligible sources now use guarded bounded ranges and durable hashed recovery; unsupported sources safely restart. Original source/quality, all features/themes, signer/package, data/account boundaries and playback remain. Private checkpoint validity is 24h with lazy owned-file cleanup at the next download; only completed synced segments resume. Manual searched Resume uses a new WorkRequest UUID for the same source; automatic recovery retains the job identity.

| Executed or compiled check | Result and limit |
| --- | --- |
| Full Windows builder | PASS final Android/Compose/APK and instrumentation-source compilation; initial API-compatibility candidate superseded |
| JVM | **1,726 / 214 suites**, zero failures/errors/skips; 29 added real HTTP/file/policy methods |
| Current source gates | **72 PASS**, including broken-caller fixtures; structural evidence only |
| Lint | **0 errors / 314 warnings / 17 hints**; warnings unchanged, no suppression/baseline |
| Source preservation | 223 protected files identical, eleven intentional existing offline edits; nine callback inventories retained |
| Source/quality/privacy | Gateway, bitrate selection, playback, public-address body, package/build inputs and account/schema files unchanged |
| Controlled host transfer | Complete exact-byte loopback fixture measured; not phone/provider speed proof |
| Signing | Same certificate as previous owner-delivered APK; no installation |
| Android/theme/device | Existing instrumentation sources compile only; 612 theme cases and prior queue/import/local routing cases remain NOT_RUN |
| Physical acceptance | Original NW136 four rows and fourteen supplemental phone cases NOT_RUN; no connected device |
| History/recovery/delivery | All-ref secret scan and verified private recovery/owner-only streamed-hash receipt retained outside Git |
| Hosted checks | Actions remains disabled; no hosted pass is claimed |

Build **NW-debug-CBD2E4D4E07D9BB4**, canonical source SHA256 **CBD2E4D4E07D9BB46E34E20F0929226C5355CBB984291259C55C4ECBE250225D**, **908 inputs**. APK SHA256 **2FDF1084FCF3C64E5DC94FCDC9FD3EC4B7C13DCF5DA2A16BD42ABA5A29586B07**; signer **EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394**. App `com.nightwave.player`, version `2.0.0-alpha5s` / code35, min26 / target36 remain.

The recording's slow rate does not isolate a cause. Bounded requests and numerical DNS/connect/TLS/header/receive/write/sync details now enable a useful diagnosis, but no physical 1 Mbps minimum, ISP line rate or new visual/product score is certified. The original 6.7 product baseline and latest recorded 8.8 UI rubric remain. Preserve all previous audible transport, process/storage/data/recovery/accessibility and release gates. NW137–NW145 remain the active route to measured 9.5 acceptance.
