package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Define global crash hook logger & storage
        val oldHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("CRASH_DUMP", "CRITICAL ERROR OCCURRED! APPNET CRASH PREVENTED / LOGGED", throwable)
            try {
                val prefs = getSharedPreferences("nearby_drive_prefs", android.content.Context.MODE_PRIVATE)
                val sw = java.io.StringWriter()
                val pw = java.io.PrintWriter(sw)
                throwable.printStackTrace(pw)
                prefs.edit().putString("last_crash_log", sw.toString()).apply()
            } catch (e: Throwable) {
                // Suppress save-related errors
            }
            if (oldHandler != null) {
                oldHandler.uncaughtException(thread, throwable)
            } else {
                java.lang.System.exit(1)
            }
        }

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
    val appViewModel: NearbyDriveViewModel = viewModel()
    
    val profileState by appViewModel.profile.collectAsStateWithLifecycle()
    val vehiclesState by appViewModel.allVehicles.collectAsStateWithLifecycle()
    val bookingsState by appViewModel.allBookings.collectAsStateWithLifecycle()
    val reviewsState by appViewModel.allReviews.collectAsStateWithLifecycle()
    
    val selectedTypeFilter by appViewModel.selectedTypeFilter.collectAsStateWithLifecycle()
    val isEvFilterOnly by appViewModel.isEvFilterOnly.collectAsStateWithLifecycle()
    val searchQuery by appViewModel.searchQuery.collectAsStateWithLifecycle()
    val userCountry by appViewModel.userCountry.collectAsStateWithLifecycle()
    
    val countryConfig = getCountryConfig(userCountry)

    // Auth screen check: force login/signup before using the app!
    val isLoggedIn = profileState?.isLoggedIn == true
    if (!isLoggedIn) {
        com.example.ui.auth.AuthenticationScreen(appViewModel = appViewModel)
        return
    }

    var activeTab by remember { mutableStateOf("Browse") }
    
    // Recovery Dialog if previous sessions crashed
    val crashContext = androidx.compose.ui.platform.LocalContext.current
    val crashPrefs = remember { crashContext.getSharedPreferences("nearby_drive_prefs", android.content.Context.MODE_PRIVATE) }
    var crashLog by remember { mutableStateOf(crashPrefs.getString("last_crash_log", null)) }

    if (crashLog != null) {
        AlertDialog(
            onDismissRequest = {
                crashPrefs.edit().remove("last_crash_log").apply()
                crashLog = null
            },
            title = {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Filled.Warning,
                        contentDescription = "Crash Report",
                        tint = AccentCoral,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("System Exception Captured 🛡️", color = SlateDark, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "The app has recovered from an unexpected exception. Below is the technical stacktrace. Tap \"Reset & Continue\" to dismiss and load the app normally.",
                        fontSize = 11.5.sp,
                        color = SlateBlueText,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E1E2E))
                            .border(1.dp, Color(0xFF3B3B4F), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = crashLog ?: "",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = Color(0xFFFCA5A5)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        crashPrefs.edit().remove("last_crash_log").apply()
                        crashLog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OceanBlue)
                ) {
                    Text("Reset & Continue")
                }
            }
        )
    }

    // States for custom modals
    var bookingVehicleTarget by remember { mutableStateOf<VehicleEntity?>(null) }
    var calendarPrefilledDate by remember { mutableStateOf<String?>(null) }
    var calendarPrefilledStartHour by remember { mutableStateOf<Int?>(null) }
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
                    icon = { Icon(Icons.Filled.DateRange, contentDescription = "Schedules & Availability") },
                    label = { Text("Calendar") },
                    selected = activeTab == "Calendar",
                    onClick = { activeTab = "Calendar" },
                    modifier = Modifier.testTag("nav_tab_calendar"),
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
                config = countryConfig
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
                            bookings = bookingsState,
                            profile = profileState,
                            reviews = reviewsState,
                            selectedTypeFilter = selectedTypeFilter,
                            searchQuery = searchQuery,
                            isEvFilterOnly = isEvFilterOnly,
                            onTypeFilterSelected = { appViewModel.setTypeFilter(it) },
                            onSearchQueryChanged = { appViewModel.setSearchQuery(it) },
                            onEvFilterChanged = { appViewModel.setEvFilter(it) },
                            onBookClicked = { vehicle, prefilledDate ->
                                if (isProfileEmpty) {
                                    showProfileRequiredWarning = true
                                } else if (!isVerified) {
                                    showVerificationRequiredWarning = true
                                } else {
                                    bookingVehicleTarget = vehicle
                                    calendarPrefilledDate = prefilledDate
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
                            onSubmitReview = { bookingId, vehicleId, model, reviewer, reviewee, role, rating, comment ->
                                appViewModel.submitReview(bookingId, vehicleId, model, reviewer, reviewee, role, rating, comment)
                            },
                            reviews = reviewsState,
                            config = countryConfig
                        )
                        "Calendar" -> AvailabilityCalendarScreen(
                            bookings = bookingsState,
                            vehicles = vehiclesState,
                            profile = profileState,
                            onBookSlotClicked = { vehicle, date, startH ->
                                if (profileState == null || profileState?.name.isNullOrBlank()) {
                                    showProfileRequiredWarning = true
                                } else if (profileState?.isVerified == false) {
                                    showVerificationRequiredWarning = true
                                } else {
                                    bookingVehicleTarget = vehicle
                                    calendarPrefilledDate = date
                                    calendarPrefilledStartHour = startH
                                }
                            },
                            config = countryConfig
                        )
                        "Profile" -> ProfileScreen(
                            profile = profileState,
                            onSaveProfile = { name, block, flat, phone, isVerified ->
                                appViewModel.saveProfile(name, block, flat, phone, isVerified)
                            },
                            onLogout = {
                                appViewModel.logout()
                            },
                            reviews = reviewsState,
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
            prefilledDate = calendarPrefilledDate,
            prefilledStartHour = calendarPrefilledStartHour,
            onDismiss = { 
                bookingVehicleTarget = null
                calendarPrefilledDate = null
                calendarPrefilledStartHour = null
            },
            onConfirmBooking = { hours, notes, date, startHour ->
                appViewModel.requestBooking(vehicle, hours, notes, date, startHour)
                bookingVehicleTarget = null
                calendarPrefilledDate = null
                calendarPrefilledStartHour = null
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
    config: CountryConfig
) {
    var showCountryMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.DirectionsCar,
                contentDescription = "NearbyDrive Logo",
                tint = OceanBlue,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "NEARBYDRIVE",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 0.5.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(6.dp))
            val societyName = if (selectedCountry == "IN") "GREENWOOD CO-OP" else "PINEWOOD MEADOWS"
            Text(
                text = "|  $societyName",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = OceanBlue,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Dropdown Country Selector Chip
            Box {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(OceanLight)
                        .border(1.dp, OceanBlue.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .clickable { showCountryMenu = true }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (selectedCountry == "IN") "🇮🇳 IN" else "🇺🇸 US",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = OceanBlue,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
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
                            .clip(RoundedCornerShape(8.dp))
                            .background(OceanLight)
                            .border(1.dp, OceanBlue.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Home,
                                contentDescription = "Home Flat",
                                tint = OceanBlue,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${it.block}-${it.flat}".uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = OceanBlue,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
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
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) OceanBlue else SoftGray)
            .border(
                width = 1.dp,
                color = if (selected) OceanBlue else SlateBlueText.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label.uppercase(),
            color = if (selected) Color.White else SlateBlueText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

// ----------------------------------------------------
// BROWSE RIDES SCREEN
// ----------------------------------------------------
@Composable
fun BrowseRidesScreen(
    vehicles: List<VehicleEntity>,
    bookings: List<BookingEntity>,
    profile: ProfileEntity?,
    reviews: List<ReviewEntity> = emptyList(),
    selectedTypeFilter: String,
    searchQuery: String,
    isEvFilterOnly: Boolean,
    onTypeFilterSelected: (String) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onEvFilterChanged: (Boolean) -> Unit,
    onBookClicked: (VehicleEntity, String?) -> Unit,
    config: CountryConfig
) {
    val focusManager = LocalFocusManager.current
    val filterTypes = listOf("All", "Car", "Bike", "Scooter", "Bicycle", "Other")

    var selectedDayFilter by remember { mutableStateOf("Any Day") }
    var showOnlyFullyAvailable by remember { mutableStateOf(false) }

    // Filter vehicles locally
    val filteredVehicles = remember(vehicles, bookings, selectedTypeFilter, searchQuery, isEvFilterOnly, selectedDayFilter, showOnlyFullyAvailable) {
        vehicles.filter { vehicle ->
            val matchType = selectedTypeFilter == "All" || vehicle.type.equals(selectedTypeFilter, ignoreCase = true)
            val matchSearch = searchQuery.isBlank() ||
                    vehicle.model.contains(searchQuery, ignoreCase = true) ||
                    vehicle.ownerBlock.contains(searchQuery, ignoreCase = true) ||
                    vehicle.ownerFlat.contains(searchQuery, ignoreCase = true) ||
                    vehicle.desc.contains(searchQuery, ignoreCase = true)
            val matchEv = !isEvFilterOnly || vehicle.isEv
            
            val matchAvailable = if (selectedDayFilter == "Any Day" || !showOnlyFullyAvailable) {
                true
            } else {
                // Return true only if there is NO ACTIVE booking for this vehicle on this day
                val hasBooking = bookings.any {
                    it.vehicleId == vehicle.id && 
                    it.bookingDate.equals(selectedDayFilter, ignoreCase = true) &&
                    it.status != "Cancelled"
                }
                !hasBooking
            }
            
            matchType && matchSearch && matchEv && matchAvailable
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
                .padding(top = 10.dp, bottom = 4.dp)
                .padding(horizontal = 16.dp),
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

        // Horizontal Day Selector Carousel & Switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Day:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = SlateBlueText.copy(alpha = 0.7f),
                modifier = Modifier.padding(end = 4.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val daysList = listOf("Any Day", "Today", "Tomorrow", "Day After")
                items(daysList) { dayOption ->
                    val isSel = selectedDayFilter == dayOption
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) OceanLight else SoftGray)
                            .border(
                                width = 1.dp,
                                color = if (isSel) OceanBlue else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedDayFilter = dayOption }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayOption,
                            color = if (isSel) OceanBlue else SlateBlueText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Optional "Available only" filter toggle
            if (selectedDayFilter != "Any Day") {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (showOnlyFullyAvailable) MintGreen.copy(alpha = 0.15f) else SoftGray)
                        .border(
                            width = 1.dp,
                            color = if (showOnlyFullyAvailable) MintGreen else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { showOnlyFullyAvailable = !showOnlyFullyAvailable }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Show fully available",
                            tint = if (showOnlyFullyAvailable) MintGreen else SlateBlueText.copy(alpha = 0.5f),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Free Only",
                            fontSize = 10.sp,
                            color = if (showOnlyFullyAvailable) MintGreen else SlateBlueText,
                            fontWeight = FontWeight.Black
                        )
                    }
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
                        bookings = bookings,
                        selectedDayFilter = selectedDayFilter,
                        profile = profile,
                        reviews = reviews,
                        onBookClicked = { prefilledDay -> onBookClicked(vehicle, prefilledDay) },
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
    bookings: List<BookingEntity>,
    selectedDayFilter: String,
    profile: ProfileEntity?,
    reviews: List<ReviewEntity> = emptyList(),
    onBookClicked: (String?) -> Unit,
    config: CountryConfig
) {
    val isMine = profile != null && vehicle.ownerName == profile.name && vehicle.ownerBlock == profile.block

    // Dynamic rating calculations
    val vehicleReviews = remember(reviews, vehicle.id) {
        reviews.filter { it.vehicleId == vehicle.id && it.reviewerRole == "Renter" }
    }
    val avgVehicleRating = remember(vehicleReviews) {
        if (vehicleReviews.isEmpty()) 5.0 else vehicleReviews.map { it.rating }.average()
    }

    val ownerHostReviews = remember(reviews, vehicle.ownerName) {
        reviews.filter { it.revieweeName == vehicle.ownerName && it.reviewerRole == "Renter" }
    }
    val avgOwnerRating = remember(ownerHostReviews) {
        if (ownerHostReviews.isEmpty()) 4.8 else ownerHostReviews.map { it.rating }.average()
    }

    var showReviewsSection by remember { mutableStateOf(false) }

    fun formatHour(hour: Int): String {
        val h = when {
            hour == 0 || hour == 24 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        val ampm = if (hour >= 12 && hour < 24) "PM" else "AM"
        return "$h $ampm"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vehicle_card_${vehicle.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, OceanBlue.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                        .clip(RoundedCornerShape(6.dp))
                        .background(SlateDark.copy(alpha = 0.82f))
                        .border(1.dp, OceanBlue.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .align(Alignment.TopStart)
                ) {
                    Text(
                        text = vehicle.type.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = OceanBlue,
                        letterSpacing = 1.sp
                    )
                }

                // If my own ride tag
                if (isMine) {
                    Box(
                        modifier = Modifier
                            .padding(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(OceanBlue)
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                            .align(Alignment.TopEnd)
                    ) {
                        Text(
                            text = "MY LISTING",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // EV Badge
                if (vehicle.isEv) {
                    Box(
                        modifier = Modifier
                            .padding(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF2E7D32).copy(alpha = 0.9f))
                            .border(1.dp, Color(0xFFADC2A9).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
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
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "EV",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 0.5.sp
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
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            letterSpacing = 0.2.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.LocationOn,
                                contentDescription = "Owner Location",
                                tint = OceanBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Lent by ${vehicle.ownerName} (★ ${String.format(java.util.Locale.US, "%.1f", avgOwnerRating)} • ${config.blockLabel} ${vehicle.ownerBlock} • ${config.flatLabel} ${vehicle.ownerFlat})",
                                fontSize = 11.5.sp,
                                color = SlateBlueText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Price Chip
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${config.currency}${vehicle.rentPerHour.toInt()}",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = OceanBlue
                        )
                        Text(
                            text = "/ hour",
                            fontSize = 9.5.sp,
                            color = SlateBlueText,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                HorizontalDivider(color = SlateBlueText.copy(alpha = 0.12f))

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = vehicle.desc,
                    fontSize = 12.5.sp,
                    color = SlateBlueText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 4.dp),
                    lineHeight = 17.sp
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showReviewsSection = !showReviewsSection }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = "Rating",
                        tint = WarmAmber,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${String.format(java.util.Locale.US, "%.1f", avgVehicleRating)} ★ (${vehicleReviews.size} ${if (vehicleReviews.size == 1) "review" else "reviews"})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = OceanBlue
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showReviewsSection) "• Hide Reviews ▴" else "• Show Reviews ▾",
                        fontSize = 11.5.sp,
                        color = SlateBlueText.copy(alpha = 0.5f)
                    )
                }

                if (showReviewsSection) {
                    if (vehicleReviews.isEmpty()) {
                        Text(
                            "No reviews written yet. Be the first to rent and rate!",
                            fontSize = 11.sp,
                            color = SlateBlueText.copy(alpha = 0.5f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                                .background(OceanLight.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            vehicleReviews.forEach { rev ->
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = rev.reviewerName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Row {
                                            repeat(rev.rating) {
                                                Icon(Icons.Filled.Star, contentDescription = null, tint = WarmAmber, modifier = Modifier.size(10.dp))
                                            }
                                            repeat(5 - rev.rating) {
                                                Icon(Icons.Outlined.StarOutline, contentDescription = null, tint = SlateBlueText.copy(alpha = 0.3f), modifier = Modifier.size(10.dp))
                                            }
                                        }
                                    }
                                    if (rev.comment.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "\"${rev.comment}\"",
                                            fontSize = 11.sp,
                                            color = SlateBlueText,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Date-Aware Live Availability Badge Option
                if (selectedDayFilter != "Any Day") {
                    val activeBookings = remember(bookings, vehicle.id, selectedDayFilter) {
                        bookings.filter {
                            it.vehicleId == vehicle.id &&
                            it.bookingDate.equals(selectedDayFilter, ignoreCase = true) &&
                            it.status != "Cancelled"
                        }
                    }

                    val totalHoursBooked = activeBookings.sumOf { it.hours }
                    val isFullyBooked = totalHoursBooked >= 10

                    val indicatorColor = when {
                        activeBookings.isEmpty() -> MintGreen
                        isFullyBooked -> AccentCoral
                        else -> WarmAmber
                    }

                    val bgTint = indicatorColor.copy(alpha = 0.08f)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(bgTint)
                            .border(1.dp, indicatorColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(indicatorColor)
                                )
                                Text(
                                    text = when {
                                        activeBookings.isEmpty() -> "🟢 Fully Available $selectedDayFilter"
                                        isFullyBooked -> "🔴 Fully Booked $selectedDayFilter"
                                        else -> "🟡 Partially Booked ($totalHoursBooked hrs reserved)"
                                    },
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateDark
                                )
                            }

                            if (activeBookings.isNotEmpty()) {
                                // Display formatted busy slots compactly
                                val busySlotsText = activeBookings.map { b ->
                                    val startStr = formatHour(b.startHour)
                                    val endStr = formatHour(b.startHour + b.hours)
                                    "$startStr-$endStr"
                                }.joinToString(", ")

                                Text(
                                    text = "Busy: $busySlotsText",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SlateBlueText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            } else {
                                Text(
                                    text = "Tap to rent",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MintGreen
                                )
                            }
                        }
                    }
                }

                // Status and Info Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.DateRange,
                                contentDescription = "Availability Details",
                                tint = SlateBlueText.copy(alpha = 0.6f),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = vehicle.availabilityHours,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SlateBlueText
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = "Reg No",
                                tint = SlateBlueText.copy(alpha = 0.6f),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(OceanLight)
                                    .border(1.dp, OceanBlue.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = vehicle.regNumber,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = SlateDark,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }

                    if (vehicle.status == "Rented") {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AccentCoral.copy(alpha = 0.15f))
                                .border(1.dp, AccentCoral.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                "RENTED OUT",
                                color = AccentCoral,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
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
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("YOURS", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        }
                    } else {
                        Button(
                            onClick = { onBookClicked(if (selectedDayFilter == "Any Day") null else selectedDayFilter) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OceanBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.testTag("book_button_${vehicle.id}")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("RENT", fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class LuxuryBannerConfig(
    val gradientColors: List<Color>,
    val icon: java.lang.Object?, // We will use Any or just specify elements directly
    val iconVector: ImageVector,
    val tintColor: Color,
    val performanceLabel: String
)

// Custom vehicle drawing using Compose Canvas (no assets dependencies)
@Composable
fun VehicleVisualBanner(type: String) {
    val config = when (type) {
        "Car" -> LuxuryBannerConfig(
            listOf(Color(0xFFFAF7F2), Color(0xFFECE5D8)), // Exquisite Alabaster Cream
            null,
            Icons.Filled.DirectionsCar,
            OceanBlue, // Miami Riviera Sky Blue (Primary)
            "4.0L TWIN-TURBO FLAT-6 • ACTIVE AERODYNAMICS"
        )
        "Bike" -> LuxuryBannerConfig(
            listOf(Color(0xFFE0F2FE), Color(0xFFF5F9FC)), // Ice-cream Sky Blue satin
            null,
            Icons.Filled.TwoWheeler,
            Color(0xFF0284C7), // Sky Blue Custom
            "1250CC DESMOSEDICI REVO SPORT SPECIAL"
        )
        "Scooter" -> LuxuryBannerConfig(
            listOf(Color(0xFFF0FDF4), Color(0xFFE0F2FE)), // Mint & Sky Cream
            null,
            Icons.Filled.Moped,
            Color(0xFF0D9488), // Sky Mint Emerald
            "75KW SOLID-STATE ULTRA HIGH EFFICIENCY"
        )
        "Bicycle" -> LuxuryBannerConfig(
            listOf(Color(0xFFFFF1F2), Color(0xFFFCFAF7)), // Sand & Track Rose
            null,
            Icons.Filled.PedalBike,
            Color(0xFFF43F5E), // Performance Coral Red
            "CARBON MONOCOQUE SPORT CHASSIS"
        )
        else -> LuxuryBannerConfig(
            listOf(Color(0xFFFAF7F2), Color(0xFFE0F2FE)), // Cream & Sky
            null,
            Icons.Filled.ElectricScooter,
            Color(0xFF0284C7),
            "INTELLIGENT INTEGRATED KINETIC REC"
        )
    }

    // Interactive continuous luxury movement (real-time HD dynamic shaders simulation)
    val infiniteTransition = rememberInfiniteTransition(label = "luxury_motion")
    
    val xOffsetPulse by infiniteTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spotlight_beam"
    )

    val floatingParallax by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floating_drift"
    )

    val scaleGlow by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "neon_glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(config.gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        // Draw highly sleek high-end luxury dynamic custom 3D vector lines
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Sleek luxury concentric chassis contour rings (simulating 3D depth of luxury curves)
            val centerX = width / 2f
            val centerY = height / 2f
            val baseRadius = 55.dp.toPx()
            
            for (i in 1..3) {
                drawCircle(
                    color = config.tintColor.copy(alpha = 0.04f * i * scaleGlow),
                    radius = baseRadius * (1f + i * 0.3f * scaleGlow),
                    center = Offset(centerX, centerY + floatingParallax),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // 2. High-HD precision instrument technical grid lines (car speedo / chrono lines)
            val columns = 16
            val stepX = width / columns
            for (i in 0..columns) {
                val alphaVal = if (i % 4 == 0) 0.07f else 0.02f
                drawLine(
                    color = config.tintColor.copy(alpha = alphaVal),
                    start = Offset(i * stepX, 0f),
                    end = Offset(i * stepX, height),
                    strokeWidth = if (i % 4 == 0) 1.5.dp.toPx() else 1.dp.toPx()
                )
            }

            // 3. Dynamic Moving "3D Laser reflection beam" sliding smoothly across representing polished body
            val beamX1 = width * xOffsetPulse
            val beamX2 = beamX1 + (width * 0.25f)
            val pathBeam = androidx.compose.ui.graphics.Path().apply {
                moveTo(beamX1, 0f)
                lineTo(beamX2, 0f)
                lineTo(beamX2 - 80.dp.toPx(), height)
                lineTo(beamX1 - 80.dp.toPx(), height)
                close()
            }
            drawPath(
                path = pathBeam,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.0f),
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.0f)
                    ),
                    start = Offset(beamX1, 0f),
                    end = Offset(beamX2, 0f)
                )
            )

            // 4. Wind tunnel aerodynamic air flow ribbon curves (racing stream)
            val streamPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, height * 0.65f + floatingParallax)
                cubicTo(
                    width * 0.3f, height * 0.45f - floatingParallax,
                    width * 0.7f, height * 0.85f + floatingParallax * 1.5f,
                    width, height * 0.55f - floatingParallax
                )
            }
            drawPath(
                path = streamPath,
                color = config.tintColor.copy(alpha = 0.2f),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )

            // Dynamic bottom pristine titanium trim strip
            drawLine(
                color = config.tintColor.copy(alpha = 0.3f),
                start = Offset(0f, height - 4.dp.toPx()),
                end = Offset(width, height - 4.dp.toPx()),
                strokeWidth = 3.dp.toPx()
              )
        }

        // Overlay of performance metadata
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = config.performanceLabel,
                color = SlateBlueText.copy(alpha = 0.4f),
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
        }

        // Circular premium executive badge wrapper (Porsche style center logo in pearl white)
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFFFFFFF), Color(0xFFF3F1EA)),
                        radius = 120f
                    )
                )
                .border(1.5.dp, config.tintColor.copy(alpha = 0.6f), CircleShape)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = config.iconVector,
                contentDescription = type,
                tint = config.tintColor,
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
    var isEvState by remember { mutableStateOf(false) }

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
                    Spacer(modifier = Modifier.height(12.dp))

                    // EV Switch Toggle Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isEvState = !isEvState }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Filled.Bolt,
                                contentDescription = "Electric Vehicle Icon",
                                tint = if (isEvState) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    "Is Electric Vehicle (EV)?",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Toggle on if this vehicle runs on electric battery power.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                        Switch(
                            checked = isEvState,
                            onCheckedChange = { isEvState = it },
                            modifier = Modifier.testTag("host_ev_switch")
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    val canSubmit = offerModel.isNotBlank() && offerReg.isNotBlank() && offerRentStr.isNotBlank() && profile != null && profile.name.isNotBlank() && profile.isVerified
                    Button(
                        onClick = {
                            val rentDouble = offerRentStr.toDoubleOrNull() ?: 0.0
                            onAddVehicle(offerModel, selectedType, offerReg, rentDouble, offerHours, offerDesc, isEvState)
                            // Clean details after submit
                            offerModel = ""
                            offerReg = ""
                            offerRentStr = ""
                            offerDesc = ""
                            isEvState = false
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
    onSubmitReview: (Long, Long, String, String, String, String, Int, String) -> Unit,
    reviews: List<ReviewEntity> = emptyList(),
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
                        BorrowerBookingCard(
                            booking = booking,
                            onCancel = { onUpdateBookingStatus(booking, "Cancelled") },
                            onSubmitReview = onSubmitReview,
                            reviews = reviews,
                            config = config
                        )
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
                            onSubmitReview = onSubmitReview,
                            reviews = reviews,
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
    onSubmitReview: (Long, Long, String, String, String, String, Int, String) -> Unit,
    reviews: List<ReviewEntity> = emptyList(),
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

            if (booking.status == "Completed") {
                val myReview = remember(reviews, booking.id) {
                    reviews.find { it.bookingId == booking.id && it.reviewerRole == "Renter" }
                }

                if (myReview != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        colors = CardDefaults.cardColors(containerColor = OceanLight.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MintGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("You rated this ride:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SlateDark)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                repeat(myReview.rating) {
                                    Icon(Icons.Filled.Star, contentDescription = null, tint = WarmAmber, modifier = Modifier.size(14.dp))
                                }
                                repeat(5 - myReview.rating) {
                                    Icon(Icons.Outlined.StarOutline, contentDescription = null, tint = SlateBlueText.copy(alpha = 0.3f), modifier = Modifier.size(14.dp))
                                }
                            }
                            if (myReview.comment.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("\"${myReview.comment}\"", fontSize = 12.sp, color = SlateBlueText, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .background(WarmAmber.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .border(1.dp, WarmAmber.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            "How was your ride with ${booking.ownerName}?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Help neighbor community by rating and detailing your trip.",
                            fontSize = 11.5.sp,
                            color = SlateBlueText.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        var ratingChosen by remember { mutableStateOf(5) }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            (1..5).forEach { star ->
                                IconButton(
                                    onClick = { ratingChosen = star },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (star <= ratingChosen) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                        contentDescription = "$star Stars",
                                        tint = if (star <= ratingChosen) WarmAmber else SlateBlueText.copy(alpha = 0.4f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$ratingChosen / 5 Stars",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateDark
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        var writtenComment by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = writtenComment,
                            onValueChange = { writtenComment = it },
                            placeholder = { Text("What made it great or how can they improve? (Optional)", fontSize = 11.5.sp) },
                            modifier = Modifier.fillMaxWidth().testTag("renter_review_text_${booking.id}"),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                            maxLines = 3,
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                onSubmitReview(
                                    booking.id,
                                    booking.vehicleId,
                                    booking.vehicleModel,
                                    booking.renterName,
                                    booking.ownerName,
                                    "Renter",
                                    ratingChosen,
                                    writtenComment
                                )
                            },
                            modifier = Modifier.fillMaxWidth().testTag("submit_renter_review_btn_${booking.id}"),
                            colors = ButtonDefaults.buttonColors(containerColor = OceanBlue)
                        ) {
                            Text("Submit My Review", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
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
    onSubmitReview: (Long, Long, String, String, String, String, Int, String) -> Unit,
    reviews: List<ReviewEntity> = emptyList(),
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

            if (booking.status == "Completed") {
                val myLenderReview = remember(reviews, booking.id) {
                    reviews.find { it.bookingId == booking.id && it.reviewerRole == "Owner" }
                }

                if (myLenderReview != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        colors = CardDefaults.cardColors(containerColor = OceanLight.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MintGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("You rated Renter:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SlateDark)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                repeat(myLenderReview.rating) {
                                    Icon(Icons.Filled.Star, contentDescription = null, tint = WarmAmber, modifier = Modifier.size(14.dp))
                                }
                                repeat(5 - myLenderReview.rating) {
                                    Icon(Icons.Outlined.StarOutline, contentDescription = null, tint = SlateBlueText.copy(alpha = 0.3f), modifier = Modifier.size(14.dp))
                                }
                            }
                            if (myLenderReview.comment.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("\"${myLenderReview.comment}\"", fontSize = 12.sp, color = SlateBlueText, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .background(WarmAmber.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .border(1.dp, WarmAmber.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            "Rate Renter: ${booking.renterName}?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "How did they treat your vehicle? Did they return on time?",
                            fontSize = 11.5.sp,
                            color = SlateBlueText.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        var ratingChosen by remember { mutableStateOf(5) }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            (1..5).forEach { star ->
                                IconButton(
                                    onClick = { ratingChosen = star },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (star <= ratingChosen) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                        contentDescription = "$star Stars",
                                        tint = if (star <= ratingChosen) WarmAmber else SlateBlueText.copy(alpha = 0.4f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$ratingChosen / 5 Stars",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateDark
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        var writtenComment by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = writtenComment,
                            onValueChange = { writtenComment = it },
                            placeholder = { Text("How was the hand-off & return experience? (Optional)", fontSize = 11.5.sp) },
                            modifier = Modifier.fillMaxWidth().testTag("owner_review_text_${booking.id}"),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                            maxLines = 3,
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                onSubmitReview(
                                    booking.id,
                                    booking.vehicleId,
                                    booking.vehicleModel,
                                    booking.ownerName,
                                    booking.renterName,
                                    "Owner",
                                    ratingChosen,
                                    writtenComment
                                )
                            },
                            modifier = Modifier.fillMaxWidth().testTag("submit_owner_review_btn_${booking.id}"),
                            colors = ButtonDefaults.buttonColors(containerColor = OceanBlue)
                        ) {
                            Text("Submit Renter Review", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }
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
    onLogout: () -> Unit = {},
    reviews: List<ReviewEntity> = emptyList(),
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
            // Visual Residency Poster (Passport Card Style - Executive Yacht / Sports Club Member Card)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = if (isVerifiedNow) {
                                listOf(Color(0xFF0EA5E9), Color(0xFF0284C7)) // Miami Sky Blue Gradient
                            } else {
                                listOf(Color(0xFFFAF7F2), Color(0xFFECE5D8)) // Premium Alabaster Cream
                            }
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = if (isVerifiedNow) Color.White.copy(alpha = 0.35f) else SlateBlueText.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(22.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                "NEARBYDRIVE SOCIETY ID",
                                color = if (isVerifiedNow) Color.White.copy(alpha = 0.75f) else SlateBlueText.copy(alpha = 0.62f),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                name.ifBlank { "GUEST RESIDENT" }.uppercase(),
                                color = if (isVerifiedNow) Color.White else SlateDark,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Icon(
                                if (isVerifiedNow) Icons.Filled.CheckCircle else Icons.Filled.Badge,
                                contentDescription = "Verification Badge",
                                tint = if (isVerifiedNow) Color.White else SlateBlueText.copy(alpha = 0.5f),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                if (isVerifiedNow) "ACTIVE CREDENTIAL" else "UNVERIFIED STATUS",
                                color = if (isVerifiedNow) Color.White.copy(alpha = 0.9f) else AccentCoral,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "HOUSING ${config.blockLabel.uppercase()}",
                                color = if (isVerifiedNow) Color.White.copy(alpha = 0.6f) else SlateBlueText.copy(alpha = 0.6f),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                block.ifBlank { "Unassigned" }.uppercase(),
                                color = if (isVerifiedNow) Color.White else SlateDark,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text(
                                "${config.flatLabel.uppercase()} UNITS",
                                color = if (isVerifiedNow) Color.White.copy(alpha = 0.6f) else SlateBlueText.copy(alpha = 0.6f),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                flat.ifBlank { "Pending" }.uppercase(),
                                color = if (isVerifiedNow) Color.White else SlateDark,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "VIP MEMBER LEVEL",
                                color = if (isVerifiedNow) Color.White.copy(alpha = 0.6f) else SlateBlueText.copy(alpha = 0.6f),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                if (isVerifiedNow) "GOLD EXECUTIVE" else "PROVISIONAL",
                                color = if (isVerifiedNow) Color.White else SlateBlueText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
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

                    OutlinedButton(
                        onClick = { onLogout() },
                        modifier = Modifier.fillMaxWidth().testTag("profile_logout_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentCoral),
                        border = BorderStroke(1.5.dp, AccentCoral.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.ExitToApp, contentDescription = "Exit icon", tint = AccentCoral)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign Out of NearbyDrive", fontWeight = FontWeight.Bold)
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

        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("firebase_sync_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Cloud,
                            contentDescription = "Cloud Database Sync Status icon",
                            tint = OceanBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Firebase Cloud Database Sync",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = OceanBlue
                        )
                    }

                    val configured = FirebaseSyncManager.isConfigured()
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (configured) MintGreen.copy(alpha = 0.12f) else SoftGray)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (configured) MintGreen else SlateBlueText.copy(alpha = 0.5f))
                            )
                            Text(
                                text = if (configured) "CONNECTED & SYNCHRONIZING" else "OFFLINE LOCAL DATABASE MODE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (configured) MintGreen else SlateBlueText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "When connected, custom rides and bookings are saved to your global community cloud, keeping neighbors synchronized across all active devices in real-time.",
                        fontSize = 12.sp,
                        color = SlateBlueText,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    HorizontalDivider(color = SoftGray)

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "HOW TO CONNECT YOUR OWN BACKEND:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateBlueText.copy(alpha = 0.7f),
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "1. Open the Secrets Panel on the left sidebar in Google AI Studio.\n" +
                               "2. Add the following environment keys with your Firebase project properties:\n" +
                               "   • FIREBASE_API_KEY\n" +
                               "   • FIREBASE_PROJECT_ID\n" +
                               "   • FIREBASE_APPLICATION_ID\n" +
                               "3. Recompile the app — dynamic initialization handles the connection out-of-the-box!",
                        fontSize = 11.sp,
                        color = SlateDark,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        item {
            // Dynamic rating calculations for this user
            val userReviews = remember(reviews, profile?.name) {
                reviews.filter { it.revieweeName.equals(profile?.name ?: "", ignoreCase = true) }
            }

            val reviewsAsHost = remember(userReviews) {
                userReviews.filter { it.reviewerRole == "Renter" }
            }

            val reviewsAsRider = remember(userReviews) {
                userReviews.filter { it.reviewerRole == "Owner" }
            }

            val avgHostRating = remember(reviewsAsHost) {
                if (reviewsAsHost.isEmpty()) 4.8 else reviewsAsHost.map { it.rating }.average()
            }

            val avgRiderRating = remember(reviewsAsRider) {
                if (reviewsAsRider.isEmpty()) 4.9 else reviewsAsRider.map { it.rating }.average()
            }

            Card(
                modifier = Modifier.fillMaxWidth().testTag("profile_ratings_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Community Trust Ratings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = OceanBlue,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Rating as Host Card segment
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(OceanLight.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("AS LENDER", fontSize = 10.sp, fontWeight = FontWeight.Black, color = SlateBlueText.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Star, contentDescription = null, tint = WarmAmber, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = String.format(java.util.Locale.US, "%.1f", avgHostRating),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateDark
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("${reviewsAsHost.size} reviews", fontSize = 11.sp, color = SlateBlueText.copy(alpha = 0.6f))
                        }

                        // Rating as Rider Card segment
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(OceanLight.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("AS BORROWER", fontSize = 10.sp, fontWeight = FontWeight.Black, color = SlateBlueText.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Star, contentDescription = null, tint = WarmAmber, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = String.format(java.util.Locale.US, "%.1f", avgRiderRating),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateDark
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("${reviewsAsRider.size} reviews", fontSize = 11.sp, color = SlateBlueText.copy(alpha = 0.6f))
                        }
                    }

                    if (userReviews.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = SlateBlueText.copy(alpha = 0.08f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            "Recent Written Reviews (${userReviews.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = SlateDark,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            userReviews.take(5).forEach { rev ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(SoftGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "By ${rev.reviewerName} (${if (rev.reviewerRole == "Renter") "Borrower" else "Lender"})",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SlateDark
                                        )
                                        Row {
                                            repeat(rev.rating) {
                                                Icon(Icons.Filled.Star, contentDescription = null, tint = WarmAmber, modifier = Modifier.size(11.dp))
                                            }
                                            repeat(5 - rev.rating) {
                                                Icon(Icons.Outlined.StarOutline, contentDescription = null, tint = SlateBlueText.copy(alpha = 0.3f), modifier = Modifier.size(11.dp))
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Ride: ${rev.vehicleModel}",
                                        fontSize = 10.5.sp,
                                        color = SlateBlueText.copy(alpha = 0.6f)
                                    )
                                    if (rev.comment.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "\"${rev.comment}\"",
                                            fontSize = 11.5.sp,
                                            color = SlateDark
                                        )
                                    }
                                }
                            }
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
// POP-UP DIALOG: SOCIETY INTERCOM & MYGATE ERP REGISTER VERIFICATION
// ----------------------------------------------------
@Composable
fun SocietyVerificationDialog(
    block: String,
    flat: String,
    onDismiss: () -> Unit,
    onVerificationSuccess: () -> Unit,
    config: CountryConfig
) {
    var verificationMethod by remember { mutableStateOf("mygate") } // "mygate" vs "intercom"
    var myGateSubMethod by remember { mutableStateOf("phone") } // "phone" vs "passcode"
    
    var phoneNumber by remember { mutableStateOf("") }
    var isOtpSent by remember { mutableStateOf(false) }
    var otpCodeEntered by remember { mutableStateOf("") }
    
    var partnerApiKey by remember { mutableStateOf("MYGATE-PROP-982W") }
    var activePasscodeEntered by remember { mutableStateOf("") }
    
    var localIntercomPin by remember { mutableStateOf("") }
    
    var isVerifying by remember { mutableStateOf(false) }
    var progressStatus by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()
    
    // Live Http / API payload logs terminal state
    val apiLogs = remember { mutableStateListOf<String>() }
    val apiTerminalScrollState = rememberScrollState()

    fun logApiCall(msg: String) {
        apiLogs.add(msg)
    }

    Dialog(onDismissRequest = if (isVerifying) ({}) else onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("verification_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.VerifiedUser,
                            contentDescription = "Verify",
                            tint = OceanBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Resident Verification",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateDark
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp),
                        enabled = !isVerifying
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = SlateBlueText.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Verify your account to rent or list neighbor vehicles. We support standard guard intercom matching or direct MyGate ERP Sync to automatically pull verified flat residency.",
                    fontSize = 11.5.sp,
                    color = SlateBlueText,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Tab selectors: MyGate ERP vs Intercom
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SoftGray)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        "mygate" to "MyGate ERP Sync 🎪",
                        "intercom" to "Guard Intercom PIN"
                    ).forEach { (method, label) ->
                        val isSelected = verificationMethod == method
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) OceanBlue else Color.Transparent)
                                .clickable { 
                                    if (!isVerifying) {
                                        verificationMethod = method 
                                        errorMessage = ""
                                    }
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else SlateBlueText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Render Verification Methods
                if (verificationMethod == "mygate") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Sub-options: OTP or Passcode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Verification Mode:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateBlueText
                            )
                            
                            // Radio Option Phone
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable(enabled = !isVerifying) { myGateSubMethod = "phone" }
                            ) {
                                RadioButton(
                                    selected = myGateSubMethod == "phone",
                                    onClick = { myGateSubMethod = "phone" },
                                    enabled = !isVerifying,
                                    colors = RadioButtonDefaults.colors(selectedColor = OceanBlue)
                                )
                                Text("MyGate OTP", fontSize = 11.5.sp, color = SlateDark, fontWeight = if (myGateSubMethod == "phone") FontWeight.Bold else FontWeight.Normal)
                            }

                            // Radio Option Passcode
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable(enabled = !isVerifying) { myGateSubMethod = "passcode" }
                            ) {
                                RadioButton(
                                    selected = myGateSubMethod == "passcode",
                                    onClick = { myGateSubMethod = "passcode" },
                                    enabled = !isVerifying,
                                    colors = RadioButtonDefaults.colors(selectedColor = OceanBlue)
                                )
                                Text("App Passcode", fontSize = 11.5.sp, color = SlateDark, fontWeight = if (myGateSubMethod == "passcode") FontWeight.Bold else FontWeight.Normal)
                            }
                        }

                        if (myGateSubMethod == "phone") {
                            // Phone OTP flow
                            if (!isOtpSent) {
                                Text(
                                    text = "Send an API lookup handshake request to MyGate resident registry matching your registered phone number.",
                                    fontSize = 11.sp,
                                    color = SlateBlueText.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                OutlinedTextField(
                                    value = phoneNumber,
                                    onValueChange = { phoneNumber = it },
                                    label = { Text("MyGate Phone Number") },
                                    placeholder = { Text(config.phonePlaceholder) },
                                    modifier = Modifier.fillMaxWidth().testTag("mygate_phone_input"),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OceanBlue)
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = partnerApiKey,
                                    onValueChange = { partnerApiKey = it },
                                    label = { Text("MyGate Society Partner Key (Optional)") },
                                    placeholder = { Text("e.g. MYGATE-PROP-982W") },
                                    modifier = Modifier.fillMaxWidth().testTag("mygate_partner_key"),
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OceanBlue)
                                )
                            } else {
                                Text(
                                    text = "We found Flat ${config.blockLabel} $block, ${config.flatLabel} $flat tied to phone in MyGate integration database. Please enter the 4-digit security OTP sent (simulated code: 7719).",
                                    fontSize = 11.sp,
                                    color = SlateBlueText,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                OutlinedTextField(
                                    value = otpCodeEntered,
                                    onValueChange = { 
                                        if (it.length <= 4) {
                                            otpCodeEntered = it.filter { char -> char.isDigit() }
                                        }
                                    },
                                    label = { Text("MyGate 4-digit OTP") },
                                    placeholder = { Text("e.g. 7719") },
                                    modifier = Modifier.fillMaxWidth().testTag("mygate_otp_input"),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OceanBlue)
                                )
                            }
                        } else {
                            // Passcode flow
                            Text(
                                "Generate a pre-approved resident delivery or service passcode directly inside your MyGate App ('Invite Guest' -> 'Delivery PIN' or 'Service entry token'), then input it below.",
                                fontSize = 11.sp,
                                color = SlateBlueText.copy(alpha = 0.8f),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            OutlinedTextField(
                                value = activePasscodeEntered,
                                onValueChange = { 
                                    if (it.length <= 8) {
                                        activePasscodeEntered = it
                                    }
                                },
                                label = { Text("MyGate Active Passcode") },
                                placeholder = { Text("e.g. 529014") },
                                modifier = Modifier.fillMaxWidth().testTag("mygate_passcode_input"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OceanBlue)
                            )
                        }
                    }
                } else {
                    // Classic intercom PIN flow
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "To keep NearbyDrive extremely secure for local neighbor-to-neighbor sharing, we run an intercom verification matching log. For this pilot trial, we simulate connecting to the guard intercom matching console at ${config.blockLabel} $block, ${config.flatLabel} $flat. Enter any 4-digit PIN (e.g., 1234) to verify.",
                            fontSize = 11.sp,
                            color = SlateBlueText,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        OutlinedTextField(
                            value = localIntercomPin,
                            onValueChange = { 
                                if (it.length <= 4) {
                                    localIntercomPin = it.filter { char -> char.isDigit() }
                                }
                            },
                            label = { Text("Intercom Verification PIN") },
                            placeholder = { Text("e.g. 1234") },
                            modifier = Modifier.fillMaxWidth().testTag("verification_pin_input"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OceanBlue)
                        )
                    }
                }

                if (errorMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage,
                        color = AccentCoral,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Dynamic Live API logs terminal container
                if (apiLogs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "MyGate ERP Partnership Live API Log Output:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = OceanBlue
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E1E2E))
                            .border(1.dp, Color(0xFF3B3B4F), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(apiTerminalScrollState),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            apiLogs.forEach { log ->
                                Text(
                                    text = log,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = when {
                                        log.startsWith("[OUTBOUND]") -> Color(0xFFFDBA74) // Orange
                                        log.startsWith("[RESPONSE]") -> Color(0xFF81E6D9) // Cyan
                                        log.startsWith("[SUCCESS]") -> Color(0xFFA3E635) // Light Green
                                        else -> Color(0xFFE2E8F0) // Gray
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons area
                if (isVerifying) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = OceanBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = progressStatus,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = OceanBlue
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).testTag("verification_cancel_btn")
                        ) {
                            Text("Cancel", color = SlateBlueText, fontSize = 12.5.sp)
                        }

                        Button(
                            onClick = {
                                if (verificationMethod == "mygate") {
                                    if (myGateSubMethod == "phone") {
                                        if (!isOtpSent) {
                                            if (phoneNumber.isBlank()) {
                                                errorMessage = "Please enter your MyGate registered phone number"
                                            } else {
                                                isVerifying = true
                                                progressStatus = "Initializing handshake with api.mygate.com..."
                                                errorMessage = ""
                                                scope.launch {
                                                    kotlinx.coroutines.delay(600)
                                                    logApiCall("[OUTBOUND] POST https://api.mygate.com/v3/auth/request-otp")
                                                    logApiCall("Headers: { X-Partner-Platform: \"NearbyDrive\", Authorization: \"Bearer ${partnerApiKey}\" }")
                                                    logApiCall("Payload: { \"phone\": \"${phoneNumber}\", \"block\": \"${block}\", \"flat\": \"${flat}\", \"timestamp\": ${System.currentTimeMillis()} }")
                                                    kotlinx.coroutines.delay(800)
                                                    logApiCall("[RESPONSE] 200 OK")
                                                    logApiCall("Response Body: { \"success\": true, \"api_version\": \"3.2.0\", \"otp_sent\": true, \"flat_registered_owners\": 1 }")
                                                    isOtpSent = true
                                                    isVerifying = false
                                                }
                                            }
                                        } else {
                                            if (otpCodeEntered != "7719" && otpCodeEntered.length != 4) {
                                                errorMessage = "Please enter valid simulated OTP (try 7719)"
                                            } else {
                                                isVerifying = true
                                                progressStatus = "Verifying MyGate database mapping..."
                                                errorMessage = ""
                                                scope.launch {
                                                    kotlinx.coroutines.delay(600)
                                                    logApiCall("[OUTBOUND] POST https://api.mygate.com/v3/auth/verify-otp")
                                                    logApiCall("Payload: { \"phone\": \"${phoneNumber}\", \"otp\": \"${otpCodeEntered}\", \"block\": \"${block}\", \"flat\": \"${flat}\" }")
                                                    kotlinx.coroutines.delay(800)
                                                    logApiCall("[RESPONSE] 200 OK")
                                                    logApiCall("[SUCCESS] Resident confirmed active in Greenwood Co-Op / Pinewood ERP list.")
                                                    logApiCall("Metadata: { \"resident_status\": \"VERIFIED\", \"type\": \"OWNER_TENANT\" }")
                                                    kotlinx.coroutines.delay(400)
                                                    onVerificationSuccess()
                                                }
                                            }
                                        }
                                    } else {
                                        // Passcode verification
                                        if (activePasscodeEntered.length < 5) {
                                            errorMessage = "Please enter a valid MyGate App generated passcode"
                                        } else {
                                            isVerifying = true
                                            progressStatus = "Validating passcode with MyGate secure API..."
                                            errorMessage = ""
                                            scope.launch {
                                                kotlinx.coroutines.delay(600)
                                                logApiCall("[OUTBOUND] POST https://api.mygate.com/v3/partner/validate-passcode")
                                                logApiCall("Payload: { \"passcode\": \"${activePasscodeEntered}\", \"block\": \"${block}\", \"flat\": \"${flat}\" }")
                                                kotlinx.coroutines.delay(1000)
                                                logApiCall("[RESPONSE] 200 OK")
                                                logApiCall("[SUCCESS] Passcode verified! Authorized partner session token issued.")
                                                kotlinx.coroutines.delay(500)
                                                onVerificationSuccess()
                                            }
                                        }
                                    }
                                } else {
                                    // Guard intercom PIN verification matches any 4 digit PIN
                                    if (localIntercomPin.length != 4) {
                                        errorMessage = "Please enter a valid 4-digit intercom PIN"
                                    } else {
                                        isVerifying = true
                                        progressStatus = "Pinging local gated intercom receiver..."
                                        errorMessage = ""
                                        scope.launch {
                                            kotlinx.coroutines.delay(600)
                                            progressStatus = "Verifying flat flat registry with Guard Console..."
                                            kotlinx.coroutines.delay(600)
                                            progressStatus = "PIN Verified successfully."
                                            kotlinx.coroutines.delay(300)
                                            onVerificationSuccess()
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                            modifier = Modifier.weight(1.5f).testTag("verification_submit_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (verificationMethod == "mygate" && !isOtpSent && myGateSubMethod == "phone") "Send OTP Sync" else "Verify Now",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
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
    prefilledDate: String? = null,
    prefilledStartHour: Int? = null,
    onDismiss: () -> Unit,
    onConfirmBooking: (Int, String, String, Int) -> Unit,
    config: CountryConfig
) {
    var rentHours by remember { mutableStateOf(3) }
    var bookingNotes by remember { mutableStateOf("") }
    var bookingDate by remember { mutableStateOf(prefilledDate ?: "Today") }
    var startHour by remember { mutableStateOf(prefilledStartHour ?: 9) }

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

                // Date Selector
                Text(
                    text = "When will you start?",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateBlueText.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Today", "Tomorrow", "Day After").forEach { day ->
                        val isSel = bookingDate == day
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) OceanBlue else OceanLight)
                                .clickable { bookingDate = day }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day,
                                color = if (isSel) Color.White else OceanBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Starting Hour Selector
                Text(
                    text = "Starting Hour:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateBlueText.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(16) { index ->
                        val hourVal = index + 7 // 7 AM to 10 PM
                        val hourStr = when {
                            hourVal == 12 -> "12 PM"
                            hourVal > 12 -> "${hourVal - 12} PM"
                            else -> "$hourVal AM"
                        }
                        val isSel = startHour == hourVal
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) OceanBlue else SoftGray)
                                .clickable { startHour = hourVal }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = hourStr,
                                color = if (isSel) Color.White else SlateBlueText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

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
                    onClick = { onConfirmBooking(rentHours, bookingNotes, bookingDate, startHour) },
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


@Composable
fun AvailabilityCalendarScreen(
    bookings: List<BookingEntity>,
    vehicles: List<VehicleEntity>,
    profile: ProfileEntity?,
    onBookSlotClicked: (VehicleEntity, String, Int) -> Unit,
    config: CountryConfig
) {
    var selectedVehicleId by remember { mutableStateOf<Long?>(null) }
    var selectedDate by remember { mutableStateOf("Today") }

    // Synchronize selectedVehicleId with first vehicle in the list if null
    LaunchedEffect(vehicles) {
        if (selectedVehicleId == null && vehicles.isNotEmpty()) {
            selectedVehicleId = vehicles.first().id
        }
    }

    val selectedVehicle = vehicles.find { it.id == selectedVehicleId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // App header intro
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(OceanLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.DateRange,
                    contentDescription = "Calendar Hub",
                    tint = OceanBlue,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Hourly Booking Hub 🗓️",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = SlateDark
                )
                Text(
                    text = "Track availability & secure key sharing hour-by-hour",
                    fontSize = 11.sp,
                    color = SlateBlueText.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Vehicles Horizontally Scrollable Selectable Row
        Text(
            text = "SELECT VEHICLE TO INSPECT SCHEDULE:",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = SlateBlueText.copy(alpha = 0.7f),
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        
        if (vehicles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SoftGray),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No vehicles shared in your community yet.\nGo to Host tab to offer a drive!",
                    fontSize = 12.sp,
                    color = SlateBlueText,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(vehicles.size) { index ->
                    val vehicle = vehicles[index]
                    val isSelected = vehicle.id == selectedVehicleId
                    
                    Card(
                        modifier = Modifier
                            .width(150.dp)
                            .clickable { selectedVehicleId = vehicle.id },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) OceanBlue else Color.White
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) OceanBlue else SlateBlueText.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Badge with Type
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isSelected) Color.White.copy(alpha = 0.2f) 
                                        else OceanLight
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = vehicle.type.uppercase(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isSelected) Color.White else OceanBlue
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = vehicle.model,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else SlateDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${config.currency}${vehicle.rentPerHour.toInt()}/hr",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) Color.White.copy(alpha = 0.9f) else SlateBlueText
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Date selection tabs
        Text(
            text = "CHOOSE DATE OFFSET:",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = SlateBlueText.copy(alpha = 0.7f),
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Today", "Tomorrow", "Day After").forEach { day ->
                val isSel = selectedDate == day
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSel) OceanLight else Color.White)
                        .clickable { selectedDate = day }
                        .border(
                            width = 1.dp,
                            color = if (isSel) OceanBlue else SlateBlueText.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day,
                        color = if (isSel) OceanBlue else SlateBlueText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Hourly booking timeline title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$selectedDate Hourly Timeline",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = SlateDark
            )
            // Color guides indicator
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LegendIndicator(color = MintGreen.copy(alpha = 0.15f), label = "Available")
                LegendIndicator(color = OceanBlue.copy(alpha = 0.15f), label = "Booked")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Now, design the hourly slots grid!
        if (selectedVehicle == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Please select a vehicle to display its live availability grid.",
                    fontSize = 12.sp,
                    color = SlateBlueText,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // Find active bookings on chosen day for chosen vehicle
            val relevantBookings = bookings.filter {
                it.vehicleId == selectedVehicle.id && 
                it.bookingDate.equals(selectedDate, ignoreCase = true) &&
                it.status != "Cancelled"
            }

            // Outer scrollable box containing hours
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // We inspect hours from 7:00 AM (7) to 10:00 PM (22)
                    items(16) { index ->
                        val hourVal = index + 7
                        val hourStr = when {
                            hourVal == 12 -> "12:00 PM"
                            hourVal > 12 -> "${hourVal - 12}:00 PM"
                            else -> "$hourVal:00 AM"
                        }
                        
                        // Check if this hour falls inside any active booking interval
                        val activeBooking = relevantBookings.find { booking ->
                            val start = booking.startHour
                            val end = start + booking.hours
                            hourVal >= start && hourVal < end
                        }

                        if (activeBooking != null) {
                            val isApproved = activeBooking.status == "Approved" || activeBooking.status == "Completed"
                            val statusColor = if (isApproved) OceanBlue else WarmAmber
                            val statusLabel = if (isApproved) "Booked" else "Pending Request"
                            val accentContainerColor = if (isApproved) OceanLight else WarmAmber.copy(alpha = 0.12f)
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = accentContainerColor),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = hourStr,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            color = statusColor,
                                            modifier = Modifier.width(75.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = if (isApproved) "Rented by ${activeBooking.renterName}" else "Requested by ${activeBooking.renterName}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SlateDark
                                            )
                                            Text(
                                                text = "${config.blockLabel} ${activeBooking.renterBlock} • ${config.flatLabel} ${activeBooking.renterFlat}",
                                                fontSize = 10.sp,
                                                color = SlateBlueText
                                            )
                                        }
                                    }
                                    
                                    // Status pill badge
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(statusColor)
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = statusLabel.uppercase(),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        } else {
                            // Available Hour Grid Cell
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onBookSlotClicked(selectedVehicle, selectedDate, hourVal)
                                    },
                                colors = CardDefaults.cardColors(containerColor = MintGreen.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MintGreen.copy(alpha = 0.15f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = hourStr,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MintGreen,
                                            modifier = Modifier.width(75.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Available to Reserve",
                                            fontSize = 12.sp,
                                            color = SlateBlueText,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    
                                    // Direct action arrow button
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "BOOK NOW",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MintGreen
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Filled.AddCircle,
                                            contentDescription = "Reserve Slot",
                                            tint = MintGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LegendIndicator(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 10.sp, color = SlateBlueText, fontWeight = FontWeight.Bold)
    }
}

