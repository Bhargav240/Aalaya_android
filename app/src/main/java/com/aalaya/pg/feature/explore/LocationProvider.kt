package com.aalaya.pg.feature.explore

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat

enum class LocationStatus {
    Available,
    PermissionDenied,
    ServicesDisabled,
}

/**
 * Lightweight, dependency-free location lookup for "near me" discovery. Returns
 * the most recent fix from any enabled provider, or null if permission is missing
 * or no fix is available.
 */
object LocationProvider {

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun isLocationServiceEnabled(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return LocationManagerCompat.isLocationEnabled(lm)
    }

    fun getLocationStatus(context: Context): LocationStatus {
        return when {
            !hasPermission(context) -> LocationStatus.PermissionDenied
            !isLocationServiceEnabled(context) -> LocationStatus.ServicesDisabled
            else -> LocationStatus.Available
        }
    }

    fun lastKnownLatLng(context: Context): Pair<Double, Double>? {
        if (!hasPermission(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        if (!LocationManagerCompat.isLocationEnabled(lm)) return null
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        return try {
            providers.asSequence()
                .mapNotNull { p -> if (lm.isProviderEnabled(p)) lm.getLastKnownLocation(p) else null }
                .maxByOrNull { it.time }
                ?.let { it.latitude to it.longitude }
        } catch (e: SecurityException) {
            null
        }
    }
}
