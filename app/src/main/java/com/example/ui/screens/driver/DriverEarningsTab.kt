package com.example.ui.screens.driver

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.*
import com.example.data.repository.RideRepository
import com.example.ui.components.RideStatusBadge
import com.example.ui.theme.*

@Composable
fun DriverEarningsTabContent(
    driver: Driver
) {
    val allRides by RideRepository.ridesList.collectAsState()
    val driverRides = allRides.filter { it.driverId == driver.id }
    val summary = remember(allRides, driver) {
        RideRepository.getDriverEarningsSummary(driver.id)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Period Earnings Grid
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BrandGreenDark)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Driver Net Earnings Overview", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                    Text("₹${summary.totalEarnings.toInt()}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 32.sp)

                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Today", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                            Text("₹${summary.todayEarnings.toInt()}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Column {
                            Text("This Week", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                            Text("₹${summary.thisWeekEarnings.toInt()}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Column {
                            Text("This Month", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                            Text("₹${summary.thisMonthEarnings.toInt()}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }

        // Cash vs Online Collection Breakdown Card
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
                    Text("Collection & Commission Breakdown", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CharcoalDark)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Cash Collected in Hand:", fontSize = 12.sp, color = SlateGray)
                        Text("₹${summary.cashCollected.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CharcoalDark)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Online Collected by Platform:", fontSize = 12.sp, color = SlateGray)
                        Text("₹${summary.onlineCollected.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CharcoalDark)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Admin Commission (10%):", fontSize = 12.sp, color = SlateGray)
                        Text("₹${summary.adminCommission.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RedCancel)
                    }
                    HorizontalDivider()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Driver Net Take-Home (90%):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandGreenDark)
                        Text("₹${summary.netDriverEarnings.toInt()}", fontWeight = FontWeight.Black, fontSize = 15.sp, color = BrandGreenDark)
                    }
                }
            }
        }

        // Trip History with Breakdown
        item {
            Text("Trip History & Commission Details", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CharcoalDark)
        }

        if (driverRides.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Text("No rides completed yet.", modifier = Modifier.padding(16.dp), fontSize = 13.sp, color = SlateGray)
                }
            }
        } else {
            items(driverRides) { ride ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(ride.id, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SlateGray)
                            RideStatusBadge(status = ride.status)
                        }
                        Text("${ride.pickupLocation} ➔ ${ride.dropoffLocation}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Payment: ${ride.paymentMode.name.replace("_", " ")}", fontSize = 11.sp, color = SlateGray)
                            Text("Gross: ₹${ride.fare.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CharcoalDark)
                        }
                        if (ride.status == RideStatus.COMPLETED) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SlateLight,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Comm (${ride.commissionRate.toInt()}%): ₹${ride.commissionAmount.toInt()}", fontSize = 11.sp, color = SlateGray)
                                    Text("Net Earning: ₹${ride.driverEarning.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandGreenDark)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
