package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.models.*
import com.example.data.repository.RideRepository
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.LocationHelper
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerHomeScreen(
    onRideRequested: () -> Unit,
    onViewHistory: () -> Unit,
    onOpenHelp: () -> Unit,
    onBackToPortal: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val currentPassenger by RideRepository.currentPassenger.collectAsStateWithLifecycle()
    val activeRide by RideRepository.activeRide.collectAsStateWithLifecycle()
    val recentDestinations by RideRepository.recentDestinations.collectAsStateWithLifecycle()

    // Default to Tezpur ASTC (Pickup) and Mahabhairab Temple (Drop)
    var selectedPickup by remember {
        mutableStateOf(
            LocationDetails(
                placeName = "ASTC Bus Stand, Tezpur",
                formattedAddress = "Court Chariali, ASTC Central Station, Tezpur, Sonitpur, Assam - 784001",
                latitude = 26.6322,
                longitude = 92.7930,
                locality = "Court Chariali",
                district = "Sonitpur",
                postalCode = "784001"
            )
        )
    }

    var selectedDropoff by remember {
        mutableStateOf(
            LocationDetails(
                placeName = "Mahabhairab Temple, Tezpur",
                formattedAddress = "Mahabhairab Road, Tezpur, Sonitpur, Assam - 784001",
                latitude = 26.6395,
                longitude = 92.7975,
                locality = "Mahabhairab",
                district = "Sonitpur",
                postalCode = "784001"
            )
        )
    }

    var selectedPaymentMode by remember { mutableStateOf(PaymentMode.CASH) }
    var selectedBottomNav by remember { mutableStateOf(0) }

    // Modal states
    var showPickupSearchModal by remember { mutableStateOf(false) }
    var showDropSearchModal by remember { mutableStateOf(false) }
    var showPickupMapPicker by remember { mutableStateOf(false) }
    var showDropMapPicker by remember { mutableStateOf(false) }
    var isFetchingGpsLocation by remember { mutableStateOf(false) }
    var gpsErrorMessage by remember { mutableStateOf<String?>(null) }
    var showGpsErrorDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }

    // Service Area 30km validation
    val pickupValidation = remember(selectedPickup) {
        LocationHelper.validateServiceArea(selectedPickup.latitude, selectedPickup.longitude)
    }

    val dropValidation = remember(selectedDropoff) {
        LocationHelper.validateServiceArea(selectedDropoff.latitude, selectedDropoff.longitude)
    }

    val isBookingAllowed = pickupValidation.isValid && dropValidation.isValid

    // Accurate road distance and estimated time
    val (distanceKm, estimatedMins) = remember(selectedPickup, selectedDropoff) {
        LocationHelper.calculateRoute(
            selectedPickup.latitude,
            selectedPickup.longitude,
            selectedDropoff.latitude,
            selectedDropoff.longitude
        )
    }

    val estimatedFare = remember(distanceKm) {
        Math.max(RideRepository.minimumFare, Math.round(distanceKm * RideRepository.perKmRate * 1.0).toDouble())
    }

    // Permission launcher for runtime location permission
    lateinit var triggerFetchGps: () -> Unit
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            triggerFetchGps()
        } else {
            gpsErrorMessage = "Location permission was denied. You can search your pickup location manually."
            showGpsErrorDialog = true
        }
    }

    // Function to acquire real GPS location
    fun fetchCurrentGpsLocation() {
        if (!LocationHelper.hasLocationPermission(context)) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }

        if (!LocationHelper.isLocationServiceEnabled(context)) {
            gpsErrorMessage = "Please turn on Location Services (GPS) on your device."
            showGpsErrorDialog = true
            return
        }

        isFetchingGpsLocation = true
        LocationHelper.getCurrentGpsCoordinates(
            context = context,
            onSuccess = { lat, lng ->
                coroutineScope.launch {
                    val locationDetails = LocationHelper.reverseGeocode(context, lat, lng)
                    selectedPickup = locationDetails
                    isFetchingGpsLocation = false
                }
            },
            onError = { error ->
                isFetchingGpsLocation = false
                gpsErrorMessage = error
                showGpsErrorDialog = true
            }
        )
    }

    triggerFetchGps = { fetchCurrentGpsLocation() }

    // Automatically detect GPS Current Location on screen open
    LaunchedEffect(Unit) {
        if (LocationHelper.hasLocationPermission(context)) {
            fetchCurrentGpsLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // If active ride already exists, redirect automatically
    LaunchedEffect(activeRide) {
        if (activeRide != null && activeRide!!.status != RideStatus.COMPLETED && activeRide!!.status != RideStatus.CANCELLED) {
            onRideRequested()
        }
    }

    Scaffold(
        topBar = {
            HeaderAppBar(
                title = "E-Ride 3 Passenger",
                subtitle = "Assam E-Rickshaw Booking",
                onBack = onBackToPortal,
                actions = {
                    IconButton(onClick = onViewHistory) {
                        Icon(Icons.Default.History, contentDescription = "History", tint = Color.White)
                    }
                    IconButton(onClick = onOpenHelp) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "Help", tint = Color.White)
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedBottomNav == 0,
                    onClick = { selectedBottomNav = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BrandGreenDark,
                        selectedTextColor = BrandGreenDark,
                        indicatorColor = BrandGreenLight
                    )
                )
                NavigationBarItem(
                    selected = selectedBottomNav == 1,
                    onClick = {
                        selectedBottomNav = 1
                        onViewHistory()
                    },
                    icon = { Icon(Icons.Default.DirectionsCar, contentDescription = "My Rides") },
                    label = { Text("My Rides", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BrandGreenDark,
                        selectedTextColor = BrandGreenDark,
                        indicatorColor = BrandGreenLight
                    )
                )
                NavigationBarItem(
                    selected = selectedBottomNav == 2,
                    onClick = {
                        selectedBottomNav = 2
                        onOpenHelp()
                    },
                    icon = { Icon(Icons.Default.HelpOutline, contentDescription = "Help") },
                    label = { Text("Help", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BrandGreenDark,
                        selectedTextColor = BrandGreenDark,
                        indicatorColor = BrandGreenLight
                    )
                )
                NavigationBarItem(
                    selected = selectedBottomNav == 3,
                    onClick = {
                        selectedBottomNav = 3
                        showProfileDialog = true
                    },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BrandGreenDark,
                        selectedTextColor = BrandGreenDark,
                        indicatorColor = BrandGreenLight
                    )
                )
            }
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
            // Service Areas Notification Banner
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BrandGreenSurface,
                    border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationCity,
                            contentDescription = null,
                            tint = BrandGreenDark,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "E-Ride 3 Service Areas (Assam)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = BrandGreenDark
                            )
                            Text(
                                text = "Available across Tezpur, Biswanath Chariali and Nagsankar (30 km radius each)",
                                fontSize = 11.sp,
                                color = SlateGray,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }

            // Passenger Profile greeting
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(BrandGreenLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = BrandGreenDark, modifier = Modifier.size(26.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Hello, ${currentPassenger.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CharcoalDark)
                            Text(text = "Base: Tezpur / Biswanath • ⭐ ${currentPassenger.rating}", fontSize = 12.sp, color = SlateGray)
                        }
                        Surface(shape = RoundedCornerShape(20.dp), color = BrandGreenLight) {
                            Text(
                                text = "${currentPassenger.totalRides} Trips",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandGreenDark
                            )
                        }
                    }
                }
            }

            // BOOK YOUR E-RICKSHAW Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "BOOK YOUR E-RICKSHAW",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = CharcoalDark,
                                letterSpacing = 0.5.sp
                            )
                            Surface(shape = RoundedCornerShape(6.dp), color = BrandGreenLight) {
                                Text(
                                    text = "30 KM Radius Coverage",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandGreenDark
                                )
                            }
                        }

                        // 1. PICKUP / CURRENT LOCATION SECTION
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "PICKUP / CURRENT LOCATION",
                                    fontSize = 11.sp,
                                    color = SlateGray,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                if (isFetchingGpsLocation) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = BrandGreen)
                                        Text("Detecting GPS...", fontSize = 11.sp, color = BrandGreenDark, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }

                            // Selected Pickup Card Display
                            OutlinedCard(
                                onClick = { showPickupSearchModal = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (pickupValidation.isValid) CardBorder else RedCancel.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.TripOrigin, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(18.dp))
                                        Text(
                                            text = selectedPickup.placeName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = CharcoalDark
                                        )
                                    }
                                    Text(
                                        text = selectedPickup.formattedAddress,
                                        fontSize = 11.sp,
                                        color = SlateGray,
                                        maxLines = 2,
                                        lineHeight = 15.sp,
                                        modifier = Modifier.padding(start = 26.dp)
                                    )
                                    Text(
                                        text = "GPS: ${String.format(Locale.US, "%.4f, %.4f", selectedPickup.latitude, selectedPickup.longitude)} • ${if (selectedPickup.locality.isNotBlank()) selectedPickup.locality + ", " else ""}${selectedPickup.district}",
                                        fontSize = 10.sp,
                                        color = BrandGreenDark,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(start = 26.dp)
                                    )

                                    // Service area status pill
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (pickupValidation.isValid) BrandGreenLight else RedCancelLight,
                                        modifier = Modifier.padding(start = 26.dp, top = 2.dp)
                                    ) {
                                        Text(
                                            text = if (pickupValidation.isValid) "✓ ${pickupValidation.message}" else "❌ ${pickupValidation.message}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (pickupValidation.isValid) BrandGreenDark else RedCancel,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            // Pickup Action Buttons: [Use Current Location], [Search], [Map Picker]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (LocationHelper.hasLocationPermission(context)) {
                                            fetchCurrentGpsLocation()
                                        } else {
                                            locationPermissionLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                                )
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1.3f).height(38.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.MyLocation, contentDescription = null, tint = BrandGreenDark, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Use Current Location", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandGreenDark)
                                }

                                OutlinedButton(
                                    onClick = { showPickupSearchModal = true },
                                    modifier = Modifier.weight(0.9f).height(38.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = CharcoalDark, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Search", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CharcoalDark)
                                }

                                OutlinedButton(
                                    onClick = { showPickupMapPicker = true },
                                    modifier = Modifier.weight(0.9f).height(38.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                ) {
                                    Icon(Icons.Default.Map, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Map", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandGreen)
                                }
                            }
                        }

                        HorizontalDivider(color = LightSlate)

                        // 2. DROP LOCATION SECTION
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "DROP LOCATION",
                                fontSize = 11.sp,
                                color = SlateGray,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )

                            // Selected Drop Card Display
                            OutlinedCard(
                                onClick = { showDropSearchModal = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (dropValidation.isValid) CardBorder else RedCancel.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = RedCancel, modifier = Modifier.size(18.dp))
                                        Text(
                                            text = selectedDropoff.placeName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = CharcoalDark
                                        )
                                    }
                                    Text(
                                        text = selectedDropoff.formattedAddress,
                                        fontSize = 11.sp,
                                        color = SlateGray,
                                        maxLines = 2,
                                        lineHeight = 15.sp,
                                        modifier = Modifier.padding(start = 26.dp)
                                    )
                                    Text(
                                        text = "GPS: ${String.format(Locale.US, "%.4f, %.4f", selectedDropoff.latitude, selectedDropoff.longitude)} • ${if (selectedDropoff.locality.isNotBlank()) selectedDropoff.locality + ", " else ""}${selectedDropoff.district}",
                                        fontSize = 10.sp,
                                        color = RedCancel,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(start = 26.dp)
                                    )

                                    // Service area status pill
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (dropValidation.isValid) BrandGreenLight else RedCancelLight,
                                        modifier = Modifier.padding(start = 26.dp, top = 2.dp)
                                    ) {
                                        Text(
                                            text = if (dropValidation.isValid) "✓ ${dropValidation.message}" else "❌ ${dropValidation.message}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (dropValidation.isValid) BrandGreenDark else RedCancel,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            // Drop Action Buttons: [Search Drop Location], [Map Picker]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showDropSearchModal = true },
                                    modifier = Modifier.weight(1.2f).height(38.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = RedCancel, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Search Drop Location", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RedCancel)
                                }

                                OutlinedButton(
                                    onClick = { showDropMapPicker = true },
                                    modifier = Modifier.weight(0.9f).height(38.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                ) {
                                    Icon(Icons.Default.Map, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Map Picker", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandGreen)
                                }
                            }

                            if (recentDestinations.isNotEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Icon(Icons.Default.History, contentDescription = null, tint = SlateGray, modifier = Modifier.size(14.dp))
                                    Text("Recent:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateGray)
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(recentDestinations.take(4)) { rd ->
                                            SuggestionChip(
                                                onClick = { selectedDropoff = rd },
                                                label = { Text(rd.placeName, fontSize = 10.sp, maxLines = 1) },
                                                colors = SuggestionChipDefaults.suggestionChipColors(
                                                    containerColor = BrandGreenLight.copy(alpha = 0.5f),
                                                    labelColor = CharcoalDark
                                                ),
                                                border = BorderStroke(0.5.dp, BrandGreen.copy(alpha = 0.3f))
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = LightSlate)

                        // Payment mode selection
                        Text(text = "Payment Mode", fontSize = 12.sp, color = SlateGray, fontWeight = FontWeight.Medium)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilterChip(
                                selected = selectedPaymentMode == PaymentMode.CASH,
                                onClick = { selectedPaymentMode = PaymentMode.CASH },
                                label = { Text("💵 Cash to Driver", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedPaymentMode == PaymentMode.ONLINE_UPI,
                                onClick = { selectedPaymentMode = PaymentMode.ONLINE_UPI },
                                label = { Text("📱 UPI QR Scan", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Trip Estimate Details
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = BrandGreenSurface,
                            border = BorderStroke(1.dp, BrandGreenLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = "Estimated Distance & Time", fontSize = 11.sp, color = SlateGray)
                                    Text(text = "$distanceKm km • ~$estimatedMins mins", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CharcoalDark)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "Fixed E-Rickshaw Fare", fontSize = 11.sp, color = SlateGray)
                                    Text(text = "₹${estimatedFare.toInt()}", fontWeight = FontWeight.Black, fontSize = 20.sp, color = BrandGreenDark)
                                }
                            }
                        }

                        // Service Area Warning Banner if outside
                        if (!isBookingAllowed) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = RedCancelLight,
                                border = BorderStroke(1.dp, RedCancel.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "⚠️ Booking blocked: Location outside service area",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = RedCancel
                                    )
                                    Text(
                                        text = "E-Ride 3 is available only within a 30 km radius of Tezpur, Biswanath Chariali, and Nagsankar. Please adjust pickup or drop destination to a supported area.",
                                        fontSize = 11.sp,
                                        color = RedCancel,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }

                        // Book Ride Button
                        Button(
                            onClick = {
                                if (isBookingAllowed) {
                                    RideRepository.requestRide(
                                        pickup = selectedPickup.placeName,
                                        pickupAddress = selectedPickup.formattedAddress,
                                        pickupLat = selectedPickup.latitude,
                                        pickupLng = selectedPickup.longitude,
                                        dropoff = selectedDropoff.placeName,
                                        dropoffAddress = selectedDropoff.formattedAddress,
                                        dropoffLat = selectedDropoff.latitude,
                                        dropoffLng = selectedDropoff.longitude,
                                        distanceKm = distanceKm,
                                        paymentMode = selectedPaymentMode
                                    )
                                    onRideRequested()
                                }
                            },
                            enabled = isBookingAllowed,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandGreen,
                                disabledContainerColor = LightSlate
                            )
                        ) {
                            Icon(Icons.Default.ElectricRickshaw, contentDescription = null, tint = if (isBookingAllowed) Color.White else SlateGray)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBookingAllowed) "Request E-Ride • ₹${estimatedFare.toInt()}" else "Select Location in Service Area",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (isBookingAllowed) Color.White else SlateGray
                            )
                        }
                    }
                }
            }

            // Popular Landmarks in Official Service Areas
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "POPULAR LANDMARKS IN SERVICE AREAS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = SlateGray,
                        letterSpacing = 0.5.sp
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(LocationHelper.defaultServiceLocations) { loc ->
                            Card(
                                modifier = Modifier
                                    .width(220.dp)
                                    .clickable {
                                        selectedDropoff = loc
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, CardBorder)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.Place, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(16.dp))
                                        Text(text = loc.placeName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CharcoalDark, maxLines = 1)
                                    }
                                    Text(text = loc.formattedAddress, fontSize = 10.sp, color = SlateGray, maxLines = 2, lineHeight = 13.sp)
                                    Surface(shape = RoundedCornerShape(4.dp), color = BrandGreenLight) {
                                        Text(
                                            text = "Set as Drop Point",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandGreenDark
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Available Eco-Rickshaws nearby (Without exposing phone numbers before booking!)
            item {
                Text(
                    text = "Active Eco-Fleet in Service Area",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = CharcoalDark
                )
            }

            items(RideRepository.driversList.value.filter { it.isOnline && it.status == DriverStatus.APPROVED }) { driver ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(BrandGreenLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ElectricRickshaw, contentDescription = null, tint = BrandGreenDark, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = driver.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CharcoalDark)
                            Text(text = "${driver.vehicleModel} • ${driver.rickshawRegNo}", fontSize = 11.sp, color = SlateGray)
                        }
                        Surface(shape = RoundedCornerShape(6.dp), color = LightSlate) {
                            Text(
                                text = "⭐ ${driver.rating}",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CharcoalDark
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal: Pickup Location Search
    if (showPickupSearchModal) {
        LocationSearchModal(
            title = "Search Pickup Location",
            initialQuery = "",
            onOpenMapPicker = { showPickupMapPicker = true },
            onLocationSelected = { loc ->
                selectedPickup = loc
                showPickupSearchModal = false
            },
            onDismiss = { showPickupSearchModal = false }
        )
    }

    // Modal: Drop Location Search
    if (showDropSearchModal) {
        LocationSearchModal(
            title = "Search Drop Location",
            initialQuery = "",
            onOpenMapPicker = { showDropMapPicker = true },
            onLocationSelected = { loc ->
                selectedDropoff = loc
                showDropSearchModal = false
            },
            onDismiss = { showDropSearchModal = false }
        )
    }

    // Modal: Pickup Map Picker
    if (showPickupMapPicker) {
        MapPickerModal(
            mode = MapPickerMode.PICKUP,
            initialLocation = selectedPickup,
            onLocationConfirmed = { loc ->
                selectedPickup = loc
                showPickupMapPicker = false
            },
            onDismiss = { showPickupMapPicker = false }
        )
    }

    // Modal: Drop Map Picker
    if (showDropMapPicker) {
        MapPickerModal(
            mode = MapPickerMode.DROPOFF,
            initialLocation = selectedDropoff,
            onLocationConfirmed = { loc ->
                selectedDropoff = loc
                showDropMapPicker = false
            },
            onDismiss = { showDropMapPicker = false }
        )
    }

    // GPS / Permission Error Dialog
    if (showGpsErrorDialog) {
        AlertDialog(
            onDismissRequest = { showGpsErrorDialog = false },
            icon = { Icon(Icons.Default.LocationOff, contentDescription = null, tint = RedCancel, modifier = Modifier.size(32.dp)) },
            title = { Text("Location Access Notice", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = gpsErrorMessage ?: "Could not access GPS location. Please check your device location settings.",
                    fontSize = 13.sp,
                    color = CharcoalDark
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showGpsErrorDialog = false
                        LocationHelper.openLocationSettings(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                ) {
                    Text("Settings", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showGpsErrorDialog = false
                    showPickupSearchModal = true
                }) {
                    Text("Search Manually")
                }
            }
        )
    }

    // Passenger Profile Dialog
    if (showProfileDialog) {
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text("Passenger Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Name: ${currentPassenger.name}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Phone: ${currentPassenger.phone}", fontSize = 13.sp, color = SlateGray)
                    Text("Primary City: Assam (Tezpur / Biswanath / Nagsankar)", fontSize = 13.sp, color = SlateGray)
                    Text("Total Completed Trips: ${currentPassenger.totalRides}", fontSize = 13.sp, color = SlateGray)
                    Text("Rating: ⭐ ${currentPassenger.rating}", fontSize = 13.sp, color = ElectricAmber)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showProfileDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                ) {
                    Text("Close", color = Color.White)
                }
            }
        )
    }
}

/**
 * Passenger Active Ride Screen.
 * Demonstrates the exact Post-Booking Phone Number Visibility and Normal Phone Call feature.
 */
@Composable
fun PassengerActiveRideScreen(
    onRideCompleted: () -> Unit,
    onBack: () -> Unit
) {
    val activeRide by RideRepository.activeRide.collectAsStateWithLifecycle()
    val searchState by RideRepository.driverSearchState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showCancelDialog by remember { mutableStateOf(false) }
    var showRatingDialog by remember { mutableStateOf(false) }
    var selectedRating by remember { mutableIntStateOf(5) }
    var reviewText by remember { mutableStateOf("") }

    if (activeRide == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "No active booking", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Button(onClick = onBack) {
                    Text("Back to Home")
                }
            }
        }
        return
    }

    val ride = activeRide!!

    // Handle completed state
    LaunchedEffect(ride.status) {
        if (ride.status == RideStatus.COMPLETED) {
            showRatingDialog = true
        }
    }

    Scaffold(
        topBar = {
            HeaderAppBar(
                title = "Active Trip: ${ride.id}",
                subtitle = "Assam E-Rickshaw Live Tracker",
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
            // Status Banner & OTP
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
                            RideStatusBadge(status = ride.status)
                            Text(
                                text = "Fare: ₹${ride.fare.toInt()}",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = BrandGreenDark
                            )
                        }

                        // OTP Boarding Card
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ElectricAmberLight,
                            border = BorderStroke(1.dp, ElectricAmber.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Start Ride OTP",
                                        fontSize = 11.sp,
                                        color = CharcoalDark,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Share with driver upon boarding",
                                        fontSize = 10.sp,
                                        color = SlateGray
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = ElectricAmber
                                ) {
                                    Text(
                                        text = ride.otp,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 22.sp,
                                        color = Color.White,
                                        letterSpacing = 2.sp,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Live Map
            item {
                MapViewComponent(
                    pickupName = ride.pickupLocation,
                    dropoffName = ride.dropoffLocation,
                    status = ride.status,
                    driverName = ride.driverName
                )
            }

            // Assigned Driver Details & Call Feature (Post-Booking Confirmation)
            item {
                if (ride.driverName != null) {
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
                                Text(
                                    text = "ASSIGNED DRIVER DETAILS",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = SlateGray,
                                    letterSpacing = 0.5.sp
                                )
                                Surface(shape = RoundedCornerShape(6.dp), color = BrandGreenLight) {
                                    Text(
                                        text = "Verified E-Rickshaw",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandGreenDark
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(BrandGreenLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ElectricRickshaw,
                                        contentDescription = null,
                                        tint = BrandGreenDark,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ride.driverName ?: "Driver Partner",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = CharcoalDark
                                    )
                                    Text(
                                        text = ride.driverPhone ?: "Phone available on confirmation",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = BrandGreenDark
                                    )
                                    Text(
                                        text = "Assam Eco-Fleet Partner",
                                        fontSize = 11.sp,
                                        color = SlateGray
                                    )
                                }
                            }

                            if (!ride.driverPhone.isNullOrBlank()) {
                                Button(
                                    onClick = {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                            data = android.net.Uri.parse("tel:${ride.driverPhone}")
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
                                    Text("Call Driver (${ride.driverPhone})", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                } else {
                    if (searchState.noDriverAvailable) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, RedCancel.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(RedCancelLight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = RedCancel,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "No driver is currently available nearby.",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = CharcoalDark
                                        )
                                        Text(
                                            text = "All nearby drivers within 30 km are currently busy or offline.",
                                            fontSize = 12.sp,
                                            color = SlateGray
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = { RideRepository.searchAgain(ride.id) },
                                        modifier = Modifier.weight(1f).height(44.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("SEARCH AGAIN", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            RideRepository.cancelRide(ride.id, "No driver available", "PASSENGER")
                                            onBack()
                                        },
                                        modifier = Modifier.weight(1f).height(44.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RedCancel),
                                        border = BorderStroke(1.dp, RedCancel)
                                    ) {
                                        Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("CANCEL BOOKING", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, CardBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp, color = BrandGreen)
                                Column {
                                    Text(
                                        text = "Searching for Driver (Stage ${searchState.stage} • ${searchState.currentRadiusKm.toInt()} KM)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = CharcoalDark
                                    )
                                    Text(
                                        text = "Searching within ${searchState.currentRadiusKm.toInt()} km • ${searchState.secondsRemainingInStage}s left in stage",
                                        fontSize = 11.sp,
                                        color = BrandGreenDark,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Driver phone number will appear once driver accepts",
                                        fontSize = 11.sp,
                                        color = SlateGray
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Trip Locations Details
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
                        Text(text = "Trip Details", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CharcoalDark)
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.TripOrigin, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(18.dp))
                            Column {
                                Text(text = "Pickup Point", fontSize = 11.sp, color = SlateGray)
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

            // Cancel Button (if trip not completed)
            if (ride.status in listOf(RideStatus.REQUESTED, RideStatus.DRIVER_ASSIGNED, RideStatus.DRIVER_ARRIVED)) {
                item {
                    OutlinedButton(
                        onClick = { showCancelDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RedCancel),
                        border = BorderStroke(1.dp, RedCancel)
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cancel Booking", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Cancel Dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel E-Ride?") },
            text = { Text("Are you sure you want to cancel this booking?") },
            confirmButton = {
                Button(
                    onClick = {
                        RideRepository.cancelRide(ride.id, "Cancelled by passenger", "PASSENGER")
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

    // Rating Dialog after trip completion
    if (showRatingDialog) {
        Dialog(onDismissRequest = {}) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(52.dp))
                    Text("Trip Completed!", fontWeight = FontWeight.Black, fontSize = 20.sp, color = CharcoalDark)
                    Text("Total Fare Paid: ₹${ride.fare.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = BrandGreenDark)

                    Text("How was your driver ${ride.driverName ?: "partner"}?", fontSize = 13.sp, color = SlateGray, textAlign = TextAlign.Center)

                    Row(horizontalArrangement = Arrangement.Center) {
                        for (i in 1..5) {
                            IconButton(onClick = { selectedRating = i }) {
                                Icon(
                                    imageVector = if (i <= selectedRating) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "$i Stars",
                                    tint = if (i <= selectedRating) ElectricAmber else SlateGray,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = reviewText,
                        onValueChange = { reviewText = it },
                        placeholder = { Text("Optional feedback...", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Button(
                        onClick = {
                            RideRepository.rateRide(ride.id, selectedRating, reviewText)
                            RideRepository.clearActiveRide()
                            showRatingDialog = false
                            onRideCompleted()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Submit Feedback & Finish", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun PassengerRideHistoryScreen(
    onBack: () -> Unit
) {
    val currentPassenger by RideRepository.currentPassenger.collectAsStateWithLifecycle()
    val allRides by RideRepository.ridesList.collectAsStateWithLifecycle()
    val passengerRides = allRides.filter { it.passengerId == currentPassenger.id }

    Scaffold(
        topBar = {
            HeaderAppBar(
                title = "Your Ride History",
                subtitle = "${passengerRides.size} Trips Recorded",
                onBack = onBack
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        if (passengerRides.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No past rides found", color = SlateGray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(passengerRides) { ride ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = ride.id, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SlateGray)
                                RideStatusBadge(status = ride.status)
                            }
                            Text(text = "${ride.pickupLocation} ➔ ${ride.dropoffLocation}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CharcoalDark)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Driver: ${ride.driverName ?: "Unassigned"}", fontSize = 12.sp, color = SlateGray)
                                Text(text = "₹${ride.fare.toInt()}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = BrandGreenDark)
                            }
                        }
                    }
                }
            }
        }
    }
}
