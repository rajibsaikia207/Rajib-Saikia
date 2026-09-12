package com.example.ui.screens.admin

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.*
import com.example.data.repository.RideRepository
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminNotificationsTab(
    notifications: List<AdminNotification>,
    onNavigate: (AdminNavSection) -> Unit
) {
    val unreadCount = notifications.count { !it.isRead }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Admin Operational Alerts & Notifications", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CharcoalDark)
                    Text("$unreadCount Unread alerts requiring operational action", fontSize = 12.sp, color = SlateGray)
                }
                if (unreadCount > 0) {
                    TextButton(onClick = { RideRepository.markAllNotificationsAsRead() }) {
                        Text("Mark all read", fontSize = 12.sp, color = BrandGreenDark)
                    }
                }
            }
        }

        if (notifications.isEmpty()) {
            item {
                Text("No notifications at this time.", color = SlateGray, fontSize = 13.sp)
            }
        } else {
            items(notifications) { notif ->
                val timeStr = SimpleDateFormat("dd MMM, HH:mm", Locale.ENGLISH).format(Date(notif.timestamp))
                val icon = when (notif.type) {
                    AdminNotificationType.DRIVER_APPROVAL_REQUEST -> Icons.Default.HowToReg
                    AdminNotificationType.WITHDRAWAL_REQUEST -> Icons.Default.Paid
                    AdminNotificationType.PENDING_SETTLEMENT -> Icons.Default.ReceiptLong
                    AdminNotificationType.NEW_COMPLAINT -> Icons.Default.SupportAgent
                    else -> Icons.Default.Notifications
                }
                val iconColor = if (!notif.isRead) BrandGreenDark else SlateGray

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = if (!notif.isRead) BrandGreenLight.copy(alpha = 0.5f) else Color.White),
                    border = BorderStroke(1.dp, if (!notif.isRead) BrandGreen.copy(alpha = 0.5f) else CardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = iconColor.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(notif.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CharcoalDark)
                                Text(timeStr, fontSize = 10.sp, color = SlateGray)
                            }
                            Text(notif.message, fontSize = 12.sp, color = CharcoalDark)

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (notif.targetSection != null) {
                                    Button(
                                        onClick = {
                                            RideRepository.markNotificationAsRead(notif.id)
                                            when (notif.targetSection) {
                                                "DRIVERS" -> onNavigate(AdminNavSection.DRIVERS)
                                                "WITHDRAW" -> onNavigate(AdminNavSection.WITHDRAW)
                                                "SETTLEMENTS" -> onNavigate(AdminNavSection.SETTLEMENTS)
                                                "COMPLAINT" -> onNavigate(AdminNavSection.COMPLAINT)
                                                "RIDES" -> onNavigate(AdminNavSection.RIDES)
                                                else -> {}
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreenDark),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Open Action", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Spacer(modifier = Modifier.width(1.dp))
                                }

                                if (!notif.isRead) {
                                    TextButton(onClick = { RideRepository.markNotificationAsRead(notif.id) }) {
                                        Text("Dismiss", fontSize = 11.sp, color = SlateGray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
