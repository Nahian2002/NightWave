package com.nightwave.player.v2.ui

import android.graphics.drawable.Drawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap

/** Shared feedback reads the real Material interaction source; never intercepts gestures. */
@Composable
private fun pressedScale(source: MutableInteractionSource): Float {
    val pressed by source.collectIsPressedAsState()
    val reduceMotion = LocalNightWaveReduceMotion.current
    val scale by animateFloatAsState(NightWaveMattePolicy.pressScale(pressed, reduceMotion),
        tween(NightWaveMattePolicy.pressDuration(reduceMotion), easing = LinearOutSlowInEasing),
        label = "Matte press")
    return scale
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NightWaveMatteButton(
    onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(20.dp),
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = paletteColor(NightWaveMattePolicy.transportFill(LocalNightWavePalette.current)),
        contentColor = paletteColor(NightWaveMattePolicy.transportInk(LocalNightWavePalette.current))),
    elevation: ButtonElevation? = null, border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val scale = pressedScale(source)
    CompositionLocalProvider(LocalRippleConfiguration provides null) {
        Button(onClick, modifier, enabled, shape, colors, elevation, border, contentPadding, source) {
            Row(Modifier.graphicsLayer { scaleX = scale; scaleY = scale },
                verticalAlignment = Alignment.CenterVertically) { content() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NightWaveMatteOutlinedButton(
    onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(20.dp),
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val scale = pressedScale(source)
    CompositionLocalProvider(LocalRippleConfiguration provides null) {
        OutlinedButton(onClick, modifier, enabled, shape, colors, elevation, border, contentPadding, source) {
            Row(Modifier.graphicsLayer { scaleX = scale; scaleY = scale },
                verticalAlignment = Alignment.CenterVertically) { content() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NightWaveMatteTextButton(
    onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(20.dp),
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    elevation: ButtonElevation? = null, border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val scale = pressedScale(source)
    CompositionLocalProvider(LocalRippleConfiguration provides null) {
        TextButton(onClick, modifier, enabled, shape, colors, elevation, border, contentPadding, source) {
            Row(Modifier.graphicsLayer { scaleX = scale; scaleY = scale },
                verticalAlignment = Alignment.CenterVertically) { content() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NightWaveMatteIconButton(
    onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    content: @Composable () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val scale = pressedScale(source)
    CompositionLocalProvider(LocalRippleConfiguration provides null) {
        IconButton(onClick, modifier, enabled, colors, interactionSource = source) {
            // Only drawn content scales. The original 48dp touch/semantic bounds stay fixed.
            Box(Modifier.graphicsLayer { scaleX = scale; scaleY = scale }, contentAlignment = Alignment.Center) {
                content()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NightWaveMattePlayButton(
    onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit
) {
    val palette = LocalNightWavePalette.current
    val source = remember { MutableInteractionSource() }
    val scale = pressedScale(source)
    CompositionLocalProvider(LocalRippleConfiguration provides null) {
        // Player callers provide >=64dp; the visual scale cannot reduce the target below 48dp.
        FilledIconButton(onClick, modifier.graphicsLayer { scaleX = scale; scaleY = scale },
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = paletteColor(NightWaveMattePolicy.transportFill(palette)),
                contentColor = paletteColor(NightWaveMattePolicy.transportInk(palette))),
            interactionSource = source, content = content)
    }
}

/** Stable reading order and equal columns; all cells stack at narrow/large text sizes. */
@Composable
internal fun NightWaveBentoPair(first: @Composable () -> Unit, second: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("matte-bento-pair")) {
        if (NightWaveMattePolicy.bentoColumns(maxWidth.value, LocalDensity.current.fontScale) == 2) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f).fillMaxHeight()) { first() }
                Box(Modifier.weight(1f).fillMaxHeight()) { second() }
            }
        } else Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            first(); second()
        }
    }
}

/** Reuses the selected cover's successful drawable: no second request, URL or provider. */
@Composable
internal fun NightWaveArtworkBackdrop(artwork: Drawable?, modifier: Modifier = Modifier) {
    val palette = LocalNightWavePalette.current
    val colors = MaterialTheme.colorScheme
    val bitmap = remember(artwork) { artwork?.let {
        runCatching { it.toBitmap(NightWaveMattePolicy.BACKDROP_BITMAP_PX,
            NightWaveMattePolicy.BACKDROP_BITMAP_PX).asImageBitmap() }.getOrNull()
    } }
    Box(modifier.clipToBounds().background(colors.background).testTag("matte-artwork-backdrop")) {
        if (bitmap != null && NightWaveMattePolicy.supportsBlur(android.os.Build.VERSION.SDK_INT)) {
            Image(bitmap, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    alpha = NightWaveMattePolicy.backdropAlpha(palette.isDark)
                    scaleX = 1.1f; scaleY = 1.1f
                }.blur(NightWaveMattePolicy.BLUR_DP.dp, BlurredEdgeTreatment.Rectangle))
        }
        // API26-30, failed decode and missing art use the same solid matte canvas.
    }
}
