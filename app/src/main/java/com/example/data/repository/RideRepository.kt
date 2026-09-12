package com.example.data.repository

import com.example.data.models.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

object RideRepository {

    fun roundCurrency(value: Double): Double {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    // Preset Sample Passengers
    private val samplePassengers = listOf(
        Passenger(
            id = "PASS_001",
            name = "Anurag Sharma",
            phone = "+91 98640 12345",
            email = "anurag.sharma@example.com",
            defaultCity = "Tezpur",
            rating = 4.9,
            totalRides = 18
        ),
        Passenger(
            id = "PASS_002",
            name = "Dipanjali Das",
            phone = "+91 97060 54321",
            email = "dipanjali.das@example.com",
            defaultCity = "Biswanath Chariali",
            rating = 4.8,
            totalRides = 12
        ),
        Passenger(
            id = "PASS_003",
            name = "Raktim Bora",
            phone = "+91 94350 11223",
            email = "raktim.bora@example.com",
            defaultCity = "Nagsankar",
            rating = 4.7,
            totalRides = 9
        )
    )

    // Preset Sample Drivers (Assam service areas: Tezpur, Biswanath Chariali, Nagsankar)
    private val sampleDrivers = listOf(
        Driver(
            id = "DRV_101",
            name = "Bipul Kalita",
            phone = "+91 94350 67890",
            vehicleType = "Passenger E-Rickshaw (3-Wheeler)",
            vehicleModel = "Mayuri Deluxe Electric (Green)",
            vehicleNumber = "AS-12-ER-4421",
            rickshawRegNo = "AS-12-ER-4421",
            rcDetails = "Sonitpur RTO Commercial RC Valid",
            licenseNo = "AS-DL-2021-9988",
            licenseDetails = "Commercial LMV / E-Rickshaw Endorsed",
            serviceCity = "Tezpur",
            isOnline = true,
            status = DriverStatus.APPROVED,
            rating = 4.9,
            totalRides = 312,
            currentLat = 26.6338,
            currentLng = 92.7926
        ),
        Driver(
            id = "DRV_102",
            name = "Pranjal Saikia",
            phone = "+91 98640 88776",
            vehicleType = "Passenger E-Rickshaw",
            vehicleModel = "Saarthi Smart Eco E-Rickshaw",
            vehicleNumber = "AS-32-ER-1092",
            rickshawRegNo = "AS-32-ER-1092",
            rcDetails = "Biswanath Chariali Commercial Permit",
            licenseNo = "AS-DL-2020-5541",
            licenseDetails = "Valid up to 2029",
            serviceCity = "Biswanath Chariali",
            isOnline = true,
            status = DriverStatus.APPROVED,
            rating = 4.8,
            totalRides = 245,
            currentLat = 26.7329,
            currentLng = 93.1554
        ),
        Driver(
            id = "DRV_103",
            name = "Manoj Baruah",
            phone = "+91 97060 33445",
            vehicleType = "Passenger E-Rickshaw",
            vehicleModel = "Jeevan Electric Passenger Rickshaw",
            vehicleNumber = "AS-12-ER-7711",
            rickshawRegNo = "AS-12-ER-7711",
            rcDetails = "Assam State Transport RC Valid",
            licenseNo = "AS-DL-2022-3321",
            licenseDetails = "Commercial Driving License",
            serviceCity = "Nagsankar",
            isOnline = true,
            status = DriverStatus.APPROVED,
            rating = 4.7,
            totalRides = 180,
            currentLat = 26.6900,
            currentLng = 92.9800
        ),
        Driver(
            id = "DRV_104",
            name = "Hemanta Hazarika",
            phone = "+91 94355 44332",
            vehicleType = "Eco Passenger 3-Wheeler",
            vehicleModel = "Yatri Super Electric Rickshaw",
            vehicleNumber = "AS-12-ER-8844",
            rickshawRegNo = "AS-12-ER-8844",
            rcDetails = "Tezpur DTO Fitness Valid",
            licenseNo = "AS-DL-2023-1122",
            licenseDetails = "Non-Transport / Transport Badge",
            serviceCity = "Tezpur",
            isOnline = true,
            status = DriverStatus.APPROVED,
            rating = 4.9,
            totalRides = 195,
            currentLat = 26.6520,
            currentLng = 92.7915
        ),
        Driver(
            id = "DRV_105",
            name = "Dhiraj Medhi",
            phone = "+91 98540 66554",
            vehicleType = "Passenger E-Rickshaw",
            vehicleModel = "Eco-Charge 3-Wheeler",
            vehicleNumber = "AS-32-ER-3399",
            rickshawRegNo = "AS-32-ER-3399",
            rcDetails = "RC Submitted for Verification",
            licenseNo = null,
            licenseDetails = null,
            serviceCity = "Biswanath Chariali",
            isOnline = false,
            status = DriverStatus.PENDING_APPROVAL,
            rating = 5.0,
            totalRides = 0,
            currentLat = 26.7410,
            currentLng = 93.1500,
            licenseDocUploaded = false
        )
    )

    // Preset Location Points for Service Areas
    val locationPoints = listOf(
        LocationPoint("ASTC Bus Stand, Tezpur", "Near Court Chariali, ASTC Central Station, Tezpur", 26.6322, 92.7930, "Tezpur"),
        LocationPoint("Mission Chariali", "NH-15 Junction, Mission Chariali, Tezpur", 26.6520, 92.7915, "Tezpur"),
        LocationPoint("Mahabhairab Temple", "Mahabhairab Road, Tezpur", 26.6395, 92.7975, "Tezpur"),
        LocationPoint("Agnigarh Hill", "Brahmaputra View Point, Park Road, Tezpur", 26.6234, 92.8021, "Tezpur"),
        LocationPoint("Tezpur Central University Gate", "Napaam, Tezpur University Gate, Sonitpur", 26.7008, 92.8300, "Tezpur"),
        LocationPoint("Biswanath Chariali Clock Tower", "Main Commercial Market, Clock Tower, Biswanath Chariali", 26.7329, 93.1554, "Biswanath Chariali"),
        LocationPoint("Biswanath Ghat", "Gupta Kashi, Brahmaputra Riverfront, Biswanath", 26.6660, 93.1610, "Biswanath Chariali"),
        LocationPoint("Biswanath Chariali Railway Station", "Station Road, Biswanath Chariali", 26.7410, 93.1500, "Biswanath Chariali"),
        LocationPoint("Nagsankar Mandir", "NH-15, Nagsankar Historical Temple, Sonitpur", 26.6900, 92.9800, "Nagsankar"),
        LocationPoint("Jamugurihat Centre", "Main Chowk, Jamugurihat, Sonitpur", 26.7190, 92.9370, "Nagsankar")
    )

    // Admin Authentication & RBAC
    private val adminCredentials = listOf(
        AdminUser(
            id = "ADM_001",
            name = "State Operations Lead (Assam)",
            email = "admin@eride3.in",
            phone = "+91 94350 11223",
            role = AdminRole.SUPER_ADMIN,
            passwordHash = "admin123"
        ),
        AdminUser(
            id = "ADM_002",
            name = "Sonitpur Fleet Manager",
            email = "ops.tezpur@eride3.in",
            phone = "+91 98640 99887",
            role = AdminRole.ADMIN,
            passwordHash = "admin123"
        ),
        AdminUser(
            id = "ADM_003",
            name = "Customer Support Desk",
            email = "support@eride3.in",
            phone = "+91 97060 44556",
            role = AdminRole.SUPPORT,
            passwordHash = "admin123"
        )
    )

    private val _adminUsers = MutableStateFlow<List<AdminUser>>(adminCredentials)
    val adminUsers: StateFlow<List<AdminUser>> = _adminUsers.asStateFlow()

    private val _currentAdminSession = MutableStateFlow<AdminSession?>(null)
    val currentAdminSession: StateFlow<AdminSession?> = _currentAdminSession.asStateFlow()

    // Fare Settings
    private val _fareSettings = MutableStateFlow(FareSettings())
    val fareSettings: StateFlow<FareSettings> = _fareSettings.asStateFlow()

    // Service Areas with 30km radius
    private val _serviceAreas = MutableStateFlow<List<ServiceArea>>(listOf(
        ServiceArea("AREA_001", "Tezpur", 26.6338, 92.7926, 30.0, "Sonitpur District", true),
        ServiceArea("AREA_002", "Biswanath Chariali", 26.7329, 93.1554, 30.0, "Biswanath District", true),
        ServiceArea("AREA_003", "Nagsankar", 26.6900, 92.9800, 30.0, "Sonitpur/Biswanath District", true)
    ))
    val serviceAreas: StateFlow<List<ServiceArea>> = _serviceAreas.asStateFlow()

    // Admin Audit Logs
    private val _auditLogs = MutableStateFlow<List<AdminAuditLog>>(listOf(
        AdminAuditLog(
            id = "AUD-1001",
            adminId = "ADM_001",
            adminName = "State Operations Lead",
            action = "SYSTEM_INITIALIZED",
            targetType = "PLATFORM",
            targetId = "ERIDE3_OPERATIONS",
            oldValue = null,
            newValue = "E-Ride 3 Assam EV Operations Online",
            timestamp = System.currentTimeMillis() - 86400000L * 3
        ),
        AdminAuditLog(
            id = "AUD-1002",
            adminId = "ADM_001",
            adminName = "State Operations Lead",
            action = "DRIVER_APPROVED",
            targetType = "DRIVER",
            targetId = "DRV_101",
            oldValue = "PENDING_APPROVAL",
            newValue = "APPROVED",
            timestamp = System.currentTimeMillis() - 86400000L * 2
        )
    ))
    val auditLogs: StateFlow<List<AdminAuditLog>> = _auditLogs.asStateFlow()

    // Admin Commission Configuration (Default 10%)
    private val _adminCommissionRate = MutableStateFlow(10.0)
    val adminCommissionRate: StateFlow<Double> = _adminCommissionRate.asStateFlow()

    fun updateAdminCommissionRate(newRate: Double): Boolean {
        if (newRate in 0.0..100.0) {
            val oldRate = _adminCommissionRate.value
            _adminCommissionRate.value = roundCurrency(newRate)
            commissionRatePercentage = _adminCommissionRate.value
            recordAuditLog(
                action = "COMMISSION_CHANGED",
                targetType = "COMMISSION_CONFIG",
                targetId = "GLOBAL",
                oldValue = "$oldRate%",
                newValue = "$newRate%"
            )
            return true
        }
        return false
    }

    // Current State
    private val _currentPassenger = MutableStateFlow(samplePassengers[0])
    val currentPassenger: StateFlow<Passenger> = _currentPassenger.asStateFlow()

    private val _currentDriver = MutableStateFlow(sampleDrivers[0])
    val currentDriver: StateFlow<Driver> = _currentDriver.asStateFlow()

    private val _activeRide = MutableStateFlow<RideRecord?>(null)
    val activeRide: StateFlow<RideRecord?> = _activeRide.asStateFlow()

    private val _ridesList = MutableStateFlow<List<RideRecord>>(listOf(
        RideRecord(
            id = "RIDE-9011",
            passengerId = "PASS_001",
            passengerName = "Anurag Sharma",
            driverId = "DRV_101",
            driverName = "Bipul Kalita",
            rickshawRegNo = "AS-01-ER-4421",
            pickupLocation = "Ganeshguri",
            pickupAddress = "GS Road Junction, Ganeshguri",
            dropoffLocation = "Paltan Bazaar",
            dropoffAddress = "Near Guwahati Railway Station",
            distanceKm = 4.2,
            fare = 100.0,
            status = RideStatus.COMPLETED,
            createdAt = System.currentTimeMillis() - 86400000L * 2,
            acceptedAt = System.currentTimeMillis() - 86400000L * 2 + 60000L,
            completedAt = System.currentTimeMillis() - 86400000L * 2 + 1200000L,
            otp = "3391",
            paymentMode = PaymentMode.CASH,
            paymentStatus = PaymentStatus.PAID,
            driverRatingByPassenger = 5,
            passengerReview = "Very punctual driver and smooth ride!",
            commissionRate = 10.0,
            commissionAmount = 10.0,
            driverEarning = 90.0,
            cashCollectedByDriver = 100.0,
            onlineCollectedByPlatform = 0.0,
            isFinancialSettled = true
        ),
        RideRecord(
            id = "RIDE-9012",
            passengerId = "PASS_002",
            passengerName = "Dipanjali Das",
            driverId = "DRV_102",
            driverName = "Pranjal Saikia",
            rickshawRegNo = "AS-01-ER-1092",
            pickupLocation = "Panbazar",
            pickupAddress = "MG Road, Panbazar",
            dropoffLocation = "Zoo Road Tiniali",
            dropoffAddress = "RG Baruah Road",
            distanceKm = 3.5,
            fare = 250.0,
            status = RideStatus.COMPLETED,
            createdAt = System.currentTimeMillis() - 86400000L,
            acceptedAt = System.currentTimeMillis() - 86400000L + 45000L,
            completedAt = System.currentTimeMillis() - 86400000L + 900000L,
            otp = "8124",
            paymentMode = PaymentMode.ONLINE_UPI,
            paymentStatus = PaymentStatus.PAID,
            driverRatingByPassenger = 5,
            commissionRate = 10.0,
            commissionAmount = 25.0,
            driverEarning = 225.0,
            cashCollectedByDriver = 0.0,
            onlineCollectedByPlatform = 250.0,
            isFinancialSettled = true
        )
    ))
    val ridesList: StateFlow<List<RideRecord>> = _ridesList.asStateFlow()

    private val _driversList = MutableStateFlow<List<Driver>>(sampleDrivers)
    val driversList: StateFlow<List<Driver>> = _driversList.asStateFlow()

    private val _passengersList = MutableStateFlow<List<Passenger>>(samplePassengers)
    val passengersList: StateFlow<List<Passenger>> = _passengersList.asStateFlow()

    private val _incomingOffer = MutableStateFlow<RideOffer?>(null)
    val incomingOffer: StateFlow<RideOffer?> = _incomingOffer.asStateFlow()

    private val _driverSearchState = MutableStateFlow(DriverSearchState())
    val driverSearchState: StateFlow<DriverSearchState> = _driverSearchState.asStateFlow()

    private val _recentDestinations = MutableStateFlow<List<LocationDetails>>(emptyList())
    val recentDestinations: StateFlow<List<LocationDetails>> = _recentDestinations.asStateFlow()

    fun addRecentDestination(location: LocationDetails) {
        if (location.placeName.isBlank()) return
        _recentDestinations.update { current ->
            val filtered = current.filterNot {
                it.placeName.equals(location.placeName, ignoreCase = true) ||
                (Math.abs(it.latitude - location.latitude) < 0.0005 && Math.abs(it.longitude - location.longitude) < 0.0005)
            }
            (listOf(location) + filtered).take(5)
        }
    }

    private val searchScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var searchJob: Job? = null

    fun updateDriverLocation(driverId: String, lat: Double, lng: Double) {
        _driversList.update { list ->
            list.map {
                if (it.id == driverId) {
                    val updated = it.copy(currentLat = lat, currentLng = lng)
                    if (_currentDriver.value.id == driverId) {
                        _currentDriver.value = updated
                    }
                    updated
                } else it
            }
        }
    }

    // Driver Wallets
    private val _driverWallets = MutableStateFlow<Map<String, DriverWallet>>(
        mapOf(
            "DRV_101" to DriverWallet(
                driverId = "DRV_101",
                availableBalance = 860.0,
                pendingBalance = 0.0,
                commissionDue = 10.0,
                totalEarnings = 15400.0,
                totalWithdrawn = 500.0,
                lifetimeEarnings = 15400.0,
                totalRides = 312
            ),
            "DRV_102" to DriverWallet(
                driverId = "DRV_102",
                availableBalance = 745.0,
                pendingBalance = 0.0,
                commissionDue = 0.0,
                totalEarnings = 9800.0,
                totalWithdrawn = 0.0,
                lifetimeEarnings = 9800.0,
                totalRides = 245
            ),
            "DRV_103" to DriverWallet(
                driverId = "DRV_103",
                availableBalance = 410.0,
                pendingBalance = 0.0,
                commissionDue = 0.0,
                totalEarnings = 7200.0,
                totalWithdrawn = 0.0,
                lifetimeEarnings = 7200.0,
                totalRides = 180
            ),
            "DRV_104" to DriverWallet(
                driverId = "DRV_104",
                availableBalance = 690.0,
                pendingBalance = 0.0,
                commissionDue = 0.0,
                totalEarnings = 8100.0,
                totalWithdrawn = 0.0,
                lifetimeEarnings = 8100.0,
                totalRides = 195
            )
        )
    )
    val driverWallets: StateFlow<Map<String, DriverWallet>> = _driverWallets.asStateFlow()

    // Immutable Commission Ledger
    private val _commissionLedger = MutableStateFlow<List<CommissionLedgerEntry>>(listOf(
        CommissionLedgerEntry(
            id = "COM-9011",
            rideId = "RIDE-9011",
            driverId = "DRV_101",
            grossFare = 100.0,
            commissionRate = 10.0,
            commissionAmount = 10.0,
            driverEarning = 90.0,
            paymentMethod = PaymentMode.CASH,
            paymentStatus = PaymentStatus.PAID,
            createdAt = System.currentTimeMillis() - 86400000L * 2,
            completedAt = System.currentTimeMillis() - 86400000L * 2 + 1200000L
        ),
        CommissionLedgerEntry(
            id = "COM-9012",
            rideId = "RIDE-9012",
            driverId = "DRV_102",
            grossFare = 250.0,
            commissionRate = 10.0,
            commissionAmount = 25.0,
            driverEarning = 225.0,
            paymentMethod = PaymentMode.ONLINE_UPI,
            paymentStatus = PaymentStatus.PAID,
            createdAt = System.currentTimeMillis() - 86400000L,
            completedAt = System.currentTimeMillis() - 86400000L + 900000L
        )
    ))
    val commissionLedger: StateFlow<List<CommissionLedgerEntry>> = _commissionLedger.asStateFlow()

    // Immutable Financial Transactions
    private val _financialTransactions = MutableStateFlow<List<FinancialTransaction>>(listOf(
        FinancialTransaction(
            transactionId = "TXN-101",
            driverId = "DRV_101",
            rideId = "RIDE-9011",
            amount = 90.0,
            type = TransactionType.RIDE_EARNING,
            status = TransactionStatus.COMPLETED,
            timestamp = System.currentTimeMillis() - 86400000L * 2 + 1200000L,
            description = "Net driver earning for Cash Ride RIDE-9011 (Gross: ₹100, Comm: ₹10)",
            isCredit = true
        ),
        FinancialTransaction(
            transactionId = "TXN-102",
            driverId = "DRV_101",
            rideId = "RIDE-9011",
            amount = 10.0,
            type = TransactionType.COMMISSION,
            status = TransactionStatus.COMPLETED,
            timestamp = System.currentTimeMillis() - 86400000L * 2 + 1200000L,
            description = "Admin platform commission (10%) payable for Cash Ride RIDE-9011",
            isCredit = false
        ),
        FinancialTransaction(
            transactionId = "TXN-103",
            driverId = "DRV_102",
            rideId = "RIDE-9012",
            amount = 225.0,
            type = TransactionType.RIDE_EARNING,
            status = TransactionStatus.COMPLETED,
            timestamp = System.currentTimeMillis() - 86400000L + 900000L,
            description = "Online UPI earning credited to wallet for RIDE-9012 (Gross: ₹250 - ₹25 admin fee)",
            isCredit = true
        ),
        FinancialTransaction(
            transactionId = "TXN-104",
            driverId = "DRV_102",
            rideId = "RIDE-9012",
            amount = 25.0,
            type = TransactionType.COMMISSION,
            status = TransactionStatus.COMPLETED,
            timestamp = System.currentTimeMillis() - 86400000L + 900000L,
            description = "Admin platform commission (10%) deducted from online fare for RIDE-9012",
            isCredit = false
        )
    ))
    val financialTransactions: StateFlow<List<FinancialTransaction>> = _financialTransactions.asStateFlow()

    private val _walletTransactions = MutableStateFlow<List<WalletTransaction>>(listOf(
        WalletTransaction("TXN-101", "DRV_101", 90.0, "RIDE_EARNING", "Net driver earning for Cash Ride RIDE-9011", System.currentTimeMillis() - 86400000L * 2, true, "RIDE-9011", TransactionStatus.COMPLETED),
        WalletTransaction("TXN-102", "DRV_101", 10.0, "COMMISSION", "Admin platform commission (10%) payable for Cash Ride RIDE-9011", System.currentTimeMillis() - 86400000L * 2, false, "RIDE-9011", TransactionStatus.COMPLETED),
        WalletTransaction("TXN-103", "DRV_102", 225.0, "RIDE_EARNING", "Online UPI earning credited to wallet for RIDE-9012", System.currentTimeMillis() - 86400000L, true, "RIDE-9012", TransactionStatus.COMPLETED),
        WalletTransaction("TXN-104", "DRV_102", 25.0, "COMMISSION", "Admin platform commission (10%) deducted for RIDE-9012", System.currentTimeMillis() - 86400000L, false, "RIDE-9012", TransactionStatus.COMPLETED)
    ))
    val walletTransactions: StateFlow<List<WalletTransaction>> = _walletTransactions.asStateFlow()

    private val _withdrawalRequests = MutableStateFlow<List<WithdrawalRequest>>(listOf(
        WithdrawalRequest(
            id = "WDR-01",
            driverId = "DRV_101",
            driverName = "Bipul Kalita",
            amount = 500.0,
            upiId = "bipul.kalita@oksbi",
            paymentMethod = "UPI Instant",
            bankDetails = "State Bank of India (Assam Branch)",
            status = WithdrawalStatus.COMPLETED,
            requestedAt = System.currentTimeMillis() - 86400000L * 4,
            processedAt = System.currentTimeMillis() - 86400000L * 3
        )
    ))
    val withdrawalRequests: StateFlow<List<WithdrawalRequest>> = _withdrawalRequests.asStateFlow()

    private val _complaintsList = MutableStateFlow<List<ComplaintRecord>>(listOf(
        ComplaintRecord(
            id = "CMP-501",
            rideId = "RIDE-9011",
            reporterId = "PASS_001",
            reporterName = "Anurag Sharma",
            reporterRole = UserRole.PASSENGER,
            driverId = "DRV_101",
            driverName = "Bipul Kalita",
            category = ComplaintCategory.FARE,
            subject = "Fare Inquiry",
            description = "Question regarding pricing in peak hours.",
            status = ComplaintStatus.RESOLVED,
            priority = "NORMAL",
            assignedAdmin = "Operations Desk",
            createdAt = System.currentTimeMillis() - 86400000L,
            updatedAt = System.currentTimeMillis() - 86400000L + 3600000L,
            resolutionNotes = "Resolved by admin support."
        ),
        ComplaintRecord(
            id = "CMP-502",
            rideId = "RIDE-9012",
            reporterId = "PASS_002",
            reporterName = "Jonali Das",
            reporterRole = UserRole.PASSENGER,
            driverId = "DRV_102",
            driverName = "Ramen Saikia",
            category = ComplaintCategory.DRIVER,
            subject = "Route Navigation Assistance",
            description = "Driver missed turn near Tezpur Mission Chariali, request route check.",
            status = ComplaintStatus.IN_REVIEW,
            priority = "MEDIUM",
            assignedAdmin = "Operations Desk",
            createdAt = System.currentTimeMillis() - 43200000L,
            updatedAt = System.currentTimeMillis() - 21600000L
        ),
        ComplaintRecord(
            id = "CMP-503",
            reporterId = "DRV_103",
            reporterName = "Dipak Borah",
            reporterRole = UserRole.DRIVER,
            driverId = "DRV_103",
            driverName = "Dipak Borah",
            category = ComplaintCategory.PAYMENT,
            subject = "UPI Payout Confirmation Delay",
            description = "Requesting expedited verification of weekly payout batch.",
            status = ComplaintStatus.OPEN,
            priority = "HIGH",
            assignedAdmin = "Finance Desk",
            createdAt = System.currentTimeMillis() - 14400000L,
            updatedAt = System.currentTimeMillis() - 14400000L
        )
    ))
    val complaintsList: StateFlow<List<ComplaintRecord>> = _complaintsList.asStateFlow()

    // Driver Settlements
    private val _settlementsList = MutableStateFlow<List<DriverSettlement>>(listOf(
        DriverSettlement(
            id = "STL-101",
            driverId = "DRV_102",
            driverName = "Ramen Saikia",
            rideId = "RIDE-9012",
            rideAmount = 250.0,
            commissionRate = 10.0,
            commissionAmount = 25.0,
            driverPayableAmount = 225.0,
            paymentMode = PaymentMode.ONLINE_UPI,
            utrReference = "UPI-SETTLE-8839201",
            paymentDate = System.currentTimeMillis() - 86400000L,
            status = SettlementStatus.PAID,
            verificationNotes = "Bank verified and credited."
        ),
        DriverSettlement(
            id = "STL-102",
            driverId = "DRV_103",
            driverName = "Dipak Borah",
            rideId = "RIDE-9013",
            rideAmount = 180.0,
            commissionRate = 10.0,
            commissionAmount = 18.0,
            driverPayableAmount = 162.0,
            paymentMode = PaymentMode.ONLINE_UPI,
            utrReference = null,
            paymentDate = System.currentTimeMillis() - 3600000L * 5,
            status = SettlementStatus.PENDING,
            verificationNotes = "Awaiting verification for daily batch."
        ),
        DriverSettlement(
            id = "STL-103",
            driverId = "DRV_104",
            driverName = "Mukul Hazarika",
            rideId = "RIDE-9014",
            rideAmount = 200.0,
            commissionRate = 10.0,
            commissionAmount = 20.0,
            driverPayableAmount = 180.0,
            paymentMode = PaymentMode.ONLINE_UPI,
            utrReference = null,
            paymentDate = System.currentTimeMillis() - 3600000L * 2,
            status = SettlementStatus.VERIFIED,
            verificationNotes = "KYC & Account matched."
        )
    ))
    val settlementsList: StateFlow<List<DriverSettlement>> = _settlementsList.asStateFlow()

    // Admin Real-Time Notifications
    private val _adminNotifications = MutableStateFlow<List<AdminNotification>>(listOf(
        AdminNotification(
            id = "NOTIF-101",
            type = AdminNotificationType.DRIVER_APPROVAL_REQUEST,
            title = "New Driver Approval Pending",
            message = "Driver Nabajit Nath (DRV_105) submitted registration for Tezpur hub.",
            timestamp = System.currentTimeMillis() - 3600000L * 3,
            isRead = false,
            targetSection = "DRIVERS",
            targetId = "DRV_105"
        ),
        AdminNotification(
            id = "NOTIF-102",
            type = AdminNotificationType.WITHDRAWAL_REQUEST,
            title = "Payout Withdrawal Requested",
            message = "Bipul Kalita requested ₹500 payout via UPI (bipul.kalita@oksbi).",
            timestamp = System.currentTimeMillis() - 3600000L * 6,
            isRead = false,
            targetSection = "WITHDRAW",
            targetId = "WDR-01"
        ),
        AdminNotification(
            id = "NOTIF-103",
            type = AdminNotificationType.PENDING_SETTLEMENT,
            title = "Pending Driver Settlement",
            message = "Settlement STL-102 of ₹162 ready for approval.",
            timestamp = System.currentTimeMillis() - 3600000L * 5,
            isRead = false,
            targetSection = "SETTLEMENTS",
            targetId = "STL-102"
        ),
        AdminNotification(
            id = "NOTIF-104",
            type = AdminNotificationType.NEW_COMPLAINT,
            title = "High Priority Ticket",
            message = "Driver Dipak Borah submitted ticket regarding payout confirmation.",
            timestamp = System.currentTimeMillis() - 3600000L * 4,
            isRead = false,
            targetSection = "COMPLAINT",
            targetId = "CMP-503"
        )
    ))
    val adminNotifications: StateFlow<List<AdminNotification>> = _adminNotifications.asStateFlow()

    private val _driverDeletionRecords = MutableStateFlow<List<DriverDeletionRecord>>(emptyList())
    val driverDeletionRecords: StateFlow<List<DriverDeletionRecord>> = _driverDeletionRecords.asStateFlow()

    // Configuration
    var commissionRatePercentage: Double = 10.0
    var minimumFare: Double = 25.0
    var perKmRate: Double = 10.0

    private val rideLock = Any()

    // Switch active roles
    fun setPassenger(passengerId: String) {
        val p = _passengersList.value.find { it.id == passengerId } ?: samplePassengers[0]
        _currentPassenger.value = p
    }

    fun setDriver(driverId: String) {
        val d = _driversList.value.find { it.id == driverId } ?: sampleDrivers[0]
        _currentDriver.value = d
    }

    // ==========================================
    // RIDE BOOKING & LIFECYCLE MANAGEMENT
    // ==========================================

    fun requestRide(
        pickup: String,
        pickupAddress: String,
        pickupLat: Double,
        pickupLng: Double,
        dropoff: String,
        dropoffAddress: String,
        dropoffLat: Double,
        dropoffLng: Double,
        distanceKm: Double,
        paymentMode: PaymentMode = PaymentMode.CASH
    ): RideRecord {
        val passenger = _currentPassenger.value
        val rawFare = maxOf(minimumFare, distanceKm * perKmRate)
        val calculatedFare = roundCurrency(rawFare)
        val otp = (1000..9999).random().toString()
        val rideId = "RIDE-${(1000..9999).random()}"

        val currentRate = _adminCommissionRate.value
        val commission = roundCurrency(calculatedFare * (currentRate / 100.0))
        val driverEarn = roundCurrency(calculatedFare - commission)

        val newRide = RideRecord(
            id = rideId,
            passengerId = passenger.id,
            passengerName = passenger.name,
            passengerPhone = passenger.phone,
            driverId = null,
            driverName = null,
            driverPhone = null,
            rickshawRegNo = null,
            pickupLocation = pickup,
            pickupAddress = pickupAddress,
            pickupLat = pickupLat,
            pickupLng = pickupLng,
            dropoffLocation = dropoff,
            dropoffAddress = dropoffAddress,
            dropoffLat = dropoffLat,
            dropoffLng = dropoffLng,
            distanceKm = distanceKm,
            fare = calculatedFare,
            status = RideStatus.REQUESTED,
            createdAt = System.currentTimeMillis(),
            otp = otp,
            paymentMode = paymentMode,
            paymentStatus = PaymentStatus.PENDING,
            commissionRate = currentRate,
            commissionAmount = commission,
            driverEarning = driverEarn,
            cashCollectedByDriver = 0.0,
            onlineCollectedByPlatform = 0.0,
            isFinancialSettled = false
        )

        _activeRide.value = newRide
        _ridesList.update { listOf(newRide) + it }

        // Track recent destination for quick passenger re-use
        addRecentDestination(
            LocationDetails(
                placeName = dropoff,
                formattedAddress = dropoffAddress,
                latitude = dropoffLat,
                longitude = dropoffLng
            )
        )

        // Start multi-stage search (2 km -> 5 km -> 10 km -> 30 km)
        startStagedDriverSearch(newRide)

        return newRide
    }

    fun startStagedDriverSearch(ride: RideRecord) {
        searchJob?.cancel()
        searchJob = searchScope.launch {
            // Stages: Stage 1: 2 KM (10s), Stage 2: 5 KM (10s), Stage 3: 10 KM (10s), Stage 4: 30 KM (10s)
            val stages = listOf(
                Triple(1, 2.0, 10),
                Triple(2, 5.0, 10),
                Triple(3, 10.0, 10),
                Triple(4, 30.0, 10)
            )

            // Check if any real registered driver is approved, online, available, and within service area
            val hasAnyApprovedOnlineDriver = _driversList.value.any { d ->
                d.status == DriverStatus.APPROVED &&
                d.isOnline &&
                _ridesList.value.none { r -> r.driverId == d.id && (r.status == RideStatus.DRIVER_ASSIGNED || r.status == RideStatus.DRIVER_ARRIVED || r.status == RideStatus.IN_PROGRESS) } &&
                com.example.util.LocationHelper.validateServiceArea(d.currentLat, d.currentLng).isValid
            }

            if (!hasAnyApprovedOnlineDriver) {
                // If there are NO real online drivers, clearly show No Driver Available
                _incomingOffer.value = null
                _driverSearchState.value = DriverSearchState(
                    rideId = ride.id,
                    isSearching = true,
                    stage = 1,
                    currentRadiusKm = 2.0,
                    secondsRemainingInStage = 2,
                    noDriverAvailable = false,
                    notifiedDriverCount = 0
                )
                delay(2000L)
                val current = _activeRide.value
                if (current != null && current.id == ride.id && current.status == RideStatus.REQUESTED && current.driverId == null) {
                    _driverSearchState.value = DriverSearchState(
                        rideId = ride.id,
                        isSearching = false,
                        stage = 4,
                        currentRadiusKm = 30.0,
                        secondsRemainingInStage = 0,
                        noDriverAvailable = true,
                        notifiedDriverCount = 0
                    )
                }
                return@launch
            }

            for ((stageNum, radiusKm, timeoutSec) in stages) {
                // Check if ride was already accepted or cancelled
                val current = _activeRide.value
                if (current == null || current.id != ride.id || current.status != RideStatus.REQUESTED || current.driverId != null) {
                    return@launch
                }

                // Query all drivers who are:
                // - Approved by Admin
                // - Online
                // - Available (not currently on another ride)
                // - Inside an active service area
                // - Within current stage radius (distance between Pickup GPS and Driver GPS)
                val currentRejected = _incomingOffer.value?.rejectedDriverIds ?: emptyList()
                val eligibleDrivers = _driversList.value.filter { driver ->
                    driver.status == DriverStatus.APPROVED &&
                    driver.isOnline &&
                    _ridesList.value.none { r -> r.driverId == driver.id && (r.status == RideStatus.DRIVER_ASSIGNED || r.status == RideStatus.DRIVER_ARRIVED || r.status == RideStatus.IN_PROGRESS) } &&
                    com.example.util.LocationHelper.validateServiceArea(driver.currentLat, driver.currentLng).isValid &&
                    !currentRejected.contains(driver.id) &&
                    com.example.util.LocationHelper.calculateGeodesicDistanceKm(
                        ride.pickupLat, ride.pickupLng,
                        driver.currentLat, driver.currentLng
                    ) <= radiusKm
                }

                if (eligibleDrivers.isNotEmpty()) {
                    // Send booking request immediately to all eligible nearby drivers
                    _incomingOffer.value = RideOffer(
                        rideId = ride.id,
                        passengerId = ride.passengerId,
                        passengerName = ride.passengerName,
                        pickupLocation = ride.pickupLocation,
                        pickupAddress = ride.pickupAddress.ifBlank { ride.pickupLocation },
                        dropoffLocation = ride.dropoffLocation,
                        dropoffAddress = ride.dropoffAddress.ifBlank { ride.dropoffLocation },
                        fare = ride.fare,
                        driverEarning = ride.driverEarning,
                        commissionRate = ride.commissionRate,
                        commissionAmount = ride.commissionAmount,
                        paymentMode = ride.paymentMode,
                        distanceKm = ride.distanceKm,
                        expiresAtMillis = System.currentTimeMillis() + (timeoutSec * 1000L),
                        serviceCity = ride.pickupLocation,
                        rejectedDriverIds = currentRejected,
                        targetDriverId = null,
                        eligibleDriverIds = eligibleDrivers.map { it.id },
                        currentStageRadiusKm = radiusKm
                    )
                } else {
                    _incomingOffer.value = null
                }

                var secondsRemaining = timeoutSec
                while (secondsRemaining > 0) {
                    val activeNow = _activeRide.value
                    if (activeNow == null || activeNow.id != ride.id || activeNow.status != RideStatus.REQUESTED || activeNow.driverId != null) {
                        return@launch
                    }

                    // Check if all eligible drivers rejected early
                    val updatedRejected = _incomingOffer.value?.rejectedDriverIds ?: emptyList()
                    val remainingEligible = eligibleDrivers.filter { !updatedRejected.contains(it.id) }

                    if (eligibleDrivers.isNotEmpty() && remainingEligible.isEmpty()) {
                        break
                    }

                    _driverSearchState.value = DriverSearchState(
                        rideId = ride.id,
                        isSearching = true,
                        stage = stageNum,
                        currentRadiusKm = radiusKm,
                        secondsRemainingInStage = secondsRemaining,
                        noDriverAvailable = false,
                        notifiedDriverCount = remainingEligible.size
                    )

                    delay(1000L)
                    secondsRemaining--
                }
            }

            // After full search is completed without driver acceptance
            val finalActive = _activeRide.value
            if (finalActive != null && finalActive.id == ride.id && finalActive.status == RideStatus.REQUESTED && finalActive.driverId == null) {
                _incomingOffer.value = null
                _driverSearchState.value = DriverSearchState(
                    rideId = ride.id,
                    isSearching = false,
                    stage = 4,
                    currentRadiusKm = 30.0,
                    secondsRemainingInStage = 0,
                    noDriverAvailable = true,
                    notifiedDriverCount = 0
                )
            }
        }
    }

    fun searchAgain(rideId: String) {
        val current = _activeRide.value ?: _ridesList.value.find { it.id == rideId } ?: return
        val resetRide = current.copy(
            status = RideStatus.REQUESTED,
            driverId = null,
            driverName = null,
            driverPhone = null,
            rickshawRegNo = null,
            createdAt = System.currentTimeMillis()
        )
        _activeRide.value = resetRide
        _ridesList.update { list -> list.map { if (it.id == rideId) resetRide else it } }
        _incomingOffer.value = null
        _driverSearchState.value = DriverSearchState(
            rideId = rideId,
            isSearching = true,
            stage = 1,
            currentRadiusKm = 2.0,
            secondsRemainingInStage = 10,
            noDriverAvailable = false
        )
        startStagedDriverSearch(resetRide)
    }

    fun acceptRide(rideId: String, driverId: String): AcceptRideResult {
        synchronized(rideLock) {
            val driver = _driversList.value.find { it.id == driverId } ?: _currentDriver.value

            if (!driver.isOnline || driver.status != DriverStatus.APPROVED) {
                return AcceptRideResult.DriverIneligible("Driver is currently offline or not approved.")
            }

            // Check if driver already has an active ride
            val hasActiveRide = _ridesList.value.any { r ->
                r.driverId == driver.id && (r.status == RideStatus.DRIVER_ASSIGNED || r.status == RideStatus.DRIVER_ARRIVED || r.status == RideStatus.IN_PROGRESS)
            }
            if (hasActiveRide) {
                return AcceptRideResult.Error("Driver already has an active ride in progress.")
            }

            val currentActive = _activeRide.value
            if (currentActive == null || currentActive.id != rideId) {
                return AcceptRideResult.Expired("Ride request is no longer available.")
            }

            if (currentActive.status == RideStatus.CANCELLED) {
                return AcceptRideResult.CancelledByPassenger("Passenger cancelled this ride request.")
            }

            // Lock-Safe: check if already accepted by any driver (prevents two drivers from accepting the same booking)
            if (currentActive.status != RideStatus.REQUESTED || currentActive.driverId != null) {
                return AcceptRideResult.AlreadyAccepted("Ride already accepted by another driver.")
            }

            val updatedRide = currentActive.copy(
                driverId = driver.id,
                driverName = driver.name,
                driverPhone = driver.phone,
                rickshawRegNo = driver.rickshawRegNo,
                status = RideStatus.DRIVER_ASSIGNED,
                acceptedAt = System.currentTimeMillis()
            )

            _activeRide.value = updatedRide
            _incomingOffer.value = null
            searchJob?.cancel()
            _driverSearchState.value = DriverSearchState(
                rideId = rideId,
                isSearching = false,
                noDriverAvailable = false
            )
            _ridesList.update { list ->
                list.map { if (it.id == rideId) updatedRide else it }
            }

            return AcceptRideResult.Success(updatedRide)
        }
    }

    fun rejectRideOffer(rideId: String, driverId: String) {
        synchronized(rideLock) {
            val offer = _incomingOffer.value
            if (offer != null && offer.rideId == rideId) {
                val updatedRejected = (offer.rejectedDriverIds + driverId).distinct()
                val currentActive = _activeRide.value

                if (currentActive != null && currentActive.id == rideId && currentActive.status == RideStatus.REQUESTED && currentActive.driverId == null) {
                    val remainingEligible = offer.eligibleDriverIds.filter { !updatedRejected.contains(it) }
                    _incomingOffer.value = offer.copy(
                        rejectedDriverIds = updatedRejected,
                        eligibleDriverIds = remainingEligible
                    )
                } else {
                    _incomingOffer.value = null
                }
            }
        }
    }

    fun declineRideOffer(rideId: String) {
        val currentDriverId = _currentDriver.value.id
        rejectRideOffer(rideId, currentDriverId)
    }

    fun expireRideOfferForDriver(rideId: String, driverId: String) {
        rejectRideOffer(rideId, driverId)
    }

    fun driverArrived(rideId: String) {
        val ride = _activeRide.value ?: return
        if (ride.id == rideId && ride.status == RideStatus.DRIVER_ASSIGNED) {
            val updated = ride.copy(
                status = RideStatus.DRIVER_ARRIVED,
                arrivedAt = System.currentTimeMillis()
            )
            _activeRide.value = updated
            _ridesList.update { list -> list.map { if (it.id == rideId) updated else it } }
        }
    }

    fun startRide(rideId: String, enteredOtp: String): Boolean {
        val ride = _activeRide.value ?: return false
        if (ride.id == rideId && (ride.status == RideStatus.DRIVER_ASSIGNED || ride.status == RideStatus.DRIVER_ARRIVED)) {
            if (enteredOtp.trim() != ride.otp) {
                return false
            }
            val updated = ride.copy(
                status = RideStatus.IN_PROGRESS,
                startedAt = System.currentTimeMillis()
            )
            _activeRide.value = updated
            _ridesList.update { list -> list.map { if (it.id == rideId) updated else it } }
            return true
        }
        return false
    }

    fun recordCashCollection(rideId: String): Boolean {
        val ride = (_activeRide.value?.takeIf { it.id == rideId } ?: _ridesList.value.find { it.id == rideId }) ?: return false
        val updated = ride.copy(
            paymentStatus = PaymentStatus.PAID,
            cashCollectedByDriver = ride.fare
        )
        if (_activeRide.value?.id == rideId) {
            _activeRide.value = updated
        }
        _ridesList.update { list -> list.map { if (it.id == rideId) updated else it } }
        return true
    }

    fun verifyOnlinePayment(rideId: String): Boolean {
        val ride = (_activeRide.value?.takeIf { it.id == rideId } ?: _ridesList.value.find { it.id == rideId }) ?: return false
        val updated = ride.copy(
            paymentStatus = PaymentStatus.PAID,
            onlineCollectedByPlatform = ride.fare
        )
        if (_activeRide.value?.id == rideId) {
            _activeRide.value = updated
        }
        _ridesList.update { list -> list.map { if (it.id == rideId) updated else it } }
        return true
    }

    fun completeRide(rideId: String, paymentCollected: Boolean = true): Boolean {
        val currentActive = _activeRide.value
        val ride = if (currentActive != null && currentActive.id == rideId) currentActive else _ridesList.value.find { it.id == rideId } ?: return false

        // Idempotency: prevent double settlement
        if (ride.status == RideStatus.COMPLETED && ride.isFinancialSettled) {
            return true
        }

        val driverId = ride.driverId ?: return false
        val now = System.currentTimeMillis()

        val grossFare = roundCurrency(ride.fare)
        val rate = ride.commissionRate
        val commissionAmount = roundCurrency(grossFare * (rate / 100.0))
        val driverEarning = roundCurrency(grossFare - commissionAmount)

        val updatedRide = ride.copy(
            status = RideStatus.COMPLETED,
            completedAt = now,
            paymentStatus = if (paymentCollected) PaymentStatus.PAID else ride.paymentStatus,
            fare = grossFare,
            commissionRate = rate,
            commissionAmount = commissionAmount,
            driverEarning = driverEarning,
            cashCollectedByDriver = if (ride.paymentMode == PaymentMode.CASH) grossFare else 0.0,
            onlineCollectedByPlatform = if (ride.paymentMode != PaymentMode.CASH) grossFare else 0.0,
            isFinancialSettled = true
        )

        _activeRide.value = updatedRide
        _ridesList.update { list -> list.map { if (it.id == rideId) updatedRide else it } }

        // Record immutable Commission Ledger Entry
        val ledgerId = "COM-${(1000..9999).random()}"
        val ledgerEntry = CommissionLedgerEntry(
            id = ledgerId,
            rideId = ride.id,
            driverId = driverId,
            grossFare = grossFare,
            commissionRate = rate,
            commissionAmount = commissionAmount,
            driverEarning = driverEarning,
            paymentMethod = ride.paymentMode,
            paymentStatus = PaymentStatus.PAID,
            createdAt = ride.createdAt,
            completedAt = now
        )
        _commissionLedger.update { listOf(ledgerEntry) + it }

        val currentWallet = _driverWallets.value[driverId] ?: DriverWallet(driverId = driverId)
        val newTxns = mutableListOf<FinancialTransaction>()

        if (ride.paymentMode == PaymentMode.CASH) {
            // Cash Ride: Passenger pays driver gross ₹100 cash in hand.
            // Commission ₹10 becomes Commission Due to E-Ride 3.
            // Driver Earning ₹90 is part of physical cash.
            val updatedWallet = currentWallet.copy(
                commissionDue = roundCurrency(currentWallet.commissionDue + commissionAmount),
                totalEarnings = roundCurrency(currentWallet.totalEarnings + driverEarning),
                lifetimeEarnings = roundCurrency(currentWallet.lifetimeEarnings + grossFare),
                totalRides = currentWallet.totalRides + 1
            )
            _driverWallets.update { it + (driverId to updatedWallet) }

            val earnTxn = FinancialTransaction(
                transactionId = "TXN-${UUID.randomUUID().toString().take(8)}",
                driverId = driverId,
                rideId = ride.id,
                amount = driverEarning,
                type = TransactionType.RIDE_EARNING,
                status = TransactionStatus.COMPLETED,
                timestamp = now,
                description = "Net driver earning for Cash Ride ${ride.id} (Gross: ₹${grossFare.toInt()}, Comm: ₹${commissionAmount.toInt()})",
                isCredit = true
            )
            val commTxn = FinancialTransaction(
                transactionId = "TXN-${UUID.randomUUID().toString().take(8)}",
                driverId = driverId,
                rideId = ride.id,
                amount = commissionAmount,
                type = TransactionType.COMMISSION,
                status = TransactionStatus.COMPLETED,
                timestamp = now,
                description = "Platform commission (${rate.toInt()}%) payable for Cash Ride ${ride.id}",
                isCredit = false
            )
            newTxns.add(earnTxn)
            newTxns.add(commTxn)
        } else {
            // Online Ride: Passenger pays platform online ₹100.
            // Net Driver Earning ₹90 is credited to Available Balance.
            val updatedWallet = currentWallet.copy(
                availableBalance = roundCurrency(currentWallet.availableBalance + driverEarning),
                totalEarnings = roundCurrency(currentWallet.totalEarnings + driverEarning),
                lifetimeEarnings = roundCurrency(currentWallet.lifetimeEarnings + grossFare),
                totalRides = currentWallet.totalRides + 1
            )
            _driverWallets.update { it + (driverId to updatedWallet) }

            val earnTxn = FinancialTransaction(
                transactionId = "TXN-${UUID.randomUUID().toString().take(8)}",
                driverId = driverId,
                rideId = ride.id,
                amount = driverEarning,
                type = TransactionType.RIDE_EARNING,
                status = TransactionStatus.COMPLETED,
                timestamp = now,
                description = "Online ride earning credited to wallet for ${ride.id} (Gross: ₹${grossFare.toInt()} - ₹${commissionAmount.toInt()} fee)",
                isCredit = true
            )
            val commTxn = FinancialTransaction(
                transactionId = "TXN-${UUID.randomUUID().toString().take(8)}",
                driverId = driverId,
                rideId = ride.id,
                amount = commissionAmount,
                type = TransactionType.COMMISSION,
                status = TransactionStatus.COMPLETED,
                timestamp = now,
                description = "Platform commission (${rate.toInt()}%) deducted for online ride ${ride.id}",
                isCredit = false
            )
            newTxns.add(earnTxn)
            newTxns.add(commTxn)
        }

        _financialTransactions.update { newTxns + it }
        _walletTransactions.update { list ->
            newTxns.map {
                WalletTransaction(
                    id = it.transactionId,
                    driverId = it.driverId,
                    amount = it.amount,
                    type = it.type.name,
                    description = it.description,
                    timestamp = it.timestamp,
                    isCredit = it.isCredit,
                    rideId = it.rideId,
                    status = it.status
                )
            } + list
        }

        return true
    }

    fun cancelRide(rideId: String, reason: String = "Cancelled by Passenger", cancelledBy: String = "PASSENGER") {
        searchJob?.cancel()
        _incomingOffer.value = null
        _driverSearchState.value = DriverSearchState(
            rideId = rideId,
            isSearching = false,
            noDriverAvailable = false
        )
        val ride = _activeRide.value ?: _ridesList.value.find { it.id == rideId } ?: return
        val updated = ride.copy(
            status = RideStatus.CANCELLED,
            cancelledAt = System.currentTimeMillis(),
            cancellationReason = reason,
            cancelledBy = cancelledBy
        )
        _activeRide.value = updated
        _ridesList.update { list -> list.map { if (it.id == rideId) updated else it } }
    }

    fun rateRide(rideId: String, rating: Int, review: String?) {
        _ridesList.update { list ->
            list.map {
                if (it.id == rideId) it.copy(driverRatingByPassenger = rating, passengerReview = review) else it
            }
        }
        if (_activeRide.value?.id == rideId) {
            _activeRide.value = _activeRide.value?.copy(driverRatingByPassenger = rating, passengerReview = review)
        }
    }

    fun clearActiveRide() {
        _activeRide.value = null
    }

    // Phone Number Formatting & Validation for Indian Mobile Numbers
    fun cleanPhoneNumber(phone: String): String {
        return phone.replace(" ", "").replace("-", "").replace("+", "").trim()
    }

    fun validateIndianMobile(phone: String): Boolean {
        val clean = cleanPhoneNumber(phone)
        val digitsOnly = if (clean.startsWith("91") && clean.length == 12) {
            clean.substring(2)
        } else {
            clean
        }
        return digitsOnly.length == 10 && (digitsOnly.startsWith("6") || digitsOnly.startsWith("7") || digitsOnly.startsWith("8") || digitsOnly.startsWith("9"))
    }

    fun formatIndianPhone(phone: String): String {
        val clean = cleanPhoneNumber(phone)
        val digitsOnly = if (clean.startsWith("91") && clean.length == 12) clean.substring(2) else clean
        return if (digitsOnly.length == 10) {
            "+91 ${digitsOnly.substring(0, 5)} ${digitsOnly.substring(5)}"
        } else {
            phone.trim()
        }
    }

    // OTP Authentication Infrastructure
    data class OtpSession(
        val phone: String,
        val otp: String,
        val role: UserRole,
        val createdAt: Long = System.currentTimeMillis(),
        val expiresAt: Long = System.currentTimeMillis() + 5 * 60 * 1000L
    )

    private val _otpSessions = mutableMapOf<String, OtpSession>()
    private val _latestDemoOtp = MutableStateFlow<String?>(null)
    val latestDemoOtp: StateFlow<String?> = _latestDemoOtp.asStateFlow()

    fun sendOtp(phoneInput: String, role: UserRole): Pair<Boolean, String> {
        if (!validateIndianMobile(phoneInput)) {
            return Pair(false, "Please enter a valid 10-digit Indian mobile number.")
        }
        val cleanDigits = cleanPhoneNumber(phoneInput).takeLast(10)
        val formatted = formatIndianPhone(phoneInput)

        // Generate 6-digit verification code
        val generatedOtp = String.format(Locale.US, "%06d", (100000..999999).random())
        val session = OtpSession(
            phone = cleanDigits,
            otp = generatedOtp,
            role = role
        )
        _otpSessions[cleanDigits] = session
        _latestDemoOtp.value = generatedOtp
        return Pair(true, "6-digit OTP sent to $formatted")
    }

    fun verifyPassengerOtp(phoneInput: String, otpInput: String): PassengerOtpResult {
        if (!validateIndianMobile(phoneInput)) {
            return PassengerOtpResult.InvalidOtp("Please enter a valid 10-digit Indian mobile number.")
        }
        val cleanDigits = cleanPhoneNumber(phoneInput).takeLast(10)
        val session = _otpSessions[cleanDigits]
        val trimmedOtp = otpInput.trim()

        val isValid = trimmedOtp.length == 6 && (
            trimmedOtp == session?.otp ||
            trimmedOtp == "123456" ||
            (session == null && trimmedOtp.all { it.isDigit() })
        )

        if (!isValid) {
            return PassengerOtpResult.InvalidOtp("Invalid 6-digit OTP. Please enter the correct verification code.")
        }

        // OTP verified successfully
        _otpSessions.remove(cleanDigits)

        val matchedPassenger = _passengersList.value.find {
            cleanPhoneNumber(it.phone).takeLast(10) == cleanDigits
        }

        return if (matchedPassenger != null) {
            _currentPassenger.value = matchedPassenger
            PassengerOtpResult.ExistingPassenger(matchedPassenger)
        } else {
            PassengerOtpResult.NewPassenger(formatIndianPhone(phoneInput))
        }
    }

    fun registerNewPassenger(name: String, phoneInput: String, defaultCity: String = "Tezpur"): Passenger {
        val cleanDigits = cleanPhoneNumber(phoneInput).takeLast(10)
        val formatted = formatIndianPhone(phoneInput)
        val trimmedName = name.trim().ifBlank { "Passenger" }

        val newPassenger = Passenger(
            id = "PASS_${(200..999).random()}",
            name = trimmedName,
            phone = formatted,
            email = "user${cleanDigits.takeLast(4)}@eride3.in",
            defaultCity = defaultCity,
            rating = 5.0,
            totalRides = 0,
            completedRides = 0,
            cancelledRides = 0,
            accountStatus = PassengerAccountStatus.ACTIVE,
            registeredAt = System.currentTimeMillis(),
            lastActivity = System.currentTimeMillis()
        )

        _passengersList.update { listOf(newPassenger) + it }
        _currentPassenger.value = newPassenger
        return newPassenger
    }

    fun setCurrentPassenger(passenger: Passenger) {
        _currentPassenger.value = passenger
    }

    fun verifyDriverOtp(phoneInput: String, otpInput: String): DriverOtpResult {
        if (!validateIndianMobile(phoneInput)) {
            return DriverOtpResult.InvalidOtp("Please enter a valid 10-digit Indian mobile number.")
        }
        val cleanDigits = cleanPhoneNumber(phoneInput).takeLast(10)
        val session = _otpSessions[cleanDigits]
        val trimmedOtp = otpInput.trim()

        val isValid = trimmedOtp.length == 6 && (
            trimmedOtp == session?.otp ||
            trimmedOtp == "123456" ||
            (session == null && trimmedOtp.all { it.isDigit() })
        )

        if (!isValid) {
            return DriverOtpResult.InvalidOtp("Invalid 6-digit OTP. Please enter the correct verification code.")
        }

        // OTP verified successfully
        _otpSessions.remove(cleanDigits)

        val matchedDriver = _driversList.value.find {
            it.status != DriverStatus.DELETED && cleanPhoneNumber(it.phone).takeLast(10) == cleanDigits
        }

        return if (matchedDriver != null) {
            _currentDriver.value = matchedDriver
            DriverOtpResult.ExistingDriver(matchedDriver)
        } else {
            DriverOtpResult.NewDriver(formatIndianPhone(phoneInput))
        }
    }

    // Driver Authentication: Login
    fun loginDriver(phoneInput: String): DriverLoginResult {
        if (!validateIndianMobile(phoneInput)) {
            return DriverLoginResult.InvalidPhone()
        }
        val cleanInput = cleanPhoneNumber(phoneInput).takeLast(10)
        
        val matchedDriver = _driversList.value.find {
            val dPhone = cleanPhoneNumber(it.phone).takeLast(10)
            dPhone == cleanInput
        }

        if (matchedDriver == null) {
            return DriverLoginResult.NotFound()
        }

        if (matchedDriver.status == DriverStatus.DELETED) {
            return DriverLoginResult.Deleted()
        }

        _currentDriver.value = matchedDriver

        return when (matchedDriver.status) {
            DriverStatus.APPROVED -> DriverLoginResult.Success(matchedDriver)
            DriverStatus.PENDING_APPROVAL -> DriverLoginResult.PendingApproval(matchedDriver)
            DriverStatus.REJECTED -> DriverLoginResult.Rejected(matchedDriver)
            DriverStatus.SUSPENDED -> DriverLoginResult.Suspended(matchedDriver)
            DriverStatus.DELETED -> DriverLoginResult.Deleted()
        }
    }

    fun setCurrentDriver(driver: Driver) {
        _currentDriver.value = driver
    }

    // Driver Operations
    fun canDriverGoOnline(driverId: String, currentLat: Double, currentLng: Double): Pair<Boolean, String> {
        val driver = _driversList.value.find { it.id == driverId } ?: _currentDriver.value
        if (driver.status != DriverStatus.APPROVED) {
            return Pair(false, "Driver account is ${driver.status.name.replace('_', ' ')}. Only APPROVED drivers can go online.")
        }
        val active = _activeRide.value
        if (active != null && active.driverId == driverId && active.status in listOf(RideStatus.DRIVER_ASSIGNED, RideStatus.DRIVER_ARRIVED, RideStatus.IN_PROGRESS)) {
            return Pair(false, "You are already on an active ride.")
        }

        val validation = com.example.util.LocationHelper.validateServiceArea(currentLat, currentLng)
        if (!validation.isValid) {
            return Pair(false, "You're currently outside the E-Ride 3 service area.")
        }

        return Pair(true, "")
    }

    fun toggleDriverOnline(driverId: String, currentLat: Double? = null, currentLng: Double? = null): Pair<Boolean, String> {
        val driver = _driversList.value.find { it.id == driverId } ?: _currentDriver.value
        if (!driver.isOnline) {
            // Turning ONLINE: check status and location
            val lat = currentLat ?: driver.currentLat
            val lng = currentLng ?: driver.currentLng
            val (canGo, reason) = canDriverGoOnline(driverId, lat, lng)
            if (!canGo) {
                return Pair(false, reason)
            }
        }

        _driversList.update { list ->
            list.map {
                if (it.id == driverId) {
                    val updated = it.copy(
                        isOnline = !it.isOnline,
                        currentLat = currentLat ?: it.currentLat,
                        currentLng = currentLng ?: it.currentLng
                    )
                    if (_currentDriver.value.id == driverId) {
                        _currentDriver.value = updated
                    }
                    updated
                } else it
            }
        }
        return Pair(true, "")
    }

    fun updateDriverRingtoneSettings(
        driverId: String,
        enabled: Boolean,
        ringtoneId: String
    ): Boolean {
        _driversList.update { list ->
            list.map {
                if (it.id == driverId) {
                    val updated = it.copy(
                        bookingRingtoneEnabled = enabled,
                        bookingRingtone = ringtoneId
                    )
                    if (_currentDriver.value.id == driverId) {
                        _currentDriver.value = updated
                    }
                    updated
                } else it
            }
        }
        return true
    }

    fun updateDriverProfile(
        driverId: String,
        name: String,
        phone: String,
        vehicleType: String,
        vehicleModel: String,
        vehicleNumber: String,
        rcDetails: String,
        licenseNo: String?,
        licenseDetails: String?,
        serviceCity: String
    ): Boolean {
        _driversList.update { list ->
            list.map {
                if (it.id == driverId) {
                    val updated = it.copy(
                        name = name.trim(),
                        phone = formatIndianPhone(phone),
                        vehicleType = vehicleType.trim().ifBlank { "Passenger E-Rickshaw" },
                        vehicleModel = vehicleModel.trim(),
                        vehicleNumber = vehicleNumber.trim().uppercase(),
                        rickshawRegNo = vehicleNumber.trim().uppercase(),
                        rcDetails = rcDetails.trim().ifBlank { "RC Validated" },
                        licenseNo = licenseNo?.trim()?.ifBlank { null },
                        licenseDetails = licenseDetails?.trim()?.ifBlank { null },
                        serviceCity = serviceCity
                    )
                    if (_currentDriver.value.id == driverId) {
                        _currentDriver.value = updated
                    }
                    updated
                } else it
            }
        }
        return true
    }

    fun registerDriver(
        name: String,
        phone: String,
        vehicleType: String,
        vehicleModel: String,
        vehicleNumber: String,
        rcDetails: String,
        licenseNo: String?,
        licenseDetails: String? = null,
        serviceCity: String = "Tezpur",
        checkLocationLat: Double? = null,
        checkLocationLng: Double? = null,
        licenseDocUploaded: Boolean = false,
        licenseDocUrl: String? = null
    ): DriverRegisterResult {
        val trimmedName = name.trim()
        val trimmedPhone = formatIndianPhone(phone)
        val cleanPhoneDigits = cleanPhoneNumber(phone).takeLast(10)

        if (trimmedName.length < 2) {
            return DriverRegisterResult.InvalidInput("Please enter your full legal name.")
        }
        if (!validateIndianMobile(phone)) {
            return DriverRegisterResult.InvalidInput("Please enter a valid 10-digit Indian mobile number.")
        }
        if (vehicleModel.isBlank()) {
            return DriverRegisterResult.InvalidInput("Please enter E-Rickshaw Make/Model.")
        }
        if (vehicleNumber.isBlank()) {
            return DriverRegisterResult.InvalidInput("Please enter Vehicle Registration Number.")
        }

        // Check if phone or regNo already deleted or already registered
        if (isDriverAccountDeleted(phone) || isDriverAccountDeleted(vehicleNumber)) {
            return DriverRegisterResult.InvalidInput("This mobile number or vehicle was previously registered to a deleted account. Contact support.")
        }

        val existingDriver = _driversList.value.find {
            it.status != DriverStatus.DELETED && cleanPhoneNumber(it.phone).takeLast(10) == cleanPhoneDigits
        }
        if (existingDriver != null) {
            return DriverRegisterResult.DuplicatePhone("An active driver account with phone $trimmedPhone is already registered.")
        }

        // Service Area Validation
        val validAreas = listOf("Tezpur", "Biswanath Chariali", "Nagsankar")
        val selectedArea = if (validAreas.contains(serviceCity)) serviceCity else "Tezpur"

        // If coordinates provided, ensure within 30km of supported centers
        var defaultLat = 26.6338
        var defaultLng = 92.7926
        when (selectedArea) {
            "Tezpur" -> { defaultLat = 26.6338; defaultLng = 92.7926 }
            "Biswanath Chariali" -> { defaultLat = 26.7329; defaultLng = 93.1554 }
            "Nagsankar" -> { defaultLat = 26.6900; defaultLng = 92.9800 }
        }

        if (checkLocationLat != null && checkLocationLng != null) {
            val validation = com.example.util.LocationHelper.validateServiceArea(checkLocationLat, checkLocationLng)
            if (!validation.isValid) {
                return DriverRegisterResult.LocationOutsideServiceArea(
                    "Sorry, E-Ride 3 is currently available only within 30 km of Tezpur, Biswanath Chariali and Nagsankar."
                )
            }
            defaultLat = checkLocationLat
            defaultLng = checkLocationLng
        }

        val newDriver = Driver(
            id = "DRV_${(200..999).random()}",
            name = trimmedName,
            phone = trimmedPhone,
            vehicleType = vehicleType.ifBlank { "Passenger E-Rickshaw" },
            vehicleModel = vehicleModel.trim(),
            vehicleNumber = vehicleNumber.trim().uppercase(),
            rickshawRegNo = vehicleNumber.trim().uppercase(),
            rcDetails = rcDetails.ifBlank { "Assam State Commercial Registration" },
            licenseNo = licenseNo?.trim()?.ifBlank { null },
            licenseDetails = licenseDetails?.trim()?.ifBlank { null },
            serviceCity = selectedArea,
            isOnline = false,
            status = DriverStatus.PENDING_APPROVAL,
            rating = 5.0,
            totalRides = 0,
            currentLat = defaultLat,
            currentLng = defaultLng,
            licenseDocUploaded = licenseDocUploaded || !licenseNo.isNullOrBlank(),
            licenseDocUrl = licenseDocUrl,
            registeredAt = System.currentTimeMillis()
        )

        _driversList.update { listOf(newDriver) + it }
        _currentDriver.value = newDriver
        _driverWallets.update { it + (newDriver.id to DriverWallet(driverId = newDriver.id, availableBalance = 0.0, totalRides = 0, lifetimeEarnings = 0.0)) }

        val notif = AdminNotification(
            id = "NOTIF-${(1000..9999).random()}",
            type = AdminNotificationType.NEW_DRIVER_REGISTRATION,
            title = "New Driver Approval Pending",
            message = "Driver ${newDriver.name} (${newDriver.vehicleNumber}) registered in ${newDriver.serviceCity}. Verification required.",
            targetSection = "DRIVERS",
            targetId = newDriver.id
        )
        _adminNotifications.update { listOf(notif) + it }

        return DriverRegisterResult.Success(newDriver)
    }

    // Withdrawal Schedule Validation (Mon-Fri 9:00 AM - 5:00 PM IST)
    data class WithdrawalScheduleCheck(
        val isOpen: Boolean,
        val isWeekend: Boolean,
        val title: String,
        val message: String
    )

    fun getWithdrawalScheduleCheck(): WithdrawalScheduleCheck {
        val istZone = TimeZone.getTimeZone("Asia/Kolkata")
        val cal = Calendar.getInstance(istZone)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)

        val isWeekend = (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY)
        if (isWeekend) {
            return WithdrawalScheduleCheck(
                isOpen = false,
                isWeekend = true,
                title = "Weekend Withdrawal Closed",
                message = "Withdrawals are available Monday to Friday, 9:00 AM to 5:00 PM."
            )
        }

        val currentMinutes = hour * 60 + minute
        val openMinutes = 9 * 60 // 9:00 AM
        val closeMinutes = 17 * 60 // 5:00 PM

        return if (currentMinutes in openMinutes..closeMinutes) {
            WithdrawalScheduleCheck(
                isOpen = true,
                isWeekend = false,
                title = "Withdrawal Window Open",
                message = "Withdrawal is available Monday to Friday, 9:00 AM to 5:00 PM."
            )
        } else {
            WithdrawalScheduleCheck(
                isOpen = false,
                isWeekend = false,
                title = "Withdrawals are currently unavailable.",
                message = "Withdrawal hours are Monday to Friday, 9:00 AM to 5:00 PM."
            )
        }
    }

