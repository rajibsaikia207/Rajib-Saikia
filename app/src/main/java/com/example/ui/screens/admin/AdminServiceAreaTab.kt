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

@Composable
fun AdminServiceAreaTab(
    serviceAreas: List<ServiceArea>,
    session: AdminSession
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingArea by remember { mutableStateOf<ServiceArea?>(null) }
    var deleteConfirmArea by remember { mutableStateOf<ServiceArea?>(null) }

    var nameInput by remember { mutableStateOf("") }
    var districtInput by remember { mutableStateOf("") }
    var latInput by remember { mutableStateOf("26.6338") }
    var lngInput by remember { mutableStateOf("92.7926") }
    var radiusInput by remember { mutableStateOf("30") }

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
                Column {
                    Text("Assam Operational Service Areas", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CharcoalDark)
                    Text("Geofenced E-Rickshaw operating hubs (30 km radius)", fontSize = 12.sp, color = SlateGray)
                }
                Button(
                    onClick = {
                        nameInput = ""
                        districtInput = ""
                        latInput = "26.6338"
                        lngInput = "92.7926"
                        radiusInput = "30"
                        showAddDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Area", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(serviceAreas) { area ->
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
                            Text(area.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CharcoalDark)
                            Text(area.district, fontSize = 12.sp, color = SlateGray)
                        }
                        Switch(
                            checked = area.isActive,
                            onCheckedChange = { active ->
                                if (session.admin.role != AdminRole.SUPPORT) {
                                    RideRepository.toggleServiceArea(area.id, active)
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = BrandGreen)
                        )
                    }

                    HorizontalDivider(color = CardBorder, thickness = 0.6.dp)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Center GPS:", fontSize = 12.sp, color = SlateGray)
                        Text("${area.latitude}, ${area.longitude}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = CharcoalDark)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Operating Radius:", fontSize = 12.sp, color = SlateGray)
                        Text("${area.radiusKm.toInt()} KM", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandGreenDark)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Dispatch Status:", fontSize = 12.sp, color = SlateGray)
                        Text(
                            text = if (area.isActive) "Active (Accepting Bookings)" else "Suspended",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (area.isActive) BrandGreenDark else RedCancel
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                nameInput = area.name
                                districtInput = area.district
                                latInput = area.latitude.toString()
                                lngInput = area.longitude.toString()
                                radiusInput = area.radiusKm.toInt().toString()
                                editingArea = area
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit", fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { deleteConfirmArea = area },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RedCancel),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    // Add Area Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Operational Service Area", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Area / Town Name (e.g. Tezpur)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = districtInput,
                        onValueChange = { districtInput = it },
                        label = { Text("District (e.g. Sonitpur)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = latInput,
                            onValueChange = { latInput = it },
                            label = { Text("Latitude") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = lngInput,
                            onValueChange = { lngInput = it },
                            label = { Text("Longitude") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = radiusInput,
                        onValueChange = { radiusInput = it },
                        label = { Text("Radius (KM)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nameInput.isNotBlank() && districtInput.isNotBlank()) {
                            val lat = latInput.toDoubleOrNull() ?: 26.6338
                            val lng = lngInput.toDoubleOrNull() ?: 92.7926
                            val rad = radiusInput.toDoubleOrNull() ?: 30.0
                            RideRepository.addServiceArea(nameInput, districtInput, lat, lng, rad, true)
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                ) {
                    Text("Add Area")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Edit Area Dialog
    if (editingArea != null) {
        val area = editingArea!!
        AlertDialog(
            onDismissRequest = { editingArea = null },
            title = { Text("Edit Service Area ${area.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Area Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = districtInput,
                        onValueChange = { districtInput = it },
                        label = { Text("District") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = latInput,
                            onValueChange = { latInput = it },
                            label = { Text("Latitude") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = lngInput,
                            onValueChange = { lngInput = it },
                            label = { Text("Longitude") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = radiusInput,
                        onValueChange = { radiusInput = it },
                        label = { Text("Radius (KM)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val lat = latInput.toDoubleOrNull() ?: area.latitude
                        val lng = lngInput.toDoubleOrNull() ?: area.longitude
                        val rad = radiusInput.toDoubleOrNull() ?: area.radiusKm
                        RideRepository.updateServiceArea(area.id, nameInput, districtInput, lat, lng, rad, area.isActive)
                        editingArea = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingArea = null }) { Text("Cancel") }
            }
        )
    }

    // Delete Confirmation Dialog
    if (deleteConfirmArea != null) {
        val area = deleteConfirmArea!!
        AlertDialog(
            onDismissRequest = { deleteConfirmArea = null },
            title = { Text("Delete Service Area", fontWeight = FontWeight.Bold, color = RedCancel) },
            text = { Text("Are you sure you want to delete service area '${area.name}' (${area.district})?") },
            confirmButton = {
                Button(
                    onClick = {
                        RideRepository.deleteServiceArea(area.id)
                        deleteConfirmArea = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedCancel)
                ) {
                    Text("Delete Area")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmArea = null }) { Text("Cancel") }
            }
        )
    }
}
