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
fun AdminSettlementsTab(
    settlements: List<DriverSettlement>,
    session: AdminSession
) {
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, PENDING, VERIFIED, PAID, REJECTED
    var viewingSettlement by remember { mutableStateOf<DriverSettlement?>(null) }
    var verifyDialogSettlement by remember { mutableStateOf<DriverSettlement?>(null) }
    var verifyNotesInput by remember { mutableStateOf("Bank account & ride verified with zero discrepancy.") }
    var approveDialogSettlement by remember { mutableStateOf<DriverSettlement?>(null) }
    var utrInput by remember { mutableStateOf("UPI-SETTLE-${(100000..999999).random()}") }
    var rejectDialogSettlement by remember { mutableStateOf<DriverSettlement?>(null) }
    var rejectReasonInput by remember { mutableStateOf("Bank account validation failed / Name mismatch.") }

    val filtered = remember(settlements, selectedFilter) {
        when (selectedFilter) {
            "PENDING" -> settlements.filter { it.status == SettlementStatus.PENDING }
            "VERIFIED" -> settlements.filter { it.status == SettlementStatus.VERIFIED }
            "PAID" -> settlements.filter { it.status == SettlementStatus.PAID }
            "REJECTED" -> settlements.filter { it.status == SettlementStatus.REJECTED }
            else -> settlements
        }
    }

    val totalSettled = settlements.filter { it.status == SettlementStatus.PAID }.sumOf { it.driverPayableAmount }
    val pendingVerification = settlements.filter { it.status == SettlementStatus.PENDING || it.status == SettlementStatus.VERIFIED }.sumOf { it.driverPayableAmount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Driver Earning Settlements & Payout Verification", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CharcoalDark)
        }

        // Summary Cards
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Total Paid Settled", fontSize = 11.sp, color = SlateGray)
                        Text("₹${totalSettled.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BrandGreenDark)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Pending Verification", fontSize = 11.sp, color = SlateGray)
                        Text("₹${pendingVerification.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ElectricAmber)
                    }
                }
            }
        }

        // Filter Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("ALL", "PENDING", "VERIFIED", "PAID", "REJECTED").forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandGreen,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Text("No driver settlements matching current filter.", color = SlateGray, fontSize = 13.sp)
            }
        } else {
            items(filtered) { item ->
                val statusColor = when (item.status) {
                    SettlementStatus.PAID -> BrandGreenDark
                    SettlementStatus.APPROVED -> BrandGreenDark
                    SettlementStatus.VERIFIED -> BlueInfo
                    SettlementStatus.PENDING -> ElectricAmber
                    SettlementStatus.REJECTED -> RedCancel
                }
                val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.ENGLISH).format(Date(item.paymentDate))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(item.driverName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CharcoalDark)
                            Surface(shape = RoundedCornerShape(4.dp), color = statusColor.copy(alpha = 0.12f)) {
                                Text(
                                    text = item.status.name,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = statusColor
                                )
                            }
                        }

                        Text("Driver ID: ${item.driverId} • Settlement ID: ${item.id}", fontSize = 12.sp, color = SlateGray)
                        HorizontalDivider(color = CardBorder, thickness = 0.5.dp)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Gross Ride Amount: ₹${item.rideAmount.toInt()}", fontSize = 12.sp, color = CharcoalDark)
                            Text("Admin 10%: ₹${item.commissionAmount.toInt()}", fontSize = 12.sp, color = RedCancel, fontWeight = FontWeight.SemiBold)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Driver Payable Amount:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CharcoalDark)
                            Text("₹${item.driverPayableAmount.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = BrandGreenDark)
                        }

                        if (item.utrReference != null) {
                            Text("UTR / Bank Ref: ${item.utrReference}", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = BrandGreenDark)
                        }
                        if (item.verificationNotes != null) {
                            Text("Notes: ${item.verificationNotes}", fontSize = 11.sp, color = SlateGray)
                        }
                        Text("Date: $dateStr", fontSize = 11.sp, color = SlateGray)

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { viewingSettlement = item }) {
                                Text("View Details", fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(6.dp))

                            if (item.status == SettlementStatus.PENDING) {
                                OutlinedButton(
                                    onClick = {
                                        verifyNotesInput = "Bank account & ride verified with zero discrepancy."
                                        verifyDialogSettlement = item
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Verify", fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                OutlinedButton(
                                    onClick = {
                                        rejectReasonInput = "Bank account validation failed / Name mismatch."
                                        rejectDialogSettlement = item
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RedCancel),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Reject", fontSize = 12.sp)
                                }
                            } else if (item.status == SettlementStatus.VERIFIED) {
                                Button(
                                    onClick = {
                                        utrInput = "UPI-SETTLE-${(100000..999999).random()}"
                                        approveDialogSettlement = item
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreenDark),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Approve & Pay", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // View Details Dialog
    if (viewingSettlement != null) {
        val s = viewingSettlement!!
        AlertDialog(
            onDismissRequest = { viewingSettlement = null },
            title = { Text("Settlement Details ${s.id}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Driver: ${s.driverName} (${s.driverId})")
                    if (s.rideId != null) Text("Linked Ride: ${s.rideId}")
                    Text("Gross Fare: ₹${s.rideAmount.toInt()}")
                    Text("Commission: ₹${s.commissionAmount.toInt()} (${s.commissionRate.toInt()}%)")
                    Text("Net Driver Payable: ₹${s.driverPayableAmount.toInt()}", fontWeight = FontWeight.Bold, color = BrandGreenDark)
                    Text("Payment Mode: ${s.paymentMode.name}")
                    Text("Status: ${s.status.name}")
                    if (s.utrReference != null) Text("UTR Ref: ${s.utrReference}")
                    if (s.verificationNotes != null) Text("Notes: ${s.verificationNotes}")
                }
            },
            confirmButton = {
                Button(onClick = { viewingSettlement = null }, colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)) {
                    Text("Close")
                }
            }
        )
    }

    // Verify Dialog
    if (verifyDialogSettlement != null) {
        val s = verifyDialogSettlement!!
        AlertDialog(
            onDismissRequest = { verifyDialogSettlement = null },
            title = { Text("Verify Driver Settlement", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Verify ₹${s.driverPayableAmount.toInt()} payout for driver ${s.driverName}:")
                    OutlinedTextField(
                        value = verifyNotesInput,
                        onValueChange = { verifyNotesInput = it },
                        label = { Text("Verification Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        RideRepository.verifySettlement(s.id, verifyNotesInput)
                        verifyDialogSettlement = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                ) {
                    Text("Confirm Verified")
                }
            },
            dismissButton = {
                TextButton(onClick = { verifyDialogSettlement = null }) { Text("Cancel") }
            }
        )
    }

    // Approve & Pay Dialog
    if (approveDialogSettlement != null) {
        val s = approveDialogSettlement!!
        AlertDialog(
            onDismissRequest = { approveDialogSettlement = null },
            title = { Text("Record Settlement Payment", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Confirm payment of ₹${s.driverPayableAmount.toInt()} to ${s.driverName}:")
                    OutlinedTextField(
                        value = utrInput,
                        onValueChange = { utrInput = it },
                        label = { Text("Bank UTR / Transaction Reference") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        RideRepository.approveSettlement(s.id, utrInput)
                        approveDialogSettlement = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreenDark)
                ) {
                    Text("Confirm Paid")
                }
            },
            dismissButton = {
                TextButton(onClick = { approveDialogSettlement = null }) { Text("Cancel") }
            }
        )
    }

    // Reject Dialog
    if (rejectDialogSettlement != null) {
        val s = rejectDialogSettlement!!
        AlertDialog(
            onDismissRequest = { rejectDialogSettlement = null },
            title = { Text("Reject Settlement Request", fontWeight = FontWeight.Bold, color = RedCancel) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Specify reason for rejecting settlement of ₹${s.driverPayableAmount.toInt()}:")
                    OutlinedTextField(
                        value = rejectReasonInput,
                        onValueChange = { rejectReasonInput = it },
                        label = { Text("Rejection Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        RideRepository.rejectSettlement(s.id, rejectReasonInput)
                        rejectDialogSettlement = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedCancel)
                ) {
                    Text("Confirm Reject")
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectDialogSettlement = null }) { Text("Cancel") }
            }
        )
    }
}