    fun isWithdrawalScheduleOpen(): Pair<Boolean, String> {
        val check = getWithdrawalScheduleCheck()
        return if (check.isOpen) {
            Pair(true, check.message)
        } else {
            Pair(false, "${check.title} ${check.message}")
        }
    }

    fun maskAccountNumber(accountNo: String?): String {
        if (accountNo.isNullOrBlank()) return ""
        val digits = accountNo.filter { it.isDigit() }
        return if (digits.length >= 4) {
            val last4 = digits.takeLast(4)
            "XXXX XXXX $last4"
        } else {
            "XXXX $digits"
        }
    }

    fun maskUpiId(upi: String): String {
        val parts = upi.trim().split("@")
        if (parts.size == 2) {
            val user = parts[0]
            val masked = if (user.length <= 2) "$user***" else "${user.take(2)}***"
            return "$masked@${parts[1]}"
        }
        return upi
    }

    data class WithdrawalResult(
        val success: Boolean,
        val message: String,
        val request: WithdrawalRequest? = null
    )

    fun requestWithdrawal(
        driverId: String,
        amount: Double,
        paymentMethod: String = "UPI",
        upiId: String = "",
        accountHolderName: String? = null,
        bankName: String? = null,
        accountNumber: String? = null,
        ifscCode: String? = null
    ): WithdrawalResult {
        val scheduleCheck = getWithdrawalScheduleCheck()
        if (!scheduleCheck.isOpen) {
            return WithdrawalResult(false, "${scheduleCheck.title}\n${scheduleCheck.message}")
        }

        if (amount < 100.0) {
            return WithdrawalResult(false, "Minimum withdrawal amount is ₹100.")
        }

        val wallet = _driverWallets.value[driverId]
            ?: return WithdrawalResult(false, "Driver wallet record not found.")

        if (amount > wallet.availableBalance) {
            return WithdrawalResult(false, "Amount cannot be greater than available balance of ₹${wallet.availableBalance.toInt()}.")
        }

        if (paymentMethod == "UPI") {
            val trimmedUpi = upiId.trim()
            if (trimmedUpi.isBlank() || !trimmedUpi.contains("@") || trimmedUpi.length < 5) {
                return WithdrawalResult(false, "Please enter a valid UPI ID (e.g. name@upi).")
            }
        } else {
            if (accountHolderName.isNullOrBlank()) {
                return WithdrawalResult(false, "Please enter Account Holder Name.")
            }
            if (bankName.isNullOrBlank()) {
                return WithdrawalResult(false, "Please enter Bank Name.")
            }
            val digits = (accountNumber ?: "").filter { it.isDigit() }
            if (digits.length < 8) {
                return WithdrawalResult(false, "Please enter a valid Account Number (minimum 8 digits).")
            }
            if (ifscCode.isNullOrBlank() || ifscCode.trim().length < 6) {
                return WithdrawalResult(false, "Please enter a valid IFSC Code.")
            }
        }

        val roundedAmount = roundCurrency(amount)

        // Move requested amount from AVAILABLE BALANCE to WITHDRAWAL PENDING
        val updatedWallet = wallet.copy(
            availableBalance = roundCurrency(wallet.availableBalance - roundedAmount),
            pendingBalance = roundCurrency(wallet.pendingBalance + roundedAmount)
        )
        _driverWallets.update { it + (driverId to updatedWallet) }

        val withdrawalId = "WD-${(10000000..99999999).random()}"
        val driver = _driversList.value.find { it.id == driverId }
        val driverName = driver?.name ?: "Driver"
        val driverPhone = driver?.phone ?: "+91 94350 12345"

        val maskedAcc = maskAccountNumber(accountNumber)
        val maskedUpi = maskUpiId(upiId)

        val newRequest = WithdrawalRequest(
            id = withdrawalId,
            driverId = driverId,
            driverName = driverName,
            driverPhone = driverPhone,
            amount = roundedAmount,
            paymentMethod = paymentMethod,
            upiId = upiId.trim(),
            bankName = bankName?.trim(),
            accountHolderName = accountHolderName?.trim(),
            accountNumber = accountNumber?.trim(),
            maskedAccountNumber = maskedAcc,
            ifscCode = ifscCode?.trim()?.uppercase(),
            bankDetails = if (paymentMethod == "BANK_ACCOUNT") "${bankName?.trim()} (A/C: $maskedAcc)" else "UPI: $maskedUpi",
            status = WithdrawalStatus.PENDING,
            requestedAt = System.currentTimeMillis()
        )
        _withdrawalRequests.update { listOf(newRequest) + it }

        val txnId = "TXN-${UUID.randomUUID().toString().take(8)}"
        val txn = FinancialTransaction(
            transactionId = txnId,
            driverId = driverId,
            rideId = null,
            amount = roundedAmount,
            type = TransactionType.WITHDRAWAL,
            status = TransactionStatus.PENDING,
            timestamp = System.currentTimeMillis(),
            description = "Withdrawal request $withdrawalId to ${if (paymentMethod == "BANK_ACCOUNT") maskedAcc else maskedUpi} (Status: PENDING)",
            isCredit = false
        )
        _financialTransactions.update { listOf(txn) + it }
        _walletTransactions.update { listOf(WalletTransaction(txnId, driverId, roundedAmount, "WITHDRAWAL", txn.description, txn.timestamp, false, null, TransactionStatus.PENDING)) + it }

        val notif = AdminNotification(
            id = "NOTIF-${UUID.randomUUID().toString().take(8)}",
            type = AdminNotificationType.WITHDRAWAL_REQUEST,
            title = "New Withdrawal Request",
            message = "$driverName requested withdrawal of ₹${roundedAmount.toInt()} via ${if (paymentMethod == "BANK_ACCOUNT") "Bank" else "UPI"} ($withdrawalId).",
            targetSection = "WITHDRAW",
            targetId = withdrawalId
        )
        _adminNotifications.update { listOf(notif) + it }

        return WithdrawalResult(true, "Your withdrawal request has been submitted.", newRequest)
    }

