package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.location.Geocoder
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.components.GlassCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

@SuppressLint("SetJavaScriptEnabled", "MissingPermission")
@Composable
fun MapPickerScreen(
    onLocationSelected: (address: String, lat: Double, lng: Double) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var currentLat by remember { mutableDoubleStateOf(15.369444) } // Default Sana'a / Middle East
    var currentLng by remember { mutableDoubleStateOf(44.191007) }
    var locationAddress by remember { mutableStateOf("جاري جلب العنوان...") }
    var isGeocodingLoading by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    // Helper to get device GPS location
    fun fetchGPSLocation() {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (locationManager != null) {
                val gpsLoc = try { locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) } catch (e: Exception) { null }
                val netLoc = try { locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } catch (e: Exception) { null }
                val bestLoc: Location? = gpsLoc ?: netLoc

                if (bestLoc != null) {
                    currentLat = bestLoc.latitude
                    currentLng = bestLoc.longitude
                    webViewInstance?.loadUrl("javascript:setLocation(${bestLoc.latitude}, ${bestLoc.longitude})")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Permission launcher for GPS
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) {
            fetchGPSLocation()
        }
    }

    fun requestAndFetchLocation() {
        val finePerm = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
        val coarsePerm = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION)
        if (finePerm == android.content.pm.PackageManager.PERMISSION_GRANTED || coarsePerm == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            fetchGPSLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Attempt GPS detection on start
    LaunchedEffect(Unit) {
        requestAndFetchLocation()
    }

    // Reverse Geocode when Lat/Lng changes
    fun performReverseGeocode(lat: Double, lng: Double) {
        coroutineScope.launch(Dispatchers.IO) {
            isGeocodingLoading = true
            var addressFound = ""

            // 1. Try Android Geocoder first
            try {
                if (Geocoder.isPresent()) {
                    val geocoder = Geocoder(context, Locale("ar"))
                    val addresses = geocoder.getFromLocation(lat, lng, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        val feature = addr.featureName ?: ""
                        val thoro = addr.thoroughfare ?: ""
                        val subLoc = addr.subLocality ?: addr.locality ?: addr.adminArea ?: ""
                        addressFound = listOf(feature, thoro, subLoc)
                            .filter { it.isNotBlank() }
                            .distinct()
                            .joinToString("، ")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. Fallback to OpenStreetMap Nominatim API if Android Geocoder empty
            if (addressFound.isBlank()) {
                try {
                    val urlStr = "https://nominatim.openstreetmap.org/reverse?format=json&accept-language=ar&lat=$lat&lon=$lng"
                    val connection = URL(urlStr).openConnection() as HttpURLConnection
                    connection.setRequestProperty("User-Agent", "AzamatFuelApp/1.0")
                    connection.connectTimeout = 4000
                    connection.readTimeout = 4000
                    if (connection.responseCode == 200) {
                        val stream = connection.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(stream)
                        addressFound = json.optString("display_name", "")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            withContext(Dispatchers.Main) {
                locationAddress = if (addressFound.isNotBlank()) {
                    addressFound
                } else {
                    String.format(Locale.US, "موقع محدد (%.5f, %.5f)", lat, lng)
                }
                isGeocodingLoading = false
            }
        }
    }

    // Forward Geocode Search
    fun performSearch(query: String) {
        if (query.isBlank()) return
        coroutineScope.launch(Dispatchers.IO) {
            isSearching = true
            var foundLat: Double? = null
            var foundLng: Double? = null
            var foundName: String? = null

            // 1. Try Photon Komoot Geocoding API (Fast, Arabic support, highly accurate)
            try {
                val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                val urlStr = "https://photon.komoot.io/api/?q=$encodedQuery&lang=ar"
                val connection = URL(urlStr).openConnection() as HttpURLConnection
                connection.setRequestProperty("User-Agent", "AzamatFuelApp/1.0")
                connection.connectTimeout = 4000
                connection.readTimeout = 4000
                if (connection.responseCode == 200) {
                    val stream = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(stream)
                    val features = json.optJSONArray("features")
                    if (features != null && features.length() > 0) {
                        val first = features.getJSONObject(0)
                        val geometry = first.getJSONObject("geometry")
                        val coords = geometry.getJSONArray("coordinates")
                        foundLng = coords.getDouble(0)
                        foundLat = coords.getDouble(1)

                        val props = first.optJSONObject("properties")
                        foundName = props?.optString("name") ?: query
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. Try Nominatim Search API
            if (foundLat == null) {
                try {
                    val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                    val urlStr = "https://nominatim.openstreetmap.org/search?format=json&accept-language=ar&q=$encodedQuery"
                    val connection = URL(urlStr).openConnection() as HttpURLConnection
                    connection.setRequestProperty("User-Agent", "AzamatFuelApp/1.0")
                    connection.connectTimeout = 4000
                    connection.readTimeout = 4000
                    if (connection.responseCode == 200) {
                        val stream = connection.inputStream.bufferedReader().use { it.readText() }
                        val jsonArray = JSONArray(stream)
                        if (jsonArray.length() > 0) {
                            val first = jsonArray.getJSONObject(0)
                            foundLat = first.getDouble("lat")
                            foundLng = first.getDouble("lon")
                            foundName = first.optString("display_name", query)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 3. Fallback to Android Geocoder
            if (foundLat == null) {
                try {
                    if (Geocoder.isPresent()) {
                        val geocoder = Geocoder(context, Locale("ar"))
                        val results = geocoder.getFromLocationName(query, 1)
                        if (!results.isNullOrEmpty()) {
                            val first = results[0]
                            foundLat = first.latitude
                            foundLng = first.longitude
                            foundName = first.featureName ?: first.locality ?: query
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            withContext(Dispatchers.Main) {
                isSearching = false
                if (foundLat != null && foundLng != null) {
                    currentLat = foundLat
                    currentLng = foundLng
                    if (!foundName.isNullOrBlank()) {
                        locationAddress = foundName
                    }
                    webViewInstance?.loadUrl("javascript:setLocation($foundLat, $foundLng)")
                    performReverseGeocode(foundLat, foundLng)
                }
            }
        }
    }

    val htmlContent = remember {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                html, body, #map { height: 100%; width: 100%; background: #e8ecef; font-family: sans-serif; }
                .pin-marker {
                    position: absolute;
                    top: 50%;
                    left: 50%;
                    transform: translate(-50%, -100%);
                    z-index: 1000;
                    pointer-events: none;
                    transition: transform 0.15s ease-out;
                }
                .pin-shadow {
                    position: absolute;
                    top: 50%;
                    left: 50%;
                    width: 14px;
                    height: 7px;
                    background: rgba(0,0,0,0.35);
                    border-radius: 50%;
                    transform: translate(-50%, -50%);
                    z-index: 999;
                    pointer-events: none;
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <div class="pin-shadow"></div>
            <div class="pin-marker">
                <svg width="44" height="54" viewBox="0 0 24 32">
                    <path fill="#E53935" stroke="#FFFFFF" stroke-width="1.5" d="M12 0C5.37 0 0 5.37 0 12c0 9 12 20 12 20s12-11 12-20c0-6.63-5.37-12-12-12zm0 16c-2.21 0-4-1.79-4-4s1.79-4 4-4 4 1.79 4 4-1.79 4-4 4z"/>
                </svg>
            </div>
            <script>
                var map;
                try {
                    map = L.map('map', { 
                        zoomControl: false, 
                        attributionControl: false,
                        fadeAnimation: true,
                        zoomAnimation: true
                    }).setView([15.369444, 44.191007], 14);

                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        maxZoom: 19,
                        subdomains: ['a', 'b', 'c']
                    }).addTo(map);

                    function notifyKotlin() {
                        if (!map) return;
                        var center = map.getCenter();
                        if (window.AndroidMapInterface) {
                            window.AndroidMapInterface.onLocationChanged(center.lat, center.lng);
                        }
                    }

                    map.on('moveend', notifyKotlin);
                    
                    // Click on map directly moves the red marker pin & pans to point
                    map.on('click', function(e) {
                        map.panTo(e.latlng);
                        setTimeout(notifyKotlin, 100);
                    });

                    setTimeout(function() {
                        if (map) map.invalidateSize();
                    }, 300);

                } catch(e) {
                    console.error(e);
                }

                function setLocation(lat, lng) {
                    if (map) {
                        map.setView([lat, lng], 16);
                        setTimeout(notifyKotlin, 100);
                    }
                }
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Fullscreen WebView OpenStreetMap
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    }
                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            view?.loadUrl("javascript:notifyKotlin();")
                        }
                    }
                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun onLocationChanged(lat: Double, lng: Double) {
                            currentLat = lat
                            currentLng = lng
                            performReverseGeocode(lat, lng)
                        }
                    }, "AndroidMapInterface")

                    loadDataWithBaseURL("https://unpkg.com", htmlContent, "text/html", "UTF-8", null)
                    webViewInstance = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top Search Bar & Header Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter)
        ) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("map_picker_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = "رجوع",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("ابحث عن منطقة أو شارع أو محطة...", fontSize = 13.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { performSearch(searchQuery) }),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("map_search_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        )
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = { performSearch(searchQuery) },
                        modifier = Modifier.testTag("map_search_button")
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "بحث",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Quick Center Floating Button on Side
        FloatingActionButton(
            onClick = {
                requestAndFetchLocation()
            },
            modifier = Modifier
                .padding(bottom = 180.dp, end = 16.dp)
                .align(Alignment.BottomEnd)
                .testTag("map_center_my_location_button"),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            elevation = FloatingActionButtonDefaults.elevation(6.dp),
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Outlined.MyLocation,
                contentDescription = "تحديد موقعي"
            )
        }

        // Bottom Confirmation Floating Card
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(24.dp),
            elevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = "الموقع المحدد",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "الموقع المختار",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isGeocodingLoading) "جاري تحديث العنوان..." else locationAddress,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        onLocationSelected(locationAddress, currentLat, currentLng)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("confirm_map_location_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تأكيد هذا الموقع",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

