package com.njiasalama.domain.model

import java.io.Serializable

data class GeocodeLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double
) : Serializable
