package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.models.RideStatus
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderAppBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    showLogo: Boolean = true,
    actions: @Composable (RowScope.() -> Unit)? = null
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (showLogo) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        modifier = Modifier.size(32.dp),
                        shadowElevation = 1.dp
                    ) {
                        Box(
                            modifier = Modifier.padding(3.dp),
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
                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }
        },
        actions = {
            actions?.invoke(this)
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BrandGreen,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        )
    )
}

@Composable
fun RideStatusBadge(status: RideStatus) {
    val (bgColor, textColor, text) = when (status) {
        RideStatus.REQUESTED -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), "Searching Driver")
        RideStatus.DRIVER_ASSIGNED -> Triple(Color(0xFFDBEAFE), Color(0xFF2563EB), "Driver Found")
        RideStatus.DRIVER_ARRIVED -> Triple(Color(0xFFEDE9FE), Color(0xFF7C3AED), "Driver Arrived")
        RideStatus.IN_PROGRESS -> Triple(Color(0xFFDCFCE7), Color(0xFF16A34A), "Trip In Progress")
        RideStatus.COMPLETED -> Triple(Color(0xFFD1FAE5), Color(0xFF059669), "Trip Completed")
        RideStatus.CANCELLED -> Triple(Color(0xFFFEE2E2), Color(0xFFDC2626), "Cancelled")
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = textColor
            )
        }
    }
}

