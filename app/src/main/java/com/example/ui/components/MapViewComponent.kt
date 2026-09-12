package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricRickshaw
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.RideStatus
import com.example.ui.theme.*

@Composable
fun MapViewComponent(
    pickupName: String,
    dropoffName: String,
    status: RideStatus,
    driverName: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFE5E7EB))
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
    ) {
        // Map Canvas with Roads & Grid
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Background terrain & river (Brahmaputra aesthetic)
            drawRect(color = Color(0xFFF1F5F9))

            // River curve
            drawLine(
                color = Color(0xFFBAE6FD),
                start = Offset(0f, height * 0.25f),
                end = Offset(width, height * 0.15f),
                strokeWidth = 36f
            )

            // Roads
            drawLine(
                color = Color.White,
                start = Offset(0f, height * 0.55f),
                end = Offset(width, height * 0.55f),
                strokeWidth = 20f
            )
            drawLine(
                color = Color.White,
                start = Offset(width * 0.4f, 0f),
                end = Offset(width * 0.4f, height),
                strokeWidth = 16f
            )
            drawLine(
                color = Color.White,
                start = Offset(width * 0.75f, 0f),
                end = Offset(width * 0.75f, height),
                strokeWidth = 14f
            )

            // Route trajectory line
            val pickupOffset = Offset(width * 0.25f, height * 0.65f)
            val dropoffOffset = Offset(width * 0.8f, height * 0.45f)

            drawLine(
                color = BrandGreen,
                start = pickupOffset,
                end = dropoffOffset,
                strokeWidth = 6f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
            )

            // Draw circle around pickup
            drawCircle(
                color = BrandGreen.copy(alpha = 0.25f),
                radius = 28f,
                center = pickupOffset
            )
            drawCircle(
                color = BrandGreen,
                radius = 10f,
                center = pickupOffset
            )

            // Draw circle around dropoff
            drawCircle(
                color = RedCancel.copy(alpha = 0.25f),
                radius = 28f,
                center = dropoffOffset
            )
            drawCircle(
                color = RedCancel,
                radius = 10f,
                center = dropoffOffset
            )
        }

        // Top Banner with City & Tracking Status
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.White.copy(alpha = 0.92f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = null,
                    tint = BrandGreenDark,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "E-Ride 3 GPS Route Live Feed",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = CharcoalDark
                )
            }
        }

        // Locations Overlay Box
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color.White,
            shadowElevation = 4.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(14.dp))
                        Text(text = "Pickup: $pickupName", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = RedCancel, modifier = Modifier.size(14.dp))
                        Text(text = "Drop: $dropoffName", fontSize = 12.sp, fontWeight = FontWeight.Normal, maxLines = 1, color = SlateGray)
                    }
                }
                if (driverName != null && status in listOf(RideStatus.DRIVER_ASSIGNED, RideStatus.DRIVER_ARRIVED, RideStatus.IN_PROGRESS)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = BrandGreenLight
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.ElectricRickshaw, contentDescription = null, tint = BrandGreenDark, modifier = Modifier.size(14.dp))
                            Text(text = "En Route", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandGreenDark)
                        }
                    }
                }
            }
        }
    }
}
