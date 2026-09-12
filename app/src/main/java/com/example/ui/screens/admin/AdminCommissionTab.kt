package com.example.ui.screens.admin

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
fun AdminCommissionTab(
    commissionLedger: List<CommissionLedgerEntry>,
    rides: List<RideRecord>,
    drivers: List<Driver>,
    driverWallets: Map<String, DriverWallet>,
    currentCommissionRate: Double,
    session: AdminSession
) {
    var selectedView by remember { mutableStateOf("DRIVER_WISE") } // DRIVER_WISE, DATE_WISE, RIDE_WISE
    var editRateInput by remember(currentCommissionRate) { mutableStateOf(currentCommissionRate.toInt().toString()) }
    var updateSuccess by remember { mutableStateOf(false) }
    var updateError by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val completedRides = rides.filter { it.status == RideStatus.COMPLETED }
    val totalBookingValue = completedRides.sumOf { it.fare }
    val totalCommissionGenerated = completedRides.sumOf { it.commissionAmount }
    val totalCommissionPending = driverWallets.values.sumOf { it.commissionDue }
    val totalCommissionCollected = maxOf(0.0, totalCommissionGenerated - totalCommissionPending)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Commission Rate Management Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Platform Commission Configuration", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CharcoalDark)
                            Text("Standard Rate: ${currentCommissionRate.toInt()}% for all upcoming rides", fontSize = 12.sp, color = SlateGray)
                        }
                        Surface(shape = RoundedCornerShape(8.dp), color = BrandGreenLight) {
                            Text(
                                text = "${currentCommissionRate.toInt()}% ACTIVE",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = BrandGreenDark
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editRateInput,
                            onValueChange = {
                                if (it.all { c -> c.isDigit() || c == '.' }) {
                                    editRateInput = it
                                    updateSuccess = false
                                    updateError = null
                                }
                            },
                            label = { Text("Update Rate (%)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Button(
                            onClick = {
                                if (session.admin.role == AdminRole.SUPPORT) {
                                    updateError = "Support role cannot edit commission rate."
                                    return@Button
                                }
                                val parsed = editRateInput.toDoubleOrNull()
                                if (parsed != null && parsed in 0.0..100.0) {
                                    RideRepository.updateAdminCommissionRate(parsed)
                                    updateSuccess = true
                                } else {
                                    updateError = "Enter valid rate between 0-100"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Save Rate", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (updateSuccess) {
                        Text("✓ Commission rate successfully updated.", color = BrandGreenDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    if (updateError != null) {
                        Text(updateError!!, color = RedCancel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 4 Financial Commission Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricBox("Total Booking Value", "₹${totalBookingValue.toInt()}", BrandGreenDark, Modifier.weight(1f))
                    MetricBox("Commission Generated", "₹${totalCommissionGenerated.toInt()}", BrandGreenDark, Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricBox("Commission Collected", "₹${totalCommissionCollected.toInt()}", BrandGreenDark, Modifier.weight(1f))
                    MetricBox("Commission Pending", "₹${totalCommissionPending.toInt()}", RedCancel, Modifier.weight(1f))
                }
            }
        }

        // View Tabs: Driver-wise | Date-wise | Ride-wise
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TabButton("Driver-wise", selectedView == "DRIVER_WISE", Modifier.weight(1f)) { selectedView = "DRIVER_WISE" }
                TabButton("Date-wise", selectedView == "DATE_WISE", Modifier.weight(1f)) { selectedView = "DATE_WISE" }
                TabButton("Ride-wise", selectedView == "RIDE_WISE", Modifier.weight(1f)) { selectedView = "RIDE_WISE" }
            }
        }

        // 1. DRIVER-WISE COMMISSION VIEW
        if (selectedView == "DRIVER_WISE") {
            item {
                Text("Driver Commission Breakdown (${drivers.size} Drivers)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            items(drivers) { driver ->
                val wallet = driverWallets[driver.id] ?: DriverWallet(driverId = driver.id)
                val driverRides = completedRides.filter { it.driverId == driver.id }
                val driverGross = driverRides.sumOf { it.fare }
                val driverCommission = driverRides.sumOf { it.commissionAmount }
                val driverCollected = maxOf(0.0, driverCommission - wallet.commissionDue)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(driver.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CharcoalDark)
                            Text(driver.rickshawRegNo, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SlateGray)
                        }
                        Text("ID: ${driver.id} • ${driverRides.size} Completed Rides", fontSize = 11.sp, color = SlateGray)
                        HorizontalDivider(color = CardBorder, thickness = 0.5.dp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Booking: ₹${driverGross.toInt()}", fontSize = 12.sp, color = CharcoalDark)
                            Text("Commission (10%): ₹${driverCommission.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandGreenDark)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Collected: ₹${driverCollected.toInt()}", fontSize = 11.sp, color = BrandGreenDark)
                            Text("Pending Due: ₹${wallet.commissionDue.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (wallet.commissionDue > 0) RedCancel else BrandGreenDark)
                        }
                    }
                }
            }
        }

        // 2. DATE-WISE COMMISSION VIEW
        if (selectedView == "DATE_WISE") {
            val groupedByDate = completedRides.groupBy {
                SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date(it.completedAt ?: it.createdAt))
            }
            item {
                Text("Date-wise Commission Records (${groupedByDate.size} Days)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            items(groupedByDate.entries.toList()) { entry ->
                val dateStr = entry.key
                val dayRides = entry.value
                val dayGross = dayRides.sumOf { it.fare }
                val dayCommission = dayRides.sumOf { it.commissionAmount }
                val dayDriverNet = dayRides.sumOf { it.driverEarning }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(dateStr, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CharcoalDark)
                            Text("${dayRides.size} Rides", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BlueInfo)
                        }
                        HorizontalDivider(color = CardBorder, thickness = 0.5.dp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Gross Value: ₹${dayGross.toInt()}", fontSize = 12.sp, color = CharcoalDark)
                            Text("Admin 10%: ₹${dayCommission.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandGreenDark)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Driver Net (90%): ₹${dayDriverNet.toInt()}", fontSize = 11.sp, color = SlateGray)
                        }
                    }
                }
            }
        }

        // 3. RIDE-WISE COMMISSION VIEW
        if (selectedView == "RIDE_WISE") {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by Ride ID or Driver Name...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                )
            }
            val filteredRides = completedRides.filter {
                searchQuery.isBlank() || it.id.contains(searchQuery, ignoreCase = true) || (it.driverName ?: "").contains(searchQuery, ignoreCase = true)
            }
            items(filteredRides) { r ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(r.id, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CharcoalDark)
                            Text("₹${r.fare.toInt()}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandGreenDark)
                        }
                        Text("Driver: ${r.driverName ?: "N/A"} • Passenger: ${r.passengerName}", fontSize = 12.sp, color = SlateGray)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Commission (10%): ₹${r.commissionAmount.toInt()}", fontSize = 11.sp, color = RedCancel, fontWeight = FontWeight.SemiBold)
                            Text("Driver 90%: ₹${r.driverEarning.toInt()}", fontSize = 11.sp, color = BrandGreenDark, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Mode: ${r.paymentMode.name}", fontSize = 10.sp, color = SlateGray)
                            Text("Status: ${r.paymentStatus.name}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BrandGreenDark)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBox(title: String, amount: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontSize = 11.sp, color = SlateGray)
            Text(amount, fontSize = 18.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}

@Composable
private fun TabButton(text: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) BrandGreen else Color.White,
        border = BorderStroke(1.dp, if (isSelected) BrandGreen else CardBorder),
        modifier = modifier.height(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else CharcoalDark
            )
        }
    }
}
