package com.example.ui.screens.admin

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminDashboardTab(
    rides: List<RideRecord>,
    drivers: List<Driver>,
    passengers: List<Passenger>,
    withdrawals: List<WithdrawalRequest>,
    settlements: List<DriverSettlement>,
    driverWallets: Map<String, DriverWallet>,
    currentSession: AdminSession,
    onNavigate: (AdminNavSection) -> Unit
) {
    val istZone = TimeZone.getTimeZone("Asia/Kolkata")
    val calNow = Calendar.getInstance(istZone)
    val todayYear = calNow.get(Calendar.YEAR)
    val todayDay = calNow.get(Calendar.DAY_OF_YEAR)
    val todayMonth = calNow.get(Calendar.MONTH)

    val rideCal = Calendar.getInstance(istZone)

    val completedPaidRides = rides.filter { it.status == RideStatus.COMPLETED }
    val todayCompletedRides = completedPaidRides.filter {
        rideCal.timeInMillis = it.completedAt ?: it.createdAt
        rideCal.get(Calendar.YEAR) == todayYear && rideCal.get(Calendar.DAY_OF_YEAR) == todayDay
    }
    val thisMonthCompletedRides = completedPaidRides.filter {
        rideCal.timeInMillis = it.completedAt ?: it.createdAt
        rideCal.get(Calendar.YEAR) == todayYear && rideCal.get(Calendar.MONTH) == todayMonth
    }

    // Dynamic Financials (Real Calculations)
    val totalBookingValue = rides.sumOf { it.fare }
    val totalRevenue = completedPaidRides.sumOf { it.fare }
    val totalCommissionEarned = completedPaidRides.sumOf { it.commissionAmount }
    val outstandingDue = driverWallets.values.sumOf { it.commissionDue }
    val commissionCollected = maxOf(0.0, totalCommissionEarned - outstandingDue)

    val todayRevenue = todayCompletedRides.sumOf { it.fare }
    val todayCommission = todayCompletedRides.sumOf { it.commissionAmount }

    val thisMonthRevenue = thisMonthCompletedRides.sumOf { it.fare }
    val thisMonthCommission = thisMonthCompletedRides.sumOf { it.commissionAmount }

    val pendingSettlements = settlements.filter { it.status == SettlementStatus.PENDING || it.status == SettlementStatus.VERIFIED }
    val pendingSettlementsAmount = pendingSettlements.sumOf { it.driverPayableAmount }

    val pendingWithdrawals = withdrawals.filter { it.status == WithdrawalStatus.PENDING }
    val pendingWithdrawalsAmount = pendingWithdrawals.sumOf { it.amount }

    // Dynamic Fleet Stats
    val totalDrivers = drivers.size
    val approvedDrivers = drivers.count { it.status == DriverStatus.APPROVED }
    val pendingDrivers = drivers.count { it.status == DriverStatus.PENDING_APPROVAL }
    val activeOnlineDrivers = drivers.count { it.isOnline && it.status == DriverStatus.APPROVED }

    val totalPassengers = passengers.size
    val totalRidesCount = rides.size
    val completedRidesCount = completedPaidRides.size
    val cancelledRidesCount = rides.count { it.status == RideStatus.CANCELLED }
    val ongoingRidesCount = rides.count {
        it.status in listOf(RideStatus.REQUESTED, RideStatus.DRIVER_ASSIGNED, RideStatus.DRIVER_ARRIVED, RideStatus.IN_PROGRESS)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header & Session Details
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BrandGreenDark)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "E-Ride 3 Central Administration Web Panel",
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Assam EV Rickshaw Network Operations • Active Staff: ${currentSession.admin.name}",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "ROLE: ${currentSession.admin.role.name}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

                    // 6-step Financial Flow Indicator
                    Text(
                        text = "FINANCIAL LIFECYCLE PIPELINE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = ElectricAmberLight
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FlowStepChip("1. Booking Value", "₹${totalBookingValue.toInt()}", Color.White)
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
                        FlowStepChip("2. Commission (10%)", "₹${totalCommissionEarned.toInt()}", ElectricAmberLight)
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
                        FlowStepChip("3. Collected", "₹${commissionCollected.toInt()}", Color(0xFF86EFAC))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
                        FlowStepChip("4. Outstanding", "₹${outstandingDue.toInt()}", Color(0xFFFCA5A5))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
                        FlowStepChip("5. Driver Settlement", "₹${pendingSettlementsAmount.toInt()}", Color(0xFFBAE6FD))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
                        FlowStepChip("6. Withdrawable", "₹${driverWallets.values.sumOf { it.availableBalance }.toInt()}", Color.White)
                    }
                }
            }
        }

        // Section Title: Core Financial Metrics
        item {
            Text(
                text = "Financial & Revenue Overview (Live System Data)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = CharcoalDark
            )
        }

        // Top Financial Cards Grid (11 Required Cards)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Row 1: Booking Value & Commission Earned
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FinancialMetricCard(
                        title = "Total Booking Value",
                        amount = "₹${totalBookingValue.toInt()}",
                        subtitle = "Gross value across all rides",
                        icon = Icons.Default.CurrencyRupee,
                        iconTint = BrandGreenDark,
                        containerColor = BrandGreenLight,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AdminNavSection.RIDES) }
                    )
                    FinancialMetricCard(
                        title = "Total Commission Earned",
                        amount = "₹${totalCommissionEarned.toInt()}",
                        subtitle = "10% Platform Fee",
                        icon = Icons.Default.AccountBalanceWallet,
                        iconTint = BrandGreenDark,
                        containerColor = Color.White,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AdminNavSection.COMMISSION) }
                    )
                }

                // Row 2: Collected & Outstanding
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FinancialMetricCard(
                        title = "Commission Collected",
                        amount = "₹${commissionCollected.toInt()}",
                        subtitle = "Direct UPI & Settled Cash",
                        icon = Icons.Default.CheckCircle,
                        iconTint = BrandGreenDark,
                        containerColor = Color.White,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AdminNavSection.COMMISSION) }
                    )
                    FinancialMetricCard(
                        title = "Outstanding Due",
                        amount = "₹${outstandingDue.toInt()}",
                        subtitle = "Pending cash driver liabilities",
                        icon = Icons.Default.Warning,
                        iconTint = RedCancel,
                        containerColor = RedCancel.copy(alpha = 0.06f),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AdminNavSection.COMMISSION) }
                    )
                }

                // Row 3: Today's Revenue & Today's Commission
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FinancialMetricCard(
                        title = "Today's Revenue",
                        amount = "₹${todayRevenue.toInt()}",
                        subtitle = "${todayCompletedRides.size} rides completed today",
                        icon = Icons.Default.Today,
                        iconTint = BlueInfo,
                        containerColor = Color.White,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AdminNavSection.REPORTS) }
                    )
                    FinancialMetricCard(
                        title = "Today's Commission",
                        amount = "₹${todayCommission.toInt()}",
                        subtitle = "Platform share for today",
                        icon = Icons.Default.TrendingUp,
                        iconTint = BrandGreenDark,
                        containerColor = Color.White,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AdminNavSection.COMMISSION) }
                    )
                }

                // Row 4: This Month Revenue & This Month Commission
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FinancialMetricCard(
                        title = "This Month Revenue",
                        amount = "₹${thisMonthRevenue.toInt()}",
                        subtitle = "${thisMonthCompletedRides.size} rides this month",
                        icon = Icons.Default.DateRange,
                        iconTint = BlueInfo,
                        containerColor = Color.White,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AdminNavSection.REPORTS) }
                    )
                    FinancialMetricCard(
                        title = "This Month Commission",
                        amount = "₹${thisMonthCommission.toInt()}",
                        subtitle = "Monthly platform net",
                        icon = Icons.Default.Insights,
                        iconTint = BrandGreenDark,
                        containerColor = Color.White,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AdminNavSection.COMMISSION) }
                    )
                }

                // Row 5: Total Revenue, Pending Driver Settlements & Pending Withdrawals
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FinancialMetricCard(
                        title = "Total Revenue",
                        amount = "₹${totalRevenue.toInt()}",
                        subtitle = "Lifetime paid bookings",
                        icon = Icons.Default.AccountBalance,
                        iconTint = BrandGreenDark,
                        containerColor = Color.White,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AdminNavSection.REPORTS) }
                    )
                    FinancialMetricCard(
                        title = "Pending Settlements",
                        amount = "₹${pendingSettlementsAmount.toInt()}",
                        subtitle = "${pendingSettlements.size} driver payouts awaiting",
                        icon = Icons.Default.ReceiptLong,
                        iconTint = ElectricAmber,
                        containerColor = ElectricAmber.copy(alpha = 0.08f),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AdminNavSection.SETTLEMENTS) }
                    )
                }

                // Row 6: Pending Withdrawals Full Width
                FinancialMetricCard(
                    title = "Pending Withdrawals",
                    amount = "₹${pendingWithdrawalsAmount.toInt()}",
                    subtitle = "${pendingWithdrawals.size} driver wallet cashout requests waiting for approval",
                    icon = Icons.Default.Paid,
                    iconTint = if (pendingWithdrawals.isNotEmpty()) RedCancel else BrandGreenDark,
                    containerColor = if (pendingWithdrawals.isNotEmpty()) RedCancel.copy(alpha = 0.08f) else Color.White,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onNavigate(AdminNavSection.WITHDRAW) }
                )
            }
        }

        // Section Title: Operational Fleet & Platform Statistics
        item {
            Text(
                text = "Fleet & Operations Status",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = CharcoalDark
            )
        }

        // 9 Fleet Stats Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatBox("Total Drivers", totalDrivers.toString(), Icons.Default.ElectricRickshaw, BrandGreenDark, Modifier.weight(1f)) { onNavigate(AdminNavSection.DRIVERS) }
                    StatBox("Approved Drivers", approvedDrivers.toString(), Icons.Default.Verified, BrandGreenDark, Modifier.weight(1f)) { onNavigate(AdminNavSection.DRIVERS) }
                    StatBox("Pending Drivers", pendingDrivers.toString(), Icons.Default.HowToReg, ElectricAmber, Modifier.weight(1f)) { onNavigate(AdminNavSection.DRIVERS) }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatBox("Total Passengers", totalPassengers.toString(), Icons.Default.People, BlueInfo, Modifier.weight(1f)) { onNavigate(AdminNavSection.PASSENGERS) }
                    StatBox("Total Rides", totalRidesCount.toString(), Icons.Default.ListAlt, CharcoalDark, Modifier.weight(1f)) { onNavigate(AdminNavSection.RIDES) }
                    StatBox("Completed Rides", completedRidesCount.toString(), Icons.Default.CheckCircle, BrandGreenDark, Modifier.weight(1f)) { onNavigate(AdminNavSection.RIDES) }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatBox("Cancelled Rides", cancelledRidesCount.toString(), Icons.Default.Cancel, RedCancel, Modifier.weight(1f)) { onNavigate(AdminNavSection.RIDES) }
                    StatBox("Active / Online", activeOnlineDrivers.toString(), Icons.Default.Sensors, BrandGreenDark, Modifier.weight(1f)) { onNavigate(AdminNavSection.DRIVERS) }
                    StatBox("Ongoing Rides", ongoingRidesCount.toString(), Icons.Default.DirectionsCar, ElectricAmber, Modifier.weight(1f)) { onNavigate(AdminNavSection.RIDES) }
                }
            }
        }

        // Quick Management Hub Shortcuts
        item {
            Text(
                text = "Quick Operations Hub",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = CharcoalDark
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onNavigate(AdminNavSection.DRIVERS) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                        ) {
                            Text("Verify Drivers ($pendingDrivers)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { onNavigate(AdminNavSection.WITHDRAW) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreenDark)
                        ) {
                            Text("Payouts (${pendingWithdrawals.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onNavigate(AdminNavSection.FARE_SETTING) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Fare Setting", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { onNavigate(AdminNavSection.SERVICE_AREA) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Service Area", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlowStepChip(label: String, value: String, textColor: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.15f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(text = label, fontSize = 10.sp, color = textColor.copy(alpha = 0.8f))
            Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
        }
    }
}

@Composable
private fun FinancialMetricCard(
    title: String,
    amount: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    containerColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SlateGray
                )
                Surface(
                    shape = CircleShape,
                    color = iconTint.copy(alpha = 0.12f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Text(
                text = amount,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = CharcoalDark
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = SlateGray,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun StatBox(
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CharcoalDark)
            Text(text = title, fontSize = 10.sp, color = SlateGray, maxLines = 1)
        }
    }
}
