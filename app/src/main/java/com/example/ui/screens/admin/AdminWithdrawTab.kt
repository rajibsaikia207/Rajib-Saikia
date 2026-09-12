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
fun AdminWithdrawTab(
    withdrawals: List<WithdrawalRequest>,
    driverWallets: Map<String, DriverWallet>,
    session: AdminSession
) {
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, PENDING, APPROVED, REJECTED
    var settleDialogReq by remember { mutableStateOf<WithdrawalRequest?>(null) }
    var refNumber by remember { mutableStateOf("UPI-BANK-SETTLE-${(1000..9999).random()}") }
    var rejectDialogReq by remember { mutableStateOf<WithdrawalRequest?>(null) }
    var rejectReason by remember { mutableStateOf("Bank account / UPI ID validation failed.") }

    val availableDriverBalance = driverWallets.values.sumOf { it.availableBalance }
    val totalWithdrawn = withdrawals.filter { it.status == WithdrawalStatus.COMPLETED || it.status == WithdrawalStatus.PAID }.sumOf { it.amount }
    val pendingWithdrawals = withdrawals.filter { it.status == WithdrawalStatus.PENDING || it.status == WithdrawalStatus.PROCESSING }.sumOf { it.amount }
    val rejectedWithdrawals = withdrawals.filter { it.status == WithdrawalStatus.REJECTED || it.status == WithdrawalStatus.FAILED }.sumOf { it.amount }

    val filtered = remember(withdrawals, selectedFilter) {
        when (selectedFilter) {
            "PENDING" -> withdrawals.filter { it.status == WithdrawalStatus.PENDING || it.status == WithdrawalStatus.PROCESSING }
            "APPROVED" -> withdrawals.filter { it.status in listOf(WithdrawalStatus.COMPLETED, WithdrawalStatus.PAID, WithdrawalStatus.APPROVED) }
            "REJECTED" -> withdrawals.filter { it.status in listOf(WithdrawalStatus.REJECTED, WithdrawalStatus.FAILED, WithdrawalStatus.CANCELLED) }
            else -> withdrawals
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Driver Earnings Withdrawal & Payout Management", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CharcoalDark)
        }

        // Payout Schedule Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = BrandGreenLight)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = BrandGreenDark)
                    Column {
                        Text("Official Assam Payout Schedule", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandGreenDark)
                        Text("Monday – Friday: 9:00 AM to 5:00 PM IST (Saturday & Sunday Closed).", fontSize = 11.sp, color = CharcoalDark)
                    }
                }
            }
        }

        // 4 Metric Overview Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WithdrawMetricCard("Available Driver Balance", "₹${availableDriverBalance.toInt()}", BrandGreenDark, Modifier.weight(1f))
                    WithdrawMetricCard("Total Withdrawn / Paid", "₹${totalWithdrawn.toInt()}", BrandGreenDark, Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WithdrawMetricCard("Pending Withdrawals", "₹${pendingWithdrawals.toInt()}", ElectricAmber, Modifier.weight(1f))
                    WithdrawMetricCard("Rejected Withdrawals", "₹${rejectedWithdrawals.toInt()}", RedCancel, Modifier.weight(1f))
                }
            }
        }

        // Filters
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("ALL", "PENDING", "APPROVED", "REJECTED").forEach { f ->
                    FilterChip(
                        selected = selectedFilter == f,
                        onClick = { selectedFilter = f },
                        label = { Text(f, fontSize = 11.sp) },
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
                Text("No withdrawal requests found.", color = SlateGray, fontSize = 13.sp)
            }
        } else {
            items(filtered) { req ->
                val statusColor = when (req.status) {
                    WithdrawalStatus.COMPLETED, WithdrawalStatus.PAID, WithdrawalStatus.APPROVED -> BrandGreenDark
                    WithdrawalStatus.PENDING -> ElectricAmber
                    WithdrawalStatus.PROCESSING -> BlueInfo
                    WithdrawalStatus.REJECTED, WithdrawalStatus.FAILED, WithdrawalStatus.CANCELLED -> RedCancel
                }
                val reqDate = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.ENGLISH).format(Date(req.requestedAt))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(req.driverName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CharcoalDark)
                                Text(req.driverPhone, fontSize = 12.sp, color = SlateGray)
                            }
                            Surface(shape = RoundedCornerShape(4.dp), color = statusColor.copy(alpha = 0.12f)) {
                                Text(
                                    text = req.status.name,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = statusColor
                                )
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Withdrawal Amount:", fontSize = 13.sp, color = SlateGray)
                            Text("₹${req.amount.toInt()}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = BrandGreenDark)
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BackgroundLight,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Payout Method: ${if (req.paymentMethod == "BANK_ACCOUNT") "Bank Account Transfer" else "UPI Instant"}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = CharcoalDark
                                )
                                if (req.paymentMethod == "BANK_ACCOUNT") {
                                    if (req.bankName != null) Text("Bank: ${req.bankName}", fontSize = 11.sp, color = SlateGray)
                                    if (req.accountHolderName != null) Text("A/C Holder: ${req.accountHolderName}", fontSize = 11.sp, color = SlateGray)
                                    if (req.maskedAccountNumber != null) Text("A/C Number: ${req.maskedAccountNumber}", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = CharcoalDark)
                                    if (req.ifscCode != null) Text("IFSC: ${req.ifscCode}", fontSize = 11.sp, color = SlateGray)
                                } else {
                                    Text("UPI ID: ${req.upiId}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = CharcoalDark)
                                }
                            }
                        }

                        Text("Request ID: ${req.id} • Driver ID: ${req.driverId} • Date: $reqDate", fontSize = 11.sp, color = SlateGray)

                        if (req.status in listOf(WithdrawalStatus.PAID, WithdrawalStatus.COMPLETED) && req.transactionReference != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = BrandGreenLight,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Payment Reference / UTR: ${req.transactionReference}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandGreenDark)
                                    if (req.paidAt != null) {
                                        val paidDate = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.ENGLISH).format(Date(req.paidAt))
                                        Text("Paid At: $paidDate", fontSize = 11.sp, color = BrandGreenDark)
                                    }
                                }
                            }
                        }

                        if (req.rejectionReason != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = RedCancel.copy(alpha = 0.08f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Rejection Reason: ${req.rejectionReason}", fontSize = 11.sp, color = RedCancel, fontWeight = FontWeight.SemiBold)
                                    Text("Amount of ₹${req.amount.toInt()} has been returned to driver wallet.", fontSize = 10.sp, color = RedCancel)
                                }
                            }
                        }

                        // Actions for PENDING and PROCESSING
                        if (req.status == WithdrawalStatus.PENDING) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        rejectReason = "Bank account / UPI ID validation failed."
                                        rejectDialogReq = req
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RedCancel),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Reject", fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { RideRepository.approveWithdrawal(req.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Approve", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = {
                                        refNumber = "UPI-BANK-SETTLE-${(1000..9999).random()}"
                                        settleDialogReq = req
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreenDark),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1.3f)
                                ) {
                                    Text("Mark Paid", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else if (req.status == WithdrawalStatus.PROCESSING || req.status == WithdrawalStatus.APPROVED) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        rejectReason = "Bank account / UPI ID validation failed."
                                        rejectDialogReq = req
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RedCancel),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Reject", fontSize = 12.sp)
                                }
                                Button(
                                    onClick = {
                                        refNumber = "UPI-BANK-SETTLE-${(1000..9999).random()}"
                                        settleDialogReq = req
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreenDark),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1.3f)
                                ) {
                                    Text("Mark Paid (Add UTR)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (settleDialogReq != null) {
        val req = settleDialogReq!!
        AlertDialog(
            onDismissRequest = { settleDialogReq = null },
            title = { Text("Record Settlement Reference", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Confirm payment of ₹${req.amount.toInt()} to ${req.upiId}:")
                    OutlinedTextField(
                        value = refNumber,
                        onValueChange = { refNumber = it },
                        label = { Text("Bank UTR / Transaction Ref") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        RideRepository.markWithdrawalAsPaid(req.id, refNumber)
                        settleDialogReq = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                ) {
                    Text("Confirm Paid")
                }
            },
            dismissButton = {
                TextButton(onClick = { settleDialogReq = null }) { Text("Cancel") }
            }
        )
    }

    if (rejectDialogReq != null) {
        val req = rejectDialogReq!!
        AlertDialog(
            onDismissRequest = { rejectDialogReq = null },
            title = { Text("Reject Withdrawal Request", fontWeight = FontWeight.Bold, color = RedCancel) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Amount ₹${req.amount.toInt()} will be refunded to driver wallet.")
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        label = { Text("Rejection Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        RideRepository.rejectWithdrawal(req.id, rejectReason)
                        rejectDialogReq = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedCancel)
                ) {
                    Text("Confirm Reject")
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectDialogReq = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun WithdrawMetricCard(title: String, amount: String, color: Color, modifier: Modifier = Modifier) {
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
