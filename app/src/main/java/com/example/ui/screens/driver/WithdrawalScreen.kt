package com.example.ui.screens.driver

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.models.*
import com.example.data.repository.RideRepository
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

enum class WithdrawalStep {
    INPUT,
    CONFIRMATION,
    SUCCESS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawalScreen(
    onBack: () -> Unit
) {
    val currentDriver by RideRepository.currentDriver.collectAsStateWithLifecycle()
    val driverWallets by RideRepository.driverWallets.collectAsStateWithLifecycle()
    val wallet = driverWallets[currentDriver.id] ?: DriverWallet(driverId = currentDriver.id)

    val scheduleCheck = remember { RideRepository.getWithdrawalScheduleCheck() }

    var currentStep by remember { mutableStateOf(WithdrawalStep.INPUT) }
    var selectedMethod by remember { mutableStateOf("UPI") } // "UPI" or "BANK_ACCOUNT"

    // UPI Fields
    var upiIdInput by remember { mutableStateOf("driver.eride3@oksbi") }

    // Bank Account Fields
    var accountHolderName by remember { mutableStateOf(currentDriver.name) }
    var bankName by remember { mutableStateOf("State Bank of India (SBI)") }
    var accountNumber by remember { mutableStateOf("") }
    var confirmAccountNumber by remember { mutableStateOf("") }
    var ifscCode by remember { mutableStateOf("SBIN0000123") }

    // Amount
    var amountInput by remember { mutableStateOf("500") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var submittedRequest by remember { mutableStateOf<WithdrawalRequest?>(null) }

    val amountDouble = amountInput.toDoubleOrNull() ?: 0.0
    val balanceAfter = (wallet.availableBalance - amountDouble).coerceAtLeast(0.0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = when (currentStep) {
                                WithdrawalStep.INPUT -> "Withdraw Earnings"
                                WithdrawalStep.CONFIRMATION -> "Withdrawal Summary"
                                WithdrawalStep.SUCCESS -> "Request Submitted"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = CharcoalDark
                        )
                        Text(
                            text = "Available: ₹${wallet.availableBalance.toInt()}",
                            fontSize = 12.sp,
                            color = SlateGray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep == WithdrawalStep.CONFIRMATION) {
                            currentStep = WithdrawalStep.INPUT
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CharcoalDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        AnimatedContent(
            targetState = currentStep,
            label = "WithdrawalStepAnimation",
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) { step ->
            when (step) {
                WithdrawalStep.INPUT -> {
                    WithdrawalInputContent(
                        wallet = wallet,
                        scheduleCheck = scheduleCheck,
                        selectedMethod = selectedMethod,
                        onMethodChange = { selectedMethod = it },
                        upiIdInput = upiIdInput,
                        onUpiChange = { upiIdInput = it },
                        accountHolderName = accountHolderName,
                        onAccountHolderChange = { accountHolderName = it },
                        bankName = bankName,
                        onBankNameChange = { bankName = it },
                        accountNumber = accountNumber,
                        onAccountNumberChange = { accountNumber = it },
                        confirmAccountNumber = confirmAccountNumber,
                        onConfirmAccountNumberChange = { confirmAccountNumber = it },
                        ifscCode = ifscCode,
                        onIfscChange = { ifscCode = it },
                        amountInput = amountInput,
                        onAmountChange = {
                            amountInput = it.filter { char -> char.isDigit() }
                            errorMessage = null
                        },
                        balanceAfter = balanceAfter,
                        errorMessage = errorMessage,
                        onProceedToSummary = {
                            val amt = amountInput.toDoubleOrNull() ?: 0.0
                            if (!scheduleCheck.isOpen) {
                                errorMessage = "${scheduleCheck.title}: ${scheduleCheck.message}"
                                return@WithdrawalInputContent
                            }
                            if (amt < 100.0) {
                                errorMessage = "Minimum withdrawal amount is ₹100."
                                return@WithdrawalInputContent
                            }
                            if (amt > wallet.availableBalance) {
                                errorMessage = "Amount cannot exceed available balance of ₹${wallet.availableBalance.toInt()}."
                                return@WithdrawalInputContent
                            }
                            if (selectedMethod == "UPI") {
                                if (upiIdInput.isBlank() || !upiIdInput.contains("@") || upiIdInput.length < 5) {
                                    errorMessage = "Please enter a valid UPI ID (e.g. name@upi)."
                                    return@WithdrawalInputContent
                                }
                            } else {
                                if (accountHolderName.isBlank()) {
                                    errorMessage = "Please enter Account Holder Name."
                                    return@WithdrawalInputContent
                                }
                                if (bankName.isBlank()) {
                                    errorMessage = "Please enter Bank Name."
                                    return@WithdrawalInputContent
                                }
                                val digits = accountNumber.filter { it.isDigit() }
                                if (digits.length < 8) {
                                    errorMessage = "Account number must be at least 8 digits."
                                    return@WithdrawalInputContent
                                }
                                if (accountNumber != confirmAccountNumber) {
                                    errorMessage = "Account numbers do not match. Please re-check."
                                    return@WithdrawalInputContent
                                }
                                if (ifscCode.isBlank() || ifscCode.length < 6) {
                                    errorMessage = "Please enter a valid IFSC code (minimum 6 characters)."
                                    return@WithdrawalInputContent
                                }
                            }
                            errorMessage = null
                            currentStep = WithdrawalStep.CONFIRMATION
                        }
                    )
                }

                WithdrawalStep.CONFIRMATION -> {
                    WithdrawalSummaryContent(
                        amount = amountDouble,
                        selectedMethod = selectedMethod,
                        upiId = upiIdInput,
                        accountHolder = accountHolderName,
                        bankName = bankName,
                        accountNumber = accountNumber,
                        ifsc = ifscCode,
                        availableBalance = wallet.availableBalance,
                        balanceAfter = balanceAfter,
                        onCancel = { currentStep = WithdrawalStep.INPUT },
                        onConfirm = {
                            val result = RideRepository.requestWithdrawal(
                                driverId = currentDriver.id,
                                amount = amountDouble,
                                paymentMethod = selectedMethod,
                                upiId = upiIdInput,
                                accountHolderName = accountHolderName,
                                bankName = bankName,
                                accountNumber = accountNumber,
                                ifscCode = ifscCode
                            )
                            if (result.success) {
                                submittedRequest = result.request
                                currentStep = WithdrawalStep.SUCCESS
                            } else {
                                errorMessage = result.message
                                currentStep = WithdrawalStep.INPUT
                            }
                        }
                    )
                }

                WithdrawalStep.SUCCESS -> {
                    WithdrawalSuccessContent(
                        request = submittedRequest,
                        onDone = onBack
                    )
                }
            }
        }
    }
}

@Composable
private fun WithdrawalInputContent(
    wallet: DriverWallet,
    scheduleCheck: RideRepository.WithdrawalScheduleCheck,
    selectedMethod: String,
    onMethodChange: (String) -> Unit,
    upiIdInput: String,
    onUpiChange: (String) -> Unit,
    accountHolderName: String,
    onAccountHolderChange: (String) -> Unit,
    bankName: String,
    onBankNameChange: (String) -> Unit,
    accountNumber: String,
    onAccountNumberChange: (String) -> Unit,
    confirmAccountNumber: String,
    onConfirmAccountNumberChange: (String) -> Unit,
    ifscCode: String,
    onIfscChange: (String) -> Unit,
    amountInput: String,
    onAmountChange: (String) -> Unit,
    balanceAfter: Double,
    errorMessage: String?,
    onProceedToSummary: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Available to Withdraw Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BrandGreenDark)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Available to Withdraw",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium
                    )
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = if (scheduleCheck.isOpen) "Window Open" else "Closed",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "₹${wallet.availableBalance.toInt()}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Minimum Withdrawal: ₹100",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        text = "Pending: ₹${wallet.pendingBalance.toInt()}",
                        fontSize = 12.sp,
                        color = ElectricAmber
                    )
                }
            }
        }

        // 2. Withdrawal Schedule & Time Restriction Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (scheduleCheck.isOpen) BrandGreenLight else RedCancel.copy(alpha = 0.08f)
            ),
            border = BorderStroke(
                1.dp,
                if (scheduleCheck.isOpen) BrandGreen else RedCancel.copy(alpha = 0.4f)
            )
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    if (scheduleCheck.isOpen) Icons.Default.AccessTime else Icons.Default.Info,
                    contentDescription = null,
                    tint = if (scheduleCheck.isOpen) BrandGreenDark else RedCancel,
                    modifier = Modifier.size(20.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = scheduleCheck.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (scheduleCheck.isOpen) BrandGreenDark else RedCancel
                    )
                    Text(
                        text = scheduleCheck.message,
                        fontSize = 12.sp,
                        color = CharcoalDark,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Official Schedule:\n• Monday – Friday: 9:00 AM – 5:00 PM IST\n• Saturday – Sunday: Withdrawal Closed",
                        fontSize = 11.sp,
                        color = SlateGray,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // 3. Amount Entry & Quick Buttons
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
                    text = "Enter Withdrawal Amount",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = CharcoalDark
                )

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = onAmountChange,
                    leadingIcon = {
                        Text(
                            text = "₹",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandGreenDark,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("withdrawal_amount_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandGreen,
                        unfocusedBorderColor = CardBorder
                    )
                )

                // Quick Amount Buttons: ₹100, ₹500, ₹1000, MAX
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val maxBalance = wallet.availableBalance.toInt()
                    listOf(100, 500, 1000).forEach { quickAmt ->
                        OutlinedButton(
                            onClick = { onAmountChange(quickAmt.toString()) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (amountInput == quickAmt.toString()) BrandGreenDark else CardBorder),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (amountInput == quickAmt.toString()) BrandGreenLight else Color.Transparent
                            )
                        ) {
                            Text(
                                text = "₹$quickAmt",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (amountInput == quickAmt.toString()) BrandGreenDark else CharcoalDark
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (maxBalance > 0) {
                                onAmountChange(maxBalance.toString())
                            }
                        },
                        modifier = Modifier.weight(1.1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                    ) {
                        Text("MAX", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Balance After Withdrawal:", fontSize = 12.sp, color = SlateGray)
                    Text("₹${balanceAfter.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CharcoalDark)
                }
            }
        }

        // 4. Withdrawal Method Tabs (UPI vs BANK ACCOUNT)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, CardBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Select Withdrawal Method",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = CharcoalDark
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(BackgroundLight)
                        .padding(4.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onMethodChange("UPI") },
                        shape = RoundedCornerShape(8.dp),
                        color = if (selectedMethod == "UPI") Color.White else Color.Transparent,
                        shadowElevation = if (selectedMethod == "UPI") 2.dp else 0.dp
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                            Text(
                                text = "UPI ID",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (selectedMethod == "UPI") BrandGreenDark else SlateGray
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onMethodChange("BANK_ACCOUNT") },
                        shape = RoundedCornerShape(8.dp),
                        color = if (selectedMethod == "BANK_ACCOUNT") Color.White else Color.Transparent,
                        shadowElevation = if (selectedMethod == "BANK_ACCOUNT") 2.dp else 0.dp
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                            Text(
                                text = "Bank Account",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (selectedMethod == "BANK_ACCOUNT") BrandGreenDark else SlateGray
                            )
                        }
                    }
                }

                if (selectedMethod == "UPI") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("UPI ID (e.g. driver@oksbi)", fontSize = 12.sp, color = SlateGray, fontWeight = FontWeight.Medium)
                        OutlinedTextField(
                            value = upiIdInput,
                            onValueChange = onUpiChange,
                            placeholder = { Text("name@upi") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("withdrawal_upi_input"),
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.QrCode, contentDescription = null, tint = BrandGreenDark)
                            }
                        )
                        Text(
                            text = "Instant transfer to any active VPA/UPI account.",
                            fontSize = 11.sp,
                            color = SlateGray
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = accountHolderName,
                            onValueChange = onAccountHolderChange,
                            label = { Text("Account Holder Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = bankName,
                            onValueChange = onBankNameChange,
                            label = { Text("Bank Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = accountNumber,
                            onValueChange = { onAccountNumberChange(it.filter { c -> c.isDigit() }) },
                            label = { Text("Account Number") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = confirmAccountNumber,
                            onValueChange = { onConfirmAccountNumberChange(it.filter { c -> c.isDigit() }) },
                            label = { Text("Confirm Account Number") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = ifscCode,
                            onValueChange = { onIfscChange(it.uppercase()) },
                            label = { Text("IFSC Code (e.g. SBIN0000123)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Text(
                            text = "Note: Account numbers are masked (XXXX XXXX 1234) for privacy.",
                            fontSize = 11.sp,
                            color = SlateGray
                        )
                    }
                }
            }
        }

        // Error message banner
        if (errorMessage != null) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = RedCancel.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, RedCancel.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = RedCancel)
                    Text(
                        text = errorMessage,
                        fontSize = 12.sp,
                        color = RedCancel,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Proceed Button
        Button(
            onClick = onProceedToSummary,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("proceed_withdrawal_button"),
            colors = ButtonDefaults.buttonColors(containerColor = BrandGreenDark),
            shape = RoundedCornerShape(12.dp),
            enabled = wallet.availableBalance >= 100.0
        ) {
            Text(
                text = "Continue to Summary",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(Icons.Default.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
private fun WithdrawalSummaryContent(
    amount: Double,
    selectedMethod: String,
    upiId: String,
    accountHolder: String,
    bankName: String,
    accountNumber: String,
    ifsc: String,
    availableBalance: Double,
    balanceAfter: Double,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val maskedAccount = RideRepository.maskAccountNumber(accountNumber)
    val maskedUpi = RideRepository.maskUpiId(upiId)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, CardBorder)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = BrandGreenLight,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = BrandGreenDark)
                        }
                    }
                    Column {
                        Text("Withdrawal Summary", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CharcoalDark)
                        Text("Review details before confirming", fontSize = 12.sp, color = SlateGray)
                    }
                }

                HorizontalDivider(color = CardBorder)

                // Amount
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Requested Amount:", fontSize = 13.sp, color = SlateGray)
                    Text("₹${amount.toInt()}", fontWeight = FontWeight.Black, fontSize = 22.sp, color = BrandGreenDark)
                }

                // Method
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Withdrawal Method:", fontSize = 13.sp, color = SlateGray)
                    Text(
                        text = if (selectedMethod == "BANK_ACCOUNT") "Bank Account Transfer" else "UPI Instant",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = CharcoalDark
                    )
                }

                if (selectedMethod == "UPI") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("UPI ID:", fontSize = 13.sp, color = SlateGray)
                        Text(maskedUpi, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = CharcoalDark)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Beneficiary:", fontSize = 13.sp, color = SlateGray)
                        Text(accountHolder, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = CharcoalDark)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Bank:", fontSize = 13.sp, color = SlateGray)
                        Text(bankName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = CharcoalDark)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Account Number:", fontSize = 13.sp, color = SlateGray)
                        Text(maskedAccount, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = CharcoalDark)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("IFSC Code:", fontSize = 13.sp, color = SlateGray)
                        Text(ifsc, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = CharcoalDark)
                    }
                }

                HorizontalDivider(color = CardBorder)

                // Balances
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Available Balance:", fontSize = 13.sp, color = SlateGray)
                    Text("₹${availableBalance.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CharcoalDark)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Balance After Withdrawal:", fontSize = 13.sp, color = SlateGray)
                    Text("₹${balanceAfter.toInt()}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandGreenDark)
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BackgroundLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Note: Once confirmed, ₹${amount.toInt()} will move to Withdrawal Pending. Operations will verify and disburse the payout.",
                        modifier = Modifier.padding(10.dp),
                        fontSize = 11.sp,
                        color = SlateGray,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Text("Cancel", color = CharcoalDark, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .weight(1.5f)
                    .height(48.dp)
                    .testTag("confirm_withdrawal_button"),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Confirm Withdrawal", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun WithdrawalSuccessContent(
    request: WithdrawalRequest?,
    onDone: () -> Unit
) {
    val req = request ?: return
    val reqDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).format(Date(req.requestedAt))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(40.dp),
            color = BrandGreenLight,
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = BrandGreenDark,
                    modifier = Modifier.size(50.dp)
                )
            }
        }

        Text(
            text = "Withdrawal Request Submitted",
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
            color = CharcoalDark
        )

        Text(
            text = "Your withdrawal request has been submitted successfully.",
            fontSize = 13.sp,
            color = SlateGray,
            lineHeight = 18.sp
        )

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Amount:", fontSize = 13.sp, color = SlateGray)
                    Text("₹${req.amount.toInt()}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = BrandGreenDark)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Request ID:", fontSize = 13.sp, color = SlateGray)
                    Text(req.id, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CharcoalDark)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Requested Time:", fontSize = 13.sp, color = SlateGray)
                    Text(reqDate, fontSize = 12.sp, color = CharcoalDark)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Status:", fontSize = 13.sp, color = SlateGray)
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = ElectricAmber.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = req.status.name,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = CharcoalDark
                        )
                    }
                }

                if (req.bankDetails != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Payout Destination:", fontSize = 13.sp, color = SlateGray)
                        Text(req.bankDetails, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = CharcoalDark)
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = BrandGreenLight,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = BrandGreenDark, modifier = Modifier.size(18.dp))
                Text(
                    text = "Withdrawals are processed Mon–Fri, 9:00 AM – 5:00 PM IST. You will receive real-time status updates.",
                    fontSize = 11.sp,
                    color = BrandGreenDark,
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("done_withdrawal_button"),
            colors = ButtonDefaults.buttonColors(containerColor = BrandGreenDark),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Done (View Wallet)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
fun DriverWithdrawalDialog(
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            WithdrawalScreen(onBack = onDismiss)
        }
    }
}

