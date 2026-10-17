# NightWave

## NightWave 2.0 Alpha 5P — Safe Up Next swipe and Undo

**Version:** `2.0.0-alpha5p` · **Build 32** · Branch: `feature/nightwave-v2-phase5p-safe-queue-swipes` (draft).

[Full Phase 5P source ZIP](https://github.com/Nahian2002/NightWave/archive/refs/heads/feature/nightwave-v2-phase5p-safe-queue-swipes.zip) · [Phase 5P Android/queue QA](docs/NIGHTWAVE_V2_PHASE5P_QA.md)

- **Up Next gestures:** Swipe **left** on a *future public track* to remove it from this listening queue, or tap the accessible Remove action. A brief Undo snackbar can restore the entry in its original order.
- **Protection by design:** current and previously played tracks, offline-file queues, and shuffle mode never allow swipe removal. Neither Remove nor Undo touches saved playlists, liked songs, downloaded/imported files or the real playback stream.
- **Service ownership:** queue changes happen inside the Media3 playback service, guarded by the expected absolute index + public URL and one-shot UUID Undo token. Playback advancement and other queue actions invalidate stale undo state. UI refreshes from the persisted queue asynchronously.
- **Motion and access:** 72dp left-swipe threshold, visually restrained Remove reveal, 48dp Remove tap target, Undo below large queues, no entry removal on a short/right drag and Reduced Motion-compatible settling.
- **Testing:** pure JVM Kotlin queue logic compiled and passed **19 isolated checks**. Added eight repo JUnit queue tests, two nondestructive Compose sheet gesture/Undo tests and `tools/verify_v2_phase5p.py` source gate.
- **Retained:** Phase 5O horizontal tabs, mini-player swipe-up/down, album-art next/previous, full-player swipe-down, compact You, Android launcher, imported music data, app ID and signing identity.

**Limitations:** Android Gradle, Compose UI instrumentation, real Media3 service delivery, signed APK and phone playback/crash tests are **not verified** in this environment. Stable main remains unchanged; keep this PR draft until Windows and Android acceptance pass.

**Next:** **5Q** fix the native search failure seen on the phone, then **5R** integrate motion/accessibility and perform genuine Android build/phone stability QA.

---

## NightWave 2.0 Alpha 5O — Swipe across all four main pages

**Version:** `2.0.0-alpha5o` · **Build 31** · Branch: `feature/nightwave-v2-phase5o-swipe-tab-pager` (draft).

[Download the Phase 5O complete source ZIP](https://github.com/Nahian2002/NightWave/archive/refs/heads/feature/nightwave-v2-phase5o-swipe-tab-pager.zip) · [Phase 5O gesture/Android QA](docs/NIGHTWAVE_V2_PHASE5O_QA.md)

- **Native horizontal swiping:** Swipe Home ↔ Search ↔ Library ↔ You through a Compose HorizontalPager; the bottom navigation automatically tracks the visible page.
- **All navigation agrees:** bottom tabs, the Settings gear, Home discovery shortcuts, Library navigation and system Back use the same saveable pager position rather than competing page states.
- **Gesture isolation:** page swiping is disabled behind full-screen Now Playing and Up Next, while artwork left/right track switching, player swipe-down to dismiss and mini-player up/down gestures remain as before. Vertical music list scrolling and horizontal avatar chips remain separate.
- **Reduced motion and keyboard:** bottom-tab transitions honor Reduced Motion, and changing pages releases text focus and closes the keyboard.
- **Accessibility:** selected tab semantics/test IDs; added two Android instrumentation tests for swipes, vertical scrolling and tab synchronization.
- **No destructive changes:** existing premium UI, compact You settings, classic fallback, imported audio, offline resume, playlists, media notifications and app identity are unchanged.

**Verification:** source-level structural checks added in `tools/verify_v2_phase5o.py`. **Android Gradle, UI tests, APK signing, emulator and phone gesture QA have not run in this environment**. Keep this Phase 5O PR draft; don't clear/uninstall the previous app to bypass a signing mismatch.

**Next:** Phase 5P — queue-only swipe-to-remove with Undo, followed by native search recovery in 5Q and accessibility/stability QA in 5R.

---

## NightWave 2.0 Alpha 5N — Compact premium You tab

**Version:** `2.0.0-alpha5n` (build **30**) · Branch: `feature/nightwave-v2-phase5n-compact-profile` (draft).

[Full Phase 5N source ZIP](https://github.com/Nahian2002/NightWave/archive/refs/heads/feature/nightwave-v2-phase5n-compact-profile.zip) · [Design and swipe roadmap](docs/NIGHTWAVE_V2_PHASE5N_QA.md)

- **Redesigned You:** condensed avatar/name header, inline profile editing and compact grouped panels, with tighter spacing and restrained typography.
- **Appearance & playback:** themes and avatar choices open on demand; reduced-motion switch stays one tap away.
- **Library & backup:** export, confirmed restore and protected undo are preserved under an expandable row. Advanced Room/DataStore read previews are collapsed by default. Classic UI fallback and crash diagnostics remain accessible.
- **Zero migration:** original music, local playlists, offline files, queue/bookmarks, app ID, signing identity, premium launcher and media notification paths are unchanged.
- **Gesture development, ordered:** **5O** swipe between four premium tabs; **5P** safe mini-player and Up Next gestures with Undo for queue-only removals; **5Q** native discovery/search recovery; **5R** animation, accessibility, Android build and playback/crash acceptance.

Added `tools/verify_v2_phase5n.py` and two Android UI test cases. Source readback passed 20 targeted checks. **Android Gradle, emulator and phone validation remain pending**; don't install over an existing app until a real signed APK build and read-only device-update check succeed. Stable `main` remains unchanged.

---

## NightWave 2.0 Alpha 5M — Premium interface by default

**Version:** `2.0.0-alpha5m` · **code 29** · **draft branch:** `feature/nightwave-v2-phase5m-premium-default`.

[Download complete Phase 5M source ZIP](https://github.com/Nahian2002/NightWave/archive/refs/heads/feature/nightwave-v2-phase5m-premium-default.zip) · [Phase 5M Android navigation and QA](docs/NIGHTWAVE_V2_PHASE5M_QA.md)

- **New default:** NightWave's redesigned Home/Search/Library/You app is Android's sole launcher and opens from the media playback notification. The old early-access label is removed.
- **Real Settings:** The premium header gear opens **You**, not the old interface. The classic screen is available explicitly under **You → Interface fallback → Open classic interface**. Use **Return to NightWave** in classic Home to come back without duplicating activities.
- **Shares stay premium:** External YouTube watch links and shared plain text now enter the new UI, not the old one. Valid watch links play through NightWave's existing Media3 service; other text opens native Search. Config changes do not replay an external share.
- **Navigation:** Android Back dismisses queue/full player/playlist first, then navigates to premium Home; it doesn't unexpectedly return to classic UI.
- **Data and stability:** Existing app ID, saved profiles, offline audio, queue/bookmarks, existing signing configuration, crash diagnostics, and original Android playback service remain intact. The entire classic UI remains installed as a non-exported in-app fallback.

**Verification:** Added `tools/verify_v2_phase5m.py` and two emulator/device instrumentation tests validating launcher and share routing. **No real Android Gradle build, emulator run, APK install or phone playback test was possible in this environment.** Stable `main` is unchanged; the PR stays draft pending local Windows/phone QA.

---

## NightWave 2.0 Alpha 5L — Private crash evidence & correct update checks

**Version:** `2.0.0-alpha5l` (build 28). **Draft branch:** `feature/nightwave-v2-phase5l-private-crash-capture`.

[Download Phase 5L complete source ZIP](https://github.com/Nahian2002/NightWave/archive/refs/heads/feature/nightwave-v2-phase5l-private-crash-capture.zip) · [Phase 5L privacy and Android QA](docs/NIGHTWAVE_V2_PHASE5L_QA.md)

- **One-click sanitized Android crash summary:** `capture-nightwave-crash.cmd` now finds ADB in the SDK prepared by NightWave's own Windows builder, standard SDK paths or PATH. It writes only **`dist/NightWave-crash-summary.txt`** with recent NightWave-only fatal exception class names, method frames and timestamps, never raw other-app/system logs. It does not modify the phone or clear its logs.
- **Testable without a phone:** run `powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools\capture-nightwave-crash.ps1 -SelfTest`; a synthetic NightWave crash and unrelated app crash test the privacy filter. The Windows builder requires this self-test before APK compilation.
- **Update safety:** the connected-device APK checker now compares parsed build-status version/code directly against the current `$versionName` and `$versionCode`, avoiding another hardcoded-alpha mismatch.
- **Session cleanup:** the premium Media3 bridge releases its disconnected future before allowing user-initiated reconnect, preserving all imported audio and existing queue state.
- **Previously shipped Phase 5K in-app crash/ANR/low-memory exit report is unchanged**, as are the premium player and offline recovery behavior. Stable `main` and signing identity are untouched.

**Verification:** source-level checks and earlier isolated JVM Kotlin smoke harnesses pass, but **the new PowerShell self-test, Android Gradle build, signed APK, emulator and phone checks have not run here**. This is a draft development branch; do not uninstall NightWave or clear user data to work around a build/signature error.

---

## NightWave 2.0 Alpha 5K — Crash triage without data loss

**Version:** `2.0.0-alpha5k` (build 27). **Draft branch:** `feature/nightwave-v2-phase5k-crash-triage`.

[Download complete Phase 5K source ZIP](https://github.com/Nahian2002/NightWave/archive/refs/heads/feature/nightwave-v2-phase5k-crash-triage.zip) · [Crash triage and Phase 5K QA](docs/NIGHTWAVE_V2_PHASE5K_QA.md)

- **Safer playback controls:** Play/Pause and Seek now handle synchronous Media3 controller exceptions when Android disconnects a session during a tap. Only the stale Activity/controller connection is discarded; NightWave retains the saved queue, offline files, bookmarks and original playback service.
- **App health in the premium UI:** Open **You → View / share crash summary** to inspect a local report before explicitly choosing to share it. No telemetry uploads automatically. The classic screen's diagnostics entry also remains available.
- **Android 11+ system exit reasons:** A privacy-limited summary now shows up to 5 recent app process exits, such as Java/native crash, ANR, low-memory termination or startup failure, with UTC timestamps. It never reads full system traces, exit descriptions, process names, identifiers, local songs or account information. This may provide evidence when Android displays a frequent-crash warning but the app's exception recorder is empty.
- **Verification:** An isolated Kotlin compilation with Android API stubs passed 14 behavior checks for the new process-exit report. Five other crash-aware/device/queue checks remain source-only or emulator-gated. `tools/verify_v2_phase5k.py` and a non-destructive Android instrumentation test are included.
- **Device-update gate repaired:** the checker previously still matched the older alpha5i build-status string despite updated version variables, incorrectly rejecting valid new builds. Its exact status regex and regression checks now match alpha5k/code 27.
- **Safety:** The existing local Windows build/signature and connected-device update checks remain present, with `2.0.0-alpha5k` code 27. No user-data migration, deletion, signing-key replacement or app-ID change.

**Release status:** The recurring crash warning is **not diagnosed or proven repaired** without the actual Android device evidence. Android Gradle, physical-phone and instrumentation runs remain **PENDING** due to missing Android SDK/Gradle and unavailable downloads in this environment. Keep this PR draft and stable `main` unchanged.

---

## NightWave 2.0 Alpha 5J — Player-state efficiency

**Version:** `2.0.0-alpha5j` (build 26). **Branch:** `feature/nightwave-v2-phase5j-player-state-efficiency` (draft).

[Download complete Phase 5J source ZIP](https://github.com/Nahian2002/NightWave/archive/refs/heads/feature/nightwave-v2-phase5j-player-state-efficiency.zip) · [Phase 5J player-state QA guide](docs/NIGHTWAVE_V2_PHASE5J_QA.md)

- **Targeted Android UI responsiveness fix:** the premium player's 750 ms position ticker previously loaded and parsed saved queue / offline-session preferences on each tick. Its new fast path updates position, duration and play/buffering state straight from the already-connected Media3 controller, without synchronous saved-library reads.
- **Safety:** Media3 events still update song metadata and queue immediately, a periodic full refresh catches queue Intent changes, and returning to the Activity forces one full reconciliation. Leaving and returning from Offline Library invalidates the cached imported-track preview so deleted/added local songs are reflected.
- **No playback-engine rewrite:** the Media3 background service, stream resolver, audio formats, queue persistence, preferences and downloaded/imported audio bytes are unchanged.
- **Release controls retained:** local Windows build/signing gates and explicit read-only device-APK compatibility check from Alpha 5I are carried forward with versionCode 26.
- **Tests:** the pure Kotlin PlayerSnapshotPolicy compiled under Kotlin CLI and passed 8 focused assertions. A repository JUnit test with five scenarios and source gate `tools/verify_v2_phase5j.py` were added.

**Verification limitation:** Full Android Gradle, repository JUnit, Windows PowerShell, real Android device profiles and previous crash reproduction have **not** run in this environment. The Phase 5J PR remains DRAFT; do not uninstall/clear NightWave or merge into stable `main` until Windows and phone QA pass.

---

## NightWave 2.0 Alpha 5I — Safe on-device update preflight

**Version:** `2.0.0-alpha5i` (build 25). **Draft branch:** `feature/nightwave-v2-phase5i-device-update-check`.

[Download Phase 5I complete source ZIP](https://github.com/Nahian2002/NightWave/archive/refs/heads/feature/nightwave-v2-phase5i-device-update-check.zip) · [Read-only device check and Phase 5I QA](docs/NIGHTWAVE_V2_PHASE5I_QA.md)

- **New: `check-device-update.cmd`.** After a verified local Windows build, connect one authorized Android phone and run this manually to compare the new APK against the package **actually installed on that phone**. It performs read-only ADB package discovery and reads only the app APK, not music, private data or account content.
- **Signer and downgrade safety.** The check refuses a mismatched APK signing certificate, newer installed NightWave version, unauthorized/multiple devices, stale or altered local APK, and unexpected device responses. It returns `PASS - IN-PLACE UPDATE SIGNER MATCH`, `PASS - FRESH INSTALL`, or `INCONCLUSIVE`. It never installs, uninstalls, resets, or changes user data.
- **Builder support.** The existing local (zero GitHub Actions billing) Windows builder now prepares Android SDK Platform Tools and validates source scripts before build. All prior APK identity/signature checks remain active.
- **Nothing changes in music playback logic.** Media3, offline resume, classic UI, premium player, public search, app identity and local music persistence stay as they were in 5H.

**Verification:** Prior isolated Kotlin smoke harnesses were re-run (38 behavior assertions passed), and Phase 5I has a source-only contract gate. **A complete Android Gradle build, Windows PowerShell execution, connected-device check, APK install and Samsung playback/crash test have NOT occurred.** Stable `main` is unchanged and this phase stays draft.

---

## NightWave 2.0 Alpha 5H — APK verification and safer in-place updates

**Version:** `2.0.0-alpha5h` (build 24). **Draft branch:** `feature/nightwave-v2-phase5h-apk-signing-gates`.

[Download full Phase 5H source ZIP](https://github.com/Nahian2002/NightWave/archive/refs/heads/feature/nightwave-v2-phase5h-apk-signing-gates.zip) · [Phase 5H release/smoke guide](docs/NIGHTWAVE_V2_PHASE5H_QA.md)

- Adds official Android Build Tools `aapt` **package/version** checks and `apksigner` **cryptographic APK-signature** verification to the existing **local, zero-GitHub-billing** Windows builder.
- If a previous locally built NightWave APK is available, its signing certificate **must match** before the new APK is published. If no previous APK exists, signing continuity with the app already installed on your phone remains **unverified**; do not uninstall to work around failed update signatures.
- Preflight parses both PowerShell build scripts and records errors even when an invalid script prevents Gradle from running; old built APKs are archived instead of presented as fresh build output.
- Adds non-destructive Android instrumented startup checks for classic and premium NightWave, designed to run on an emulator/disposable profile.
- No changes to actual audio playback, YouTube backend, private offline files, persistent profile/library schemas, Android app ID or existing signing keys.

**Status:** Previously authored pure JVM tests were re-run (38 checks passed). Android Gradle compilation, Windows PowerShell execution and APK-device compatibility remain **PENDING** because this environment has no Android SDK/Gradle and cannot download them. The longstanding crash report still needs device evidence. The PR remains draft, stable `main` unchanged.

---

## NightWave 2.0 Alpha 5G — Reliability and verifiable builds

**Version:** `2.0.0-alpha5g` (code 23). **Draft branch:** `feature/nightwave-v2-phase5g-reliability-diagnostics`.

[Download complete Phase 5G source ZIP](https://github.com/Nahian2002/NightWave/archive/refs/heads/feature/nightwave-v2-phase5g-reliability-diagnostics.zip) · [Phase 5G QA / startup and crash checklist](docs/NIGHTWAVE_V2_PHASE5G_QA.md)

- Media3 controllers now recover from failed connections, report session disconnection, and expose manual Reconnect instead of becoming permanently stuck.
- Startup no longer depends on successful public-video extractor initialization or the optional Room/DataStore preview; the original library and offline music remain untouched.
- Privacy-minimal in-app crash summaries include the installed app version, making old and new failures easier to distinguish.
- The free **Windows local builder** retains its previous working APK separately, generates a failure summary for caught preparation errors, and publishes a fresh APK only after Gradle reports BUILD SUCCESSFUL and Android Build Tools verify `com.nightwave.player` / versionCode `23`. The status and SHA-256 appear in `dist/NightWave-build-status.txt`.
- Previous Alpha 5F player visuals/gestures and Phase 5D offline resume are retained.

**Verification:** Source wiring checks and new `tools/verify_v2_phase5g.py` were added. **Actual Windows PowerShell, Android Gradle/JUnit, emulator and phone tests have not been executed in this environment.** The original Android frequent-crash cause is not yet diagnosed; use **Diagnostics · Share recent crash summary** in classic Home or the USB log script for actionable evidence. Leave this PR DRAFT until tested. Do not uninstall or clear NightWave's existing data.

---

## NightWave 2.0 Alpha 5F — Immersive player motion

**Version:** `2.0.0-alpha5f` (code 22). **Branch:** `feature/nightwave-v2-phase5f-player-motion` (draft).

[Download complete Phase 5F source ZIP](https://github.com/Nahian2002/NightWave/archive/refs/heads/feature/nightwave-v2-phase5f-player-motion.zip) · [Phase 5F QA and known limits](docs/NIGHTWAVE_V2_PHASE5F_QA.md)

- Full-screen album artwork supports guarded left/right Next/Previous swipes, with a deliberate 88dp threshold and a no-action rule during buffering, disconnection or single-track playback.
- Improved NightWave player backdrop, subtle artwork transitions that respect Reduced Animations, loading feedback over the cover, and reliable placeholder rendering when an upstream thumbnail fails.
- Mini-player includes Next where a multi-track queue exists, while tap-to-expand and vertical collapse/expand gestures remain.
- Progress and seek calculations are bounded and scoped to each track; an unfinished scrub cannot seek the next song. Full player offers Retry when playback reports an error.
- Previously implemented offline library, explicit offline session recovery, queue, native search, Home/Library design and stable app data remain unchanged. No YouTube downloading or new privacy-sensitive behavior.

**Verification:** Isolated Kotlin CLI compilation of the pure swipe/progress rules passed 23 focused assertions; Android JUnit, Compose compilation, APK assembly and phone testing are still **PENDING**. Run `build-windows.cmd` before any install and do not uninstall/clear the existing app. Keep this PR DRAFT until hardware QA and outstanding crash evidence are addressed.

---

## NightWave 2.0 Alpha 5E — Discovery and Library redesign

**Version:** `2.0.0-alpha5e` (code 21). **Draft branch:** `feature/nightwave-v2-phase5e-discovery-polish`.

[Download complete Phase 5E source ZIP](https://github.com/Nahian2002/NightWave/archive/refs/heads/feature/nightwave-v2-phase5e-discovery-polish.zip) · [Full Phase 5E QA guide](docs/NIGHTWAVE_V2_PHASE5E_QA.md)

- Home now opens with a real last-listened song spotlight when local recent history exists. It shows actual saved video metadata and artwork; when empty it clearly offers discovery rather than fake recommendations.
- Six curated mood/search shortcuts and four Search-tab shortcuts send ordinary public queries through the existing native gateway, while YouTube browser fallback remains available.
- Library now displays compact saved-artwork collection tiles with real track counts, quick access to recent likes, and title/artist filtering inside saved collections. Current playback is visually highlighted and missing artwork has a branded fallback.
- Rounded, refined bottom navigation animates its selection subtly but disables that animation for Reduced Animations. Android Back navigates inside the premium UI before exiting.
- Existing premium player, Phase 5D offline session recovery, public-video streaming paths, legacy library, original Android launcher and user data remain unchanged. No paid resources or cloud login were introduced.

**Verification status:** A standalone Kotlin CLI smoke run of the pure discovery shortcuts passed seven assertions. Remote Kotlin source review and a new static gate were authored. **An actual Android Gradle build and phone UI/playback tests have NOT been executed here.** Do not install or merge until `build-windows.cmd` succeeds and device QA passes; never uninstall or clear existing music data.

---

## NightWave 2.0 Alpha 5D — Offline session resume & premium Up Next

**Version:** `2.0.0-alpha5d` (code 20). **Draft branch:** `feature/nightwave-v2-phase5d-offline-resume`.

[Download full Phase 5D source ZIP](https://github.com/Nahian2002/NightWave/archive/refs/heads/feature/nightwave-v2-phase5d-offline-resume.zip) · [Complete Phase 5D QA instructions](docs/NIGHTWAVE_V2_PHASE5D_QA.md)

- Remembers the last imported song, its position, and the ordered offline playback list in isolated, integrity-checked app-private preferences. The session is resumed **only when you request it**; no autoplay on service creation or relaunch.
- Adds **Resume** to the classic Offline library and **Continue Listening** to the premium Library. Offline Up Next now shows imported songs and allows jumping to another one.
- In the premium player, controls meant only for online songs (YouTube favorites, playlist saving, shuffle and repeat) are disabled for private offline files rather than accidentally affecting your streaming queue.
- Retains Phase 5C local search/sorting/Play All, Media3/background controls, imported audio storage, existing library, playlists, guest profiles and public-video playback.
- Includes six pure decoder JUnit test cases, an offline queue jump case, and a Phase 5D source gate. **No Android Gradle build or phone QA has run here**, so this is not yet a release APK.

**Safety:** do not uninstall or clear app data, and keep the PR draft until `build-windows.cmd` and device QA succeed. The earlier Android crash warning still requires actual logcat evidence to diagnose.

---

## NightWave 2.0 Alpha 5C — Offline-first Library and Queue

**Version:** `2.0.0-alpha5c` (code 19) · **Development:** `feature/nightwave-v2-phase5c-offline-queue`.

[Get complete Phase 5C source ZIP](https://github.com/Nahian2002/NightWave/archive/refs/heads/feature/nightwave-v2-phase5c-offline-queue.zip) · [Phase 5C testing and limits](docs/NIGHTWAVE_V2_PHASE5C_QA.md)

- The imported-audio library now supports instant local title search, four sort modes, **Play all**, and tapping a song to play it within the currently visible ordered list.
- Next/Previous (including Bluetooth/headset controls) and end-of-song auto-advance work across imported tracks without network access. Missing files are skipped; private file IDs never enter public YouTube queues or favorites.
- Offline metadata and queue UI remain distinct from online playback, avoiding stale YouTube artwork, old track titles, and stale public queue rows.
- The offline queue exists only while the playback service lives; **imported audio files remain saved**, but automatic queue restoration after service death is not yet implemented.
- Previous Phase 5A/5B launch protection, native search, retry and classic player remain. **No YouTube downloading** or cloud login/sync.

**Verification:** the standalone offline queue compiled under JVM Kotlin and passed seven targeted behavior checks. This is **not** a successful Android build or phone test. Run `build-windows.cmd` and real-device QA before installing or merging. Preserve installed app data; leave the PR draft until acceptance.

---

## NightWave 2.0 Alpha 5B — Native Search & Playback Improvements

**Version:** \`2.0.0-alpha5b\` (code 18). **Branch:** \`feature/nightwave-v2-phase5b-search-playback\` (source only, Android tests pending).

[Download complete Phase 5B source ZIP](https://github.com/Nahian2002/NightWave/archive/refs/heads/feature/nightwave-v2-phase5b-search-playback.zip) · [Full Phase 5B QA and known limits](docs/NIGHTWAVE_V2_PHASE5B_QA.md)

- Premium Search now shows real native public Songs/Videos result rows when existing YouTube/community providers respond. Tapping a result builds a queue from the displayed collection, allowing Next through selected results. **Open YouTube search** remains a one-tap in-app fallback when providers fail.
- Stream errors now appear with a **Retry** action. Media3 automatically refreshes a failed stream URL **at most once** for selected transient failures, with an explicit user retry available afterward. It never loops indefinitely or preserves signed/expiring streaming links.
- Standard Previous behavior: restart current song if playback is already more than 3 seconds in; otherwise navigate backward. Existing shuffle, repeat, queue selection, seek, background/headset controls and Phase 5A lazy restoration are retained.
- Imported offline tracks display their own metadata instead of the previous YouTube cover and are never treated as YouTube playlist favorites. Local offline-file playback remains private and independent from streaming.
- Added queue-transition unit tests and public-video-scoped playback-failure storage checks. Prior Phase 5A source/safety checks remain available.

**Important:** This work is implemented in the private GitHub source branch, but **Android Gradle, device tests and real-world YouTube playback have not been verified here**. YouTube may restrict streams or change parsing independently of NightWave. Build using \`build-windows.cmd\`; do not uninstall/clear NightWave when upgrading. Report compiler failures through \`dist/NightWave-build-errors.txt\`, or runtime exceptions using the included crash collector. Keep this branch DRAFT until hardware QA passes.

---


## NightWave 2.0 — Phase 5A stability and crash diagnostics

**Version:** \`2.0.0-alpha5a\` (code 17) · **branch:** \`feature/nightwave-v2-phase5a-stability\` · **not Android-verified yet**.

[Download the full Phase 5A source ZIP](https://github.com/Nahian2002/NightWave/archive/refs/heads/feature/nightwave-v2-phase5a-stability.zip) · [Phase 5A QA and crash guidance](docs/NIGHTWAVE_V2_PHASE5A_STABILITY_QA.md)

This phase addresses the user's recurring Android crash warning **without guessing the actual fatal exception**:
- Media3 no longer resolves the last YouTube stream automatically when Android recreates the service; background work and saved-position writes only happen when music actually plays. Saved track/queue metadata remains available immediately, and explicit Play (including supported Bluetooth play commands) restores the stream.
- Corrupt legacy guest preferences, bookmark fields and offline-file indices no longer have to crash startup or background playback. No original library values are automatically deleted.
- Room/DataStore mirroring runs on screen resume or explicit storage checks, not on every playback state update.
- A privacy-limited on-device crash summary stores up to five exception classes and NightWave-only stack frames, retaining Android's normal crash reporting. Open classic Home → **Diagnostics · Share recent crash summary** to share it voluntarily, or use \`capture-nightwave-crash.cmd\` with ADB.
- The previous compact NightWave design, offline audio imports, YouTube search, existing playlists/favorites, backup format and current launcher remain preserved.

**Release warning:** Android Gradle compilation, new device tests and the user's S24 Ultra crash reproduction have **not** been run here. Extract the source ZIP in a fresh folder and run \`build-windows.cmd\`; install the resulting APK **only after BUILD SUCCESS**, without uninstalling or clearing app data. If the build fails, send \`dist/NightWave-build-errors.txt\`. Do not run destructive instrumentation tests on a phone containing important playlists.

This is a draft stability milestone. Google sign-in, cloud recovery and permanent Room-primary writes are still future phases; the root cause of the repeated-crash notification still requires the phone's error trace.

---


## NightWave Alpha4 Hotfix2 — Premium startup repair + offline audio

**Development branch:** \`feature/nightwave-alpha4-ui-offline-repair\` · **Version:** 2.0.0-alpha4-hotfix2 / code 16.

[Download the complete Hotfix2 source ZIP](https://github.com/Nahian2002/NightWave/archive/refs/heads/feature/nightwave-alpha4-ui-offline-repair.zip) · [Detailed QA and limitations](docs/NIGHTWAVE_ALPHA4_HOTFIX2_QA.md)

Responds to phone screenshots/feedback:
- The header now shows **NightWave** in two-tone typography, without the inert Night Mode badge. Smaller Home/Library cards and compact selected navigation tabs reduce clipping.
- Classic Now Playing is collapsed to its core controls and progress, with a collapse control and swipe-down gesture on the artwork; tap the small Now Playing pill to restore it. Expanded player still contains secondary actions.
- Fixes a Compose ARGB rendering error that could cause the premium preview to fail: \`Color(raw.toULong())\` incorrectly interpreted 32-bit ARGB values as packed Compose color values. It now uses \`Color(raw.toInt())\`. The actual cause of the reported crash still needs confirmation with an Android crash log.
- Adds **Offline library** to Home and Library in the classic app and Library in the premium preview. Import personal/licensed MP3, M4A, AAC, FLAC, WAV, OGG, OPUS, MP4, WebM or 3GP files through the Android picker; NightWave copies these files to private storage and plays them through the Media3 music session without Wi-Fi. Import caps: 250 MiB per file, 3 GiB total, 400 tracks. Files are removed by Android if app data is cleared or NightWave is uninstalled.
- **This is not YouTube download functionality.** NightWave still streams public video audio when online; it does not download videos, bypass restrictions, or save temporary signed audio streams.
- The classic Home remains the launcher until the premium screen is physically tested. Existing likes, playlists, guest settings and Room mirrors are preserved.

**Build/QA:** Source uploaded but APK compilation and real-phone playback are **NOT verified**. Download, extract to a new folder, run \`build-windows.cmd\`. Do not uninstall the current app or clear its data. If build fails, send \`dist/NightWave-build-errors.txt\`. If the premium preview still exits, use \`capture-nightwave-crash.cmd\` and share a redacted Android \`FATAL EXCEPTION\` stack.

---


## Alpha 4 Hotfix 1 — startup recovery (Android crash triage)

**This recovery release has NOT been compiled or run on a phone here.** It is based on Alpha 4 and retains all saved music and playback data.

**[Download the full recovery ZIP](https://github.com/Nahian2002/NightWave/archive/refs/heads/fix/alpha4-launcher-recovery.zip)**

After reports that NightWave Alpha 4 installs but immediately closes on Android, this hotfix restores the previously usable classic Home as the default launcher while preserving the new premium Compose interface as an **opt-in preview** through classic Home → **Explore the new premium interface**. The Media3 session's notification also returns to the classic Activity. The Android application ID and private data locations remain unchanged, so **install the debug APK as an update, without uninstalling or clearing data**.

The root cause of the premium screen's runtime crash is **not identified yet**. Build with `build-windows.cmd`, then install only if `BUILD SUCCESS`. To diagnose the remaining premium crash, enable USB debugging, connect the phone, run `capture-nightwave-crash.cmd`, tap the premium preview until it closes, then collect `NightWave-crash.log` and share the part starting with `FATAL EXCEPTION`. Review the log for personal information first.

Hotfix version: **2.0.0-alpha4-hotfix1** (versionCode 15). Stable `main` is unchanged. See [hotfix QA](docs/NIGHTWAVE_ALPHA4_STARTUP_RECOVERY.md).

---


## NightWave Alpha 4 — Premium UI + last-session recovery

**Current development preview:** \`2.0.0-alpha4\` on \`feature/nightwave-v2-phase4-premium-session\`.

[Download full Phase 4 source ZIP](https://github.com/Nahian2002/NightWave/archive/refs/heads/feature/nightwave-v2-phase4-premium-session.zip) · [Phase 4 QA and release limits](docs/NIGHTWAVE_V2_PHASE4_QA.md)

**Major change:** the premium Jetpack Compose Home/Search/Library/Profile is the default launcher in this branch. The classic player is still available via the top-right Settings icon and still handles Android share-to-NightWave links.

The prior song and queue are restored after app reopen, with the last saved playback position and metadata. Media3 resolves a fresh stream and prepares it **paused**; tapping Play explicitly resumes. Neither authentication nor expiring signed audio stream links are saved. A compact mini-player (~60dp) supports swipe down to hide, swipe up to expand, and a tappable "Now playing" control to show it again. Four compact tabs replace the oversized old navigation; album artwork, title layouts and Profile wording were refined for phone screen ratios.

**Build status:** Source uploaded and reviewed, but the full Android Gradle build and real-phone tests for Alpha 4 are **not verified here**. Download/extract to a new folder, run \`build-windows.cmd\`, and only install the debug APK over your existing installation after \`BUILD SUCCESS\`. Never uninstall or clear app data just to upgrade. On compilation errors send \`dist/NightWave-build-errors.txt\`.

Upstream YouTube search is unchanged and may still fail for some queries; the new UI does not promise a search backend fix. Room-primary writing and Google account/Drive recovery remain separate future milestones.

---


## NightWave 2.0 Alpha 3D — guarded single-writer storage

**Branch:** \`feature/nightwave-v2-phase3d-single-writer\` · **Version:** 2.0.0-alpha3d (code 13) · **Draft, NOT Android verified**

[Download the full Phase 3D GitHub source ZIP](https://github.com/Nahian2002/NightWave/archive/refs/heads/feature/nightwave-v2-phase3d-single-writer.zip) · [Detailed Phase 3D QA and limitations](docs/NIGHTWAVE_V2_PHASE3D_QA.md)

This phase consolidates all local favorites, listening-history and playlist mutations into one guarded, synchronous SharedPreferences writer. Each operation validates its source, persists a recovery snapshot and commits the logical library state together rather than publishing playlist names before songs. Manual backup imports share the same lock. The preview's Profile screen offers **UNDO MOST RECENT LIBRARY EDIT** with explicit confirmation and a fingerprint that refuses to recover over newer changes.

**Room primary writing remains disabled** until migration and rollback are actually verified. The current Room copy and DataStore preferences are still secondary; the legacy player and Android media service are preserved. The source includes 10 new JVM mutation tests and seven Android device regression tests, but no Android build or hardware test has run here.

Extract the source ZIP into a fresh folder and run \`build-windows.cmd\`. **Do not uninstall the working app or clear its data** if compilation fails. Room/device tests require a disposable test profile and a separate \`:app:connectedDebugAndroidTest\`; those tests clear test preferences and must NOT be run against personal playlists.

---


## NightWave 2.0 — Phase 3C verified storage preview

**Current development branch:** \`feature/nightwave-v2-phase3c-verified-read-routing\`  
**Version:** \`2.0.0-alpha3c\` · code 12 · **not an APK verified on Android**

[Download the Phase 3C source ZIP](https://github.com/Nahian2002/NightWave/archive/refs/heads/feature/nightwave-v2-phase3c-verified-read-routing.zip) · [Full QA and safety requirements](docs/NIGHTWAVE_V2_PHASE3C_QA.md)

Phase 3C adds an **optional, session-only Room library read preview** in the Compose Profile: choose **TRY VERIFIED ROOM READS** or **RETURN TO ORIGINAL LIBRARY**. The preview only activates if the copied Room database **exactly matches** the current legacy favorites, recent-history order, playlist order, track metadata and source checkpoint. Every unmatched or stale copy returns to legacy reads without exposing Room rows. Song and playlist **writes still go to the original preferences**. No migration deletes original songs, no account sync is claimed, and a vanished legacy source cannot silently clear a populated Room mirror.

New tests: 10 JVM parity tests, six Room reader Android tests, and one unexpected-source-loss Android test (in addition to previous Phase 3B tests). **None have yet been executed in this environment.** Windows build with \`build-windows.cmd\` remains necessary; Android instrumentation requires a test phone/emulator and a separate \`:app:connectedDebugAndroidTest\` run. Keep a manual guest backup outside the app before installing any alpha release.

**Stable main remains NightWave v0.8**, and the development branches have not been merged into main. Google login, cloud backup and the final Room write cutover are not yet implemented.

---


## NightWave 2.0 Phase 3B — Room + DataStore storage preview

**Branch:** \`feature/nightwave-v2-phase3b-room-datastore\` · **Version:** \`2.0.0-alpha3b\` · **Production main:** unchanged.

- [Download Phase 3B source ZIP](https://github.com/Nahian2002/NightWave/archive/refs/heads/feature/nightwave-v2-phase3b-room-datastore.zip)
- [Storage and test instructions](docs/NIGHTWAVE_V2_PHASE3B_STORAGE_QA.md)

The preview now builds an **isolated Room library copy** and a **DataStore guest-profile copy** when you enter the new interface. It does not delete old preferences or change the Media3 playback owner. Reopening the preview rechecks for changed playlists and guest settings. Profile → Storage upgrade preview shows its status and offers a manual recheck.

Room stores public video metadata, ordered playlists, favorites and locally held recent history, with its migration checkpoint committed in the same SQLite transaction. DataStore receives guest settings through a non-destructive, atomic edit. This is deliberately a **shadow migration**, not the final cutover to Room/DataStore as the canonical writer.

The new branch includes eight JVM snapshot tests, five Android Room instrumented tests and one Android DataStore instrumented test. **None have been executed here**, and the Android/KSP build has not been verified. Build on Windows with \`build-windows.cmd\` before installing; do not uninstall or clear the old app. The separate instrumented tests require a connected phone or emulator and \`:app:connectedDebugAndroidTest\`.

**Not provided yet:** Google sign-in, cloud restore, full dual-write/cutover, and a proven Android APK for this phase.

---


> **NightWave 2.0 Phase 2 — opt-in developer preview**: A new native Compose music player is available on this branch. It adds a Media3-connected mini-player, full-screen artwork player, track queue, shuffle/repeat, usable playlists and on-screen actions. YouTube website song searches now return to the Compose player. The legacy launch screen and playback service remain intact. **This is NOT a verified Android build or stable release yet.**
>
> **Download Phase 2:** [Source ZIP](https://github.com/Nahian2002/NightWave/archive/refs/heads/feature/nightwave-v2-phase2-player-ui.zip) · [Phase 2 QA guide](docs/NIGHTWAVE_V2_PHASE2_QA.md) · [Overall phased plan](docs/NIGHTWAVE_2_PHASES.md)
>
> Extract ZIP to a fresh folder and run \`build-windows.cmd\` without GitHub Actions billing. If the Windows Gradle tests and build succeed, install the new debug APK **over**, not instead of, your previous app. Then open **Home → Explore the new premium interface**. If it fails compilation, keep the old app installed and share the first Kotlin compiler error. Version: **2.0.0-alpha2** / code **9**.


### A personal Android music player · v0.8.0

NightWave is an experimental, independent Android music app. Explore public YouTube videos by song or artist name, maintain a local music library, and listen using Android's Media3 background player. **NightWave is not Spotify or YouTube, and it is not affiliated with either company.**

**Release status:** v0.8 source is uploaded. The new YouTube website search and video-selection workflow still need to be compiled and tested on an Android device; do **not** treat this as a confirmed fix until those tests pass. A previous version's direct-link playback passed on the owner's phone. See [TEST_REPORT.md](TEST_REPORT.md).

## v0.8 — Direct YouTube search without failing public API servers

**Why this changed:** v0.6 native search yielded \`raw=0, streams=0, errors=4\`, and the community Invidious search provider in v0.7 also failed on the owner's phone. More retries against unavailable endpoints won't produce music.

NightWave's **main Search button now opens YouTube's real search-result website in an Android WebView**, using only the song or artist name. Tap a public video to return its link to NightWave's existing media player. The app intercepts standard video URL navigation and YouTube's JavaScript-driven thumbnail clicks; **▶ PLAY** also lets you select the currently opened YouTube video manually if automatic interception fails. An **↗ BROWSER** fallback opens the same query in your browser/YouTube app; choose a video there and use **Share → NightWave** if necessary.

**Native song lists are now clearly labeled experimental** rather than being presented as a working primary search engine. The six Home discovery categories open direct YouTube web searches. The optional API-based native list may still fail because upstream services are unavailable.

The in-app YouTube browser does not expose a privileged JavaScript interface to websites, restricts navigation to known HTTPS YouTube hosts, disables WebView file access and blocks other-site navigation inside the app. Avoid signing into Google within the embedded search browser; use the external-browser button if an account is required.

**Limitations:** YouTube may block pages in embedded WebViews; this has not yet been device-tested. The website browser can show results without an API key, but selecting a video does **not** guarantee playback—NewPipe stream resolution is still needed. Private, restricted, protected, and otherwise unsupported videos may fail. Official YouTube APIs prohibit background playing through their player, and unofficial extraction remains subject to platform changes.

## Features

- **Discover:** song-name search through YouTube's own website, plus six tappable genres and moods (Bangla, Bollywood, Chill & Lofi, Night Drive, Workout, Acoustic).
- **Experimental native results:** optional NewPipe/Invidious-based lists; these may fail and are no longer the default. Website search uses real YouTube results.
- **Library:** Liked Songs, local playlists (create/rename/delete), recent listening history, saved playback queue, and adding a public YouTube link to a playlist.
- **Player:** audio-focused streaming, lock-screen/notification media controls, Bluetooth play/pause/next/previous, seek, shuffle/repeat, immersive Now Playing, and automatic next-song playback from the background service.
- **Visual identity:** Midnight Sessions — navy, violet, aqua, layered cover art and native lightweight motion.
- No root, YouTube/Spotify login, or payment required for this locally built personal prototype.

### Limitations

A result appearing in search **does not guarantee it will play**. Age gates, region restrictions, private/paid videos, DRM, extractor failures, platform changes and network problems can prevent streaming. NightWave does not have Spotify's licensed catalog, algorithms, account sync or offline downloads. The current queue can restore its list after a service restart, but cannot promise seamless resumption after a force-stop or device reboot.

YouTube's API policies prohibit separating audio from video or background playback through the official API. This unofficial approach can conflict with platform terms and may break when YouTube changes its services. Do not redistribute without appropriate policy and license review.

## Download and build for free on Windows

You **do not** need GitHub billing or a payment card. Builds run on your own PC.

1. While signed into the private GitHub repo, select **Code → Download ZIP**, or [download the current main-branch ZIP](https://github.com/Nahian2002/NightWave/archive/refs/heads/main.zip).
2. Extract into a **new folder** rather than overwriting an old extracted version.
3. Double-click **build-windows.cmd**. The script uses cached JDK/Gradle/Android SDK downloads when possible; the first run can take several minutes and several GB of space. Accept Google SDK licenses only if you agree.
4. It runs Android unit tests and assembles a debug APK. Wait for **BUILD SUCCESS**.
5. Copy **dist\NightWave-debug.apk** to your Android device and install it **over the old debug APK**. Do not uninstall or clear the current app if you have saved playlists and favorites.

**If the build fails:** keep the existing phone app and send the first Gradle \`e:\` Kotlin error or \`FAILURE:\` section. The new version has not been verified until that build succeeds.

## Test checklist on your phone

1. Search \`Baby shark\`, \`saiyara\`, or \`Adele Hello\`. The main search arrow should open YouTube's search website **inside NightWave** (not a blank native result page).
2. Confirm YouTube displays matching video thumbnails; tap a video. If it opens in the embedded browser, press **▶ PLAY**. Then lock the screen to check playback.
3. Use **Home** mood cards; create and rename a playlist; like a song; add one via a full YouTube URL.
4. Queue multiple tracks and verify next/previous, shuffle/repeat and Bluetooth media buttons.
5. Visit **Now Playing** and test artwork, seeking, and the expanded controls.
6. If the embedded website does not load, press **↗ BROWSER**, select a YouTube video, and choose **Share → NightWave**. Report what the embedded browser shows. Optional filtered log: \`adb logcat -d -s NightWaveSearch:D\`.

### Search privacy

Direct search uses the NewPipe Extractor. If it fails, the app may send the **query text** to one of several public [Invidious instances](https://docs.invidious.io/instances/), which are independent servers not controlled by NightWave. Avoid submitting personal or sensitive search strings. NightWave does not send Google login credentials or signed playback URLs to those search instances. Instance availability and privacy practices may vary.

The optional native search fallback uses the Invidious [public search endpoint](https://docs.invidious.io/api/) and locally cached results for up to 10 minutes during the app process lifetime.

## Development notes

- Kotlin, Android Gradle Plugin, Media3, NewPipeExtractor, OkHttp, Android local preferences.
- \`app/src/main/java/com/nightwave/player/net/YouTubeGateway.kt\`: main search, ranking, stream resolution and caching.
- \`app/src/main/java/com/nightwave/player/net/CommunityVideoSearch.kt\`: independent search and strict JSON parsing.
- \`app/src/main/java/com/nightwave/player/ui/MainActivity.kt\`: home, search, library, animations, and player controller UI.
- \`app/src/main/java/com/nightwave/player/playback/PlaybackService.kt\`: background MediaSessionService and automatic queue advancement.
- \`app/src/main/java/com/nightwave/player/library/MusicLibrary.kt\`: local favorites, history and playlists.
- \`app/src/test\`: keyword routing, queue tests, search parser and fake-server failover tests.
- \`tools/build-windows.ps1\`: local no-billing Windows build bootstrap.

The current GitHub-hosted Actions runner has an account billing/spending restriction. The local builder avoids those hosted runners. No remote APK build is claimed.

## License and upstream acknowledgement

See [LICENSE](LICENSE) and [NOTICE](NOTICE). This project incorporates NewPipeExtractor, GPL-3.0-or-later. Respect its copyright/license obligations, particularly if distributing binaries. This is an independent, privately developed experimental client and does not claim affiliation with YouTube, Google, NewPipe or Spotify.
