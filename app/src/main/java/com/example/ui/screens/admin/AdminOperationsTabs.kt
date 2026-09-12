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
fun AdminPassengersView(
    passengers: List<Passenger>,
    rides: List<RideRecord>
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = passengers.filter {
        searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Registered Passengers (${passengers.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CharcoalDark)
        }
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search passenger by name or phone...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
            )
        }

        items(filtered) { p ->
            val pRides = rides.filter { it.passengerId == p.id }
            val completedCount = pRides.count { it.status == RideStatus.COMPLETED }
            val totalSpent = pRides.filter { it.status == RideStatus.COMPLETED }.sumOf { it.fare }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(p.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CharcoalDark)
                        Text(p.phone, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = BrandGreenDark)
                    }
                    Text("ID: ${p.id} • Rating: ★ ${p.rating}", fontSize = 11.sp, color = SlateGray)
                    HorizontalDivider(color = CardBorder, thickness = 0.5.dp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Completed Rides: $completedCount", fontSize = 12.sp, color = CharcoalDark)
                        Text("Total Spent: ₹${totalSpent.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandGreenDark)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminDriversView(
    drivers: List<Driver>,
    driverWallets: Map<String, DriverWallet>,
    session: AdminSession
) {
    var driverTab by remember { mutableStateOf("ALL") } // ALL, APPROVED, PENDING, REJECTED
    var selectedDriverForKyc by remember { mutableStateOf<Driver?>(null) }

    val filtered = remember(drivers, driverTab) {
        when (driverTab) {
            "APPROVED" -> drivers.filter { it.status == DriverStatus.APPROVED }
            "PENDING" -> drivers.filter { it.status == DriverStatus.PENDING_APPROVAL }
            "REJECTED" -> drivers.filter { it.status == DriverStatus.REJECTED || it.status == DriverStatus.SUSPENDED }
            else -> drivers
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Driver Fleet Management (${drivers.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CharcoalDark)
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("ALL", "APPROVED", "PENDING", "REJECTED").forEach { tab ->
                    val count = when (tab) {
                        "APPROVED" -> drivers.count { it.status == DriverStatus.APPROVED }
                        "PENDING" -> drivers.count { it.status == DriverStatus.PENDING_APPROVAL }
                        "REJECTED" -> drivers.count { it.status == DriverStatus.REJECTED || it.status == DriverStatus.SUSPENDED }
                        else -> drivers.size
                    }
                    FilterChip(
                        selected = driverTab == tab,
                        onClick = { driverTab = tab },
                        label = { Text("$tab ($count)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BrandGreen, selectedLabelColor = Color.White)
                    )
                }
            }
        }

        items(filtered) { d ->
            val wallet = driverWallets[d.id] ?: DriverWallet(driverId = d.id)
            val statusColor = when (d.status) {
                DriverStatus.APPROVED -> BrandGreenDark
                DriverStatus.PENDING_APPROVAL -> ElectricAmber
                DriverStatus.REJECTED, DriverStatus.SUSPENDED -> RedCancel
                else -> SlateGray
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(d.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CharcoalDark)
                            Text(d.phone, fontSize = 12.sp, color = SlateGray)
                        }
                        Surface(shape = RoundedCornerShape(4.dp), color = statusColor.copy(alpha = 0.12f)) {
                            Text(
                                text = d.status.name,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = statusColor
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Reg: ${d.rickshawRegNo}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CharcoalDark)
                        Text("City: ${d.serviceCity}", fontSize = 12.sp, color = SlateGray)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Wallet Balance: ₹${wallet.availableBalance.toInt()}", fontSize = 12.sp, color = BrandGreenDark, fontWeight = FontWeight.Bold)
                        Text("Commission Due: ₹${wallet.commissionDue.toInt()}", fontSize = 12.sp, color = if (wallet.commissionDue > 0) RedCancel else BrandGreenDark, fontWeight = FontWeight.Bold)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Live Duty: ${if (d.isOnline) "🟢 ONLINE" else "⚪ OFFLINE"}", fontSize = 11.sp, color = CharcoalDark)
                        Text("Rating: ★ ${d.rating}", fontSize = 11.sp, color = BrandGreenDark)
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { selectedDriverForKyc = d },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("KYC & Vehicle", fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.width(6.dp))

                        if (d.status == DriverStatus.PENDING_APPROVAL) {
                            Button(
                                onClick = { RideRepository.updateDriverStatus(d.id, DriverStatus.APPROVED) },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Approve", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            OutlinedButton(
                                onClick = { RideRepository.updateDriverStatus(d.id, DriverStatus.REJECTED) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RedCancel),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Reject", fontSize = 11.sp)
                            }
                        } else if (d.status == DriverStatus.APPROVED) {
                            OutlinedButton(
                                onClick = { RideRepository.updateDriverStatus(d.id, DriverStatus.SUSPENDED) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RedCancel),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Suspend", fontSize = 11.sp)
                            }
                        } else {
                            Button(
                                onClick = { RideRepository.updateDriverStatus(d.id, DriverStatus.APPROVED) },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Re-activate", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedDriverForKyc != null) {
        val d = selectedDriverForKyc!!
        AlertDialog(
            onDismissRequest = { selectedDriverForKyc = null },
            title = { Text("Driver Verification Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Name: ${d.name}")
                    Text("Phone: ${d.phone}")
                    Text("RC Details: ${d.rcDetails}")
                    Text("License: ${d.licenseNo ?: "Verified"}")
                    Text("License Details: ${d.licenseDetails ?: "Commercial LMV/E-Rickshaw"}")
                    Text("Rickshaw Reg: ${d.rickshawRegNo}")
                    Text("Assigned City: ${d.serviceCity}")
                }
            },
            confirmButton = {
                Button(onClick = { selectedDriverForKyc = null }, colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun AdminRidesView(
    rides: List<RideRecord>
) {
    var selectedStatus by remember { mutableStateOf("ALL") }
    var selectedRide by remember { mutableStateOf<RideRecord?>(null) }

    val filtered = remember(rides, selectedStatus) {
        if (selectedStatus == "ALL") rides else rides.filter { it.status.name == selectedStatus }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Ride Booking Records (${rides.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CharcoalDark)
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("ALL", "REQUESTED", "DRIVER_ASSIGNED", "IN_PROGRESS", "COMPLETED", "CANCELLED").forEach { st ->
                    FilterChip(
                        selected = selectedStatus == st,
                        onClick = { selectedStatus = st },
                        label = { Text(st, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BrandGreen, selectedLabelColor = Color.White)
                    )
                }
            }
        }

        items(filtered) { r ->
            val dateStr = SimpleDateFormat("dd MMM, HH:mm", Locale.ENGLISH).format(Date(r.createdAt))

            Card(
                modifier = Modifier.fillMaxWidth().clickable { selectedRide = r },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(r.id, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CharcoalDark)
                        Text("₹${r.fare.toInt()}", fontWeight = FontWeight.Black, fontSize = 15.sp, color = BrandGreenDark)
                    }
                    Text("Passenger: ${r.passengerName} • Driver: ${r.driverName ?: "Unassigned"}", fontSize = 12.sp, color = CharcoalDark)
                    Text("From: ${r.pickupAddress.ifBlank { r.pickupLocation }}", fontSize = 11.sp, color = SlateGray, maxLines = 1)
                    Text("To: ${r.dropoffAddress.ifBlank { r.dropoffLocation }}", fontSize = 11.sp, color = SlateGray, maxLines = 1)
                    HorizontalDivider(color = CardBorder, thickness = 0.5.dp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Status: ${r.status.name}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (r.status == RideStatus.COMPLETED) BrandGreenDark else SlateGray)
                        Text("Mode: ${r.paymentMode.name} • $dateStr", fontSize = 10.sp, color = SlateGray)
                    }
                }
            }
        }
    }

    if (selectedRide != null) {
        val r = selectedRide!!
        AlertDialog(
            onDismissRequest = { selectedRide = null },
            title = { Text("Ride Details ${r.id}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Passenger: ${r.passengerName} (${r.passengerPhone})")
                    Text("Driver: ${r.driverName ?: "None"}")
                    Text("Pickup: ${r.pickupAddress.ifBlank { r.pickupLocation }}")
                    Text("Drop: ${r.dropoffAddress.ifBlank { r.dropoffLocation }}")
                    Text("Distance: ${r.distanceKm} KM")
                    Text("Gross Fare: ₹${r.fare.toInt()}")
                    Text("Platform Commission: ₹${r.commissionAmount.toInt()} (10%)")
                    Text("Driver Net: ₹${r.driverEarning.toInt()} (90%)")
                    Text("Payment: ${r.paymentMode.name} (${r.paymentStatus.name})")
                    Text("Status: ${r.status.name}")
                }
            },
            confirmButton = {
                Button(onClick = { selectedRide = null }, colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun AdminPaymentsView(
    transactions: List<FinancialTransaction>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Ledger Transactions & Payments (${transactions.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CharcoalDark)
        }

        items(transactions) { t ->
            val dateStr = SimpleDateFormat("dd MMM, HH:mm", Locale.ENGLISH).format(Date(t.timestamp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(t.type.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CharcoalDark)
                        Text("₹${t.amount.toInt()}", fontWeight = FontWeight.Black, fontSize = 15.sp, color = if (t.type == TransactionType.COMMISSION || t.type == TransactionType.RIDE_EARNING) BrandGreenDark else SlateGray)
                    }
                    Text(t.description, fontSize = 12.sp, color = CharcoalDark)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Txn ID: ${t.id}", fontSize = 10.sp, color = SlateGray)
                        Text("Status: ${t.status.name} • $dateStr", fontSize = 10.sp, color = SlateGray)
                    }
                }
            }
        }
    }
}
