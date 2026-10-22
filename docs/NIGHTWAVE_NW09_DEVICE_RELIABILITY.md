# NightWave NW-09 — Device Reliability Recovery

**Date:** 22 October 2026  
**Base commit:** NW-08 `58666075096cb960a5bb520b60d15fdbae8c85e9`  
**Branch:** `fix/nightwave-v2-nw09-device-reliability-diagnostics`  
**Release:** **NO-GO**, pending a real Android build, signer-safe installation, and Samsung device tests.

## Device reports still driving priorities

- Public-video Shuffle was delayed or inconsistent on the previously installed build. NW-01 introduced explicit state and service receipts; **real-device acceptance is pending**.
- Equalizer was basic or had no audible effect / unavailable status. NW-02 replaced UI and NW-05 bounded gains; **actual DSP routing on speaker/Bluetooth remains unverified**.
- Google Drive showed no account/consent dialog or returned errors. NW-03 improved onboarding; **real consent and OAuth app registration remain unverified**.
- Previous screenshots showed poor Now Playing seek thumb, blurry cover images and compactness problems. NW-04 made source changes; **screenshot and accessibility acceptance pending**.

## NW-09 changes made

### Google Play services and consent lifecycle

1. Before calling `AuthorizationClient.authorize`, check `GoogleApiAvailability.isGooglePlayServicesAvailable`. Show a bounded numeric status and clear guidance when Play services are missing/outdated. This is **not** full Google Identity sign-in.
2. The pending action (`connect`, `backup`, `preview`, `merge`) is saved to the Activity's `onSaveInstanceState` **only while an external Google permission sheet is open**. Restored state includes only this action flag, never an access token, email or backup content.
3. When a result returns, require an expected pending action before processing it. A stale/unknown result cannot start a Drive operation.
4. A Google Play services `Task` that never returns is considered stuck after a bounded 20-second request timeout. The timeout **does not run while the user is viewing a Google account picker or consent sheet**.
5. Superseded Tasks cannot overwrite the status of a later request or a destroyed Activity.
6. Three pure unit tests cover allowed pending operations, stale generation tokens and the bounded request timeout.

**What was not implemented:** A genuine Google ID-token identity session, a custom OAuth project, a backend or automatic multi-device library synchronization. Google permission may legitimately complete without a dialog if access was previously granted. <https://developers.google.com/android/reference/com/google/android/gms/auth/api/identity/AuthorizationClient>

### Equalizer discovery and UI truthfulness

1. `ACTION_EQ_DISCOVER` now updates the current service receipt for the active audio session. It will not leave a previous process's `active = true` value on screen when no current effect exists.
2. No audio session, no supported effect and inactive preset are reported distinctly.
3. Sound Studio now briefly rechecks Android's reported capabilities after `startService`, allowing an asynchronously starting playback service to report its audio session.
4. A manual preset command waits longer for a matching service receipt; pending UI is bounded and recovers when no response arrives.
5. A missing current audio session takes precedence over a stale `EFFECT ACTIVE` label.

**No audible EQ success is claimed.** Platform effects can be unsupported, overridden by other apps or bypassed on particular Bluetooth output routes.

## Source review and testing evidence

- Updated branch and files were read back from GitHub; 18 targeted marker/import/brace contracts passed.
- Added three JUnit tests for the recovery policy, but their **Gradle execution is not confirmed**.
- Android CI still requires all earlier NW-01 through NW-08 regression source gates plus `:app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin`.
- Prior GitHub Actions attempts failed before running job steps. These do **not** prove the Kotlin source either failed or passed.
- No compiled NW-09 APK, sound recording, device crash telemetry, or completed Google permission result is available.

## Deferred phone acceptance

After a complete successful Windows build (`build-windows.cmd`) and read-only signer comparison:

1. On the Samsung phone, leave NightWave guest data intact and open **You → Account & backup → Connect Google Drive**.
2. If Play services is unavailable, verify it shows a status instead of a silent failure. If Google consent opens, rotate the phone and return from the sheet. The original connect/backup/restore intent must still match the result.
3. If Google Play services is intentionally blocked by network/device settings, a stuck initial request should recover from busy state; no partial backup or profile replacement may occur.
4. Use a test Drive account before trying backup, preview, merge or disconnect. No direct restore overwrites personal songs without explicit confirmation.
5. Play a local file, open **You → Sound Studio**, select presets and manual gains, test speaker and Bluetooth separately. Stop playback and re-open: it must say the session is unavailable, not falsely `EFFECT ACTIVE`.
6. Retest quick public Shuffle and song switch during scrub, long background playback, mini-player layout, lyrics and public-video search.
7. Compare APK signer with installed app. **Do not uninstall or clear NightWave data to bypass a mismatch.**

## Recommended next work

NW-10 should depend on **real compiler/device evidence**, not another unverified feature list. If Android CI still fails without any executed job steps, use the local Windows build and share only privacy-redacted status/full/error logs. A green source check is a necessary guard, but **not a production-readiness certificate**.
