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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
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
    onLibrary: () -> Unit
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
                Text(recentCount.toString(),
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
    onClick: () -> Unit
) {
    val accent = NightWaveVisualLanguage.accent(mix.key)
    val scheme = MaterialTheme.colorScheme
    val realCover = mix.tracks.firstOrNull()?.thumbnail?.takeIf { it.isNotBlank() }
    val trackLabel = "${mix.tracks.size} " +
        (if (mix.tracks.size == 1) "track" else "tracks") +
        " · " + (if (selected) "Collapse" else "View tracks")
    Card(
        onClick = onClick,
        modifier = modifier.height(if (compact) 118.dp else 146.dp),
        shape = RoundedCornerShape(19.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surface)
    ) {
        Box(Modifier.fillMaxSize().background(scheme.surfaceVariant)) {
            if (realCover != null) {
                // Use genuine, already-saved video artwork, not repeated
                // synthetic rings or fabricated album imagery.
                AsyncImage(
                    model = realCover, contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = .70f
                )
                Box(Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(listOf(
                        scheme.surface.copy(alpha = .98f),
                        scheme.surface.copy(alpha = .78f),
                        scheme.surface.copy(alpha = .22f)
                    ))
                ))
            } else {
                Box(Modifier.fillMaxSize().background(
                    Brush.linearGradient(listOf(
                        accent.copy(alpha = .22f), scheme.surfaceVariant, scheme.surface))
                ))
                Canvas(Modifier.fillMaxSize()) {
                    val center = Offset(size.width * .89f, size.height * .16f)
                    drawCircle(accent.copy(alpha = .07f),
                        radius = size.width * .27f, center = center)
                    drawCircle(accent.copy(alpha = .20f),
                        radius = size.width * .23f, center = center,
                        style = Stroke(width = 1.5.dp.toPx()))
                }
            }
            Column(
                Modifier.fillMaxSize().padding(horizontal = 17.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null,
                        tint = accent, modifier = Modifier.size(17.dp))
                    Text("FROM YOUR LIBRARY", style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant, maxLines = 1)
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(mix.title, style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSurface, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                    Text(trackLabel, style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant, maxLines = 1)
                }
            }
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
    // A malformed/empty editorial palette should not crash Home composition.
    val safeColors = if (colors.size >= 2) colors else
        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.surfaceVariant)
    Card(onClick = onClick, modifier = modifier.height(118.dp),
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
            Column(
                Modifier.fillMaxSize().padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = null,
                    tint = Color.White.copy(alpha = .90f),
                    modifier = Modifier.size(23.dp))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium,
                        color = Color.White, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                    Text(subtitle, style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = .83f), maxLines = 2,
                        overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
