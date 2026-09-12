package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppLogo
import com.example.ui.theme.BrandGreen
import com.example.ui.theme.BrandGreenDark
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1600)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandGreen),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AppLogo(size = 84.dp, textColor = Color.White)
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Eco-Friendly Electric Mobility for Assam",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Guwahati • Dibrugarh • Jorhat • Silchar",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 12.sp
            )
        }
    }
}
