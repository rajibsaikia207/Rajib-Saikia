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
fun AdminComplaintTab(
    complaints: List<ComplaintRecord>,
    session: AdminSession
) {
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, OPEN, IN_REVIEW, RESOLVED, REJECTED
    var selectedComplaint by remember { mutableStateOf<ComplaintRecord?>(null) }
    var resolutionNotesInput by remember { mutableStateOf("") }
    var updateStatusTarget by remember { mutableStateOf<ComplaintStatus?>(null) }

    val filtered = remember(complaints, selectedFilter) {
        when (selectedFilter) {
            "OPEN" -> complaints.filter { it.status == ComplaintStatus.OPEN }
            "IN_REVIEW" -> complaints.filter { it.status == ComplaintStatus.IN_REVIEW }
            "RESOLVED" -> complaints.filter { it.status == ComplaintStatus.RESOLVED }
            "REJECTED" -> complaints.filter { it.status == ComplaintStatus.REJECTED }
            else -> complaints
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Passenger & Driver Support Ticket Desk", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CharcoalDark)
        }

        // Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("ALL", "OPEN", "IN_REVIEW", "RESOLVED", "REJECTED").forEach { f ->
                    val count = when (f) {
                        "OPEN" -> complaints.count { it.status == ComplaintStatus.OPEN }
                        "IN_REVIEW" -> complaints.count { it.status == ComplaintStatus.IN_REVIEW }
                        "RESOLVED" -> complaints.count { it.status == ComplaintStatus.RESOLVED }
                        "REJECTED" -> complaints.count { it.status == ComplaintStatus.REJECTED }
                        else -> complaints.size
                    }
                    FilterChip(
                        selected = selectedFilter == f,
                        onClick = { selectedFilter = f },
                        label = { Text("$f ($count)", fontSize = 11.sp) },
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
                Text("No support tickets matching selected filter.", color = SlateGray, fontSize = 13.sp)
            }
        } else {
            items(filtered) { c ->
                val statusColor = when (c.status) {
                    ComplaintStatus.OPEN -> RedCancel
                    ComplaintStatus.IN_REVIEW -> ElectricAmber
                    ComplaintStatus.RESOLVED -> BrandGreenDark
                    ComplaintStatus.REJECTED -> SlateGray
                    else -> SlateGray
                }
                val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.ENGLISH).format(Date(c.createdAt))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(c.subject, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CharcoalDark)
                            Surface(shape = RoundedCornerShape(4.dp), color = statusColor.copy(alpha = 0.12f)) {
                                Text(
                                    text = c.status.name,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = statusColor
                                )
                            }
                        }

                        Text("Category: ${c.category.name} • Priority: ${c.priority} • Ticket: ${c.id}", fontSize = 11.sp, color = SlateGray)
                        Text(c.description, fontSize = 13.sp, color = CharcoalDark)

                        HorizontalDivider(color = CardBorder, thickness = 0.5.dp)

                        Text("Reporter: ${c.reporterName} (${c.reporterRole.name})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CharcoalDark)
                        if (c.driverName != null) {
                            Text("Linked Driver: ${c.driverName} (${c.driverId})", fontSize = 11.sp, color = SlateGray)
                        }
                        if (c.rideId != null) {
                            Text("Linked Ride: ${c.rideId}", fontSize = 11.sp, color = BlueInfo)
                        }
                        if (c.resolutionNotes != null) {
                            Text("Resolution Notes: ${c.resolutionNotes}", fontSize = 11.sp, color = BrandGreenDark, fontWeight = FontWeight.Medium)
                        }
                        Text("Submitted: $dateStr", fontSize = 10.sp, color = SlateGray)

                        // Action button
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = {
                                    selectedComplaint = c
                                    resolutionNotesInput = c.resolutionNotes ?: ""
                                    updateStatusTarget = c.status
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Manage Ticket", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedComplaint != null) {
        val c = selectedComplaint!!
        AlertDialog(
            onDismissRequest = { selectedComplaint = null },
            title = { Text("Resolve Ticket ${c.id}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Subject: ${c.subject}", fontWeight = FontWeight.Bold)
                    Text("Details: ${c.description}", fontSize = 12.sp)

                    Text("Update Ticket Status:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ComplaintStatus.values().forEach { st ->
                            FilterChip(
                                selected = updateStatusTarget == st,
                                onClick = { updateStatusTarget = st },
                                label = { Text(st.name, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BrandGreen, selectedLabelColor = Color.White)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = resolutionNotesInput,
                        onValueChange = { resolutionNotesInput = it },
                        label = { Text("Resolution Notes / Reply") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newStatus = updateStatusTarget ?: c.status
                        RideRepository.updateComplaintStatus(c.id, newStatus, resolutionNotesInput)
                        selectedComplaint = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                ) {
                    Text("Save Resolution")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedComplaint = null }) { Text("Cancel") }
            }
        )
    }
}
