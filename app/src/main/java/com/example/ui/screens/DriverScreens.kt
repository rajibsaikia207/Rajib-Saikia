package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.models.*
import com.example.data.repository.RideRepository
import com.example.ui.components.HeaderAppBar
import com.example.ui.components.MapViewComponent
import com.example.ui.components.RideStatusBadge
import com.example.ui.theme.*
import com.example.util.LocationHelper
import com.example.util.RideAlertManager
import com.example.ui.screens.driver.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Driver Authentication Screen: Directly presents the Driver Registration Form
 * with options for existing drivers to log in.
 */
enum class DriverAuthStep {
    PHONE_INPUT,
    OTP_VERIFY,
    DRIVER_DETAILS_FORM,
    STATUS_BLOCKED
}

/**
 * Driver Authentication Screen:
 * Implements Mobile Number -> Send OTP -> Enter OTP [ _ _ _ _ _ _ ] -> Verify OTP.
 * If Existing Driver: Loads existing driver account and navigates based on approval status (APPROVED / PENDING_APPROVAL / REJECTED / SUSPENDED).
 * If New Driver: Prompts for Driver Details Form (Name, Verified Mobile, Vehicle Details, Registration Details, Service Area, and OPTIONAL Driving Licence).
 * New drivers are saved with PENDING_APPROVAL status and cannot receive rides until approved.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverAuthScreen(
    onLoginSuccess: () -> Unit,
    onPendingApproval: () -> Unit,
    onBackToPortal: () -> Unit
) {
    var currentStep by remember { mutableStateOf(DriverAuthStep.PHONE_INPUT) }
    var showRegistrationSuccessDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Phone & OTP state
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var isSendingOtp by remember { mutableStateOf(false) }
    var isVerifyingOtp by remember { mutableStateOf(false) }
    var resendTimer by remember { mutableIntStateOf(30) }
    var canResend by remember { mutableStateOf(false) }

    // Blocked status state (for REJECTED / SUSPENDED)
    var blockedStatusTitle by remember { mutableStateOf("") }
    var blockedStatusMessage by remember { mutableStateOf("") }
    var blockedDriverName by remember { mutableStateOf("") }

    // Registration Form State (for New Drivers)
    var fullName by remember { mutableStateOf("") }
    var vehicleType by remember { mutableStateOf("Passenger E-Rickshaw (3-Wheeler)") }
    var vehicleModel by remember { mutableStateOf("Mayuri Deluxe Electric") }
    var vehicleNumber by remember { mutableStateOf("") }
    var rcDetails by remember { mutableStateOf("Commercial RC Valid (Sonitpur RTO)") }
    var licenseNumber by remember { mutableStateOf("") } // OPTIONAL
    var licenseDetails by remember { mutableStateOf("") } // OPTIONAL
    var selectedServiceCity by remember { mutableStateOf("Tezpur") }

    // Field-level error messages
    var fullNameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var vehicleModelError by remember { mutableStateOf<String?>(null) }
    var vehicleNumberError by remember { mutableStateOf<String?>(null) }
    var rcDetailsError by remember { mutableStateOf<String?>(null) }
    var generalError by remember { mutableStateOf<String?>(null) }
    var successNotice by remember { mutableStateOf<String?>(null) }

    // GPS check state for registration
    var isCheckingGps by remember { mutableStateOf(false) }
    var gpsVerifiedInfo by remember { mutableStateOf<String?>(null) }
    var gpsError by remember { mutableStateOf<String?>(null) }

    val serviceAreas = listOf("Tezpur", "Biswanath Chariali", "Nagsankar")
    val latestDemoOtp by RideRepository.latestDemoOtp.collectAsStateWithLifecycle()

    // Resend countdown timer
    LaunchedEffect(currentStep, resendTimer) {
        if (currentStep == DriverAuthStep.OTP_VERIFY && resendTimer > 0) {
            delay(1000L)
            resendTimer -= 1
        } else if (resendTimer == 0) {
            canResend = true
        }
    }

    // Registration Success Confirmation Dialog
    if (showRegistrationSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showRegistrationSuccessDialog = false
                onPendingApproval()
            },
            shape = RoundedCornerShape(18.dp),
            containerColor = Color.White,
            icon = {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(BrandGreenLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = BrandGreen,
                        modifier = Modifier.size(36.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Registration Submitted",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = CharcoalDark,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = "Your driver registration has been submitted and is waiting for Admin approval. You cannot receive ride requests until an admin verifies your details.",
                    fontSize = 14.sp,
                    color = SlateGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRegistrationSuccessDialog = false
                        onPendingApproval()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                ) {
                    Text("VIEW APPROVAL STATUS", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            HeaderAppBar(
                title = when (currentStep) {
                    DriverAuthStep.PHONE_INPUT -> "Driver Partner Login"
                    DriverAuthStep.OTP_VERIFY -> "Verify Driver OTP"
                    DriverAuthStep.DRIVER_DETAILS_FORM -> "Driver Registration"
                    DriverAuthStep.STATUS_BLOCKED -> "Account Status"
                },
                subtitle = when (currentStep) {
                    DriverAuthStep.PHONE_INPUT -> "Enter registered or new mobile number"
                    DriverAuthStep.OTP_VERIFY -> "Enter 6-digit OTP sent to your phone"
                    DriverAuthStep.DRIVER_DETAILS_FORM -> "Enter your vehicle & service area details"
                    DriverAuthStep.STATUS_BLOCKED -> "Driver verification notice"
                },
                onBack = {
                    when (currentStep) {
                        DriverAuthStep.PHONE_INPUT -> onBackToPortal()
                        DriverAuthStep.OTP_VERIFY -> {
                            currentStep = DriverAuthStep.PHONE_INPUT
                            otpCode = ""
                            generalError = null
                        }
                        DriverAuthStep.DRIVER_DETAILS_FORM -> {
                            currentStep = DriverAuthStep.OTP_VERIFY
                            generalError = null
                        }
                        DriverAuthStep.STATUS_BLOCKED -> {
                            currentStep = DriverAuthStep.PHONE_INPUT
                            generalError = null
                        }
                    }
                }
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Step Indicator Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                modifier = Modifier.size(38.dp),
                                shadowElevation = 1.dp
                            ) {
                                Box(
                                    modifier = Modifier.padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_eride_rickshaw),
                                        contentDescription = "E-Ride 3 Logo",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = when (currentStep) {
                                        DriverAuthStep.PHONE_INPUT -> "Step 1 of 2: Driver Mobile"
                                        DriverAuthStep.OTP_VERIFY -> "Step 2 of 2: OTP Verification"
                                        DriverAuthStep.DRIVER_DETAILS_FORM -> "New Driver Registration"
                                        DriverAuthStep.STATUS_BLOCKED -> "Account Notice"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = CharcoalDark
                                )
                                Text(
                                    text = "E-Ride 3 Partner Portal • Assam EV Fleet",
                                    fontSize = 11.sp,
                                    color = SlateGray
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = BrandGreenLight
                        ) {
                            Text(
                                text = "DRIVER",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandGreenDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Error / Success Feedback Banners
            if (generalError != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = RedCancelLight,
                        border = BorderStroke(1.dp, RedCancel.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = RedCancel, modifier = Modifier.size(20.dp))
                            Text(text = generalError!!, color = RedCancel, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            if (successNotice != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = BrandGreenLight,
                        border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BrandGreenDark, modifier = Modifier.size(20.dp))
                            Text(text = successNotice!!, color = BrandGreenDark, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // STEP 1: DRIVER MOBILE NUMBER INPUT
            // -------------------------------------------------------------
            if (currentStep == DriverAuthStep.PHONE_INPUT) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Enter your 10-digit mobile number",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = CharcoalDark
                            )
                            Text(
                                text = "Existing drivers will be logged in immediately. New drivers can register after mobile OTP verification.",
                                fontSize = 12.sp,
                                color = SlateGray,
                                lineHeight = 16.sp
                            )

                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = {
                                    val digitsOnly = it.filter { char -> char.isDigit() }.take(10)
                                    phoneNumber = digitsOnly
                                    phoneError = null
                                    generalError = null
                                },
                                label = { Text("Driver Mobile Number") },
                                placeholder = { Text("94350 67890") },
                                prefix = {
                                    Text(
                                        text = "🇮🇳 +91 ",
                                        fontWeight = FontWeight.Bold,
                                        color = CharcoalDark,
                                        fontSize = 15.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = BrandGreen)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Button(
                                onClick = {
                                    generalError = null
                                    if (phoneNumber.length != 10) {
                                        generalError = "Please enter a valid 10-digit Indian mobile number."
                                        return@Button
                                    }
                                    isSendingOtp = true
                                    val (success, message) = RideRepository.sendOtp(phoneNumber, UserRole.DRIVER)
                                    isSendingOtp = false
                                    if (success) {
                                        successNotice = message
                                        resendTimer = 30
                                        canResend = false
                                        currentStep = DriverAuthStep.OTP_VERIFY
                                    } else {
                                        generalError = message
                                    }
                                },
                                enabled = phoneNumber.length == 10 && !isSendingOtp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                            ) {
                                if (isSendingOtp) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sending OTP...", fontSize = 14.sp)
                                } else {
                                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("SEND OTP", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }

                // Quick Demo Driver Accounts Selection
                item {
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
                            Text(
                                text = "Quick Demo Driver Accounts (Click to autofill)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = CharcoalDark
                            )

                            val driversList by RideRepository.driversList.collectAsStateWithLifecycle()
                            driversList.take(4).forEach { driver ->
                                OutlinedButton(
                                    onClick = {
                                        phoneNumber = driver.phone.replace("+91", "").replace(" ", "").trim().takeLast(10)
                                        generalError = null
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CharcoalDark),
                                    border = BorderStroke(1.dp, CardBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(horizontalAlignment = Alignment.Start) {
                                            Text(driver.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("${driver.phone} • ${driver.serviceCity} • ${driver.rickshawRegNo}", fontSize = 11.sp, color = SlateGray)
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (driver.status == DriverStatus.APPROVED) BrandGreenLight else ElectricAmberLight
                                        ) {
                                            Text(
                                                text = driver.status.name.replace('_', ' '),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (driver.status == DriverStatus.APPROVED) BrandGreenDark else ElectricAmber
                                            )
                                        }
                                    }
                                }
                            }

                            // New driver registration demo
                            OutlinedButton(
                                onClick = {
                                    phoneNumber = "94351${(10000..99999).random()}"
                                    generalError = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CharcoalDark),
                                border = BorderStroke(1.dp, CardBorder)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = Alignment.Start) {
                                        Text("✨ New Driver Partner Mobile", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Tests OTP verification + Driver Registration Form", fontSize = 11.sp, color = SlateGray)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = ElectricAmberLight
                                    ) {
                                        Text(
                                            text = "New Driver",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ElectricAmber
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // STEP 2: DRIVER OTP VERIFICATION
            // -------------------------------------------------------------
            if (currentStep == DriverAuthStep.OTP_VERIFY) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Enter Driver OTP", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CharcoalDark)
                                    Text(
                                        text = "Verification code sent to +91 $phoneNumber",
                                        fontSize = 12.sp,
                                        color = SlateGray
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        currentStep = DriverAuthStep.PHONE_INPUT
                                        otpCode = ""
                                        generalError = null
                                    }
                                ) {
                                    Text("Edit Number", color = BrandGreenDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // OTP Test Helper Banner
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = BrandGreenSurface,
                                border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("TESTING DRIVER OTP", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateGray)
                                        Text(
                                            text = latestDemoOtp ?: "123456",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 18.sp,
                                            color = BrandGreenDark,
                                            letterSpacing = 2.sp
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            otpCode = latestDemoOtp ?: "123456"
                                            generalError = null
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreenLight),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Auto-Fill OTP", color = BrandGreenDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // 6-Digit OTP Text Field
                            OutlinedTextField(
                                value = otpCode,
                                onValueChange = {
                                    val digitsOnly = it.filter { char -> char.isDigit() }.take(6)
                                    otpCode = digitsOnly
                                    generalError = null
                                },
                                label = { Text("6-Digit OTP Code") },
                                placeholder = { Text("• • • • • •") },
                                leadingIcon = {
                                    Icon(Icons.Default.Key, contentDescription = null, tint = BrandGreen)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Verify Button
                            Button(
                                onClick = {
                                    generalError = null
                                    if (otpCode.length != 6) {
                                        generalError = "Please enter the complete 6-digit OTP code."
                                        return@Button
                                    }

                                    isVerifyingOtp = true
                                    when (val result = RideRepository.verifyDriverOtp(phoneNumber, otpCode)) {
                                        is DriverOtpResult.ExistingDriver -> {
                                            isVerifyingOtp = false
                                            val driver = result.driver
                                            when (driver.status) {
                                                DriverStatus.APPROVED -> {
                                                    onLoginSuccess()
                                                }
                                                DriverStatus.PENDING_APPROVAL -> {
                                                    onPendingApproval()
                                                }
                                                DriverStatus.REJECTED -> {
                                                    blockedDriverName = driver.name
                                                    blockedStatusTitle = "Driver Account Rejected"
                                                    blockedStatusMessage = "Your driver registration review was not approved by Admin. Please contact operations support for assistance."
                                                    currentStep = DriverAuthStep.STATUS_BLOCKED
                                                }
                                                DriverStatus.SUSPENDED -> {
                                                    blockedDriverName = driver.name
                                                    blockedStatusTitle = "Driver Account Suspended"
                                                    blockedStatusMessage = "Your driver account is temporarily suspended by Admin. Contact customer helpline."
                                                    currentStep = DriverAuthStep.STATUS_BLOCKED
                                                }
                                                DriverStatus.DELETED -> {
                                                    generalError = "This driver account has been deactivated."
                                                }
                                            }
                                        }
                                        is DriverOtpResult.NewDriver -> {
                                            isVerifyingOtp = false
                                            currentStep = DriverAuthStep.DRIVER_DETAILS_FORM
                                        }
                                        is DriverOtpResult.InvalidOtp -> {
                                            isVerifyingOtp = false
                                            generalError = result.message
                                        }
                                    }
                                },
                                enabled = otpCode.length == 6 && !isVerifyingOtp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                            ) {
                                if (isVerifyingOtp) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Verifying Driver...", fontSize = 14.sp)
                                } else {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("VERIFY OTP & CONTINUE", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }

                            // Resend OTP Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (canResend) {
                                    TextButton(
                                        onClick = {
                                            val (success, message) = RideRepository.sendOtp(phoneNumber, UserRole.DRIVER)
                                            if (success) {
                                                successNotice = message
                                                resendTimer = 30
                                                canResend = false
                                            } else {
                                                generalError = message
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, tint = BrandGreenDark, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Resend OTP Code", color = BrandGreenDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                } else {
                                    Text(
                                        text = "Resend OTP in ${resendTimer}s",
                                        fontSize = 12.sp,
                                        color = SlateGray
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // STEP 3: NEW DRIVER DETAILS FORM (After OTP Verification)
            // -------------------------------------------------------------
            if (currentStep == DriverAuthStep.DRIVER_DETAILS_FORM) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White,
                                    modifier = Modifier.size(40.dp),
                                    shadowElevation = 1.dp
                                ) {
                                    Box(
                                        modifier = Modifier.padding(4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.ic_eride_rickshaw),
                                            contentDescription = "E-Ride 3 Logo",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Driver Registration Form",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = CharcoalDark
                                    )
                                    Text(
                                        text = "Complete your profile to register as an E-Rickshaw Driver",
                                        fontSize = 12.sp,
                                        color = SlateGray
                                    )
                                }
                            }

                            // Verified Mobile Badge
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = BrandGreenLight,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = BrandGreenDark, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = "Verified Mobile: +91 $phoneNumber (OTP Verified ✓)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandGreenDark
                                    )
                                }
                            }

                            HorizontalDivider(color = LightSlate)

                            // 1. Full Name (Required)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "1. Full Name *",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = CharcoalDark
                                )
                                OutlinedTextField(
                                    value = fullName,
                                    onValueChange = {
                                        fullName = it
                                        fullNameError = null
                                        generalError = null
                                    },
                                    placeholder = { Text("e.g. Ramesh Chandra Das") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = BrandGreen)
                                    },
                                    singleLine = true,
                                    isError = fullNameError != null,
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                if (fullNameError != null) {
                                    Text(text = fullNameError!!, color = RedCancel, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
                                }
                            }

                            // 2. E-Rickshaw Vehicle Details (Required)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "2. E-Rickshaw Vehicle Details *",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = CharcoalDark
                                )
                                OutlinedTextField(
                                    value = vehicleType,
                                    onValueChange = { vehicleType = it },
                                    label = { Text("Vehicle Type") },
                                    placeholder = { Text("Passenger E-Rickshaw (3-Wheeler)") },
                                    leadingIcon = {
                                        Icon(Icons.Default.ElectricRickshaw, contentDescription = null, tint = BrandGreen)
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = vehicleModel,
                                    onValueChange = {
                                        vehicleModel = it
                                        vehicleModelError = null
                                        generalError = null
                                    },
                                    label = { Text("E-Rickshaw Make / Model *") },
                                    placeholder = { Text("e.g. Mayuri Deluxe Electric / Saarthi") },
                                    leadingIcon = {
                                        Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = BrandGreen)
                                    },
                                    singleLine = true,
                                    isError = vehicleModelError != null,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                if (vehicleModelError != null) {
                                    Text(text = vehicleModelError!!, color = RedCancel, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
                                }
                            }

                            // 3. E-Rickshaw Registration Details (Required)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "3. E-Rickshaw Registration Details *",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = CharcoalDark
                                )
                                OutlinedTextField(
                                    value = vehicleNumber,
                                    onValueChange = {
                                        vehicleNumber = it
                                        vehicleNumberError = null
                                        generalError = null
                                    },
                                    label = { Text("Registration / Vehicle Number *") },
                                    placeholder = { Text("e.g. AS-12-ER-4421") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Pin, contentDescription = null, tint = BrandGreen)
                                    },
                                    singleLine = true,
                                    isError = vehicleNumberError != null,
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                if (vehicleNumberError != null) {
                                    Text(text = vehicleNumberError!!, color = RedCancel, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
                                }

                                OutlinedTextField(
                                    value = rcDetails,
                                    onValueChange = {
                                        rcDetails = it
                                        rcDetailsError = null
                                        generalError = null
                                    },
                                    label = { Text("Registration / RC Details *") },
                                    placeholder = { Text("e.g. Commercial RC Valid (Sonitpur RTO)") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Description, contentDescription = null, tint = BrandGreen)
                                    },
                                    singleLine = true,
                                    isError = rcDetailsError != null,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                if (rcDetailsError != null) {
                                    Text(text = rcDetailsError!!, color = RedCancel, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
                                }
                            }

                            // 4. SERVICE AREA SELECTION (Required)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "4. Select Your Service Area *",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = CharcoalDark
                                )
                                Text(
                                    text = "Coverage zone: 30 KM radius from selected service area",
                                    fontSize = 11.sp,
                                    color = SlateGray
                                )

                                serviceAreas.forEach { areaName ->
                                    val isSelected = selectedServiceCity == areaName
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedServiceCity = areaName
                                                gpsError = null
                                            },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) BrandGreenLight else Color.White
                                        ),
                                        border = BorderStroke(1.5.dp, if (isSelected) BrandGreen else CardBorder)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                RadioButton(
                                                    selected = isSelected,
                                                    onClick = { selectedServiceCity = areaName }
                                                )
                                                Column {
                                                    Text(areaName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CharcoalDark)
                                                    Text("30 KM operating coverage zone", fontSize = 11.sp, color = SlateGray)
                                                }
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isSelected) BrandGreen else LightSlate
                                            ) {
                                                Text(
                                                    text = if (isSelected) "Selected" else "Available",
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Color.White else SlateGray
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 5. Driving Licence Details — OPTIONAL (Clearly Displayed)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = BackgroundLight),
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
                                        Text(
                                            text = "5. Driving Licence — Optional",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = CharcoalDark
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = BrandGreenLight
                                        ) {
                                            Text(
                                                text = "OPTIONAL",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BrandGreenDark
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Driving Licence is not mandatory. You can submit registration without licence details.",
                                        fontSize = 11.sp,
                                        color = SlateGray
                                    )

                                    OutlinedTextField(
                                        value = licenseNumber,
                                        onValueChange = { licenseNumber = it },
                                        label = { Text("Driving Licence Number (Optional)") },
                                        placeholder = { Text("e.g. AS-DL-2023-9988 (Leave blank if none)") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Badge, contentDescription = null, tint = SlateGray)
                                        },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    OutlinedTextField(
                                        value = licenseDetails,
                                        onValueChange = { licenseDetails = it },
                                        label = { Text("Licence Class / Details (Optional)") },
                                        placeholder = { Text("e.g. Commercial LMV / Transport") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Event, contentDescription = null, tint = SlateGray)
                                        },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }

                            // Optional GPS Verification
                            OutlinedButton(
                                onClick = {
                                    isCheckingGps = true
                                    gpsError = null
                                    gpsVerifiedInfo = null
                                    LocationHelper.getCurrentGpsCoordinates(
                                        context = context,
                                        onSuccess = { lat, lng ->
                                            isCheckingGps = false
                                            val validation = LocationHelper.validateServiceArea(lat, lng)
                                            if (validation.isValid) {
                                                selectedServiceCity = validation.nearestCentreName
                                                gpsVerifiedInfo = "GPS Verified: Within 30 km of ${validation.nearestCentreName} (${validation.distanceKm} km from centre)"
                                            } else {
                                                gpsError = "Sorry, E-Ride 3 is currently available only within 30 km of Tezpur, Biswanath Chariali and Nagsankar."
                                            }
                                        },
                                        onError = {
                                            isCheckingGps = false
                                            gpsVerifiedInfo = "GPS unavailable. Operating area: $selectedServiceCity"
                                        }
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandGreenDark),
                                border = BorderStroke(1.dp, BrandGreen)
                            ) {
                                if (isCheckingGps) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = BrandGreen)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Verifying Device Location...", fontSize = 12.sp)
                                } else {
                                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Verify Operating Zone with GPS (Optional)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            if (gpsVerifiedInfo != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = BrandGreenLight,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = gpsVerifiedInfo!!,
                                        color = BrandGreenDark,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(10.dp),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // SUBMIT BUTTON
                            Button(
                                onClick = {
                                    var hasError = false
                                    if (fullName.trim().length < 2) {
                                        fullNameError = "Please enter your full legal name."
                                        hasError = true
                                    }
                                    if (vehicleModel.isBlank()) {
                                        vehicleModelError = "Please enter E-Rickshaw Make/Model."
                                        hasError = true
                                    }
                                    if (vehicleNumber.isBlank()) {
                                        vehicleNumberError = "Please enter Vehicle Registration Number."
                                        hasError = true
                                    }
                                    if (rcDetails.isBlank()) {
                                        rcDetailsError = "Please enter Registration / RC Details."
                                        hasError = true
                                    }

                                    if (hasError) return@Button

                                    // Submit Registration (Driving Licence optional/nullable)
                                    when (val result = RideRepository.registerDriver(
                                        name = fullName,
                                        phone = phoneNumber,
                                        vehicleType = vehicleType,
                                        vehicleModel = vehicleModel,
                                        vehicleNumber = vehicleNumber,
                                        rcDetails = rcDetails,
                                        licenseNo = licenseNumber.ifBlank { null },
                                        licenseDetails = licenseDetails.ifBlank { null },
                                        serviceCity = selectedServiceCity
                                    )) {
                                        is DriverRegisterResult.Success -> {
                                            showRegistrationSuccessDialog = true
                                        }
                                        is DriverRegisterResult.DuplicatePhone -> {
                                            generalError = result.message
                                        }
                                        is DriverRegisterResult.InvalidInput -> {
                                            generalError = result.message
                                        }
                                        is DriverRegisterResult.LocationOutsideServiceArea -> {
                                            generalError = result.message
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                            ) {
                                Text("SUBMIT DRIVER REGISTRATION", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // STEP 4: STATUS BLOCKED (REJECTED / SUSPENDED NOTICES)
            // -------------------------------------------------------------
            if (currentStep == DriverAuthStep.STATUS_BLOCKED) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(RedCancelLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Block,
                                    contentDescription = null,
                                    tint = RedCancel,
                                    modifier = Modifier.size(34.dp)
                                )
                            }

                            Text(
                                text = blockedStatusTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = CharcoalDark,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "Driver Partner: $blockedDriverName",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = SlateGray,
                                textAlign = TextAlign.Center
                            )

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = RedCancelLight,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = blockedStatusMessage,
                                    color = RedCancel,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(14.dp),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                            }

                            Button(
                                onClick = {
                                    currentStep = DriverAuthStep.PHONE_INPUT
                                    phoneNumber = ""
                                    otpCode = ""
                                    generalError = null
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CharcoalDark)
                            ) {
                                Text("TRY ANOTHER MOBILE NUMBER", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Driver Pending Screen: Displayed when driver account status is PENDING_APPROVAL
 */
