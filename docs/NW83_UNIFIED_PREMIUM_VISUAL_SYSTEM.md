# NW83 — Unified Premium Visual System

**Owner roadmap:** [NW76–NW90](CURRENT_CUMULATIVE_ROADMAP.md) · Wave 2, phase NW83  
**Cumulative parent:** `feature/nightwave-nw82-search-precision`  
**Current source:** `feature/nightwave-nw83-unified-premium-visual-system`  
**Status:** Theme and shared Compose surface changes committed; source-audit and narrow Kotlin token compilation completed. Final Android compilation, rendered screenshots, accessibility and physical-device quality acceptance remain **pending** at NW90.

## Actual changes

### 1. Single premium design-token contract
- Added `NightWavePremiumDesignTokens.kt` with shared card/hero/navigation corners, touch targets, artwork and row geometry, page content cap, row gutters, border selection and font-aware minimum row heights.
- For 320 / 390 / 900dp windows the established responsive page gutters are 14 / 20 / 28dp. Width, font-scale NaN and negative-size inputs fail to safe values.
- Song row minimum height grows from 72dp at standard scaling to 88dp (>=1.3) or 108dp (>=1.8), using **minimum**, never rigid heights that clip larger text.
- Title lines adapt to available window width or enlarged fonts. Icon-button click targets continue to meet 48dp baseline.

### 2. Typography and complete Material3 color roles
- Explicitly define **all 15 Material3 typography roles** in `NightWaveTheme.kt`: display/headline/title/body/label, large/medium/small. This prevents some dialogs, sections and support surfaces from falling back to inconsistent stock text styles.
- Extend all four palette Material3 schemes with explicit primary/secondary/tertiary container and text roles, surfaceTint, outlineVariant, inverse surface and error action colors. Previously unspecified defaults could inject mismatched Material tones into NightWave's custom themes.
- **Contrast correction found in review:** Pearl danger text against raised color was only ~4.02:1; error containers now use the theme's normal surface, yielding **~4.68:1** in Pearl and at least 4.5:1 across the other built-in palettes. Keep the 12sp small-label role readable rather than shrinking it to 11sp.
- Preserve saved theme choice including AMOLED, Aurora and Pearl. Preserve existing contrast requirements and Android's SP accessibility scaling. No remote fonts or billing.

### 3. Shared card, navigation and page geometry
- `MusicTrackTile`: responsive multiline titles, readable row-height minima, consistent corner radius and artwork size, subtle selected outline/background, uncluttered `Creator · Playing` subtitle rather than a leading decorative bullet.
- `MusicCollectionTile`, `NightWaveCollectionHeader` and Search Offline action share rounded geometry from the new contract.
- `NightWavePremiumNavigation` has a controlled outline, theme-consistent corner radius and unified icon sizing. The Home hero shares the hero radius.
- Home, Search, Library and You all use the **same adaptive horizontal page gutter and maximum 740dp content width**, replacing independent fixed paddings.
- Now Playing's Up Next rows use the same row-height and responsive song-title rules without changing their swipe-to-remove or selection callbacks.
- Mini-player uses a single identical radius for shadow, clipping and outline (previously 19dp and 16dp differed), with subdued colored illumination and a legible boundary.

### 4. Restrained tonal identity
- `NightWaveVisualLanguage.accent()` now selects from six subdued teal/cool-blue/lavender/seafoam hues, replacing unrelated bright orange/pink/green decorative tones.
- Accent variation is still deterministic from an existing saved music/collection identifier and only decorative: **no simulated album art, false metadata, altered media IDs or network asset requests**.

## Regression and evidence

- Added `NightWavePremiumDesignTokensTest.kt` with five pure unit tests covering responsive gutters, row sizing, multiline titles, 48dp+ touch targets and restrained selection states.
- Extended `NightWaveContrastPolicyTest.kt` with a Material3 container/text-role contrast matrix for Midnight, Aurora, Pearl and AMOLED, including explicit checks for error containers and readable controls. Checked those concrete ARGB pairs with a standalone WCAG luminance calculation.
- Added `tools/NW83VisualTokensSmoke.kt` — full exact-source pure-Kotlin smoke entrypoint against `NightWaveChromePolicy`, `NightWaveAdaptiveChromePolicy`, `NightWavePremiumDesignTokens` and `NightWaveMotionPolicy`. It is available for the NW90/integration build, **not claimed executed here**.
- Added `tools/verify_nw83_premium_visual.py` source-only contract for all 15 typography roles, Material color coverage, Home/Search/Library/You gutter wiring, track/queue token use, navigation and touch sizing.
- **Executed targeted Kotlin compilation and run:** `NightWavePremiumDesignTokens.kt` plus a test-only stub for its prior `NightWaveAdaptiveChromePolicy.horizontalPaddingDp` dependency, and direct policy cases, returned `NW83_PREMIUM_DESIGN_TOKENS_TARGETED_SMOKE_PASS`. The exact local tokens file Git blob matched GitHub source SHA `1ec81eb0ab944bd812efe730b830ab7df648de1f`. **This does not prove the full standalone smoke or Android app compiled.**
- GitHub source readback confirmed all 15 typography role declarations, explicitly overridden Material color roles in both light and dark palettes, all four responsive page gutters, row/queue wiring, and five new unit-test cases. Sampled Kotlin braces balanced. Full Android build/test still pending.
- Git blob SHA parity with the NW82 parent confirmed playback engine, cloud backup, offline download, library audio storage, Search ranking, AndroidManifest and app Gradle settings unchanged.

## NW90 final visual and device QA

Test on S24 Ultra and small-width/split-screen emulators in both normal and large system font sizes; inspect typography clipping, RTL/Bangla glyphs, under-360dp screens, toolbar/dock/mini-player layering, touch-target and screen reader navigation, system dynamic type, light/dark/AMOLED palettes, reduced-motion behavior, animation frame times, 60Hz/120Hz frame pacing and screenshots across Home/Search/Library/You/Now Playing/Up Next. Record exact pixel issues and fix them before claiming the 9.5+/10 quality target.

**Status boundaries:** NW83 is a shipped GitHub **source update**, not a finished visual certification. NW77 public-release music-source rights blocker remains **NO-GO**.
