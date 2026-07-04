package com.njiasalama.domain.repository

import com.njiasalama.domain.model.AuthResponse
import kotlinx.coroutines.flow.StateFlow

/**
 * Domain boundary contract defining operations for user session authentication.
 */
interface AuthRepository {
    // Flow representing whether the cyclist is currently logged in
    val isLoggedIn: StateFlow<Boolean>

    // Logs in a user using their email and password credentials
    suspend fun login(email: String, password: String): Result<AuthResponse>

    // Registers a user using their email, password, and name
    suspend fun signUp(email: String, password: String, name: String): Result<AuthResponse>

    // Exchanges a verified Google Sign-in ID token for a backend session token
    suspend fun googleLogin(idToken: String): Result<AuthResponse>

    // Retrieves the active saved session token directly (returns null if unauthenticated)
    fun getToken(): String?

    // Extracts the user's name from the active JWT token payload (returns null if invalid or unauthenticated)
    fun getUserName(): String?

    // Persists the active session token (useful after a login/signup)
    fun saveToken(token: String)

    // Clears the saved session token, logging the user out and updating the flow
    fun logout()
}