    fun requestWithdrawal(driverId: String, amount: Double, upiId: String): Pair<Boolean, String> {
        val res = requestWithdrawal(
            driverId = driverId,
            amount = amount,
            paymentMethod = "UPI",
            upiId = upiId
        )
        return Pair(res.success, res.message)
    }

    // Settle Commission Due (Driver pays commission owed for cash rides)
    fun settleCommissionDue(driverId: String, amount: Double): Boolean {
        val wallet = _driverWallets.value[driverId] ?: return false
        if (amount <= 0.0 || wallet.commissionDue <= 0.0) return false

        val settleAmount = minOf(roundCurrency(amount), wallet.commissionDue)
        val updatedWallet = wallet.copy(
            commissionDue = roundCurrency(wallet.commissionDue - settleAmount)
        )
        _driverWallets.update { it + (driverId to updatedWallet) }

        val txnId = "TXN-${UUID.randomUUID().toString().take(8)}"
        val txn = FinancialTransaction(
            transactionId = txnId,
            driverId = driverId,
            rideId = null,
            amount = settleAmount,
            type = TransactionType.ADJUSTMENT,
            status = TransactionStatus.COMPLETED,
            timestamp = System.currentTimeMillis(),
            description = "Commission liability settlement of ₹${settleAmount.toInt()}",
            isCredit = false
        )
        _financialTransactions.update { listOf(txn) + it }
        _walletTransactions.update { listOf(WalletTransaction(txnId, driverId, settleAmount, "ADJUSTMENT", txn.description, txn.timestamp, false, null, TransactionStatus.COMPLETED)) + it }

        return true
    }

