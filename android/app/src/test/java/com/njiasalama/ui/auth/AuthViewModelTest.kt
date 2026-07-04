package com.njiasalama.ui.auth

import com.njiasalama.domain.model.AuthResponse
import com.njiasalama.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * A mock double implementing the AuthRepository contract for testing AuthViewModel.
 */
class FakeAuthRepository(
    private val shouldFail: Boolean = false,
    private val expectedToken: String = "mock-jwt-token-123"
) : AuthRepository {
    private val _isLoggedIn = MutableStateFlow(false)
    override val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    override suspend fun login(email: String, password: String): Result<AuthResponse> {
        if (shouldFail) return Result.failure(Exception("Connection timeout"))
        _isLoggedIn.value = true
        return Result.success(AuthResponse(expectedToken))
    }

    override suspend fun signUp(email: String, password: String, name: String): Result<AuthResponse> {
        if (shouldFail) return Result.failure(Exception("Email duplicate check failed"))
        _isLoggedIn.value = true
        return Result.success(AuthResponse(expectedToken))
    }

    override suspend fun googleLogin(idToken: String): Result<AuthResponse> {
        if (shouldFail) return Result.failure(Exception("Google validation rejected"))
        _isLoggedIn.value = true
        return Result.success(AuthResponse(expectedToken))
    }

    override fun getToken(): String? = if (isLoggedIn.value) expectedToken else null
    override fun getUserName(): String? = if (isLoggedIn.value) "Fake Cyclist" else null
    override fun saveToken(token: String) { _isLoggedIn.value = true }
    override fun logout() { _isLoggedIn.value = false }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialUiStateIsIdle() {
        val viewModel = AuthViewModel(FakeAuthRepository())
        assertTrue("Initial state should be AuthUiState.Idle", viewModel.uiState.value is AuthUiState.Idle)
        assertEquals("", viewModel.email.value)
        assertEquals("", viewModel.password.value)
        assertEquals("", viewModel.name.value)
        assertEquals(false, viewModel.isSignUpMode.value)
    }

    @Test
    fun testToggleAuthModeUpdatesSignModeAndResetsUiState() {
        val viewModel = AuthViewModel(FakeAuthRepository())
        viewModel.toggleAuthMode()
        assertEquals(true, viewModel.isSignUpMode.value)
        assertTrue(viewModel.uiState.value is AuthUiState.Idle)
    }

    @Test
    fun testFormValidationErrorWhenInputsAreMissing() = kotlinx.coroutines.test.runTest(testDispatcher) {
        val viewModel = AuthViewModel(FakeAuthRepository())
        viewModel.setEmail("")
        viewModel.setPassword("pass")
        viewModel.submit()

        assertTrue("UiState should transition to Error", viewModel.uiState.value is AuthUiState.Error)
        val errorState = viewModel.uiState.value as AuthUiState.Error
        assertEquals("Please fill out all fields", errorState.message)
    }

    @Test
    fun testLoginSuccessTransitionsStateToSuccess() = kotlinx.coroutines.test.runTest(testDispatcher) {
        val viewModel = AuthViewModel(FakeAuthRepository(expectedToken = "success-token"))
        viewModel.setEmail("cyclist@example.com")
        viewModel.setPassword("password123")
        viewModel.submit()

        this.testScheduler.advanceUntilIdle()

        assertTrue("UiState should transition to Success", viewModel.uiState.value is AuthUiState.Success)
        val successState = viewModel.uiState.value as AuthUiState.Success
        assertEquals("success-token", successState.token)
    }

    @Test
    fun testSignUpSuccessTransitionsStateToSuccess() = kotlinx.coroutines.test.runTest(testDispatcher) {
        val viewModel = AuthViewModel(FakeAuthRepository(expectedToken = "success-token"))
        viewModel.toggleAuthMode() // Go to sign-up mode
        viewModel.setEmail("cyclist@example.com")
        viewModel.setPassword("password123")
        viewModel.setName("John Cyclist")
        viewModel.submit()

        this.testScheduler.advanceUntilIdle()

        assertTrue("UiState should transition to Success", viewModel.uiState.value is AuthUiState.Success)
        val successState = viewModel.uiState.value as AuthUiState.Success
        assertEquals("success-token", successState.token)
    }

    @Test
    fun testGoogleLoginSuccessTransitionsStateToSuccess() = kotlinx.coroutines.test.runTest(testDispatcher) {
        val viewModel = AuthViewModel(FakeAuthRepository(expectedToken = "google-success-token"))
        viewModel.handleGoogleLogin("verified-google-id-token")

        this.testScheduler.advanceUntilIdle()

        assertTrue("UiState should transition to Success", viewModel.uiState.value is AuthUiState.Success)
        val successState = viewModel.uiState.value as AuthUiState.Success
        assertEquals("google-success-token", successState.token)
    }

    @Test
    fun testApiFailureTransitionsStateToError() = kotlinx.coroutines.test.runTest(testDispatcher) {
        val viewModel = AuthViewModel(FakeAuthRepository(shouldFail = true))
        viewModel.setEmail("cyclist@example.com")
        viewModel.setPassword("password123")
        viewModel.submit()

        this.testScheduler.advanceUntilIdle()

        assertTrue("UiState should transition to Error", viewModel.uiState.value is AuthUiState.Error)
        val errorState = viewModel.uiState.value as AuthUiState.Error
        assertEquals("Connection timeout", errorState.message)
    }
}
