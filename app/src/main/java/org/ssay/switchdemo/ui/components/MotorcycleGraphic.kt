package org.ssay.switchdemo.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.ssay.switchdemo.R
import org.ssay.switchdemo.data.HeadlightMode
import org.ssay.switchdemo.data.IndicatorMode
import org.ssay.switchdemo.data.SwitchState

private data class GlowSpec(
    val drawableRes: Int,
    val cx: Float,
    val cy: Float,
    val sizeFraction: Float
)

private val HIGH_BEAM_LEFT  = GlowSpec(R.drawable.high_beam,      cx = 0.343f, cy = 0.444f, sizeFraction = 0.30f)
private val HIGH_BEAM_RIGHT = GlowSpec(R.drawable.high_beam,      cx = 0.649f, cy = 0.444f, sizeFraction = 0.30f)
private val LOW_BEAM_LEFT   = GlowSpec(R.drawable.low_beam,       cx = 0.337f, cy = 0.441f, sizeFraction = 0.30f)
private val LOW_BEAM_RIGHT  = GlowSpec(R.drawable.low_beam,       cx = 0.658f, cy = 0.441f, sizeFraction = 0.30f)
private val IND_LEFT        = GlowSpec(R.drawable.indicator_glow, cx = 0.157f, cy = 0.483f, sizeFraction = 0.25f)
private val IND_RIGHT       = GlowSpec(R.drawable.indicator_glow, cx = 0.835f, cy = 0.481f, sizeFraction = 0.25f)

@Composable
fun MotorcycleGraphic(
    switchState: SwitchState,
    blinkRateMs: Long = 500L,
    showIndicatorLabels: Boolean = true,
    modifier: Modifier = Modifier
) {
    val isUpper  = switchState.headlight == HeadlightMode.UPPER || switchState.pass
    val isDipper = switchState.headlight == HeadlightMode.DIPPER

    var blinkOn by remember { mutableStateOf(false) }
    LaunchedEffect(switchState.indicator, blinkRateMs) {
        if (switchState.indicator != IndicatorMode.NONE) {
            while (true) { blinkOn = !blinkOn; delay(blinkRateMs) }
        } else { blinkOn = false }
    }

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

    BoxWithConstraints(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopStart) {
        val basePainter   = painterResource(R.drawable.moto_base)
        val intrinsicSize = basePainter.intrinsicSize
        val aspectRatio   = if (intrinsicSize.height > 0f) intrinsicSize.width / intrinsicSize.height else 1f
        val renderedW: Dp = maxWidth
        val renderedH: Dp = maxWidth / aspectRatio

        Box(modifier = Modifier.fillMaxWidth().height(renderedH), contentAlignment = Alignment.TopStart) {
            Image(
                painter            = basePainter,
                contentDescription = "Motorcycle",
                modifier           = Modifier.fillMaxSize(),
                contentScale       = ContentScale.Fit
            )
            GlowOverlay(HIGH_BEAM_LEFT,  highLeftAlpha,  renderedW, renderedH)
            GlowOverlay(HIGH_BEAM_RIGHT, highRightAlpha, renderedW, renderedH)
            GlowOverlay(LOW_BEAM_LEFT,   lowLeftAlpha,   renderedW, renderedH)
            GlowOverlay(LOW_BEAM_RIGHT,  lowRightAlpha,  renderedW, renderedH)
            GlowOverlay(IND_LEFT,        indLeftAlpha,   renderedW, renderedH)
            GlowOverlay(IND_RIGHT,       indRightAlpha,  renderedW, renderedH)

            if (showIndicatorLabels) {
                if (switchState.indicator == IndicatorMode.LEFT)
                    IndicatorLabel("◄ LEFT",  renderedW * IND_LEFT.cx  - 28.dp, renderedH * IND_LEFT.cy  - 30.dp, indLeftAlpha,  true)
                if (switchState.indicator == IndicatorMode.RIGHT)
                    IndicatorLabel("RIGHT ►", renderedW * IND_RIGHT.cx - 28.dp, renderedH * IND_RIGHT.cy - 30.dp, indRightAlpha, true)
                val beamLabel = when { isUpper -> "HIGH BEAM"; isDipper -> "LOW BEAM"; else -> null }
                if (beamLabel != null)
                    IndicatorLabel(beamLabel, renderedW * 0.5f - 30.dp, renderedH * 0.30f, 1f, false)
                if (switchState.horn)
                    IndicatorLabel("♪ HORN",  renderedW * 0.5f - 28.dp, renderedH * 0.55f, 1f, true)
                if (switchState.warn)
                    IndicatorLabel("⚠ WARN",  renderedW * 0.5f - 28.dp, renderedH * 0.20f, 1f, true)
            }
        }
    }
}

@Composable
private fun BoxScope.IndicatorLabel(text: String, x: Dp, y: Dp, alpha: Float, highlight: Boolean) {
    if (alpha <= 0.01f) return
    Text(
        text       = text,
        fontSize   = 10.sp,
        fontWeight = FontWeight.Bold,
        color      = if (highlight) Color(0xFFFFD600) else Color(0xFF00F0FF),
        modifier   = Modifier
            .absoluteOffset(x = x, y = y)
            .background(Color(0xCC000000), RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .alpha(alpha)
    )
}

@Composable
private fun BoxScope.GlowOverlay(spec: GlowSpec, alpha: Float, renderedW: Dp, renderedH: Dp) {
    if (alpha <= 0.01f) return
    val glowSize = renderedW * spec.sizeFraction
    Image(
        painter            = painterResource(spec.drawableRes),
        contentDescription = null,
        modifier           = Modifier
            .absoluteOffset(x = renderedW * spec.cx - glowSize / 2, y = renderedH * spec.cy - glowSize / 2)
            .size(glowSize)
            .alpha(alpha),
        contentScale = ContentScale.Fit
    )
}