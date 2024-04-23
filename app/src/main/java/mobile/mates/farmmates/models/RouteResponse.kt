package com.javeriana.taller2_movil.models

import kotlinx.serialization.Serializable


@Serializable
data class polyline(
    val encodedPolyline: String
)

@Serializable
data class route(
    val polyline: polyline
)

@Serializable
data class RoutesResponse(
    val routes: List<route>
)