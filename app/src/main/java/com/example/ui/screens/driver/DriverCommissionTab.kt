package com.example.ui.screens.driver

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import com.example.ui.components.RideStatusBadge
import com.example.ui.theme.*

@Composable
fun DriverCommissionTab(
    driver: Driver,
    wallet: DriverWallet,
    onSettleClick: () -> Unit
) {
    val allRides by RideRepository.ridesList.collectAsState()
    val driverRides = allRides.filter { it.driverId == driver.id }
    val completedRides = driverRides.filter { it.status == RideStatus.COMPLETED }
    val summary = remember(allRides, driver) {
        RideRepository.getDriverEarningsSummary(driver.id)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. COMMISSION RATE BANNER
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BrandGreenDark)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Admin Commission Policy",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = BrandGreenLight
                        ) {
                            Text(
                                text = "Standard 10%",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandGreenDark
                            )
                        }
                    }

                    Text(
                        text = "10% Platform Fee",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp
                    )

                    Text(
                        text = "Admin receives 10% platform commission per completed ride. The driver keeps 90% net earnings.",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // 2. COMMISSION METRICS SUMMARY
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Commission & Collection Ledger",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = CharcoalDark
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Gross Cash Collected by Driver:", fontSize = 13.sp, color = SlateGray)
                        Text("₹${summary.cashCollected.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CharcoalDark)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Online Rides Handled by Platform:", fontSize = 13.sp, color = SlateGray)
                        Text("₹${summary.onlineCollected.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CharcoalDark)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Platform Commission (10%):", fontSize = 13.sp, color = SlateGray)
                        Text("₹${summary.adminCommission.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RedCancel)
                    }

                    HorizontalDivider(color = LightSlate)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Pending Commission Due", fontSize = 12.sp, color = SlateGray)
                            Text(
                                "₹${wallet.commissionDue.toInt()}",
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                color = if (wallet.commissionDue > 0) RedCancel else BrandGreenDark
                            )
                        }

                        if (wallet.commissionDue > 0) {
                            Button(
                                onClick = onSettleClick,
                                colors = ButtonDefaults.buttonColors(containerColor = BrandGreenDark),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Settle Commission", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = BrandGreenLight
                            ) {
                                Text(
                                    text = "✓ All Dues Clear",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandGreenDark
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. CASH PAYMENT FLOW EXPLANATION
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("How Cash Commission Works", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CharcoalDark)
                    Text(
                        "1. When passenger chooses Cash payment, driver collects 100% fare directly in hand.\n" +
                        "2. E-Ride 3 system automatically records 10% as Admin Commission due.\n" +
                        "3. 90% is driver take-home pay.\n" +
                        "4. Driver can settle the 10% commission balance via UPI at any time to keep the account active.",
                        fontSize = 12.sp,
                        color = SlateGray,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        // 4. RIDE COMMISSION BREAKDOWN LIST
        item {
            Text(
                text = "Completed Rides Commission Breakdown",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = CharcoalDark
            )
        }

        if (completedRides.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, tint = SlateGray, modifier = Modifier.size(36.dp))
                        Text("No completed rides yet", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = CharcoalDark)
                        Text("Commission breakdown will appear here once rides are finished.", fontSize = 12.sp, color = SlateGray)
                    }
                }
            }
        } else {
            items(completedRides) { ride ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(ride.id, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CharcoalDark)
                            RideStatusBadge(status = ride.status)
                        }

                        Text(
                            "${ride.pickupLocation} ➔ ${ride.dropoffLocation}",
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = CharcoalDark
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Payment: ${ride.paymentMode.name.replace("_", " ")}", fontSize = 11.sp, color = SlateGray)
                            Text("Gross Fare: ₹${ride.fare.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CharcoalDark)
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Admin 10%: ₹${ride.commissionAmount.toInt()}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = RedCancel
                                )
                                Text(
                                    "Driver 90%: ₹${ride.driverEarning.toInt()}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandGreenDark
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
