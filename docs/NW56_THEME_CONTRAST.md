# NW56 — Theme contrast and playback control readability

**Parent:** `feature/nightwave-nw55-offline-download-polish`  
**Branch:** `feature/nightwave-nw56-theme-contrast`  
**Status:** Source and numerical palette checks implemented. Android/Compose visual QA remains pending.

## Measured source issue and correction
- Previously Pearl's accent text was approximately **2.22:1 on raised cards**, and secondary text approximately **4.11:1**. These are below a 4.5:1 normal-text target and consistent with the weak labels observed in the latest screen recording.
- Pearl's accent changed to a darker wine/rose `0xFF815154`, secondary to `0xFF625A63`, and accent button text to cream `0xFFF9F7F2`. Resulting calculated ratios: **accent on raised 4.66:1**, **secondary on raised 4.77:1**, **text on accent 6.06:1**.
- Midnight, Aurora and AMOLED are unchanged in appearance. Source ratio checks confirm the four themes' configured text colors clear 4.5:1 against their tested surfaces.
- The expandable **Sound & playback** heading explicitly uses `colors.onSurface`, avoiding inherited/dim text against a semitransparent card.
- No extra glow animations, heavy gradients, frame loops or playback service changes introduced.

## Tests and caveats
- New platform-independent `NightWaveContrastPolicy.ratio` and three JVM tests validate all four palette text/background combinations, near-maximum black-white contrast and Pearl's readable accents.
- `tools/verify_nw56_contrast.py --self-test` validates the real ARGB palette values and includes four negative mutations. Android CI runs it after NW53–NW55.
- Actual accessible typography depends on device rendering, font scaling, transparency/overlays and TalkBack; phone screenshots/visual QA still required.
- Next: NW57 Home hierarchy and player transition polish. All branches remain sequential and unpublished.
