package com.njiasalama.domain.model

/**
 * Data model sent to the server for registering a new user account.
 */
data class SignUpRequest(
    val email: String,
    val password: String,
    val name: String
)

/**
 * Data model sent to the server for verifying standard password credentials.
 */
data class LoginRequest(
    val email: String,
    val password: String
)

/**
 * Data model sent to the server to verify a Google Sign-in ID Token.
 */
data class GoogleAuthRequest(
    val idToken: String
)

/**
 * Response returned by the server on successful authentication.
 * Holds the JWT session token.
 */
data class AuthResponse(
    val accessToken: String
)
