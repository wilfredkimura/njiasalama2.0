package com.njiasalama.data

import android.content.Context
import com.njiasalama.domain.model.AuthResponse
import com.njiasalama.domain.model.GoogleAuthRequest
import com.njiasalama.domain.model.LoginRequest
import com.njiasalama.domain.model.SignUpRequest
import com.njiasalama.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Concrete implementation of the AuthRepository.
 * Handles network REST auth endpoint requests and manages the session token using SharedPreferences.
 */
class AuthRepositoryImpl(
    private val context: Context,
    private val api: NjiaSalamaApi
) : AuthRepository {

    // Retrieve system shared preferences namespace to store simple string key-pairs
    private val sharedPrefs = context.getSharedPreferences("njia_salama_prefs", Context.MODE_PRIVATE)
    private val TOKEN_KEY = "auth_token"

    // Track active login status. True if a session token is currently saved locally.
    private val _isLoggedIn = MutableStateFlow(getToken() != null)
    override val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    override suspend fun login(email: String, password: String): Result<AuthResponse> {
        return try {
            val response = api.login(LoginRequest(email, password))
            saveToken(response.accessToken)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUp(email: String, password: String, name: String): Result<AuthResponse> {
        return try {
            val response = api.signUp(SignUpRequest(email, password, name))
            saveToken(response.accessToken)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun googleLogin(idToken: String): Result<AuthResponse> {
        return try {
            val response = api.googleLogin(GoogleAuthRequest(idToken))
            saveToken(response.accessToken)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getToken(): String? {
        return sharedPrefs.getString(TOKEN_KEY, null)
    }

    override fun getUserName(): String? {
        val token = getToken() ?: return null
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return null
            val payloadBytes = decodeBase64Url(parts[1])
            val payloadString = String(payloadBytes, Charsets.UTF_8)
            val nameRegex = """"name"\s*:\s*"([^"]+)"""".toRegex()
            nameRegex.find(payloadString)?.groupValues?.get(1)
        } catch (e: Exception) {
            null
        }
    }

    private fun decodeBase64Url(input: String): ByteArray {
        val base64Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        val clean = input.replace('-', '+').replace('_', '/').filter { !it.isWhitespace() }
        val padding = when (clean.length % 4) {
            2 -> "=="
            3 -> "="
            else -> ""
        }
        val data = clean + padding
        val output = java.io.ByteArrayOutputStream()
        var buffer = 0
        var bitsCollected = 0
        for (char in data) {
            if (char == '=') break
            val value = base64Chars.indexOf(char)
            if (value < 0) continue
            buffer = (buffer shl 6) or value
            bitsCollected += 6
            if (bitsCollected >= 8) {
                bitsCollected -= 8
                output.write((buffer shr bitsCollected) and 0xFF)
            }
        }
        return output.toByteArray()
    }

    override fun saveToken(token: String) {
        sharedPrefs.edit().putString(TOKEN_KEY, token).apply()
        _isLoggedIn.value = true
    }

    override fun logout() {
        sharedPrefs.edit().remove(TOKEN_KEY).apply()
        _isLoggedIn.value = false
    }
}
