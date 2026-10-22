package com.nightwave.player.v2.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Equal-width, persistent destinations: text is always visible on compact
 * phones, unlike the previous selected-only expanding tab. The caller owns
 * the pager; this component cannot start playback or replace navigation.
 */
@Composable
fun NightWavePremiumNavigation(
    selectedIndex: Int,
    reducedMotion: Boolean,
    onNavigate: (Int) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val icons = listOf(
        Icons.Outlined.Home,
        Icons.Outlined.Search,
        Icons.Outlined.LibraryMusic,
        Icons.Outlined.PersonOutline
    )
    val names = listOf("Home", "Search", "Library", "You")
    val tags = listOf("home", "search", "library", "profile")
    Surface(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp),
        color = colors.surface,
        tonalElevation = 2.dp,
        shadowElevation = 5.dp,
        shape = RoundedCornerShape(19.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .height(NightWaveChromePolicy.NAV_HEIGHT_DP.dp)
                .padding(horizontal = 5.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            names.forEachIndexed { index, label ->
                val active = NightWaveChromePolicy.selectedTab(selectedIndex) == index
                val color by animateColorAsState(
                    targetValue = if (active) colors.primary.copy(alpha = .12f)
                        else Color.Transparent,
                    animationSpec = tween(
                        durationMillis = if (reducedMotion) 0 else 160
                    ),
                    label = "NightWave selected destination"
                )
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight()
                        .clip(RoundedCornerShape(17.dp))
                        .background(color)
                        .clickable(role = Role.Tab) { onNavigate(index) }
                        .semantics { selected = active }
                        .testTag("nightwave-tab-" + tags[index]),
                    contentAlignment = Alignment.Center
                ) {
                    if (active) {
                        Box(
                            modifier = Modifier.align(Alignment.TopCenter)
                                .padding(top = 1.dp)
                                .size(width = 22.dp, height = 2.dp)
                                .clip(CircleShape)
                                .background(colors.primary)
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icons[index],
                            contentDescription = null,
                            tint = if (active) colors.primary
                                else colors.onSurfaceVariant,
                            modifier = Modifier.size(21.dp)
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 12.sp, lineHeight = 16.sp),
                            fontWeight = if (active) FontWeight.Bold
                                else FontWeight.Medium,
                            color = if (active) colors.onSurface
                                else colors.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact, artwork-independent Home greeting. It only displays local profile
 * and theme data: no fabricated listening streaks, recommendations or art.
 */
@Composable
fun NightWaveHomeHero(
    greeting: String,
    displayName: String,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val accent = colors.primary
    val firstName = NightWaveChromePolicy.displayFirstName(displayName)
    val largeText = LocalDensity.current.fontScale >= 1.3f
    val heroHeight = if (largeText) 162.dp else 132.dp
    Box(
        modifier = modifier.fillMaxWidth()
            .height(heroHeight)
            .clip(RoundedCornerShape(23.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        accent.copy(alpha = .21f),
                        colors.surfaceVariant,
                        colors.surface
                    )
                )
            )
            .testTag("nightwave-home-hero")
    ) {
        Canvas(
            modifier = Modifier.align(Alignment.CenterEnd)
                .size(width = 138.dp, height = heroHeight)
        ) {
            val center = Offset(size.width * .78f, size.height * .52f)
            val scale = size.minDimension
            for (ring in 0..2) {
                drawCircle(
                    accent.copy(alpha = .12f + ring * .055f),
                    radius = scale * (.22f + ring * .15f),
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            drawCircle(accent.copy(alpha = .32f), radius = 4.dp.toPx(),
                center = center)
        }
        Column(
            modifier = Modifier.fillMaxHeight().fillMaxWidth(.85f)
                .padding(horizontal = 17.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            NightWaveMicroLabel("Your listening space")
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    greeting,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                    maxLines = if (largeText) 2 else 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Welcome, " + firstName + " · Your music, your pace",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
