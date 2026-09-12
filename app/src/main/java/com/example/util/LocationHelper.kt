package com.example.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.models.LocationDetails
import com.example.data.models.ServiceAreaValidation
import com.example.data.models.ServiceCentre
import com.example.data.models.ServiceCentres
import com.example.data.repository.RideRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.*

object LocationHelper {

    private const val TAG = "ERide3Location"

    /**
     * Calculate geodesic (Haversine) distance between two coordinate pairs in Kilometers.
     */
    fun calculateGeodesicDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth mean radius in KM
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /**
     * Check if latitude & longitude falls within 30 KM radius of ANY active service centre.
     * Service centres: Tezpur (26.6338, 92.7926), Biswanath Chariali (26.7329, 93.1554), Nagsankar (26.6900, 92.9800).
     * Service-area validation MUST use latitude + longitude, NOT address text.
     */
    fun validateServiceArea(latitude: Double, longitude: Double): ServiceAreaValidation {
        val activeRepoAreas = RideRepository.serviceAreas.value.filter { it.isActive }
        val centresToCheck = if (activeRepoAreas.isNotEmpty()) {
            activeRepoAreas.map { ServiceCentre(it.name, it.latitude, it.longitude, it.radiusKm, it.district) }
        } else {
            ServiceCentres.ALL
        }

        var minDistance = Double.MAX_VALUE
        var nearestCentre = centresToCheck.firstOrNull() ?: ServiceCentres.TEZPUR

        for (centre in centresToCheck) {
            val dist = calculateGeodesicDistanceKm(latitude, longitude, centre.latitude, centre.longitude)
            if (dist < minDistance) {
                minDistance = dist
                nearestCentre = centre
            }
        }

        val roundedDistance = (minDistance * 10.0).roundToInt() / 10.0
        val isValid = minDistance <= nearestCentre.radiusKm // 30.0 km

        val message = if (isValid) {
            "Within ${nearestCentre.name} service coverage ($roundedDistance km from centre)"
        } else {
            "Sorry, this location is currently outside the E-Ride 3 service area ($roundedDistance km from nearest active centre ${nearestCentre.name})."
        }

        return ServiceAreaValidation(
            isValid = isValid,
            nearestCentreName = nearestCentre.name,
            distanceKm = roundedDistance,
            message = message
        )
    }

    /**
     * Calculate road distance and travel time for E-Rickshaw based on coordinates.
     */
    fun calculateRoute(
        pickupLat: Double,
        pickupLng: Double,
        dropLat: Double,
        dropLng: Double
    ): Pair<Double, Int> {
        val geodesic = calculateGeodesicDistanceKm(pickupLat, pickupLng, dropLat, dropLng)
        // Road routing curvature factor for urban/semi-urban Assam roads (~1.25x-1.35x)
        val roadDist = max(0.8, (geodesic * 1.28 * 10.0).roundToInt() / 10.0)
        // E-Rickshaws travel at average 20-25 km/h + 2 mins pickup buffer
        val estimatedMins = max(3, ((roadDist / 22.0) * 60).roundToInt() + 2)
        return Pair(roadDist, estimatedMins)
    }

