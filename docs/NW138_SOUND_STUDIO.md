# NW138 Sound Studio implementation plan

Protected parent: `9f05a0d5dda60ceeb22d184b24669a0ce842242f`, cumulative NW137 review tip. Recovery: private `20261030-nw138-sound-studio` baseline bundle and working-byte archive. No parallel batch or source edits were active at admission.

1. Put current verified session state and simple Off/device presets first. Separate the manual curve as an explicitly opened advanced draft; preserve all existing controls, transition preferences, guided curves, refresh and compatibility information.
2. Keep a full-width Apply action outside the scrolling content, with current readback separate from unapplied edits. Add discard-to-readback and accessible frequency/gain semantics with independent adjustment targets. Preserve system font scaling and theme tokens.
3. Preserve service-side session/control/frequency/range/preset checks and all engine/data formats. Retain a failed manual draft for the same identity; adopt acknowledged valid readback after successful Apply, reset on changed/unavailable identity and never imply hardware audibility from Android readback.
4. Add meaningful pure state/readback tests and actual reusable Compose component tests (runtime remains NOT_RUN without a device), plus current source caller checks with negative fixtures. Run current source checks, the full Windows builder, then sequential lint; verify exact source/APK/signer and preservation against NW137.
5. Record source/build evidence and remaining speaker/headphone/Bluetooth A/B, large-font/TalkBack, stale ownership and transition/speed phone cases. Publish only a cumulative draft review and verified owner-only Drive APK with byte/hash readback. No installation, reset, merge, public binary, provider/quality change or score increase.

Budget checks apply before expensive phases and at least every ten minutes. Physical acceptance remains NOT_RUN. NW139 follows after local verification; final certification still requires real device evidence.
