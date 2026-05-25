package org.ssay.switchdemo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ssay.switchdemo.R
import org.ssay.switchdemo.data.Screen

/**
 * FIXED #14, #33, #34, #35, #53:
 *  - Iterates [Screen.all] (sealed-class) instead of magic strings.
 *  - All icons are vector drawables (consistent visual style).
 *  - Each tab is at least 56dp tall and uses the Material `selectable` Role,
 *    which announces the right TalkBack state.
 */
@Composable
fun BottomNavBar(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color       = androidx.compose.ui.graphics.Color(0x33FFFFFF),
                    start       = Offset(0f, 0f),
                    end         = Offset(size.width, 0f),
                    strokeWidth = 1.5f
                )
            }
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Screen.all.forEach { screen ->
            NavBarItem(
                screen = screen,
                isActive = screen == currentScreen,
                onClick  = { onNavigate(screen) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NavBarItem(
    screen: Screen,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (label, painter) = when (screen) {
        is Screen.Dashboard -> "Dashboard" to painterResource(R.drawable.ic_dashboard)
        is Screen.Settings  -> "Settings"  to painterResource(R.drawable.ic_settings)
        is Screen.About     -> "About"     to painterResource(R.drawable.ic_info_outline)
    }
    val color =
        if (isActive) MaterialTheme.colorScheme.secondary
        else          MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    Column(
        modifier = modifier
            .heightIn(min = 56.dp)
            .selectable(
                selected = isActive,
                role     = Role.Tab,
                onClick  = onClick
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter            = painter,
            contentDescription = label,
            tint               = color,
            modifier           = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text       = label,
            fontSize   = 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color      = color,
            textAlign  = TextAlign.Center
        )
    }
}