@Composable
fun DriverPendingApprovalScreen(
    onBackToPortal: () -> Unit,
    onSwitchAccount: () -> Unit,
    onApproved: () -> Unit
) {
    val currentDriver by RideRepository.currentDriver.collectAsStateWithLifecycle()
    val driversList by RideRepository.driversList.collectAsStateWithLifecycle()
    val latestDriver = driversList.find { it.id == currentDriver.id } ?: currentDriver

    // Check if status transitioned to APPROVED
    LaunchedEffect(latestDriver.status) {
        if (latestDriver.status == DriverStatus.APPROVED) {
            onApproved()
        }
    }

    Scaffold(
        topBar = {
            HeaderAppBar(
                title = "Driver Registration Status",
                subtitle = latestDriver.name,
                onBack = onBackToPortal
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (latestDriver.status) {
                            DriverStatus.APPROVED -> BrandGreenLight
                            DriverStatus.PENDING_APPROVAL -> ElectricAmberLight
                            DriverStatus.REJECTED -> RedCancelLight
                            DriverStatus.SUSPENDED -> Color(0xFFFEEBC8)
                            DriverStatus.DELETED -> LightSlate
                        }
                    ),
                    border = BorderStroke(
                        1.5.dp,
                        when (latestDriver.status) {
                            DriverStatus.APPROVED -> BrandGreen
                            DriverStatus.PENDING_APPROVAL -> ElectricAmber
                            DriverStatus.REJECTED -> RedCancel
                            DriverStatus.SUSPENDED -> Color(0xFFC05621)
                            DriverStatus.DELETED -> SlateGray
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = when (latestDriver.status) {
                                DriverStatus.APPROVED -> Icons.Default.CheckCircle
                                DriverStatus.PENDING_APPROVAL -> Icons.Default.HourglassTop
                                DriverStatus.REJECTED -> Icons.Default.Cancel
                                DriverStatus.SUSPENDED -> Icons.Default.Warning
                                DriverStatus.DELETED -> Icons.Default.Delete
                            },
                            contentDescription = null,
                            tint = when (latestDriver.status) {
                                DriverStatus.APPROVED -> BrandGreenDark
                                DriverStatus.PENDING_APPROVAL -> ElectricAmber
                                DriverStatus.REJECTED -> RedCancel
                                DriverStatus.SUSPENDED -> Color(0xFFC05621)
                                DriverStatus.DELETED -> SlateGray
                            },
                            modifier = Modifier.size(52.dp)
                        )

                        Text(
                            text = when (latestDriver.status) {
                                DriverStatus.APPROVED -> "Account Approved!"
                                DriverStatus.PENDING_APPROVAL -> "Registration Submitted"
                                DriverStatus.REJECTED -> "Registration Rejected"
                                DriverStatus.SUSPENDED -> "Account Suspended"
                                DriverStatus.DELETED -> "Account Deleted"
                            },
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = CharcoalDark
                        )

                        Text(
                            text = when (latestDriver.status) {
                                DriverStatus.APPROVED -> "Your driver partner account is approved and ready. You can now go online and receive rides."
                                DriverStatus.PENDING_APPROVAL -> "Your Driver registration has been submitted. E-Ride 3 Admin will verify your details before you can receive rides."
                                DriverStatus.REJECTED -> "Your registration was not approved during administrative verification. Please contact support helpline for details."
                                DriverStatus.SUSPENDED -> "Your driver account is temporarily suspended. Please contact E-Ride 3 support."
                                DriverStatus.DELETED -> "This driver profile is deactivated."
                            },
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            color = CharcoalDark,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Submitted Information Review Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Submitted Registration Information", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CharcoalDark)
                        HorizontalDivider(color = LightSlate)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Full Name:", fontSize = 12.sp, color = SlateGray)
                            Text(latestDriver.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CharcoalDark)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Mobile Number:", fontSize = 12.sp, color = SlateGray)
                            Text(latestDriver.phone, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CharcoalDark)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Vehicle Model:", fontSize = 12.sp, color = SlateGray)
                            Text("${latestDriver.vehicleType} • ${latestDriver.vehicleModel}", fontSize = 12.sp, color = CharcoalDark)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Vehicle Reg No:", fontSize = 12.sp, color = SlateGray)
                            Text(latestDriver.rickshawRegNo, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandGreenDark)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("RC Details:", fontSize = 12.sp, color = SlateGray)
                            Text(latestDriver.rcDetails, fontSize = 12.sp, color = CharcoalDark)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Driving Licence:", fontSize = 12.sp, color = SlateGray)
                            Text(
                                text = if (!latestDriver.licenseNo.isNullOrBlank()) latestDriver.licenseNo!! else "Not provided (Optional)",
                                fontSize = 12.sp,
                                fontWeight = if (!latestDriver.licenseNo.isNullOrBlank()) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (!latestDriver.licenseNo.isNullOrBlank()) BrandGreenDark else SlateGray
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Service Area (30 km):", fontSize = 12.sp, color = SlateGray)
                            Text(latestDriver.serviceCity, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CharcoalDark)
                        }
                    }
                }
            }

            // Quick Admin Approval Simulation for testing
            if (latestDriver.status == DriverStatus.PENDING_APPROVAL) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Admin Approval Action (Simulation)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CharcoalDark)
                            Text("For testing and verification, you can simulate Admin approval directly or from the Admin Console.", fontSize = 11.sp, color = SlateGray)
                            
                            Button(
                                onClick = {
                                    RideRepository.updateDriverStatus(latestDriver.id, DriverStatus.APPROVED)
                                    onApproved()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Approve My Registration (Simulation)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Switch / Log in another account
            item {
                OutlinedButton(
                    onClick = onSwitchAccount,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CharcoalDark),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Text("Switch / Log In Another Driver Account", fontWeight = FontWeight.SemiBold)
                }
            }

            // Back to Main Portal
            item {
                TextButton(
                    onClick = onBackToPortal,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back to Home Screen", color = SlateGray)
                }
            }
        }
    }
}

