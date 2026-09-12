package com.example.ui.screens.admin

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * 15 EXACT Navigation Items in requested order:
 * Dashboard | Passengers | Drivers | Rides | Payments | Commission | Settlements | Reports | Notifications | Settings | Withdraw | Fare Setting | Service Area | Complaint | Log Out
 */
enum class AdminNavSection(val title: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    PASSENGERS("Passengers", Icons.Default.People),
    DRIVERS("Drivers", Icons.Default.ElectricRickshaw),
    RIDES("Rides", Icons.Default.ListAlt),
    PAYMENTS("Payments", Icons.Default.CreditCard),
    COMMISSION("Commission", Icons.Default.AccountBalanceWallet),
    SETTLEMENTS("Settlements", Icons.Default.ReceiptLong),
    REPORTS("Reports", Icons.Default.Assessment),
    NOTIFICATIONS("Notifications", Icons.Default.Notifications),
    SETTINGS("Settings", Icons.Default.Settings),
    WITHDRAW("Withdraw", Icons.Default.AccountBalance),
    FARE_SETTING("Fare Setting", Icons.Default.PriceChange),
    SERVICE_AREA("Service Area", Icons.Default.Map),
    COMPLAINT("Complaint", Icons.Default.SupportAgent),
    LOG_OUT("Log Out", Icons.AutoMirrored.Filled.Logout)
}

@Composable
fun AdminHorizontalNavBar(
    selectedSection: AdminNavSection,
    onSelectSection: (AdminNavSection) -> Unit,
    pendingDriversCount: Int = 0,
    pendingWithdrawalsCount: Int = 0,
    pendingSettlementsCount: Int = 0,
    unreadNotificationsCount: Int = 0,
    openComplaintsCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 3.dp,
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AdminNavSection.values().forEach { section ->
                val isSelected = section == selectedSection
                val badgeCount = when (section) {
                    AdminNavSection.DRIVERS -> pendingDriversCount
                    AdminNavSection.WITHDRAW -> pendingWithdrawalsCount
                    AdminNavSection.SETTLEMENTS -> pendingSettlementsCount
                    AdminNavSection.NOTIFICATIONS -> unreadNotificationsCount
                    AdminNavSection.COMPLAINT -> openComplaintsCount
                    else -> 0
                }

                Surface(
                    onClick = { onSelectSection(section) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) BrandGreenDark else if (section == AdminNavSection.LOG_OUT) RedCancel.copy(alpha = 0.08f) else BackgroundLight,
                    border = BorderStroke(
                        width = if (isSelected) 0.dp else 1.dp,
                        color = if (isSelected) Color.Transparent else if (section == AdminNavSection.LOG_OUT) RedCancel.copy(alpha = 0.3f) else CardBorder
                    ),
                    modifier = Modifier.height(38.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = section.icon,
                            contentDescription = section.title,
                            tint = if (isSelected) Color.White else if (section == AdminNavSection.LOG_OUT) RedCancel else CharcoalDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = section.title,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else if (section == AdminNavSection.LOG_OUT) RedCancel else CharcoalDark
                        )

                        if (badgeCount > 0) {
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) Color.White else RedCancel,
                                modifier = Modifier.size(18.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) BrandGreenDark else Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
