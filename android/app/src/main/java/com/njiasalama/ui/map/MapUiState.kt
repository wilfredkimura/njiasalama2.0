package com.njiasalama.ui.map

import com.njiasalama.domain.model.DangerPin // Importing the DangerPin data class from the domain model package to be used in the MapUiState sealed interface.

/**
 * A sealed interface defines a closed, strict hierarchy.
 * The compiler knows exactly what states the Map Screen can be in,
 * ensuring our Jetpack Compose UI pattern explicitly addresses all outcomes.
 */
sealed interface MapUiState {

    /**
     * Represents the initial application fetch state.
     * Tells the Compose framework to display an active loading spinner
     * while the device awaits data from the network.
     */
    object Loading : MapUiState

    /**
     * Represents a successful network or cache retrieval pipeline.
     * Contains the immutable immutable array list of danger pins ready to map out.
     */
    data class Success(val pins: List<DangerPin>) : MapUiState

    /**
     * Captures system, permission, or infrastructure communication failures.
     * Holds an error string directly readable by the UI layer to notify the cyclist.
     */
    data class Error(val message: String) : MapUiState
}