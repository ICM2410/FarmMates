package com.javeriana.taller2_movil.models

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.Date

class myRoute {

    @Serializable
    data class site(
        val date: String,
        val latitud: Double,
        val longitud: Double
    )

    @Serializable
    data class sites(
        val list: MutableList<site> = mutableListOf()
    )
}
