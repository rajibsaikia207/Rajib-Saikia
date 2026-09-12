package com.example.ui.screens.driver

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrandGreen
import com.example.ui.theme.BrandGreenDark
import com.example.ui.theme.SlateGray

enum class DriverTab(val title: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Home),
    WALLET("Wallet", Icons.Default.AccountBalanceWallet),
    EARNINGS("Earnings", Icons.Default.TrendingUp),
    COMMISSION("Commission", Icons.Default.ReceiptLong),
    PROFILE("Profile", Icons.Default.Person)
}

@Composable
fun DriverBottomNavigationBar(
    selectedTab: DriverTab,
    onTabSelected: (DriverTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        DriverTab.entries.forEach { tab ->
            val isSelected = selectedTab == tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = tab.title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = BrandGreenDark,
                    selectedTextColor = BrandGreenDark,
                    indicatorColor = BrandGreen.copy(alpha = 0.15f),
                    unselectedIconColor = SlateGray,
                    unselectedTextColor = SlateGray
                )
            )
        }
    }
}
