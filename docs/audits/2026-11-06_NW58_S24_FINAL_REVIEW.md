# NightWave v58 S24 product audit

Review the exact installed v58 APK on the owner's S24 Ultra. Inspect the visible core journeys and read-only settings, and use the existing feature inventories and fresh device evidence to distinguish presentation from untested behavior. Keep raw device screenshots and logs private.

Fix reproduced copy and accessibility defects in Offline, Downloads, Home artwork, Queue, You and Sound Studio. Preserve transport behavior, files, history, provider permissions, settings, and signing. Validate the same-version review APK before changing private delivery. Keep the signed public channel on v55.

## Scope and rating

**Observed score: 8.4/10 before the fixes and 8.5/10 after.**

Baseline is the APK pulled directly from the Galaxy S24 Ultra, build code 58 with SHA-256 49B1E534FD2A2940AC8163EEC89724F7E664FDCF94D599ED5F6D7E086FFB35BB. Observations are from the AMOLED theme and the phone's current text scale. Ratings are subjective product-quality judgments. The overall score is the arithmetic mean of the 24 rated areas, rounded to one decimal. Each row rates the scope in its evidence column.

Spotify's documented Search and Queue journeys were used as reference points for discovery and queue affordances. Android's accessibility guidance informed the review of labels, touch controls and readable text. [Spotify Search](https://support.spotify.com/article/search/) · [Spotify Queue](https://support.spotify.com/article/play-queue/) · [Android accessibility](https://developer.android.com/guide/topics/ui/accessibility/apps)

| Area | Baseline | Observed behavior and quality limit |
| --- | ---: | --- |
| Design and colors | 8.8 | AMOLED has a cohesive matte palette. Native Offline surfaces still have a heavier visual hierarchy than the main tabs. |
| Density and spacing | 8.4 | Main Search filters fit on one row on this phone. Expanded Sources and Sound Studio occupy more vertical space. |
| Typography and readability | 8.6 | Titles and section labels are distinct. Some long creator captions truncate in compact cards. |
| Text and status clarity | 8.2 | Main actions are readable. Offline pluralization, repeated queue copy and ambiguous backup/EQ wording needed work. |
| Accessibility semantics | 7.9 | Main destinations and player actions have names. Decorative art exposed long source captions and repeated artwork nodes. Full TalkBack and large-font evaluation remain open. |
| Navigation and discoverability | 8.6 | Home, Search, Library, You, Back and player entry were exercised. App details and Sound Studio are inside settings groups. |
| Home and recent listening | 8.7 | Continue, recent tracks, prominent Find music and mixes are visible on first launch. Deeper history edits were avoided. |
| Mix previews and actions | 8.6 | Preview opened on the first tap and closed. The track list, Play mix and See all actions were visible. Mix playback itself was outside this pass. |
| Main Search and local filters | 8.5 | The default Music, Your music and Saved links filters are reachable, with an explicit query entry. An exhaustive catalog or search relevance test was outside this pass. |
| Audius, PeerTube, Live radio and Openverse | 8.8 | The four filters, provider guide and Openverse attribution are visible. Prior same-path S24 checks returned live results from all four. The discovery actions open provider sites. |
| Library, playlists and collections | 8.3 | The empty liked/playlist state, new playlist entry, Offline, history, and automatic collection routes are coherent. Populated and bulk-edit states were not exercised. |
| Mini-player | 8.9 | Paused state, compact artwork and expansion are clear. The phone ended on Home with playback paused. |
| Full player and artwork | 8.7 | Play, previous/next, seek, shuffle, repeat and Details are visible. Fit preserves the entire thumbnail but leaves visible side space for square artwork in the wide cover slot. Transport mutations and long playback were outside this pass. |
| Queue | 8.3 | Current track correctly says Paused and opens quickly. The one-track queue repeated its empty-upcoming message. Reordering and undo were left untouched. |
| Lyrics and LRC | 8.2 | The source-specific import and synchronized lyrics guide are visible. Editor timing and file round-trips were not re-exercised. |
| Offline library, import and FLAC | 8.6 | File health, import and resume are discoverable. A prior S24 test verified stereo 16-bit/44.1 kHz FLAC playback. The one-file count used incorrect plurals. |
| Downloads and recovery copy | 7.9 | Active/Ready/Issues filters and history controls are visible. The empty state used internal language and the wrong route name. Live throughput remains unmeasured in this pass. |
| You and local profile | 8.4 | Settings are grouped with direct update and Offline actions. The local-profile copy did not clearly distinguish on-device identity from the enabled backup. |
| App details and support entry | 8.8 | Version, build, update, copy and technical disclosure fit in the modal. Support entry and Help and privacy were inspected. No email was sent. |
| Manual update | 8.6 | The S24 authenticated the live feed and accurately showed no newer approved build. The owner separately confirmed build 55 through in-app updates on another phone. |
| Themes and motion settings | 8.3 | Theme choices and reduced-motion controls are visible. Rendered grading covers the current AMOLED configuration. |
| Listening history and statistics | 8.2 | Recent listening, management and on-device statistics entries are discoverable. Accuracy over a longer listening period was not scored. |
| Backup, recovery and storage | 8.0 | Drive verification and backup management are visible. No restore, account change, storage cleanup or consent change was performed. |
| Sound Studio and local playback controls | 8.2 | The screen renders presets, advanced EQ, crossfade and offline volume controls. The inactive saved EQ state repeated itself and offered a generic action label. No sound settings were changed. |

The unscored feature set includes live playlist exchange and bulk editing, external-site playback, direct download and interrupted-transfer recovery, cross-device or disposable Drive restore, long playback and audio-route behavior, EQ sound fidelity, lock screen/headset/Android Auto, widgets, background behavior over long intervals, older devices, all-theme rendering, frame timing and the full Android instrumentation matrix. The existing [feature inventory](2026-11-04_CODE40_PRODUCT_REVIEW.md) and [Spotify comparison](../NW157_SPOTIFY_COMPARISON.md) preserve the wider source-level inventory.

The Sound Studio screenshot rendered normally. The Android accessibility XML dump failed on that modal, so only its visible presentation was rated. The actual cause was not determined.

## Fixes and physical retest

| Area changed | Before → after | S24 evidence |
| --- | ---: | --- |
| Text and status clarity | 8.2 → 8.7 | Correct one-file and one-track grammar. Downloads points to Find audio and Audio link. The You header explains optional backup. |
| Accessibility semantics | 7.9 → 8.3 | Recent play targets use short, named actions. Artwork inside named tracks and mix mosaics is decorative. Accessible tree check passed on Home and Queue. |
| Queue | 8.3 → 8.6 | The one-track queue shows one actionable message under Up Next. Current track remains Paused. |
| Offline library | 8.6 → 8.8 | The one-track library shows the correct file and track counts. The file list and import actions remain available. |
| Downloads | 7.9 → 8.4 | All and Active empty states show clear messages with actual destination names. |
| You and local profile | 8.4 → 8.7 | The profile wording and verified Drive backup can be read together without a contradiction. |
| Sound Studio | 8.2 → 8.4 | The inactive saved EQ state shows Clear saved EQ and drops the redundant status line. Verified from the rendered screenshot with no EQ changes. |

The other rated areas retain their baseline scores. The app returned to Home with playback paused and no fatal exception for its current process. The same-signer in-place update preserved all 34 backed-up files before launch. Raw evidence is kept in the private checkpoint.

## Build identity

- Source commit 715425ec76a614f95f36bf43a1580bebd4811b00, build NW-debug-D2B8FCAE7D5FA62D.
- Canonical source SHA-256 D2B8FCAE7D5FA62DFC060E3E6E58C58BC87FCB6875155C1F299AB1C492D39605.
- APK 30,573,703 bytes, SHA-256 20CA62141CF89113095E078ED78B2ED1AC9A329D8DE7EE189B39879BB108EEFF.
- Windows builder, 1,888 JVM tests and 94 source checks passed. Lint passed with zero errors, 318 warnings and 17 hints. Android instrumentation compiled.
- GitHub draft and owner-only Drive both returned the exact APK byte count and SHA-256. The public channel remains on v55.
