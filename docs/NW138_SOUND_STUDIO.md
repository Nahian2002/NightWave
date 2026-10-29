# NW138 Sound Studio active state and draft clarity

Protected parent: `9f05a0d5dda60ceeb22d184b24669a0ce842242f`, cumulative NW137 review tip. Recovery: private `20261030-nw138-sound-studio` baseline bundle and working-byte archive. No parallel batch or source edits were active at admission.

1. Put current verified session state and simple Off/device presets first. Separate the manual curve as an explicitly opened advanced draft; preserve all existing controls, transition preferences, guided curves, refresh and compatibility information.
2. Keep a full-width Apply action outside the scrolling content, with current readback separate from unapplied edits. Add discard-to-readback and accessible frequency/gain semantics with independent adjustment targets. Preserve system font scaling and theme tokens.
3. Preserve service-side session/control/frequency/range/preset checks and all engine/data formats. Retain a failed manual draft for the same identity; adopt acknowledged valid readback after successful Apply, reset on changed/unavailable identity and never imply hardware audibility from Android readback.
4. Add meaningful pure state/readback tests and actual reusable Compose component tests (runtime remains NOT_RUN without a device), plus current source caller checks with negative fixtures. Run current source checks, the full Windows builder, then sequential lint; verify exact source/APK/signer and preservation against NW137.
5. Record source/build evidence and remaining speaker/headphone/Bluetooth A/B, large-font/TalkBack, stale ownership and transition/speed phone cases. Publish only a cumulative draft review and verified owner-only Drive APK with byte/hash readback. No installation, reset, merge, public binary, provider/quality change or score increase.

Budget checks apply before expensive phases and at least every ten minutes. Physical acceptance remains NOT_RUN. NW139 follows after local verification; final certification still requires real device evidence.

## Implemented behavior

The current Android readback and exact active preset name appear before simple Off/device presets. Their immediate action is explicit. Advanced equalizer opens the original graph, guided curves, reset and every device-reported band. Closing Advanced retains the in-dialog draft and shows an unapplied reminder. Closing the dialog discards ephemeral unsubmitted edits; saved EQ intent retains its original format.

Apply and Discard are outside the weighted scrolling content. Discard reloads current readback without sending an audio command. The curve is explicitly a preview, separate from the current active state. Frequency/gain semantics and independent bounded 1 dB controls supplement the slider. Original theme tokens, engine, source quality and transition/normalization/speed policies are unchanged; the local transition block is relocated byte-for-byte.

An exact request acknowledgement alone is insufficient to replace a manual draft. The currently matching identity, active manual status, matching durable requested choice and verified/device-rounded band readback must agree. A failed save cannot masquerade as harmless rounding of an old active curve. A rejected same-identity manual/preset request retains the draft; changed/unavailable identities reset through the original invalidation policy. Refresh preserves edits for unchanged identity. Displayed identity is captured for commands and checked against current identity during slider/step callbacks.

## Verification limits

Physical acceptance remains NOT_RUN in the original NW138 matrix and eight supplemental cases. The four added Compose tests exercise the actual Apply component in its production sibling layout with isolated callbacks, not the full dialog/service/phone route. Their source compilation is separate from execution. No acoustic, TalkBack, all-font/theme, transition route, saved-choice migration or 9.5 certification is inferred. Public-provider/source, account, playback service and stored data contracts are unchanged. No automatic install, reset, account operation, merge, public APK or Actions/settings change.

## Local evidence and exact identity

- Protected parent `9f05a0d5dda60ceeb22d184b24669a0ce842242f`; plan `4a474fbd42af94a4f81ca02f8e7457a4920e446f`; implementation `d91b659cb16925fd9b21cd7026c7bb861ef0d222`.
- Full Windows builder PASS: Android/Compose/APK and instrumentation-source compilation. **1,793 JVM tests / 221 suites**, zero failures/errors/skips; nine new policy tests. **78 current source checks PASS**, including negative caller fixtures.
- Sequential lint PASS: **0 errors / 322 warnings / 17 hints**, identical ID/severity/message/location inventory to NW137 after ignoring line movement. No new suppression or baseline. Existing unchanged Library compiler condition warning remains.
- **927 prior canonical inputs byte-identical**, three intended existing edits and five additions, 935 final inputs. All five Sound Studio command types retained; original local transition UI block is byte-identical. Playback service, capabilities/command/selection/readback stores and policies, sources/quality, themes, dependency/package/signing and data formats unchanged.
- Implementation-stage all-ref Gitleaks: **3,561 commits**, no leaks; reviewed exclusions unchanged. Private source recovery, raw JVM/lint/build receipts and owner-only Drive permission plus streamed bytes/SHA readback verified.
- Build **NW-debug-B4B205C1BB2D17BA**; canonical source **B4B205C1BB2D17BA11E1334EB5E322DD59B513E1CAF7C85A1A6AB17B94BE6D9E**.
- APK **E9117263EA0B0F20FDC9218E8F6F8A0111EF0D30133738932D367D5B6308052C**, **33,844,842 bytes**; signer **EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394**. Package `com.nightwave.player`, version `2.0.0-alpha5s` / code35 unchanged.
- Four new Compose runtime methods and [eight supplemental phone cases](quality/NW138_SOUND_STUDIO_ACCEPTANCE.csv) **NOT_RUN**; original NW138 and prior matrices retained. Final rating/release acceptance remains open. NW139 is next.

One successful pre-refinement build is retained privately as superseded evidence. Review then strengthened the durable-choice/readback check and added a failed-save regression; all final checks above use the final refined source. An evidence-summary assertion initially expected the generic label PASS rather than actual PASS_SOURCE_ONLY; its corrected readback confirmed all 78 checks without altering product source or claiming Android runtime results.
