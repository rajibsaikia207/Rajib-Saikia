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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.*
import com.example.data.repository.RideRepository
import com.example.ui.theme.*

@Composable
fun AdminFareSettingsTab(
    fareSettings: FareSettings,
    session: AdminSession
) {
    var isEditing by remember { mutableStateOf(false) }

    var baseFareInput by remember(fareSettings) { mutableStateOf(fareSettings.baseFare.toInt().toString()) }
    var perKmInput by remember(fareSettings) { mutableStateOf(fareSettings.perKmRate.toInt().toString()) }
    var perMinInput by remember(fareSettings) { mutableStateOf(fareSettings.perMinuteRate.toInt().toString()) }
    var minFareInput by remember(fareSettings) { mutableStateOf(fareSettings.minimumFare.toInt().toString()) }
    var waitingChargeInput by remember(fareSettings) { mutableStateOf(fareSettings.waitingCharge.toInt().toString()) }
    var addChargesInput by remember(fareSettings) { mutableStateOf(fareSettings.additionalCharges.toInt().toString()) }
    var cancelFeeInput by remember(fareSettings) { mutableStateOf(fareSettings.cancellationFee.toInt().toString()) }
    var commissionInput by remember(fareSettings) { mutableStateOf(fareSettings.platformCommissionRate.toInt().toString()) }

    var testDistance by remember { mutableStateOf("5") }
    var testDuration by remember { mutableStateOf("15") }
    var testWaiting by remember { mutableStateOf("3") }

    var saveSuccess by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

    val calculatedPreview = remember(baseFareInput, perKmInput, perMinInput, minFareInput, waitingChargeInput, testDistance, testDuration, testWaiting) {
        val base = baseFareInput.toDoubleOrNull() ?: 20.0
        val perKm = perKmInput.toDoubleOrNull() ?: 10.0
        val perMin = perMinInput.toDoubleOrNull() ?: 1.0
        val minFare = minFareInput.toDoubleOrNull() ?: 25.0
        val waitCharge = waitingChargeInput.toDoubleOrNull() ?: 2.0

        val dist = testDistance.toDoubleOrNull() ?: 5.0
        val dur = testDuration.toDoubleOrNull() ?: 15.0
        val wait = testWaiting.toDoubleOrNull() ?: 0.0

        val computed = base + (dist * perKm) + (dur * perMin) + (wait * waitCharge)
        maxOf(minFare, computed)
    }

    val commRate = commissionInput.toDoubleOrNull() ?: 10.0
    val platformShare = (calculatedPreview * commRate) / 100.0
    val driverNet = calculatedPreview - platformShare

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Fare Configuration & Pricing Engine", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CharcoalDark)
                if (!isEditing) {
                    Button(
                        onClick = { isEditing = true; saveSuccess = false; saveError = null },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit Pricing", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Fare Settings Form / Display Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Configured Base Rates (Applies to all new bookings)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CharcoalDark)

                    // Row 1: Base Fare & Per KM Rate
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = baseFareInput,
                            onValueChange = { baseFareInput = it; saveSuccess = false },
                            label = { Text("Base Fare (₹)") },
                            enabled = isEditing,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = perKmInput,
                            onValueChange = { perKmInput = it; saveSuccess = false },
                            label = { Text("Per KM Rate (₹)") },
                            enabled = isEditing,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    // Row 2: Per Minute & Min Fare
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = perMinInput,
                            onValueChange = { perMinInput = it; saveSuccess = false },
                            label = { Text("Per Minute Rate (₹)") },
                            enabled = isEditing,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = minFareInput,
                            onValueChange = { minFareInput = it; saveSuccess = false },
                            label = { Text("Minimum Fare (₹)") },
                            enabled = isEditing,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    // Row 3: Waiting Charge & Cancellation Fee
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = waitingChargeInput,
                            onValueChange = { waitingChargeInput = it; saveSuccess = false },
                            label = { Text("Waiting Charge (₹/min)") },
                            enabled = isEditing,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = cancelFeeInput,
                            onValueChange = { cancelFeeInput = it; saveSuccess = false },
                            label = { Text("Cancellation Fee (₹)") },
                            enabled = isEditing,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    // Row 4: Platform Commission Rate (%)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = commissionInput,
                            onValueChange = { commissionInput = it; saveSuccess = false },
                            label = { Text("Platform Commission (%)") },
                            enabled = isEditing,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = addChargesInput,
                            onValueChange = { addChargesInput = it; saveSuccess = false },
                            label = { Text("Tolls / Addl (₹)") },
                            enabled = isEditing,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    if (isEditing) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    baseFareInput = fareSettings.baseFare.toInt().toString()
                                    perKmInput = fareSettings.perKmRate.toInt().toString()
                                    perMinInput = fareSettings.perMinuteRate.toInt().toString()
                                    minFareInput = fareSettings.minimumFare.toInt().toString()
                                    waitingChargeInput = fareSettings.waitingCharge.toInt().toString()
                                    cancelFeeInput = fareSettings.cancellationFee.toInt().toString()
                                    commissionInput = fareSettings.platformCommissionRate.toInt().toString()
                                    isEditing = false
                                    saveError = null
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Cancel")
                            }

                            Button(
                                onClick = {
                                    if (session.admin.role == AdminRole.SUPPORT) {
                                        saveError = "Support staff cannot modify fare settings."
                                        return@Button
                                    }
                                    val b = baseFareInput.toDoubleOrNull() ?: 20.0
                                    val km = perKmInput.toDoubleOrNull() ?: 10.0
                                    val minRate = perMinInput.toDoubleOrNull() ?: 1.0
                                    val minF = minFareInput.toDoubleOrNull() ?: 25.0
                                    val wait = waitingChargeInput.toDoubleOrNull() ?: 2.0
                                    val add = addChargesInput.toDoubleOrNull() ?: 0.0
                                    val c = cancelFeeInput.toDoubleOrNull() ?: 15.0
                                    val comm = commissionInput.toDoubleOrNull() ?: 10.0

                                    RideRepository.updateFareSettings(b, km, minRate, minF, wait, add, c, comm)
                                    isEditing = false
                                    saveSuccess = true
                                    saveError = null
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Save & Publish", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (saveSuccess) {
                        Text("✓ Fare settings published successfully across the network.", color = BrandGreenDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    if (saveError != null) {
                        Text(saveError!!, color = RedCancel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Live Interactive Fare Preview Simulator
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BrandGreenLight)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Interactive Fare Simulator & Driver Earning Calculator", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandGreenDark)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = testDistance,
                            onValueChange = { testDistance = it },
                            label = { Text("Dist (km)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = testDuration,
                            onValueChange = { testDuration = it },
                            label = { Text("Time (min)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = testWaiting,
                            onValueChange = { testWaiting = it },
                            label = { Text("Wait (min)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    HorizontalDivider(color = BrandGreen.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Estimated Passenger Fare:", fontSize = 12.sp, color = CharcoalDark)
                            Text("₹${calculatedPreview.toInt()}", fontWeight = FontWeight.Black, fontSize = 22.sp, color = BrandGreenDark)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Admin (${commRate.toInt()}%): ₹${platformShare.toInt()}", fontSize = 11.sp, color = RedCancel, fontWeight = FontWeight.SemiBold)
                            Text("Driver 90%: ₹${driverNet.toInt()}", fontSize = 13.sp, color = BrandGreenDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
