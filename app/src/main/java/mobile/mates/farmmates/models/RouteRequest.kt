package com.javeriana.taller2_movil.models

import kotlinx.serialization.Serializable

@Serializable
data class LatLngGR(
    val latitude: Double,
    val longitude: Double
)
@Serializable
data class Location(
    val latLng: LatLngGR
)
@Serializable
data class OriginOrDestination(
    val location: Location,
)
@Serializable
data class googleRoutesRequest(
    val origin: OriginOrDestination,
    val destination: OriginOrDestination,
    val travelMode: String,
    val routingPreference: String
)
