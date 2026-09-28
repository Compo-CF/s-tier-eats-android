package com.compofelice.stiereats.location

import android.annotation.SuppressLint
import android.content.Context
import com.compofelice.stiereats.data.GeoPoint
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await

/**
 * Thin wrapper over FusedLocationProviderClient. Callers MUST confirm the
 * COARSE_LOCATION runtime permission is granted before calling [current] — the
 * @SuppressLint is safe under that contract. Returns null on any failure
 * (permission race, no fix available, Play services missing) so the UI can fall
 * back to "search by area" instead of crashing.
 */
class LocationProvider(context: Context) {
    private val fused = LocationServices.getFusedLocationProviderClient(context.applicationContext)

    @SuppressLint("MissingPermission")
    suspend fun current(): GeoPoint? = try {
        val loc = fused.lastLocation.await()
            ?: fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
        loc?.let { GeoPoint(it.latitude, it.longitude) }
    } catch (e: Exception) {
        null
    }
}
