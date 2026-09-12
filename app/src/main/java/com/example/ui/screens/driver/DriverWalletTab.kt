package com.example.ui.screens.driver

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.models.*
import com.example.data.repository.RideRepository
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DriverWalletTabContent(
    driver: Driver,
    wallet: DriverWallet,
    onWithdrawClick: () -> Unit,
    onSettleClick: () -> Unit
) {
    val transactions by RideRepository.financialTransactions.collectAsStateWithLifecycle()
    val driverTxns = remember(transactions, driver.id) {
        transactions.filter { it.driverId == driver.id }
    }
    val allWithdrawals by RideRepository.withdrawalRequests.collectAsStateWithLifecycle()
    val driverWithdrawals = remember(allWithdrawals, driver.id) {
        allWithdrawals.filter { it.driverId == driver.id }
    }
    val earningsSummary = remember(driver.id) {
        RideRepository.getDriverEarningsSummary(driver.id)
    }

    val scheduleCheck = remember { RideRepository.getWithdrawalScheduleCheck() }
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    var selectedWithdrawalDetail by remember { mutableStateOf<WithdrawalRequest?>(null) }
    var actionMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Schedule Announcement Banner
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (scheduleCheck.isOpen) BrandGreenLight else RedCancel.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, if (scheduleCheck.isOpen) BrandGreen else RedCancel.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        if (scheduleCheck.isOpen) Icons.Default.CheckCircle else Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = if (scheduleCheck.isOpen) BrandGreenDark else RedCancel
                    )
                    Column {
                        Text(
                            text = scheduleCheck.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (scheduleCheck.isOpen) BrandGreenDark else RedCancel
                        )
                        Text(
                            text = "Mon – Fri: 9:00 AM – 5:00 PM IST (Sat & Sun Closed)",
                            fontSize = 11.sp,
                            color = CharcoalDark.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Section 1: Available Balance & Withdraw Button
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BrandGreenDark)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Driver Wallet (Net Take-Home)",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = "Available Balance",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )

                    Text(
                        text = "₹${wallet.availableBalance.toInt()}",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 36.sp
                    )

                    Text(
                        text = "Available Balance (After 10% Commission)",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = onWithdrawClick,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("driver_wallet_withdraw_button")
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Withdraw", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        // Sections 2 & 3: Pending Withdrawal & Total Withdrawn
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = ElectricAmber, modifier = Modifier.size(14.dp))
                            Text("Pending Earnings", fontSize = 11.sp, color = SlateGray, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${wallet.pendingBalance.toInt()}",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = CharcoalDark
                        )
                        Text("In approval / queue", fontSize = 10.sp, color = SlateGray)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BrandGreenDark, modifier = Modifier.size(14.dp))
                            Text("Total Withdrawn", fontSize = 11.sp, color = SlateGray, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${wallet.totalWithdrawn.toInt()}",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = BrandGreenDark
                        )
                        Text("Paid to bank/UPI", fontSize = 10.sp, color = SlateGray)
                    }
                }
            }
        }

        // Sections 4 & 5: Today's Earnings & Weekly Earnings
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Today's Earnings", fontSize = 11.sp, color = SlateGray, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${earningsSummary.todayEarnings.toInt()}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = CharcoalDark
                        )
                        Text("Completed rides", fontSize = 10.sp, color = SlateGray)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Weekly Earnings", fontSize = 11.sp, color = SlateGray, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${earningsSummary.thisWeekEarnings.toInt()}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = CharcoalDark
                        )
                        Text("This calendar week", fontSize = 10.sp, color = SlateGray)
                    }
                }
            }
        }

        // Section 6: Commission (10% platform commission)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
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
                        Column {
                            Text("Platform Commission (10%)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CharcoalDark)
                            Text("Automatic 90% Driver / 10% Platform split", fontSize = 11.sp, color = SlateGray)
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (wallet.commissionDue > 0) ElectricAmber.copy(alpha = 0.15f) else BrandGreenLight
                        ) {
                            Text(
                                text = if (wallet.commissionDue > 0) "DUE: ₹${wallet.commissionDue.toInt()}" else "UP TO DATE",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (wallet.commissionDue > 0) CharcoalDark else BrandGreenDark
                            )
                        }
                    }

                    if (wallet.commissionDue > 0) {
                        Button(
                            onClick = onSettleClick,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Settle Commission Due (₹${wallet.commissionDue.toInt()})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Section 7: Cash Collection & Open Ledger
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
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
                        Text("Cash Collection", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CharcoalDark)
                        Text("Settlement Due: ₹${wallet.commissionDue.toInt()}", fontSize = 12.sp, color = SlateGray, fontWeight = FontWeight.Medium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Today's Cash", fontSize = 11.sp, color = SlateGray)
                            Text("₹${earningsSummary.cashCollected.toInt()}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CharcoalDark)
                        }
                        Column {
                            Text("Total Cash in Hand", fontSize = 11.sp, color = SlateGray)
                            Text("₹${earningsSummary.cashCollected.toInt()}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CharcoalDark)
                        }
                        Column {
                            Text("Trips Done", fontSize = 11.sp, color = SlateGray)
                            Text("${wallet.totalRides}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CharcoalDark)
                        }
                    }
                }
            }
        }

        // Section 8: Withdrawal History
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Withdrawal History",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = CharcoalDark
                )
                Text(
                    text = "${driverWithdrawals.size} requests",
                    fontSize = 12.sp,
                    color = SlateGray
                )
            }
        }

        if (actionMessage != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BrandGreenLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = actionMessage!!,
                        modifier = Modifier.padding(10.dp),
                        fontSize = 12.sp,
                        color = BrandGreenDark,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (driverWithdrawals.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, tint = SlateGray, modifier = Modifier.size(32.dp))
                        Text("No withdrawal requests yet.", fontSize = 13.sp, color = SlateGray)
                        Text("Tap Withdraw above to request payout.", fontSize = 11.sp, color = SlateGray)
                    }
                }
            }
        } else {
            items(driverWithdrawals) { req ->
                val statusColor = when (req.status) {
                    WithdrawalStatus.PAID, WithdrawalStatus.COMPLETED, WithdrawalStatus.APPROVED -> BrandGreenDark
                    WithdrawalStatus.PENDING -> ElectricAmber
                    WithdrawalStatus.PROCESSING -> BlueInfo
                    WithdrawalStatus.REJECTED, WithdrawalStatus.FAILED, WithdrawalStatus.CANCELLED -> RedCancel
                }
                val reqDateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).format(Date(req.requestedAt))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedWithdrawalDetail = req },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "₹${req.amount.toInt()}",
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp,
                                color = BrandGreenDark
                            )

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = statusColor.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = req.status.name,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = statusColor
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("ID: ${req.id}", fontSize = 11.sp, color = SlateGray)
                            Text(reqDateStr, fontSize = 11.sp, color = SlateGray)
                        }

                        Text(
                            text = if (req.paymentMethod == "BANK_ACCOUNT") {
                                "Bank: ${req.bankName ?: "Bank Account"} (${req.maskedAccountNumber ?: ""})"
                            } else {
                                "UPI: ${RideRepository.maskUpiId(req.upiId)}"
                            },
                            fontSize = 11.sp,
                            color = CharcoalDark
                        )

                        if (req.status == WithdrawalStatus.PAID && req.transactionReference != null) {
                            Text("UTR Ref: ${req.transactionReference}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = BrandGreenDark)
                        }

                        if (req.status == WithdrawalStatus.REJECTED && req.rejectionReason != null) {
                            Text("Reason: ${req.rejectionReason} (Refunded to wallet)", fontSize = 11.sp, color = RedCancel)
                        }
                    }
                }
            }
        }

        // Section: Immutable Financial Ledger
        item {
            Text(
                text = "Immutable Financial Ledger",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = CharcoalDark,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (driverTxns.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = SlateGray, modifier = Modifier.size(32.dp))
                        Text("No ledger transactions recorded yet.", fontSize = 13.sp, color = SlateGray)
                    }
                }
            }
        } else {
            items(driverTxns) { txn ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(if (txn.isCredit) BrandGreenLight else RedCancel.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (txn.isCredit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = if (txn.isCredit) BrandGreenDark else RedCancel,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(txn.description, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = CharcoalDark)
                                Text(dateFormat.format(Date(txn.timestamp)), fontSize = 11.sp, color = SlateGray)
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${if (txn.isCredit) "+" else "-"}₹${txn.amount.toInt()}",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = if (txn.isCredit) BrandGreenDark else RedCancel
                            )
                            Text(
                                text = txn.status.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (txn.status) {
                                    TransactionStatus.COMPLETED -> BrandGreenDark
                                    TransactionStatus.PENDING -> ElectricAmber
                                    TransactionStatus.REVERSED -> RedCancel
                                    TransactionStatus.FAILED -> RedCancel
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (selectedWithdrawalDetail != null) {
        WithdrawalDetailDialog(
            request = selectedWithdrawalDetail!!,
            onDismiss = { selectedWithdrawalDetail = null },
            onCancelRequest = { id ->
                val (success, msg) = RideRepository.cancelWithdrawal(id)
                actionMessage = msg
            }
        )
    }
}
