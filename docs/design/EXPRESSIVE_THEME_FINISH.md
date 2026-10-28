# DESIGN02 — expressive theme finish

Owner-requested theme correction following the [recorded 6.0/10 theme review](2026-10-28_THEME_RECORDING_REVIEW.md). Branch `design/nightwave-expressive-themes`; protected DESIGN01 parent `1413f0f3838df497b6c696ccd4243b5a6285916b`. Implementation `0b4e4dea6528f5faebf4c63659545d93cfc54a87`. [Draft PR #146](https://github.com/Nahian2002/NightWave/pull/146), stacked on PR #145. [Current exact verification](../../TEST_REPORT.md). This is visual/theme work; NW134 remains the next feature batch.

## Theme roles

| Role | Midnight | Aurora | Pearl | AMOLED |
| --- | --- | --- | --- | --- |
| Canvas | #090F1B | #160D24 | #F8F5F0 | #000000 |
| Card | #121E30 | #241636 | #FFFFFF | #0C171C |
| Raised | #1B2D44 | #342447 | #E8EDF8 | #142A30 |
| Main accent | #66D9EF | #C3A0FF | #1C47AD | #72EAD8 |
| Secondary accent | #B8ADFF | #FFB8D0 | #7C3BA9 | #9ABEFF |
| Primary container | #13384A | #4D2E70 | #DDE7FF | #143E3C |
| Secondary container | #342D55 | #542C48 | #F0E1F8 | #203551 |
| Supporting text | #BCCAE0 | #D3C0E5 | #4E6080 | #A8CAD3 |

Midnight is the recommended default direction: navy layers with cyan emphasis. Existing selected themes are preserved, never reset. Secondary accent/container roles are presentation-only values, with compatible defaults for older Palette constructor calls; the stored theme enum, preferences and archive format do not change.

## Actual application

- Shared primary buttons and full Play use the selected accent and its tested contrast ink. The prior Material interaction sources, 100ms feedback, Reduce Motion, disabled states, 48dp hit bounds and all callbacks stay intact.
- Selected navigation uses the primary container, mini-player uses the raised surface and progress uses the accent. Existing semantic selected/health/transport state remains authoritative.
- Continue has an opaque surface-to-theme-container transition. Profile and selected/featured mix headers use the primary container. Supporting text remains fully opaque.
- Mix captions use the actual card/raised theme surface. A final singleton uses the existing bounded horizontal feature treatment, with no removed track, artwork source, collection or action. Two-column cells and large-type stacking remain.
- Mood cards use stable semantic primary/secondary theme roles, visible theme-colored vector motifs and readable labels. Their destinations still perform the same explicit search; the color does not claim to describe a song or album.
- Material tertiary roles use the new secondary colors. Native theme selection now distinguishes all four dialog styles, including plum Aurora, teal AMOLED and cobalt Pearl. Dark recovery/widget resources follow the new Midnight palette; original platform/control behavior remains.
- Full player retains its actual loaded-art backdrop, bounded image candidates/cache/fallback, API26–30 solid fallback, identity checks, current-song status and input-aware seeking. This batch adds no request/provider or effect animation.

No feature menu, account/consent/backup, worker, parser, persistence, schema, package/signing identity or dependency changes. No font or external artwork is added. Existing classic recovery remains available. Scope is theme/visual hierarchy only.

## Verification

Eight new executed JVM methods test contrast on colored section surfaces, secondary roles, actual accented control fills/ink, separate tonal levels, stable mood roles, bounded singleton selection, compatible Palette defaults and distinct color accents. Accent channel spread ≥64 is a declared design criterion for avoiding the previous near-neutral accents, not a universal quality metric. Required normal text still meets ≥4.5:1, outlines ≥3:1 and primary control ink ≥7:1.

Two actual UI methods extend the existing 36-configuration matrix (320/360/412dp × 1×/2×/3× × four themes) from eight to ten methods: **360 cases compile only**, including 72 new singleton/mood cases. The [104-case supplement](EXPRESSIVE_THEME_ACCEPTANCE.csv) records those 72 plus 32 manual theme checks; actual new-build device execution remains NOT_RUN. The older DESIGN01 ledger and original 139+80 acceptance identities remain intact. The subsequent [attributable D5 recording](2026-10-28_THEME_VALIDATION_202916.md) supplies partial appearance evidence for all four choices; it is not an executed matrix or a pass for the B017 follow-up.

`verify_theme_expression.py --self-test` now includes ten broken wiring fixtures (eight original plus two boundary guards) to the current runner (62 checks), alongside every previous safeguard. NW59 follows the new approved Pearl accent; executed contrast tests remain mandatory. No assertion/suite or lint baseline is removed. Exact Gradle/JUnit/lint/source/signing receipts are in the current report.

Private checkpoint: `C:\Users\jayed\NightWave-checkpoints\20261028-theme-vibrancy`; retain recording review frames, source-palette reference (explicitly not an app screenshot), raw validation, exact source/APK and recovery archives privately. Repository Actions remains disabled. No public APK upload, installation, account or app-data operation is included. The attributable D5 recording rates primary Midnight 7.6/10 on the previous five-dimension rubric; overall product score/release gates are not re-rated. Subsequent Material3 container and system-bar fixes require new phone confirmation.

## Recorded acceptance follow-up

See the [new review](2026-10-28_THEME_VALIDATION_202916.md). Commit `6057bfaa2a0d8e0ed47b13705adc247902d67b4b` maps all seven Material3 container/brightness roles to each selected palette and disables Android's added navigation contrast scrim on API29+. The existing parameterized theme test asserts the actual color scheme roles. Palette values, feature behavior and the explicit classic option remain. Build B017 is locally verified; phone appearance is unrun.
