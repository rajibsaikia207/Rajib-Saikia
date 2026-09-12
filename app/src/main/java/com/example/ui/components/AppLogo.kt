package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.BrandGreen
import com.example.ui.theme.BrandGreenDark
import com.example.ui.theme.BrandGreenLight
import com.example.ui.theme.ElectricAmber

/**
 * Professional, modern and original E-Ride 3 Logo Icon badge based on a clearly recognizable
 * Indian Electric Rickshaw (Toto) with 3-wheel vehicle geometry, green curved roof,
 * front windshield, passenger cabin with safety bars, wheels, and electric energy bolt.
 */
@Composable
fun ERickshawLogoIcon(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    containerColor: Color = Color.White,
    withBorder: Boolean = true
) {
    val cornerRadius = size * 0.22f
    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(cornerRadius),
        color = containerColor,
        shadowElevation = 2.dp,
        border = if (withBorder) BorderStroke(1.dp, BrandGreen.copy(alpha = 0.25f)) else null
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(size * 0.12f),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_eride_rickshaw),
                contentDescription = "E-Ride 3 Logo",
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    showText: Boolean = true,
    showTagline: Boolean = false,
    textColor: Color = MaterialTheme.colorScheme.onBackground
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ERickshawLogoIcon(size = size)

        if (showText) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "E-RIDE",
                        fontWeight = FontWeight.Black,
                        fontSize = (size.value * 0.38f).sp,
                        color = textColor,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ElectricAmber
                    ) {
                        Text(
                            text = "3",
                            fontWeight = FontWeight.Black,
                            fontSize = (size.value * 0.35f).sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (showTagline) {
                    Text(
                        text = "Your Local E-Rickshaw Ride",
                        fontSize = (size.value * 0.18f).sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}
