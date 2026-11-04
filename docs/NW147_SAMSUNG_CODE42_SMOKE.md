# NW147 — Approved code42 phone comparison

4 November 2026. Owner explicitly approved **“Install version42 and test.”** Connected Samsung SM-S928B reports Android17/API37; current read-only profile is1440×3120, density override560, font scale0.8, AMOLED. This is one profile, not all-phone coverage. Raw captures, UI trees, file hashes, package/installer receipts and APKs remain private outside Git.

## Installation and data

Actual installed code41 was pulled again and its hash matched the previous candidate. The code42 signer matched it. In-place installation succeeded and the pulled installed code42 exactly matches **234662C9E20922AC0FD4BCED765B1648F2257E9C9DDB21AB2783187D5883C736**, 28,066,796 bytes. Exact canonical source and signer are in [NW147 evidence](quality/NW147_SOURCE_EVIDENCE.json).

All **29 collected preference/database file hashes were unchanged** immediately before/after installation, before first launch, with the app stopped. This does not certify every audio file or restoration path. Subsequent normal app opening/discovery can update runtime state. Cold opening reported1,362ms in one ADB sample; no fully-drawn or percentile claim.

## Matched observations

| Check | Code41 → Code42 | Scope |
| --- | --- | --- |
| Featured mix card | 112dp →104dp | Same profile and item, actual clickable bounds; artwork remains88dp |
| Sound Studio settings row | 64dp →56dp | Full title/detail visible |
| Theme and reduced-motion rows | 64dp →60dp each | Existing trailing controls retained; no preference toggled |
| Sound Studio status | Smaller readback card | Same Active/Normal and verified-readback/draft-status text visible; no audible quality claim |
| Playback compatibility | Expand →scroll →collapse observed | Original band/preset/range and route explanation visible; Refresh reachable; no preset, Apply, Discard, crossfade or balance action invoked |
| Queue | Current track +Paused retained | Existing selection/count and Edit order/Clear controls retained; no queue edits invoked; Back returns to player then Home |
| Current-process crash buffer | No fatal record found | Current app PID only; not historical or all-process certification |

All measurements use fresh matched captures/trees and560dpi/3.5 density conversion. They establish these specific bounds, not universal compactness or tap efficiency. Source/layout values and earlier NOT_RUN checkpoints remain in [implementation report](NW147_COMPACT_SECONDARY.md).

The UI hierarchy tool returned a nonzero status in Sound Studio on both code41 and42, while screenshots succeeded. The failure was retained; no stale tree or screenshot was treated as a successful hierarchy read. Disclosure touches used its observed screenshot position, followed by fresh visual readback. No app-crash cause, spoken accessibility result or resolution of R06 is inferred. One Sound Studio label lookup encountered Home after an earlier You capture and performed no tap; re-entering You and taking two settled snapshots succeeded. The cause of that state movement is unknown; retain it for controlled navigation/frame review in NW149.

## Remaining acceptance and final state

Device acceptance is **PARTIAL**. The40 new Android UI cases remain COMPILED_NOT_RUN; these manual checks do not execute them. Other themes, fonts1/2/3,320/360/411dp synthetic profiles, short landscape, TalkBack, keyboard/input/action isolation, actual sound edits/draft conflict/Apply/Discard and other devices remain NOT_RUN. Original219 and supplemental matrices, older Android, long absence/process death, performance/audio/battery/route/soak/study gates stay unpromoted. No new score or release approval follows.

Phone left **Home, existing selection paused**. No sound preference, library/account/consent or system setting was changed by these tests; no reset/uninstall or test APK installed. Installation approval covers code42 only. Full NW145 remains PARTIAL; next source batch **NW148**. Documentation does not change app source/APK; prior host results were not rerun.

[Draft review #173](https://github.com/Nahian2002/NightWave/pull/173) remains unmerged.
