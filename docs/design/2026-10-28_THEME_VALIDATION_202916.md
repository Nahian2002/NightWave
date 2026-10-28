# DESIGN02 owner-recording validation and bounded follow-up

28 October 2026. [Draft PR #146](https://github.com/Nahian2002/NightWave/pull/146), `design/nightwave-expressive-themes`. This follows the [original 6.0/10 theme review](2026-10-28_THEME_RECORDING_REVIEW.md); the original review remains unchanged. [Current verification](../../TEST_REPORT.md) identifies the follow-up candidate separately.

## Evidence and scope

Owner media: `Screen_Recording_20261028_202916.mp4`, 150,094,125 bytes; SHA256 `ca97ad5673112cd5c1f715d7afc788ce52726b1e655aba4a2dbf676f023f51fe`. Reported 1080×2340, 17,256 frames at about 57.4446 fps, approximately 300.39 seconds by frame count/rate. These describe media metadata, not app performance.

Review used timestamp seeks at one-second intervals: 296 frames decoded successfully from offsets 0–296s, with 295s and 297–300s unavailable. A five-second overview and detail frames at 10, 50, 80, 95, 111, 119, 130, 132, 135, 167, 188, 190, 193, 205, 266 and 276 seconds were inspected. This is visual sampling, not continuous every-frame or audio review. Recording, extracted frames and raw receipts remain outside Git in the private `20261028-theme-review-202916` checkpoint.

At about **02:12**, App details exposes `NW-debug-D5AD44B8F8DCB21B`, source `D5AD44B8F8DCB21BE5A4401DA5D34F13A6D2451ABDED86EF282821389141F098`, 860 inputs, revision `0b4e4dea6528f5faebf4c63659545d93cfc54a87`, APK `130478C947F201D5AD3B02200CA7330D0359B2C0E521E339B0C28054A2939E59` and the retained EA737A70… certificate. The recorded build is attributable to the delivered DESIGN02 artifact; it is not the subsequent boundary-fix candidate.

## Recorded result: 7.6/10 for the primary Midnight theme

The same five equally weighted dimensions as the previous review are retained. This is a subjective appearance judgment within NightWave's scope, not a measured Spotify score, accessibility certification or whole-app re-rating.

| Dimension | Before | Recorded DESIGN02 | Reason |
| --- | ---: | ---: | --- |
| Identity | 6 | 8 | Navy/cyan, wave mark and selected controls feel recognizably NightWave. |
| Color character | 4 | 8 | Colored primary actions and visibly separate theme containers replace the near-gray treatment. |
| Tonal hierarchy | 5 | 7 | Cards/player/navigation separate better; repeated outlined panels and the heavy bottom stack still compete with music. |
| Primary cross-screen coherence | 8 | 8 | Main tabs/player/settings share a palette; dialog container defaults and the alternate classic interface remain visible boundaries. |
| Perceived legibility | 7 | 7 | Labels remain readable at recorded scale, but small supporting text and long metadata still feel dense. Rendered contrast is not measured from compressed video. |
| Equal-weight mean | **6.0** | **7.6** | Appearance only; original overall provisional product score stays **6.7/10**. |

All four theme choices are demonstrated, with different evidence depth. Midnight covers Home, Search, Library, You, player and native sound/offline screens. Pearl appears around **01:34–01:46** on You/Search; Aurora around **01:48–01:57** on Home/Library; AMOLED around **01:58–02:09** on You/theme controls; Midnight is restored around 02:10. Pearl is clearly warm-white/cobalt, Aurora plum/lavender and AMOLED black/teal. These short alternatives do not justify four independent full-journey scores or marking the formal matrix passed.

The singleton collection is now a compact horizontal card at approximately **00:50**, confirming the oversized square-collage correction. The classic interface is explicitly opened around **03:09–03:14**: its different header, three-tab navigation and mood tiles are intentional retained alternatives, not missing primary features.

## Findings and response

| Finding | Evidence | Response and acceptance limit |
| --- | --- | --- |
| TV01: Pearl system navigation remains charcoal | About 01:35, light You screen with dark Android three-button strip | Source still enabled the platform contrast scrim. The follow-up disables that scrim on API29+ while retaining theme background and light/dark icon selection. New phone confirmation is pending; the recording does not prove the fix. |
| TV02: App details has a default gray/purple container | About 02:12, cyan Midnight behind a purple-gray Material dialog | The theme supplied legacy surface roles but omitted Material3 container roles. All seven container/brightness roles now map to the selected palette; the existing 36-configuration instrumented method checks each actual scheme value. New phone confirmation is pending. |
| TV03: heavy bottom chrome | Repeated mini-player plus separate rounded navigation panel | Keep for a separately reviewed composition polish; reduce repeated visual emphasis without deleting actions or shrinking touch targets. No layout restructuring is included in this bounded follow-up. |
| TV04: dense copy and equal-weight outlines | Home collections and long You/tool screens | Future polish should prioritize music titles, supporting-copy rhythm and quieter secondary outlines. Preserve every feature and necessary status explanation. |
| TV05: classic/primary aesthetic boundary | Explicit classic switch around 03:09–03:14 | Retain the alternate interface. Any later harmonization must preserve its navigation and recovery behavior. |

The diagnostics dialog around 02:15 is sampled during a close interaction; apparent translucency may be a transition. It is not reported as a confirmed opacity defect. No search latency, audio, backup-content, download or EQ-correctness conclusion is drawn from this theme review.

## Follow-up source and remaining acceptance

Boundary implementation `6057bfaa2a0d8e0ed47b13705adc247902d67b4b` changes only the shared Material scheme, shared system-bar helper and their acceptance/source guards. All four palette values, feature routes, callback inventories, native/classic choice, account/consent, storage, queue, original URLs, dependencies and signing identity remain. Two additional broken-wiring fixtures protect container and scrim wiring; ten negative fixtures now run inside the existing 62-check source runner. No new Android test method is added: ten methods × 36 configurations remain **360 compiled cases, NOT_RUN**.

The new candidate requires Pearl three-button/gesture checks and Material dialog/menu screenshots in all four themes on the phone. The recording supplies **partial observational appearance evidence for D5**, not executed instrumentation or a visual pass for the B017 follow-up. Large type, input/accessibility, restart, data-preserving installation, native dialogs, performance, artwork/cache, audio and release gates remain pending. NW134 remains next in the feature roadmap.