@Composable
fun WithdrawalDetailDialog(
    request: WithdrawalRequest,
    onDismiss: () -> Unit,
    onCancelRequest: ((String) -> Unit)? = null
) {
    val reqDate = remember(request.requestedAt) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).format(Date(request.requestedAt))
    }
    val paidDate = remember(request.paidAt) {
        request.paidAt?.let { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).format(Date(it)) }
    }

    val statusColor = when (request.status) {
        WithdrawalStatus.PAID, WithdrawalStatus.COMPLETED, WithdrawalStatus.APPROVED -> BrandGreenDark
        WithdrawalStatus.PENDING -> ElectricAmber
        WithdrawalStatus.PROCESSING -> BlueInfo
        WithdrawalStatus.REJECTED, WithdrawalStatus.FAILED, WithdrawalStatus.CANCELLED -> RedCancel
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Withdrawal Details", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = request.status.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = statusColor
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Amount:", fontSize = 13.sp, color = SlateGray)
                    Text("₹${request.amount.toInt()}", fontWeight = FontWeight.Black, fontSize = 20.sp, color = BrandGreenDark)
                }

                HorizontalDivider(color = CardBorder)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Request ID:", fontSize = 12.sp, color = SlateGray)
                    Text(request.id, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = CharcoalDark)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Requested Date:", fontSize = 12.sp, color = SlateGray)
                    Text(reqDate, fontSize = 12.sp, color = CharcoalDark)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Method:", fontSize = 12.sp, color = SlateGray)
                    Text(
                        if (request.paymentMethod == "BANK_ACCOUNT") "Bank Transfer" else "UPI Transfer",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = CharcoalDark
                    )
                }

                if (request.paymentMethod == "BANK_ACCOUNT") {
                    if (request.bankName != null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Bank:", fontSize = 12.sp, color = SlateGray)
                            Text(request.bankName, fontSize = 12.sp, color = CharcoalDark)
                        }
                    }
                    if (request.maskedAccountNumber != null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("A/C Number:", fontSize = 12.sp, color = SlateGray)
                            Text(request.maskedAccountNumber, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = CharcoalDark)
                        }
                    }
                    if (request.ifscCode != null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("IFSC Code:", fontSize = 12.sp, color = SlateGray)
                            Text(request.ifscCode, fontSize = 12.sp, color = CharcoalDark)
                        }
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("UPI ID:", fontSize = 12.sp, color = SlateGray)
                        Text(RideRepository.maskUpiId(request.upiId), fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = CharcoalDark)
                    }
                }

                if (request.transactionReference != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = BrandGreenLight,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("UTR / Ref: ${request.transactionReference}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandGreenDark)
                            if (paidDate != null) {
                                Text("Paid on: $paidDate", fontSize = 11.sp, color = BrandGreenDark)
                            }
                        }
                    }
                }

                if (request.rejectionReason != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = RedCancel.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Note: ${request.rejectionReason}", fontSize = 12.sp, color = RedCancel, fontWeight = FontWeight.SemiBold)
                            Text("The withdrawal amount has been refunded to your Available Balance.", fontSize = 11.sp, color = RedCancel)
                        }
                    }
                }

                if (request.status == WithdrawalStatus.PENDING) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ElectricAmber.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Awaiting operations approval. Withdrawals are processed during working hours (Mon–Fri 9 AM – 5 PM).",
                            modifier = Modifier.padding(8.dp),
                            fontSize = 11.sp,
                            color = CharcoalDark
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (request.status == WithdrawalStatus.PENDING && onCancelRequest != null) {
                OutlinedButton(
                    onClick = {
                        onCancelRequest(request.id)
                        onDismiss()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RedCancel),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel Request", fontSize = 12.sp)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}

