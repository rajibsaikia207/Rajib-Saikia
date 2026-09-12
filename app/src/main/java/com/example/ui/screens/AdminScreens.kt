package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.models.*
import com.example.data.repository.RideRepository
import com.example.ui.components.HeaderAppBar
import com.example.ui.screens.admin.*
import com.example.ui.theme.*

@Composable
fun AdminDashboardScreen(
    onBackToPortal: () -> Unit
) {
    val currentSession by RideRepository.currentAdminSession.collectAsStateWithLifecycle()

    if (currentSession == null) {
        AdminLoginView(onBack = onBackToPortal)
    } else {
        AdminMainControlPanelView(
            session = currentSession!!,
            onBackToPortal = onBackToPortal
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminLoginView(onBack: () -> Unit) {
    var identifier by remember { mutableStateOf("admin@eride3.in") }
    var password by remember { mutableStateOf("admin123") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            HeaderAppBar(
                title = "E-Ride 3 Admin Web Panel",
                subtitle = "Assam Central Operations & Fleet Control",
                onBack = onBack
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = BrandGreenLight,
                        modifier = Modifier.size(60.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Admin Login",
                                tint = BrandGreenDark,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Text(
                        text = "Admin Web Panel Login",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = CharcoalDark
                    )
                    Text(
                        text = "Sign in to manage passengers, drivers, rides, automated commission, settlements and withdrawals.",
                        fontSize = 12.sp,
                        color = SlateGray,
                        lineHeight = 16.sp
                    )

                    OutlinedTextField(
                        value = identifier,
                        onValueChange = { identifier = it; errorMessage = null },
                        label = { Text("Admin Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = BrandGreenDark) }
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMessage = null },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = BrandGreenDark) }
                    )

                    if (errorMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = RedCancel.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, RedCancel.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = RedCancel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            isLoading = true
                            val result = RideRepository.loginAdmin(identifier, password)
                            if (result.isFailure) {
                                errorMessage = result.exceptionOrNull()?.message ?: "Invalid admin credentials. Please try again."
                            }
                            isLoading = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (isLoading) "Authenticating..." else "Access Admin Panel",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BackgroundLight,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Default Demo Credentials:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CharcoalDark)
                            Text("Email: admin@eride3.in • Password: admin123", fontSize = 11.sp, color = SlateGray)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminMainControlPanelView(
    session: AdminSession,
    onBackToPortal: () -> Unit
) {
    var currentSection by remember { mutableStateOf(AdminNavSection.DASHBOARD) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    // Observe Repository StateFlows
    val rides by RideRepository.ridesList.collectAsStateWithLifecycle()
    val drivers by RideRepository.driversList.collectAsStateWithLifecycle()
    val passengers by RideRepository.passengersList.collectAsStateWithLifecycle()
    val withdrawals by RideRepository.withdrawalRequests.collectAsStateWithLifecycle()
    val settlements by RideRepository.settlementsList.collectAsStateWithLifecycle()
    val notifications by RideRepository.adminNotifications.collectAsStateWithLifecycle()
    val driverWallets by RideRepository.driverWallets.collectAsStateWithLifecycle()
    val commissionLedger by RideRepository.commissionLedger.collectAsStateWithLifecycle()
    val transactions by RideRepository.financialTransactions.collectAsStateWithLifecycle()
    val fareSettings by RideRepository.fareSettings.collectAsStateWithLifecycle()
    val serviceAreas by RideRepository.serviceAreas.collectAsStateWithLifecycle()
    val complaints by RideRepository.complaintsList.collectAsStateWithLifecycle()
    val auditLogs by RideRepository.auditLogs.collectAsStateWithLifecycle()

    val pendingDriversCount = drivers.count { it.status == DriverStatus.PENDING_APPROVAL }
    val pendingWithdrawalsCount = withdrawals.count { it.status == WithdrawalStatus.PENDING }
    val pendingSettlementsCount = settlements.count { it.status == SettlementStatus.PENDING || it.status == SettlementStatus.VERIFIED }
    val unreadNotificationsCount = notifications.count { !it.isRead }
    val openComplaintsCount = complaints.count { it.status == ComplaintStatus.OPEN || it.status == ComplaintStatus.IN_REVIEW }

    Scaffold(
        topBar = {
            Column {
                // Main Admin Top Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = BrandGreenDark,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                modifier = Modifier.size(36.dp),
                                shadowElevation = 1.dp
                            ) {
                                Box(
                                    modifier = Modifier.padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_eride_rickshaw),
                                        contentDescription = "E-Ride 3",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "E-Ride 3 Admin Web Panel",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Assam Central Operational Fleet Desk",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White.copy(alpha = 0.18f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color(0xFF86EFAC), CircleShape)
                                    )
                                    Text(
                                        text = session.admin.name.split(" ").firstOrNull() ?: "Admin",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            IconButton(
                                onClick = { showLogoutConfirm = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = "Log Out",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                // 15-Item Horizontal Scrollable Navigation Bar
                AdminHorizontalNavBar(
                    selectedSection = currentSection,
                    onSelectSection = { section ->
                        if (section == AdminNavSection.LOG_OUT) {
                            showLogoutConfirm = true
                        } else {
                            currentSection = section
                        }
                    },
                    pendingDriversCount = pendingDriversCount,
                    pendingWithdrawalsCount = pendingWithdrawalsCount,
                    pendingSettlementsCount = pendingSettlementsCount,
                    unreadNotificationsCount = unreadNotificationsCount,
                    openComplaintsCount = openComplaintsCount
                )
            }
        },
        containerColor = BackgroundLight
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (currentSection) {
                AdminNavSection.DASHBOARD -> AdminDashboardTab(
                    rides = rides,
                    drivers = drivers,
                    passengers = passengers,
                    withdrawals = withdrawals,
                    settlements = settlements,
                    driverWallets = driverWallets,
                    currentSession = session,
                    onNavigate = { currentSection = it }
                )
                AdminNavSection.PASSENGERS -> AdminPassengersView(
                    passengers = passengers,
                    rides = rides
                )
                AdminNavSection.DRIVERS -> AdminDriversView(
                    drivers = drivers,
                    driverWallets = driverWallets,
                    session = session
                )
                AdminNavSection.RIDES -> AdminRidesView(
                    rides = rides
                )
                AdminNavSection.PAYMENTS -> AdminPaymentsView(
                    transactions = transactions
                )
                AdminNavSection.COMMISSION -> AdminCommissionTab(
                    commissionLedger = commissionLedger,
                    rides = rides,
                    drivers = drivers,
                    driverWallets = driverWallets,
                    currentCommissionRate = RideRepository.commissionRatePercentage,
                    session = session
                )
                AdminNavSection.SETTLEMENTS -> AdminSettlementsTab(
                    settlements = settlements,
                    session = session
                )
                AdminNavSection.REPORTS -> AdminReportsTab(
                    rides = rides,
                    drivers = drivers,
                    passengers = passengers
                )
                AdminNavSection.NOTIFICATIONS -> AdminNotificationsTab(
                    notifications = notifications,
                    onNavigate = { currentSection = it }
                )
                AdminNavSection.SETTINGS -> AdminSettingsTab(
                    session = session,
                    auditLogs = auditLogs
                )
                AdminNavSection.WITHDRAW -> AdminWithdrawTab(
                    withdrawals = withdrawals,
                    driverWallets = driverWallets,
                    session = session
                )
                AdminNavSection.FARE_SETTING -> AdminFareSettingsTab(
                    fareSettings = fareSettings,
                    session = session
                )
                AdminNavSection.SERVICE_AREA -> AdminServiceAreaTab(
                    serviceAreas = serviceAreas,
                    session = session
                )
                AdminNavSection.COMPLAINT -> AdminComplaintTab(
                    complaints = complaints,
                    session = session
                )
                AdminNavSection.LOG_OUT -> {
                    // Handled via modal dialog
                }
            }
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Confirm Admin Logout", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to end your administration session and return to the main portal?") },
            confirmButton = {
                Button(
                    onClick = {
                        RideRepository.logoutAdmin()
                        showLogoutConfirm = false
                        onBackToPortal()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedCancel)
                ) {
                    Text("Log Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
