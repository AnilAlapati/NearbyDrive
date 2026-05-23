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
                        name = "Alex Carter",
                        block = "Block B",
                        flat = "302",
                        phone = "+1 (555) 019-2834",
                        isVerified = true,
                        tripsOffered = 12,
                        tripsTaken = 8,
                        ratingAsHost = 4.8,
                        ratingAsRider = 4.9
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

    fun requestBooking(vehicle: VehicleEntity, hours: Int, notes: String) {
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
                ownerName = "Michael Scott",
                ownerBlock = "Block D",
                ownerFlat = "404",
                ownerPhone = "+1 (555) 123-4567",
                model = "Tesla Model 3 (Midnight Silver)",
                type = "Car",
                regNumber = "CA-889-TES",
                rentPerHour = 150.0,
                availabilityHours = "Weekends Only (8 AM - 10 PM)",
                desc = "Pure electric luxury car. Impeccably clean, Autopilot, Charger included in trunk. Please return with at least 50% charge.",
                status = "Available",
                isEv = true
            ),
            VehicleEntity(
                ownerName = "Penny Hofstadter",
                ownerBlock = "Block A",
                ownerFlat = "402",
                ownerPhone = "+1 (555) 987-6543",
                model = "Honda Activa 6G (Matte Blue)",
                type = "Scooter",
                regNumber = "NY-77A-ACT",
                rentPerHour = 35.0,
                availabilityHours = "Everyday 7 AM - 9 PM",
                desc = "Very easy to ride, high mileage. Perfect for grocery runs or quick errands. Helmet is placed in the boot, keys are near shoe rack.",
                status = "Available",
                isEv = false
            ),
            VehicleEntity(
                ownerName = "Rajesh Koothrappali",
                ownerBlock = "Block B",
                ownerFlat = "501",
                ownerPhone = "+1 (555) 111-2222",
                model = "Royal Enfield Classic 350 (Chrome)",
                type = "Bike",
                regNumber = "RE-350-CHP",
                rentPerHour = 65.0,
                availabilityHours = "Mon-Fri (Evening), All Day Sat-Sun",
                desc = "Vintage cruiser bike with heavy thump. Please wear safety gear. Double helmet available on request.",
                status = "Available",
                isEv = false
            ),
            VehicleEntity(
                ownerName = "Dustin Henderson",
                ownerBlock = "Block C",
                ownerFlat = "12",
                ownerPhone = "+1 (555) 444-5555",
                model = "Trek Marlin 7 Mountain Bike",
                type = "Bicycle",
                regNumber = "TRK-MAR-007",
                rentPerHour = 10.0,
                availabilityHours = "Always Available",
                desc = "Premium 1x10 drivetrain hardtail mountain bike with hydraulic disc brakes. Lock is set to 1983. Pick/drop at the cycle stand near Block C gate.",
                status = "Available",
                isEv = false
            )
        )

        for (vehicle in sampleVehicles) {
            repository.addVehicle(vehicle)
        }
    }
}
