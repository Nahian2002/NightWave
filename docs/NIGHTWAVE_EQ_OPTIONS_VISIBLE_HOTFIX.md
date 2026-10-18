# Equalizer option-visibility hotfix (Phase 6W8)

## Root cause
The native Android AlertDialog combined Builder.setMessage() and Builder.setItems(). Android gives the message view precedence over the items list, hiding *Off*, device presets and *Manual* even when EqualizerCapabilities had a valid gain range.

## Change
- Replaced the message/title pair with a custom title view containing the heading and last service status / gain-range detail.
- Kept Builder.setItems() as the real selectable action list, plus Cancel.
- Existing dynamic presets, Android band capabilities, manual EQ sliders, atomic application, and service receipts are unchanged.
- No NightWave backend, data migration, app ID/version/signing changes or audio engine replacement.

## Source checks (read-back from GitHub)
- The EQ callback contains a custom header and setItems.
- No competing setMessage in this callback.
- Off, discovered preset names and Manual band action are present.
- Manual band action still dispatches ACTION_EQ_BANDS_APPLY.
- All playback service and capability files are unchanged from the parent branch.

## Real-device acceptance (not yet verified)
1. Run build-windows.cmd; require BUILD SUCCESS and an APK from that build.
2. Install over the current version only if signing matches. Never uninstall or clear app data to fix signing errors.
3. Start a local or public song. You > Appearance & playback > Audio equalizer.
4. Confirm Off and actual device preset choices are visible under the status; when supported, Manual N bands is visible.
5. Open Manual; check labeled Hz bands, gain sliders and Apply. Change a few sliders and confirm an EQ receipt appears.
6. Switch Bluetooth route, reopen EQ, change presets, and confirm playback/queue continue normally.
7. Stop playback; when no session exists, Off remains available and UI explains that playback is needed.

**Limitations:** Gradle and on-device verification for this exact hotfix have not been performed.