/**
 * Driver Home Screen: Main Dashboard for Approved Drivers
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverHomeScreen(
    onActiveRide: () -> Unit,
    onViewWallet: () -> Unit,
    onViewEarnings: () -> Unit,
    onViewProfile: () -> Unit,
    onBackToPortal: () -> Unit,
    onOpenAuth: () -> Unit
) {
    val currentDriver by RideRepository.currentDriver.collectAsStateWithLifecycle()
    val activeRide by RideRepository.activeRide.collectAsStateWithLifecycle()
    val incomingOffer by RideRepository.incomingOffer.collectAsStateWithLifecycle()
    val driverWallets by RideRepository.driverWallets.collectAsStateWithLifecycle()
    val wallet = driverWallets[currentDriver.id] ?: DriverWallet(driverId = currentDriver.id)
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(DriverTab.DASHBOARD) }

    var showWithdrawDialog by remember { mutableStateOf(false) }
    var showSettleDialog by remember { mutableStateOf(false) }
    var withdrawAmount by remember { mutableStateOf("") }
    var upiIdInput by remember { mutableStateOf("driver.eride3@oksbi") }
    var withdrawError by remember { mutableStateOf<String?>(null) }
    var withdrawSuccessMessage by remember { mutableStateOf<String?>(null) }
    var settleAmountInput by remember { mutableStateOf("") }
    var settleError by remember { mutableStateOf<String?>(null) }
    var settleSuccessMessage by remember { mutableStateOf<String?>(null) }

    var acceptErrorDialogMsg by remember { mutableStateOf<String?>(null) }

    // Alert sound when incoming offer arrives for an approved, online driver without active ride
    val isDriverEligibleForOffer = currentDriver.status == DriverStatus.APPROVED &&
            currentDriver.isOnline &&
            (activeRide == null || activeRide!!.driverId != currentDriver.id || activeRide!!.status !in listOf(RideStatus.DRIVER_ASSIGNED, RideStatus.DRIVER_ARRIVED, RideStatus.IN_PROGRESS))

    val isOfferActiveForDriver = incomingOffer != null &&
            isDriverEligibleForOffer &&
            !incomingOffer!!.rejectedDriverIds.contains(currentDriver.id) &&
            (incomingOffer!!.eligibleDriverIds.isEmpty() || incomingOffer!!.eligibleDriverIds.contains(currentDriver.id))

    // Periodically update driver GPS location while Online, Approved, and Available
    LaunchedEffect(currentDriver.id, currentDriver.isOnline, currentDriver.status) {
        if (currentDriver.isOnline && currentDriver.status == DriverStatus.APPROVED) {
            while (isActive) {
                if (LocationHelper.hasLocationPermission(context)) {
                    LocationHelper.getCurrentGpsCoordinates(
                        context = context,
                        onSuccess = { lat, lng ->
                            RideRepository.updateDriverLocation(currentDriver.id, lat, lng)
                        },
                        onError = { /* Location error ignored in background loop */ }
                    )
                }
                delay(15000L)
            }
        }
    }

    LaunchedEffect(isOfferActiveForDriver, incomingOffer?.rideId, currentDriver.bookingRingtoneEnabled, currentDriver.bookingRingtone) {
        if (isOfferActiveForDriver && incomingOffer != null) {
            RideAlertManager.startAlert(
                context = context,
                offer = incomingOffer,
                ringtoneEnabled = currentDriver.bookingRingtoneEnabled,
                ringtoneId = currentDriver.bookingRingtone
            )
        } else {
            RideAlertManager.stopAlert(context)
        }
    }

    // Auto-redirect if driver is in active ride
    LaunchedEffect(activeRide?.status, activeRide?.driverId) {
        if (activeRide != null && activeRide!!.driverId == currentDriver.id &&
            activeRide!!.status in listOf(RideStatus.DRIVER_ASSIGNED, RideStatus.DRIVER_ARRIVED, RideStatus.IN_PROGRESS)) {
            RideAlertManager.stopAlert(context)
            onActiveRide()
        }
    }

    Scaffold(
        topBar = {
            HeaderAppBar(
                title = when (selectedTab) {
                    DriverTab.DASHBOARD -> "E-Ride 3 Driver Partner"
                    DriverTab.WALLET -> "Driver Wallet & Payouts"
                    DriverTab.EARNINGS -> "Driver Earnings & Trips"
                    DriverTab.COMMISSION -> "Admin Commission (10%)"
                    DriverTab.PROFILE -> "Driver Profile & Settings"
                },
                subtitle = when (selectedTab) {
                    DriverTab.DASHBOARD -> "${currentDriver.serviceCity} (30 km zone) • ${currentDriver.rickshawRegNo}"
                    DriverTab.WALLET -> "Available: ₹${wallet.availableBalance.toInt()}"
                    DriverTab.EARNINGS -> "Assam Performance Record"
                    DriverTab.COMMISSION -> "Cash & Settlement Ledger"
                    DriverTab.PROFILE -> currentDriver.name
                },
                onBack = {
                    if (selectedTab != DriverTab.DASHBOARD) {
                        selectedTab = DriverTab.DASHBOARD
                    } else {
                        onBackToPortal()
                    }
                },
                actions = {
                    IconButton(onClick = { selectedTab = DriverTab.WALLET }) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Wallet", tint = Color.White)
                    }
                    IconButton(onClick = { selectedTab = DriverTab.PROFILE }) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile", tint = Color.White)
                    }
                }
            )
        },
        bottomBar = {
            DriverBottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                DriverTab.DASHBOARD -> {
                    DriverDashboardTab(
                        driver = currentDriver,
                        wallet = wallet,
                        onNavigateToTab = { selectedTab = it },
                        onOpenAuth = onOpenAuth,
                        onWithdrawClick = {
                            withdrawError = null
                            withdrawSuccessMessage = null
                            showWithdrawDialog = true
                        },
                        onBackToPortal = onBackToPortal
                    )
                }
                DriverTab.WALLET -> {
                    DriverWalletTabContent(
                        driver = currentDriver,
                        wallet = wallet,
                        onWithdrawClick = {
                            withdrawError = null
                            withdrawSuccessMessage = null
                            showWithdrawDialog = true
                        },
                        onSettleClick = {
                            settleError = null
                            settleSuccessMessage = null
                            settleAmountInput = wallet.commissionDue.toInt().toString()
                            showSettleDialog = true
                        }
                    )
                }
                DriverTab.EARNINGS -> {
                    DriverEarningsTabContent(
                        driver = currentDriver
                    )
                }
                DriverTab.COMMISSION -> {
                    DriverCommissionTab(
                        driver = currentDriver,
                        wallet = wallet,
                        onSettleClick = {
                            settleError = null
                            settleSuccessMessage = null
                            settleAmountInput = wallet.commissionDue.toInt().toString()
                            showSettleDialog = true
                        }
                    )
                }
                DriverTab.PROFILE -> {
                    DriverProfileTabContent(
                        driver = currentDriver,
                        onLogout = onBackToPortal,
                        onDeleteAccount = onViewProfile
                    )
                }
            }
        }
    }

    // WITHDRAWAL DIALOG & FLOW
    if (showWithdrawDialog) {
        DriverWithdrawalDialog(
            onDismiss = { showWithdrawDialog = false }
        )
    }

    // SETTLE COMMISSION DIALOG
    if (showSettleDialog) {
        AlertDialog(
            onDismissRequest = { showSettleDialog = false },
            title = { Text("Settle 10% Commission Due", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Total Commission Owed: ₹${wallet.commissionDue.toInt()}", fontWeight = FontWeight.Bold, color = RedCancel)
                    Text("Pay admin commission via UPI / Instant Banking to maintain active driver partner status.", fontSize = 12.sp, color = SlateGray)

                    OutlinedTextField(
                        value = settleAmountInput,
                        onValueChange = { settleAmountInput = it },
                        label = { Text("Settlement Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (settleError != null) {
                        Text(settleError!!, color = RedCancel, fontSize = 12.sp)
                    }
                    if (settleSuccessMessage != null) {
                        Text(settleSuccessMessage!!, color = BrandGreenDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = settleAmountInput.toDoubleOrNull()
                        if (amount == null || amount <= 0) {
                            settleError = "Please enter a valid amount."
                            return@Button
                        }
                        val success = RideRepository.settleCommissionDue(
                            driverId = currentDriver.id,
                            amount = amount
                        )
                        if (success) {
                            settleSuccessMessage = "Successfully settled ₹${amount.toInt()} commission."
                            settleError = null
                        } else {
                            settleError = "Failed to settle commission. Please check the amount."
                            settleSuccessMessage = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreenDark)
                ) {
                    Text("Pay & Settle")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // INCOMING RIDE OFFER POPUP ALERT (High-Priority Alert Screen)
    if (isOfferActiveForDriver && incomingOffer != null) {
        val offer = incomingOffer!!
        val remainingSeconds = maxOf(1, ((offer.expiresAtMillis - System.currentTimeMillis()) / 1000).toInt().coerceAtMost(10))
        var timeLeftSeconds by remember(offer.rideId, offer.currentStageRadiusKm) { mutableIntStateOf(remainingSeconds) }

        LaunchedEffect(offer.rideId, offer.currentStageRadiusKm) {
            while (timeLeftSeconds > 0) {
                delay(1000)
                timeLeftSeconds--
            }
            RideAlertManager.stopAlert(context)
            RideRepository.expireRideOfferForDriver(offer.rideId, currentDriver.id)
        }

        Dialog(
            onDismissRequest = {
                // Prevent accidental dismiss without explicit choice
            }
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(2.5.dp, BrandGreen),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header with Alert Badge & Live 30s Countdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = BrandGreenLight
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(BrandGreen)
                                )
                                Text(
                                    text = "NEW RIDE REQUEST",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = BrandGreenDark,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (timeLeftSeconds <= 10) Color(0xFFFFEBEE) else ElectricAmberLight
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (timeLeftSeconds <= 10) RedCancel else ElectricAmber
                                )
                                Text(
                                    text = "${timeLeftSeconds}s",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = if (timeLeftSeconds <= 10) RedCancel else ElectricAmber
                                )
                            }
                        }
                    }

                    // Progress Bar for stage timer
                    LinearProgressIndicator(
                        progress = { (timeLeftSeconds / 10f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (timeLeftSeconds <= 10) RedCancel else BrandGreen,
                        trackColor = LightSlate
                    )

                    // Estimated Fare and Driver Net Earnings Display
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = LightSlate,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "ESTIMATED FARE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateGray,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "₹${offer.fare.toInt()}",
                                fontWeight = FontWeight.Black,
                                fontSize = 34.sp,
                                color = BrandGreenDark
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                thickness = 1.dp,
                                color = Color(0xFFE2E8F0)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "ESTIMATED DRIVER EARNING",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SlateGray
                                    )
                                    Text(
                                        text = "₹${offer.driverEarning.toInt()}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = BrandGreen
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (offer.paymentMode == PaymentMode.CASH) Color(0xFFFFF3E0) else BrandGreenLight
                                ) {
                                    Text(
                                        text = if (offer.paymentMode == PaymentMode.CASH) "PAYMENT: CASH" else "PAYMENT: ONLINE",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        color = if (offer.paymentMode == PaymentMode.CASH) Color(0xFFE65100) else BrandGreenDark,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Admin Commission: ${offer.commissionRate.toInt()}% (₹${offer.commissionAmount.toInt()})",
                                fontSize = 10.sp,
                                color = SlateGray,
                                modifier = Modifier.align(Alignment.Start)
                            )
                        }
                    }

                    // Trip Route & Passenger Details (PII protected: Name only)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFAFAFA),
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Passenger Name
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = CharcoalDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Passenger: ${offer.passengerName}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = CharcoalDark
                                )
                            }

                            // Pickup Location
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.TripOrigin,
                                    contentDescription = null,
                                    tint = BrandGreen,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .padding(top = 2.dp)
                                )
                                Column {
                                    Text(
                                        text = "PICKUP LOCATION",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = SlateGray
                                    )
                                    Text(
                                        text = offer.pickupAddress.ifBlank { offer.pickupLocation },
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = CharcoalDark,
                                        lineHeight = 16.sp
                                    )
                                }
                            }

                            // Drop Location
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = RedCancel,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .padding(top = 2.dp)
                                )
                                Column {
                                    Text(
                                        text = "DROP LOCATION",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = SlateGray
                                    )
                                    Text(
                                        text = offer.dropoffAddress.ifBlank { offer.dropoffLocation },
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = CharcoalDark,
                                        lineHeight = 16.sp
                                    )
                                }
                            }

                            // Estimated Distance
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Route,
                                    contentDescription = null,
                                    tint = SlateGray,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "ESTIMATED DISTANCE: ${offer.distanceKm} km",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateGray
                                )
                            }
                        }
                    }

                    // Prominent Accept & Reject Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                RideAlertManager.stopAlert(context)
                                RideRepository.rejectRideOffer(offer.rideId, currentDriver.id)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RedCancel),
                            border = BorderStroke(1.5.dp, RedCancel)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "REJECT",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                        }

                        Button(
                            onClick = {
                                RideAlertManager.stopAlert(context)
                                val result = RideRepository.acceptRide(offer.rideId, currentDriver.id)
                                when (result) {
                                    is AcceptRideResult.Success -> {
                                        onActiveRide()
                                    }
                                    is AcceptRideResult.AlreadyAccepted -> {
                                        acceptErrorDialogMsg = result.message
                                    }
                                    is AcceptRideResult.CancelledByPassenger -> {
                                        acceptErrorDialogMsg = result.message
                                    }
                                    is AcceptRideResult.Expired -> {
                                        acceptErrorDialogMsg = result.message
                                    }
                                    is AcceptRideResult.DriverIneligible -> {
                                        acceptErrorDialogMsg = result.message
                                    }
                                    is AcceptRideResult.Error -> {
                                        acceptErrorDialogMsg = result.message
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "ACCEPT RIDE",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    // Acceptance Error or Conflict Dialog (e.g. Another Driver Accepted)
    if (acceptErrorDialogMsg != null) {
        AlertDialog(
            onDismissRequest = { acceptErrorDialogMsg = null },
            title = {
                Text(
                    text = "Booking Status",
                    fontWeight = FontWeight.Bold,
                    color = CharcoalDark
                )
            },
            text = {
                Text(
                    text = acceptErrorDialogMsg!!,
                    fontSize = 14.sp,
                    color = CharcoalDark
                )
            },
            confirmButton = {
                Button(
                    onClick = { acceptErrorDialogMsg = null },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                ) {
                    Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

/**
 * Driver Active Ride Screen: Live trip navigation, OTP validation, and completion
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverActiveRideScreen(
    onRideFinished: () -> Unit,
    onBack: () -> Unit
) {
    val activeRide by RideRepository.activeRide.collectAsStateWithLifecycle()
    val currentDriver by RideRepository.currentDriver.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var otpInput by remember { mutableStateOf("") }
    var otpError by remember { mutableStateOf<String?>(null) }
    var showCancelDialog by remember { mutableStateOf(false) }

    if (activeRide == null) {
        LaunchedEffect(Unit) {
            onRideFinished()
        }
        return
    }

    val ride = activeRide!!

    Scaffold(
        topBar = {
            HeaderAppBar(
                title = "Live Trip In Progress",
                subtitle = "Booking ID: ${ride.id}",
                onBack = onBack
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Map
            item {
                MapViewComponent(
                    pickupName = ride.pickupLocation,
                    dropoffName = ride.dropoffLocation,
                    status = ride.status,
                    driverName = currentDriver.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }

            // Passenger Info & Calling Card (Phone exposed ONLY during active ride)
            item {
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Passenger Details", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CharcoalDark)
                            RideStatusBadge(status = ride.status)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(BrandGreenLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = BrandGreenDark)
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ride.passengerName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = CharcoalDark
                                )
                                Text(
                                    text = ride.passengerPhone,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = BrandGreenDark
                                )
                                Text(
                                    text = "Payment: ${ride.paymentMode.name.replace('_', ' ')}",
                                    fontSize = 11.sp,
                                    color = SlateGray
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${ride.passengerPhone}")
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Call Passenger (${ride.passengerPhone})", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // Trip Locations Info
            item {
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
                        Text(text = "Route Directions", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CharcoalDark)
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.TripOrigin, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(18.dp))
                            Column {
                                Text(text = "Pickup Location", fontSize = 11.sp, color = SlateGray)
                                Text(text = "${ride.pickupLocation} - ${ride.pickupAddress}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        HorizontalDivider(color = LightSlate)
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = RedCancel, modifier = Modifier.size(18.dp))
                            Column {
                                Text(text = "Destination", fontSize = 11.sp, color = SlateGray)
                                Text(text = "${ride.dropoffLocation} - ${ride.dropoffAddress}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Action Steps (Arrived -> Verify OTP -> Complete)
            item {
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
                        when (ride.status) {
                            RideStatus.DRIVER_ASSIGNED -> {
                                Text(text = "Step 1: Head to pickup location", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Button(
                                    onClick = { RideRepository.driverArrived(ride.id) },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("I Have Arrived at Pickup", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            RideStatus.DRIVER_ARRIVED -> {
                                Text(text = "Step 2: Enter Passenger's 4-digit OTP", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                OutlinedTextField(
                                    value = otpInput,
                                    onValueChange = {
                                        if (it.length <= 4) {
                                            otpInput = it
                                            otpError = null
                                        }
                                    },
                                    placeholder = { Text("Enter 4-digit OTP") },
                                    isError = otpError != null,
                                    supportingText = { if (otpError != null) Text(otpError!!, color = RedCancel) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                Button(
                                    onClick = {
                                        val success = RideRepository.startRide(ride.id, otpInput)
                                        if (!success) {
                                            otpError = "Incorrect OTP. Please ask passenger."
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Verify OTP & Start Trip", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            RideStatus.IN_PROGRESS -> {
                                Text(text = "Step 3: Trip Completion & Payment Collection", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                
                                if (ride.paymentMode == PaymentMode.CASH) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = BrandYellow.copy(alpha = 0.15f)),
                                        border = BorderStroke(1.5.dp, BrandYellow)
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
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = BrandYellow
                                                ) {
                                                    Text(
                                                        text = "CASH TO COLLECT",
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 12.sp,
                                                        color = CharcoalDark
                                                    )
                                                }
                                                Text(
                                                    text = "₹${ride.fare.toInt()}",
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 24.sp,
                                                    color = CharcoalDark
                                                )
                                            }

                                            Divider(color = BrandYellow.copy(alpha = 0.5f))

                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Total Ride Fare (100%):", fontSize = 12.sp, color = CharcoalDark)
                                                Text("₹${ride.fare.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CharcoalDark)
                                            }
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Admin Commission (${ride.commissionRate.toInt()}%):", fontSize = 12.sp, color = SlateGray)
                                                Text("₹${ride.commissionAmount.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = RedCancel)
                                            }
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Driver Net Earning (90%):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandGreenDark)
                                                Text("₹${ride.driverEarning.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Black, color = BrandGreenDark)
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color.White.copy(alpha = 0.8f),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = "Collect full ₹${ride.fare.toInt()} cash from passenger. E-Ride 3 platform commission (₹${ride.commissionAmount.toInt()}) will be added to your Commission Ledger.",
                                                    modifier = Modifier.padding(8.dp),
                                                    fontSize = 11.sp,
                                                    color = CharcoalDark
                                                )
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            RideRepository.completeRide(ride.id, paymentCollected = true)
                                            onRideFinished()
                                        },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                                    ) {
                                        Icon(Icons.Default.DoneAll, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Confirm ₹${ride.fare.toInt()} Cash Collected & Finish", fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                } else {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = BrandGreenLight),
                                        border = BorderStroke(1.5.dp, BrandGreen)
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
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = BrandGreen
                                                ) {
                                                    Text(
                                                        text = "ONLINE PAYMENT — DO NOT COLLECT CASH",
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 11.sp,
                                                        color = Color.White
                                                    )
                                                }
                                                Text(
                                                    text = "PAID",
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 16.sp,
                                                    color = BrandGreenDark
                                                )
                                            }

                                            Divider(color = BrandGreen.copy(alpha = 0.3f))

                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Total Ride Fare Paid Online:", fontSize = 12.sp, color = CharcoalDark)
                                                Text("₹${ride.fare.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CharcoalDark)
                                            }
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Admin Commission (${ride.commissionRate.toInt()}%):", fontSize = 12.sp, color = SlateGray)
                                                Text("-₹${ride.commissionAmount.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = RedCancel)
                                            }
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Credit to Wallet Balance (90%):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandGreenDark)
                                                Text("+₹${ride.driverEarning.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Black, color = BrandGreenDark)
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color.White.copy(alpha = 0.8f),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = "Passenger has already paid ₹${ride.fare.toInt()} online. Net earning ₹${ride.driverEarning.toInt()} will be credited immediately to your Driver Wallet.",
                                                    modifier = Modifier.padding(8.dp),
                                                    fontSize = 11.sp,
                                                    color = CharcoalDark
                                                )
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            RideRepository.completeRide(ride.id, paymentCollected = true)
                                            onRideFinished()
                                        },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                                    ) {
                                        Icon(Icons.Default.DoneAll, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Complete Trip & Credit ₹${ride.driverEarning.toInt()}", fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }

                            else -> {}
                        }
                    }
                }
            }

            // Cancel Button for Driver
            if (ride.status in listOf(RideStatus.DRIVER_ASSIGNED, RideStatus.DRIVER_ARRIVED)) {
                item {
                    OutlinedButton(
                        onClick = { showCancelDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RedCancel),
                        border = BorderStroke(1.dp, RedCancel)
                    ) {
                        Text("Cancel Ride (Driver)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Ride?") },
            text = { Text("Are you sure you want to cancel this ride? Doing so will release the booking and revoke telephone contact.") },
            confirmButton = {
                Button(
                    onClick = {
                        RideRepository.cancelRide(ride.id, "Driver emergency cancel", "DRIVER")
                        showCancelDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedCancel)
                ) {
                    Text("Confirm Cancel", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Keep Ride")
                }
            }
        )
    }
}

/**
 * Driver Wallet Screen: Payouts and Balance Management
 */
@Composable
fun DriverWalletScreen(
    onBack: () -> Unit
) {
    val currentDriver by RideRepository.currentDriver.collectAsStateWithLifecycle()
    val driverWallets by RideRepository.driverWallets.collectAsStateWithLifecycle()
    val transactions by RideRepository.financialTransactions.collectAsStateWithLifecycle()
    val withdrawals by RideRepository.withdrawalRequests.collectAsStateWithLifecycle()

    val wallet = driverWallets[currentDriver.id] ?: DriverWallet(driverId = currentDriver.id)
    val driverTxns = transactions.filter { it.driverId == currentDriver.id }
    val (isScheduleOpen, scheduleMessage) = remember { RideRepository.isWithdrawalScheduleOpen() }

    var showWithdrawDialog by remember { mutableStateOf(false) }
    var showSettleDialog by remember { mutableStateOf(false) }
    var settleAmountInput by remember { mutableStateOf("") }
    var withdrawAmount by remember { mutableStateOf("") }
    var upiIdInput by remember { mutableStateOf("driver.eride3@oksbi") }
    var withdrawError by remember { mutableStateOf<String?>(null) }
    var withdrawSuccessMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            HeaderAppBar(
                title = "Driver Wallet & Payouts",
                subtitle = "Available Balance: ₹${wallet.availableBalance.toInt()}",
                onBack = onBack
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Schedule Announcement Banner
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isScheduleOpen) BrandGreenLight else RedCancel.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, if (isScheduleOpen) BrandGreen else RedCancel.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            if (isScheduleOpen) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isScheduleOpen) BrandGreenDark else RedCancel
                        )
                        Column {
                            Text(
                                text = if (isScheduleOpen) "Withdrawal Window OPEN" else "WITHDRAWAL CLOSED",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isScheduleOpen) BrandGreenDark else RedCancel
                            )
                            Text(
                                text = "Monday to Friday: 9:00 AM – 5:00 PM IST\nSaturday & Sunday: Closed",
                                fontSize = 11.sp,
                                color = CharcoalDark.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Main Balance Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandGreenDark)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Available Payout Balance", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                        Text("₹${wallet.availableBalance.toInt()}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 34.sp)

                        Divider(color = Color.White.copy(alpha = 0.2f))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Pending Balance", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                Text("₹${wallet.pendingBalance.toInt()}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Commission Due (Owed)", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                Text("₹${wallet.commissionDue.toInt()}", color = if (wallet.commissionDue > 0) BrandYellow else Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    withdrawError = null
                                    withdrawSuccessMessage = null
                                    showWithdrawDialog = true
                                },
                                enabled = isScheduleOpen && wallet.availableBalance > 0,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    disabledContainerColor = Color.White.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Request UPI Payout", color = BrandGreenDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            if (wallet.commissionDue > 0) {
                                OutlinedButton(
                                    onClick = {
                                        settleAmountInput = wallet.commissionDue.toInt().toString()
                                        showSettleDialog = true
                                    },
                                    border = BorderStroke(1.dp, Color.White),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Settle Commission", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Recent Transactions
            item {
                Text("Immutable Financial Ledger", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CharcoalDark)
            }

            if (driverTxns.isEmpty()) {
                item {
                    Text("No transactions recorded yet.", fontSize = 13.sp, color = SlateGray)
                }
            } else {
                items(driverTxns) { txn ->
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = when (txn.type) {
                                        TransactionType.RIDE_EARNING -> BrandGreenLight
                                        TransactionType.COMMISSION -> BrandYellow.copy(alpha = 0.2f)
                                        TransactionType.WITHDRAWAL -> SlateLight
                                        TransactionType.REFUND -> RedCancel.copy(alpha = 0.1f)
                                        TransactionType.ADJUSTMENT -> BrandGreenLight
                                    }
                                ) {
                                    Text(
                                        text = txn.type.name.replace("_", " "),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = CharcoalDark
                                    )
                                }

                                Text(
                                    text = if (txn.isCredit) "+₹${txn.amount.toInt()}" else "-₹${txn.amount.toInt()}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = if (txn.isCredit) BrandGreenDark else RedCancel
                                )
                            }

                            Text(txn.description, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = CharcoalDark)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(txn.transactionId, fontSize = 11.sp, color = SlateGray)
                                Text(
                                    text = txn.status.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (txn.status) {
                                        TransactionStatus.COMPLETED -> BrandGreenDark
                                        TransactionStatus.PENDING -> BrandYellowDark
                                        TransactionStatus.REVERSED -> RedCancel
                                        TransactionStatus.FAILED -> RedCancel
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showWithdrawDialog) {
        DriverWithdrawalDialog(
            onDismiss = { showWithdrawDialog = false }
        )
    }

    if (showSettleDialog) {
        AlertDialog(
            onDismissRequest = { showSettleDialog = false },
            title = { Text("Settle Commission Due") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Total Commission Due: ₹${wallet.commissionDue.toInt()}", fontSize = 13.sp, color = CharcoalDark, fontWeight = FontWeight.Bold)
                    Text("Pay your pending platform commission for cash rides to keep your account in good standing.", fontSize = 12.sp, color = SlateGray)
                    OutlinedTextField(
                        value = settleAmountInput,
                        onValueChange = { settleAmountInput = it },
                        label = { Text("Settlement Amount (₹)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = settleAmountInput.toDoubleOrNull() ?: 0.0
                        RideRepository.settleCommissionDue(currentDriver.id, amt)
                        showSettleDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                ) {
                    Text("Pay Commission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Driver Earnings Screen: Trip Logs and Performance
 */
@Composable
fun DriverEarningsScreen(
    onBack: () -> Unit
) {
    val currentDriver by RideRepository.currentDriver.collectAsStateWithLifecycle()
    val allRides by RideRepository.ridesList.collectAsStateWithLifecycle()
    val driverRides = allRides.filter { it.driverId == currentDriver.id }
    val summary = remember(allRides, currentDriver) {
        RideRepository.getDriverEarningsSummary(currentDriver.id)
    }

    Scaffold(
        topBar = {
            HeaderAppBar(
                title = "Driver Earnings & Trips",
                subtitle = "Assam Performance Record",
                onBack = onBack
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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

                        Divider(color = Color.White.copy(alpha = 0.2f))

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
                        Divider()
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
                    Text("No rides completed yet.", fontSize = 13.sp, color = SlateGray)
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
}

/**
 * Driver Profile Screen: View and Edit Profile Details
 */
@Composable
fun DriverProfileScreen(
    onDeleteAccount: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentDriver by RideRepository.currentDriver.collectAsStateWithLifecycle()
    var showEditDialog by remember { mutableStateOf(false) }
    var showRingtoneDialog by remember { mutableStateOf(false) }
    var tempSelectedRingtone by remember(currentDriver.bookingRingtone) { mutableStateOf(currentDriver.bookingRingtone) }
    var testSoundMessage by remember { mutableStateOf<String?>(null) }
    var previewingRingtoneId by remember { mutableStateOf<String?>(null) }

    // Edit form states
    var editName by remember(currentDriver) { mutableStateOf(currentDriver.name) }
    var editPhone by remember(currentDriver) { mutableStateOf(currentDriver.phone) }
    var editVehicleType by remember(currentDriver) { mutableStateOf(currentDriver.vehicleType) }
    var editVehicleModel by remember(currentDriver) { mutableStateOf(currentDriver.vehicleModel) }
    var editVehicleNumber by remember(currentDriver) { mutableStateOf(currentDriver.rickshawRegNo) }
    var editRcDetails by remember(currentDriver) { mutableStateOf(currentDriver.rcDetails) }
    var editLicenseNo by remember(currentDriver) { mutableStateOf(currentDriver.licenseNo ?: "") }
    var editLicenseDetails by remember(currentDriver) { mutableStateOf(currentDriver.licenseDetails ?: "") }
    var editServiceCity by remember(currentDriver) { mutableStateOf(currentDriver.serviceCity) }
    var editError by remember { mutableStateOf<String?>(null) }

    val serviceAreas = listOf("Tezpur", "Biswanath Chariali", "Nagsankar")

    Scaffold(
        topBar = {
            HeaderAppBar(
                title = "Driver Profile & Credentials",
                subtitle = currentDriver.name,
                onBack = onBack
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                                Text(currentDriver.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = CharcoalDark)
                                Text("Driver Partner ID: ${currentDriver.id}", fontSize = 12.sp, color = SlateGray)
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (currentDriver.status == DriverStatus.APPROVED) BrandGreenLight else ElectricAmberLight
                            ) {
                                Text(
                                    text = currentDriver.status.name.replace('_', ' '),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentDriver.status == DriverStatus.APPROVED) BrandGreenDark else ElectricAmber
                                )
                            }
                        }

                        HorizontalDivider(color = LightSlate)

                        // Key Profile Info
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Mobile Number", fontSize = 13.sp, color = SlateGray)
                            Text(currentDriver.phone, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CharcoalDark)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Vehicle Model", fontSize = 13.sp, color = SlateGray)
                            Text("${currentDriver.vehicleType} • ${currentDriver.vehicleModel}", fontSize = 13.sp, color = CharcoalDark)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Registration Number", fontSize = 13.sp, color = SlateGray)
                            Text(currentDriver.rickshawRegNo, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandGreenDark)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("RC Details", fontSize = 13.sp, color = SlateGray)
                            Text(currentDriver.rcDetails, fontSize = 13.sp, color = CharcoalDark)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Driving Licence", fontSize = 13.sp, color = SlateGray)
                            Text(
                                text = if (!currentDriver.licenseNo.isNullOrBlank()) "${currentDriver.licenseNo} (${currentDriver.licenseDetails ?: "Valid"})" else "Not provided (Optional)",
                                fontSize = 13.sp,
                                fontWeight = if (!currentDriver.licenseNo.isNullOrBlank()) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (!currentDriver.licenseNo.isNullOrBlank()) BrandGreenDark else SlateGray
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Primary Service Area", fontSize = 13.sp, color = SlateGray)
                            Text("${currentDriver.serviceCity} (30 km operating zone)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CharcoalDark)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Completed Trips", fontSize = 13.sp, color = SlateGray)
                            Text("${currentDriver.totalRides} Trips • ⭐ ${currentDriver.rating}", fontSize = 13.sp, color = SlateGray)
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
                        // Section Header
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
                                    imageVector = if (currentDriver.bookingRingtoneEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                    contentDescription = null,
                                    tint = if (currentDriver.bookingRingtoneEnabled) BrandGreenDark else SlateGray,
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
                                        text = if (currentDriver.bookingRingtoneEnabled) {
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
                                checked = currentDriver.bookingRingtoneEnabled,
                                onCheckedChange = { enabled ->
                                    RideRepository.updateDriverRingtoneSettings(
                                        driverId = currentDriver.id,
                                        enabled = enabled,
                                        ringtoneId = currentDriver.bookingRingtone
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
                        val currentRingtoneObj = BookingRingtone.fromId(currentDriver.bookingRingtone)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (currentDriver.bookingRingtoneEnabled) SlateLight.copy(alpha = 0.5f) else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, CardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    tempSelectedRingtone = currentDriver.bookingRingtone
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
                                RideAlertManager.testBookingSound(context, currentDriver.bookingRingtone) {
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

                        // Test confirmation feedback
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

                        // Volume / Silent warning notice
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
                            driverId = currentDriver.id,
                            enabled = currentDriver.bookingRingtoneEnabled,
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
                            driverId = currentDriver.id,
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

/**
 * Driver Delete Account Screen: Permanent deactivation with verification
 */
@Composable
fun DriverDeleteAccountScreen(
    onBack: () -> Unit,
    onAccountDeleted: () -> Unit
) {
    val currentDriver by RideRepository.currentDriver.collectAsStateWithLifecycle()
    var selectedReason by remember { mutableStateOf(AccountDeletionReasons.NO_LONGER_NEED) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isDeleted by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            HeaderAppBar(
                title = "Delete Driver Account",
                subtitle = "Permanent Deactivation",
                onBack = onBack
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isDeleted) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RedCancel, modifier = Modifier.size(54.dp))
                        Text("Account Deleted", fontWeight = FontWeight.Black, fontSize = 20.sp)
                        Text("Your driver account has been permanently deactivated.", textAlign = TextAlign.Center, color = SlateGray, fontSize = 13.sp)
                        Button(
                            onClick = onAccountDeleted,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                        ) {
                            Text("Back to Main Screen")
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    border = BorderStroke(1.dp, RedCancel)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Warning: Permanent Action", fontWeight = FontWeight.Bold, color = RedCancel, fontSize = 15.sp)
                        Text(
                            "Deleting your account will permanently remove your profile, revoke booking offers, and close wallet operations.",
                            fontSize = 12.sp,
                            color = CharcoalDark
                        )
                    }
                }

                Text("Reason for Leaving", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                AccountDeletionReasons.options.forEach { reason ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { selectedReason = reason }
                    ) {
                        RadioButton(selected = selectedReason == reason, onClick = { selectedReason = reason })
                        Text(reason, fontSize = 13.sp)
                    }
                }

                if (errorMessage != null) {
                    Text(errorMessage!!, color = RedCancel, fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        val result = RideRepository.requestDriverAccountDeletion(currentDriver.id, selectedReason)
                        when (result) {
                            is DeleteAccountResult.Success -> {
                                isDeleted = true
                            }
                            is DeleteAccountResult.HasActiveRide -> {
                                errorMessage = result.message
                            }
                            is DeleteAccountResult.FinancialPending -> {
                                errorMessage = result.message
                            }
                            is DeleteAccountResult.Unauthorized -> {
                                errorMessage = result.message
                            }
                            is DeleteAccountResult.Error -> {
                                errorMessage = result.message
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedCancel),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Confirm Account Deletion", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
