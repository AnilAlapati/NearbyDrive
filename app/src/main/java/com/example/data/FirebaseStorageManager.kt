package com.example.data

import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

object FirebaseStorageManager {
    private const val TAG = "FirebaseStorage"

    suspend fun uploadVehicleImage(imageUri: Uri): String? {
        return try {
            val storageRef = FirebaseStorage.getInstance().reference
            val imageRef = storageRef.child("vehicle_images/${UUID.randomUUID()}.jpg")
            
            imageRef.putFile(imageUri).await()
            val downloadUrl = imageRef.downloadUrl.await()
            
            Log.d(TAG, "Successfully uploaded image: $downloadUrl")
            downloadUrl.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload image", e)
            null
        }
    }
}
