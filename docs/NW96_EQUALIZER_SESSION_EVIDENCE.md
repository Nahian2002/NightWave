# NW96 — Session-Safe Equalizer Intent Restoration

**26 October 2026**, cumulative NW95 parent `86764d95c4c4bb7394051195ef7f02ab9dbfe998`. Branch `feature/nightwave-nw96-equalizer-session-restoration`.

## Confirmed issues

1. The service kept the user's requested EQ preset and manual gain map **only in process memory**. Service recreation started with `requestedEqPreset=0` even if the user had selected a preset earlier.
2. On a new Android audio-effect session, a manual EQ curve was validated by **band count and index only**. Hardware can have a different frequency center arrangement with the same band count; blindly replaying boosts risks modifying the wrong frequencies.
3. Single-band legacy edits could seed from the previous session's manual index map even when the frequency layout had changed.

## Fix

- Add `EqualizerSelectionStore` in an **additive, separate** `nightwave_eq_selection_v1` SharedPreferences namespace. It synchronously saves **only explicitly chosen preset intent** or a complete manual curve (frequency centers + gain in millibels); the receipt never claims an effect is active.
- Restore validated intent on service creation. Android's live audio-session callback still controls when actual EQ can be instantiated; status initially shows waiting/not active.
- Require manual session frequency centers to **match exactly**, not merely have the same number of bands, before applying any stored manual gain values. Mismatch retains the saved user choice but leaves effect inactive and requests a Sound Studio refresh.
- A single-band edit now only reuses the prior manual map if the session's center frequencies match; otherwise uses a compatible device readback baseline or rejects the edit.
- Commit user EQ intent before replacing a working effect; failed preference writes leave the current selection/effect unchanged.
- Reuse existing Android Equalizer and Media3 audio-session ownership and crossfade handoff. No second effect host or new output routing.

## Tests and certification

- Pure Kotlin policy smoke: **15 synthetic cases** covering same/different center arrays, swapped bands, malformed curves, oversize counts, safe presets and bounds; compiler execution when available.
- Six JUnit regression tests added for Android Gradle execution; not a substitute for real Android Effect API/device readback.
- Source-level integration checker with real production wiring; CI branch and canary triggers updated.
- Full Android compilation/JUnit, signed APK installation, EQ output-route listening test, Bluetooth route verification and release acceptance remain **NOT VERIFIED**, especially if runner jobs again fail before any steps.
- The app still cannot guarantee actual audible EQ effect on a Bluetooth device purely from framework-level acknowledgements.

## Data and release preservation

No changes to existing `nightwave_equalizer_status` or `nightwave_eq_capabilities` formats, music files, library database, queue snapshots, OAuth, signing identity, app package, manifest, YouTube sources, billing or serverless model. Previously in-memory-only EQ choices from older app processes cannot be recovered without a valid new intent record.

**Public release: NO-GO** until full build/signing, device acceptance, source rights and Android Play target API checks pass.

For local owner checks download the fresh NW96 ZIP, use the included `run-nw91-final.cmd` cumulative Windows builder, and `check-device-update.cmd` before installing. Do not uninstall or clear application data.
