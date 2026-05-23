package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.example.data.*
import com.example.ui.theme.*
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen()
            }
        }
    }
}

// ----------------------------------------------------
// REGIONALIZATION CONFIGURATION (INDIA & US SUPPORT)
// ----------------------------------------------------
data class CountryConfig(
    val countryCode: String,
    val flag: String,
    val name: String,
    val currency: String,
    val distanceUnit: String,
    val blockLabel: String,
    val flatLabel: String,
    val phoneCode: String,
    val phonePlaceholder: String
)

val IndianConfig = CountryConfig(
    countryCode = "IN",
    flag = "🇮🇳",
    name = "India",
    currency = "₹",
    distanceUnit = "km",
    blockLabel = "Block",
    flatLabel = "Flat",
    phoneCode = "+91",
    phonePlaceholder = "e.g. 98765-43210"
)

val UsConfig = CountryConfig(
    countryCode = "US",
    flag = "🇺🇸",
    name = "United States",
    currency = "$",
    distanceUnit = "mi",
    blockLabel = "Building",
    flatLabel = "Apt",
    phoneCode = "+1",
    phonePlaceholder = "e.g. (555) 019-2834"
)

fun getCountryConfig(code: String): CountryConfig {
    return if (code == "US") UsConfig else IndianConfig
}

