package com.example.data.models

import kotlinx.serialization.Serializable

enum class UserRole {
    PASSENGER,
    DRIVER,
    ADMIN
}

enum class DriverStatus {
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    SUSPENDED,
    DELETED
}

enum class RideStatus {
    REQUESTED,
    DRIVER_ASSIGNED,
    DRIVER_ARRIVED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

enum class PaymentMode {
    CASH,
    ONLINE_UPI,
    WALLET
}

enum class PaymentStatus {
    PENDING,
    PAID,
    FAILED,
    REFUNDED,
    CANCELLED
}

enum class TransactionType {
    RIDE_EARNING,
    COMMISSION,
    WITHDRAWAL,
    REFUND,
    ADJUSTMENT
}

enum class TransactionStatus {
    COMPLETED,
    PENDING,
    REVERSED,
    FAILED
}

enum class WithdrawalStatus {
    PENDING,
    PROCESSING,
    APPROVED,
    PAID,
    COMPLETED,
    REJECTED,
    FAILED,
    CANCELLED
}

enum class ComplaintStatus {
    PENDING,
    UNDER_REVIEW,
    OPEN,
    IN_REVIEW,
    RESOLVED,
    CLOSED,
    DISMISSED,
    REJECTED
}

enum class ComplaintCategory {
    RIDE,
    DRIVER,
    PASSENGER,
    PAYMENT,
    CASH,
    FARE,
    WITHDRAWAL,
    APP_PROBLEM,
    OTHER
}

enum class AdminRole {
    SUPER_ADMIN,
    ADMIN,
    SUPPORT
}

enum class PassengerAccountStatus {
    ACTIVE,
    SUSPENDED
}

@Serializable
data class AdminUser(
    val id: String = "ADM_001",
    val name: String = "State Operations Lead",
    val email: String = "admin@eride3.in",
    val phone: String = "+91 94350 11223",
    val role: AdminRole = AdminRole.SUPER_ADMIN,
    val lastLogin: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val passwordHash: String = "admin123"
)

@Serializable
data class AdminSession(
    val admin: AdminUser,
    val token: String,
    val loginTime: Long = System.currentTimeMillis()
)

@Serializable
data class AdminAuditLog(
    val id: String,
    val adminId: String,
    val adminName: String,
    val action: String,
    val targetType: String,
    val targetId: String,
    val oldValue: String? = null,
    val newValue: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class FareSettings(
    val baseFare: Double = 20.0,
    val perKmRate: Double = 10.0,
    val perMinuteRate: Double = 1.0,
    val minimumFare: Double = 25.0,
    val waitingCharge: Double = 2.0,
    val additionalCharges: Double = 0.0,
    val cancellationFee: Double = 15.0,
    val platformCommissionRate: Double = 10.0,
    val effectiveDate: Long = System.currentTimeMillis(),
    val updatedBy: String = "SUPER_ADMIN"
)

enum class SettlementStatus {
    PENDING,
    VERIFIED,
    APPROVED,
    PAID,
    REJECTED
}

@Serializable
data class DriverSettlement(
    val id: String,
    val driverId: String,
    val driverName: String,
    val rideId: String? = null,
    val rideAmount: Double,
    val commissionRate: Double = 10.0,
    val commissionAmount: Double,
    val driverPayableAmount: Double,
    val paymentMode: PaymentMode = PaymentMode.ONLINE_UPI,
    val utrReference: String? = null,
    val paymentDate: Long = System.currentTimeMillis(),
    val status: SettlementStatus = SettlementStatus.PENDING,
    val verificationNotes: String? = null
)

enum class AdminNotificationType {
    NEW_DRIVER_REGISTRATION,
    DRIVER_APPROVAL_REQUEST,
    PENDING_SETTLEMENT,
    NEW_COMPLAINT,
    WITHDRAWAL_REQUEST,
    SYSTEM_ALERT
}

@Serializable
data class AdminNotification(
    val id: String,
    val type: AdminNotificationType,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val targetSection: String = "DASHBOARD",
    val targetId: String? = null
)

@Serializable
data class ServiceArea(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusKm: Double = 30.0,
    val district: String,
    val isActive: Boolean = true,
    val lastModifiedAt: Long = System.currentTimeMillis()
)

@Serializable
data class Passenger(
    val id: String = "PASS_001",
    val name: String = "Anurag Sharma",
    val phone: String = "+91 98640 12345",
    val email: String = "anurag.assam@example.com",
    val defaultCity: String = "Guwahati",
    val rating: Double = 4.8,
    val totalRides: Int = 14,
    val completedRides: Int = 13,
    val cancelledRides: Int = 1,
    val accountStatus: PassengerAccountStatus = PassengerAccountStatus.ACTIVE,
    val registeredAt: Long = System.currentTimeMillis() - 86400000L * 45,
    val lastActivity: Long = System.currentTimeMillis() - 3600000L * 2
)

@Serializable
enum class BookingRingtone(val id: String, val title: String, val description: String) {
    ALERT_1("E_RIDE_ALERT_1", "E-Ride Alert 1", "High-Priority Classic Chime"),
    ALERT_2("E_RIDE_ALERT_2", "E-Ride Alert 2", "Energetic Electronic Pulse"),
    ALERT_3("E_RIDE_ALERT_3", "E-Ride Alert 3", "Urgent Double Siren Tone"),
    ALERT_4("E_RIDE_ALERT_4", "E-Ride Alert 4", "Melodic Digital Bell"),
    ALERT_5("E_RIDE_ALERT_5", "E-Ride Alert 5", "High-Frequency Emergency Beep");

    companion object {
        fun fromId(id: String): BookingRingtone = entries.find { it.id == id } ?: ALERT_1
    }
}

@Serializable
data class Driver(
    val id: String = "DRV_101",
    val name: String = "Bipul Kalita",
    val phone: String = "+91 94350 67890",
    val vehicleType: String = "Passenger E-Rickshaw (3-Wheeler)",
    val vehicleModel: String = "Mayuri Deluxe Electric (Green)",
    val vehicleNumber: String = "AS-12-ER-4421",
    val rickshawRegNo: String = "AS-12-ER-4421",
    val rcDetails: String = "Commercial RC Valid (Sonitpur RTO)",
    val licenseNo: String? = "AS-DL-2021-9988",
    val licenseDetails: String? = "Commercial LMV/E-Rickshaw Endorsed",
    val serviceCity: String = "Tezpur",
    val isOnline: Boolean = true,
    val status: DriverStatus = DriverStatus.APPROVED,
    val rating: Double = 4.9,
    val totalRides: Int = 312,
    val currentLat: Double = 26.6338,
    val currentLng: Double = 92.7926,
    val licenseDocUploaded: Boolean = true,
    val licenseDocUrl: String? = null,
    val bookingRingtoneEnabled: Boolean = true,
    val bookingRingtone: String = "E_RIDE_ALERT_1",
    val registeredAt: Long = System.currentTimeMillis()
)

sealed class PassengerOtpResult {
    data class ExistingPassenger(val passenger: Passenger) : PassengerOtpResult()
    data class NewPassenger(val verifiedPhone: String) : PassengerOtpResult()
    data class InvalidOtp(val message: String = "Invalid 6-digit OTP. Please enter the correct verification code.") : PassengerOtpResult()
}

sealed class DriverOtpResult {
    data class ExistingDriver(val driver: Driver) : DriverOtpResult()
    data class NewDriver(val verifiedPhone: String) : DriverOtpResult()
    data class InvalidOtp(val message: String = "Invalid 6-digit OTP. Please enter the correct verification code.") : DriverOtpResult()
}

sealed class DriverLoginResult {
    data class Success(val driver: Driver) : DriverLoginResult()
    data class PendingApproval(val driver: Driver) : DriverLoginResult()
    data class Rejected(val driver: Driver, val reason: String = "Registration review not approved by Admin.") : DriverLoginResult()
    data class Suspended(val driver: Driver, val reason: String = "Driver account temporarily suspended by Admin.") : DriverLoginResult()
    data class Deleted(val reason: String = "This driver account has been deactivated.") : DriverLoginResult()
    data class NotFound(val message: String = "No driver account found with this mobile number.") : DriverLoginResult()
    data class InvalidPhone(val message: String = "Please enter a valid 10-digit Indian mobile phone number.") : DriverLoginResult()
}

sealed class DriverRegisterResult {
    data class Success(val driver: Driver) : DriverRegisterResult()
    data class DuplicatePhone(val message: String = "A driver account with this mobile phone number already exists.") : DriverRegisterResult()
    data class InvalidInput(val message: String) : DriverRegisterResult()
    data class LocationOutsideServiceArea(val message: String) : DriverRegisterResult()
}

@Serializable
data class LocationPoint(
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val city: String = "Tezpur"
)

@Serializable
data class LocationDetails(
    val placeName: String,
    val formattedAddress: String,
    val latitude: Double,
    val longitude: Double,
    val route: String? = null,
    val road: String? = null,
    val street: String? = null,
    val premise: String? = null,
    val subpremise: String? = null,
    val village: String? = null,
    val locality: String = "",
    val sublocality: String? = null,
    val neighborhood: String? = null,
    val postalTown: String? = null,
    val town: String? = null,
    val district: String = "",
    val state: String = "Assam",
    val postalCode: String = "",
    val country: String = "India",
    val placeId: String? = null
)

@Serializable
data class ServiceCentre(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusKm: Double = 30.0,
    val district: String
)

@Serializable
data class ServiceAreaValidation(
    val isValid: Boolean,
    val nearestCentreName: String,
    val distanceKm: Double,
    val message: String
)

object ServiceCentres {
    val TEZPUR = ServiceCentre("Tezpur", 26.6338, 92.7926, 30.0, "Sonitpur District")
    val BISWANATH_CHARIALI = ServiceCentre("Biswanath Chariali", 26.7329, 93.1554, 30.0, "Biswanath District")
    val NAGSANKAR = ServiceCentre("Nagsankar", 26.6900, 92.9800, 30.0, "Sonitpur/Biswanath District")

    val ALL = listOf(TEZPUR, BISWANATH_CHARIALI, NAGSANKAR)
}

@Serializable
data class CommissionLedgerEntry(
    val id: String,
    val rideId: String,
    val driverId: String,
    val grossFare: Double,
    val commissionRate: Double = 10.0, // Specific percentage used for this ride (e.g., 10.0)
    val commissionAmount: Double,
    val driverEarning: Double,
    val paymentMethod: PaymentMode,
    val paymentStatus: PaymentStatus,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long = System.currentTimeMillis()
)

@Serializable
data class FinancialTransaction(
    val transactionId: String,
    val driverId: String,
    val rideId: String? = null,
    val amount: Double,
    val type: TransactionType,
    val status: TransactionStatus = TransactionStatus.COMPLETED,
    val timestamp: Long = System.currentTimeMillis(),
    val description: String,
    val isCredit: Boolean
) {
    val id: String get() = transactionId
}

@Serializable
data class DriverEarningsSummary(
    val todayEarnings: Double = 0.0,
    val thisWeekEarnings: Double = 0.0,
    val thisMonthEarnings: Double = 0.0,
    val totalEarnings: Double = 0.0,
    val totalRides: Int = 0,
    val completedRides: Int = 0,
    val cashCollected: Double = 0.0,
    val onlineCollected: Double = 0.0,
    val adminCommission: Double = 0.0,
    val netDriverEarnings: Double = 0.0
)

@Serializable
data class AdminFinancialMetrics(
    val totalRevenue: Double = 0.0,
    val adminCommission: Double = 0.0,
    val driverEarnings: Double = 0.0,
    val cashCollection: Double = 0.0,
    val onlineCollection: Double = 0.0,
    val pendingCommission: Double = 0.0,
    val completedRidesCount: Int = 0
)

@Serializable
data class RideRecord(
    val id: String,
    val passengerId: String,
    val passengerName: String,
    val passengerPhone: String = "+91 98640 12345",
    val driverId: String? = null,
    val driverName: String? = null,
    val driverPhone: String? = null,
    val rickshawRegNo: String? = null,
    val pickupLocation: String,
    val pickupAddress: String = "",
    val pickupLat: Double = 26.1445,
    val pickupLng: Double = 91.7362,
    val dropoffLocation: String,
    val dropoffAddress: String = "",
    val dropoffLat: Double = 26.1820,
    val dropoffLng: Double = 91.7580,
    val distanceKm: Double = 3.8,
    val fare: Double = 45.0,
    val status: RideStatus = RideStatus.REQUESTED,
    val createdAt: Long = System.currentTimeMillis(),
    val acceptedAt: Long? = null,
    val arrivedAt: Long? = null,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val cancelledAt: Long? = null,
    val cancellationReason: String? = null,
    val cancelledBy: String? = null,
    val otp: String = "4821",
    val paymentMode: PaymentMode = PaymentMode.CASH,
    val paymentStatus: PaymentStatus = PaymentStatus.PENDING,
    val driverRatingByPassenger: Int? = null,
    val passengerReview: String? = null,
    val commissionRate: Double = 10.0, // Immutable historical snapshot
    val commissionAmount: Double = 4.5,
    val driverEarning: Double = 40.5,
    val commissionPaid: Boolean = false,
    val cashCollectedByDriver: Double = 0.0,
    val onlineCollectedByPlatform: Double = 0.0,
    val isFinancialSettled: Boolean = false
) {
    val grossFare: Double get() = fare
}

@Serializable
data class DriverWallet(
    val driverId: String,
    val availableBalance: Double = 0.0,
    val pendingBalance: Double = 0.0,
    val commissionDue: Double = 0.0,
    val totalEarnings: Double = 0.0,
    val totalWithdrawn: Double = 0.0,
    val lifetimeEarnings: Double = 0.0,
    val totalRides: Int = 0
)

@Serializable
data class WalletTransaction(
    val id: String,
    val driverId: String,
    val amount: Double,
    val type: String, // "RIDE_EARNING", "COMMISSION", "WITHDRAWAL", "REFUND", "ADJUSTMENT"
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isCredit: Boolean,
    val rideId: String? = null,
    val status: TransactionStatus = TransactionStatus.COMPLETED
)

@Serializable
data class WithdrawalRequest(
    val id: String,
    val driverId: String,
    val driverName: String,
    val driverPhone: String = "+91 94350 12345",
    val amount: Double,
    val paymentMethod: String = "UPI", // "UPI" or "BANK_ACCOUNT"
    val upiId: String = "",
    val bankName: String? = null,
    val accountHolderName: String? = null,
    val accountNumber: String? = null,
    val maskedAccountNumber: String? = null,
    val ifscCode: String? = null,
    val bankDetails: String? = "State Bank of India (Assam Branch)",
    val status: WithdrawalStatus = WithdrawalStatus.PENDING,
    val requestedAt: Long = System.currentTimeMillis(),
    val processedAt: Long? = null,
    val paidAt: Long? = null,
    val transactionReference: String? = null,
    val rejectionReason: String? = null
)

@Serializable
data class ComplaintRecord(
    val id: String,
    val rideId: String? = null,
    val reporterId: String,
    val reporterName: String,
    val reporterRole: UserRole,
    val driverId: String? = null,
    val driverName: String? = null,
    val passengerId: String? = null,
    val passengerName: String? = null,
    val priority: String = "NORMAL",
    val category: ComplaintCategory = ComplaintCategory.RIDE,
    val subject: String,
    val description: String,
    val status: ComplaintStatus = ComplaintStatus.PENDING,
    val assignedAdmin: String = "Operations Team",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val internalNotes: List<String> = emptyList(),
    val replies: List<String> = emptyList(),
    val resolutionNotes: String? = null
)

enum class DeletionRequestStatus {
    REQUESTED,
    VERIFIED,
    COMPLETED,
    REJECTED
}

@Serializable
data class DriverDeletionRecord(
    val deletionRequestId: String,
    val driverId: String,
    val driverName: String,
    val driverPhone: String,
    val rickshawRegNo: String,
    val requestedAt: Long,
    val completedAt: Long,
    val status: DeletionRequestStatus = DeletionRequestStatus.COMPLETED,
    val reason: String,
    val processedBy: String = "AUTHENTICATED_DRIVER_SELF",
    val retainedFinancialSummary: String = "",
    val totalRidesCompleted: Int = 0,
    val lifetimeEarnings: Double = 0.0
) {
    val id: String get() = deletionRequestId
}

sealed class DeleteAccountResult {
    data class Success(val deletionRecord: DriverDeletionRecord) : DeleteAccountResult()
    data class HasActiveRide(val rideId: String, val message: String) : DeleteAccountResult()
    data class FinancialPending(
        val message: String,
        val pendingAmount: Double,
        val commissionDue: Double,
        val pendingWithdrawal: Double
    ) : DeleteAccountResult()
    data class Unauthorized(val message: String) : DeleteAccountResult()
    data class Error(val message: String) : DeleteAccountResult()
}

object AccountDeletionReasons {
    const val NO_LONGER_NEED = "No longer need the service"
    const val MOVING_SERVICE = "Moving to another service"
    const val PRIVACY_CONCERNS = "Privacy concerns"
    const val TECHNICAL_PROBLEMS = "Technical problems"
    const val OTHER = "Other"

    val options = listOf(
        NO_LONGER_NEED,
        MOVING_SERVICE,
        PRIVACY_CONCERNS,
        TECHNICAL_PROBLEMS,
        OTHER
    )
}

data class DriverFinancialStatus(
    val availableBalance: Double = 0.0,
    val pendingEarnings: Double = 0.0,
    val commissionDue: Double = 0.0,
    val hasPendingWithdrawal: Boolean = false,
    val pendingWithdrawalAmount: Double = 0.0,
    val lifetimeEarnings: Double = 0.0,
    val totalRides: Int = 0,
    val hasOutstandingLiability: Boolean = false
) {
    val walletBalance: Double get() = availableBalance
}

sealed class AcceptRideResult {
    data class Success(val ride: RideRecord) : AcceptRideResult()
    data class CancelledByPassenger(val message: String = "Passenger cancelled this ride request.") : AcceptRideResult()
    data class Expired(val message: String = "Ride request expired.") : AcceptRideResult()
    data class AlreadyAccepted(val message: String = "Another driver has already accepted this ride.") : AcceptRideResult()
    data class DriverIneligible(val message: String = "Driver is currently offline or unapproved.") : AcceptRideResult()
    data class Error(val message: String) : AcceptRideResult()
}

@Serializable
data class RideOffer(
    val rideId: String,
    val passengerId: String,
    val passengerName: String,
    val pickupLocation: String,
    val pickupAddress: String = "",
    val dropoffLocation: String,
    val dropoffAddress: String = "",
    val fare: Double,
    val driverEarning: Double = 0.0,
    val commissionRate: Double = 10.0,
    val commissionAmount: Double = 0.0,
    val paymentMode: PaymentMode = PaymentMode.CASH,
    val distanceKm: Double,
    val expiresAtMillis: Long = System.currentTimeMillis() + 30000L,
    val serviceCity: String = "Tezpur",
    val rejectedDriverIds: List<String> = emptyList(),
    val targetDriverId: String? = null,
    val eligibleDriverIds: List<String> = emptyList(),
    val currentStageRadiusKm: Double = 2.0
)

@Serializable
data class DriverSearchState(
    val rideId: String? = null,
    val isSearching: Boolean = false,
    val stage: Int = 1,
    val currentRadiusKm: Double = 2.0,
    val secondsRemainingInStage: Int = 10,
    val noDriverAvailable: Boolean = false,
    val notifiedDriverCount: Int = 0
)

