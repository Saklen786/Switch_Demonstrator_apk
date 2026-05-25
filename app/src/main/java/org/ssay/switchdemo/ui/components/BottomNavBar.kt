package org.ssay.switchdemo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ssay.switchdemo.R
import org.ssay.switchdemo.ui.theme.*

private data class NavItemData(
    val key: String,
    val label: String,
    val iconRes: Int?,
    val isCustomIcon: Boolean = false
)

private val navItems = listOf(
    NavItemData("dashboard", "Dashboard", R.drawable.icon_dashboard),
    NavItemData("settings", "Settings", R.drawable.icon_setting),
    NavItemData("about", "About", null, isCustomIcon = true)
)

@Composable
fun BottomNavBar(
    currentScreen: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = CardBorder,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.5f
                )
            }
            .background(DarkNavBar)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        navItems.forEach { item ->
            val isActive = currentScreen == item.key
            val color = if (isActive) NeonPink else GreyText

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigate(item.key) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.isCustomIcon) {
                        Canvas(modifier = Modifier.size(24.dp)) {
                            drawCircle(
                                color = color,
                                radius = 11.dp.toPx(),
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                        }
                        Text(
                            text = "i",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = color,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Image(
                            painter = painterResource(item.iconRes!!),
                            contentDescription = item.label,
                            modifier = Modifier.size(22.dp),
                            colorFilter = ColorFilter.tint(color)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.label,
                    fontSize = 10.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color = color,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
