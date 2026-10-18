# NightWave Phase 6V7B — Premium Search and Library

## Goal and scope

This is an original **Search and Library presentation redesign** building
on the 6V7A premium Home/navigation/mini-player foundation. NightWave
remains serverless and music-first. This phase does not add licensed music
sources, change the Media3 playback engine, rewrite the saved library, touch
downloaded audio, modify Google Drive credentials, change LRC content or
alter APK package/signature identity.

## Delivered

### Search: cleaner hierarchy and honest result provenance

- A compact, themed **Explore sound** header replaces the loose heading,
  with restrained ambient geometry and without invented covers or artwork.
- Search input is easier to identify and has a stable QA test tag.
- Saved local-library matches and public video results receive **distinct
  source headers and counts**. Public YouTube entries are explicitly
  labeled as internet-dependent; saved video references are only called
  saved entries, **not automatically downloadable/offline audio**.
- Existing local creator browsing, mood shortcuts, typo/search implementation,
  result identity guards, YouTube handoff, filters, existing local matches,
  quick in-app search, provider errors and all play/options handlers remain.
- Result section badges only show counts already calculated by the existing
  search code. No fabricated artist profiles, song rankings or provider data.

### Library: compact mobile-first hierarchy

- A new themed header displays the real collection name and saved-track
  counts while preserving the Back-to-Library action.
- The two main collection shortcuts, **Offline music** and **Liked songs**,
  are stacked for available width under **440dp** and presented side by side
  when enough room is available. Only the **layout** changes.
- Playlist rows use smaller 54dp thumbnail/placeholder artwork, clear title
  and count, restrained outlines, and a compact card shape.
- Track rows use authentic upstream cover imagery when present, with a
  neutral fallback otherwise; the current-playing row is distinguished
  by a subtle accent.
- Existing user-curated playlist titles, sort/filter behavior, library
  state snapshots, local/offline album sources, share/import actions,
  saved URL identity, queue behavior and resume action remain unchanged.
- `MusicCollectionTile` gains an optional `Modifier` parameter while
  retaining the trailing click callback for old callers.

### Performance, accessibility and safety

- New artwork visuals are static, Compose-native gradients and lines, not
  paid AI images, external graphics, or expensive per-frame blurs.
- The layout uses existing Coil thumbnail rendering, with **no additional
  image fetches** required for the new headers.
- Responsive collection layout is decided from actual available Compose
  constraints; NaN/undefined widths fall back to a vertical stack.
- No new background service, OAuth scope, storage permission, analytics
  upload, notification or network endpoint is introduced.

## New checks

- **9 JVM policy tests:** collection counts, safe title display, data-source
  descriptions, 320–740dp breakpoints and tile dimensions.
- **4 isolated Compose Android UI tests:** 320dp Search and Library headers,
  local/public label separation, and collection click behavior without
  touching music or a playback service.
- `tools/verify_v2_phase6v7b.py --self-test` detects missing playlist/play
  wiring and invalid offline claims in synthetic source contracts.
- `tools/check-phase6v7b-integration.ps1` provides a Python-free,
  read-only pre-Gradle source audit on Windows.
- Both are connected after previous phase gates and before
  `:app:testDebugUnitTest`, `:app:assembleDebug`, and
  `:app:compileDebugAndroidTestKotlin` in the existing pipelines.
- A standalone Kotlin compiler smoke with **17 pure policy assertions**
  passed locally. This does **not** compile Compose or certify the APK.

## When ready for consolidated device testing

1. Download the **complete pinned 6V7B source** to a fresh short folder,
   then run `build-windows.cmd`. Verify every stage and final
   **BUILD SUCCESSFUL**. Stop if Kotlin, Compose, JVM tests, Android
   test-source compile or signing fails; never install a previously built APK.
2. Run `check-device-update.cmd` before any in-place installation.
   Do not uninstall, clear user app data, overwrite offline tracks or
   create an incompatible signing identity.
3. Test Search at **320dp, 360dp, 440dp and tablet width**, using saved music
   and a separate public-video query. Validate visible headers, counts,
   authentic thumbnails, keyboard insets, filters and empty/error states.
4. Test Library with **0, 1 and many playlists**, long titles, empty
   playlists, downloaded MP3s, liked songs and large saved lists.
   On narrow screens shortcuts stack; on wider screens they fit side by side.
5. Verify every playlist still opens, sorts, filters, shares, imports and
   plays the intended tracks. Open an offline collection and confirm it
   still works in airplane mode.
6. With music playing, switch tabs, open Now Playing/Queue, minimize and
   restore. The UI change must not interrupt Media3 or alter playlists.
7. Run the older 6V6A–6V6G lyrics, cloud, queue, playback and
   app-signature regression suite on a disposable emulator.

## Release constraints

A GitHub Actions runner must start and complete actual Gradle compilation
before this branch can be considered build verified. The recent GitHub
workflows have failed **before runner allocation**, so source inspections,
test declarations and isolated compiler checks are not a replacement.
This phase is intentionally an **unmerged draft** until a complete release
verification becomes available.

Next proposed phase: **6V7C — Now Playing polish and accessible gestures**,
then consolidate the visual QA before changing playback internals.
