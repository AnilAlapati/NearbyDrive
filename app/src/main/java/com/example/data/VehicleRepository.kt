package com.example.data

import kotlinx.coroutines.flow.Flow

class VehicleRepository(private val database: AppDatabase) {
    val allVehicles: Flow<List<VehicleEntity>> = database.vehicleDao().getAllVehicles()
    val allBookings: Flow<List<BookingEntity>> = database.bookingDao().getAllBookings()
    val profile: Flow<ProfileEntity?> = database.profileDao().getProfileFlow()

    suspend fun getProfile(): ProfileEntity? {
        return database.profileDao().getProfile()
    }

    suspend fun saveProfile(profile: ProfileEntity) {
        database.profileDao().insertOrUpdateProfile(profile)
    }

    suspend fun addVehicle(vehicle: VehicleEntity): Long {
        return database.vehicleDao().insertVehicle(vehicle)
    }

    suspend fun updateVehicle(vehicle: VehicleEntity) {
        database.vehicleDao().updateVehicle(vehicle)
    }

    suspend fun deleteVehicle(vehicle: VehicleEntity) {
        database.vehicleDao().deleteVehicle(vehicle)
    }

    suspend fun getVehicleById(id: Long): VehicleEntity? {
        return database.vehicleDao().getVehicleById(id)
    }

    suspend fun createBooking(booking: BookingEntity): Long {
        return database.bookingDao().insertBooking(booking)
    }

    suspend fun updateBookingStatus(bookingId: Long, status: String) {
        database.bookingDao().updateBookingStatus(bookingId, status)
    }
}
