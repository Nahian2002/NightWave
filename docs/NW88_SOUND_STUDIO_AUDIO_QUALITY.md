# NW88 — Sound Studio & Audio Quality

**Original roadmap:** [NW76–NW90](CURRENT_CUMULATIVE_ROADMAP.md) · Wave 3, NW88  
**Cumulative parent:** `feature/nightwave-nw87-transfer-offline-ux`  
**Branch:** `feature/nightwave-nw88-sound-studio-audio-quality`  
**Status:** Code and regression sources committed. Targeted exact-hash Kotlin EQ-readback compilation/smoke **PASS**. Full clean Android/Gradle compilation, JVM suite, Android device verification, real headphone/speaker acoustics and NW90 release QA remain **pending**.

## Changes delivered

### Real EQ acknowledgement rather than a simulated curve

- Existing Sound Studio obtains an `android.media.audiofx.Equalizer` bound to the **active Media3 audio session**. Existing session ownership, Android-supported band center frequencies and preset data remain authoritative.
- New pure `EqualizerReadbackPolicy` checks the values actually returned by `getBandLevel` after applying the requested per-band levels, not only whether `setBandLevel` completed.
- Classifies exact matching values as **Verified**, device-quantized readings up to 100 millibels (1 dB) as **Device rounded**, a larger mismatch as **Mismatch**, and invalid or inconsistent response data as **Invalid**.
- A large mismatch/invalid band receipt **does not activate the requested effect**, and releases the candidate effect while allowing playback to continue without a false green indication. Presets remain read from Android itself. Cached capability status is refreshed from the same live session.
- Sound Studio now refreshes the manual draft from Android-reported gains on a successful Apply, avoiding a misleading `Not applied` label when the device has rounded the requested value.
- The visible badge says **Android session EQ**, not that EQ is guaranteed to affect a Bluetooth receiver or offloaded/system output path. No audio analysis, microphone loopback, external DSP provider or subjective audible verification is claimed.

### Local transitions and loudness settings surfaced together

- Sound Studio now controls **Off, 2, 4, 6, 8, 10 second crossfade** using the existing service action `ACTION_CROSSFADE_SET`. The screen polls the actual locally stored setting before confirming success.
- Adds an **Offline volume balancing** toggle using existing service action `ACTION_NORMALIZATION_SET`; the settings readback is verified before claiming success.
- EQ commands and transition-setting commands are serialized while an update is pending.
- Existing two-deck Media3 crossfade remains limited to **verified app-private offline audio** and is disabled under Shuffle, Repeat One and playback speeds other than 1×; if the second decoder is unavailable, normal gapless playback remains the fallback where supported. Crossfade remains opt-in and OFF by default.
- Existing normalization is **bounded RMS-based attenuation** on a sample of up to 12 seconds from verified local files, never gain amplification, not ReplayGain, BS.1770 integrated loudness, streaming normalization or guaranteed acoustic uniformity. The UI now explains this clearly.
- Existing Media3 gapless mode, saved preferences, automatic audio-focus handling, EQ session rebind and output-device compatibility notes are preserved. No added online server or music-rights expansion.

## Regression and evidence

- Added `EqualizerReadbackPolicyTest.kt`: five JVM unit groups covering exact gains, quantization, rejected large mismatch, invalid/stale band data, and honest device-route wording.
- Added `tools/NW88AudioSmoke.kt`: a standalone pure-Kotlin suite using the original `EqualizerReadbackPolicy`, `CrossfadePolicy`, `OfflineNormalizationPolicy` and `AudioTransitionUiPolicy`; run instructions are in the source. It has been **committed but not executed as the complete multi-policy smoke** during this phase.
- Added `tools/verify_nw88_sound_studio.py`: source-only contract checking existing Media3 crossfade/repeat/gapless controls, effect readback, route honesty, verified app-private normalization, and real UI-to-service integration.
- **Executed now:** The newly committed `EqualizerReadbackPolicy.kt` was reproduced locally; `git hash-object` SHA `257ccd40be19e4a0519ce423da7e8f4821f61587` exactly matched the live GitHub blob. `kotlinc` compiled the production policy plus a targeted five-check Kotlin runner; Java printed `NW88_EQ_READBACK_KOTLIN_SMOKE_PASS`.
- GitHub source readback verified the integration markers, five new regression test groups, and structurally balanced Kotlin braces. **This is not a whole-project compiler pass.** Physical EQ effect strength and route behavior cannot be proven by static tests.
- No changes to downloading, source eligibility, saved audio indexing, Google Drive/OAuth, cloud backup data, Android manifest, Gradle configuration, audio asset files, or public Search metadata.

## NW90 acceptance

Build the current cumulative branch clean, execute JVM/Compose/Android tests and test live playback using the owner's S24 Ultra with built-in speaker, USB audio (if available) and Bluetooth headphones. Explicitly test EQ Off/stock presets/manual curves/device rounding, session re-creation on Next/background/route reconnect, audible frequency response under real device routing, 0/2/4/6/8/10s crossfade and fallback, sample accuracy with differing tracks, Shuffle/Repeat/Speed interactions, pause and seeks during overlap, gapless correctness, volume focus, and offline normalization for quiet/hot/invalid files. Capture any route that ignores Android EQ as unsupported rather than claiming 100% effect compatibility.

**Public release is still NO-GO:** NW77 media-source rights review and NW90 full quality certification have not cleared.
