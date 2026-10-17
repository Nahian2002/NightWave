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
    private val tones = listOf(
        Color(0xFF76CFCE), Color(0xFFA7A1F6), Color(0xFFFAAD86),
        Color(0xFFA1C992), Color(0xFFCF97CD), Color(0xFF96BDE9)
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
    Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
        Surface(
            onClick = onLibrary,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(favoriteCount.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold)
                Text("Liked songs", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(recentCount.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold)
                Text("Recent tracks", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    Card(
        onClick = onClick,
        modifier = modifier.height(if (compact) 106.dp else 122.dp),
        shape = RoundedCornerShape(19.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surface)
    ) {
        Box(Modifier.fillMaxSize().background(
            Brush.linearGradient(
                listOf(accent.copy(alpha = .23f), scheme.surfaceVariant, scheme.surface)
            )
        )) {
            Canvas(Modifier.fillMaxSize()) {
                val center = Offset(size.width * .89f, size.height * .16f)
                drawCircle(accent.copy(alpha = .08f), radius = size.width * .31f, center = center)
                drawCircle(accent.copy(alpha = .25f), radius = size.width * .24f,
                    center = center, style = Stroke(width = 1.5.dp.toPx()))
            }
            Column(
                Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 11.dp),
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
                    Text("${mix.tracks.size} tracks · ${if (selected) "Collapse" else "View tracks"}",
                        style = MaterialTheme.typography.labelSmall,
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
    Card(onClick = onClick, modifier = modifier.height(126.dp),
        shape = RoundedCornerShape(21.dp)) {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(safeColors))) {
            Canvas(Modifier.fillMaxSize()) {
                val center = Offset(size.width * .81f, size.height * .18f)
                val radius = size.width * .34f
                drawCircle(Color.White.copy(alpha = .09f), radius, center)
                drawCircle(Color.White.copy(alpha = .24f), radius * .73f, center,
                    style = Stroke(width = 1.5.dp.toPx()))
                drawCircle(Color.White.copy(alpha = .13f), radius * .46f, center,
                    style = Stroke(width = 1.5.dp.toPx()))
                for (line in 0..2) {
                    val y = size.height * (.25f + line * .14f)
                    drawLine(Color.White.copy(alpha = .08f),
                        start = Offset(size.width * .50f, y),
                        end = Offset(size.width * .96f, y),
                        strokeWidth = 1.dp.toPx(), cap = StrokeCap.Round)
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
