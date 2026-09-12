package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.models.Passenger
import com.example.data.models.PassengerOtpResult
import com.example.data.models.UserRole
import com.example.data.repository.RideRepository
import com.example.ui.components.HeaderAppBar
import com.example.ui.theme.*
import kotlinx.coroutines.delay

enum class PassengerAuthStep {
    PHONE_INPUT,
    OTP_VERIFY,
    NEW_PROFILE
}

/**
 * Passenger Authentication Screen:
 * Implements clean Mobile Number -> Send OTP -> Enter OTP [ _ _ _ _ _ _ ] -> Verify OTP.
 * If Existing Passenger: Immediately opens existing Passenger Home screen.
 * If New Passenger: Prompts for minimum required profile details (Full Name) before opening Passenger App.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerAuthScreen(
    onLoginSuccess: () -> Unit,
    onBackToPortal: () -> Unit
) {
    var currentStep by remember { mutableStateOf(PassengerAuthStep.PHONE_INPUT) }
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var newPassengerName by remember { mutableStateOf("") }
    var selectedCity by remember { mutableStateOf("Tezpur") }

    var isSendingOtp by remember { mutableStateOf(false) }
    var isVerifyingOtp by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var resendTimer by remember { mutableIntStateOf(30) }
    var canResend by remember { mutableStateOf(false) }

    val latestDemoOtp by RideRepository.latestDemoOtp.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    // Resend countdown timer
    LaunchedEffect(currentStep, resendTimer) {
        if (currentStep == PassengerAuthStep.OTP_VERIFY && resendTimer > 0) {
            delay(1000L)
            resendTimer -= 1
        } else if (resendTimer == 0) {
            canResend = true
        }
    }

    Scaffold(
        topBar = {
            HeaderAppBar(
                title = when (currentStep) {
                    PassengerAuthStep.PHONE_INPUT -> "Passenger Login"
                    PassengerAuthStep.OTP_VERIFY -> "Verify OTP"
                    PassengerAuthStep.NEW_PROFILE -> "Passenger Profile"
                },
                subtitle = when (currentStep) {
                    PassengerAuthStep.PHONE_INPUT -> "Enter mobile number for instant OTP login"
                    PassengerAuthStep.OTP_VERIFY -> "Enter 6-digit verification code"
                    PassengerAuthStep.NEW_PROFILE -> "Enter your name to complete setup"
                },
                onBack = {
                    when (currentStep) {
                        PassengerAuthStep.PHONE_INPUT -> onBackToPortal()
                        PassengerAuthStep.OTP_VERIFY -> {
                            currentStep = PassengerAuthStep.PHONE_INPUT
                            otpCode = ""
                            errorMessage = null
                        }
                        PassengerAuthStep.NEW_PROFILE -> {
                            currentStep = PassengerAuthStep.OTP_VERIFY
                            errorMessage = null
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
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
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (currentStep == PassengerAuthStep.PHONE_INPUT) {
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
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(BrandGreenLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (currentStep) {
                                            PassengerAuthStep.OTP_VERIFY -> Icons.Default.Security
                                            PassengerAuthStep.NEW_PROFILE -> Icons.Default.Person
                                            else -> Icons.Default.PhoneIphone
                                        },
                                        contentDescription = null,
                                        tint = BrandGreenDark,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = when (currentStep) {
                                        PassengerAuthStep.PHONE_INPUT -> "Step 1 of 2: Mobile Number"
                                        PassengerAuthStep.OTP_VERIFY -> "Step 2 of 2: Verify OTP"
                                        PassengerAuthStep.NEW_PROFILE -> "Complete Registration"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = CharcoalDark
                                )
                                Text(
                                    text = "Secure OTP Verification • E-Ride 3 Assam",
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
                                text = "PASSENGER",
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
            if (errorMessage != null) {
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
                            Text(text = errorMessage!!, color = RedCancel, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            if (successMessage != null) {
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
                            Text(text = successMessage!!, color = BrandGreenDark, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // STEP 1: MOBILE NUMBER INPUT
            // -------------------------------------------------------------
            if (currentStep == PassengerAuthStep.PHONE_INPUT) {
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
                                text = "We will send a 6-digit One Time Password (OTP) to verify your account.",
                                fontSize = 12.sp,
                                color = SlateGray,
                                lineHeight = 16.sp
                            )

                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = {
                                    val digitsOnly = it.filter { char -> char.isDigit() }.take(10)
                                    phoneNumber = digitsOnly
                                    errorMessage = null
                                },
                                label = { Text("Mobile Phone Number") },
                                placeholder = { Text("98640 12345") },
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
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Phone,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Button(
                                onClick = {
                                    errorMessage = null
                                    if (phoneNumber.length != 10) {
                                        errorMessage = "Please enter a valid 10-digit Indian mobile number."
                                        return@Button
                                    }
                                    isSendingOtp = true
                                    val (success, message) = RideRepository.sendOtp(phoneNumber, UserRole.PASSENGER)
                                    isSendingOtp = false
                                    if (success) {
                                        successMessage = message
                                        resendTimer = 30
                                        canResend = false
                                        currentStep = PassengerAuthStep.OTP_VERIFY
                                    } else {
                                        errorMessage = message
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

                // Quick Demo Accounts Section
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
                                text = "Quick Demo Passengers (Click to autofill)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = CharcoalDark
                            )

                            val passengersList by RideRepository.passengersList.collectAsStateWithLifecycle()
                            passengersList.take(3).forEach { passenger ->
                                OutlinedButton(
                                    onClick = {
                                        phoneNumber = passenger.phone.replace("+91", "").replace(" ", "").trim().takeLast(10)
                                        errorMessage = null
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
                                            Text(passenger.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("${passenger.phone} • ${passenger.defaultCity}", fontSize = 11.sp, color = SlateGray)
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = BrandGreenLight
                                        ) {
                                            Text(
                                                text = "Existing",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BrandGreenDark
                                            )
                                        }
                                    }
                                }
                            }

                            // New user demo option
                            OutlinedButton(
                                onClick = {
                                    phoneNumber = "91012${(10000..99999).random()}"
                                    errorMessage = null
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
                                        Text("✨ New Passenger Mobile", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Tests automatic new user registration flow", fontSize = 11.sp, color = SlateGray)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = ElectricAmberLight
                                    ) {
                                        Text(
                                            text = "New User",
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
            // STEP 2: OTP VERIFICATION
            // -------------------------------------------------------------
            if (currentStep == PassengerAuthStep.OTP_VERIFY) {
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
                                    Text("Enter Verification Code", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CharcoalDark)
                                    Text(
                                        text = "OTP sent to +91 $phoneNumber",
                                        fontSize = 12.sp,
                                        color = SlateGray
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        currentStep = PassengerAuthStep.PHONE_INPUT
                                        otpCode = ""
                                        errorMessage = null
                                    }
                                ) {
                                    Text("Edit Number", color = BrandGreenDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // OTP Display Test Helper Banner
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
                                        Text("TESTING OTP CODE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateGray)
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
                                            errorMessage = null
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
                                    errorMessage = null
                                },
                                label = { Text("6-Digit OTP Code") },
                                placeholder = { Text("• • • • • •") },
                                leadingIcon = {
                                    Icon(Icons.Default.Key, contentDescription = null, tint = BrandGreen)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.NumberPassword,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Verify Button
                            Button(
                                onClick = {
                                    errorMessage = null
                                    if (otpCode.length != 6) {
                                        errorMessage = "Please enter the complete 6-digit OTP code."
                                        return@Button
                                    }

                                    isVerifyingOtp = true
                                    when (val result = RideRepository.verifyPassengerOtp(phoneNumber, otpCode)) {
                                        is PassengerOtpResult.ExistingPassenger -> {
                                            isVerifyingOtp = false
                                            onLoginSuccess()
                                        }
                                        is PassengerOtpResult.NewPassenger -> {
                                            isVerifyingOtp = false
                                            currentStep = PassengerAuthStep.NEW_PROFILE
                                        }
                                        is PassengerOtpResult.InvalidOtp -> {
                                            isVerifyingOtp = false
                                            errorMessage = result.message
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
                                    Text("Verifying...", fontSize = 14.sp)
                                } else {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("VERIFY OTP", fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
                                            val (success, message) = RideRepository.sendOtp(phoneNumber, UserRole.PASSENGER)
                                            if (success) {
                                                successMessage = message
                                                resendTimer = 30
                                                canResend = false
                                            } else {
                                                errorMessage = message
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
            // STEP 3: NEW PASSENGER PROFILE SETUP (Only shown for new numbers)
            // -------------------------------------------------------------
            if (currentStep == PassengerAuthStep.NEW_PROFILE) {
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
                                text = "Welcome to E-Ride 3!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = CharcoalDark
                            )
                            Text(
                                text = "Your mobile number has been verified. Please enter your name to complete your profile.",
                                fontSize = 12.sp,
                                color = SlateGray,
                                lineHeight = 16.sp
                            )

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
                                        text = "Verified Mobile: +91 $phoneNumber",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandGreenDark
                                    )
                                }
                            }

                            // Full Name Field (REQUIRED)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Full Name *", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = CharcoalDark)
                                OutlinedTextField(
                                    value = newPassengerName,
                                    onValueChange = {
                                        newPassengerName = it
                                        errorMessage = null
                                    },
                                    placeholder = { Text("e.g. Manas Pratim Kalita") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = BrandGreen)
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Words,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            // Primary Service Area Selection
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Primary Service Area", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = CharcoalDark)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("Tezpur", "Biswanath Chariali", "Nagsankar").forEach { city ->
                                        val isSel = selectedCity == city
                                        FilterChip(
                                            selected = isSel,
                                            onClick = { selectedCity = city },
                                            label = { Text(city, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = BrandGreenLight,
                                                selectedLabelColor = BrandGreenDark
                                            )
                                        )
                                    }
                                }
                            }

                            // Submit & Continue Button
                            Button(
                                onClick = {
                                    if (newPassengerName.trim().length < 2) {
                                        errorMessage = "Please enter your full name."
                                        return@Button
                                    }

                                    RideRepository.registerNewPassenger(
                                        name = newPassengerName,
                                        phoneInput = phoneNumber,
                                        defaultCity = selectedCity
                                    )
                                    onLoginSuccess()
                                },
                                enabled = newPassengerName.trim().isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                            ) {
                                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("COMPLETE & ENTER PASSENGER APP", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
