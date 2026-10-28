package com.nightwave.player.v2.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nightwave.player.v2.recommendation.PersonalMix

/**
 * NightWave 6A artwork-independent palette. The accent is based only on a
 * stable saved track/collection identifier, NOT fake art, remote content,
 * third-party metadata, sensor data or an expensive thumbnail bitmap decode.
 */
object NightWaveVisualLanguage {
    // NW83: a cool, coherent ambient spectrum around NightWave's teal
    // identity. Per-track variation stays subtle; loud orange/pink accents
    // no longer make Home, queue and music cards look like separate apps.
    // These tones are decorative only, never status/text contrast tokens.
    private val tones = listOf(
        Color(0xFF83D9D5), Color(0xFF7FBAD3), Color(0xFF8DAFD2),
        Color(0xFFA6A7DB), Color(0xFF90C8BD), Color(0xFFA9C5DD)
    )

    fun accent(identity: String): Color {
        val index = (identity.hashCode().toLong() and 0x7fffffffL).rem(tones.size).toInt()
        return tones[index]
    }
}

@Composable
fun NightWaveBackdrop(identity: String, modifier: Modifier = Modifier) {
    // The route identity remains a caller contract; ambient color belongs only to real player art.
    Box(modifier.background(MaterialTheme.colorScheme.background))
}

