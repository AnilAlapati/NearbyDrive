package com.example.data

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NearbyDriveViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: VehicleRepository

    val allVehicles: StateFlow<List<VehicleEntity>>
    val allBookings: StateFlow<List<BookingEntity>>
    val profile: StateFlow<ProfileEntity?>

    // Persistent Country Selection
    private val prefs = application.getSharedPreferences("nearby_drive_prefs", Context.MODE_PRIVATE)
    private val _userCountry = MutableStateFlow(prefs.getString("selected_country", "IN") ?: "IN")
    val userCountry: StateFlow<String> = _userCountry.asStateFlow()

    fun setCountry(countryCode: String) {
        _userCountry.value = countryCode
        prefs.edit().putString("selected_country", countryCode).apply()
    }

    // Filter states
    private val _selectedTypeFilter = MutableStateFlow("All")
    val selectedTypeFilter: StateFlow<String> = _selectedTypeFilter.asStateFlow()

    private val _isEvFilterOnly = MutableStateFlow(false)
    val isEvFilterOnly: StateFlow<Boolean> = _isEvFilterOnly.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setEvFilter(isEvOnly: Boolean) {
        _isEvFilterOnly.value = isEvOnly
    }

    init {
        val database = AppDatabase.getDatabase(application)
        repository = VehicleRepository(database)

        profile = repository.profile.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        allVehicles = repository.allVehicles.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allBookings = repository.allBookings.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Seed initial data if database is empty
        viewModelScope.launch {
            repository.allVehicles.first().let { currentList ->
                if (currentList.isEmpty()) {
                    seedSampleData()
                }
            }
            // Populate a default profile if none exists
            val existingProfile = repository.getProfile()
            if (existingProfile == null) {
                repository.saveProfile(
                    ProfileEntity(
                        id = 1,
                        name = "Rohan Sharma",
                        block = "Tower B",
                        flat = "302",
                        phone = "+91 98765 43210",
                        isVerified = true,
                        tripsOffered = 15,
                        tripsTaken = 9,
                        ratingAsHost = 4.9,
                        ratingAsRider = 4.8
                    )
                )
            }
        }
    }

    fun setTypeFilter(type: String) {
        _selectedTypeFilter.value = type
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun saveProfile(
        name: String,
        block: String,
        flat: String,
        phone: String,
        isVerified: Boolean = false,
        tripsOffered: Int? = null,
        tripsTaken: Int? = null,
        ratingAsHost: Double? = null,
        ratingAsRider: Double? = null
    ) {
        viewModelScope.launch {
            val existing = repository.getProfile()
            repository.saveProfile(
                ProfileEntity(
                    id = 1,
                    name = name.trim(),
                    block = block.trim(),
                    flat = flat.trim(),
                    phone = phone.trim(),
                    isVerified = isVerified,
                    tripsOffered = tripsOffered ?: existing?.tripsOffered ?: 12,
                    tripsTaken = tripsTaken ?: existing?.tripsTaken ?: 8,
                    ratingAsHost = ratingAsHost ?: existing?.ratingAsHost ?: 4.8,
                    ratingAsRider = ratingAsRider ?: existing?.ratingAsRider ?: 4.9
                )
            )
        }
    }

    fun addVehicle(
        model: String,
        type: String,
        regNumber: String,
        rentPerHour: Double,
        availabilityHours: String,
        desc: String,
        isEv: Boolean = false,
        customOwnerName: String? = null,
        customBlock: String? = null,
        customFlat: String? = null,
        customPhone: String? = null
    ) {
        viewModelScope.launch {
            val p = profile.value
            val vOwner = customOwnerName ?: p?.name ?: "Resident"
            val vBlock = customBlock ?: p?.block ?: "A"
            val vFlat = customFlat ?: p?.flat ?: "001"
            val vPhone = customPhone ?: p?.phone ?: ""

            val newVehicle = VehicleEntity(
                ownerName = vOwner,
                ownerBlock = vBlock,
                ownerFlat = vFlat,
                ownerPhone = vPhone,
                model = model.trim(),
                type = type,
                regNumber = regNumber.trim().uppercase(),
                rentPerHour = rentPerHour,
                availabilityHours = availabilityHours.trim(),
                desc = desc.trim(),
                status = "Available",
                isEv = isEv
            )
            repository.addVehicle(newVehicle)
        }
    }

    fun deleteVehicle(vehicle: VehicleEntity) {
        viewModelScope.launch {
            repository.deleteVehicle(vehicle)
        }
    }

    fun requestBooking(
        vehicle: VehicleEntity,
        hours: Int,
        notes: String,
        bookingDate: String = "Today",
        startHour: Int = 9
    ) {
        viewModelScope.launch {
            val p = profile.value ?: ProfileEntity(name = "Resident", block = "A", flat = "101", phone = "")
            
            val booking = BookingEntity(
                vehicleId = vehicle.id,
                vehicleModel = vehicle.model,
                vehicleType = vehicle.type,
                ownerName = vehicle.ownerName,
                ownerBlock = vehicle.ownerBlock,
                ownerFlat = vehicle.ownerFlat,
                ownerPhone = vehicle.ownerPhone,
                renterName = p.name.ifEmpty { "Guest Renter" },
                renterBlock = p.block.ifEmpty { "Block A" },
                renterFlat = p.flat.ifEmpty { "101" },
                renterPhone = p.phone.ifEmpty { "+1 555-5555" },
                rentPerHour = vehicle.rentPerHour,
                hours = hours,
                totalPrice = vehicle.rentPerHour * hours,
                notes = notes,
                bookingDate = bookingDate,
                startHour = startHour,
                status = "Requested"
            )
            repository.createBooking(booking)
        }
    }

    fun updateBookingStatus(booking: BookingEntity, nextStatus: String) {
        viewModelScope.launch {
            repository.updateBookingStatus(booking.id, nextStatus)
            
            // If rental is approved, update the vehicle status accordingly
            if (nextStatus == "Approved") {
                val vehicle = repository.getVehicleById(booking.vehicleId)
                if (vehicle != null) {
                    repository.updateVehicle(vehicle.copy(status = "Rented"))
                }
            } else if (nextStatus == "Completed" || nextStatus == "Cancelled") {
                val vehicle = repository.getVehicleById(booking.vehicleId)
                if (vehicle != null) {
                    // Check if there are any other approved bookings currently active or simply free it
                    repository.updateVehicle(vehicle.copy(status = "Available"))
                }
            }
        }
    }

    private suspend fun seedSampleData() {
        val sampleVehicles = listOf(
            VehicleEntity(
                ownerName = "Amit Pathak",
                ownerBlock = "Tower A",
                ownerFlat = "1203",
                ownerPhone = "+91 99887 76655",
                model = "Tata Nexon EV (Teal Blue)",
                type = "Car",
                regNumber = "KA-51-EV-4321",
                rentPerHour = 180.0,
                availabilityHours = "Weekends Only (8 AM - 10 PM)",
                desc = "Ultra-premium pure electric SUV, highly spacious. Autopilot assist with fast charger cable inside boot. Please return with >60% charge level.",
                status = "Available",
                isEv = true
            ),
            VehicleEntity(
                ownerName = "Priya Nair",
                ownerBlock = "Tower C",
                ownerFlat = "402",
                ownerPhone = "+91 98234 56789",
                model = "Ather 450X (Cosmic Mint)",
                type = "Scooter",
                regNumber = "KA-03-AT-9080",
                rentPerHour = 40.0,
                availabilityHours = "Everyday (6 AM - 10 PM)",
                desc = "Sleek smart electric scooter. Reverse mode & touchscreen navigation. Perfect for clean emission society rides. Helmet inside underseat boot.",
                status = "Available",
                isEv = true
            ),
            VehicleEntity(
                ownerName = "Rajesh Deshmukh",
                ownerBlock = "Tower B",
                ownerFlat = "501",
                ownerPhone = "+91 88776 65544",
                model = "Royal Enfield Classic 350 (Stealth Black)",
                type = "Bike",
                regNumber = "MH-12-RE-1981",
                rentPerHour = 75.0,
                availabilityHours = "Weekdays (6 PM - 11 PM), All Sat-Sun",
                desc = "Pure heavy cruiser ride with legendary Royal Enfield signature exhaust note. Double helmets and riding gloves available on requested ping.",
                status = "Available",
                isEv = false
            ),
            VehicleEntity(
                ownerName = "Karan Patel",
                ownerBlock = "Tower D",
                ownerFlat = "82",
                ownerPhone = "+91 77665 54433",
                model = "Firefox Sniper 21-Speed Carbon",
                type = "Bicycle",
                regNumber = "IN-FOX-982",
                rentPerHour = 15.0,
                availabilityHours = "Always Available",
                desc = "Premium lightweight carbon frame hybrid speed bicycle with dynamic dual disc brakes. Cable lock dial set to 2026. Gate D cycle slots.",
                status = "Available",
                isEv = false
            )
        )

        for (vehicle in sampleVehicles) {
            repository.addVehicle(vehicle)
        }
    }
}
