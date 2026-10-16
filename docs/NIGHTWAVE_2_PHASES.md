# NightWave 2.0 — staged implementation and release gates

Last updated: 2026-10-17

## Contract

Treat \`main\` v0.8 as the preserved production baseline until an opt-in test branch passes its gates. No account authentication is simulated. Never silently overwrite guest playlists or history with a cloud backup. Real music search and playlist playback need device verification before a polished interface becomes the default.

## Phase 1 — architecture + Compose design preview (this branch)

Delivered in \`feature/nightwave-v2-phase1-foundation\`:
- Kotlin 2.0 Compose compiler plugin with compatible AndroidX Compose BOM and Material 3; old ViewBinding screens left in place.
- Dedicated v2 UI package with own Material 3 theme, typography, spacing tokens, accessible-size controls and four palettes.
- Opt-in Compose Home, Search, Library and Guest Profile, with real saved-song thumbnails and responsive Row/Column/LazyColumn layouts.
- Crossfade navigation, reduced-motion preference, separate profile guest storage namespace, theme choice and display-name saving.
- Preview routes to the old player for streaming and playlists. No claim that this preview replaces the full player yet.
- No Google sign-in or cloud recovery until explicit OAuth/API setup and storage migration are tested.

Build gate: \`python tools/verify_source.py && python tools/verify_v2_phase1.py\`, then on Windows run \`build-windows.cmd\`. On device verify old and new navigation, back stack, theme changes across restart, local favorites from v0.8, and direct-link screen-off playback. If compilation fails, no changes to main. Reopen PR after errors fixed.

## Phase 2 — production Compose UI

Break down the legacy 58k-character MainActivity and move playback control into ViewModel/MediaController presentation state, retaining foreground MediaSessionService. Replace Home/Search/Library/Now Playing with new screens; add mini-player to full-screen shared-element-like transition, responsive sizes, artwork loading and accessibility. Search backend must be tested against real data; no fictional recommendations or music metadata.

Gate: navigation, artist/title lookup, real artwork, playlist editing, queue, 60 Hz smoothness measurement, 360/412/600/840 dp layouts, font 1.3x/2.0x, TalkBack and reduced motion, no old media regression.

## Phase 3 — library and account-safe storage

Implement versioned Room entities for playlists, tracks, order, favorites, history and queue, and DataStore for configuration. Use migration snapshots from current SharedPreferences. Provide guest export/import with checksum/schema validation, rollback, and consent-aware history treatment. Avoid signed stream URLs or cached audio in backups.

Gate: upgrade from v0.8 without lost data, guest export → fresh install → import, import of corrupt/duplicate/old-version backups, low-storage failure recovery, no overwriting remote data.

## Phase 4 — player improvements

Sleep timer, per-device media focus, audio quality, buffer status, queue persistence, gapless where feasible, crossfade only with eligible sources, optional Android EQ. No feature may regress locked-screen playback or Bluetooth buttons. Add network recovery and cancellation handling.

Gate: audio under screen-off for 2+ track transitions, interruptions (calls, Bluetooth disconnected), device idle and Wi-Fi→cellular, invalid streams, app process recreation.

## Phase 5 — Google identities and Drive backup

Use Google Identity Services through Credential Manager. Guest works without auth. After user chooses Connect, explicitly request Google Drive \`appDataFolder\` access (a separate consent step) and keep profiles private. Backups should store only validated non-secret IDs, metadata, configuration and opt-in history. Show last successful backup time, restore preview and manual conflict merge. A cloud upload never means synced until remote success.

Gate: Google Cloud project and Android OAuth client configured by owner with signing cert hashes; consent flow; guest→Google upgrade; reinstalls; two-device merges; offline changes; revoked scopes; account switch; failed restore; no plaintext tokens in backup; app deletion privacy request.

## Phase 6 — production stabilization

Automated compile/unit/instrumented tests, manual matrix on Samsung/Pixel and non-Samsung Android where available, UI screenshot and accessibility checks, cold starts and jank metrics, optional release signing. Audit dependencies/licenses, platform policies and app data disclosures. Stage release after evidence, not cosmetic completion.

## Architecture decisions

- UI: Jetpack Compose Material 3, original NightWave design rather than proprietary Spotify assets.
- Playback: Android Media3 separate service, isolated from Activity lifecycle.
- Guest local profile: the new, namespaced preferences store is an intermediate migration source; Room/DataStore become canonical only after safe migration.
- Search: current YouTube WebView and NewPipe route is a prototype and unverified in v0.8; long-term UX requires a stable, permitted search/playback source.
- Backup: Google login authenticates identity, Drive backup provides restoration. Google account alone does not recover local state.
- Costs: local Windows builds without GitHub billing; Drive depends on user account storage/quota; Google Cloud APIs require configuration, quotas and possible verification.

## Invariants

1. Working direct-link player remains available until a replacement passes on a physical device.
2. No phantom sign-in, invented recommendations, or fake "backed up" status.
3. Do not upload passwords, tokens, signed media stream URLs, or private history without opt-in.
4. Updates preserve local favorites/playlists; no uninstall-as-upgrade recommendation.
5. Theme and motion are optional user choices; maintain readable light/dark contrast and 48dp targets.
6. Build failure → revert branch fix; never replace main from an unverified build.
