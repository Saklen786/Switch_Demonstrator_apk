package org.ssay.switchdemo.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.ssay.switchdemo.R
import org.ssay.switchdemo.data.HeadlightMode
import org.ssay.switchdemo.data.IndicatorMode
import org.ssay.switchdemo.data.SwitchState

private data class GlowOverlay(
    val drawableRes: Int,
    val centerX: Float,
    val centerY: Float,
    val sizeFraction: Float
)

@Composable
fun MotorcycleGraphic(
    switchState: SwitchState,
    modifier: Modifier = Modifier
) {
    val isUpper = switchState.headlight == HeadlightMode.UPPER || switchState.pass
    val isDipper = switchState.headlight == HeadlightMode.DIPPER

    // Indicator blink state
    var blinkOn by remember { mutableStateOf(false) }
    LaunchedEffect(switchState.indicator) {
        if (switchState.indicator != IndicatorMode.NONE) {
            while (true) {
                blinkOn = !blinkOn
                delay(500)
            }
        } else {
            blinkOn = false
        }
    }

    // Opacity animations
    val highLeftAlpha by animateFloatAsState(if (isUpper) 1f else 0f, tween(150), label = "hl")
    val highRightAlpha by animateFloatAsState(if (isUpper) 1f else 0f, tween(150), label = "hr")
    val lowLeftAlpha by animateFloatAsState(if (isDipper) 1f else 0f, tween(150), label = "ll")
    val lowRightAlpha by animateFloatAsState(if (isDipper) 1f else 0f, tween(150), label = "lr")
    val indLeftAlpha by animateFloatAsState(
        if (switchState.indicator == IndicatorMode.LEFT && blinkOn) 1f else 0f,
        tween(150), label = "il"
    )
    val indRightAlpha by animateFloatAsState(
        if (switchState.indicator == IndicatorMode.RIGHT && blinkOn) 1f else 0f,
        tween(150), label = "ir"
    )

    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Base motorcycle image
        Image(
            painter = painterResource(R.drawable.moto_base),
            contentDescription = "Motorcycle",
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    imageSize = coordinates.size
                },
            contentScale = ContentScale.Fit
        )

        if (imageSize.width > 0 && imageSize.height > 0) {
            val imgW = with(density) { imageSize.width.toDp() }
            val imgH = with(density) { imageSize.height.toDp() }

            // Compute the actual rendered image dimensions (accounting for aspect ratio)
            val painter = painterResource(R.drawable.moto_base)
            val intrinsicRatio = painter.intrinsicSize.width / painter.intrinsicSize.height
            val containerRatio = imageSize.width.toFloat() / imageSize.height.toFloat()

            val (renderedW, renderedH) = if (intrinsicRatio > containerRatio) {
                imgW to (imgW / intrinsicRatio)
            } else {
                (imgH * intrinsicRatio) to imgH
            }

            val offsetX = (imgW - renderedW) / 2
            val offsetY = (imgH - renderedH) / 2

            // Glow overlays
            val glowSize = renderedW * 0.3f
            val indSize = renderedW * 0.25f

            // High beam left
            GlowImage(R.drawable.high_beam, highLeftAlpha, 0.343f, 0.556f, glowSize, renderedW, renderedH, offsetX, offsetY)
            // High beam right
            GlowImage(R.drawable.high_beam, highRightAlpha, 0.649f, 0.556f, glowSize, renderedW, renderedH, offsetX, offsetY)
            // Low beam left
            GlowImage(R.drawable.low_beam, lowLeftAlpha, 0.337f, 0.559f, glowSize, renderedW, renderedH, offsetX, offsetY)
            // Low beam right
            GlowImage(R.drawable.low_beam, lowRightAlpha, 0.658f, 0.559f, glowSize, renderedW, renderedH, offsetX, offsetY)
            // Indicator left
            GlowImage(R.drawable.indicator_glow, indLeftAlpha, 0.157f, 0.517f, indSize, renderedW, renderedH, offsetX, offsetY)
            // Indicator right
            GlowImage(R.drawable.indicator_glow, indRightAlpha, 0.835f, 0.519f, indSize, renderedW, renderedH, offsetX, offsetY)
        }
    }
}

@Composable
private fun BoxScope.GlowImage(
    drawableRes: Int,
    alpha: Float,
    cx: Float,
    cy: Float,
    size: androidx.compose.ui.unit.Dp,
    renderedW: androidx.compose.ui.unit.Dp,
    renderedH: androidx.compose.ui.unit.Dp,
    offsetX: androidx.compose.ui.unit.Dp,
    offsetY: androidx.compose.ui.unit.Dp
) {
    if (alpha > 0.01f) {
        val x = offsetX + renderedW * cx - size / 2
        // Invert Y because Compose uses top-left origin, but our fractions are from bottom
        val y = offsetY + renderedH * (1f - cy) - size / 2

        Image(
            painter = painterResource(drawableRes),
            contentDescription = null,
            modifier = Modifier
                .offset(x = x, y = y)
                .size(size)
                .alpha(alpha),
            contentScale = ContentScale.Fit
        )
    }
}
