# NW123 — Accessibility and typography (source evidence)

Date: 2026-10-27
Parent: NW122 `3c61b31e68839f207ca6b6b8f2d0f4374b15c0e4`
Branch: `update/nightwave-nw123-accessibility-typography`
Status: **SOURCE_IMPLEMENTED_UNVERIFIED; RELEASE NO-GO**

## Protected boundaries
- No changes to `com.nightwave.player`, Gradle, signing, Google OAuth/Drive, media URLs, playback queue/service, offline database, downloads, library persistence, network or permissions.
- Existing action callbacks remain connected; no music-source rights or offline eligibility changes.
- Never clear app data or uninstall to test an accessibility improvement.

## Implementation
- Navigation destinations expose full TalkBack names and selected Tab state, including 320dp with 300% font size. Navigation height grows from 84dp to 112dp at 2.4–3x scale.
- Queue entries preserve full Unicode English/Bangla titles/artists as accessible announcements. The row remains a button; Remove remains independently accessible.
- The seek timeline hit area grows from 32dp to 48dp while keeping the slim drawn line, fractional accessibility progress and track-identity safety checks. Audio seek callbacks are untouched.
- Feature, mix and mood cards grow for large fonts rather than clipping their content. Recent-song titles and artists and library-collection metadata can use two lines. Active track rows advertise Playing state.
- All four theme palettes have >=4.5:1 computed relative text contrast for foreground/background, secondary/surface and accent/surface token pairs. This is not a blanket claim about image overlays.

## Test contracts
- Seven new pure JVM JUnit tests: tab announcement, Bangla marks, malformed queue fields, card heights, malformed font scales, secondary text line limits, four-palette contrast.
- Existing adaptive sizing JVM test extended for 2.4x and 3x scales.
- One new Compose Android instrumentation test asserts the four labels/selected state at a 320dp container and 3x font scale.
- **Not yet run on this exact commit:** full Gradle/JUnit, Android instrumentation execution, Android Kotlin/Compose compilation, signed APK build, physical device layout, TalkBack/touch exploration/switch access, Arabic RTL fallback, screen-reader navigation, reduced-motion verification, 30-minute playback soak.
- Prior NW122 measured 1,464 JVM passes, 50 source checks and APK signer evidence are a baseline, not NW123 validation. GitHub Actions is presently blocked by repository account billing limits.

## Consolidated device acceptance at NW125
Use 320/360/412dp equivalent widths and 1.0/1.3/1.8/2.0/3.0 font scales. Read all tabs and queue rows in TalkBack, confirm Remove stays separate, test drag/tap/accessibility SetProgress against the right track, inspect clipped cards/labels, test Midnight/Aurora/Pearl/AMOLED text contrast and motion preferences. Also verify app upgrade retains signed package identity and all saved user data.

Next ordered source wave: **NW124 — useful optional extras**. NW125 remains final certification. Public release NO-GO until real acceptance, permissions and rights gates are satisfied.
