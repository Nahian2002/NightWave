# NightWave — current cumulative roadmap and final-test policy

**Updated:** 26 October 2026  
**Source of truth:** `feature/nightwave-nw77-source-safety-release-gate`  
**Not the cumulative source:** default `main` (historical v0.8), standalone `feature/nightwave-nw62-backup-verification-guard`.

## Intended product
A distinct, polished, Android-first, offline-capable, audio-focused app with reliable listening, Spotify-level interaction quality, and no required NightWave backend or paid infrastructure. Preserve installed app identity, signing compatibility, library history, likes, playlists, offline audio and working Google sign-in. Do not promise that every internet song can legally or technically be downloaded.

## Roadmap implemented in cumulative source (runtime acceptance is separate)

| Phases | Product focus |
| --- | --- |
| NW43–NW52 | Correct single-song search queue, search relevance, Google recovery, Now Playing, Home/Library/You, downloads, sound/lyrics, premium motion, reliability and integration |
| NW53–NW63 | Recovery/clarity and downloads history UX |
| NW64–NW66 | Download performance/status, transfer recovery and offline file health |
| NW67–NW69 | Playback transitions, audio-session/crossfade protections and queue/shuffle consistency |
| NW70–NW71 | Unicode/local search and smart library discovery |
| NW72–NW73 | Safe cloud restore and truthful Google Drive backup status |
| NW74–NW75 | UI-state reliability and cumulative feature integration/hotfixes |
| NW76 | Build/JUnit certification gates |
| NW77 | Read-only music-source rights and public-release safety audit |

**Integration rule:** New work must branch from the latest reviewed cumulative source, preserving the NW75 fixes, NW76 certification gate and NW77 source audit. Do not branch new product updates from historical v0.8/main or the isolated NW62 backup timestamp experiment. Future code edits should follow the existing numbered feature lineage without rewriting already-delivered phases.

## Remaining work after NW77, in priority order

1. **Authorized-audio download responsiveness and diagnostics:** investigate the reported 35–37 KB/s transfer observation using measured transfer timing, source/connection separation and privacy-safe reporting. Do not claim an app speed cap or a speed fix without evidence. Preserve cancellation, staging safety, DNS/redirect/MIME protections and atomic verified library import.
2. **Search metadata and library continuity:** clearer song/artist metadata and playable-source indicators; preserve search relevance and avoid unsolicited queue entries.
3. **Cloud verification transparency:** continue to distinguish Google *connected* from a verified backup and readable restore snapshot, without redoing a working OAuth setup or overwriting local data.
4. **Premium usability and stability:** polish unfinished interactions (downloads, offline browsing, queue, Now Playing, navigation and accessibility); avoid visual polish that destabilizes playback.
5. **Single consolidated final acceptance phase:** integrated build and source/JUnit/Android suites, compatible signing and update checks, Samsung real-device session, offline/online media controls, backup readback, visual QA, privacy/security/rights and publication decisions.

These five workstreams are the next priorities; they are not represented as already implemented or assigned invented NW numbers.

## User testing preference

**No per-batch phone APK installation, recording requests or manual test checklist.** Stack the development changes in the agreed order, and ask for **one integrated build and device acceptance process at the end**. During development, source review and non-device checks may be used where available to avoid accumulating obvious faults. Any automated checks not run must be explicitly reported; **source committed** does not mean **full Android build passed**.

The owner reported NW76's installed APK built and basic device tests succeeded, and separately that Google sign-in connected after the OAuth client was configured. This does not prove Google Drive backup upload/readback, full device regression, public OAuth verification or release authorization.

## Release decision

Public APK / Play remains **NO-GO** pending source/media authorization or compliant redesign, API target/signing and privacy obligations, authenticated Drive backup proof, the final current build, and physical-device acceptance. NW77's audit flags YouTube-derived background/offline paths; do not bypass or disguise that blocker.
