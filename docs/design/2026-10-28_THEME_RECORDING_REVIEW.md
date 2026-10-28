# Theme-only review of the 19:46 recording

28 October 2026. Owner request: judge the theme, rate it, then correct the dull presentation. This review does not reassess search results, playback, account flows, feature availability or the overall product score.

## Evidence and attribution

Recording `Screen_Recording_20261028_194647.mp4`: 83,334,766 bytes; SHA256 `c0212edbb44541e7314bf0738cc2efa3343413de19432e1d200a7dab6d72a17a`. Container reports 1080×2340, 10,117 frames, about 167.73 seconds and 60.316 reported fps. These are media properties, not a measured app frame rate.

167 timestamp-seek samples were extracted at one-second intervals. The complete sequence was visually reviewed in six private contact sheets, with a six-second overview and larger key frames. This is sampled visual inspection, not every-frame or acoustic testing. Recording/frame/account media stays outside Git.

App details around 01:09–01:12 visibly reports build `NW-debug-E5B5F71165F89BF8`, source/APK identities consistent with the protected DESIGN01 artifact, 855 inputs, implementation `b30b7a2e4e9318ac2a91d7edb2418488c649cd03`, and the retained signing certificate. The selected theme visibly says Midnight. The recording does not demonstrate Aurora, Pearl or AMOLED; those old palettes are inspected from source rather than given invented recording scores.

## Subjective theme score: 6.0/10

| Theme dimension | Score | Visual reason |
| --- | ---: | --- |
| Identity | 6 | Recognizable wave/mint cue, but too little character in the actual screen surfaces. |
| Color character | 4 | Most canvas/cards/controls collapse into black, gray and very pale mint. White primary pills feel disconnected from the small brand accents. |
| Tonal hierarchy | 5 | Navigation, mini-player and content cards differ too little; large single-collection mosaics overpower their captions. |
| Cross-screen coherence | 8 | Main screens use a consistent system; the full player is the strongest composition because real art supplies color. |
| Perceived legibility | 7 | Main labels are generally readable; small muted supporting copy has little visual emphasis. This is not a rendered WCAG contrast measurement. |

Equal weights produce 6.0/10. This is a theme-only judgment of the recorded Midnight build, not a new whole-app rating or a measured Spotify comparison. No post-change quality score is awarded from passing tests.

## Findings and correction

- **TF01 — low color character:** Home/Search/You around 00:02–00:39 and 02:24–02:46 show large nearly black areas with weak brand presence. Revise Midnight to navy/cyan, Aurora to plum/lavender/rose, Pearl to warm white/cobalt and AMOLED to black/teal/blue. Keep the existing saved enum identities.
- **TF02 — weak layer separation:** navigation, mini-player and main cards blend at 00:37–00:54. Raise the mini-player tone and give the selected navigation pill its theme container; use differentiated card and header surfaces.
- **TF03 — disconnected primary controls:** Search/backup white pills and full player white Play at 00:08–00:33, 00:40–00:47 and 01:56–02:08 contribute little theme identity. Use contrast-tested theme accent fills with paired ink, retaining target size and existing feedback/callbacks.
- **TF04 — decorative sections lost their character:** the small mood cards at 00:06 and 02:39 are almost the same black as surrounding areas. Use explicit primary/secondary theme section roles and visible vector motifs. Source inspection additionally finds white mood glyphs on Pearl's light surfaces; give glyphs the proper theme accent. This Pearl issue is source-derived, not observed in the recording.
- **TF05 — singleton visual imbalance:** around 00:05–00:06, 02:37–02:39, the final unpaired collection becomes a full-width square collage. Its existing compact feature treatment is appropriate; keep all four real thumbnails, collection, explanation and actions without the oversized square.

The correction preserves the main information architecture, density/type/accessibility rules, feature disclosures and data contracts. No glass, chrome, fake album artwork, invented service or neon lime is added. New gradients are opaque surface/container color transitions, not glossy effects. See [DESIGN02 implementation](EXPRESSIVE_THEME_FINISH.md).
