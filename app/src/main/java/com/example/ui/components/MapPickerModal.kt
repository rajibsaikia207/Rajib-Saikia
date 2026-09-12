package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.models.LocationDetails
import com.example.data.models.ServiceCentres
import com.example.ui.theme.*
import com.example.util.LocationHelper
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

enum class MapPickerMode {
    PICKUP,
    DROPOFF
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerModal(
    mode: MapPickerMode,
    initialLocation: LocationDetails,
    onLocationConfirmed: (LocationDetails) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentCenterLat by remember { mutableDoubleStateOf(initialLocation.latitude) }
    var currentCenterLng by remember { mutableDoubleStateOf(initialLocation.longitude) }
    var zoomLevel by remember { mutableFloatStateOf(14.0f) } // 10 to 18

    var isGeocoding by remember { mutableStateOf(false) }
    var currentAddressDetails by remember { mutableStateOf(initialLocation) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<LocationDetails>>(emptyList()) }

    // Live service area check for current pin
    val serviceValidation = remember(currentCenterLat, currentCenterLng) {
        LocationHelper.validateServiceArea(currentCenterLat, currentCenterLng)
    }

    // Trigger reverse geocoding when pin coordinates change
    fun updateCoordinates(lat: Double, lng: Double) {
        currentCenterLat = (lat * 1000000.0).roundToInt() / 1000000.0
        currentCenterLng = (lng * 1000000.0).roundToInt() / 1000000.0
    }

    // Automatically reverse geocode after map movement settles
    LaunchedEffect(currentCenterLat, currentCenterLng) {
        kotlinx.coroutines.delay(350)
        isGeocoding = true
        val result = LocationHelper.reverseGeocode(context, currentCenterLat, currentCenterLng)
        currentAddressDetails = result
        isGeocoding = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = if (mode == MapPickerMode.PICKUP) "Select Pickup on Map" else "Select Drop on Map",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Drag map or tap to place pin",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                if (LocationHelper.hasLocationPermission(context)) {
                                    LocationHelper.getCurrentGpsCoordinates(
                                        context = context,
                                        onSuccess = { lat, lng ->
                                            updateCoordinates(lat, lng)
                                        },
                                        onError = {}
                                    )
                                }
                            }
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = "Current GPS", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = BrandGreenDark,
                        titleContentColor = Color.White
                    )
                )
            },
            containerColor = BackgroundLight
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Interactive Map Canvas
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                // Sensitivity adjusted by zoom level
                                val factor = 0.00003 / (zoomLevel / 12f)
                                val newLat = currentCenterLat + (dragAmount.y * factor)
                                val newLng = currentCenterLng - (dragAmount.x * factor)
                                updateCoordinates(newLat, newLng)
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val width = size.width
                                val height = size.height
                                val dx = offset.x - (width / 2f)
                                val dy = offset.y - (height / 2f)
                                val factor = 0.00003 / (zoomLevel / 12f)
                                updateCoordinates(
                                    currentCenterLat - (dy * factor),
                                    currentCenterLng + (dx * factor)
                                )
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val center = Offset(width / 2f, height / 2f)

                        // Map Background (clean cartographic style)
                        drawRect(color = Color(0xFFF0F4F8))

                        // Grid Lines
                        val gridSize = 60f * (zoomLevel / 14f)
                        val numX = (width / gridSize).toInt() + 2
                        val numY = (height / gridSize).toInt() + 2

                        for (i in -1..numX) {
                            val x = i * gridSize
                            drawLine(
                                color = Color(0xFFE2E8F0),
                                start = Offset(x, 0f),
                                end = Offset(x, height),
                                strokeWidth = 1.5f
                            )
                        }
                        for (j in -1..numY) {
                            val y = j * gridSize
                            drawLine(
                                color = Color(0xFFE2E8F0),
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = 1.5f
                            )
                        }

                        // Brahmaputra River simulation
                        drawLine(
                            color = Color(0xFFBAE6FD),
                            start = Offset(0f, height * 0.7f),
                            end = Offset(width, height * 0.62f),
                            strokeWidth = 48f * (zoomLevel / 14f)
                        )

                        // Arterial Highway NH-15
                        drawLine(
                            color = Color.White,
                            start = Offset(0f, height * 0.45f),
                            end = Offset(width, height * 0.42f),
                            strokeWidth = 24f * (zoomLevel / 14f)
                        )
                        drawLine(
                            color = Color(0xFFFBBF24),
                            start = Offset(0f, height * 0.45f),
                            end = Offset(width, height * 0.42f),
                            strokeWidth = 3f * (zoomLevel / 14f),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f))
                        )

                        // Cross road linking service zone
                        drawLine(
                            color = Color.White,
                            start = Offset(width * 0.5f, 0f),
                            end = Offset(width * 0.5f, height),
                            strokeWidth = 18f * (zoomLevel / 14f)
                        )

                        // Service Area 30km visual coverage rings
                        val ringColor = if (serviceValidation.isValid) BrandGreen.copy(alpha = 0.12f) else RedCancel.copy(alpha = 0.08f)
                        drawCircle(
                            color = ringColor,
                            radius = 240f * (zoomLevel / 14f),
                            center = center
                        )
                    }

                    // Center Pin & Reticle
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = (-24).dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (serviceValidation.isValid) BrandGreenDark else RedCancel,
                                shadowElevation = 6.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (mode == MapPickerMode.PICKUP) Icons.Default.TripOrigin else Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = if (mode == MapPickerMode.PICKUP) "PICKUP PIN" else "DROP PIN",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Pin",
                                tint = if (serviceValidation.isValid) BrandGreen else RedCancel,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                }

                // Top Search Bar inside map
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = BrandGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            TextField(
                                value = searchQuery,
                                onValueChange = { query ->
                                    searchQuery = query
                                    if (query.isNotBlank()) {
                                        isSearching = true
                                        coroutineScope.launch {
                                            searchResults = LocationHelper.searchLocations(context, query)
                                        }
                                    } else {
                                        isSearching = false
                                    }
                                },
                                placeholder = { Text("Search landmark, village, road, town...", fontSize = 13.sp) },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    isSearching = false
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = SlateGray)
                                }
                            }
                        }
                    }

                    // Live Search Dropdown Suggestions
                    if (isSearching && searchResults.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                searchResults.take(4).forEach { loc ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White)
                                            .padding(10.dp)
                                            .pointerInput(Unit) {
                                                detectTapGestures {
                                                    updateCoordinates(loc.latitude, loc.longitude)
                                                    searchQuery = loc.placeName
                                                    isSearching = false
                                                }
                                            },
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Default.Place, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(18.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = loc.placeName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(text = loc.formattedAddress, fontSize = 11.sp, color = SlateGray, maxLines = 1)
                                        }
                                    }
                                    HorizontalDivider(color = LightSlate)
                                }
                            }
                        }
                    }
                }

                // Map Zoom Controls & Locate Me Floating Buttons
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FloatingActionButton(
                        onClick = {
                            if (LocationHelper.hasLocationPermission(context)) {
                                LocationHelper.getCurrentGpsCoordinates(
                                    context = context,
                                    onSuccess = { lat, lng ->
                                        updateCoordinates(lat, lng)
                                    },
                                    onError = {}
                                )
                            }
                        },
                        containerColor = Color.White,
                        contentColor = BrandGreenDark,
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Center on GPS", modifier = Modifier.size(20.dp))
                    }

                    FloatingActionButton(
                        onClick = { zoomLevel = (zoomLevel + 1f).coerceAtMost(18f) },
                        containerColor = Color.White,
                        contentColor = CharcoalDark,
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom In")
                    }

                    FloatingActionButton(
                        onClick = { zoomLevel = (zoomLevel - 1f).coerceAtLeast(10f) },
                        containerColor = Color.White,
                        contentColor = CharcoalDark,
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
                    }
                }

                // Bottom Location Details & Confirmation Sheet
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (mode == MapPickerMode.PICKUP) Icons.Default.TripOrigin else Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = if (mode == MapPickerMode.PICKUP) BrandGreen else RedCancel
                                )
                                Text(
                                    text = if (mode == MapPickerMode.PICKUP) "SELECTED PICKUP" else "SELECTED DROP",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateGray,
                                    letterSpacing = 1.sp
                                )
                            }

                            if (isGeocoding) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = BrandGreen)
                                    Text("Updating...", fontSize = 11.sp, color = SlateGray)
                                }
                            }
                        }

                        // Display Detected Address & Coordinates
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = currentAddressDetails.placeName.ifBlank { "Selected Pin" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = CharcoalDark
                            )
                            Text(
                                text = currentAddressDetails.formattedAddress.ifBlank { "Assam" },
                                fontSize = 12.sp,
                                color = SlateGray,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "GPS: ${String.format(Locale.US, "%.5f, %.5f", currentCenterLat, currentCenterLng)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BrandGreenDark
                            )
                        }

                        // 30 KM Service Area Banner
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (serviceValidation.isValid) BrandGreenLight else RedCancelLight,
                            border = BorderStroke(1.dp, if (serviceValidation.isValid) BrandGreen.copy(alpha = 0.3f) else RedCancel.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (serviceValidation.isValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (serviceValidation.isValid) BrandGreenDark else RedCancel,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = serviceValidation.message,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (serviceValidation.isValid) BrandGreenDark else RedCancel
                                )
                            }
                        }

                        // Confirm Location Button
                        Button(
                            onClick = {
                                onLocationConfirmed(
                                    currentAddressDetails.copy(
                                        latitude = currentCenterLat,
                                        longitude = currentCenterLng
                                    )
                                )
                            },
                            enabled = serviceValidation.isValid,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandGreen,
                                disabledContainerColor = LightSlate
                            )
                        ) {
                            Text(
                                text = if (mode == MapPickerMode.PICKUP) "CONFIRM PICKUP LOCATION" else "CONFIRM DROP LOCATION",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (serviceValidation.isValid) Color.White else SlateGray
                            )
                        }
                    }
                }
            }
        }
    }
}
