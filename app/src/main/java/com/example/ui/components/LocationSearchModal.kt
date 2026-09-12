package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.models.LocationDetails
import com.example.data.repository.RideRepository
import com.example.ui.theme.*
import com.example.util.LocationHelper
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSearchModal(
    title: String,
    initialQuery: String = "",
    onOpenMapPicker: (() -> Unit)? = null,
    onLocationSelected: (LocationDetails) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val recentDestinations by RideRepository.recentDestinations.collectAsState()
    val isInternetAvailable = remember { LocationHelper.isInternetAvailable(context) }

    var searchQuery by remember { mutableStateOf(initialQuery) }
    var searchResults by remember { mutableStateOf<List<LocationDetails>>(LocationHelper.defaultServiceLocations) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Tezpur Hub", "Biswanath Hub", "Nagsankar Hub", "Bus & Rail", "Temples & Landmarks", "Colleges & Hospitals")

    val isDestinationSearch = title.contains("Drop", ignoreCase = true) || title.contains("Destination", ignoreCase = true)

    // Live search execution without filtering out valid geographic results
    fun performSearch(query: String, category: String) {
        isLoading = true
        coroutineScope.launch {
            val baseResults = if (query.isNotBlank()) {
                LocationHelper.searchLocations(context, query)
            } else {
                LocationHelper.defaultServiceLocations
            }

            val filtered = when (category) {
                "Tezpur Hub" -> baseResults.filter { LocationHelper.calculateGeodesicDistanceKm(it.latitude, it.longitude, 26.6338, 92.7926) <= 30.0 }
                "Biswanath Hub" -> baseResults.filter { LocationHelper.calculateGeodesicDistanceKm(it.latitude, it.longitude, 26.7329, 93.1554) <= 30.0 }
                "Nagsankar Hub" -> baseResults.filter { LocationHelper.calculateGeodesicDistanceKm(it.latitude, it.longitude, 26.6900, 92.9800) <= 30.0 }
                "Bus & Rail" -> baseResults.filter { it.placeName.contains("Station", true) || it.placeName.contains("Bus", true) || it.placeName.contains("ASTC", true) }
                "Temples & Landmarks" -> baseResults.filter { it.placeName.contains("Temple", true) || it.placeName.contains("Agnigarh", true) || it.placeName.contains("Ghat", true) || it.placeName.contains("Mandir", true) || it.placeName.contains("Satra", true) }
                "Colleges & Hospitals" -> baseResults.filter { it.placeName.contains("College", true) || it.placeName.contains("University", true) || it.placeName.contains("School", true) || it.placeName.contains("TMCH", true) || it.placeName.contains("Hospital", true) }
                else -> baseResults
            }

            searchResults = filtered
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        performSearch(searchQuery, selectedCategory)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Close", tint = Color.White)
                        }
                    },
                    actions = {
                        if (onOpenMapPicker != null) {
                            TextButton(onClick = {
                                onDismiss()
                                onOpenMapPicker()
                            }) {
                                Icon(Icons.Default.Map, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pick on Map", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Internet connection warning if offline
                if (!isInternetAvailable) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BrandYellow.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, BrandYellowDark.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.WifiOff, contentDescription = null, tint = BrandYellowDark, modifier = Modifier.size(18.dp))
                            Text(
                                "Internet connection is required to search locations.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = CharcoalDark
                            )
                        }
                    }
                }

                // Search Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { query ->
                        searchQuery = query
                        performSearch(query, selectedCategory)
                    },
                    placeholder = {
                        Text(
                            if (isDestinationSearch) "🔍 Search Destination (village, road, market, landmark...)"
                            else "🔍 Search Pickup (village, road, market, landmark...)",
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = BrandGreen) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                performSearch("", selectedCategory)
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = SlateGray)
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandGreen,
                        unfocusedBorderColor = CardBorder,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                // Select on map quick button
                if (onOpenMapPicker != null) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onOpenMapPicker()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = BrandGreenDark,
                            containerColor = Color.White
                        ),
                        border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Place, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pinpoint Village / Road on Map (Center Pin)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                // Category Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = {
                                selectedCategory = cat
                                performSearch(searchQuery, cat)
                            },
                            label = { Text(cat, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandGreenLight,
                                selectedLabelColor = BrandGreenDark
                            )
                        )
                    }
                }

                // Results Counter / Loading Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) "POPULAR & SERVICE HUBS" else "SEARCH RESULTS (${searchResults.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateGray,
                        letterSpacing = 1.sp
                    )

                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = BrandGreen)
                    }
                }

                // Search Results List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Show Recent Destinations when query is empty
                    if (searchQuery.isBlank() && recentDestinations.isNotEmpty()) {
                        item {
                            Text(
                                text = "RECENT DESTINATIONS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateGray,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                            )
                        }

                        items(recentDestinations) { recentLoc ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onLocationSelected(recentLoc) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, CardBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
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
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            tint = BrandGreenDark,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = recentLoc.placeName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = CharcoalDark
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = recentLoc.formattedAddress,
                                            fontSize = 11.sp,
                                            color = SlateGray,
                                            maxLines = 2,
                                            lineHeight = 15.sp
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Select",
                                        tint = SlateGray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "SERVICE LOCATIONS & LOCALITIES",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateGray,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                    }
                    if (searchResults.isEmpty() && !isLoading) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, CardBorder)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.LocationOff, contentDescription = null, tint = SlateGray, modifier = Modifier.size(36.dp))
                                    Text("No matching places found for \"$searchQuery\"", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CharcoalDark)
                                    Text("Use the Map Picker to select any village or road directly with precision.", fontSize = 12.sp, color = SlateGray)
                                    if (onOpenMapPicker != null) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Button(
                                            onClick = {
                                                onDismiss()
                                                onOpenMapPicker()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Open Map Picker")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    items(searchResults) { loc ->
                        val validation = LocationHelper.validateServiceArea(loc.latitude, loc.longitude)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onLocationSelected(loc)
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, if (validation.isValid) CardBorder else RedCancel.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (validation.isValid) BrandGreenLight else RedCancelLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (validation.isValid) Icons.Default.Place else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (validation.isValid) BrandGreenDark else RedCancel,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = loc.placeName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = CharcoalDark
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = loc.formattedAddress,
                                        fontSize = 11.sp,
                                        color = SlateGray,
                                        maxLines = 2,
                                        lineHeight = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "GPS: ${String.format(Locale.US, "%.4f, %.4f", loc.latitude, loc.longitude)}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = SlateGray
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (validation.isValid) BrandGreenLight else RedCancelLight
                                        ) {
                                            Text(
                                                text = if (validation.isValid) "✓ 30km ${validation.nearestCentreName}" else "Outside Service Area",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (validation.isValid) BrandGreenDark else RedCancel,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Select",
                                    tint = SlateGray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
