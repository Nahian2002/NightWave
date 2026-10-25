# NW41 — Recorded-device visual audit and first premium system pass

**Source:** Two Android screen recordings provided 25 October 2026 (~79s and ~84s).  
**Working branch:** `feature/nightwave-nw41-premium-experience-auth-diagnostics`  
**Status:** Source implementation; **full Gradle compile and Android device testing not yet verified**.  
**Scope:** Presentation, read-only OAuth troubleshooting; not a downloader, account or playback rewrite.

## Observed in the recordings

- **Home/Search:** small header branding, stacked discovery CTAs, fragmented search controls, explanatory text competing with results. Search opened a distinct YouTube surface that did not match NightWave's visual language.
- **Now Playing:** artwork and transport were functional, but the surrounding menu/queue, sound and lyrics elements were crowded and not consistently prioritized.
- **Downloads:** tall uniform cards for downloading and previously saved entries, textual-only progress, repeated 48dp link rows, and completed WorkManager records whose actual local audio was missing. One observed active transfer displayed a low speed; no network-speed root cause can be inferred from the recording alone.
- **Library:** all-purpose tall collection tiles and menu actions. The song-action alert sheet used a light gray native Android surface unlike the rest of NightWave.
- **You / Google:** account chooser appeared but Google Drive authorization ended with **internal error (8)**. A visible chooser does not prove OAuth permission or a connected backup account.

## Implemented in NW41

1. **Coherent typography**: improved small/body label readability, stronger title scale, consistent editorial headline spacing while retaining original theme selection and color model.
2. **Shared song and collection treatments**: fine outline hierarchy, quieter inactive result cards, mint outline for the active song, more deliberate selected-state visual treatment; library/playback data not changed.
3. **Bottom navigation**: slightly larger icons and labels, clearer selected pill, retained four persistent destinations and existing test tags/accessibility semantics.
4. **Discovery header**: reduced secondary CTA height, simplified copy, less oversized space above useful search results. The action still opens the authorized offline catalog, not a different source.
5. **Downloads**: visible thin byte-progress bar (determinate when WorkManager supplies an expected total; indeterminate otherwise); compact two-column entry actions, less padding, tighter active Pause/Cancel row, more visible unavailable-file state, and an accessible history-hide/restore control by each completed row title. Pause **still restarts the download**; it is not presented as byte-level resume. Records with missing private audio are **never** marked as playable just because WorkManager once succeeded.
6. **Native dialogs**: root Android dialog palette now uses a consistent dark NightWave rounded background and mint accents. This applies to legacy song-action/confirm dialogs without changing their callbacks or file writes.
7. **Google status 8 diagnostics**: added InstalledApkOAuthIdentity with read-only package ID, current installed APK signer SHA-1 (Google Console colon-separated format), Drive scope and Play Services status. An account-setup card under **You > Account & backup** displays the SHA-1 and has **Copy Google setup report**. The report contains no Google email/password/access token/Drive file content.
8. **New Android instrumentation source**: checks the on-device SHA-1 shape and validates that the report uses this app package and contains only public setup facts.

## What NW41 does NOT claim

- **Google isn't fixed by styling.** After account selection returns status 8, the configured Google Cloud project may need an Android OAuth client registered for **com.nightwave.player and the actual installed certificate SHA-1**, with Drive API enabled and the user included in the test audience if testing. This is an actionable diagnostic, **not a confirmed root cause or automatic registration**.
- No guarantee any searched YouTube track is licensed for download; NightWave's existing offline source rules remain.
- No guarantee of faster transfer rates or true byte-resume: server support, source availability and WorkManager details still need investigation.
- No claim that one styling pass makes every Activity, modal, widget and tablet layout final or device-tested.
- No changes to app signing keys, Google Cloud registration, previously downloaded audio, playlists, Room writer, cloud tokens, main branch or user's installed app.

## Required QA before an APK is approved

1. Run `build-windows.cmd` from exact NW41 checkout and inspect build-status/log/test failures. The app/Android test source must compile.
2. Before updating the installed app, run `check-device-update.cmd` for matching package and signing certificate; never uninstall/clear app data to force signature replacement.
3. Phone: at 360–430dp width and both 100% and 150–200% font scales, inspect Home/Search/Library/You/Downloads/full player, status/nav inset clipping, no unexpected scroll or non-clickable targets.
4. Downloads: start a permitted job and check the progress bar as WorkManager updates; confirm Pause/Cancel and retry semantics are unchanged; don't hide missing-file warnings.
5. Check previous native song menu and destructive confirmation on light/dark/AMOLED themes; ensure text visibility and 48dp options.
6. You > Account & backup: copy the report; verify signer SHA-1 matches Google Cloud Console registration for this same installed APK. Verify Drive API, OAuth audience/test-user settings and Play Services. Never share tokens, passwords, or private account email in diagnostics.
7. Confirm background audio, lock-screen controls, queue, search results, already-saved audio, playlists, NW39 multi-select/Undo/reorder, local export and cloud busy/reconnect still function.
8. If the Google error persists after verifying Cloud settings, attach only a **redacted** test log containing the numeric status and whether the permission sheet opened. No private account or token data.

**Next dedicated visual pass:** rework Search source-switching and result grouping; design an intentional compact player-action bottom sheet; unify Library/You section density; refine empty/error states, animations/reduced-motion and tablet layouts with Android screenshot proof.