    fun hasLocationPermission(context: Context): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocation || coarseLocation
    }

    fun isInternetAvailable(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            false
        }
    }

    fun isLocationServiceEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return try {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (_: Exception) {
            false
        }
    }

    fun openLocationSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    /**
     * Get real GPS coordinates from device hardware with high accuracy.
     * Uses device GPS and network location simultaneously.
     * Never serves stale cached location when a fresh fix is available.
     */
    fun getCurrentGpsCoordinates(
        context: Context,
        onSuccess: (Double, Double) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!hasLocationPermission(context)) {
            onError("Location permission is required to detect your current location.")
            return
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager == null) {
            onError("Location service unavailable on this device.")
            return
        }

        if (!isLocationServiceEnabled(context)) {
            onError("Please turn on your device location to use Current Location.")
            return
        }

        try {
            val hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val hasNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            var bestLocation: Location? = null
            var bestAccuracy = Float.MAX_VALUE

            // Check if there is an extremely fresh (< 20 seconds old) high accuracy location
            val currentTime = System.currentTimeMillis()
            if (hasGps) {
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { loc ->
                    val age = currentTime - loc.time
                    if (age < 20_000 && loc.hasAccuracy() && loc.accuracy <= 30f) {
                        onSuccess(loc.latitude, loc.longitude)
                        return
                    } else if (loc.hasAccuracy() && loc.accuracy < bestAccuracy) {
                        bestLocation = loc
                        bestAccuracy = loc.accuracy
                    }
                }
            }
            if (hasNetwork && bestLocation == null) {
                locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)?.let { loc ->
                    val age = currentTime - loc.time
                    if (age < 15_000 && loc.hasAccuracy() && loc.accuracy <= 50f) {
                        onSuccess(loc.latitude, loc.longitude)
                        return
                    } else if (loc.hasAccuracy() && loc.accuracy < bestAccuracy) {
                        bestLocation = loc
                        bestAccuracy = loc.accuracy
                    }
                }
            }

            // Fresh high-accuracy location listener
            var isDelivered = false
            val handler = Handler(Looper.getMainLooper())

            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    if (isDelivered) return
                    // If accuracy is high (e.g. <= 45m) or comes from GPS
                    val isAccurate = (location.hasAccuracy() && location.accuracy <= 45f) || location.provider == LocationManager.GPS_PROVIDER
                    if (isAccurate) {
                        isDelivered = true
                        handler.removeCallbacksAndMessages(null)
                        try {
                            locationManager.removeUpdates(this)
                        } catch (_: Exception) {}
                        onSuccess(location.latitude, location.longitude)
                    } else {
                        // Store best interim location
                        if (location.hasAccuracy() && location.accuracy < bestAccuracy) {
                            bestLocation = location
                            bestAccuracy = location.accuracy
                        }
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            // Request updates from GPS and Network simultaneously
            if (hasGps) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    0L,
                    0f,
                    listener,
                    Looper.getMainLooper()
                )
            }
            if (hasNetwork) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    0L,
                    0f,
                    listener,
                    Looper.getMainLooper()
                )
            }

            // Safety timeout: 5.5 seconds max wait for high-accuracy fix
            handler.postDelayed({
                if (!isDelivered) {
                    isDelivered = true
                    try {
                        locationManager.removeUpdates(listener)
                    } catch (_: Exception) {}

                    val fallback = bestLocation
                    if (fallback != null) {
                        onSuccess(fallback.latitude, fallback.longitude)
                    } else {
                        onError("Unable to acquire high accuracy GPS fix. Please ensure location is enabled and try again.")
                    }
                }
            }, 5500L)

        } catch (e: SecurityException) {
            onError("Location permission is required to detect your current location.")
        } catch (e: Exception) {
            onError("Could not obtain location: ${e.localizedMessage}")
        }
    }

    /**
     * Build display address using the most specific available components.
     * Example: Village / Road / Locality, District, Assam, PIN
     */
    private fun buildDisplayAddress(
        featureName: String?,
        village: String?,
        road: String?,
        locality: String?,
        sublocality: String?,
        district: String?,
        state: String?,
        postalCode: String?
    ): String {
        val parts = linkedSetOf<String>()

        // 1. Specific feature name / landmark (if distinct and not a raw number/plus code)
        if (!featureName.isNullOrBlank() &&
            featureName != road &&
            featureName != village &&
            featureName != locality &&
            featureName != district &&
            featureName != state &&
            !featureName.matches(Regex("^[0-9+\\-., ]+$"))) {
            parts.add(featureName.trim())
        }

        // 2. Village / Gaon
        if (!village.isNullOrBlank()) {
            parts.add(village.trim())
        }

        // 3. Sublocality / Locality / Neighborhood / Gali
        if (!sublocality.isNullOrBlank() && sublocality != village) {
            parts.add(sublocality.trim())
        }

        // 4. Road / Thoroughfare
        if (!road.isNullOrBlank()) {
            parts.add(road.trim())
        }

        // 5. Town / Locality / Post Office
        if (!locality.isNullOrBlank() && locality != village && locality != sublocality) {
            parts.add(locality.trim())
        }

        // 6. District (Sonitpur, Biswanath, etc.)
        if (!district.isNullOrBlank()) {
            parts.add(district.trim())
        }

        // 7. State (Assam)
        if (!state.isNullOrBlank()) {
            parts.add(state.trim())
        }

        // 8. Postal Code
        if (!postalCode.isNullOrBlank()) {
            parts.add(postalCode.trim())
        }

        return parts.joinToString(", ")
    }

    /**
     * Reverse Geocode coordinates to detailed human-readable address with Village/Road/Locality.
     * Preserves exact coordinates and applies strict fallback without overriding to city names.
     */
    suspend fun reverseGeocode(
        context: Context,
        latitude: Double,
        longitude: Double
    ): LocationDetails = withContext(Dispatchers.IO) {
        var rawFormattedAddress = ""
        var route: String? = null
        var road: String? = null
        var street: String? = null
        var premise: String? = null
        var subpremise: String? = null
        var village: String? = null
        var locality = ""
        var sublocality: String? = null
        var neighborhood: String? = null
        var postalTown: String? = null
        var town: String? = null
        var district = ""
        var state = "Assam"
        var postalCode = ""
        var country = "India"
        var featureName: String? = null

        // 1. Try Android Geocoder
        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale("en", "IN"))
                val addresses: List<Address>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(latitude, longitude, 1)
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latitude, longitude, 1)
                }

                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    featureName = addr.featureName
                    route = addr.thoroughfare
                    road = addr.thoroughfare
                    street = addr.thoroughfare
                    premise = addr.subThoroughfare
                    sublocality = addr.subLocality
                    neighborhood = addr.subLocality
                    locality = addr.locality ?: ""
                    postalTown = addr.locality
                    town = addr.locality

                    // Extract Village if present
                    if (!addr.subLocality.isNullOrBlank()) {
                        village = addr.subLocality
                    } else if (!addr.premises.isNullOrBlank()) {
                        village = addr.premises
                    } else if (addr.featureName != null && (addr.featureName.contains("gaon", true) || addr.featureName.contains("village", true) || addr.featureName.contains("basti", true) || addr.featureName.contains("chuk", true))) {
                        village = addr.featureName
                    }

                    district = addr.subAdminArea ?: ""
                    state = addr.adminArea ?: "Assam"
                    postalCode = addr.postalCode ?: ""
                    country = addr.countryName ?: "India"

                    rawFormattedAddress = (0..addr.maxAddressLineIndex).mapNotNull { addr.getAddressLine(it) }.joinToString(", ")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Geocoder lookup failed: ${e.message}")
        }

        // 2. Online reverse geocode fallback if village and road are still unknown
        if ((village.isNullOrBlank() || road.isNullOrBlank()) && isInternetAvailable(context)) {
            val online = fetchOnlineReverseGeocode(latitude, longitude)
            if (online != null) {
                if (village.isNullOrBlank()) village = online.village
                if (road.isNullOrBlank()) road = online.road
                if (street.isNullOrBlank()) street = online.street
                if (locality.isBlank()) locality = online.locality
                if (district.isBlank()) district = online.district
                if (state.isBlank()) state = online.state
                if (postalCode.isBlank()) postalCode = online.postalCode
                if (rawFormattedAddress.isBlank()) rawFormattedAddress = online.formattedAddress
            }
        }

        // 3. Check if a nearby landmark/village matches within 1.0 km for localized precision
        val nearestCurated = findNearestCuratedLandmark(latitude, longitude)
        if (nearestCurated != null && calculateGeodesicDistanceKm(latitude, longitude, nearestCurated.latitude, nearestCurated.longitude) <= 1.0) {
            if (village.isNullOrBlank()) village = nearestCurated.village
            if (road.isNullOrBlank()) road = nearestCurated.road
            if (street.isNullOrBlank()) street = nearestCurated.street
            if (locality.isBlank()) locality = nearestCurated.locality
            if (district.isBlank()) district = nearestCurated.district
            if (postalCode.isBlank()) postalCode = nearestCurated.postalCode
            if (featureName.isNullOrBlank()) featureName = nearestCurated.placeName
        }

        // 4. Construct display address from specific available components
        val constructedAddress = buildDisplayAddress(
            featureName = featureName,
            village = village,
            road = road ?: route,
            locality = locality,
            sublocality = sublocality,
            district = district,
            state = state,
            postalCode = postalCode
        )

        val finalFormattedAddress = when {
            constructedAddress.isNotBlank() -> constructedAddress
            rawFormattedAddress.isNotBlank() -> rawFormattedAddress
            else -> ""
        }

        // 5. Determine placeName and formattedAddress according to display & fallback rules
        val placeName: String
        val formattedAddress: String

        val hasSpecificComponent = !village.isNullOrBlank() ||
                                   !road.isNullOrBlank() ||
                                   !route.isNullOrBlank() ||
                                   !sublocality.isNullOrBlank() ||
                                   (!featureName.isNullOrBlank() && !featureName.matches(Regex("^[0-9+\\-., ]+$")))

        if (hasSpecificComponent && finalFormattedAddress.isNotBlank()) {
            placeName = when {
                !village.isNullOrBlank() -> village
                !road.isNullOrBlank() -> road
                !route.isNullOrBlank() -> route
                !sublocality.isNullOrBlank() -> sublocality
                !featureName.isNullOrBlank() -> featureName
                locality.isNotBlank() -> locality
                else -> "Selected Location"
            }
            formattedAddress = finalFormattedAddress
        } else {
            // FALLBACK RULE:
            // If reverse geocoding returns NO village/road/locality name:
            // DO NOT replace the location with: Tezpur / Biswanath Chariali / Nagsankar.
            // Instead show: "Selected Map Location" and the exact Latitude / Longitude.
            placeName = "Selected Map Location"
            formattedAddress = "Lat: ${String.format(Locale.US, "%.6f", latitude)}, Lng: ${String.format(Locale.US, "%.6f", longitude)}"
        }

        // 6. Calculate service distances
        val distTezpur = calculateGeodesicDistanceKm(latitude, longitude, 26.6338, 92.7926)
        val distBiswanath = calculateGeodesicDistanceKm(latitude, longitude, 26.7329, 93.1554)
        val distNagsankar = calculateGeodesicDistanceKm(latitude, longitude, 26.6900, 92.9800)
        val validation = validateServiceArea(latitude, longitude)

        // 7. DEBUG LOGGING
        Log.d(TAG, """
            === E-RIDE 3 LOCATION SELECTION DEBUG ===
            GPS:
            latitude: $latitude
            longitude: $longitude

            Geocoding response:
            formatted_address: $formattedAddress

            Address components:
            route: $route
            locality: $locality
            village: $village
            postal town: $postalTown
            district: $district
            state: $state
            postal code: $postalCode

            Service distances:
            Tezpur = ${String.format(Locale.US, "%.2f", distTezpur)} KM
            Biswanath = ${String.format(Locale.US, "%.2f", distBiswanath)} KM
            Nagsankar = ${String.format(Locale.US, "%.2f", distNagsankar)} KM

            Final result:
            ${if (validation.isValid) "INSIDE" else "OUTSIDE"}
            =========================================
        """.trimIndent())

        LocationDetails(
            placeName = placeName,
            formattedAddress = formattedAddress,
            latitude = latitude,
            longitude = longitude,
            route = route,
            road = road,
            street = street,
            premise = premise,
            subpremise = subpremise,
            village = village,
            locality = locality,
            sublocality = sublocality,
            neighborhood = neighborhood,
            postalTown = postalTown,
            town = town,
            district = district,
            state = state,
            postalCode = postalCode,
            country = country,
            placeId = "loc_${(latitude * 100000).roundToInt()}_${(longitude * 100000).roundToInt()}"
        )
    }

    private fun fetchOnlineReverseGeocode(lat: Double, lng: Double): LocationDetails? {
        return try {
            val url = URL("https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lng&zoom=18&addressdetails=1")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3000
                readTimeout = 3000
                setRequestProperty("User-Agent", "ERide3-AssamApp/1.0")
            }
            if (conn.responseCode == 200) {
                val json = conn.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(json)
                val displayName = obj.optString("display_name", "")
                val addressObj = obj.optJSONObject("address")

                var road = addressObj?.optString("road")?.ifBlank { null }
                if (road.isNullOrBlank()) road = addressObj?.optString("pedestrian")?.ifBlank { null }
                if (road.isNullOrBlank()) road = addressObj?.optString("highway")?.ifBlank { null }

                var village = addressObj?.optString("village")?.ifBlank { null }
                if (village.isNullOrBlank()) village = addressObj?.optString("hamlet")?.ifBlank { null }
                if (village.isNullOrBlank()) village = addressObj?.optString("suburb")?.ifBlank { null }
                if (village.isNullOrBlank()) village = addressObj?.optString("neighbourhood")?.ifBlank { null }

                val town = addressObj?.optString("town")?.ifBlank { null }
                    ?: addressObj?.optString("city")?.ifBlank { null }
                    ?: addressObj?.optString("municipality")?.ifBlank { null }

                val district = addressObj?.optString("state_district")?.ifBlank { null }
                    ?: addressObj?.optString("county")?.ifBlank { null }
                    ?: ""

                val state = addressObj?.optString("state", "Assam") ?: "Assam"
                val postalCode = addressObj?.optString("postcode", "")

                val specific = village ?: road ?: town ?: ""
                val placeName = if (specific.isNotBlank()) specific else "Selected Map Location"

                LocationDetails(
                    placeName = placeName,
                    formattedAddress = displayName.ifBlank { "$placeName, $district, $state" },
                    latitude = lat,
                    longitude = lng,
                    road = road,
                    street = road,
                    village = village,
                    locality = town ?: village ?: "",
                    district = district,
                    state = state,
                    postalCode = postalCode ?: ""
                )
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchOnlineSearchLocations(query: String): List<LocationDetails> {
        val list = mutableListOf<LocationDetails>()
        try {
            val encoded = URLEncoder.encode("$query Assam", "UTF-8")
            val url = URL("https://nominatim.openstreetmap.org/search?q=$encoded&format=json&addressdetails=1&countrycodes=in&limit=8")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3000
                readTimeout = 3000
                setRequestProperty("User-Agent", "ERide3-AssamApp/1.0")
            }
            if (conn.responseCode == 200) {
                val json = conn.inputStream.bufferedReader().use { it.readText() }
                val arr = JSONArray(json)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val lat = obj.optDouble("lat", 0.0)
                    val lon = obj.optDouble("lon", 0.0)
                    val displayName = obj.optString("display_name", "")
                    val name = obj.optString("name", "")
                    val addressObj = obj.optJSONObject("address")

                    val road = addressObj?.optString("road")?.ifBlank { null }
                    val village = addressObj?.optString("village")?.ifBlank { null }
                        ?: addressObj?.optString("hamlet")?.ifBlank { null }
                        ?: addressObj?.optString("suburb")?.ifBlank { null }
                    val locality = addressObj?.optString("town")?.ifBlank { null }
                        ?: addressObj?.optString("city")?.ifBlank { null }
                        ?: village ?: ""
                    val district = addressObj?.optString("state_district")?.ifBlank { null }
                        ?: addressObj?.optString("county")?.ifBlank { null }
                        ?: ""
                    val state = addressObj?.optString("state", "Assam") ?: "Assam"
                    val postalCode = addressObj?.optString("postcode", "")

                    val pName = if (name.isNotBlank()) name else (village ?: road ?: locality.ifBlank { query })

                    list.add(
                        LocationDetails(
                            placeName = pName,
                            formattedAddress = displayName,
                            latitude = lat,
                            longitude = lon,
                            road = road,
                            village = village,
                            locality = locality,
                            district = district,
                            state = state,
                            postalCode = postalCode ?: ""
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        return list
    }

    /**
     * Search locations across towns, villages, roads, markets, bus stands, schools, hospitals, landmarks.
     * Prioritizes active service areas (Tezpur, Biswanath Chariali, Nagsankar).
     * Autocomplete works for Village, Gaon, Gali, Road, Ward, Locality, Market, School, Hospital, Bus Stand, Temple, Railway Station.
     */
    suspend fun searchLocations(
        context: Context,
        query: String
    ): List<LocationDetails> = withContext(Dispatchers.IO) {
        val results = mutableListOf<LocationDetails>()
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            return@withContext defaultServiceLocations
        }

        // 1. Curated Landmark/Village/Road/Market match across Sonitpur & Biswanath
        val curatedMatches = curatedLocations.filter { loc ->
            loc.placeName.contains(trimmed, ignoreCase = true) ||
            loc.formattedAddress.contains(trimmed, ignoreCase = true) ||
            loc.locality.contains(trimmed, ignoreCase = true) ||
            loc.district.contains(trimmed, ignoreCase = true) ||
            (loc.village != null && loc.village.contains(trimmed, ignoreCase = true)) ||
            (loc.road != null && loc.road.contains(trimmed, ignoreCase = true)) ||
            (loc.street != null && loc.street.contains(trimmed, ignoreCase = true))
        }
        results.addAll(curatedMatches)

        // 2. Online Places / Autocomplete Search (Nominatim) when network is available
        if (isInternetAvailable(context)) {
            val onlineMatches = fetchOnlineSearchLocations(trimmed)
            for (item in onlineMatches) {
                if (results.none { abs(it.latitude - item.latitude) < 0.0008 && abs(it.longitude - item.longitude) < 0.0008 }) {
                    results.add(item)
                }
            }
        }

        // 3. Android Geocoder search (Sonitpur, Biswanath, Assam)
        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale("en", "IN"))
                val searchQueries = listOf(
                    "$trimmed, Sonitpur, Assam",
                    "$trimmed, Biswanath, Assam",
                    "$trimmed, Assam, India",
                    trimmed
                )

                for (q in searchQueries) {
                    val addresses: List<Address>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        geocoder.getFromLocationName(q, 8)
                    } else {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocationName(q, 8)
                    }

                    addresses?.forEach { addr ->
                        val pName = addr.featureName ?: addr.thoroughfare ?: addr.subLocality ?: addr.locality ?: trimmed
                        val fAddr = buildDisplayAddress(
                            featureName = addr.featureName,
                            village = addr.subLocality,
                            road = addr.thoroughfare,
                            locality = addr.locality,
                            sublocality = addr.subLocality,
                            district = addr.subAdminArea,
                            state = addr.adminArea,
                            postalCode = addr.postalCode
                        ).ifBlank {
                            (0..addr.maxAddressLineIndex).mapNotNull { addr.getAddressLine(it) }.joinToString(", ")
                        }

                        val item = LocationDetails(
                            placeName = pName,
                            formattedAddress = fAddr.ifBlank { "$pName, ${addr.locality ?: "Assam"}" },
                            latitude = addr.latitude,
                            longitude = addr.longitude,
                            route = addr.thoroughfare,
                            road = addr.thoroughfare ?: addr.featureName,
                            street = addr.thoroughfare,
                            premise = addr.subThoroughfare,
                            village = addr.subLocality ?: addr.subAdminArea,
                            locality = addr.locality ?: addr.subLocality ?: "",
                            sublocality = addr.subLocality,
                            neighborhood = addr.subLocality,
                            postalTown = addr.locality,
                            town = addr.locality,
                            district = addr.subAdminArea ?: "",
                            state = addr.adminArea ?: "Assam",
                            postalCode = addr.postalCode ?: "",
                            country = addr.countryName ?: "India",
                            placeId = "geo_${(addr.latitude * 100000).roundToInt()}_${(addr.longitude * 100000).roundToInt()}"
                        )
                        if (results.none { abs(it.latitude - item.latitude) < 0.0005 && abs(it.longitude - item.longitude) < 0.0005 }) {
                            results.add(item)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Geocoder search failed: ${e.message}")
        }

        // 4. Fuzzy search fallback across tokens in curated collection
        if (results.isEmpty()) {
            curatedLocations.filter { loc ->
                trimmed.split(" ").any { word ->
                    word.length >= 3 && (loc.formattedAddress.contains(word, ignoreCase = true) || loc.placeName.contains(word, ignoreCase = true))
                }
            }.let { results.addAll(it) }
        }

        // 5. Rank results prioritizing active service areas (Tezpur, Biswanath Chariali, Nagsankar)
        results.sortedBy { loc ->
            val dTezpur = calculateGeodesicDistanceKm(loc.latitude, loc.longitude, 26.6338, 92.7926)
            val dBiswanath = calculateGeodesicDistanceKm(loc.latitude, loc.longitude, 26.7329, 93.1554)
            val dNagsankar = calculateGeodesicDistanceKm(loc.latitude, loc.longitude, 26.6900, 92.9800)
            minOf(dTezpur, dBiswanath, dNagsankar)
        }
    }

    private fun findNearestCuratedLandmark(lat: Double, lng: Double): LocationDetails? {
        return curatedLocations.minByOrNull {
            calculateGeodesicDistanceKm(lat, lng, it.latitude, it.longitude)
        }
    }

    // Default primary locations in official service zones
    val defaultServiceLocations = listOf(
        // TEZPUR (Centre: 26.6338, 92.7926)
        LocationDetails(
            placeName = "ASTC Bus Stand, Tezpur",
            formattedAddress = "Near Court Chariali, ASTC Central Station, Tezpur, Sonitpur, Assam - 784001",
            latitude = 26.6322,
            longitude = 92.7930,
            road = "Court Chariali Road",
            locality = "Court Chariali",
            district = "Sonitpur",
            postalCode = "784001"
        ),
        LocationDetails(
            placeName = "Mission Chariali, Tezpur",
            formattedAddress = "NH-15 Junction, Mission Chariali, Tezpur, Sonitpur, Assam - 784001",
            latitude = 26.6520,
            longitude = 92.7915,
            road = "NH-15",
            locality = "Mission Chariali",
            district = "Sonitpur",
            postalCode = "784001"
        ),
        LocationDetails(
            placeName = "Mahabhairab Temple, Tezpur",
            formattedAddress = "Mahabhairab Road, Tezpur, Sonitpur, Assam - 784001",
            latitude = 26.6395,
            longitude = 92.7975,
            road = "Mahabhairab Road",
            locality = "Mahabhairab",
            district = "Sonitpur",
            postalCode = "784001"
        ),
        LocationDetails(
            placeName = "Agnigarh Hill, Tezpur",
            formattedAddress = "Brahmaputra View Point, Park Road, Tezpur, Sonitpur, Assam - 784001",
            latitude = 26.6234,
            longitude = 92.8021,
            road = "Park Road",
            locality = "Agnigarh",
            district = "Sonitpur",
            postalCode = "784001"
        ),
        LocationDetails(
            placeName = "Tezpur Central University Gate",
            formattedAddress = "Napaam, Tezpur University Main Gate, Sonitpur, Assam - 784028",
            latitude = 26.7008,
            longitude = 92.8300,
            village = "Napaam Gaon",
            road = "University Road",
            locality = "Napaam",
            district = "Sonitpur",
            postalCode = "784028"
        ),
        LocationDetails(
            placeName = "Dekargaon Railway Station",
            formattedAddress = "Dekargaon Stn Road, Tezpur, Sonitpur, Assam - 784501",
            latitude = 26.6710,
            longitude = 92.7750,
            village = "Dekargaon",
            road = "Station Road",
            locality = "Dekargaon",
            district = "Sonitpur",
            postalCode = "784501"
        ),

        // BISWANATH CHARIALI (Centre: 26.7329, 93.1554)
        LocationDetails(
            placeName = "Biswanath Chariali Clock Tower",
            formattedAddress = "Main Commercial Market, Clock Tower, Biswanath Chariali, Assam - 784176",
            latitude = 26.7329,
            longitude = 93.1554,
            road = "Clock Tower Chowk",
            locality = "Chariali Market",
            district = "Biswanath",
            postalCode = "784176"
        ),
        LocationDetails(
            placeName = "Biswanath Ghat (Gupta Kashi)",
            formattedAddress = "Ghat Road, Brahmaputra Riverfront, Biswanath Ghat, Biswanath, Assam - 784177",
            latitude = 26.6660,
            longitude = 93.1610,
            village = "Biswanath Ghat",
            road = "Ghat Road",
            locality = "Biswanath Ghat",
            district = "Biswanath",
            postalCode = "784177"
        ),
        LocationDetails(
            placeName = "Biswanath Chariali Railway Station",
            formattedAddress = "Station Road, Biswanath Chariali, Biswanath, Assam - 784176",
            latitude = 26.7410,
            longitude = 93.1500,
            road = "Station Road",
            locality = "Station Road",
            district = "Biswanath",
            postalCode = "784176"
        ),
        LocationDetails(
            placeName = "Biswanath College / Civil Hospital",
            formattedAddress = "College Road, Near Civil Hospital, Biswanath Chariali, Assam - 784176",
            latitude = 26.7360,
            longitude = 93.1620,
            road = "College Road",
            locality = "College Road",
            district = "Biswanath",
            postalCode = "784176"
        ),

        // NAGSANKAR & SURROUNDINGS (Centre: 26.6900, 92.9800)
        LocationDetails(
            placeName = "Nagsankar Mandir (Historical Pond)",
            formattedAddress = "NH-15, Nagsankar Temple Complex, Sonitpur/Biswanath, Assam - 784189",
            latitude = 26.6900,
            longitude = 92.9800,
            village = "Nagsankar Gaon",
            road = "NH-15",
            locality = "Nagsankar",
            district = "Sonitpur/Biswanath",
            postalCode = "784189"
        ),
        LocationDetails(
            placeName = "Jamugurihat Centre / Baresahariya Bhaona Ground",
            formattedAddress = "Main Chowk, Jamugurihat, Sonitpur, Assam - 784180",
            latitude = 26.7190,
            longitude = 92.9370,
            village = "Jamugurihat",
            road = "Main Chowk",
            locality = "Jamugurihat",
            district = "Sonitpur",
            postalCode = "784180"
        ),
        LocationDetails(
            placeName = "Sootea Daily Bazaar",
            formattedAddress = "NH-15, Sootea Town, Biswanath, Assam - 784175",
            latitude = 26.7210,
            longitude = 93.0450,
            village = "Sootea Town",
            road = "NH-15",
            locality = "Sootea",
            district = "Biswanath",
            postalCode = "784175"
        )
    )

    // Extensive curated catalog covering towns, villages, gaons, markets, tea estates, roads within 30km radius
    val curatedLocations = defaultServiceLocations + listOf(
        // TEZPUR REGION (30 KM RADIUS) - Villages, Gaons, Roads & Localities
        LocationDetails(
            placeName = "Chowk Bazaar, Tezpur",
            formattedAddress = "Chowk Bazaar Commercial Hub, Tezpur, Sonitpur, Assam - 784001",
            latitude = 26.6280,
            longitude = 92.7940,
            road = "Chowk Bazaar Road",
            locality = "Chowk Bazaar",
            district = "Sonitpur",
            postalCode = "784001"
        ),
        LocationDetails(
            placeName = "TMCH Bihaguri (Tezpur Medical College)",
            formattedAddress = "Bihaguri Hospital Road, TMCH Campus, Sonitpur, Assam - 784153",
            latitude = 26.6430,
            longitude = 92.6840,
            village = "Bihaguri Gaon",
            road = "Hospital Road",
            locality = "Bihaguri",
            district = "Sonitpur",
            postalCode = "784153"
        ),
        LocationDetails(
            placeName = "Bihaguri Tiniali, NH-15",
            formattedAddress = "NH-15 Junction, Bihaguri, Sonitpur, Assam - 784153",
            latitude = 26.6490,
            longitude = 92.6950,
            village = "Bihaguri",
            road = "NH-15",
            locality = "Bihaguri",
            district = "Sonitpur",
            postalCode = "784153"
        ),
        LocationDetails(
            placeName = "Porowa Chariali Junction",
            formattedAddress = "Porowa Chariali, Tezpur-Balipara Road, Sonitpur, Assam - 784001",
            latitude = 26.6610,
            longitude = 92.7880,
            road = "Tezpur-Balipara Road",
            locality = "Porowa",
            district = "Sonitpur",
            postalCode = "784001"
        ),
        LocationDetails(
            placeName = "Tribeni Chariali, Tezpur",
            formattedAddress = "Tribeni Chowk, Mahabhairab Area, Tezpur, Sonitpur, Assam - 784001",
            latitude = 26.6480,
            longitude = 92.7980,
            road = "Tribeni Road",
            locality = "Mahabhairab",
            district = "Sonitpur",
            postalCode = "784001"
        ),
        LocationDetails(
            placeName = "Dolabari Junction / Bypass",
            formattedAddress = "Dolabari NH-15 Bypass, Tezpur, Sonitpur, Assam - 784027",
            latitude = 26.6690,
            longitude = 92.8220,
            road = "NH-15 Bypass",
            locality = "Dolabari",
            district = "Sonitpur",
            postalCode = "784027"
        ),
        LocationDetails(
            placeName = "Bindukuri Market & Railway Crossing",
            formattedAddress = "Bindukuri Tea Estate Road, Sonitpur, Assam - 784502",
            latitude = 26.7210,
            longitude = 92.7620,
            village = "Bindukuri Gaon",
            road = "Tea Estate Road",
            locality = "Bindukuri",
            district = "Sonitpur",
            postalCode = "784502"
        ),
        LocationDetails(
            placeName = "Panchmile Tiniali, Tezpur",
            formattedAddress = "Tezpur-Balipara Road, Panchmile, Sonitpur, Assam - 784025",
            latitude = 26.6850,
            longitude = 92.7850,
            village = "Panchmile",
            road = "Tezpur-Balipara Road",
            locality = "Panchmile",
            district = "Sonitpur",
            postalCode = "784025"
        ),
        LocationDetails(
            placeName = "Haleswar Temple & Village",
            formattedAddress = "Haleswar Mandir Road, Sonitpur, Assam - 784027",
            latitude = 26.6780,
            longitude = 92.8050,
            village = "Haleswar Gaon",
            road = "Mandir Road",
            locality = "Haleswar",
            district = "Sonitpur",
            postalCode = "784027"
        ),
        LocationDetails(
            placeName = "Nikamul Satra, Mazgaon",
            formattedAddress = "Nikamul Satra Road, Mazgaon, Tezpur, Sonitpur, Assam - 784001",
            latitude = 26.6490,
            longitude = 92.8080,
            village = "Mazgaon",
            road = "Satra Road",
            locality = "Nikamul",
            district = "Sonitpur",
            postalCode = "784001"
        ),
        LocationDetails(
            placeName = "Da-Parbatia Ancient Temple",
            formattedAddress = "Da-Parbatia Historical Site, Tezpur, Sonitpur, Assam - 784001",
            latitude = 26.6180,
            longitude = 92.7720,
            village = "Da-Parbatia Gaon",
            road = "Da-Parbatia Road",
            locality = "Da-Parbatia",
            district = "Sonitpur",
            postalCode = "784001"
        ),
        LocationDetails(
            placeName = "Ketekibari Bazaar & Village",
            formattedAddress = "Ketekibari, Mission Chariali West, Tezpur, Sonitpur, Assam - 784001",
            latitude = 26.6450,
            longitude = 92.7820,
            village = "Ketekibari Gaon",
            road = "Ketekibari Road",
            locality = "Ketekibari",
            district = "Sonitpur",
            postalCode = "784001"
        ),
        LocationDetails(
            placeName = "Goraimari Gaon, Sonitpur",
            formattedAddress = "Goraimari Village Road, Sonitpur, Assam - 784153",
            latitude = 26.6320,
            longitude = 92.7420,
            village = "Goraimari Gaon",
            road = "Village Road",
            locality = "Goraimari",
            district = "Sonitpur",
            postalCode = "784153"
        ),
        LocationDetails(
            placeName = "Thelamara Bazaar & Police Station",
            formattedAddress = "NH-15, Thelamara, Sonitpur, Assam - 784149",
            latitude = 26.6380,
            longitude = 92.6250,
            village = "Thelamara Gaon",
            road = "NH-15",
            locality = "Thelamara",
            district = "Sonitpur",
            postalCode = "784149"
        ),
        LocationDetails(
            placeName = "Dhekiajuli Town ASTC Stand",
            formattedAddress = "ASTC Station, Dhekiajuli Town, Sonitpur, Assam - 784110",
            latitude = 26.7010,
            longitude = 92.5020,
            village = "Dhekiajuli Town",
            road = "Main Road",
            locality = "Dhekiajuli",
            district = "Sonitpur",
            postalCode = "784110"
        ),
        LocationDetails(
            placeName = "Balipara Chariali Market",
            formattedAddress = "NH-15 & NH-229 Junction, Balipara, Sonitpur, Assam - 784102",
            latitude = 26.8200,
            longitude = 92.7750,
            village = "Balipara",
            road = "NH-15 Junction",
            locality = "Balipara",
            district = "Sonitpur",
            postalCode = "784102"
        ),
        LocationDetails(
            placeName = "Rangapara Town Railway Station",
            formattedAddress = "Station Road, Rangapara, Sonitpur, Assam - 784505",
            latitude = 26.8150,
            longitude = 92.6850,
            road = "Station Road",
            locality = "Rangapara",
            district = "Sonitpur",
            postalCode = "784505"
        ),
        LocationDetails(
            placeName = "Mansiri Tea Estate & Village",
            formattedAddress = "Mansiri TE Road, Balipara, Sonitpur, Assam - 784102",
            latitude = 26.8350,
            longitude = 92.7210,
            village = "Mansiri Gaon",
            road = "TE Road",
            locality = "Mansiri",
            district = "Sonitpur",
            postalCode = "784102"
        ),
        LocationDetails(
            placeName = "Lokra Army Cantonment & Gate",
            formattedAddress = "Lokra Road, Sonitpur, Assam - 784102",
            latitude = 26.8520,
            longitude = 92.7680,
            road = "Lokra Road",
            locality = "Lokra",
            district = "Sonitpur",
            postalCode = "784102"
        ),
        LocationDetails(
            placeName = "Charduar Tiniali",
            formattedAddress = "Charduar Bazaar, Sonitpur, Assam - 784501",
            latitude = 26.8720,
            longitude = 92.7980,
            village = "Charduar Gaon",
            road = "Bazaar Road",
            locality = "Charduar",
            district = "Sonitpur",
            postalCode = "784501"
        ),
        LocationDetails(
            placeName = "Kolia Bhomora Bridge Point (North End)",
            formattedAddress = "Kolia Bhomora Setu Approach, Tezpur, Sonitpur, Assam - 784027",
            latitude = 26.6020,
            longitude = 92.8600,
            road = "Setu Approach Road",
            locality = "Bhomoraguri",
            district = "Sonitpur",
            postalCode = "784027"
        ),
        LocationDetails(
            placeName = "Silghat Steamer Ghat / Riverfront",
            formattedAddress = "Silghat River Road, Sonitpur/Nagaon Border, Assam - 782143",
            latitude = 26.6090,
            longitude = 92.9300,
            village = "Silghat",
            road = "River Road",
            locality = "Silghat",
            district = "Sonitpur",
            postalCode = "782143"
        ),
        LocationDetails(
            placeName = "Jahajghat Inland Waterway Terminal",
            formattedAddress = "Jahajghat Road, Tezpur, Sonitpur, Assam - 784001",
            latitude = 26.6210,
            longitude = 92.8050,
            road = "Jahajghat Road",
            locality = "Jahajghat",
            district = "Sonitpur",
            postalCode = "784001"
        ),
        LocationDetails(
            placeName = "Gotlong Gaon & Tea Garden",
            formattedAddress = "Gotlong TE Road, Tezpur, Sonitpur, Assam - 784001",
            latitude = 26.6150,
            longitude = 92.8240,
            village = "Gotlong Gaon",
            road = "TE Road",
            locality = "Gotlong",
            district = "Sonitpur",
            postalCode = "784001"
        ),
        LocationDetails(
            placeName = "Darrang College, Tezpur",
            formattedAddress = "Mahabhairab Road, Darrang College Campus, Tezpur, Sonitpur - 784001",
            latitude = 26.6370,
            longitude = 92.7950,
            road = "Mahabhairab Road",
            locality = "Darrang College",
            district = "Sonitpur",
            postalCode = "784001"
        ),
        LocationDetails(
            placeName = "Kanaklata Civil Hospital, Tezpur",
            formattedAddress = "Hospital Road, Civil Lines, Tezpur, Sonitpur, Assam - 784001",
            latitude = 26.6310,
            longitude = 92.7960,
            road = "Hospital Road",
            locality = "Civil Lines",
            district = "Sonitpur",
            postalCode = "784001"
        ),

        // BISWANATH CHARIALI REGION (30 KM RADIUS) - Villages, Gaons, Roads & Localities
        LocationDetails(
            placeName = "Sakomatha Gaon & Temple",
            formattedAddress = "Sakomatha Road, Biswanath Chariali, Assam - 784176",
            latitude = 26.7480,
            longitude = 93.1420,
            village = "Sakomatha Gaon",
            road = "Sakomatha Road",
            locality = "Sakomatha",
            district = "Biswanath",
            postalCode = "784176"
        ),
        LocationDetails(
            placeName = "Pavoi Tea Estate & Village",
            formattedAddress = "Pavoi TE Road, Biswanath, Assam - 784174",
            latitude = 26.7950,
            longitude = 93.1280,
            village = "Pavoi Gaon",
            road = "Pavoi TE Road",
            locality = "Pavoi",
            district = "Biswanath",
            postalCode = "784174"
        ),
        LocationDetails(
            placeName = "Pratapgarh Tea Estate & Market",
            formattedAddress = "Pratapgarh TE Road, Biswanath, Assam - 784176",
            latitude = 26.7820,
            longitude = 93.2050,
            village = "Pratapgarh Gaon",
            road = "TE Road",
            locality = "Pratapgarh",
            district = "Biswanath",
            postalCode = "784176"
        ),
        LocationDetails(
            placeName = "Borgang Daily Market, NH-15",
            formattedAddress = "NH-15, Borgang Commercial Area, Biswanath, Assam - 784167",
            latitude = 26.7910,
            longitude = 93.3850,
            village = "Borgang Gaon",
            road = "NH-15",
            locality = "Borgang",
            district = "Biswanath",
            postalCode = "784167"
        ),
        LocationDetails(
            placeName = "Behali Tiniali & Tea Estate",
            formattedAddress = "NH-15, Behali Tea Estate Junction, Biswanath, Assam - 784166",
            latitude = 26.8120,
            longitude = 93.3020,
            village = "Behali Gaon",
            road = "NH-15 Junction",
            locality = "Behali",
            district = "Biswanath",
            postalCode = "784166"
        ),
        LocationDetails(
            placeName = "Bedeti Tiniali & Center",
            formattedAddress = "Bedeti Road, Biswanath, Assam - 784179",
            latitude = 26.8040,
            longitude = 93.2510,
            village = "Bedeti Gaon",
            road = "Bedeti Road",
            locality = "Bedeti",
            district = "Biswanath",
            postalCode = "784179"
        ),
        LocationDetails(
            placeName = "Baghmara Village & DFO Office",
            formattedAddress = "Baghmara Gaon Road, Biswanath, Assam - 784176",
            latitude = 26.7110,
            longitude = 93.1890,
            village = "Baghmara Gaon",
            road = "Gaon Road",
            locality = "Baghmara",
            district = "Biswanath",
            postalCode = "784176"
        ),
        LocationDetails(
            placeName = "Ginseria Village & Tea Estate",
            formattedAddress = "Ginseria TE Road, Biswanath, Assam - 784176",
            latitude = 26.7720,
            longitude = 93.1150,
            village = "Ginseria Gaon",
            road = "TE Road",
            locality = "Ginseria",
            district = "Biswanath",
            postalCode = "784176"
        ),
        LocationDetails(
            placeName = "Monabarie Tea Estate",
            formattedAddress = "Monabarie Garden Road, Biswanath, Assam - 784176",
            latitude = 26.7620,
            longitude = 93.2380,
            village = "Monabarie Gaon",
            road = "Garden Road",
            locality = "Monabarie",
            district = "Biswanath",
            postalCode = "784176"
        ),
        LocationDetails(
            placeName = "Diplonga Tea Estate & Gaon",
            formattedAddress = "Diplonga Road, Biswanath, Assam - 784176",
            latitude = 26.7210,
            longitude = 93.2200,
            village = "Diplonga Gaon",
            road = "Diplonga Road",
            locality = "Diplonga",
            district = "Biswanath",
            postalCode = "784176"
        ),
        LocationDetails(
            placeName = "Bormukholi Gaon",
            formattedAddress = "Bormukholi Village Road, Biswanath, Assam - 784176",
            latitude = 26.7020,
            longitude = 93.1380,
            village = "Bormukholi Gaon",
            road = "Village Road",
            locality = "Bormukholi",
            district = "Biswanath",
            postalCode = "784176"
        ),
        LocationDetails(
            placeName = "Kochigaon / Sub-divisional Hospital",
            formattedAddress = "Hospital Road, Kochigaon, Biswanath Chariali, Assam - 784176",
            latitude = 26.7380,
            longitude = 93.1720,
            village = "Kochigaon",
            road = "Hospital Road",
            locality = "Kochigaon",
            district = "Biswanath",
            postalCode = "784176"
        ),
        LocationDetails(
            placeName = "Lehugaon Market & Road",
            formattedAddress = "Lehugaon Chowk, Biswanath Chariali, Assam - 784176",
            latitude = 26.7550,
            longitude = 93.1650,
            village = "Lehugaon",
            road = "Chowk Road",
            locality = "Lehugaon",
            district = "Biswanath",
            postalCode = "784176"
        ),

        // NAGSANKAR & JAMUGURIHAT REGION (30 KM RADIUS) - Villages, Gaons, Roads & Localities
        LocationDetails(
            placeName = "Dhalaibil Center & Bazaar",
            formattedAddress = "NH-15, Dhalaibil Bazaar, Sonitpur, Assam - 784182",
            latitude = 26.7020,
            longitude = 92.8900,
            village = "Dhalaibil Gaon",
            road = "NH-15",
            locality = "Dhalaibil",
            district = "Sonitpur",
            postalCode = "784182"
        ),
        LocationDetails(
            placeName = "Tupia Tiniali, NH-15",
            formattedAddress = "NH-15, Tupia Chowk, Sonitpur, Assam - 784182",
            latitude = 26.7110,
            longitude = 92.9120,
            village = "Tupia Gaon",
            road = "NH-15",
            locality = "Tupia",
            district = "Sonitpur",
            postalCode = "784182"
        ),
        LocationDetails(
            placeName = "Borbhugia Gaon & PHC",
            formattedAddress = "Borbhugia Hospital Road, Sonitpur, Assam - 784180",
            latitude = 26.7280,
            longitude = 92.9650,
            village = "Borbhugia Gaon",
            road = "Hospital Road",
            locality = "Borbhugia",
            district = "Sonitpur",
            postalCode = "784180"
        ),
        LocationDetails(
            placeName = "Puthimari Gaon & Road",
            formattedAddress = "Puthimari Village Road, Sonitpur, Assam - 784189",
            latitude = 26.6750,
            longitude = 92.9520,
            village = "Puthimari Gaon",
            road = "Village Road",
            locality = "Puthimari",
            district = "Sonitpur",
            postalCode = "784189"
        ),
        LocationDetails(
            placeName = "Kherbari Gaon & School",
            formattedAddress = "Kherbari School Road, Sonitpur, Assam - 784189",
            latitude = 26.6620,
            longitude = 92.9980,
            village = "Kherbari Gaon",
            road = "School Road",
            locality = "Kherbari",
            district = "Sonitpur",
            postalCode = "784189"
        ),
        LocationDetails(
            placeName = "Gamiripal Tiniali & Satra",
            formattedAddress = "Gamiripal Satra Road, Jamugurihat, Sonitpur, Assam - 784180",
            latitude = 26.7450,
            longitude = 92.9480,
            village = "Gamiripal Gaon",
            road = "Satra Road",
            locality = "Gamiripal",
            district = "Sonitpur",
            postalCode = "784180"
        ),
        LocationDetails(
            placeName = "Chilabandha Gaon, Jamugurihat",
            formattedAddress = "Chilabandha Village Road, Sonitpur, Assam - 784180",
            latitude = 26.7050,
            longitude = 92.9600,
            village = "Chilabandha Gaon",
            road = "Village Road",
            locality = "Chilabandha",
            district = "Sonitpur",
            postalCode = "784180"
        ),
        LocationDetails(
            placeName = "Dakhin Nagsankar Gaon",
            formattedAddress = "Dakhin Nagsankar Road, Sonitpur/Biswanath, Assam - 784189",
            latitude = 26.6710,
            longitude = 92.9820,
            village = "Dakhin Nagsankar",
            road = "Gaon Road",
            locality = "Nagsankar",
            district = "Sonitpur/Biswanath",
            postalCode = "784189"
        ),
        LocationDetails(
            placeName = "Borpam Gaon & Tea Garden",
            formattedAddress = "Borpam Garden Road, Biswanath, Assam - 784175",
            latitude = 26.7320,
            longitude = 93.0120,
            village = "Borpam Gaon",
            road = "Garden Road",
            locality = "Borpam",
            district = "Biswanath",
            postalCode = "784175"
        ),
        LocationDetails(
            placeName = "Baligaon / Naduar Center",
            formattedAddress = "Naduar Central Road, Jamugurihat, Sonitpur, Assam - 784180",
            latitude = 26.7380,
            longitude = 92.9250,
            village = "Baligaon",
            road = "Naduar Road",
            locality = "Naduar",
            district = "Sonitpur",
            postalCode = "784180"
        ),
        LocationDetails(
            placeName = "Telia Gaon, Sootea",
            formattedAddress = "Telia Gaon Road, Sootea, Biswanath, Assam - 784175",
            latitude = 26.7150,
            longitude = 93.0650,
            village = "Telia Gaon",
            road = "Gaon Road",
            locality = "Sootea",
            district = "Biswanath",
            postalCode = "784175"
        ),
        LocationDetails(
            placeName = "Itakhola Tiniali",
            formattedAddress = "Itakhola Bazaar Junction, Biswanath, Assam - 784175",
            latitude = 26.7580,
            longitude = 93.0720,
            village = "Itakhola Gaon",
            road = "Bazaar Junction",
            locality = "Itakhola",
            district = "Biswanath",
            postalCode = "784175"
        )
    )
}
