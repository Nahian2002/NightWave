# Current verification — NW120 Audio/EQ

27 October 2026. Branch: `update/nightwave-nw120-audio-session-truth`. Parent: verified NW119 `ccbaae441fecea1bc3ca5b018fde33bb17ced554`.

## Executed locally

- Complete one-click Windows builder: **BUILD SUCCESS**.
- Android production Kotlin/Compose and instrumentation-source compilation: **PASS**. No device instrumentation was executed.
- JVM regression suite: **1,433 tests in 179 suites; zero failures, errors or skips**. Twelve new policy test methods cover reviewed-device identity, preset readback and crossfade EQ sessions.
- Debug APK assembly and package/version/signature validation: **PASS**.
- Certificate comparison against the separately saved NW119 APK: **MATCH**. Installed-phone signer continuity is still untested.
- Current structural/negative and Windows source preflights: **49/49 PASS**.
- Android lint: **PASS**, zero errors, 313 warnings and 17 hints. See [NW120 evidence](docs/NW120_AUDIO_SESSION_EVIDENCE.md). Remaining maintenance warnings are retained; no lint baseline or new broad suppression was introduced.
- Source attestation: **785 current build-affecting files match the compiled snapshot**.

## What the tests establish

The new tests execute actual pure Kotlin policies for session/frequency/range/preset identity, invalid/unbound commands, same-count frequency swaps, Unicode/delimiter distinctions, preset mismatch/bounds and shared-session crossfade eligibility. Existing audio ownership, saved EQ intent, gapless, normalization, queue and route-command tests remain green. Android-specific callback wiring and live playback are compiled, not hardware-tested.

Sound Studio now requires a fresh discovery acknowledgement before presenting cached status as current. Its visible-screen poll runs every two seconds plus reply latency; a framework status update is not an acoustic measurement. Android can still bypass effects on an output route.

## Pending acceptance

The [NW120 consolidated device checklist](docs/NW120_AUDIO_SESSION_EVIDENCE.md) covers speaker/headphone/Bluetooth audible A/B, output switches, external effect control, stale/delayed Apply, EQ during crossfade, background/resume and safe EQ Off. Physical Samsung validation, installed-phone signer, production OAuth/Drive restoration and rights/privacy approval remain open. Public release is **NO-GO**.

Historical NW119 results remain in [its evidence](docs/NW119_CLEANUP_AND_LYRICS_EVIDENCE.md); [v0.8 results](docs/TEST_REPORT_V08_ARCHIVE.md) and [historical CI diagnostics](docs/HISTORICAL_CI_SOURCE_CHECKS_20261027.md) are preserved. They do not substitute for this current run. Next ordered feature wave: **NW121 Drive/guest backup**.
