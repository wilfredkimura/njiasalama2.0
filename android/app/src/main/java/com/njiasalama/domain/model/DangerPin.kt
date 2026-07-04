package com.njiasalama.domain.model

// HazardType Class Init
// Straict categories of road hazards
// using enum precents arbitrary string inputs from the frontend.
// enforces with NestJs constraints as well.
enum class HazardType { // This becaomes a datatype which is used in DangerPin data class to represent the type of hazard.
    POTHOLE,
    UNLIT_STREET,
    DANGEROUS_TRAFFIC,
    OTHER
}

// DangerPin Data Class Init
// Represents a road hazard pin on the map with relevant details.
// data class to aautomatically generate helper functions.
//helper functions include equals(), hashCode(), toString(), and copy().
data class DangerPin(
    val id: String, // Unique identifier for the pin
    val title: String, // Title of the hazard
    val description: String,// Description of the hazard
    val latitude: Double,// Latitude coordinate of the pin
    val longitude: Double,// Longitude coordinate of the pin
    val type: HazardType,// Type of hazard (POTHOLE, UNLIT_STREET, DANGEROUS_TRAFFIC, OTHER)
    val reportedBy: String,// User ID of the person who reported the hazard
    val imageUrl: String? = null // Optional hazard image URL (Base64 data URI or HTTP link)
)