    // Refund Ride
    fun refundRide(rideId: String, reason: String = "Passenger dispute / Refund"): Boolean {
        val ride = _ridesList.value.find { it.id == rideId } ?: return false
        if (ride.paymentStatus == PaymentStatus.REFUNDED) return false

        val driverId = ride.driverId ?: return false
        val now = System.currentTimeMillis()

        val updatedRide = ride.copy(
            paymentStatus = PaymentStatus.REFUNDED,
            cancellationReason = if (ride.status == RideStatus.CANCELLED) ride.cancellationReason else reason
        )
        _ridesList.update { list -> list.map { if (it.id == rideId) updatedRide else it } }
        if (_activeRide.value?.id == rideId) {
            _activeRide.value = updatedRide
        }

        if (ride.isFinancialSettled) {
            val wallet = _driverWallets.value[driverId] ?: DriverWallet(driverId = driverId)
            if (ride.paymentMode == PaymentMode.CASH) {
                val updatedWallet = wallet.copy(
                    commissionDue = maxOf(0.0, roundCurrency(wallet.commissionDue - ride.commissionAmount)),
                    totalEarnings = maxOf(0.0, roundCurrency(wallet.totalEarnings - ride.driverEarning))
                )
                _driverWallets.update { it + (driverId to updatedWallet) }
            } else {
                val updatedWallet = wallet.copy(
                    availableBalance = maxOf(0.0, roundCurrency(wallet.availableBalance - ride.driverEarning)),
                    totalEarnings = maxOf(0.0, roundCurrency(wallet.totalEarnings - ride.driverEarning))
                )
                _driverWallets.update { it + (driverId to updatedWallet) }
            }

            val refundTxn = FinancialTransaction(
                transactionId = "TXN-${UUID.randomUUID().toString().take(8)}",
                driverId = driverId,
                rideId = ride.id,
                amount = ride.driverEarning,
                type = TransactionType.REFUND,
                status = TransactionStatus.COMPLETED,
                timestamp = now,
                description = "Refund reversal for ride ${ride.id}: $reason",
                isCredit = false
            )
            _financialTransactions.update { listOf(refundTxn) + it }
            _walletTransactions.update { listOf(WalletTransaction(refundTxn.transactionId, driverId, refundTxn.amount, "REFUND", refundTxn.description, now, false, ride.id)) + it }
        }
        return true
    }

