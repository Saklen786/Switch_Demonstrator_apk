package org.ssay.switchdemo.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ssay.switchdemo.ui.theme.NeonPink

/**
 * FIXED #20: the previous "● DEMO" 9sp pink text was almost invisible during a
 * presentation. This banner is full-width, animated and unmistakable.
 *
 * FIXED #26: tappable directly from the dashboard so a presenter can stop
 * the demo without leaving the main view.
 */
@Composable
fun DemoBanner(
    visible: Boolean,
    onStopDemo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "demoBanner")
    val pulse by infinite.animateFloat(
        initialValue  = 0.65f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = EaseInOut), RepeatMode.Reverse),
        label = "pulse"
    )

    AnimatedVisibility(
        visible = visible,
        enter   = expandVertically() + fadeIn(),
        exit    = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(0.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(NeonPink.copy(alpha = pulse), Color(0xFFFF6BAB).copy(alpha = pulse))
                    )
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(
                    imageVector       = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint              = Color.White
                )
                Text(
                    text       = "Demo mode — showing simulated data",
                    color      = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 13.sp
                )
            }
            FilledTonalButton(
                onClick = onStopDemo,
                colors  = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color.White.copy(alpha = 0.18f),
                    contentColor   = Color.White
                ),
                shape   = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Filled.Stop, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Stop", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
