package com.njiasalama

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.njiasalama.data.RetrofitClient
import com.njiasalama.ui.auth.AuthScreen
import com.njiasalama.ui.auth.AuthViewModel
import com.njiasalama.ui.map.MapScreen
import com.njiasalama.ui.theme.AppTheme

/**
 * MainActivity is the primary entry point of our Android application.
 * Observes user authentication state to coordinate gating UI application access.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Safely fetch the cached AuthRepository singleton
        val authRepository = RetrofitClient.getAuthRepository(applicationContext)

        setContent {
            AppTheme {
                // Collect login status flow, pausing observations when app goes to background
                val isLoggedIn by authRepository.isLoggedIn.collectAsStateWithLifecycle()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (isLoggedIn) {
                        // Render MapScreen if the user holds a valid session token
                        MapScreen(
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else {
                        // Render AuthScreen if user is unauthenticated
                        val authViewModel: AuthViewModel = viewModel(
                            factory = viewModelFactory {
                                initializer {
                                    AuthViewModel(authRepository)
                                }
                            }
                        )
                        AuthScreen(
                            viewModel = authViewModel,
                            onAuthSuccess = { _ ->
                                // Login success callback. isLoggedIn flow will toggle automatically.
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}