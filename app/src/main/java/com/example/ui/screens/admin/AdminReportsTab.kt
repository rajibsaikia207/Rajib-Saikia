package com.example.ui.screens.admin

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminReportsTab(
    rides: List<RideRecord>,
    drivers: List<Driver>,
    passengers: List<Passenger>
) {
    val clipboardManager = LocalClipboardManager.current
    var selectedPeriod by remember { mutableStateOf("ALL_TIME") } // TODAY, WEEK, MONTH, ALL_TIME
    var copiedSuccess by remember { mutableStateOf(false) }

    val istZone = TimeZone.getTimeZone("Asia/Kolkata")
    val calNow = Calendar.getInstance(istZone)
    val todayYear = calNow.get(Calendar.YEAR)
    val todayDay = calNow.get(Calendar.DAY_OF_YEAR)
    val todayMonth = calNow.get(Calendar.MONTH)
    val todayWeek = calNow.get(Calendar.WEEK_OF_YEAR)

    val rideCal = Calendar.getInstance(istZone)

    val periodRides = rides.filter { r ->
        rideCal.timeInMillis = r.completedAt ?: r.createdAt
        when (selectedPeriod) {
            "TODAY" -> rideCal.get(Calendar.YEAR) == todayYear && rideCal.get(Calendar.DAY_OF_YEAR) == todayDay
            "WEEK" -> rideCal.get(Calendar.YEAR) == todayYear && rideCal.get(Calendar.WEEK_OF_YEAR) == todayWeek
            "MONTH" -> rideCal.get(Calendar.YEAR) == todayYear && rideCal.get(Calendar.MONTH) == todayMonth
            else -> true
        }
    }

    val completed = periodRides.filter { it.status == RideStatus.COMPLETED }
    val cancelled = periodRides.filter { it.status == RideStatus.CANCELLED }

    val grossRevenue = completed.sumOf { it.fare }
    val adminCommission = completed.sumOf { it.commissionAmount }
    val driverNet = completed.sumOf { it.driverEarning }

    val cashCollection = completed.filter { it.paymentMode == PaymentMode.CASH }.sumOf { it.fare }
    val onlineCollection = completed.filter { it.paymentMode == PaymentMode.ONLINE_UPI }.sumOf { it.fare }

    val csvContent = buildString {
        appendLine("Ride_ID,Date_Time,Passenger_Name,Driver_Name,Pickup_Address,Drop_Address,Gross_Fare_INR,Admin_Commission_10_INR,Driver_Net_90_INR,Payment_Mode,Status")
        periodRides.forEach { r ->
            val d = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH).format(Date(r.createdAt))
            val p = r.passengerName.replace(",", " ")
            val drv = (r.driverName ?: "Unassigned").replace(",", " ")
            val pk = (r.pickupAddress.ifBlank { r.pickupLocation }).take(30).replace(",", " ")
            val dp = (r.dropoffAddress.ifBlank { r.dropoffLocation }).take(30).replace(",", " ")
            appendLine("${r.id},$d,$p,$drv,$pk,$dp,${r.fare.toInt()},${r.commissionAmount.toInt()},${r.driverEarning.toInt()},${r.paymentMode.name},${r.status.name}")
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Financial & Operational Audit Reports", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CharcoalDark)
        }

        // Period Filters
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "TODAY" to "Today",
                    "WEEK" to "This Week",
                    "MONTH" to "This Month",
                    "ALL_TIME" to "All Time"
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = selectedPeriod == key,
                        onClick = { selectedPeriod = key; copiedSuccess = false },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BrandGreen, selectedLabelColor = Color.White)
                    )
                }
            }
        }

        // Financial KPIs Cards
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Financial Summary ($selectedPeriod)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CharcoalDark)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Gross Fare Revenue:", fontSize = 13.sp, color = SlateGray)
                        Text("₹${grossRevenue.toInt()}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CharcoalDark)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Platform Commission (10%):", fontSize = 13.sp, color = SlateGray)
                        Text("₹${adminCommission.toInt()}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = BrandGreenDark)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Driver Net Take-Home (90%):", fontSize = 13.sp, color = SlateGray)
                        Text("₹${driverNet.toInt()}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CharcoalDark)
                    }
                    HorizontalDivider(color = CardBorder, thickness = 0.5.dp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Cash Mode (Driver collected):", fontSize = 12.sp, color = SlateGray)
                        Text("₹${cashCollection.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CharcoalDark)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Online UPI Mode:", fontSize = 12.sp, color = SlateGray)
                        Text("₹${onlineCollection.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = BlueInfo)
                    }
                }
            }
        }

        // Fleet Trip Stats
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Trip & Operational Metrics", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CharcoalDark)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Bookings:", fontSize = 12.sp, color = SlateGray)
                        Text("${periodRides.size}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CharcoalDark)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Completed Rides:", fontSize = 12.sp, color = SlateGray)
                        Text("${completed.size}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandGreenDark)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Cancelled Rides:", fontSize = 12.sp, color = SlateGray)
                        Text("${cancelled.size}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RedCancel)
                    }
                    val compRate = if (periodRides.isNotEmpty()) ((completed.size.toDouble() / periodRides.size) * 100).toInt() else 0
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Completion Rate:", fontSize = 12.sp, color = SlateGray)
                        Text("$compRate%", fontSize = 13.sp, fontWeight = FontWeight.Black, color = BrandGreenDark)
                    }
                }
            }
        }

        // CSV Export Action
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Raw Data Export & CSV Generation", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CharcoalDark)
                    Text("Generates complete audit report of ${periodRides.size} ride records with financial breakdown.", fontSize = 12.sp, color = SlateGray)

                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(csvContent))
                            copiedSuccess = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreenDark),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (copiedSuccess) "✓ CSV Copied to Clipboard!" else "Copy / Export CSV Report", fontWeight = FontWeight.Bold)
                    }

                    // CSV Preview
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = CharcoalDark,
                        border = BorderStroke(1.dp, Color.DarkGray)
                    ) {
                        Text(
                            text = csvContent.lines().take(6).joinToString("\n") + "\n...",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color(0xFF86EFAC),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }
    }
}
