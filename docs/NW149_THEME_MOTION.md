# NW149 — theme and motion consistency

4 November2026. Source **LOCAL_VERIFIED**, code45 phone tests **NOT_RUN**. Cumulative branch update/nightwave-nw149-theme-motion, based on verified draft174. [Committed plan](NW149_THEME_MOTION_PLAN.md), [exact source receipt](quality/NW149_SOURCE_EVIDENCE.json), [36 theme/surface review slots](quality/NW149_THEME_SCREEN_REVIEW.csv).

## Changes and findings

The queue uses Material's default SnackbarHost for Undo. NightWave had supplied inverseSurface/inverseOnSurface but omitted inversePrimary, leaving the action in the library's default palette. It now explicitly uses the selected palette's accentSoft against inverseSurface. All four theme identities/base palettes remain intact. This is a theme consistency correction; no fabricated old contrast failure is claimed.

The saved System motion preference previously passed only `profile.motion == REDUCED` through several callers. Effective optional motion now also resolves Android's read-only animator scale for System mode. Zero/negative/nonfinite/unreadable scales reduce optional motion safely; valid positive scales leave scaling to the existing animation framework. Stored Reduced always reduces, and legacy Full retains its explicit non-following intent. Main, offline and updater entry points carry these same preference meanings without new stored fields. Navigation/haptics, mini/full player, queue, lyrics and shared components use the resolved local. Functional progress feedback is retained; no blanket spinner/frame-pacing certification.

A ContentObserver subscribes only while LocalLifecycleOwner is STARTED, re-reads on return, and unregisters on stop/disposal. Late callbacks cannot publish after stop; duplicate starts/stops, read/registration/cleanup failure and repeated foreground cycles have pure regression coverage. No settings write, polling, permission, service command or user-profile mutation was added by the observer. All durations, pager recovery, keyboard/focus cleanup and feature reachability stay unchanged; no timing/blur/layering patch was guessed from the Samsung capture anomaly.

Research: Android documents the [animator duration scale](https://developer.android.com/reference/android/provider/Settings.Global#ANIMATOR_DURATION_SCALE), the [lifecycle composition local](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.8.0), and the [Snackbar action role](https://developer.android.com/reference/kotlin/androidx/compose/material3/SnackbarDefaults). Existing pinned dependencies are unchanged.

## Measured host checks and limits

- Full Windows builder PASS; **1,841 JVM tests /230 suites**, zero failures/errors/skips. Eight new methods test real configured contrast/state roles, system/override behavior, subscription ownership, late callbacks, foreground return and inaccessible settings.
- **91 current source checks PASS_SOURCE_ONLY**, including lifecycle/entry-point cleanup/privacy negative fixtures. Retained older motion-provider checks now inspect the actual resolved local rather than the former raw flag.
- **56 opaque configured text/action/state pairs**, minimum **5.086631:1**, all above4.5:1; [measurements](quality/NW149_CONTRAST_MEASUREMENTS.json) exported from JVM results. This does not certify every translucent/artwork/disabled-state pixel. Disabled controls are retained, not scored as normal text.
- Two Android methods × four palettes = **8 cases COMPILED_NOT_RUN**: actual Material default Undo palette/action and all four production navigation actions/selected state under reduced motion. Synthetic queue callbacks only; no instrumentation APK installed.
- Sequential lint PASS: **0 errors /327 warnings /17 hints**, unchanged ID/severity/message/path inventory from44, no suppression.
- All-ref Gitleaks **3,640 commits/no leaks** at final implementation before result documentation. Eight native entries/one other asset byte-identical to44, APK ZIP entry inventory unchanged. Original219 pack **PREPARED_NOT_EXECUTED**.

Build **NW-debug-356960B7755F6DC0**, revision **3cc1e8a4c75fc1656ecca09ca4bc61e801919f23**, canonical source **356960B7755F6DC04545E2597BE263AD831AC941F816FBE5F5181500524F40D8**,998 inputs. APK **5F63200CEDBC4ECC755B971101246227DABF0CDB85F5FCEA51251E1979063B51**,28,362,773 bytes. Package com.nightwave.player, name2.0.0-alpha5s/code45,min23/target36. Signer **EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394** matches actual freshly pulled Poco44. Owner-only Drive metadata plus complete28,362,773-byte hash readback PASS. Raw APKs/device data/logs/source bundles stay private outside Git. Interim build passed but preceded final offline/updater wiring; final build/source checks were repeated and are the authoritative identity above.

## Actual device evidence and next step

Owner manually opened Sound Studio on existing code44/Poco X3 M2007J20CI,Android12/API31,1080x2400,density440,font0.81. Current saved themeAMOLED/motionSYSTEM and system animator scale1.0 read back; no preference was edited by this work. Sound screenshot and fresh UI hierarchy74 nodes succeeded in5.67s. This does not reproduce the Samsung hierarchy failure or establish its cause; no crash/accessibility diagnosis. Raw frame stats are cumulative/uncontrolled and do not justify duration tuning or a smoothness score. Prior Samsung blank-player capture root cause stays open.

**Code45 installation needs fresh explicit approval** under AGENTS.md; approval44 is not blanket. MIUI remote taps remain blocked, so targeted phone checks require owner navigation. The36 theme/surface slots remain NOT_RUN for45. Test all themes and reduced motion with original AMOLED/System restored only under approved scope; no EQ/account/library/system-setting changes. Full NW145 remains PARTIAL and original gates unpromoted; no9.5/100% rating from host checks. Next independent source batch **NW150**; physical acceptance remains separate. No feed/binary activation, main merge, settings change or automation restart.

[Draft review175](https://github.com/Nahian2002/NightWave/pull/175) is attached and unmerged.
