# NW120 — Audio/EQ session truth and crossfade continuity

Date: 27 October 2026. Parent: `ccbaae441fecea1bc3ca5b018fde33bb17ced554` (verified NW119). Branch: `update/nightwave-nw120-audio-session-truth`.

## Confirmed code defects

- Sound Studio compared session ID and band count when deciding whether to reset a draft. A device/output can change its ordered frequency centers, range or preset table while retaining the same count, so old gains could be presented against different bands.
- Apply intents carried gains or a preset index, with no identity for the output the user reviewed. A delayed intent could be interpreted using a newer capability snapshot.
- An enabled effect was sometimes sufficient to report active state; preset selection was assumed successful without checking `currentPreset`. Existing reuse/discovery paths did not revalidate manual readback.
- Android effect control/enabled changes had no listener to invalidate a stale active receipt. Sound Studio stopped reading after initial discovery, and cached preferences could be shown before a fresh service reply.
- The crossfade secondary player used a separate session; requested EQ was not attached until handoff, leaving the incoming overlap outside the selected effect.

## Implementation

`EqualizerCommandPolicy` hashes the session ID, ordered centers, actual range and length-prefixed device preset names. It rejects missing/invalid metadata and makes delimiter/Unicode distinctions explicit. The UI sends this identity with commands; the service refreshes real capabilities before accepting non-Off mutations. Stale/unbound commands reject without saving intent or modifying the existing effect. No external or current in-app producer of old unbound gain commands was found; legacy unbound service actions now reject safely. Off is still allowed without a live snapshot.

The capability store captures and validates every field before marking a snapshot ready, clears stale readiness on read/query/ownership/commit failures and returns success to the service. No new persistent library or settings schema is introduced; the command identity is calculated from ephemeral capability metadata.

The playback service checks preset index plus actual band bounds, and retains manual exact/quantized readback rules. Current effect reuse/discovery, command rejection status and Android control/enabled callbacks use owned-session readback. Released-object callbacks are ignored; listeners detach before release. Live status retains the last command acknowledgement so an asynchronous callback cannot turn a successful reply into a UI timeout. Externally disabled effects are not automatically re-enabled.

Sound Studio requires the exact discovery request acknowledgement before displaying current status. Old cached active text is hidden while unconfirmed. Polling stops outside the visible lifecycle, defers to explicit Apply and preserves unsaved gains when the device identity is unchanged. A same-count frequency/range/preset-table change resets the draft and tells the user to review it.

Crossfade preparation requests the primary session for the incoming deck when EQ is selected. A ready/playing deck with a mismatched session falls back to normal gapless; an uninitialized decoder is allowed to finish preparing. Explicit successful EQ changes cancel the old overlap before rescheduling. Handoff keeps a compatible owned effect instead of tearing it down. Existing track identity, shuffled/repeat exclusions, sleep/focus and normalization gain limits are retained.

## Automated proof and its boundary

Twelve new JUnit methods: seven command-identity cases, three preset-readback cases and two crossfade-session cases. These execute real pure Kotlin policies, not simulated Android hardware. Existing queue, audio-route, EQ intent, crossfade and normalization regressions remain in the full suite.

The first full run passed all 1,433 tests in 179 suites, Android production and instrumentation-source compilation, debug assembly and lint. Fresh-discovery/UI refinements were then checked by the final Windows build; exact final receipts follow below. No instrumentation, speaker output, Bluetooth, headphone route or acoustic A/B test was executed here.

## Consolidated physical-device acceptance — pending

1. Open Sound Studio after service/process restart or a failed service start: no old cached active text; fresh/unavailable replies are distinguished.
2. Apply manual gains, switch outputs quickly and attempt Apply again: changed session/band/range/preset identities must reject the old command and refresh the draft. Unchanged identities preserve unsaved edits.
3. Select available device presets and compare the reported preset/band readback. A mismatched driver response must not be called active.
4. Revoke effect control with another supported system effect, then return control: live status updates; no retry fight, pause, automatic re-enable or stale callback from released instances.
5. Turn EQ Off and refresh/reopen Sound Studio: the effect stays off while supported capabilities remain discoverable.
6. Check speaker, wired/headphone and Bluetooth playback with audible A/B. Framework readback alone cannot establish audible EQ on every route.
7. Crossfade eligible local tracks with EQ on/off, apply EQ during overlap and change routes near handoff: preserve selected queue order, one primary media session and safe normal-transition fallback.
8. Confirm Shuffle, Repeat One, speed changes, sleep timer, headset/audio-focus and missing-file exclusions still prevent inappropriate overlap.
9. Check normalization with loud/quiet owned recordings and user listening level: no amplification above unity; do not describe the bounded RMS sample as integrated LUFS.
10. Background/close the dialog: polling stops, playback continues, persisted EQ intent restores only on a compatible live session.

## Scope, recovery and remaining work

No package/version/signing identity, stored audio, playlists, Drive account, OAuth scopes, system volume, music permissions or database schema version changed. No phone is installed/reset or account action performed. Public release remains NO-GO pending physical-device, installed-signer, Drive/privacy/rights and earlier-wave acceptance. Next ordered wave: NW121 Drive/guest backup.

The original NW119 Git bundle is in `C:\Users\jayed\NightWave-checkpoints\20261027-nw120-audio`. Final changed-file hashes, source ZIP, verified debug candidate, full receipts, checksums and resume instructions are saved there. Restore into a separate new checkout; preserve the review branch and existing phone storage.

## Final Windows receipts

- One-click `tools/build-windows.ps1`: **BUILD SUCCESS**, followed by independent source attestation.
- JVM results: **1,433 passed, 179 suites, zero failures/errors/skips**; required floor 1,400 executed tests.
- APK package: `com.nightwave.player`; version `2.0.0-alpha5s`, code `35`.
- Debug APK SHA256: `138ACC0113EE57881168FE4B9A89E24EEEC77538D120ACEA89FFDDC9E12425AB`.
- Working-tree source fingerprint SHA256: `4C705BEE737D2C3075B9F925BC84BF42EBB63E8C35CB0A59E66BA923500B396D`, **785 source/resource/test/tool files**.
- Debug certificate SHA256: `EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394`. Independent comparison to the saved NW119 APK: **MATCH**. This does not compare an installed phone.
- Current source preflights: **49/49 PASS**.

Receipts and complete JUnit XML are retained in the external checkpoint. Full source ZIP members are checked against the tested working-tree bytes, including line endings, with CRC and per-file SHA256 checks. Git may normalize line endings in committed blobs.

Final `:app:lintDebug`: **PASS**, zero errors, 313 warnings and 17 hints. No warning baseline or new lint suppression was used. Framework readback/compiled callbacks remain separate from audible route/device proof.