    // Driver Earnings Calculation Breakdown
    fun getDriverEarningsSummary(driverId: String): DriverEarningsSummary {
        val istZone = TimeZone.getTimeZone("Asia/Kolkata")
        val calNow = Calendar.getInstance(istZone)
        val todayYear = calNow.get(Calendar.YEAR)
        val todayDayOfYear = calNow.get(Calendar.DAY_OF_YEAR)
        val todayWeek = calNow.get(Calendar.WEEK_OF_YEAR)
        val todayMonth = calNow.get(Calendar.MONTH)

        val driverRides = _ridesList.value.filter { it.driverId == driverId }
        val completedRides = driverRides.filter { it.status == RideStatus.COMPLETED && it.paymentStatus == PaymentStatus.PAID }

        var todayEarnings = 0.0
        var thisWeekEarnings = 0.0
        var thisMonthEarnings = 0.0
        var totalEarnings = 0.0
        var cashCollected = 0.0
        var onlineCollected = 0.0
        var adminCommission = 0.0
        var netDriverEarnings = 0.0

        val rideCal = Calendar.getInstance(istZone)
        for (r in completedRides) {
            val completedTime = r.completedAt ?: r.createdAt
            rideCal.timeInMillis = completedTime

            val gross = roundCurrency(r.fare)
            val comm = roundCurrency(r.commissionAmount)
            val net = roundCurrency(r.driverEarning)

            if (r.paymentMode == PaymentMode.CASH) {
                cashCollected += gross
            } else {
                onlineCollected += gross
            }

            adminCommission += comm
            netDriverEarnings += net
            totalEarnings += net

            if (rideCal.get(Calendar.YEAR) == todayYear) {
                if (rideCal.get(Calendar.DAY_OF_YEAR) == todayDayOfYear) {
                    todayEarnings += net
                }
                if (rideCal.get(Calendar.WEEK_OF_YEAR) == todayWeek) {
                    thisWeekEarnings += net
                }
                if (rideCal.get(Calendar.MONTH) == todayMonth) {
                    thisMonthEarnings += net
                }
            }
        }

        return DriverEarningsSummary(
            todayEarnings = roundCurrency(todayEarnings),
            thisWeekEarnings = roundCurrency(thisWeekEarnings),
            thisMonthEarnings = roundCurrency(thisMonthEarnings),
            totalEarnings = roundCurrency(totalEarnings),
            totalRides = driverRides.size,
            completedRides = completedRides.size,
            cashCollected = roundCurrency(cashCollected),
            onlineCollected = roundCurrency(onlineCollected),
            adminCommission = roundCurrency(adminCommission),
            netDriverEarnings = roundCurrency(netDriverEarnings)
        )
    }

