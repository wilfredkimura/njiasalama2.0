package com.njiasalama.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.njiasalama.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * State machine representing authentication process status.
 */
sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    data class Success(val token: String) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

/**
 * AuthViewModel manages state variables and credential processing logic for standard email auth and Google Sign-In.
 */
class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    // Flag indicating whether to show SignUp screen inputs instead of standard login
    private val _isSignUpMode = MutableStateFlow(false)
    val isSignUpMode: StateFlow<Boolean> = _isSignUpMode.asStateFlow()

    fun setEmail(value: String) { _email.value = value }
    fun setPassword(value: String) { _password.value = value }
    fun setName(value: String) { _name.value = value }
    
    /**
     * Swaps between SignUp and Login UI modes.
     */
    fun toggleAuthMode() { 
        _isSignUpMode.value = !_isSignUpMode.value
        _uiState.value = AuthUiState.Idle // Reset error constraints on toggle
    }

    /**
     * Triggers the appropriate authentication route (login vs registration)
     * checking basic inputs validation first.
     */
    fun submit() {
        val currentEmail = _email.value.trim()
        val currentPassword = _password.value.trim()
        val currentName = _name.value.trim()

        if (currentEmail.isEmpty() || currentPassword.isEmpty() || (_isSignUpMode.value && currentName.isEmpty())) {
            _uiState.value = AuthUiState.Error("Please fill out all fields")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = if (_isSignUpMode.value) {
                authRepository.signUp(currentEmail, currentPassword, currentName)
            } else {
                authRepository.login(currentEmail, currentPassword)
            }

            result.onSuccess { response ->
                _uiState.value = AuthUiState.Success(response.accessToken)
            }.onFailure { exception ->
                _uiState.value = AuthUiState.Error(
                    exception.message ?: "Authentication transaction failed. Please check your network connection."
                )
            }
        }
    }

    /**
     * Dispatches verified Google OAuth credentials to the backend to complete authentication.
     */
    fun handleGoogleLogin(idToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.googleLogin(idToken)
                .onSuccess { response ->
                    _uiState.value = AuthUiState.Success(response.accessToken)
                }
                .onFailure { exception ->
                    _uiState.value = AuthUiState.Error(
                        exception.message ?: "Google authentication verification failed. Please try again."
                    )
                }
        }
    }
}
