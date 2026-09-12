package com.example.ui.screens.driver

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.BookingRingtone
import com.example.data.models.Driver
import com.example.data.models.DriverStatus
import com.example.data.repository.RideRepository
import com.example.ui.theme.*
import com.example.util.RideAlertManager

@Composable
fun DriverProfileTabContent(
    driver: Driver,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    val context = LocalContext.current
    var showEditDialog by remember { mutableStateOf(false) }
    var showRingtoneDialog by remember { mutableStateOf(false) }
    var tempSelectedRingtone by remember(driver.bookingRingtone) { mutableStateOf(driver.bookingRingtone) }
    var testSoundMessage by remember { mutableStateOf<String?>(null) }
    var previewingRingtoneId by remember { mutableStateOf<String?>(null) }

    // Edit form states
    var editName by remember(driver) { mutableStateOf(driver.name) }
    var editPhone by remember(driver) { mutableStateOf(driver.phone) }
    var editVehicleType by remember(driver) { mutableStateOf(driver.vehicleType) }
    var editVehicleModel by remember(driver) { mutableStateOf(driver.vehicleModel) }
    var editVehicleNumber by remember(driver) { mutableStateOf(driver.rickshawRegNo) }
    var editRcDetails by remember(driver) { mutableStateOf(driver.rcDetails) }
    var editLicenseNo by remember(driver) { mutableStateOf(driver.licenseNo ?: "") }
    var editLicenseDetails by remember(driver) { mutableStateOf(driver.licenseDetails ?: "") }
    var editServiceCity by remember(driver) { mutableStateOf(driver.serviceCity) }
    var editError by remember { mutableStateOf<String?>(null) }

    val serviceAreas = listOf("Tezpur", "Biswanath Chariali", "Nagsankar")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Credentials Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(driver.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = CharcoalDark)
                            Text("Driver Partner ID: ${driver.id}", fontSize = 12.sp, color = SlateGray)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (driver.status == DriverStatus.APPROVED) BrandGreenLight else ElectricAmberLight
                        ) {
                            Text(
                                text = driver.status.name.replace('_', ' '),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (driver.status == DriverStatus.APPROVED) BrandGreenDark else ElectricAmber
                            )
                        }
                    }

                    HorizontalDivider(color = LightSlate)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Mobile Number", fontSize = 13.sp, color = SlateGray)
                        Text(driver.phone, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CharcoalDark)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Vehicle Model", fontSize = 13.sp, color = SlateGray)
                        Text("${driver.vehicleType} • ${driver.vehicleModel}", fontSize = 13.sp, color = CharcoalDark)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Registration Number", fontSize = 13.sp, color = SlateGray)
                        Text(driver.rickshawRegNo, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandGreenDark)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("RC Details", fontSize = 13.sp, color = SlateGray)
                        Text(driver.rcDetails, fontSize = 13.sp, color = CharcoalDark)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Driving Licence", fontSize = 13.sp, color = SlateGray)
                        Text(
                            text = if (!driver.licenseNo.isNullOrBlank()) "${driver.licenseNo} (${driver.licenseDetails ?: "Valid"})" else "Not provided (Optional)",
                            fontSize = 13.sp,
                            fontWeight = if (!driver.licenseNo.isNullOrBlank()) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (!driver.licenseNo.isNullOrBlank()) BrandGreenDark else SlateGray
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Primary Service Area", fontSize = 13.sp, color = SlateGray)
                        Text("${driver.serviceCity} (30 km operating zone)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CharcoalDark)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Completed Trips", fontSize = 13.sp, color = SlateGray)
                        Text("${driver.totalRides} Trips • ⭐ ${driver.rating}", fontSize = 13.sp, color = SlateGray)
                    }
                }
            }
        }

        // Edit Profile Button
        item {
            Button(
                onClick = { showEditDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Profile Details", fontWeight = FontWeight.Bold)
            }
        }

        // SETTINGS -> BOOKING NOTIFICATIONS -> BOOKING RINGTONE
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            tint = BrandGreenDark,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text("Settings", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CharcoalDark)
                            Text("Booking Notifications", fontSize = 12.sp, color = SlateGray)
                        }
                    }

                    HorizontalDivider(color = LightSlate)

                    Text(
                        text = "BOOKING RINGTONE",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = BrandGreenDark,
                        letterSpacing = 0.8.sp
                    )

                    // 🔔 Booking Ringtone [ ON / OFF ]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (driver.bookingRingtoneEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                contentDescription = null,
                                tint = if (driver.bookingRingtoneEnabled) BrandGreenDark else SlateGray,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    "🔔 Booking Ringtone",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = CharcoalDark
                                )
                                Text(
                                    text = if (driver.bookingRingtoneEnabled) {
                                        "When ON: New ride requests will play the selected booking ringtone."
                                    } else {
                                        "When OFF: New ride requests will NOT play the booking ringtone, but the driver should still receive the booking notification according to Android notification settings."
                                    },
                                    fontSize = 11.sp,
                                    color = SlateGray,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        Switch(
                            checked = driver.bookingRingtoneEnabled,
                            onCheckedChange = { enabled ->
                                RideRepository.updateDriverRingtoneSettings(
                                    driverId = driver.id,
                                    enabled = enabled,
                                    ringtoneId = driver.bookingRingtone
                                )
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = BrandGreen,
                                uncheckedThumbColor = SlateGray,
                                uncheckedTrackColor = SlateLight
                            )
                        )
                    }

                    // Ringtone Selection Row
                    val currentRingtoneObj = BookingRingtone.fromId(driver.bookingRingtone)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (driver.bookingRingtoneEnabled) SlateLight.copy(alpha = 0.5f) else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, CardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                tempSelectedRingtone = driver.bookingRingtone
                                showRingtoneDialog = true
                            }
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
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = BrandGreenDark,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        "Booking Ringtone",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = CharcoalDark
                                    )
                                    Text(
                                        "${currentRingtoneObj.title} (${currentRingtoneObj.description})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = BrandGreenDark
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = BrandGreenLight
                            ) {
                                Text(
                                    "Change",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandGreenDark
                                )
                            }
                        }
                    }

                    // TEST BOOKING SOUND Button
                    Button(
                        onClick = {
                            testSoundMessage = null
                            RideAlertManager.testBookingSound(context, driver.bookingRingtone) {
                                testSoundMessage = "Booking ringtone is working."
                            }
                            testSoundMessage = "Booking ringtone is working."
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreenDark)
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("🔊 TEST BOOKING SOUND", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    if (testSoundMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BrandGreenLight,
                            border = BorderStroke(1.dp, BrandGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BrandGreenDark, modifier = Modifier.size(18.dp))
                                Text(
                                    testSoundMessage!!,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = BrandGreenDark
                                )
                            }
                        }
                    }

                    val isSoundBlocked = RideAlertManager.isDeviceSoundBlocked(context)
                    if (isSoundBlocked) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ElectricAmberLight,
                            border = BorderStroke(1.dp, ElectricAmber),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.VolumeOff, contentDescription = null, tint = ElectricAmber, modifier = Modifier.size(18.dp))
                                Text(
                                    "Your phone's sound or notification settings may prevent the booking ringtone from playing.",
                                    fontSize = 11.sp,
                                    color = CharcoalDark,
                                    lineHeight = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Switch / Exit Action
        item {
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CharcoalDark),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Switch / Log In Another Driver", fontWeight = FontWeight.SemiBold)
            }
        }

        // Delete Account Action
        item {
            OutlinedButton(
                onClick = onDeleteAccount,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RedCancel),
                border = BorderStroke(1.dp, RedCancel)
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete Driver Account", fontWeight = FontWeight.Bold)
            }
        }
    }

    // SELECT BOOKING RINGTONE DIALOG
    if (showRingtoneDialog) {
        AlertDialog(
            onDismissRequest = {
                RideAlertManager.stopPreview()
                previewingRingtoneId = null
                showRingtoneDialog = false
            },
            title = {
                Text("SELECT BOOKING RINGTONE", fontWeight = FontWeight.Black, fontSize = 16.sp, color = CharcoalDark)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Choose the ringtone tone that plays when a new ride request arrives:",
                        fontSize = 12.sp,
                        color = SlateGray
                    )

                    BookingRingtone.entries.forEach { ringtone ->
                        val isSelected = tempSelectedRingtone == ringtone.id
                        val isThisPreviewing = previewingRingtoneId == ringtone.id

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) BrandGreenLight else Color.White,
                            border = BorderStroke(1.dp, if (isSelected) BrandGreen else CardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    tempSelectedRingtone = ringtone.id
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { tempSelectedRingtone = ringtone.id },
                                        colors = RadioButtonDefaults.colors(selectedColor = BrandGreenDark)
                                    )
                                    Column {
                                        Text(
                                            ringtone.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = CharcoalDark
                                        )
                                        Text(
                                            ringtone.description,
                                            fontSize = 11.sp,
                                            color = SlateGray
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        if (isThisPreviewing) {
                                            RideAlertManager.stopPreview()
                                            previewingRingtoneId = null
                                        } else {
                                            previewingRingtoneId = ringtone.id
                                            RideAlertManager.previewRingtone(context, ringtone.id) {
                                                if (previewingRingtoneId == ringtone.id) {
                                                    previewingRingtoneId = null
                                                }
                                            }
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (isThisPreviewing) RedCancel else BrandGreen),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = if (isThisPreviewing) RedCancel else BrandGreenDark
                                    )
                                ) {
                                    Icon(
                                        imageVector = if (isThisPreviewing) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        if (isThisPreviewing) "Stop" else "Preview",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        RideAlertManager.stopPreview()
                        previewingRingtoneId = null
                        RideRepository.updateDriverRingtoneSettings(
                            driverId = driver.id,
                            enabled = driver.bookingRingtoneEnabled,
                            ringtoneId = tempSelectedRingtone
                        )
                        showRingtoneDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                ) {
                    Text("SAVE RINGTONE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        RideAlertManager.stopPreview()
                        previewingRingtoneId = null
                        showRingtoneDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // EDIT PROFILE DIALOG
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Driver Profile", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Full Legal Name *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = editPhone,
                            onValueChange = { editPhone = it },
                            label = { Text("Mobile Phone *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = editVehicleModel,
                            onValueChange = { editVehicleModel = it },
                            label = { Text("E-Rickshaw Model *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = editVehicleNumber,
                            onValueChange = { editVehicleNumber = it },
                            label = { Text("Registration Number *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = editRcDetails,
                            onValueChange = { editRcDetails = it },
                            label = { Text("RC Details *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = editLicenseNo,
                            onValueChange = { editLicenseNo = it },
                            label = { Text("Driving Licence (Optional)") },
                            placeholder = { Text("Leave blank if none") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    item {
                        Text("Service Area (30 km Radius)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = CharcoalDark)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            serviceAreas.forEach { area ->
                                FilterChip(
                                    selected = editServiceCity == area,
                                    onClick = { editServiceCity = area },
                                    label = { Text(area, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    if (editError != null) {
                        item {
                            Text(editError!!, color = RedCancel, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isBlank() || editPhone.isBlank() || editVehicleModel.isBlank() || editVehicleNumber.isBlank()) {
                            editError = "Please fill in all required fields."
                            return@Button
                        }
                        RideRepository.updateDriverProfile(
                            driverId = driver.id,
                            name = editName,
                            phone = editPhone,
                            vehicleType = editVehicleType,
                            vehicleModel = editVehicleModel,
                            vehicleNumber = editVehicleNumber,
                            rcDetails = editRcDetails,
                            licenseNo = editLicenseNo.ifBlank { null },
                            licenseDetails = editLicenseDetails.ifBlank { null },
                            serviceCity = editServiceCity
                        )
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
