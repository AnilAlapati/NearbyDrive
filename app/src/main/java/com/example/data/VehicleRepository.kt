package com.example.data

import kotlinx.coroutines.flow.Flow

class VehicleRepository(private val database: AppDatabase) {
    val allVehicles: Flow<List<VehicleEntity>> = database.vehicleDao().getAllVehicles()
    val allBookings: Flow<List<BookingEntity>> = database.bookingDao().getAllBookings()
    val profile: Flow<ProfileEntity?> = database.profileDao().getProfileFlow()
    val allReviews: Flow<List<ReviewEntity>> = database.reviewDao().getAllReviewsFlow()

    fun getReviewsForVehicleFlow(vehicleId: Long): Flow<List<ReviewEntity>> {
        return database.reviewDao().getReviewsForVehicleFlow(vehicleId)
    }

    fun getReviewsForUserFlow(name: String): Flow<List<ReviewEntity>> {
        return database.reviewDao().getReviewsForUserFlow(name)
    }

    suspend fun addReview(review: ReviewEntity): Long {
        return database.reviewDao().insertReview(review)
    }

    suspend fun updateBooking(booking: BookingEntity) {
        database.bookingDao().updateBooking(booking)
    }

    suspend fun getProfile(): ProfileEntity? {
        return database.profileDao().getProfile()
    }

    suspend fun saveProfile(profile: ProfileEntity) {
        database.profileDao().insertOrUpdateProfile(profile)
    }

    suspend fun addVehicle(vehicle: VehicleEntity): Long {
        val id = database.vehicleDao().insertVehicle(vehicle)
        FirebaseSyncManager.pushVehicleToCloud(vehicle.copy(id = id))
        return id
    }

    suspend fun updateVehicle(vehicle: VehicleEntity) {
        database.vehicleDao().updateVehicle(vehicle)
        FirebaseSyncManager.pushVehicleToCloud(vehicle)
    }

    suspend fun deleteVehicle(vehicle: VehicleEntity) {
        database.vehicleDao().deleteVehicle(vehicle)
        FirebaseSyncManager.deleteVehicleFromCloud(vehicle)
    }

    suspend fun getVehicleById(id: Long): VehicleEntity? {
        return database.vehicleDao().getVehicleById(id)
    }

    suspend fun createBooking(booking: BookingEntity): Long {
        val id = database.bookingDao().insertBooking(booking)
        FirebaseSyncManager.pushBookingToCloud(booking.copy(id = id))
        return id
    }

    suspend fun updateBookingStatus(bookingId: Long, status: String) {
        database.bookingDao().updateBookingStatus(bookingId, status)
    }
}