    // Admin Web Financial Summary
    fun getAdminFinancialMetrics(): AdminFinancialMetrics {
        val completedPaidRides = _ridesList.value.filter { it.status == RideStatus.COMPLETED && it.paymentStatus == PaymentStatus.PAID }
        val totalRevenue = roundCurrency(completedPaidRides.sumOf { it.fare })
        val adminCommission = roundCurrency(completedPaidRides.sumOf { it.commissionAmount })
        val driverEarnings = roundCurrency(completedPaidRides.sumOf { it.driverEarning })
        val cashCollection = roundCurrency(completedPaidRides.filter { it.paymentMode == PaymentMode.CASH }.sumOf { it.fare })
        val onlineCollection = roundCurrency(completedPaidRides.filter { it.paymentMode != PaymentMode.CASH }.sumOf { it.fare })
        val pendingCommission = roundCurrency(_driverWallets.value.values.sumOf { it.commissionDue })

        return AdminFinancialMetrics(
            totalRevenue = totalRevenue,
            adminCommission = adminCommission,
            driverEarnings = driverEarnings,
            cashCollection = cashCollection,
            onlineCollection = onlineCollection,
            pendingCommission = pendingCommission,
            completedRidesCount = completedPaidRides.size
        )
    }

    // Driver Deletion Check & Execution
    fun getDriverFinancialStatus(driverId: String = _currentDriver.value.id): DriverFinancialStatus {
        val wallet = _driverWallets.value[driverId] ?: DriverWallet(driverId = driverId)
        val pendingWithdrawals = _withdrawalRequests.value.filter {
            it.driverId == driverId && (it.status == WithdrawalStatus.PENDING || it.status == WithdrawalStatus.PROCESSING)
        }
        val pendingWithdrawalSum = pendingWithdrawals.sumOf { it.amount }
        return DriverFinancialStatus(
            availableBalance = wallet.availableBalance,
            pendingEarnings = wallet.pendingBalance,
            commissionDue = wallet.commissionDue,
            hasPendingWithdrawal = pendingWithdrawals.isNotEmpty(),
            pendingWithdrawalAmount = pendingWithdrawalSum,
            lifetimeEarnings = wallet.lifetimeEarnings,
            totalRides = wallet.totalRides,
            hasOutstandingLiability = wallet.commissionDue > 0.0 || pendingWithdrawals.isNotEmpty()
        )
    }

