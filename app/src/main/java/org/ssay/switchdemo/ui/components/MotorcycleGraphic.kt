package org.ssay.switchdemo.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.ssay.switchdemo.R
import org.ssay.switchdemo.data.HeadlightMode
import org.ssay.switchdemo.data.IndicatorMode
import org.ssay.switchdemo.data.SwitchState

/**
 * Glow overlay descriptor.
 *
 * [cx] / [cy] are fractions of the rendered image size measured from the
 * TOP-LEFT corner (standard Compose coordinate convention: 0,0 = top-left).
 * [sizeFraction] is the glow size as a fraction of the rendered image width.
 *
 * Tune these fractions to match your moto_base.png artwork.
 */
private data class GlowSpec(
    val drawableRes: Int,
    val cx: Float,   // 0.0 = left edge, 1.0 = right edge
    val cy: Float,   // 0.0 = top edge, 1.0 = bottom edge
    val sizeFraction: Float
)

// ---------------------------------------------------------------------------
// Tunable glow positions — adjust cx / cy to match your moto_base artwork.
// All values are fractions from the TOP-LEFT of the rendered motorcycle image.
// ---------------------------------------------------------------------------
private val HIGH_BEAM_LEFT  = GlowSpec(R.drawable.high_beam,      cx = 0.343f, cy = 0.444f, sizeFraction = 0.30f)
private val HIGH_BEAM_RIGHT = GlowSpec(R.drawable.high_beam,      cx = 0.649f, cy = 0.444f, sizeFraction = 0.30f)
private val LOW_BEAM_LEFT   = GlowSpec(R.drawable.low_beam,       cx = 0.337f, cy = 0.441f, sizeFraction = 0.30f)
private val LOW_BEAM_RIGHT  = GlowSpec(R.drawable.low_beam,       cx = 0.658f, cy = 0.441f, sizeFraction = 0.30f)
private val IND_LEFT        = GlowSpec(R.drawable.indicator_glow, cx = 0.157f, cy = 0.483f, sizeFraction = 0.25f)
private val IND_RIGHT       = GlowSpec(R.drawable.indicator_glow, cx = 0.835f, cy = 0.481f, sizeFraction = 0.25f)

@Composable
fun MotorcycleGraphic(
    switchState: SwitchState,
    modifier: Modifier = Modifier
) {
    val isUpper  = switchState.headlight == HeadlightMode.UPPER || switchState.pass
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
    val highLeftAlpha  by animateFloatAsState(if (isUpper)  1f else 0f, tween(150), label = "hl")
    val highRightAlpha by animateFloatAsState(if (isUpper)  1f else 0f, tween(150), label = "hr")
    val lowLeftAlpha   by animateFloatAsState(if (isDipper) 1f else 0f, tween(150), label = "ll")
    val lowRightAlpha  by animateFloatAsState(if (isDipper) 1f else 0f, tween(150), label = "lr")
    val indLeftAlpha   by animateFloatAsState(
        if (switchState.indicator == IndicatorMode.LEFT  && blinkOn) 1f else 0f, tween(150), label = "il"
    )
    val indRightAlpha  by animateFloatAsState(
        if (switchState.indicator == IndicatorMode.RIGHT && blinkOn) 1f else 0f, tween(150), label = "ir"
    )

    // ---------------------------------------------------------------------------
    // Use BoxWithConstraints so we know the available width at composition time —
    // no need for onGloballyPositioned or a post-layout recompose.
    // ---------------------------------------------------------------------------
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopStart
    ) {
        val basePainter = painterResource(R.drawable.moto_base)

        // Rendered size: fill width, height constrained by intrinsic aspect ratio
        val intrinsicSize = basePainter.intrinsicSize
        val aspectRatio   = if (intrinsicSize.height > 0f) intrinsicSize.width / intrinsicSize.height else 1f
        val renderedW: Dp = maxWidth
        val renderedH: Dp = maxWidth / aspectRatio

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(renderedH),
            contentAlignment = Alignment.TopStart
        ) {
            // Base motorcycle image
            Image(
                painter     = basePainter,
                contentDescription = "Motorcycle",
                modifier    = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            // Glow overlays — positioned absolutely from top-left of Box
            GlowOverlay(spec = HIGH_BEAM_LEFT,  alpha = highLeftAlpha,  renderedW = renderedW, renderedH = renderedH)
            GlowOverlay(spec = HIGH_BEAM_RIGHT, alpha = highRightAlpha, renderedW = renderedW, renderedH = renderedH)
            GlowOverlay(spec = LOW_BEAM_LEFT,   alpha = lowLeftAlpha,   renderedW = renderedW, renderedH = renderedH)
            GlowOverlay(spec = LOW_BEAM_RIGHT,  alpha = lowRightAlpha,  renderedW = renderedW, renderedH = renderedH)
            GlowOverlay(spec = IND_LEFT,        alpha = indLeftAlpha,   renderedW = renderedW, renderedH = renderedH)
            GlowOverlay(spec = IND_RIGHT,       alpha = indRightAlpha,  renderedW = renderedW, renderedH = renderedH)
        }
    }
}

/**
 * Draws a single glow image at the position described by [spec].
 *
 * Skips composition entirely when [alpha] is effectively zero to avoid
 * unnecessary draw calls.
 */
@Composable
private fun BoxScope.GlowOverlay(
    spec: GlowSpec,
    alpha: Float,
    renderedW: Dp,
    renderedH: Dp
) {
    if (alpha <= 0.01f) return

    val glowSize = renderedW * spec.sizeFraction
    // Top-left corner of the glow image: centre position minus half size
    val x = renderedW * spec.cx - glowSize / 2
    val y = renderedH * spec.cy - glowSize / 2

    Image(
        painter = painterResource(spec.drawableRes),
        contentDescription = null,
        modifier = Modifier
            .absoluteOffset(x = x, y = y) // absoluteOffset ignores parent alignment
            .size(glowSize)
            .alpha(alpha),
        contentScale = ContentScale.Fit
    )
}