@Composable
fun NightWaveMicroLabel(value: String) {
    Text(
        value.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun NightWaveLibraryNumbers(
    favoriteCount: Int,
    recentCount: Int,
    onLibrary: () -> Unit,
    recentHasMore: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    // One intentional library overview instead of multiple competing cards.
    Surface(
        onClick = onLibrary,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = colors.surfaceVariant.copy(alpha = .38f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 17.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("YOUR LIBRARY", style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant)
                Text("Your music, all in one place",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(favoriteCount.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = colors.primary)
                Text("Liked", style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Home intentionally renders at most 100 unique songs.
                // Never display a hard 100 when the real recent list has more.
                Text(if (recentHasMore) "${recentCount}+" else recentCount.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = colors.onSurface)
                Text("Recent", style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun NightWaveMixCard(
    mix: PersonalMix,
    selected: Boolean,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    presentation: NightWaveHomePresentationPolicy.MixPresentation? = null,
    featured: Boolean = false,
    emphasized: Boolean = false,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val fontScale = LocalDensity.current.fontScale
    val first = mix.tracks.firstOrNull()
    val trackLabel = if (selected) "Collapse tracks" else "View tracks"
    Card(onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp).testTag("nw131-mix-" + mix.key)
            .semantics { this.selected = selected },
        shape = RoundedCornerShape(NightWavePremiumDesignTokens.CARD_CORNER_DP.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected || emphasized) scheme.primaryContainer else scheme.surface),
        border = BorderStroke(1.dp, if (selected) scheme.primary else
            scheme.onSurface.copy(alpha = NightWaveMattePolicy.BORDER_ALPHA))) {
        // Single-column cards use a real thumbnail beside the caption rather
        // than a wide empty banner. Large text receives the whole row width.
        if ((featured && fontScale < 1.3f) || (compact && fontScale < 1.3f)) {
            Row(Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                if (first != null) NightWaveMixMosaic(mix, Modifier.size(if (featured) 88.dp else 64.dp))
                Column(Modifier.weight(1f)) {
                    NightWaveMixCaption(mix, presentation, trackLabel, fontScale, selected)
                }
            }
        } else {
            Column(Modifier.fillMaxWidth()) {
                if (!compact && fontScale < 1.3f && first != null) {
                    NightWaveMixMosaic(mix, Modifier.fillMaxWidth().aspectRatio(1.6f))
                }
                Column(Modifier.fillMaxWidth().background(if (selected || emphasized) scheme.primaryContainer else scheme.surface)
                    .padding(NightWavePremiumDesignTokens.CARD_PADDING_DP.dp)) {
                    NightWaveMixCaption(mix, presentation, trackLabel, fontScale, selected)
                }
            }
        }
    }
}

/** Four actual source thumbnails, never generated album art or fabricated listening statistics. */
@Composable
private fun NightWaveMixMosaic(mix: PersonalMix, modifier: Modifier = Modifier) {
    val tracks = remember(mix.key, mix.tracks) { NightWaveEditorialPresentationPolicy.mixArtwork(mix) }
    Column(modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        verticalArrangement = Arrangement.spacedBy(2.dp)) {
        tracks.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                row.forEach { track -> TrackArtwork(track, Modifier.weight(1f).fillMaxSize(), radius = 4) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun NightWaveMixCaption(
    mix: PersonalMix,
    presentation: NightWaveHomePresentationPolicy.MixPresentation?,
    trackLabel: String,
    fontScale: Float,
    selected: Boolean
) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (fontScale >= 1.3f) Text(presentation?.caption ?: "FROM YOUR LIBRARY", style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant)
        Text(presentation?.title ?: mix.title, style = MaterialTheme.typography.titleMedium,
            color = scheme.onSurface, maxLines = if (fontScale >= 1.3f) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis)
        val startsWith = presentation?.purpose?.takeIf { it.isNotBlank() } ?: presentation?.startsWith.orEmpty()
        if (startsWith.isNotBlank()) Text(startsWith,
            style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant,
            maxLines = if (fontScale >= 1.3f) Int.MAX_VALUE else 2, overflow = TextOverflow.Ellipsis)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(NightWaveEditorialPresentationPolicy.mixSummary(mix),
                modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant)
            Icon(if (selected) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = trackLabel, modifier = Modifier.size(20.dp), tint = scheme.primary)
        }
    }
}

@Composable
fun NightWaveMoodArt(
    title: String,
    subtitle: String,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // Legacy palette arguments stay source-compatible; selected-theme roles own contrast.
    val palette = LocalNightWavePalette.current
    val secondary = NightWaveThemeExpressionPolicy.secondaryMood(title)
    val surface = paletteColor(NightWaveThemeExpressionPolicy.sectionSurface(palette, secondary))
    val accent = paletteColor(NightWaveThemeExpressionPolicy.sectionAccent(palette, secondary))
    val fontScale = LocalDensity.current.fontScale
    val motif = NightWaveMoodArtPolicy.motif(title)
    Card(onClick = onClick, modifier = modifier.heightIn(min =
        NightWaveAccessibilityPolicy.moodCardMinHeightDp(fontScale).dp),
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth()) {
            // Artwork has its own measured lane. Caption growth can never cover it.
            Row(Modifier.fillMaxWidth().height(72.dp).testTag("mood-art-header")
                .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Search, contentDescription = null,
                    tint = accent, modifier = Modifier.size(23.dp))
                Canvas(Modifier.weight(1f).height(56.dp).testTag("mood-art-viewport")) {
                    val viewport = NightWaveMoodArtPolicy.viewport(size.width, size.height, 4.dp.toPx())
                        ?: return@Canvas
                    fun point(x: Float, y: Float) = Offset(
                        viewport.left + x * viewport.side, viewport.top + y * viewport.side)
                    val soft = accent.copy(alpha = .38f)
                    val bright = accent.copy(alpha = .76f)
                    motif.lines.forEach { line ->
                        drawLine(if (line.strong) bright else soft,
                            point(line.x1, line.y1), point(line.x2, line.y2),
                            strokeWidth = line.strokeDp.dp.toPx(), cap = StrokeCap.Round)
                    }
                    motif.rings.forEach { ring ->
                        if (ring.filled) drawCircle(bright, ring.radius * viewport.side, point(ring.x, ring.y))
                        else drawCircle(soft, ring.radius * viewport.side, point(ring.x, ring.y),
                            style = Stroke(width = 1.5.dp.toPx()))
                    }
                }
            }
            Column(Modifier.fillMaxWidth().testTag("mood-caption")
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
