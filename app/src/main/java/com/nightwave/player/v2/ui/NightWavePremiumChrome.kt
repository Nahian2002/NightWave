package com.nightwave.player.v2.ui

import android.view.HapticFeedbackConstants

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import com.nightwave.player.v2.ui.NightWaveMatteIconButton as IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import com.nightwave.player.net.VideoPreview
import com.nightwave.player.net.MusicIdentity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** NW127: the actual measured window chooses text reflow, not screen metrics. */
@Composable
fun NightWaveActionGroup(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (NightWavePremiumDesignTokens.stackActions(maxWidth.value, LocalDensity.current.fontScale)) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                content()
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) { content() }
        }
    }
}

@Composable
fun NightWaveSectionActions(heading: @Composable () -> Unit, action: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (NightWavePremiumDesignTokens.stackActions(maxWidth.value, LocalDensity.current.fontScale)) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                heading()
                action()
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) { heading() }
                action()
            }
        }
    }
}

@Composable
fun NightWaveCompactTopBar(onSettings: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val showName = maxWidth.value >= 100f + 172f * LocalDensity.current.fontScale
        Row(Modifier.fillMaxWidth().heightIn(min = NightWavePremiumDesignTokens.HEADER_MIN_HEIGHT_DP.dp)
                .padding(horizontal = NightWavePremiumDesignTokens.pageGutterDp(maxWidth.value).dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            NightWaveWordmark(Modifier.semantics { if (!showName) contentDescription = "NightWave" },
                showName = showName)
            IconButton(onClick = onSettings, modifier = Modifier.size(48.dp)
                .testTag("nw127-open-settings")) {
                Icon(Icons.Outlined.Settings, contentDescription = "Open NightWave settings",
                    tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(21.dp))
            }
        }
    }
}

/**
 * Equal-width, persistent destinations: text is always visible on compact
 * phones, unlike the previous selected-only expanding tab. The caller owns
 * the pager; this component cannot start playback or replace navigation.
 */
@Composable
fun NightWavePremiumNavigation(
    selectedIndex: Int,
    reducedMotion: Boolean,
    onReselect: (() -> Unit)? = null,
    onNavigate: (Int) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val hapticView = LocalView.current
    val fontScale = LocalDensity.current.fontScale
    val navHeight = NightWaveAdaptiveChromePolicy.navHeightDp(fontScale).dp
    val extraLargeText = fontScale >= 1.6f
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
            .padding(horizontal = NightWavePremiumDesignTokens.pageGutterDp(
                androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.toFloat()).dp, vertical = 2.dp),
        color = colors.background,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        shape = RoundedCornerShape(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .height(IntrinsicSize.Min).heightIn(min = navHeight)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            names.forEachIndexed { index, label ->
                val active = NightWaveChromePolicy.selectedTab(selectedIndex) == index
                val color by animateColorAsState(
                    targetValue = if (active) colors.primaryContainer
                        else Color.Transparent,
                    animationSpec = tween(
                        durationMillis = NightWaveMotionPolicy.duration(
                            reducedMotion, NightWaveMotionPolicy.NAV_MS)
                    ),
                    label = "NightWave selected destination"
                )
                val iconScale by animateFloatAsState(
                    targetValue = NightWaveMotionPolicy.tabScale(active, reducedMotion),
                    animationSpec = if (reducedMotion) snap() else spring(
                        dampingRatio = .88f, stiffness = 600f),
                    label = "NightWave tab spring"
                )
                val indicatorProgress by animateFloatAsState(
                    targetValue = if (active) 1f else 0f,
                    animationSpec = if (reducedMotion) snap() else tween(
                        durationMillis = NightWaveMotionPolicy.duration(
                            false, NightWaveMotionPolicy.FEEDBACK_MS)),
                    label = "NightWave tab indicator"
                )
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(role = Role.Tab) {
                            if (NightWaveMotionPolicy.tabHaptic(selectedIndex, index,
                                    names.size, reducedMotion)) {
                                // Android handles device/user haptic preference.
                                hapticView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            }
                            if (!active) onNavigate(index) else onReselect?.invoke()
                        }
                        .semantics {
                            selected = active
                            // Even truncated visual labels retain a full TalkBack name.
                            contentDescription = NightWaveAccessibilityPolicy.navigationAnnouncement(label)
                        }
                        .testTag("nightwave-tab-" + tags[index]),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(Modifier.clip(CircleShape), contentAlignment = Alignment.Center) {
                            Box(Modifier.matchParentSize().graphicsLayer {
                                alpha = indicatorProgress
                                scaleX = indicatorProgress
                            }.background(color))
                            Icon(
                                imageVector = icons[index],
                                contentDescription = null,
                                tint = if (active) colors.primary
                                    else colors.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp).size(
                                    (if (extraLargeText) 24 else NightWavePremiumDesignTokens.ICON_SIZE_DP).dp)
                                    .graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                    }
                            )
                        }
                        Spacer(Modifier.height(if (extraLargeText) 3.dp else 4.dp))
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = if (extraLargeText) 14.sp else 13.sp,
                                lineHeight = if (extraLargeText) 19.sp else 18.sp),
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
    modifier: Modifier = Modifier,
    lastTrack: VideoPreview? = null,
    onContinue: (() -> Unit)? = null,
    emptyTitle: String? = null,
    emptyDetail: String? = null,
    emptyActionLabel: String? = null,
    onEmptyAction: (() -> Unit)? = null
) {
    val firstName = NightWaveChromePolicy.displayFirstName(displayName)
    val colors = MaterialTheme.colorScheme
    val fontScale = LocalDensity.current.fontScale
    val identity = lastTrack?.let { NightWaveEditorialPresentationPolicy.identity(it.title, it.author) }
    val clickableHero = if (lastTrack != null && onContinue != null) {
        modifier.clickable(role = Role.Button, onClick = onContinue)
    } else if (onEmptyAction != null) modifier.clickable(role = Role.Button,
        onClickLabel = emptyActionLabel, onClick = onEmptyAction) else modifier
    Row(clickableHero.fillMaxWidth()
        .heightIn(min = NightWaveAdaptiveChromePolicy.homeHeroHeightDp(fontScale).dp)
        .clip(RoundedCornerShape(NightWavePremiumDesignTokens.HERO_CORNER_DP.dp))
        .background(androidx.compose.ui.graphics.Brush.horizontalGradient(
            listOf(colors.surface, colors.primaryContainer)))
        .testTag("nightwave-home-hero")
        .padding(NightWavePremiumDesignTokens.CARD_PADDING_DP.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (lastTrack != null && fontScale < 1.3f) {
            TrackArtwork(lastTrack, Modifier.size(56.dp))
        }
        // Opaque surface under all captions: artwork cannot alter contrast.
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            NightWaveMicroLabel(
                if (identity == null) "Your listening space" else "Continue listening")
            Text(if (identity == null) emptyTitle ?: greeting else identity.title,
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurface, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text(if (identity == null)
                emptyDetail ?: ("Welcome, " + firstName + " · Your music, your pace")
                else identity.artistOrCreator + " · Tap to continue",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant)
            if (identity == null && emptyActionLabel != null) Text(emptyActionLabel + " →",
                style = MaterialTheme.typography.labelLarge, color = colors.primary)
        }
    }
}
