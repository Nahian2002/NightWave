# NightWave — NW76–NW90 quality roadmap (authoritative)

**Source of truth:** Owner-supplied *NightWave — Roadmap to a 9.5–10/10 Experience*.
**Baseline rating:** 7.2/10 based on the prior phone video; **target:** 9.5+/10, subject to real-device proof.  
**Development lineage:** NW77 cumulative source, then NW78 onward. Default `main` is an obsolete v0.8 branch. The standalone NW62 backup experiment is not a substitute for NW77 cumulative source.

## Wave 1 — Make every major feature dependable (NW76–NW80)

| Phase | Scope and acceptance intent | State |
| --- | --- | --- |
| **NW76** | Final build and regression reliability: Gradle, zero failing JVM tests, APK signing/fingerprint and crash evidence | Earlier source certification gate; owner reported a successful build/basic device test, not final acceptance |
| **NW77** | Music-source architecture and public-release rights: separate authorized local/licensed audio from YouTube references; retain saved metadata/files | Source audit committed; public release NO-GO |
| **NW78** | Google sign-in and backup: certificate/OAuth error 8, connect/disconnect, verified backup/readback, restore preview, account switching | Source guards committed; Google connection owner-confirmed; complete backup/readback/restore device acceptance pending NW90 |
| **NW79** | Download/offline reliability: accurate progress, pause/retry/cancel semantics, transfer history, low-storage, integrity, permitted offline playback | Source improvements committed: longer slow-transfer window, backoff for transient HTTP, separate verification status; full end-to-end phone acceptance pending NW90 |
| **NW80** | Playback engine: shuffle/history, Play Next, repeat, gapless, background controls, queue restoration, interruption and rapid seek | Targeted fixes committed for shuffle history on Play Next reorder, media-identity-safe seeks, previous-button rewind and stale gapless handoffs; broad Bluetooth/background/stress acceptance pending NW90 |

## Wave 2 — Premium music experience (NW81–NW85)

| Phase | Scope |
| --- | --- |
| **NW81** | Music title intelligence, remove display clutter without losing source metadata | Source improvements committed: safe title badges, verified-uploader artist split, Bangla vowel matching, raw Videos captions and consistent Up Next presentation; Android/JUnit/device acceptance pending NW90 |
| **NW82** | Search precision: exact artist/title, Bangla/multilingual, typo tolerance, remix/live/cover, duplicate groups, local priority; fix 'Alo alo' vs 'Alone' | Targeted source fixes committed: short-word boundaries, repeated tokens, limited Latin typo fallback, Bangla distinction, local/offline precision and separate selectable alternate versions; full search/provider/device acceptance pending NW90 |
| **NW83** | Unified premium visual system: typography, spacing, components, icons, contrasts, animation, responsiveness | Cumulative source update committed: all Material3 type/color roles, shared design tokens, cohesive Home/Search/Library/You gutters, track/queue/mini-player geometry, restrained teal ambient tones; targeted Kotlin token smoke passed, full visual/Android QA pending NW90 |
| **NW84** | Music-first Home redesign with history-driven mixes, hierarchy, real artwork, listening continuation | Source work committed: actionable last-played hero, authentic saved-but-not-recent shelf, duplicate-mix card suppression, accessible adaptive mixes and music-first ordering; targeted Kotlin layout smoke passed, full Android/Compose/phone acceptance pending NW90 |
| **NW85** | Now Playing redesign: artwork, player transport, mini-player, Up Next, lyrics, gestures, transitions | Source update committed: accessible artwork and dynamic title typography, compact Offline/Lyrics actions, truthful queue preview for shuffle/repeat/Play Next, lyric availability and reduced-motion transitions; exact-source Kotlin policy smoke PASS, full device/Compose QA pending NW90 |

NightWave remains an original midnight-themed audio product with restrained teal accents; Spotify-level polish is an interaction-quality benchmark, not a directive to copy branding or UI.

## Wave 3 — Complete product and prove its quality (NW86–NW90)

| Phase | Scope |
| --- | --- |
| **NW86** | Library, playlist folders/organization/sorting, multi-select, Recently Played and compact You | Source updated: precise playlist-name search, select/deselect visible with hidden-selection accounting, history sort/clear confirmation, filter reset and compact You backup status; exact-source Kotlin selection smoke PASS, full Android/device acceptance pending NW90 |
| **NW87** | Downloads and Offline Library unified interface, statuses, artwork, storage and file recovery | Source update committed: dynamic transfer counts, accurate verification-pending states, manual background offline recheck, indexed-file recovery instructions and user-driven trusted import; no new download eligibility, Android/device QA deferred to NW90 |
| **NW88** | Sound Studio EQ effect verification, gapless/crossfade/normalization, audio-route compatibility | Source update committed: same-session manual EQ band readback, device rounding/error truth, Sound Studio crossfade and offline normalization controls with persisted-setting receipts; narrow Kotlin policy smoke PASS, full Android/audio-route QA pending NW90 |
| **NW89** | Lyrics, LRC refinement, accessibility, responsive typography, reduced motion, startup/scrolling/battery |
| **NW90** | Final quality certification: original 139-case device checklist, stress/background/offline/auth/restore QA, security, rights, privacy, release candidate |

## Score targets (not achieved-result claims)

| Category | Baseline | Target |
| --- | ---: | ---: |
| Home | 7.6 | 9.5 |
| Search | 7.1 | 9.5 |
| Now Playing | 7.4 | 9.7 |
| Library | 7.4 | 9.5 |
| Downloads UX | 6.6 | 9.4 |
| Sound Studio UX | 7.1 | 9.4 |
| Google account UX | 4.5 | 9.5 |
| Typography and polish | 6.4 | 9.7 |

## Owner's final-test instruction

The owner has explicitly selected **batch development now, consolidated phone testing at the end**. Accordingly, do not request a new APK install, Samsung recording, or manual acceptance after each phase. Keep writing regression tests and run every **available** automated source/JVM/Android check during development; do not claim Gradle builds or phone tests that have not run. The original roadmap wanted full Windows builds at integration checkpoints; if inaccessible, mark these **pending**, not passed. No phase is called completely verified on source checks alone.

At NW90, conduct one comprehensive current-source clean Windows build, full JVM/Android testing, APK signing compatibility, device checks (including the 139-case checklist), Drive upload/readback/restore using disposable data, long playback, accessibility and source/legal reviews. Any failed gate must be fixed before release approval.

**Invariants:** No required NightWave backend, no billing dependency, preserve account connection, application ID, library and offline recordings; never promise unrestricted offline copies of every YouTube song. Public release requires appropriate music-source rights and Android/Play compliance.
