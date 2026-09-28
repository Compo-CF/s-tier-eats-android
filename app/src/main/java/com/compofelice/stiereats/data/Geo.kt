package com.compofelice.stiereats.data

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** A plain lat/lng point, so the ViewModel/screens don't couple to a Maps type. */
data class GeoPoint(val lat: Double, val lng: Double)

private const val EARTH_RADIUS_M = 6_371_000.0
const val METERS_PER_MILE = 1609.344

/** Great-circle distance in meters between two lat/lng points (haversine). */
fun haversineMeters(aLat: Double, aLng: Double, bLat: Double, bLng: Double): Double {
    val dLat = Math.toRadians(bLat - aLat)
    val dLng = Math.toRadians(bLng - aLng)
    val h = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(aLat)) * cos(Math.toRadians(bLat)) *
        sin(dLng / 2) * sin(dLng / 2)
    return EARTH_RADIUS_M * 2 * atan2(sqrt(h), sqrt(1 - h))
}

/** Straight-line distance from this restaurant to a point, in meters. */
fun Restaurant.distanceMetersTo(p: GeoPoint): Double =
    haversineMeters(latitude, longitude, p.lat, p.lng)

/** Human-readable miles string, matching the iOS formatting ("< 0.1 mi", "1.4 mi"). */
fun milesString(meters: Double): String {
    val miles = meters / METERS_PER_MILE
    return if (miles < 0.1) "< 0.1 mi" else "%.1f mi".format(miles)
}
