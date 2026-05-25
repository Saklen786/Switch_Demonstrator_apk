package org.ssay.switchdemo.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
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

/**
 * FIXED #12, #13, #21, #22, #46:
 *  - Greyed-out empty state with "No signal" badge when there is no live data.
 *  - All overlays positioned via clamped fractions of [BoxWithConstraints].
 *  - WARN renders a flashing red border + label.
 *  - Horn renders a tinted speaker icon at the handlebar centre.
 *  - The heavy glow PNGs are no longer used: Canvas radial gradients replace them.
 */
@Composable
fun MotorcycleGraphic(
    switchState: SwitchState,
    blinkRateMs: Long = 500L,
    showIndicatorLabels: Boolean = true,
    usePlainLabels: Boolean = true,
    hasLiveData: Boolean = true,
    modifier: Modifier = Modifier
) {
    val isUpper  = switchState.headlight == HeadlightMode.UPPER || switchState.pass
    val isDipper = switchState.headlight == HeadlightMode.DIPPER

    var blinkOn by remember { mutableStateOf(false) }
    LaunchedEffect(switchState.indicator, blinkRateMs) {
        if (switchState.indicator != IndicatorMode.NONE) {
            while (true) { blinkOn = !blinkOn; delay(blinkRateMs) }
        } else {
            blinkOn = false
        }
    }

    val infinite = rememberInfiniteTransition(label = "warn")
    val warnPulse by infinite.animateFloat(
        initialValue  = 0.4f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse),
        label = "warnPulse"
    )

    val highLeftAlpha  by animateFloatAsState(if (isUpper)  1f else 0f, tween(150), label = "hl")
    val highRightAlpha by animateFloatAsState(if (isUpper)  1f else 0f, tween(150), label = "hr")
    val lowLeftAlpha   by animateFloatAsState(if (isDipper) 1f else 0f, tween(150), label = "ll")
    val lowRightAlpha  by animateFloatAsState(if (isDipper) 1f else 0f, tween(150), label = "lr")
    val indLeftAlpha   by animateFloatAsState(
        if (switchState.indicator == IndicatorMode.LEFT  && blinkOn) 1f else 0f, tween(120), label = "il"
    )
    val indRightAlpha  by animateFloatAsState(
        if (switchState.indicator == IndicatorMode.RIGHT && blinkOn) 1f else 0f, tween(120), label = "ir"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (switchState.warn)
                    Modifier.border(
                        width = 3.dp,
                        color = Color(0xFFFF2D7A).copy(alpha = warnPulse),
                        shape = RoundedCornerShape(12.dp)
                    )
                else Modifier
            ),
        contentAlignment = Alignment.TopStart
    ) {
        val basePainter   = painterResource(R.drawable.moto_base)
        val intrinsicSize = basePainter.intrinsicSize
        val aspectRatio   = if (intrinsicSize.height > 0f) intrinsicSize.width / intrinsicSize.height else 1.6f
        val renderedW: Dp = maxWidth
        val renderedH: Dp = maxWidth / aspectRatio

        Box(modifier = Modifier.fillMaxWidth().height(renderedH), contentAlignment = Alignment.TopStart) {
            val grey = !hasLiveData
            Image(
                painter            = basePainter,
                contentDescription = "Motorcycle graphic showing the current handlebar switch state",
                modifier           = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = if (grey) 0.45f else 1f },
                contentScale       = ContentScale.Fit,
                colorFilter        = if (grey) ColorFilter.tint(Color(0xFF8A93A6), BlendMode.Modulate) else null
            )

            CanvasGlow(0.343f, 0.444f, 0.30f, Color(0xFFE0F8FF), Color(0xFF00F0FF), highLeftAlpha,  renderedW, renderedH)
            CanvasGlow(0.649f, 0.444f, 0.30f, Color(0xFFE0F8FF), Color(0xFF00F0FF), highRightAlpha, renderedW, renderedH)
            CanvasGlow(0.337f, 0.441f, 0.26f, Color(0xFFFFFCF0), Color(0xFFFFD600), lowLeftAlpha,   renderedW, renderedH)
            CanvasGlow(0.658f, 0.441f, 0.26f, Color(0xFFFFFCF0), Color(0xFFFFD600), lowRightAlpha,  renderedW, renderedH)
            CanvasGlow(0.157f, 0.483f, 0.22f, Color(0xFFFFE6A8), Color(0xFFFFAA00), indLeftAlpha,   renderedW, renderedH)
            CanvasGlow(0.835f, 0.481f, 0.22f, Color(0xFFFFE6A8), Color(0xFFFFAA00), indRightAlpha,  renderedW, renderedH)

            // FIXED #22: horn icon at handlebar centre
            if (switchState.horn) {
                val hornSize = 36.dp
                Box(
                    modifier = Modifier
                        .absoluteOffset(
                            x = (renderedW * 0.5f - hornSize / 2).coerceAtLeast(0.dp),
                            y = (renderedH * 0.55f).coerceAtLeast(0.dp)
                        )
                        .size(hornSize)
                        .clip(CircleShape)
                        .background(Color(0xFFFF2D7A).copy(alpha = 0.85f * warnPulse + 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector       = Icons.Filled.VolumeUp,
                        contentDescription = "Horn active",
                        tint              = Color.White,
                        modifier          = Modifier.size(22.dp)
                    )
                }
            }

            // FIXED #21: WARN ribbon
            if (switchState.warn) {
                Row(
                    modifier              = Modifier
                        .absoluteOffset(
                            x = (renderedW * 0.5f - 70.dp).coerceAtLeast(0.dp),
                            y = (renderedH * 0.20f).coerceAtLeast(0.dp)
                        )
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFF2D7A).copy(alpha = warnPulse))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.WarningAmber, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Text("Hardware warning", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            if (showIndicatorLabels && !switchState.warn && hasLiveData) {
                val beamLabel = when {
                    isUpper && usePlainLabels -> "High beam"
                    isUpper                   -> "HIGH BEAM"
                    isDipper && usePlainLabels -> "Low beam"
                    isDipper                  -> "LOW BEAM"
                    else -> null
                }
                if (beamLabel != null) {
                    OverlayLabel(text = beamLabel,
                        x = (renderedW * 0.5f - 36.dp).coerceAtLeast(0.dp),
                        y = (renderedH * 0.30f).coerceAtLeast(0.dp),
                        highlight = false, alpha = 1f)
                }
                if (switchState.indicator == IndicatorMode.LEFT) {
                    OverlayLabel(text = if (usePlainLabels) "◄ Left" else "◄ LEFT",
                        x = (renderedW * 0.157f - 26.dp).coerceAtLeast(0.dp),
                        y = (renderedH * 0.483f - 30.dp).coerceAtLeast(0.dp),
                        highlight = true, alpha = indLeftAlpha)
                }
                if (switchState.indicator == IndicatorMode.RIGHT) {
                    OverlayLabel(text = if (usePlainLabels) "Right ►" else "RIGHT ►",
                        x = (renderedW * 0.835f - 22.dp).coerceAtLeast(0.dp),
                        y = (renderedH * 0.481f - 30.dp).coerceAtLeast(0.dp),
                        highlight = true, alpha = indRightAlpha)
                }
            }

            // FIXED #12: explicit "No signal" overlay
            if (!hasLiveData) {
                Row(
                    modifier              = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xCC0B0F14))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.SignalWifiOff, null, tint = Color(0xFF7A8599), modifier = Modifier.size(14.dp))
                    Text("No signal", color = Color(0xFFBFC8D6), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun BoxScope.CanvasGlow(
    cx: Float, cy: Float, fraction: Float,
    colorInner: Color, colorOuter: Color, alpha: Float,
    renderedW: Dp, renderedH: Dp
) {
    if (alpha <= 0.01f) return
    val glowSize = renderedW * fraction
    Canvas(
        modifier = Modifier
            .absoluteOffset(
                x = (renderedW * cx - glowSize / 2).coerceAtLeast(0.dp),
                y = (renderedH * cy - glowSize / 2).coerceAtLeast(0.dp)
            )
            .size(glowSize)
            .alpha(alpha)
    ) {
        val r = size.minDimension / 2f
        val centre = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            brush  = Brush.radialGradient(
                colors = listOf(colorOuter.copy(alpha = 0.5f), Color.Transparent),
                center = centre, radius = r
            ),
            radius = r, center = centre
        )
        drawCircle(
            brush  = Brush.radialGradient(
                colors = listOf(colorInner, colorOuter.copy(alpha = 0f)),
                center = centre, radius = r * 0.45f
            ),
            radius = r * 0.45f, center = centre
        )
    }
}

@Composable
private fun BoxScope.OverlayLabel(
    text: String,
    x: Dp, y: Dp,
    highlight: Boolean, alpha: Float
) {
    if (alpha <= 0.01f) return
    Text(
        text       = text,
        fontSize   = 10.sp,
        fontWeight = FontWeight.Bold,
        color      = if (highlight) Color(0xFFFFD600) else Color(0xFF00F0FF),
        modifier   = Modifier
            .absoluteOffset(x = x, y = y)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xCC000000))
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .alpha(alpha)
    )
}
