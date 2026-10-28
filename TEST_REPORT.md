# Current verification — four-theme composition and Search input clarity

28 October 2026. Existing [draft PR #146](https://github.com/Nahian2002/NightWave/pull/146), `design/nightwave-expressive-themes`, stacked on DESIGN01 #145. Protected prior review `246bee33cbc25c2bcfb1850e20ee32a11f2125f6`; implementation `25982c3a`, completed by `17c827e4936bdb65beff3e0d952d32df74e01153`. [New recording, four-theme ratings, cause and acceptance](docs/design/2026-10-28_THEME_POLISH_211137.md). [Previous exact B017 verification](https://github.com/Nahian2002/NightWave/blob/246bee33cbc25c2bcfb1850e20ee32a11f2125f6/TEST_REPORT.md) and [attributable D5 review](docs/design/2026-10-28_THEME_VALIDATION_202916.md) remain historical evidence.

The latest approximately 147.516-second recording supplies 147 successful one-second samples, a five-second overview and selected details. All four choices appear. On the retained five-dimension appearance rubric, recorded **Midnight/Aurora score 7.6/10; Pearl/AMOLED 7.4/10**. Alternate-theme coverage is narrower, and an exact APK identity is not exposed in inspected frames. Pearl's light system bar and the navy native confirmation are observed improvements; these do not certify all combinations. No new-candidate or whole-app score is assigned from source/tests.

At the end, **Song or artist name** is a placeholder in an empty field, not submitted query text. Source separately contained a real stale-draft risk: broad `LaunchedEffect(state.query)` could reapply a retained provider query when the pager recreated Search. Explicit Home/shared-text prefill requests now carry unique transient tokens consumed once in saveable UI state; ordinary responses cannot overwrite the draft. New intentional same-query shortcuts remain supported. The hint disappears on focus, uses an explicit readable theme role and retains an accessible field name. No automatic public request or history deletion is added.

Shared design polish replaces the second outlined navigation panel with a flat canvas rail and smaller icon selection capsule, removes the mini-player's redundant stroke, aligns its gutters/corners, fills secondary actions and softens decorative separators. Four palette values, primary accents, all routes, full tab targets/labels/semantics, motion preference, native/classic option, account/consent, original URLs, queue, library, persistence, package/signing identity and dependencies remain.

| Executed or compiled check | Result and limit |
| --- | --- |
| Complete Windows builder | PASS: production Android/Compose, debug assembly and instrumentation-source compilation. Initial missing UI import was repaired before this verified attempt; failed receipt retained privately. |
| JVM suite | **1,614 tests / 201 suites**, zero failures/errors/skips; five new Search ownership methods. |
| Current source runner | **62/62 PASS** on final implementation, including **12 expressive negative fixtures**; earlier safeguards retained. |
| Android theme/layout matrix | Ten methods × 36 configurations = **360 planned cases compile only, NOT_RUN**. Existing Search method now checks focus-hint removal, exact submit, late-response non-overwrite, clear/recreate retention and intentional repeated shortcut. |
| Sequential lint | PASS: **0 errors / 313 warnings / 17 hints**, matching the protected baseline; no new suppression or lint baseline. |
| Color/interaction contracts | Existing normal-text ≥4.5:1, required outline ≥3:1, primary ink ≥7:1, responsive layout, reduced motion and target guards pass. Decorative separator opacity is not a rendered accessibility certificate. |
| Feature preservation | **234 protected nonpresentation source/schema/build files byte-identical**; prior callback-name sets retained in nine major feature files. This does not replace actual installed-data acceptance. |
| APK signer | PASS against protected delivered B017 APK, same retained certificate. No agent-connected installed-APK comparison or installation. |
| Acceptance pack | Fresh 219-case pack prepared from actual source/APK/provenance, **PREPARED_NOT_EXECUTED**. |
| Publication/device | Actions stays disabled; no public binary or hosted CI pass. No agent install/reset/account/data operation. |

| Artifact identity | Value |
| --- | --- |
| Build ID | `NW-debug-B985BE0BD6205B3D` |
| Canonical source SHA256 | `B985BE0BD6205B3DCC62857E3DB607FF725234455AD5BD33D58CA6F53A122300` |
| Source inputs | `862` |
| APK SHA256 | `3594F08F4AEB252B56B877A2C7C18E8C64D2B621A70F71FB48CB877A78795198` |
| Embedded revision context | `17c827e4936bdb65beff3e0d952d32df74e01153` |
| Separate NW08 manifest SHA256 | `D29BDCE2B58467272BB717B444E6A9A2BD2AE07483F4DE22D3BB6BCD49F45E92`; retained independent encoding/ordering contract |
| App identity | `com.nightwave.player`, `2.0.0-alpha5s`, code35; min26/compile-target36 unchanged |
| Signing certificate SHA256 | `EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394` |

Private checkpoint `C:\Users\jayed\NightWave-checkpoints\20261028-theme-polish-211137` retains recording metadata/frames, first-failure and final receipts, feature preservation, source/APK recovery and the new unexecuted acceptance pack. A candidate is delivered privately to the verified owner; media/accounts/credentials/binaries remain outside Git and public releases.

The new **B985** polish/input fix needs phone review across all themes, Search page disposal/re-entry, repeated explicit shortcuts, TalkBack/large type, keyboard, native/system-bar combinations and all prior physical/data/audio/performance/release gates. No physical test or 10/10 claim is inferred from passing code checks. Overall provisional product score stays **6.7/10**, release **NO-GO**. **NW134 remains the next feature batch** from this cumulative branch.