@Composable
fun MainScreen() {
    var isWebPortalActive by remember { mutableStateOf(false) }

    if (isWebPortalActive) {
        Box(modifier = Modifier.fillMaxSize()) {
            WebViewPortal()
            FloatingActionButton(
                onClick = { isWebPortalActive = false },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .padding(bottom = 32.dp),
                containerColor = OceanBlue
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Close Web Portal", tint = Color.White)
            }
        }
        return
    }

    val appViewModel: NearbyDriveViewModel = viewModel()
    
    val profileState by appViewModel.profile.collectAsStateWithLifecycle()
    val vehiclesState by appViewModel.allVehicles.collectAsStateWithLifecycle()
    val bookingsState by appViewModel.allBookings.collectAsStateWithLifecycle()
    
    val selectedTypeFilter by appViewModel.selectedTypeFilter.collectAsStateWithLifecycle()
    val isEvFilterOnly by appViewModel.isEvFilterOnly.collectAsStateWithLifecycle()
    val searchQuery by appViewModel.searchQuery.collectAsStateWithLifecycle()
    val userCountry by appViewModel.userCountry.collectAsStateWithLifecycle()
    
    val countryConfig = getCountryConfig(userCountry)

    var activeTab by remember { mutableStateOf("Browse") }
    
    // States for custom modals
    var bookingVehicleTarget by remember { mutableStateOf<VehicleEntity?>(null) }
    var showProfileRequiredWarning by remember { mutableStateOf(false) }
    var showVerificationRequiredWarning by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("main_scaffold"),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("app_bottom_nav"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.DirectionsCar, contentDescription = "Browse Rides") },
                    label = { Text("Rent") },
                    selected = activeTab == "Browse",
                    onClick = { activeTab = "Browse" },
                    modifier = Modifier.testTag("nav_tab_browse"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = OceanBlue,
                        selectedTextColor = OceanBlue,
                        indicatorColor = OceanLight,
                        unselectedIconColor = SlateBlueText.copy(alpha = 0.6f),
                        unselectedTextColor = SlateBlueText.copy(alpha = 0.6f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.AddCircle, contentDescription = "Host Ride") },
                    label = { Text("Host") },
                    selected = activeTab == "Host",
                    onClick = { activeTab = "Host" },
                    modifier = Modifier.testTag("nav_tab_host"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = OceanBlue,
                        selectedTextColor = OceanBlue,
                        indicatorColor = OceanLight,
                        unselectedIconColor = SlateBlueText.copy(alpha = 0.6f),
                        unselectedTextColor = SlateBlueText.copy(alpha = 0.6f)
                    )
                )
                NavigationBarItem(
                    icon = {
                        val incomingPendingCount = bookingsState.count { 
                            it.ownerName == (profileState?.name ?: "") && it.status == "Requested"
                        }
                        BadgedBox(
                            badge = {
                                if (incomingPendingCount > 0) {
                                    Badge { Text(incomingPendingCount.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Bookmarks, contentDescription = "Bookings Tracker")
                        }
                    },
                    label = { Text("Bookings") },
                    selected = activeTab == "Bookings",
                    onClick = { activeTab = "Bookings" },
                    modifier = Modifier.testTag("nav_tab_bookings"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = OceanBlue,
                        selectedTextColor = OceanBlue,
                        indicatorColor = OceanLight,
                        unselectedIconColor = SlateBlueText.copy(alpha = 0.6f),
                        unselectedTextColor = SlateBlueText.copy(alpha = 0.6f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Person, contentDescription = "Profile Settings") },
                    label = { Text("Profile") },
                    selected = activeTab == "Profile",
                    onClick = { activeTab = "Profile" },
                    modifier = Modifier.testTag("nav_tab_profile"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = OceanBlue,
                        selectedTextColor = OceanBlue,
                        indicatorColor = OceanLight,
                        unselectedIconColor = SlateBlueText.copy(alpha = 0.6f),
                        unselectedTextColor = SlateBlueText.copy(alpha = 0.6f)
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Elegant Header
            HeaderView(
                profile = profileState,
                selectedCountry = userCountry,
                onCountryChanged = { appViewModel.setCountry(it) },
                config = countryConfig,
                onToggleWebPortal = { isWebPortalActive = true }
            )

            // Missing profile/verification warnings
            val isProfileEmpty = profileState == null || profileState?.name.isNullOrBlank() || profileState?.phone.isNullOrBlank()
            val isVerified = profileState?.isVerified == true
            
            if (isProfileEmpty && activeTab != "Profile") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { activeTab = "Profile" },
                    colors = CardDefaults.cardColors(
                        containerColor = AccentCoral.copy(alpha = 0.15f),
                        contentColor = AccentCoral
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Error,
                            contentDescription = "Profile Warning",
                            tint = AccentCoral,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Profile Incomplete",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                "Please add your name, ${countryConfig.blockLabel.lowercase()} & ${countryConfig.flatLabel.lowercase()} number to request or host rides.",
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = "Go to Profile",
                            tint = AccentCoral
                        )
                    }
                }
            } else if (!isVerified && activeTab != "Profile") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { activeTab = "Profile" },
                    colors = CardDefaults.cardColors(
                        containerColor = WarmAmber.copy(alpha = 0.15f),
                        contentColor = WarmAmber
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.VerifiedUser,
                            contentDescription = "Verification pending warning",
                            tint = WarmAmber,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Society Verification Pending",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                "To secure our community, please verify your resident identity card now.",
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = "Go to Profile",
                            tint = WarmAmber
                        )
                    }
                }
            }

            // Tab contents
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "tab_fade"
                ) { targetTab ->
                    when (targetTab) {
                        "Browse" -> BrowseRidesScreen(
                            vehicles = vehiclesState,
                            profile = profileState,
                            selectedTypeFilter = selectedTypeFilter,
                            searchQuery = searchQuery,
                            isEvFilterOnly = isEvFilterOnly,
                            onTypeFilterSelected = { appViewModel.setTypeFilter(it) },
                            onSearchQueryChanged = { appViewModel.setSearchQuery(it) },
                            onEvFilterChanged = { appViewModel.setEvFilter(it) },
                            onBookClicked = { vehicle ->
                                if (isProfileEmpty) {
                                    showProfileRequiredWarning = true
                                } else if (!isVerified) {
                                    showVerificationRequiredWarning = true
                                } else {
                                    bookingVehicleTarget = vehicle
                                }
                            },
                            config = countryConfig
                        )
                        "Host" -> HostScreen(
                            allVehicles = vehiclesState,
                            profile = profileState,
                            onAddVehicle = { model, type, reg, rent, avail, desc, isEv ->
                                appViewModel.addVehicle(model, type, reg, rent, avail, desc, isEv)
                            },
                            onDeleteVehicle = { appViewModel.deleteVehicle(it) },
                            config = countryConfig
                        )
                        "Bookings" -> BookingsScreen(
                            bookings = bookingsState,
                            currentUserProfile = profileState,
                            onUpdateBookingStatus = { booking, status ->
                                appViewModel.updateBookingStatus(booking, status)
                            },
                            config = countryConfig
                        )
                        "Profile" -> ProfileScreen(
                            profile = profileState,
                            onSaveProfile = { name, block, flat, phone, isVerified ->
                                appViewModel.saveProfile(name, block, flat, phone, isVerified)
                            },
                            config = countryConfig
                        )
                    }
                }
            }
        }
    }

    // Modal Dialog to Book a Ride
    bookingVehicleTarget?.let { vehicle ->
        BookRideDialog(
            vehicle = vehicle,
            onDismiss = { bookingVehicleTarget = null },
            onConfirmBooking = { hours, notes ->
                appViewModel.requestBooking(vehicle, hours, notes)
                bookingVehicleTarget = null
            },
            config = countryConfig
        )
    }

    // Profile Incomplete Warning Dialog
    if (showProfileRequiredWarning) {
        AlertDialog(
            onDismissRequest = { showProfileRequiredWarning = false },
            title = { Text("Profile Required") },
            text = { Text("To rent vehicles in your society, you must first specify your Name, ${countryConfig.blockLabel} and ${countryConfig.flatLabel} Number under the Profile tab so owners can coordinate key handovers with you.") },
            confirmButton = {
                Button(
                    onClick = {
                        showProfileRequiredWarning = false
                        activeTab = "Profile"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OceanBlue)
                ) {
                    Text("Go to Profile")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileRequiredWarning = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Residency Verification Required Warning Dialog
    if (showVerificationRequiredWarning) {
        AlertDialog(
            onDismissRequest = { showVerificationRequiredWarning = false },
            title = { Text("Residency Verification Required") },
            text = { Text("To protect our community members and prevent external/unauthorized entries, booking listings requires verified society residency. Please go to the Profile tab and complete 'Verify Society Membership' first.") },
            confirmButton = {
                Button(
                    onClick = {
                        showVerificationRequiredWarning = false
                        activeTab = "Profile"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OceanBlue)
                ) {
                    Text("Go to Profile")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVerificationRequiredWarning = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun HeaderView(
    profile: ProfileEntity?,
    selectedCountry: String,
    onCountryChanged: (String) -> Unit,
    config: CountryConfig,
    onToggleWebPortal: () -> Unit = {}
) {
    var showCountryMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.DirectionsCar,
                    contentDescription = "NearbyDrive Logo",
                    tint = OceanBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "NearbyDrive",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontFamily = FontFamily.SansSerif
                )
            }
            Text(
                text = "Co-sharing rides in your housing society",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.61f)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Dropdown Country Selector Chip
            Box {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(OceanLight.copy(alpha = 0.5f))
                        .clickable { showCountryMenu = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (selectedCountry == "IN") "🇮🇳 IN" else "🇺🇸 US",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = OceanBlue
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            Icons.Filled.ArrowDropDown,
                            contentDescription = "Select country",
                            modifier = Modifier.size(14.dp),
                            tint = OceanBlue
                        )
                    }
                }
                DropdownMenu(
                    expanded = showCountryMenu,
                    onDismissRequest = { showCountryMenu = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    DropdownMenuItem(
                        text = { Text("🇮🇳 India (₹, km)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = OceanBlue) },
                        onClick = {
                            onCountryChanged("IN")
                            showCountryMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("🇺🇸 United States ($, mi)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = OceanBlue) },
                        onClick = {
                            onCountryChanged("US")
                            showCountryMenu = false
                        }
                    )
                }
            }

            // Mini Resident Chip
            profile?.let {
                if (it.name.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(OceanLight)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Home,
                                contentDescription = "Home Flat",
                                tint = OceanBlue,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${it.block}-${it.flat}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = OceanBlue
                            )
                        }
                    }
                }
            }

            // Web Portal Toggle
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(OceanBlue)
                    .clickable(onClick = onToggleWebPortal)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Web,
                        contentDescription = "Web Portal",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "WEB",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun WebViewPortal() {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                webViewClient = WebViewClient()
                loadUrl("https://nearbydrive.com")
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun NeighborChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) OceanBlue else SoftGray)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else SlateBlueText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ----------------------------------------------------
// BROWSE RIDES SCREEN
// ----------------------------------------------------
@Composable
fun BrowseRidesScreen(
    vehicles: List<VehicleEntity>,
    profile: ProfileEntity?,
    selectedTypeFilter: String,
    searchQuery: String,
    isEvFilterOnly: Boolean,
    onTypeFilterSelected: (String) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onEvFilterChanged: (Boolean) -> Unit,
    onBookClicked: (VehicleEntity) -> Unit,
    config: CountryConfig
) {
    val focusManager = LocalFocusManager.current
    val filterTypes = listOf("All", "Car", "Bike", "Scooter", "Bicycle", "Other")

    // Filter vehicles locally
    val filteredVehicles = remember(vehicles, selectedTypeFilter, searchQuery, isEvFilterOnly) {
        vehicles.filter { vehicle ->
            val matchType = selectedTypeFilter == "All" || vehicle.type.equals(selectedTypeFilter, ignoreCase = true)
            val matchSearch = searchQuery.isBlank() ||
                    vehicle.model.contains(searchQuery, ignoreCase = true) ||
                    vehicle.ownerBlock.contains(searchQuery, ignoreCase = true) ||
                    vehicle.ownerFlat.contains(searchQuery, ignoreCase = true) ||
                    vehicle.desc.contains(searchQuery, ignoreCase = true)
            val matchEv = !isEvFilterOnly || vehicle.isEv
            matchType && matchSearch && matchEv
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search & Explore bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .testTag("vehicle_search_input"),
            placeholder = { Text("Search specifications, ${config.blockLabel.lowercase()}s, or keywords...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChanged("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear Search")
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OceanBlue,
                unfocusedBorderColor = SlateBlueText.copy(alpha = 0.2f)
            )
        )

        // Type Filter Chips Scroll with EV Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .padding(start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(filterTypes) { type ->
                    val isSelected = selectedTypeFilter == type
                    NeighborChip(
                        selected = isSelected,
                        onClick = {
                            focusManager.clearFocus()
                            onTypeFilterSelected(type)
                        },
                        label = type,
                        modifier = Modifier.testTag("filter_chip_$type")
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            Surface(
                modifier = Modifier
                    .testTag("filter_chip_ev_only")
                    .clickable {
                        focusManager.clearFocus()
                        onEvFilterChanged(!isEvFilterOnly)
                    },
                shape = RoundedCornerShape(16.dp),
                color = if (isEvFilterOnly) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.dp,
                    if (isEvFilterOnly) MaterialTheme.colorScheme.primary else SlateBlueText.copy(alpha = 0.2f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Filled.Bolt,
                        contentDescription = "EV filter",
                        modifier = Modifier.size(16.dp),
                        tint = if (isEvFilterOnly) MaterialTheme.colorScheme.primary else SlateBlueText.copy(alpha = 0.6f)
                    )
                    Text(
                        "EVs Only",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isEvFilterOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Listed Rides Feed
        if (filteredVehicles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.DirectionsCar,
                        contentDescription = "No vehicles found",
                        tint = SlateBlueText.copy(alpha = 0.3f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No Vehicles Listed",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Try changing your search query or filters. Go to the Host tab to post yours!",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("vehicles_lazy_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredVehicles, key = { it.id }) { vehicle ->
                    VehicleCard(
                        vehicle = vehicle,
                        profile = profile,
                        onBookClicked = { onBookClicked(vehicle) },
                        config = config
                    )
                }
            }
        }
    }
}

@Composable
fun VehicleCard(
    vehicle: VehicleEntity,
    profile: ProfileEntity?,
    onBookClicked: () -> Unit,
    config: CountryConfig
) {
    val isMine = profile != null && vehicle.ownerName == profile.name && vehicle.ownerBlock == profile.block

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vehicle_card_${vehicle.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Elegant Visual Representation and Title
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
            ) {
                VehicleVisualBanner(type = vehicle.type)

                // Category Tag
                Box(
                    modifier = Modifier
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.85f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .align(Alignment.TopStart)
                ) {
                    Text(
                        text = vehicle.type.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (vehicle.type) {
                            "Car" -> Color(0xFF354B3E)      // Forest Green
                            "Bike" -> Color(0xFF5A4432)     // Chocolate Mud
                            "Scooter" -> Color(0xFF6B5B3E)  // Ochre Olive
                            "Bicycle" -> Color(0xFF7A3E2B)  // Dark Terracotta
                            else -> Color(0xFF4C4A42)
                        }
                    )
                }

                // If my own ride tag
                if (isMine) {
                    Box(
                        modifier = Modifier
                            .padding(12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(OceanBlue)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .align(Alignment.TopEnd)
                    ) {
                        Text(
                            text = "MY LISTING",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // EV Badge
                if (vehicle.isEv) {
                    Box(
                        modifier = Modifier
                            .padding(12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2E7D32)) // Forest Green
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .align(if (isMine) Alignment.BottomEnd else Alignment.TopEnd)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Filled.Bolt,
                                contentDescription = "Electric Vehicle",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "EV",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Body Details
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = vehicle.model,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.LocationOn,
                                contentDescription = "Owner Location",
                                tint = OceanBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Lent by ${vehicle.ownerName} (★ 4.8 • ${config.blockLabel} ${vehicle.ownerBlock} • ${config.flatLabel} ${vehicle.ownerFlat})",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Price Chip
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${config.currency}${vehicle.rentPerHour.toInt()}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = OceanBlue
                        )
                        Text(
                            text = "/ hour",
                            fontSize = 10.sp,
                            color = SlateBlueText.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                HorizontalDivider(color = SlateBlueText.copy(alpha = 0.1f))

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = vehicle.desc,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Status and Info Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.DateRange,
                                contentDescription = "Availability Details",
                                tint = SlateBlueText.copy(alpha = 0.5f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = vehicle.availabilityHours,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SlateBlueText.copy(alpha = 0.6f)
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = "Reg No",
                                tint = SlateBlueText.copy(alpha = 0.5f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Reg: ${vehicle.regNumber}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = SlateBlueText.copy(alpha = 0.6f)
                            )
                        }
                    }

                    if (vehicle.status == "Rented") {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(AccentCoral.copy(alpha = 0.15f))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "RENTED OUT",
                                color = AccentCoral,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (isMine) {
                        Button(
                            onClick = {},
                            enabled = false,
                            colors = ButtonDefaults.buttonColors(
                                disabledContainerColor = SoftGray,
                                disabledContentColor = SlateBlueText.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Your listing")
                        }
                    } else {
                        Button(
                            onClick = onBookClicked,
                            colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("book_button_${vehicle.id}")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Rent Now", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Custom vehicle drawing using Compose Canvas (no assets dependencies)
@Composable
fun VehicleVisualBanner(type: String) {
    val (gradientColors, icon, tintColor) = when (type) {
        "Car" -> Triple(
            listOf(Color(0xFFE2EFE0), Color(0xFFC2DCC0)),
            Icons.Filled.DirectionsCar,
            Color(0xFF2C4C38)
        )
        "Bike" -> Triple(
            listOf(Color(0xFFFAF2E9), Color(0xFFE8D5C4)),
            Icons.Filled.TwoWheeler,
            Color(0xFF5E4533)
        )
        "Scooter" -> Triple(
            listOf(Color(0xFFFAF7E6), Color(0xFFEBE3BD)),
            Icons.Filled.Moped,
            Color(0xFF6B5824)
        )
        "Bicycle" -> Triple(
            listOf(Color(0xFFFAF0EB), Color(0xFFEBD2C4)),
            Icons.Filled.PedalBike,
            Color(0xFF7A3E25)
        )
        else -> Triple(
            listOf(Color(0xFFF5F5F5), Color(0xFFDDDDDD)),
            Icons.Filled.ElectricScooter,
            Color(0xFF4C4A42)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        // Draw subtle premium artistic grid dots
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stepX = size.width / 12
            val stepY = size.height / 6
            for (i in 1..11) {
                for (j in 1..5) {
                    drawCircle(
                        color = tintColor.copy(alpha = 0.08f),
                        radius = 1.5.dp.toPx(),
                        center = Offset(i * stepX, j * stepY)
                    )
                }
            }
        }

        // Circular frosted badge wrapper
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.82f))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = type,
                tint = tintColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ----------------------------------------------------
// LEND/HOST SCREEN (OFFER A VEHICLE)
// ----------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HostScreen(
    allVehicles: List<VehicleEntity>,
    profile: ProfileEntity?,
    onAddVehicle: (String, String, String, Double, String, String, Boolean) -> Unit,
    onDeleteVehicle: (VehicleEntity) -> Unit,
    config: CountryConfig
) {
    var offerModel by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("Car") }
    var offerReg by remember { mutableStateOf("") }
    var offerRentStr by remember { mutableStateOf("") }
    var offerHours by remember { mutableStateOf("Daily (24/7)") }
    var offerDesc by remember { mutableStateOf("") }
    var offerIsEv by remember { mutableStateOf(false) }

    val vehicleTypes = listOf("Car", "Bike", "Scooter", "Bicycle", "Other")
    
    // Track vehicles hosted by THIS exact profile
    val myVehicles = remember(allVehicles, profile) {
        if (profile == null) emptyList()
        else allVehicles.filter { it.ownerName == profile.name && it.ownerBlock == profile.block }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("host_screen_scroll"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Host Your Vehicle for Rent",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Help your neighbors when they need a quick ride, while earning from your vehicle during idle times.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        item {
            // Addition Form Card
            Card(
                modifier = Modifier.fillMaxWidth().testTag("host_form_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Vehicle Specifications",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = OceanBlue
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Model Name Input
                    OutlinedTextField(
                        value = offerModel,
                        onValueChange = { offerModel = it },
                        label = { Text("Vehicle Model & Make") },
                        placeholder = { Text("e.g. Honda Civic, Activa 6G") },
                        modifier = Modifier.fillMaxWidth().testTag("host_model_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OceanBlue)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Type Choice Radio/Chips Row
                    Text(
                        "Vehicle Category",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateBlueText.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        vehicleTypes.forEach { type ->
                            val isChosen = selectedType == type
                            NeighborChip(
                                selected = isChosen,
                                onClick = { selectedType = type },
                                label = type,
                                modifier = Modifier.testTag("type_radio_$type")
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Reg Number & Price Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = offerReg,
                            onValueChange = { offerReg = it },
                            label = { Text("Reg Number") },
                            placeholder = { Text("MH-12-AB-3456") },
                            modifier = Modifier.weight(1f).testTag("host_reg_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OceanBlue)
                        )

                        OutlinedTextField(
                            value = offerRentStr,
                            onValueChange = { offerRentStr = it },
                            label = { Text("Rent ${config.currency}/hr") },
                            placeholder = { Text("50") },
                            modifier = Modifier.weight(1f).testTag("host_rent_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OceanBlue)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Hours of availability
                    OutlinedTextField(
                        value = offerHours,
                        onValueChange = { offerHours = it },
                        label = { Text("Availability Schedule") },
                        placeholder = { Text("e.g. Weekends only, Daily 8 AM - 10 PM") },
                        modifier = Modifier.fillMaxWidth().testTag("host_availability_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OceanBlue)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Short description
                    OutlinedTextField(
                        value = offerDesc,
                        onValueChange = { offerDesc = it },
                        label = { Text("Short Description & Guidelines") },
                        placeholder = { Text("Where are keys placed? Double helmets available etc.") },
                        modifier = Modifier.fillMaxWidth().height(90.dp).testTag("host_desc_input"),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OceanBlue)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // EV Switch toggle row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SoftGray.copy(alpha = 0.5f))
                            .clickable { offerIsEv = !offerIsEv }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = "EV Icon",
                                tint = if (offerIsEv) MintGreen else SlateBlueText.copy(alpha = 0.5f),
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Electric Vehicle (EV)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Tag this vehicle as fully electric",
                                    fontSize = 11.sp,
                                    color = SlateBlueText.copy(alpha = 0.6f)
                                )
                            }
                        }
                        Switch(
                            checked = offerIsEv,
                            onCheckedChange = { offerIsEv = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MintGreen,
                                uncheckedThumbColor = SlateBlueText.copy(alpha = 0.5f),
                                uncheckedTrackColor = SoftGray
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    val canSubmit = offerModel.isNotBlank() && offerReg.isNotBlank() && offerRentStr.isNotBlank() && profile != null && profile.name.isNotBlank() && profile.isVerified
                    Button(
                        onClick = {
                            val rentDouble = offerRentStr.toDoubleOrNull() ?: 0.0
                            onAddVehicle(offerModel, selectedType, offerReg, rentDouble, offerHours, offerDesc, offerIsEv)
                            // Clean details after submit
                            offerModel = ""
                            offerReg = ""
                            offerRentStr = ""
                            offerDesc = ""
                            offerIsEv = false
                        },
                        enabled = canSubmit,
                        modifier = Modifier.fillMaxWidth().testTag("host_submit_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = OceanBlue, disabledContainerColor = SoftGray)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Confirm & Set For Rent", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (profile == null || profile.name.isBlank()) {
                        Text(
                            "⚠️ You must complete your residency details on the Profile tab before you can host a ride.",
                            fontSize = 11.sp,
                            color = AccentCoral,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    } else if (!profile.isVerified) {
                        Text(
                            "⚠️ Society Residency Verification Pending. Please go to the Profile tab and click 'Verify Society Membership' to verify your ${config.blockLabel} & ${config.flatLabel} first.",
                            fontSize = 11.sp,
                            color = WarmAmber,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Active Listings Title
        item {
            Text(
                "My Submissions (${myVehicles.size})",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Active Listings list
        if (myVehicles.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SoftGray)
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            "You haven't added any vehicles for rent yet.",
                            fontSize = 13.sp,
                            color = SlateBlueText.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(myVehicles) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("hosted_vehicle_item_${item.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, SoftGray)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(OceanLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    when (item.type) {
                                        "Car" -> Icons.Filled.DirectionsCar
                                        "Bike" -> Icons.Filled.TwoWheeler
                                        "Scooter" -> Icons.Filled.ElectricScooter
                                        "Bicycle" -> Icons.Filled.PedalBike
                                        else -> Icons.Filled.DirectionsTransit
                                    },
                                    contentDescription = null,
                                    tint = OceanBlue
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    item.model,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "Rate: ${config.currency}${item.rentPerHour.toInt()}/hr • Reg: ${item.regNumber}",
                                    fontSize = 12.sp,
                                    color = SlateBlueText.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Status tag
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (item.status == "Available") MintGreen.copy(alpha = 0.15f)
                                        else AccentCoral.copy(alpha = 0.15f)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    item.status.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.status == "Available") MintGreen else AccentCoral
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { onDeleteVehicle(item) },
                                modifier = Modifier.testTag("delete_vehicle_${item.id}")
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete Listing", tint = AccentCoral)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// BOOKINGS SCREEN Tracker / Approvals
// ----------------------------------------------------
@Composable
fun BookingsScreen(
    bookings: List<BookingEntity>,
    currentUserProfile: ProfileEntity?,
    onUpdateBookingStatus: (BookingEntity, String) -> Unit,
    config: CountryConfig
) {
    val myName = currentUserProfile?.name ?: ""
    val myBlock = currentUserProfile?.block ?: ""

    // Bookings requested BY the current resident (Borrower perspective)
    val myRentedBookings = remember(bookings, myName, myBlock) {
        bookings.filter { it.renterName == myName && it.renterBlock == myBlock }
    }

    // Bookings requested FROM the current resident (Lender perspective)
    val incomingRequests = remember(bookings, myName, myBlock) {
        bookings.filter { it.ownerName == myName && it.ownerBlock == myBlock }
    }

    var requestSubTab by remember { mutableStateOf("Borrowing") }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = if (requestSubTab == "Borrowing") 0 else 1,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = OceanBlue
        ) {
            Tab(
                selected = requestSubTab == "Borrowing",
                onClick = { requestSubTab = "Borrowing" },
                modifier = Modifier.testTag("bookings_subtab_borrowing")
            ) {
                Box(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Input, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "My Bookings (${myRentedBookings.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            Tab(
                selected = requestSubTab == "Lending",
                onClick = { requestSubTab = "Lending" },
                modifier = Modifier.testTag("bookings_subtab_lending")
            ) {
                Box(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val pendingHostCount = incomingRequests.count { it.status == "Requested" }
                        BadgedBox(
                            badge = {
                                if (pendingHostCount > 0) {
                                    Badge { Text(pendingHostCount.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Filled.AssignmentReturned, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Host Dashboard (${incomingRequests.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        if (requestSubTab == "Borrowing") {
            // Renter views outbound bookings
            if (myRentedBookings.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.BookOnline, contentDescription = null, modifier = Modifier.size(64.dp), tint = SlateBlueText.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No active rentals", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Looking to grab a car or scooter? Browse existing offers on the Rent tab!", fontSize = 13.sp, color = SlateBlueText.copy(alpha = 0.5f), textAlign = TextAlign.Center)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().testTag("bookings_borrower_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(myRentedBookings, key = { it.id }) { booking ->
                        BorrowerBookingCard(booking = booking, onCancel = { onUpdateBookingStatus(booking, "Cancelled") }, config = config)
                    }
                }
            }
        } else {
            // Lender views inbound requests from neighbors
            if (incomingRequests.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.AssignmentReturned, contentDescription = null, modifier = Modifier.size(64.dp), tint = SlateBlueText.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No sharing requests yet", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Post your vehicles in the Host tab. Neighbor requests will show up here!", fontSize = 13.sp, color = SlateBlueText.copy(alpha = 0.5f), textAlign = TextAlign.Center)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().testTag("bookings_lender_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(incomingRequests, key = { it.id }) { booking ->
                        LenderBookingCard(
                            booking = booking,
                            onApprove = { onUpdateBookingStatus(booking, "Approved") },
                            onDecline = { onUpdateBookingStatus(booking, "Cancelled") },
                            onComplete = { onUpdateBookingStatus(booking, "Completed") },
                            config = config
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BorrowerBookingCard(
    booking: BookingEntity,
    onCancel: () -> Unit,
    config: CountryConfig
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("borrower_booking_card_${booking.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        booking.vehicleModel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "Owner: ${booking.ownerName} (${config.blockLabel} ${booking.ownerBlock} • ${config.flatLabel} ${booking.ownerFlat})",
                        fontSize = 12.sp,
                        color = SlateBlueText.copy(alpha = 0.6f)
                    )
                }

                StatusBadge(status = booking.status)
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = SlateBlueText.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("RENT DETAILS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateBlueText.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("${booking.hours} hours • ${config.currency}${booking.rentPerHour.toInt()}/hr", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("TOTAL COST", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateBlueText.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("${config.currency}${booking.totalPrice.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OceanBlue)
                }
            }

            if (booking.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SoftGray)
                        .padding(8.dp)
                ) {
                    Text("My Notes: ${booking.notes}", fontSize = 12.sp, color = SlateBlueText.copy(alpha = 0.7f))
                }
            }

            // Allowed to cancel if Requested/Approved but not completed
            if (booking.status == "Requested" || booking.status == "Approved") {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth().testTag("cancel_borrow_booking_${booking.id}"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentCoral),
                    border = BorderStroke(1.dp, AccentCoral)
                ) {
                    Text("Cancel Booking Request", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun LenderBookingCard(
    booking: BookingEntity,
    onApprove: () -> Unit,
    onDecline: () -> Unit,
    onComplete: () -> Unit,
    config: CountryConfig
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("lender_booking_card_${booking.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = booking.vehicleModel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Renter: ${booking.renterName} (${config.blockLabel} ${booking.renterBlock} • ${config.flatLabel} ${booking.renterFlat})",
                        fontSize = 12.sp,
                        color = SlateBlueText.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Phone: ${booking.renterPhone}",
                        fontSize = 11.sp,
                        color = SlateBlueText.copy(alpha = 0.5f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                StatusBadge(status = booking.status)
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = SlateBlueText.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("TIMELINE REQUESTED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateBlueText.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("${booking.hours} hours rental", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("YOUR REVENUE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateBlueText.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("${config.currency}${booking.totalPrice.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MintGreen)
                }
            }

            if (booking.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SoftGray)
                        .padding(8.dp)
                ) {
                    Text("Resident Note: \"${booking.notes}\"", fontSize = 12.sp, color = SlateBlueText)
                }
            }

            // Interactive Actions based on requested states
            when (booking.status) {
                "Requested" -> {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDecline,
                            modifier = Modifier.weight(1f).testTag("decline_booking_btn_${booking.id}"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentCoral),
                            border = BorderStroke(1.dp, AccentCoral)
                        ) {
                            Text("Reject", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onApprove,
                            modifier = Modifier.weight(1f).testTag("approve_booking_btn_${booking.id}"),
                            colors = ButtonDefaults.buttonColors(containerColor = MintGreen)
                        ) {
                            Text("Approve", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                "Approved" -> {
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onComplete,
                        modifier = Modifier.fillMaxWidth().testTag("complete_booking_btn_${booking.id}"),
                        colors = ButtonDefaults.buttonColors(containerColor = OceanBlue)
                    ) {
                        Text("Complete Ride & Declare Return", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val tintColor = when (status) {
        "Requested" -> WarmAmber
        "Approved" -> MintGreen
        "Completed" -> SlateBlueText.copy(alpha = 0.5f)
        "Cancelled" -> AccentCoral
        else -> OceanBlue
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(tintColor.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = status.uppercase(),
            color = tintColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black
        )
    }
}

// ----------------------------------------------------
// USER PROFILE SCREEN & VERIFICATION FLOW
// ----------------------------------------------------
@Composable
fun ProfileScreen(
    profile: ProfileEntity?,
    onSaveProfile: (String, String, String, String, Boolean) -> Unit,
    config: CountryConfig
) {
    var name by remember(profile) { mutableStateOf(profile?.name ?: "") }
    var block by remember(profile) { mutableStateOf(profile?.block ?: "Block B") }
    var flat by remember(profile) { mutableStateOf(profile?.flat ?: "") }
    var phone by remember(profile) { mutableStateOf(profile?.phone ?: "") }

    val blocks = listOf("Block A", "Block B", "Block C", "Block D", "Block E")
    var showVerifyDialog by remember { mutableStateOf(false) }

    // Check if the current inputs differ from the last saved profile, resetting verification warning
    val addressChanged = profile == null || 
            name.trim() != profile.name || 
            block.trim() != profile.block || 
            flat.trim() != profile.flat

    val isVerifiedNow = profile?.isVerified == true && !addressChanged

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("profile_screen_scroll"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Society Identity Card",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "Verify your residency credentials so neighbors can recognize you.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        }

        item {
            // Visual Residency Poster (Passport Card Style)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.radialGradient(
                            colors = if (isVerifiedNow) {
                                listOf(Color(0xFF2E7D32), Color(0xFF1B5E20)) // Deep Green Forest theme for verified
                            } else {
                                listOf(Color(0xFF0369A1), Color(0xFF0F172A)) // Classic styling for pending/unverified
                            },
                            radius = 500f
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                "MEMBER PASSPORT",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                            Text(
                                name.ifBlank { "Guest Resident" },
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                              )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Icon(
                                if (isVerifiedNow) Icons.Filled.CheckCircle else Icons.Filled.Badge,
                                contentDescription = "Verification Badge",
                                tint = if (isVerifiedNow) Color(0xFFADC2A9) else OceanLight,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                if (isVerifiedNow) "VERIFIED MEMBER" else "UNVERIFIED STATUS",
                                color = if (isVerifiedNow) Color(0xFFADC2A9) else AccentCoral,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Column {
                            Text("HOUSING ${config.blockLabel.uppercase()}", color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(block.ifBlank { "Unassigned" }, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("${config.flatLabel.uppercase()} NUMBER", color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(flat.ifBlank { "Pending Setup" }, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("REGISTERED CONTACT", color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(phone.ifBlank { "None added" }, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Active prompt urging verification if name & flat are filled, but not verified
        if (!isVerifiedNow && name.isNotBlank() && flat.isNotBlank() && phone.isNotBlank()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("verification_pending_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = WarmAmber.copy(alpha = 0.12f)),
                    border = BorderStroke(1.dp, WarmAmber.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.VerifiedUser,
                                contentDescription = "Unverified user action badge",
                                tint = WarmAmber,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Society Verification Required",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = SlateDark
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Your residency coordinates for ${config.blockLabel} $block, ${config.flatLabel} $flat are configured but pending society registrar validation. Verify your membership to activate full listing & booking access.",
                            fontSize = 12.sp,
                            color = SlateBlueText
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showVerifyDialog = true },
                            modifier = Modifier.fillMaxWidth().testTag("start_verification_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = OceanBlue)
                        ) {
                            Text("Verify residency at $block • $flat", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("profile_form"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Society Residency Pass",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = OceanBlue
                        )
                        if (isVerifiedNow) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = MintGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Verified Active", color = MintGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth().testTag("profile_name_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OceanBlue)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Block dropdown selection in single chips row
                    Text(
                        "Society ${config.blockLabel}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateBlueText.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        blocks.forEach { b ->
                            val isSel = block == b
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) OceanBlue else SoftGray)
                                    .clickable { block = b }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = b.removePrefix("Block "),
                                    color = if (isSel) Color.White else SlateBlueText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = flat,
                        onValueChange = { flat = it },
                        label = { Text("${config.flatLabel} Number") },
                        modifier = Modifier.fillMaxWidth().testTag("profile_flat_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OceanBlue)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Contact Number") },
                        modifier = Modifier.fillMaxWidth().testTag("profile_phone_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OceanBlue)
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    if (addressChanged && profile?.isVerified == true) {
                        Text(
                            "⚠️ Saving these modifications will reset your verified society membership badge.",
                            fontSize = 11.sp,
                            color = WarmAmber,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    Button(
                        onClick = { 
                            onSaveProfile(name, block, flat, phone, isVerifiedNow) 
                        },
                        modifier = Modifier.fillMaxWidth().testTag("profile_save_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = OceanBlue)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Society Profile", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    HorizontalDivider(color = SoftGray)

                    // Reset Profile & Register as a Fresh Resident Action (To let testers try full verification flows)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                name = ""
                                block = "Block A"
                                flat = ""
                                phone = ""
                                onSaveProfile("", "Block A", "", "", false)
                            },
                            modifier = Modifier.testTag("reset_profile_btn")
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Trash icon", modifier = Modifier.size(16.dp), tint = AccentCoral)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Register New Resident (Clear Profile)", color = AccentCoral, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showVerifyDialog) {
        SocietyVerificationDialog(
            block = block,
            flat = flat,
            onDismiss = { showVerifyDialog = false },
            onVerificationSuccess = {
                onSaveProfile(name, block, flat, phone, true)
                showVerifyDialog = false
            },
            config = config
        )
    }
}

// ----------------------------------------------------
// POP-UP DIALOG: SOCIETY INTERCOM REGISTER VERIFICATION
// ----------------------------------------------------
@Composable
fun SocietyVerificationDialog(
    block: String,
    flat: String,
    onDismiss: () -> Unit,
    onVerificationSuccess: () -> Unit,
    config: CountryConfig
) {
    var code by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    var progressStatus by remember { mutableStateOf("Ready to begin") }
    
    val scope = rememberCoroutineScope()
    
    Dialog(onDismissRequest = if (isVerifying) ({}) else onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("verification_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Filled.VerifiedUser,
                    contentDescription = null,
                    tint = OceanBlue,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Gated Registry Check",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "A virtual pairing ping is sent to the Guard Intercom at ${config.blockLabel} $block, ${config.flatLabel} $flat. Type any 4-digit code (e.g. 1234) as the security PIN to verify society membership.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isVerifying) {
                    CircularProgressIndicator(
                        color = OceanBlue,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = progressStatus,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = OceanBlue
                    )
                } else {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { 
                            if (it.length <= 4) {
                                code = it.filter { char -> char.isDigit() } 
                            }
                        },
                        label = { Text("Intercom Verification PIN") },
                        placeholder = { Text("e.g. 1234") },
                        modifier = Modifier.fillMaxWidth().testTag("verification_pin_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OceanBlue)
                    )
                    
                    if (errorMessage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            color = AccentCoral,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).testTag("verification_cancel_btn")
                        ) {
                            Text("Cancel", color = SlateBlueText)
                        }
                        
                        Button(
                            onClick = {
                                if (code.length != 4) {
                                    errorMessage = "Please enter a valid 4-digit verification PIN"
                                } else {
                                    isVerifying = true
                                    errorMessage = ""
                                    scope.launch {
                                        kotlinx.coroutines.delay(800)
                                        progressStatus = "Verifying ${config.blockLabel} Registry..."
                                        kotlinx.coroutines.delay(800)
                                        progressStatus = "Verifying ${config.flatLabel} $flat guard registry..."
                                        kotlinx.coroutines.delay(800)
                                        progressStatus = "Success! Credential verified."
                                        kotlinx.coroutines.delay(400)
                                        onVerificationSuccess()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                            modifier = Modifier.weight(1.5f).testTag("verification_submit_btn")
                        ) {
                            Text("Verify Now", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// BOTTOM DLGS: BOOK RIDE DIALOG
// ----------------------------------------------------
@Composable
fun BookRideDialog(
    vehicle: VehicleEntity,
    onDismiss: () -> Unit,
    onConfirmBooking: (Int, String) -> Unit,
    config: CountryConfig
) {
    var rentHours by remember { mutableStateOf(3) }
    var bookingNotes by remember { mutableStateOf("") }

    val totalPrice = vehicle.rentPerHour * rentHours

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("book_ride_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Booking Proposal",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close Booking Modal")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Vehicle Quick Details Box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = OceanLight)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (vehicle.type) {
                                    "Car" -> Icons.Filled.DirectionsCar
                                    "Bike" -> Icons.Filled.TwoWheeler
                                    "Scooter" -> Icons.Filled.ElectricScooter
                                    "Bicycle" -> Icons.Filled.PedalBike
                                    else -> Icons.Filled.DirectionsTransit
                                },
                                contentDescription = null,
                                tint = OceanBlue
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(vehicle.model, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "Owner: ${vehicle.ownerName} (${config.blockLabel} ${vehicle.ownerBlock} • ${config.flatLabel} ${vehicle.ownerFlat})",
                                fontSize = 11.sp,
                                color = SlateBlueText.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Hours Choice Box
                Text(
                    text = "How long do you need it?",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateBlueText.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (rentHours > 1) rentHours-- },
                        modifier = Modifier.testTag("decrease_hours_btn")
                    ) {
                        Icon(Icons.Filled.RemoveCircleOutline, contentDescription = "Decrease Hours", tint = OceanBlue, modifier = Modifier.size(28.dp))
                    }

                    Text(
                        text = "$rentHours hours",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = OceanBlue
                    )

                    IconButton(
                        onClick = { if (rentHours < 24) rentHours++ },
                        modifier = Modifier.testTag("increase_hours_btn")
                    ) {
                        Icon(Icons.Filled.AddCircleOutline, contentDescription = "Increase Hours", tint = OceanBlue, modifier = Modifier.size(28.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Notes input
                OutlinedTextField(
                    value = bookingNotes,
                    onValueChange = { bookingNotes = it },
                    label = { Text("Note for the owner") },
                    placeholder = { Text("e.g. going to buy groceries, returning by evening") },
                    modifier = Modifier.fillMaxWidth().testTag("booking_notes_input"),
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OceanBlue)
                )

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(color = SlateBlueText.copy(alpha = 0.08f))

                Spacer(modifier = Modifier.height(16.dp))

                // Rate calculation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Society Sharing Rate:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = SlateBlueText.copy(alpha = 0.6f)
                    )
                    Text(
                        "${config.currency}${vehicle.rentPerHour.toInt()} / hr",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Calculated Total:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "${config.currency}${totalPrice.toInt()}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = OceanBlue
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { onConfirmBooking(rentHours, bookingNotes) },
                    modifier = Modifier.fillMaxWidth().testTag("confirm_booking_submit"),
                    colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Request Ride Confirmation", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}
