# NW126 — build identity and reproducible acceptance baseline

28 October 2026. Protected parent: post-NW125 audit `daec2f4d91701286fe7168d8ecff4c3ba3c2287f`. Branch: `update/nightwave-nw126-build-identity-baseline`.

## Delivered source behavior

You → App health → App details opens the installed build identity. It shows a human-readable source ID and complete source digest, version/variant and revision context. App details also hashes the installed base APK and its public signing certificate bytes on IO when explicitly opened. The receipt labels split installations as base-APK hashes with a split count. Copy is an explicit local clipboard action; no report is sent. APK hashing is streaming and bounded at 512MiB, with unavailable status on failure; no file path or signing key is exposed. Loading and invalid/missing metadata have explicit states. The packaged asset is read on IO, is limited to 4,096 bytes and cannot manufacture a verified identity from malformed metadata.

Gradle generates `assets/nightwave-build.json` separately for debug/release, outside `app/src`. It fingerprints the actual app source/resources/tests, six existing build configuration inputs and the same versioned tools selected by NW08. The recipe is `SHA256(UTF8("NightWave-source-v1\n" + canonical_manifest))`; manifest rows are uppercase file SHA256, two spaces, project-relative path and LF, ordered by ordinal UTF-16 path comparison. This is a new canonical identity alongside the existing NW08 manifest-file fingerprint, whose Windows encoding/ordering contract is retained.

The ID covers actual working-tree bytes, including relevant uncommitted inputs. Git HEAD is only labelled **revision context**, not a claim that the source tree equals that commit. Archive builds report revision unavailable while retaining the byte-derived source ID. No timestamps, account, username, machine path, branch or remote URL enter the packaged identity. Actual APK bytes remain separately identified by their complete APK SHA256; a source ID does not make every compiler/environment produce an identical APK.

The independent PowerShell verifier reads the identity from the actual APK, rejects absent/duplicate/oversized or malformed assets, recomputes the source digest and records APK SHA256. It is required before the Windows builder promotes the candidate and in current CI before artifact publication. The builder invalidates older provenance receipts at the start of each attempt. Source identity is build-chain evidence; signer continuity, installed provenance and device acceptance remain separate checks.

## Disposable baseline pack

Run after a verified current build:

```powershell
python tools/nw126_acceptance.py --self-test
python tools/nw126_acceptance.py --prepare --provenance dist/NightWave-build-provenance.json --apk dist/NightWave-debug.apk --output build/nw126-fixture-pack
```

Preparation checks the APK hash, canonical current-source digest and input count against the packaged-identity receipt. It refuses an existing output directory. It creates:

- Six original two-second PCM WAV test tones for a separately approved disposable import/codec test, plus clearly separated invalid/empty negative files.
- A synthetic 10,000-row metadata corpus and 0/500/5,000/10,000 corpus-size/boundary recipes, with 30 playlists as the future UI-harness target. References are synthetic, explicitly unplayable and must never be fetched. This is not a production backup/import format and does not seed the app.
- An APK-bound plan mapping all original 139 and new 80 acceptance IDs, with source-matrix hashes. Every run begins NOT_RUN while preserving source-ledger status separately.
- Flagship and midrange measurement-profile templates for OS/model/RAM/storage/refresh, power/temperature/network/route/volume, font/window conditions. Actual device fields remain unset until captured. The existing S24 Ultra checklist is a proposed flagship reference, not proof of a connected device.
- Per-file SHA256 inventory. The pack contains no real library, credentials or account archive. Longer owned audio is required for real transition/soak testing; six short tones are insufficient for those claims.

No phone, app store, Google account or existing app data is contacted or modified by preparation.

## Required device completion

No Android device was connected during this batch. The two new Android tests compile only; no on-phone rendering, clipboard, signer or in-place upgrade was executed.

1. Record App details with the full installed APK/source/signer information and retain the exact matching APK/provenance receipt. The old owner's recording cannot be retroactively bound to a source hash.
2. Confirm and capture both reference-device profiles before measuring; use identical conditions for before/after. Fill an actual midrange model rather than treating a target profile as hardware evidence.
3. Run the existing installed-signer checker before any separately approved in-place installation. Preserve library/profile/settings/queue/account boundaries and compare disposable before/after content; no reinstall/data reset.
4. Execute packaged-identity/dialog Android tests and mapped startup/preservation cases, then attach real evidence to NW126 acceptance rows. Device acceptance remains NOT_RUN until that happens.

The next independent source batch is NW127 compact visual hierarchy. NW126 local completion may enable that work while these physical checks remain explicit dependencies for final certification. The subjective product score remains 6.7/10 pending fresh product evidence; this infrastructure batch does not earn a visual rating increase.

## Executed local evidence

- Complete one-click Windows builder: BUILD SUCCESS. Production/Compose, Android instrumentation-source compilation and debug assembly pass.
- 1,496 JVM tests / 188 suites: zero failures/errors/skips. Twelve new methods execute metadata validation, bounded/stalled reads, complete SHA256 vectors, artifact/signer receipt formatting and split-base labeling.
- 54 current checks pass, including NW125 positive/negative and normal gates, fixture preparation self-test, and PowerShell packaged-asset/source mismatch cases. A real older APK/source snapshot was rejected and left NOT_VERIFIED.
- Two new Android tests compile for packaged metadata and dialog/copy semantics; no Android test was device-executed.
- Android lint: zero errors, 313 warnings, 17 hints. No new warning suppression/baseline.
- Build ID: `NW-debug-F2C870939591E7C8`. Canonical source SHA256: `F2C870939591E7C82DE9D357C3FD1F2379469ED920E3B477C251ABA9E3402DB9` (809 source inputs). Gradle, the independent PowerShell APK verifier and Python fixture preparation agree.
- APK SHA256: `9297D5F9747D38208321652A709DE06DCDDAF4131572B5EDE2B7AE3FEDEFC581`. NW08 manifest fingerprint: `95796293E6F87A84713DE48BD3524510402B235312EC60975756587AF23B4435`.
- Certificate SHA256: `EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394`; local previous-APK continuity matches. Installed-device continuity remains untested.
- `adb devices -l` returned no connected devices. No install, Google/account operation or user-library mutation occurred.
- Final APK-bound fixture pack is prepared with all 219 cases NOT_RUN in the run plan; source-ledger outcomes are preserved separately. Actual hardware fields remain unset.

Acceptance ledger: NW126-02 PASS for executed gates; NW126-01 and NW126-03 PARTIAL for local identity/fixture delivery with physical proof pending; NW126-04 NOT_RUN. NW126 implementation is LOCAL_VERIFIED; device acceptance is NOT_RUN. Next independent source batch: NW127.

Implementation commit: `5ddc9827d3d596925de328cfcfbbadaa2599f2eb`. This commit records the runtime/tooling change; the following ledger-only update does not change the compiled source identity.
