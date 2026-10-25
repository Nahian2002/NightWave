# NW52 pre-build design, actions and reliability audit — 25 Oct 2026

**Candidate:** `feature/nightwave-nw52-final-integration-release-gates` (the NW43–NW52 stacked branch).  
**Conclusion:** SOURCE CONTRACTS REVIEWED; **full Android compilation, render QA and runtime behavior NOT VERIFIED**. The user may attempt a local **private development build** after obtaining a complete source ZIP; **public distribution remains NO-GO**.

## Confirmed issues fixed on this exact NW52 branch

1. **Home / Find music routing:** NW47 incorrectly issued the generic query `music` when the user tapped **Find music**, rather than opening Search. Home's empty **Discover music** action had the same behavior. Both now route directly to `StudioTab.SEARCH` without auto-sending a fake query. Real mood shortcuts preserve their own targeted queries.
2. **Now Playing / Save offline with no song:** disabled an inert button when `state.hasTrack=false` and added **Choose a track first**, preventing a tap that silently did nothing. When an offline song is playing, the existing action still opens its library entry.
3. **Collapsed mini-player / restoration touch area:** increased from 40dp to 48dp while preserving active playback and one-tap restoration.
4. **Offline Library / Google Drive shortcut:** expanded its touch height from 44dp to 48dp, preserving the original Drive destination.
5. **Offline Library / sort accessibility:** added and refreshed a full `contentDescription` so an ellipsized sort label can still be announced correctly.

## Source-level static review (not rendering or compilation)

- Changed Kotlin production and test files from NW43–NW52: **44 key files** reviewed with balanced delimiter, string/comment end-state and duplicate-import checks; no issue found in these checks. The Python/PowerShell gates are source-text contracts and do not evaluate Kotlin type resolution or Material3 APIs.
- Home: real navigation callbacks, recently played Play / Options / Remove separation, saved library routes and intentional mix search.
- Search: NW43 selected-track-only playback, NW44 smart ranking, basic saved/offline filtering and NW51 asynchronous offline index. Search can briefly display no offline suggestions before its async index loads; runtime UX is pending.
- Library: saved item actions, playlist bulk/undo, offline file-health filters and read-only recovery states. NW48 folder/queue correctness still needs installed-device regression.
- You: account/Drive, privacy and Sound Studio buttons use the original handlers; account authentication is still externally blocked/unverified.
- Now Playing: artwork bounds, reduced motion, unique queue entry action, track-fenced seek and LRC, active/offline states and explicit player status.
- Sound Studio: actual device EQ band request and apply state, conservative curves; output route support remains device-dependent.
- Motion: bounded player entrance, 48dp navbar actions, reduced-motion fallbacks, haptic guards and no infinite visual loops introduced by NW50 source. Real smoothness/frame pacing not measured.
- Downloads: WorkManager active/ready/attention filters, actual saved-file status, honest stalled-rate and stop/restart-from-zero wording. True HTTP Range byte resume remains unavailable.
- Privacy: app backup remains disabled and cleartext traffic disabled; no extra exported component or permission introduced by these UI fixes.

## Additional preflight introduced

- `tools/verify_nw52_ui_prebuild.py`: **28 static UI/source invariants** plus six deliberately broken fixtures (`--self-test`) covering Search routing, inert buttons, recent removal, tab reselect, offline health and real saved-file readiness.
- `tools/check-nw52-integration.ps1` now enforces critical corrected routes/button guards before the Windows builder invokes Gradle, with no Python requirement.
- GitHub Android verification workflow invokes the Python source check. **The repo's hosted runner has not executed jobs**: earlier actual NW52 canary and Android jobs returned `runner_id=0`, `steps=[]`, so there is no pass/fail compiler result from CI.

## Known outstanding hard gates

**A. Kotlin/Android build and tests — NOT RUN:** source text checks cannot catch unresolved symbols, incompatible Compose overloads, or failing JUnit assertions. The local Windows build must pass `:app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin`, signer fingerprint and fresh APK identity. Do not reuse an older APK after a failed build.

**B. Visual/device QA — NOT RUN:** the new Home shelf, offline filter-row heights, Now Playing artwork and lyrics, NightWave nav and UI under 320/360/390dp, 2× text, TalkBack, dark/light, rotation/split-screen and Samsung screen insets must be reviewed on-device. Code inspection cannot tell whether glyphs clip or any frame stutters.

**C. Functional runtime — NOT RUN:** Google OAuth error 8 account connection, actual Drive backup/restore, downloaded file playback after network loss, pause/resume/seek/shuffle/repeat, gapless/crossfade and Bluetooth/background notifications need device evidence.

**D. Public release — NO-GO:** API target 35 instead of required 36 for 2026 Play submissions, production signing/AAB, platform/media rights, hosted privacy disclosures, full-history secret audit and asset/dependency legal checks remain outside a source-only UI audit.

## Safe next execution

1. Download **Code → Download ZIP** from exact NW52 branch, extract completely into a new folder.
2. Run `build-windows.cmd`. It now requires the NW52 PowerShell source guard before Gradle and must write a new attempt-specific status.
3. If build fails, provide `dist/NightWave-build-errors.txt`, `dist/NightWave-build-status.txt`, `dist/NightWave-build-diagnostic.txt` if produced, and the full Gradle log from **that attempt**. Do not install.
4. If build succeeds, run `check-device-update.cmd` and verify installed signing identity before any upgrade; **do not uninstall/clear local music**.
5. On-device audit: screenshots/video for every tab, Now Playing, queue, downloads, Offline Library, You/Sound Studio, search/Google auth. Fix actual clipping and runtime errors before publication.

**Source-audit conclusion:** Several confirmed interaction/accessibility regressions have been fixed. **Zero critical structural-text errors were observed in the reviewed Kotlin source set**, but there is no justified claim that the entire app is build-clean, visually perfect, crash-free, or public-release-ready.