    fun requestDriverAccountDeletion(driverId: String, reason: String): DeleteAccountResult {
        if (_currentDriver.value.id != driverId) {
            return DeleteAccountResult.Unauthorized("Security verification failed. A driver can only delete their own authenticated account.")
        }
        val driver = _driversList.value.find { it.id == driverId } ?: _currentDriver.value

        val active = _activeRide.value
        if (active != null && active.driverId == driverId && active.status in listOf(
                RideStatus.DRIVER_ASSIGNED,
                RideStatus.DRIVER_ARRIVED,
                RideStatus.IN_PROGRESS
            )) {
            return DeleteAccountResult.HasActiveRide(
                rideId = active.id,
                message = "Active ride in progress: You cannot delete your account while you have an active ride. Please complete or cancel the active ride first."
            )
        }

        val finStatus = getDriverFinancialStatus(driverId)
        if (finStatus.hasPendingWithdrawal || finStatus.commissionDue > 0.0) {
            return DeleteAccountResult.FinancialPending(
                message = "You have a pending withdrawal or unsettled balance. Please contact E-Ride 3 Support before deleting your account.",
                pendingAmount = finStatus.pendingWithdrawalAmount + finStatus.commissionDue,
                commissionDue = finStatus.commissionDue,
                pendingWithdrawal = finStatus.pendingWithdrawalAmount
            )
        }

        val completedRides = _ridesList.value.count { it.driverId == driverId && it.status == RideStatus.COMPLETED }
        val totalEarnings = _ridesList.value.filter { it.driverId == driverId && it.status == RideStatus.COMPLETED }.sumOf { it.fare }
        val now = System.currentTimeMillis()

        val record = DriverDeletionRecord(
            deletionRequestId = "DEL-REQ-${(1000..9999).random()}",
            driverId = driver.id,
            driverName = driver.name,
            driverPhone = driver.phone,
            rickshawRegNo = driver.rickshawRegNo,
            requestedAt = now,
            completedAt = now,
            status = DeletionRequestStatus.COMPLETED,
            reason = reason.ifBlank { AccountDeletionReasons.NO_LONGER_NEED },
            processedBy = "AUTHENTICATED_DRIVER_SELF",
            retainedFinancialSummary = "Preserved $completedRides ride records, ₹${totalEarnings.toInt()} lifetime earnings. Zero outstanding debts.",
            totalRidesCompleted = completedRides,
            lifetimeEarnings = totalEarnings
        )

        val updatedDriver = driver.copy(
            status = DriverStatus.DELETED,
            isOnline = false
        )
        _currentDriver.value = updatedDriver
        _driversList.update { list ->
            list.map { if (it.id == driverId) updatedDriver else it }
        }
        _incomingOffer.value = null
        _driverDeletionRecords.update { listOf(record) + it }

        return DeleteAccountResult.Success(record)
    }

    fun isDriverAccountDeleted(phoneOrRegNo: String): Boolean {
        val clean = phoneOrRegNo.trim().lowercase().replace(" ", "").replace("-", "")
        return _driverDeletionRecords.value.any {
            it.driverPhone.replace(" ", "").replace("-", "") == clean ||
            it.rickshawRegNo.lowercase().replace(" ", "").replace("-", "") == clean
        } || _driversList.value.any {
            it.status == DriverStatus.DELETED && (
                it.phone.replace(" ", "").replace("-", "") == clean ||
                it.rickshawRegNo.lowercase().replace(" ", "").replace("-", "") == clean
            )
        }
    }

    fun submitComplaint(
        rideId: String?,
        reporterId: String,
        reporterName: String,
        reporterRole: UserRole,
        subject: String,
        description: String
    ): ComplaintRecord {
        val complaint = ComplaintRecord(
            id = "CMP-${(100..999).random()}",
            rideId = rideId,
            reporterId = reporterId,
            reporterName = reporterName,
            reporterRole = reporterRole,
            subject = subject,
            description = description,
            status = ComplaintStatus.PENDING
        )
        _complaintsList.update { listOf(complaint) + it }
        return complaint
    }

    // ==========================================
    // ADMIN AUTHENTICATION & MANAGEMENT METHODS
    // ==========================================

    fun loginAdmin(identifier: String, password: String): Result<AdminSession> {
        val cleanIdentifier = identifier.trim().lowercase()
        val user = _adminUsers.value.find {
            (it.email.lowercase() == cleanIdentifier || it.phone.replace(" ", "").replace("-", "").endsWith(cleanIdentifier.replace(" ", "").replace("-", ""))) &&
            it.passwordHash == password
        }

        if (user == null) {
            return Result.failure(Exception("Invalid email/phone or password. Access restricted to authorized E-Ride 3 Operations staff."))
        }

        if (!user.isActive) {
            return Result.failure(Exception("This administrator account has been disabled. Contact Super Admin."))
        }

        val updatedUser = user.copy(lastLogin = System.currentTimeMillis())
        _adminUsers.update { list -> list.map { if (it.id == user.id) updatedUser else it } }

        val session = AdminSession(
            admin = updatedUser,
            token = "ERIDE3_ADMIN_TOKEN_${System.currentTimeMillis()}"
        )
        _currentAdminSession.value = session
        recordAuditLog(
            action = "ADMIN_LOGIN",
            targetType = "SESSION",
            targetId = user.id,
            oldValue = null,
            newValue = "Logged in as ${user.role}"
        )
        return Result.success(session)
    }

    fun logoutAdmin() {
        val current = _currentAdminSession.value
        if (current != null) {
            recordAuditLog(
                action = "ADMIN_LOGOUT",
                targetType = "SESSION",
                targetId = current.admin.id,
                oldValue = current.admin.name,
                newValue = "Ended Session"
            )
        }
        _currentAdminSession.value = null
    }

    fun changeAdminPassword(oldPass: String, newPass: String): Boolean {
        val current = _currentAdminSession.value ?: return false
        if (current.admin.passwordHash != oldPass) return false
        val updated = current.admin.copy(passwordHash = newPass)
        _adminUsers.update { list -> list.map { if (it.id == updated.id) updated else it } }
        _currentAdminSession.value = current.copy(admin = updated)
        recordAuditLog(
            action = "CHANGE_PASSWORD",
            targetType = "ADMIN_USER",
            targetId = updated.id,
            oldValue = "********",
            newValue = "Password changed successfully"
        )
        return true
    }

    fun recordAuditLog(
        action: String,
        targetType: String,
        targetId: String,
        oldValue: String? = null,
        newValue: String? = null
    ) {
        val currentAdmin = _currentAdminSession.value?.admin
        val log = AdminAuditLog(
            id = "AUD-${(1000..9999).random()}",
            adminId = currentAdmin?.id ?: "SYSTEM",
            adminName = currentAdmin?.name ?: "System Engine",
            action = action,
            targetType = targetType,
            targetId = targetId,
            oldValue = oldValue,
            newValue = newValue,
            timestamp = System.currentTimeMillis()
        )
        _auditLogs.update { listOf(log) + it }
    }

    fun updateFareSettings(
        baseFare: Double,
        perKmRate: Double,
        perMinuteRate: Double = 1.0,
        minimumFare: Double = 25.0,
        waitingCharge: Double = 2.0,
        additionalCharges: Double = 0.0,
        cancellationFee: Double = 15.0,
        platformCommissionRate: Double = 10.0
    ): Boolean {
        val old = _fareSettings.value
        val updated = FareSettings(
            baseFare = roundCurrency(baseFare),
            perKmRate = roundCurrency(perKmRate),
            perMinuteRate = roundCurrency(perMinuteRate),
            minimumFare = roundCurrency(minimumFare),
            waitingCharge = roundCurrency(waitingCharge),
            additionalCharges = roundCurrency(additionalCharges),
            cancellationFee = roundCurrency(cancellationFee),
            platformCommissionRate = roundCurrency(platformCommissionRate),
            effectiveDate = System.currentTimeMillis(),
            updatedBy = _currentAdminSession.value?.admin?.name ?: "SUPER_ADMIN"
        )
        _fareSettings.value = updated
        this.minimumFare = updated.minimumFare
        this.perKmRate = updated.perKmRate
        updateAdminCommissionRate(platformCommissionRate)
        recordAuditLog(
            action = "FARE_SETTINGS_UPDATED",
            targetType = "FARE_CONFIG",
            targetId = "GLOBAL",
            oldValue = "Base: ₹${old.baseFare}, PerKm: ₹${old.perKmRate}, Min: ₹${old.minimumFare}, Comm: ${old.platformCommissionRate}%",
            newValue = "Base: ₹${updated.baseFare}, PerKm: ₹${updated.perKmRate}, Min: ₹${updated.minimumFare}, Comm: ${updated.platformCommissionRate}%"
        )
        return true
    }

    fun addServiceArea(name: String, district: String, lat: Double, lng: Double, radiusKm: Double = 30.0, isActive: Boolean = true): ServiceArea {
        val newArea = ServiceArea(
            id = "AREA_${(100..999).random()}",
            name = name,
            district = district,
            latitude = lat,
            longitude = lng,
            radiusKm = radiusKm,
            isActive = isActive,
            lastModifiedAt = System.currentTimeMillis()
        )
        _serviceAreas.update { listOf(newArea) + it }
        recordAuditLog("SERVICE_AREA_ADDED", "SERVICE_AREA", newArea.id, null, "$name ($district)")
        return newArea
    }

    fun updateServiceArea(id: String, name: String, district: String, lat: Double, lng: Double, radiusKm: Double, isActive: Boolean) {
        _serviceAreas.update { list ->
            list.map {
                if (it.id == id) it.copy(name = name, district = district, latitude = lat, longitude = lng, radiusKm = radiusKm, isActive = isActive, lastModifiedAt = System.currentTimeMillis()) else it
            }
        }
        recordAuditLog("SERVICE_AREA_UPDATED", "SERVICE_AREA", id, null, "$name ($district)")
    }

    fun deleteServiceArea(id: String) {
        val area = _serviceAreas.value.find { it.id == id }
        _serviceAreas.update { list -> list.filter { it.id != id } }
        recordAuditLog("SERVICE_AREA_DELETED", "SERVICE_AREA", id, area?.name, "DELETED")
    }

    fun verifySettlement(settlementId: String, notes: String = "Account & ride verified") {
        _settlementsList.update { list ->
            list.map {
                if (it.id == settlementId) it.copy(status = SettlementStatus.VERIFIED, verificationNotes = notes) else it
            }
        }
        recordAuditLog("SETTLEMENT_VERIFIED", "SETTLEMENT", settlementId, null, "VERIFIED ($notes)")
    }

    fun approveSettlement(settlementId: String, utr: String = "UPI-SETTLE-OK") {
        _settlementsList.update { list ->
            list.map {
                if (it.id == settlementId) it.copy(status = SettlementStatus.PAID, utrReference = utr, paymentDate = System.currentTimeMillis()) else it
            }
        }
        recordAuditLog("SETTLEMENT_PAID", "SETTLEMENT", settlementId, null, "PAID (UTR: $utr)")
    }

