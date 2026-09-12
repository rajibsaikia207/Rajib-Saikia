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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.*
import com.example.data.repository.RideRepository
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminSettingsTab(
    session: AdminSession,
    auditLogs: List<AdminAuditLog>
) {
    var oldPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var passError by remember { mutableStateOf<String?>(null) }
    var passSuccess by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Admin Staff Settings & Security Controls", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CharcoalDark)
        }

        // Active Admin Profile Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Admin Staff Profile", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CharcoalDark)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Name:", fontSize = 13.sp, color = SlateGray)
                        Text(session.admin.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CharcoalDark)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Email:", fontSize = 13.sp, color = SlateGray)
                        Text(session.admin.email, fontSize = 13.sp, color = CharcoalDark)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Role:", fontSize = 13.sp, color = SlateGray)
                        Text(session.admin.role.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandGreenDark)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Admin ID:", fontSize = 13.sp, color = SlateGray)
                        Text(session.admin.id, fontSize = 13.sp, color = SlateGray)
                    }
                }
            }
        }

        // Password Change Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Security Credentials / Change Password", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CharcoalDark)

                    OutlinedTextField(
                        value = oldPass,
                        onValueChange = { oldPass = it; passError = null; passSuccess = false },
                        label = { Text("Current Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it; passError = null; passSuccess = false },
                        label = { Text("New Password (min 6 chars)") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = confirmPass,
                        onValueChange = { confirmPass = it; passError = null; passSuccess = false },
                        label = { Text("Confirm New Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Button(
                        onClick = {
                            if (newPass.length < 6) {
                                passError = "Password must be at least 6 characters."
                                return@Button
                            }
                            if (newPass != confirmPass) {
                                passError = "New passwords do not match."
                                return@Button
                            }
                            val success = RideRepository.changeAdminPassword(oldPass, newPass)
                            if (success) {
                                passSuccess = true
                                oldPass = ""
                                newPass = ""
                                confirmPass = ""
                                passError = null
                            } else {
                                passError = "Incorrect current password."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Update Password", fontWeight = FontWeight.Bold)
                    }

                    if (passSuccess) {
                        Text("✓ Password updated successfully.", color = BrandGreenDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    if (passError != null) {
                        Text(passError!!, color = RedCancel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Platform System Specifications
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Platform Operational Parameters", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CharcoalDark)
                    Text("• App Name: E-Ride 3 (E-Rickshaw Booking & Driver Platform)", fontSize = 12.sp, color = SlateGray)
                    Text("• Dispatch Engine: Intelligent Proximity & Battery Matching", fontSize = 12.sp, color = SlateGray)
                    Text("• Location Accuracy: Assam Multi-tier Sub-district & Village Hierarchy", fontSize = 12.sp, color = SlateGray)
                    Text("• Payment Modes: Cash on Ride Completion & Direct UPI", fontSize = 12.sp, color = SlateGray)
                    Text("• Commission Automation: Instant 10% Auto-Ledger Debit", fontSize = 12.sp, color = SlateGray)
                }
            }
        }

        // Audit Logs
        item {
            Text("System Audit Trail (${auditLogs.size} Events)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CharcoalDark)
        }

        items(auditLogs.take(15)) { log ->
            val logDate = SimpleDateFormat("dd MMM, HH:mm:ss", Locale.ENGLISH).format(Date(log.timestamp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(log.action, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BrandGreenDark)
                        Text(logDate, fontSize = 10.sp, color = SlateGray)
                    }
                    Text("By: ${log.adminName} • Target: ${log.targetType} [${log.targetId}]", fontSize = 11.sp, color = SlateGray)
                    if (log.newValue != null) {
                        Text("Value: ${log.newValue}", fontSize = 11.sp, color = CharcoalDark)
                    }
                }
            }
        }
    }
}
