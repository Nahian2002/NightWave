package com.nightwave.player.v2.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    val theme = MaterialTheme.colorScheme
    val accent = NightWaveVisualLanguage.accent(identity)
    Box(modifier.background(theme.background)) {
        Canvas(Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            // Restrained, static ambient light. No high-cost blurs or animation.
            drawCircle(
                brush = Brush.radialGradient(
                    0f to accent.copy(alpha = .21f),
                    .57f to accent.copy(alpha = .07f),
                    1f to Color.Transparent,
                    center = Offset(width * .80f, height * .13f),
                    radius = (width * .92f).coerceAtLeast(1f)
                ),
                radius = width * .92f,
                center = Offset(width * .80f, height * .13f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(theme.primary.copy(alpha = .075f), Color.Transparent),
                    center = Offset(width * .15f, height * .82f),
                    radius = (width * .72f).coerceAtLeast(1f)
                ),
                radius = width * .72f,
                center = Offset(width * .15f, height * .82f)
            )
        }
    }
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
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val fontScale = LocalDensity.current.fontScale
    val first = mix.tracks.firstOrNull()
    val trackLabel = "${mix.tracks.size} " +
        (if (mix.tracks.size == 1) "track" else "tracks") +
        " · " + (if (selected) "Collapse" else "View tracks")
    Card(onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp).testTag("nw131-mix-" + mix.key),
        shape = RoundedCornerShape(NightWavePremiumDesignTokens.CARD_CORNER_DP.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surface)) {
        // Single-column cards use a real thumbnail beside the caption rather
        // than a wide empty banner. Large text receives the whole row width.
        if (compact && fontScale < 1.3f) {
            Row(Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                if (first != null) TrackArtwork(first, Modifier.size(64.dp))
                Column(Modifier.weight(1f)) {
                    NightWaveMixCaption(mix, presentation, trackLabel, fontScale)
                }
            }
        } else {
            Column(Modifier.fillMaxWidth()) {
                if (!compact && fontScale < 1.3f && first != null) {
                    TrackArtwork(first, Modifier.fillMaxWidth().height(80.dp))
                }
                Column(Modifier.fillMaxWidth().background(scheme.surface)
                    .padding(NightWavePremiumDesignTokens.CARD_PADDING_DP.dp)) {
                    NightWaveMixCaption(mix, presentation, trackLabel, fontScale)
                }
            }
        }
    }
}

@Composable
private fun NightWaveMixCaption(
    mix: PersonalMix,
    presentation: NightWaveHomePresentationPolicy.MixPresentation?,
    trackLabel: String,
    fontScale: Float
) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(presentation?.caption ?: "FROM YOUR LIBRARY", style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant)
        Text(presentation?.title ?: mix.title, style = MaterialTheme.typography.titleMedium,
            color = scheme.onSurface, maxLines = if (fontScale >= 1.3f) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis)
        val startsWith = presentation?.startsWith.orEmpty()
        if (startsWith.isNotBlank()) Text(startsWith,
            style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant,
            maxLines = if (fontScale >= 1.3f) Int.MAX_VALUE else 2, overflow = TextOverflow.Ellipsis)
        Text(trackLabel, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
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
    // A malformed/empty editorial palette should not crash Home composition.
    val safeColors = if (colors.size >= 2) colors else
        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.surfaceVariant)
    val fontScale = LocalDensity.current.fontScale
    Card(onClick = onClick, modifier = modifier.heightIn(min =
        NightWaveAccessibilityPolicy.moodCardMinHeightDp(fontScale).dp),
        shape = RoundedCornerShape(21.dp)) {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(safeColors))) {
            Canvas(Modifier.fillMaxSize()) {
                // Six lightweight, genuinely distinct editorial motifs rather
                // than the same ring graphic pasted across every mood card.
                val w = size.width
                val h = size.height
                val soft = Color.White.copy(alpha = .17f)
                val bright = Color.White.copy(alpha = .30f)
                when (title.lowercase()) {
                    "after hours" -> {
                        for (i in 0..3) {
                            drawCircle(soft, radius = w * (.13f + i * .07f),
                                center = Offset(w * .91f, h * .22f),
                                style = Stroke(width = 1.3.dp.toPx()))
                        }
                        drawCircle(bright, radius = 3.dp.toPx(),
                            center = Offset(w * .56f, h * .22f))
                    }
                    "deep focus" -> {
                        for (i in 0..5) {
                            val x = w * (.57f + i * .065f)
                            drawLine(if (i == 2) bright else soft,
                                Offset(x, h * .14f), Offset(x, h * .60f),
                                strokeWidth = if (i == 2) 3.dp.toPx() else 1.dp.toPx(),
                                cap = StrokeCap.Round)
                        }
                    }
                    "desi pulse" -> {
                        for (i in 0..8) {
                            val x = w * (.48f + i * .058f)
                            val amplitude = (i % 4 + 1) * .07f
                            drawLine(if (i % 3 == 0) bright else soft,
                                Offset(x, h * (.37f - amplitude)),
                                Offset(x, h * (.37f + amplitude)),
                                strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                        }
                    }
                    "unplugged" -> {
                        for (i in 0..4) {
                            val y = h * (.13f + i * .10f)
                            drawLine(soft, Offset(w * .47f, y),
                                Offset(w * .98f, y + h * .18f),
                                strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
                        }
                    }
                    "momentum" -> {
                        for (i in 0..4) {
                            val x = w * (.50f + i * .09f)
                            val height = h * (.12f + i * .095f)
                            drawLine(if (i == 4) bright else soft,
                                Offset(x, h * .57f), Offset(x, h * .57f - height),
                                strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
                        }
                    }
                    else -> {
                        val sun = Offset(w * .77f, h * .28f)
                        drawCircle(bright, radius = w * .12f, center = sun)
                        for (i in 0..2) {
                            drawLine(soft, Offset(w * .49f, h * (.44f + i * .08f)),
                                Offset(w * .99f, h * (.44f + i * .08f)),
                                strokeWidth = 1.6.dp.toPx(), cap = StrokeCap.Round)
                        }
                    }
                }
            }
            Column(Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth().height(56.dp).padding(12.dp)) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null,
                        tint = Color.White, modifier = Modifier.size(23.dp))
                }
                Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
                    .padding(NightWavePremiumDesignTokens.CARD_PADDING_DP.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text(subtitle, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
