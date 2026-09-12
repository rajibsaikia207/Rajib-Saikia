package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.UserRole
import com.example.data.repository.RideRepository
import com.example.ui.components.HeaderAppBar
import com.example.ui.theme.*

@Composable
fun HelpScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var complaintSubmitted by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            HeaderAppBar(
                title = "Help & Support",
                subtitle = "E-Ride 3 Assam Helpline",
                onBack = onBack
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Emergency Contacts
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Emergency Contacts (Assam)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CharcoalDark)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("National Emergency", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Police, Fire & Ambulance: 112", fontSize = 11.sp, color = SlateGray)
                        }
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112")).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RedCancel),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Dial 112", color = Color.White, fontSize = 12.sp)
                        }
                    }

                    HorizontalDivider(color = LightSlate)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Women Helpline", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Assam State: 1091", fontSize = 11.sp, color = SlateGray)
                        }
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:1091")).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Dial 1091", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Lodge Complaint or Inquiry
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Contact E-Ride 3 Support", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CharcoalDark)

                    if (complaintSubmitted) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = BrandGreenLight,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Your support inquiry has been submitted. Our Assam operations team will review it shortly.",
                                modifier = Modifier.padding(12.dp),
                                color = BrandGreenDark,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = subject,
                            onValueChange = { subject = it },
                            label = { Text("Subject") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Describe your issue...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            shape = RoundedCornerShape(10.dp)
                        )

                        Button(
                            onClick = {
                                if (subject.isNotBlank() && description.isNotBlank()) {
                                    RideRepository.submitComplaint(
                                        rideId = null,
                                        reporterId = "USER_APP",
                                        reporterName = "App User",
                                        reporterRole = UserRole.PASSENGER,
                                        subject = subject,
                                        description = description
                                    )
                                    complaintSubmitted = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Submit Support Request", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

