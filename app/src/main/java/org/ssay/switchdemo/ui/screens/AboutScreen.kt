package org.ssay.switchdemo.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ssay.switchdemo.R
import org.ssay.switchdemo.ui.theme.*

@Composable
fun AboutScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        Text(
            text = "About",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = NeonPink
        )

        // Info card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurface)
                .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.elmos_logo),
                contentDescription = "Elmos Logo",
                modifier = Modifier.height(60.dp).width(100.dp),
                contentScale = ContentScale.Fit
            )

            Text(
                text = "E521.39 Switch Demonstrator",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = WhiteText,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Version 1.0.0",
                fontSize = 13.sp,
                color = NeonCyan,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "This application demonstrates the capabilities of the E521.39 IC for two-wheeler switch monitoring via Bluetooth Low Energy.",
                fontSize = 12.sp,
                color = GreyText,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Developed by SSAY",
                fontSize = 13.sp,
                color = LightGreyText,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}
