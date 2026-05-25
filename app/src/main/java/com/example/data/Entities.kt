package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerName: String,
    val ownerBlock: String,
    val ownerFlat: String,
    val ownerPhone: String,
    val model: String,
    val type: String, // "Car", "Bike", "Scooter", "Bicycle", "Other"
    val regNumber: String,
    val rentPerHour: Double,
    val availabilityHours: String, // e.g. "9 AM - 9 PM" or "Always"
    val desc: String,
    val status: String = "Available", // "Available", "Rented"
    val isEv: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "bookings")
data class BookingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val vehicleModel: String,
    val vehicleType: String,
    val ownerName: String,
    val ownerBlock: String,
    val ownerFlat: String,
    val ownerPhone: String,
    val renterName: String,
    val renterBlock: String,
    val renterFlat: String,
    val renterPhone: String,
    val rentPerHour: Double,
    val hours: Int,
    val totalPrice: Double,
    val notes: String = "",
    val bookingDate: String = "Today",
    val startHour: Int = 9,
    val status: String = "Requested", // "Requested", "Approved", "Completed", "Cancelled"
    val timestamp: Long = System.currentTimeMillis(),
    val isRenterReviewed: Boolean = false,
    val isOwnerReviewed: Boolean = false
)

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookingId: Long,
    val vehicleId: Long,
    val vehicleModel: String,
    val reviewerName: String,
    val revieweeName: String,
    val reviewerRole: String, // "Renter" or "Owner"
    val rating: Int, // 1 to 5 stars
    val comment: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val block: String = "",
    val flat: String = "",
    val phone: String = "",
    val isVerified: Boolean = false,
    val tripsOffered: Int = 0,
    val tripsTaken: Int = 0,
    val ratingAsHost: Double = 4.8,
    val ratingAsRider: Double = 4.9,
    val email: String = "",
    val isLoggedIn: Boolean = false,
    val authProvider: String = "Email" // "Email" or "Google" or "Fallback"
)
