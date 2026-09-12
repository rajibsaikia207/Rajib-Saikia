package com.example.ui.screens.driver

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.models.Driver
import com.example.data.models.DriverStatus
import com.example.data.models.DriverWallet
import com.example.data.repository.RideRepository
import com.example.ui.theme.*

@Composable
fun DriverDashboardTab(
    driver: Driver,
    wallet: DriverWallet,
    onNavigateToTab: (DriverTab) -> Unit,
    onOpenAuth: () -> Unit,
    onWithdrawClick: () -> Unit,
    onBackToPortal: () -> Unit
) {
    val allRides by RideRepository.ridesList.collectAsState()
    val summary = remember(allRides, driver) {
        RideRepository.getDriverEarningsSummary(driver.id)
    }
    var onlineErrorMsg by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. HEADER SECTION
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BrandGreenDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            modifier = Modifier.size(48.dp),
                            shadowElevation = 2.dp
                        ) {
                            Box(
                                modifier = Modifier.padding(5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_eride_rickshaw),
                                    contentDescription = "E-Ride 3 Logo",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "E-Ride 3",
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Your Local E-Rickshaw Ride",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }

                    // Account / Approval Status Badge
                    val statusColor = when (driver.status) {
                        DriverStatus.APPROVED -> Color(0xFF10B981)
                        DriverStatus.PENDING_APPROVAL -> ElectricAmber
                        DriverStatus.REJECTED -> RedCancel
                        DriverStatus.SUSPENDED -> Color(0xFFF97316)
                        DriverStatus.DELETED -> RedCancel
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Text(
                                text = when (driver.status) {
                                    DriverStatus.APPROVED -> "Approved"
                                    DriverStatus.PENDING_APPROVAL -> "Pending Approval"
                                    DriverStatus.REJECTED -> "Rejected"
                                    DriverStatus.SUSPENDED -> "Suspended"
                                    DriverStatus.DELETED -> "Deactivated"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                    }
                }
            }
        }

        // 2. PENDING APPROVAL CARD (Conditional)
        if (driver.status == DriverStatus.PENDING_APPROVAL) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ElectricAmberLight),
                    border = BorderStroke(1.5.dp, ElectricAmber)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = ElectricAmber,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Registration Under Review",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = CharcoalDark
                            )
                        }
                        Text(
                            text = "Your driver profile and vehicle RC (${driver.rickshawRegNo}) are undergoing verification by the E-Ride 3 Admin Team. You will be notified once approved.",
                            fontSize = 12.sp,
                            color = SlateGray,
                            lineHeight = 16.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onOpenAuth,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, ElectricAmber),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricAmber)
                            ) {
                                Text("View Details", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            // Quick Demo Approval helper
                            Button(
                                onClick = {
                                    RideRepository.approveDriver(driver.id)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandGreenDark),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Approve (Demo)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 3. ONLINE / OFFLINE TOGGLE CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (driver.isOnline && driver.status == DriverStatus.APPROVED) Color.White else Color(0xFFFEF2F2)
                ),
                border = BorderStroke(
                    1.5.dp,
                    if (driver.isOnline && driver.status == DriverStatus.APPROVED) BrandGreen else RedCancel.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (driver.isOnline && driver.status == DriverStatus.APPROVED) BrandGreenLight else RedCancelLight
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (driver.isOnline && driver.status == DriverStatus.APPROVED) Icons.Default.ElectricRickshaw else Icons.Default.PowerSettingsNew,
                                    contentDescription = null,
                                    tint = if (driver.isOnline && driver.status == DriverStatus.APPROVED) BrandGreenDark else RedCancel
                                )
                            }
                            Column {
                                Text(
                                    text = if (driver.isOnline && driver.status == DriverStatus.APPROVED) "ONLINE — Ready for Rides" else "OFFLINE — Not Accepting Rides",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (driver.isOnline && driver.status == DriverStatus.APPROVED) BrandGreenDark else RedCancel
                                )
                                Text(
                                    text = if (driver.isOnline && driver.status == DriverStatus.APPROVED)
                                        "Active in ${driver.serviceCity} (30 km operating zone)"
                                    else
                                        "Toggle ON to start receiving ride requests",
                                    fontSize = 12.sp,
                                    color = SlateGray
                                )
                            }
                        }

                        Switch(
                            checked = driver.isOnline && driver.status == DriverStatus.APPROVED,
                            onCheckedChange = {
                                if (driver.status != DriverStatus.APPROVED) {
                                    onlineErrorMsg = "Account verification is pending. Admin approval is required before going online."
                                    return@Switch
                                }
                                val (success, reason) = RideRepository.toggleDriverOnline(driver.id)
                                if (!success) {
                                    onlineErrorMsg = reason
                                } else {
                                    onlineErrorMsg = null
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = BrandGreen,
                                uncheckedThumbColor = SlateGray,
                                uncheckedTrackColor = SlateLight
                            )
                        )
                    }

                    if (onlineErrorMsg != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = RedCancelLight,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = onlineErrorMsg!!,
                                color = RedCancel,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. DRIVER WALLET CARD (Dynamic & Real)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = BrandGreenDark,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Driver Wallet (Net Take-Home)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = CharcoalDark
                            )
                        }
                        TextButton(
                            onClick = { onNavigateToTab(DriverTab.WALLET) },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Full Wallet ➔", fontSize = 12.sp, color = BrandGreenDark, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Available Balance", fontSize = 11.sp, color = SlateGray)
                            Text(
                                "₹${wallet.availableBalance.toInt()}",
                                fontWeight = FontWeight.Black,
                                fontSize = 28.sp,
                                color = BrandGreenDark
                            )
                            Text("Available Balance (After 10% Commission)", fontSize = 10.sp, color = SlateGray)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onWithdrawClick,
                                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Withdraw", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = { onNavigateToTab(DriverTab.WALLET) },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, CardBorder),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text("History", fontSize = 12.sp, color = CharcoalDark)
                            }
                        }
                    }

                    HorizontalDivider(color = LightSlate)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Pending Earnings", fontSize = 11.sp, color = SlateGray)
                            Text("₹${wallet.pendingBalance.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ElectricAmber)
                        }
                        Column {
                            Text("Total Withdrawn", fontSize = 11.sp, color = SlateGray)
                            Text("₹${wallet.totalWithdrawn.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandGreenDark)
                        }
                        Column {
                            Text("Commission Due", fontSize = 11.sp, color = SlateGray)
                            Text(
                                "₹${wallet.commissionDue.toInt()}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (wallet.commissionDue > 0) RedCancel else BrandGreenDark
                            )
                        }
                    }
                }
            }
        }

        // 5. CASH PAYMENT & COLLECTION CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = BrandGreenDark,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Cash Payment & Commission",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = BrandGreenDark
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = BrandGreenLight
                        ) {
                            Text(
                                text = "10% Platform Fee",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandGreenDark
                            )
                        }
                    }

                    Text(
                        text = "For every cash ride: You collect 100% cash from the passenger. 10% platform commission is tracked and 90% is your net earnings.",
                        fontSize = 12.sp,
                        color = CharcoalDark,
                        lineHeight = 16.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, CardBorder)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Cash in Hand", fontSize = 11.sp, color = SlateGray)
                                Text("₹${summary.cashCollected.toInt()}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = CharcoalDark)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, CardBorder)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Commission Owed", fontSize = 11.sp, color = SlateGray)
                                Text("₹${wallet.commissionDue.toInt()}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = RedCancel)
                            }
                        }
                    }

                    Button(
                        onClick = { onNavigateToTab(DriverTab.COMMISSION) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreenDark),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("View Commission & Settle", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // 6. PERFORMANCE & EARNINGS SUMMARY (2x2 Grid)
        item {
            Text(
                text = "Performance & Earnings",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = CharcoalDark
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Today's Earnings
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToTab(DriverTab.EARNINGS) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Today, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(16.dp))
                            Text("Today", fontSize = 12.sp, color = SlateGray, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("₹${summary.todayEarnings.toInt()}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = CharcoalDark)
                        Text("Net Earning", fontSize = 10.sp, color = SlateGray)
                    }
                }

                // Total Completed Trips
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToTab(DriverTab.EARNINGS) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = ElectricAmber, modifier = Modifier.size(16.dp))
                            Text("Total Trips", fontSize = 12.sp, color = SlateGray, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("${wallet.totalRides}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = CharcoalDark)
                        Text("Completed rides", fontSize = 10.sp, color = SlateGray)
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Driver Rating
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToTab(DriverTab.PROFILE) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = ElectricAmber, modifier = Modifier.size(16.dp))
                            Text("Rating", fontSize = 12.sp, color = SlateGray, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("⭐ ${driver.rating}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = CharcoalDark)
                        Text("Passenger reviews", fontSize = 10.sp, color = SlateGray)
                    }
                }

                // Net Driver Earnings
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToTab(DriverTab.EARNINGS) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = BrandGreenDark, modifier = Modifier.size(16.dp))
                            Text("Net (90%)", fontSize = 12.sp, color = SlateGray, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("₹${summary.netDriverEarnings.toInt()}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = BrandGreenDark)
                        Text("After commission", fontSize = 10.sp, color = SlateGray)
                    }
                }
            }
        }

        // 7. DRIVER & VEHICLE CREDENTIALS CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Driver & Vehicle Credentials", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CharcoalDark)
                        TextButton(onClick = { onNavigateToTab(DriverTab.PROFILE) }, contentPadding = PaddingValues(0.dp)) {
                            Text("View Profile", fontSize = 12.sp, color = BrandGreenDark, fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Driver Partner", fontSize = 12.sp, color = SlateGray)
                        Text(text = driver.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CharcoalDark)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Registration No", fontSize = 12.sp, color = SlateGray)
                        Text(text = driver.rickshawRegNo, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandGreenDark)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "E-Rickshaw Model", fontSize = 12.sp, color = SlateGray)
                        Text(text = driver.vehicleModel, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = CharcoalDark)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Service Area (30 km)", fontSize = 12.sp, color = SlateGray)
                        Text(text = driver.serviceCity, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CharcoalDark)
                    }
                }
            }
        }

        // 8. GPS / SERVICE NOTICE
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SlateLight.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Sensors, contentDescription = null, tint = BrandGreenDark, modifier = Modifier.size(20.dp))
                    Text(
                        text = if (driver.isOnline && driver.status == DriverStatus.APPROVED)
                            "GPS active in ${driver.serviceCity} (30 km operating area). Ready to receive incoming bookings..."
                        else
                            "Turn Online switch ON to receive incoming passenger requests.",
                        fontSize = 12.sp,
                        color = CharcoalDark,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