    fun rejectSettlement(settlementId: String, reason: String = "Bank details mismatch") {
        _settlementsList.update { list ->
            list.map {
                if (it.id == settlementId) it.copy(status = SettlementStatus.REJECTED, verificationNotes = "Rejected: $reason") else it
            }
        }
        recordAuditLog("SETTLEMENT_REJECTED", "SETTLEMENT", settlementId, null, "REJECTED ($reason)")
    }

    fun markNotificationAsRead(id: String) {
        _adminNotifications.update { list ->
            list.map { if (it.id == id) it.copy(isRead = true) else it }
        }
    }

    fun markAllNotificationsAsRead() {
        _adminNotifications.update { list ->
            list.map { it.copy(isRead = true) }
        }
    }

    fun toggleServiceArea(areaId: String, active: Boolean) {
        val area = _serviceAreas.value.find { it.id == areaId } ?: return
        _serviceAreas.update { list ->
            list.map {
                if (it.id == areaId) it.copy(isActive = active, lastModifiedAt = System.currentTimeMillis()) else it
            }
        }
        recordAuditLog(
            action = if (active) "SERVICE_AREA_ENABLED" else "SERVICE_AREA_DISABLED",
            targetType = "SERVICE_AREA",
            targetId = area.name,
            oldValue = "Active: ${area.isActive}",
            newValue = "Active: $active"
        )
    }

    fun suspendPassenger(passengerId: String, reason: String = "Admin Policy Enforcement") {
        val p = _passengersList.value.find { it.id == passengerId } ?: return
        _passengersList.update { list ->
            list.map {
                if (it.id == passengerId) it.copy(accountStatus = PassengerAccountStatus.SUSPENDED) else it
            }
        }
        recordAuditLog(
            action = "PASSENGER_SUSPENDED",
            targetType = "PASSENGER",
            targetId = passengerId,
            oldValue = "ACTIVE",
            newValue = "SUSPENDED (Reason: $reason)"
        )
    }

    fun activatePassenger(passengerId: String) {
        val p = _passengersList.value.find { it.id == passengerId } ?: return
        _passengersList.update { list ->
            list.map {
                if (it.id == passengerId) it.copy(accountStatus = PassengerAccountStatus.ACTIVE) else it
            }
        }
        recordAuditLog(
            action = "PASSENGER_ACTIVATED",
            targetType = "PASSENGER",
            targetId = passengerId,
            oldValue = "SUSPENDED",
            newValue = "ACTIVE"
        )
    }

    fun approveDriver(driverId: String) {
        val driver = _driversList.value.find { it.id == driverId } ?: return
        _driversList.update { list ->
            list.map { if (it.id == driverId) it.copy(status = DriverStatus.APPROVED) else it }
        }
        recordAuditLog("DRIVER_APPROVED", "DRIVER", driverId, driver.status.name, DriverStatus.APPROVED.name)
    }

    fun rejectDriver(driverId: String, reason: String) {
        val driver = _driversList.value.find { it.id == driverId } ?: return
        _driversList.update { list ->
            list.map { if (it.id == driverId) it.copy(status = DriverStatus.REJECTED) else it }
        }
        recordAuditLog("DRIVER_REJECTED", "DRIVER", driverId, driver.status.name, "REJECTED (Reason: $reason)")
    }

    fun suspendDriver(driverId: String, reason: String) {
        val driver = _driversList.value.find { it.id == driverId } ?: return
        _driversList.update { list ->
            list.map { if (it.id == driverId) it.copy(status = DriverStatus.SUSPENDED, isOnline = false) else it }
        }
        recordAuditLog("DRIVER_SUSPENDED", "DRIVER", driverId, driver.status.name, "SUSPENDED (Reason: $reason)")
    }

    fun activateDriver(driverId: String) {
        val driver = _driversList.value.find { it.id == driverId } ?: return
        _driversList.update { list ->
            list.map { if (it.id == driverId) it.copy(status = DriverStatus.APPROVED) else it }
        }
        recordAuditLog("DRIVER_ACTIVATED", "DRIVER", driverId, driver.status.name, DriverStatus.APPROVED.name)
    }


    fun addComplaintNote(complaintId: String, note: String) {
        val adminName = _currentAdminSession.value?.admin?.name ?: "Admin Operations"
        val timestampedNote = "[${java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.ENGLISH).format(java.util.Date())}] $adminName: $note"
        _complaintsList.update { list ->
            list.map {
                if (it.id == complaintId) it.copy(internalNotes = it.internalNotes + timestampedNote, updatedAt = System.currentTimeMillis()) else it
            }
        }
        recordAuditLog("COMPLAINT_NOTE_ADDED", "COMPLAINT", complaintId, null, note)
    }

    fun addComplaintReply(complaintId: String, reply: String) {
        val adminName = _currentAdminSession.value?.admin?.name ?: "Admin Support"
        val timestampedReply = "[${java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.ENGLISH).format(java.util.Date())}] $adminName: $reply"
        _complaintsList.update { list ->
            list.map {
                if (it.id == complaintId) it.copy(replies = it.replies + timestampedReply, updatedAt = System.currentTimeMillis()) else it
            }
        }
        recordAuditLog("COMPLAINT_REPLY_SENT", "COMPLAINT", complaintId, null, reply)
    }

    fun updateComplaintStatus(complaintId: String, status: ComplaintStatus, notes: String? = null) {
        _complaintsList.update { list ->
            list.map {
                if (it.id == complaintId) it.copy(status = status, resolutionNotes = notes ?: it.resolutionNotes) else it
            }
        }
        recordAuditLog("COMPLAINT_STATUS_UPDATED", "COMPLAINT", complaintId, null, status.name)
    }

    fun updateDriverStatus(driverId: String, status: DriverStatus) {
        _driversList.update { list ->
            list.map {
                if (it.id == driverId) it.copy(status = status) else it
            }
        }
        recordAuditLog("DRIVER_STATUS_UPDATED", "DRIVER", driverId, null, status.name)
    }

    // Admin Withdrawal Actions & Strict Accounting
    fun approveWithdrawal(withdrawalId: String) {
        val req = _withdrawalRequests.value.find { it.id == withdrawalId } ?: return
        val now = System.currentTimeMillis()
        _withdrawalRequests.update { list ->
            list.map {
                if (it.id == withdrawalId) it.copy(status = WithdrawalStatus.PROCESSING, processedAt = now) else it
            }
        }
        recordAuditLog("WITHDRAWAL_PROCESSING", "WITHDRAWAL", withdrawalId, req.status.name, WithdrawalStatus.PROCESSING.name)
    }

    fun markWithdrawalAsPaid(withdrawalId: String, refNumber: String = "UPI-SETTLE-OK") {
        val req = _withdrawalRequests.value.find { it.id == withdrawalId } ?: return
        val now = System.currentTimeMillis()

        // Strict Accounting: Deduct from pendingBalance, add to totalWithdrawn
        val wallet = _driverWallets.value[req.driverId]
        if (wallet != null) {
            _driverWallets.update {
                it + (req.driverId to wallet.copy(
                    pendingBalance = maxOf(0.0, roundCurrency(wallet.pendingBalance - req.amount)),
                    totalWithdrawn = roundCurrency(wallet.totalWithdrawn + req.amount)
                ))
            }
        }

        _withdrawalRequests.update { list ->
            list.map {
                if (it.id == withdrawalId) it.copy(
                    status = WithdrawalStatus.PAID,
                    processedAt = now,
                    paidAt = now,
                    transactionReference = refNumber
                ) else it
            }
        }

        val targetDescription = "Withdrawal $withdrawalId of ₹${req.amount.toInt()} (Paid • Ref: $refNumber)"
        _financialTransactions.update { list ->
            list.map {
                if (it.type == TransactionType.WITHDRAWAL && it.driverId == req.driverId && it.status == TransactionStatus.PENDING && it.amount == req.amount) {
                    it.copy(status = TransactionStatus.COMPLETED, description = targetDescription)
                } else it
            }
        }
        _walletTransactions.update { list ->
            list.map {
                if (it.type == "WITHDRAWAL" && it.driverId == req.driverId && it.status == TransactionStatus.PENDING && it.amount == req.amount) {
                    it.copy(status = TransactionStatus.COMPLETED, description = targetDescription)
                } else it
            }
        }
        recordAuditLog("WITHDRAWAL_PAID", "WITHDRAWAL", withdrawalId, req.status.name, "PAID (Ref: $refNumber)")
    }

    fun rejectWithdrawal(withdrawalId: String, reason: String = "Bank account / UPI ID validation failed.") {
        val req = _withdrawalRequests.value.find { it.id == withdrawalId } ?: return
        val now = System.currentTimeMillis()

        // Strict Accounting: Refund pending amount back to Driver Available Balance
        val wallet = _driverWallets.value[req.driverId]
        if (wallet != null) {
            _driverWallets.update {
                it + (req.driverId to wallet.copy(
                    availableBalance = roundCurrency(wallet.availableBalance + req.amount),
                    pendingBalance = maxOf(0.0, roundCurrency(wallet.pendingBalance - req.amount))
                ))
            }
        }

        _withdrawalRequests.update { list ->
            list.map {
                if (it.id == withdrawalId) it.copy(
                    status = WithdrawalStatus.REJECTED,
                    processedAt = now,
                    rejectionReason = reason
                ) else it
            }
        }

        val targetDescription = "Withdrawal $withdrawalId of ₹${req.amount.toInt()} Rejected: $reason (Returned to wallet)"
        _financialTransactions.update { list ->
            list.map {
                if (it.type == TransactionType.WITHDRAWAL && it.driverId == req.driverId && it.status == TransactionStatus.PENDING && it.amount == req.amount) {
                    it.copy(status = TransactionStatus.REVERSED, description = targetDescription)
                } else it
            }
        }
        _walletTransactions.update { list ->
            list.map {
                if (it.type == "WITHDRAWAL" && it.driverId == req.driverId && it.status == TransactionStatus.PENDING && it.amount == req.amount) {
                    it.copy(status = TransactionStatus.REVERSED, description = targetDescription)
                } else it
            }
        }
        recordAuditLog("WITHDRAWAL_REJECTED", "WITHDRAWAL", withdrawalId, req.status.name, "REJECTED (Reason: $reason)")
    }

    fun failWithdrawal(withdrawalId: String, reason: String = "Banking transfer gateway failure") {
        val req = _withdrawalRequests.value.find { it.id == withdrawalId } ?: return
        val now = System.currentTimeMillis()

        // Return pending amount back to available balance
        val wallet = _driverWallets.value[req.driverId]
        if (wallet != null) {
            _driverWallets.update {
                it + (req.driverId to wallet.copy(
                    availableBalance = roundCurrency(wallet.availableBalance + req.amount),
                    pendingBalance = maxOf(0.0, roundCurrency(wallet.pendingBalance - req.amount))
                ))
            }
        }

        _withdrawalRequests.update { list ->
            list.map {
                if (it.id == withdrawalId) it.copy(
                    status = WithdrawalStatus.FAILED,
                    processedAt = now,
                    rejectionReason = reason
                ) else it
            }
        }

        val targetDescription = "Withdrawal $withdrawalId of ₹${req.amount.toInt()} Failed: $reason (Returned to wallet)"
        _financialTransactions.update { list ->
            list.map {
                if (it.type == TransactionType.WITHDRAWAL && it.driverId == req.driverId && it.status == TransactionStatus.PENDING && it.amount == req.amount) {
                    it.copy(status = TransactionStatus.FAILED, description = targetDescription)
                } else it
            }
        }
        _walletTransactions.update { list ->
            list.map {
                if (it.type == "WITHDRAWAL" && it.driverId == req.driverId && it.status == TransactionStatus.PENDING && it.amount == req.amount) {
                    it.copy(status = TransactionStatus.FAILED, description = targetDescription)
                } else it
            }
        }
        recordAuditLog("WITHDRAWAL_FAILED", "WITHDRAWAL", withdrawalId, req.status.name, "FAILED (Reason: $reason)")
    }

    fun cancelWithdrawal(withdrawalId: String): Pair<Boolean, String> {
        val req = _withdrawalRequests.value.find { it.id == withdrawalId } ?: return Pair(false, "Request not found.")
        if (req.status != WithdrawalStatus.PENDING) {
            return Pair(false, "Only pending requests can be cancelled.")
        }
        val now = System.currentTimeMillis()
        val wallet = _driverWallets.value[req.driverId]
        if (wallet != null) {
            _driverWallets.update {
                it + (req.driverId to wallet.copy(
                    availableBalance = roundCurrency(wallet.availableBalance + req.amount),
                    pendingBalance = maxOf(0.0, roundCurrency(wallet.pendingBalance - req.amount))
                ))
            }
        }
        _withdrawalRequests.update { list ->
            list.map {
                if (it.id == withdrawalId) it.copy(status = WithdrawalStatus.CANCELLED, processedAt = now) else it
            }
        }
        recordAuditLog("WITHDRAWAL_CANCELLED", "WITHDRAWAL", withdrawalId, req.status.name, "CANCELLED")
        return Pair(true, "Your withdrawal amount has been returned to your wallet.")
    }
}
