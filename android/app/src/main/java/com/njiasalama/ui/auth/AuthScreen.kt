package com.njiasalama.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val name by viewModel.name.collectAsState()
    val isSignUpMode by viewModel.isSignUpMode.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    // Google Web Client ID injected via BuildConfig from local.properties
    val googleWebClientId = com.njiasalama.BuildConfig.GOOGLE_CLIENT_ID

    // Side-effect: trigger onAuthSuccess callback once successful
    if (uiState is AuthUiState.Success) {
        onAuthSuccess((uiState as AuthUiState.Success).token)
    }

    /**
     * Executes Google Sign-in flow using Android Credential Manager API.
     */
    fun onGoogleSignInClick() {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(googleWebClientId)
            .setAutoSelectEnabled(true)
            .build()

        val getCredRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        coroutineScope.launch {
            try {
                val result = credentialManager.getCredential(context, getCredRequest)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    viewModel.handleGoogleLogin(idToken)
                }
            } catch (e: Exception) {
                // Catches API exceptions (e.g. user cancels credentials choosing prompt dialog)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E293B), // Premium Slate Dark Blue
                        Color(0xFF0F172A)  // Very dark blue
                    )
                )
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Branding Logo Placeholder / Title
            Text(
                text = "NjiaSalama",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF38BDF8), // Radiant Light Blue Accent
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Secure cycling route safety updates",
                fontSize = 14.sp,
                color = Color.LightGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
            )

            // Dynamic Form Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x1FFFFFFF), RoundedCornerShape(16.dp)) // Glassmorphism-style overlay
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isSignUpMode) "Create Account" else "Welcome Back",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Name field only visible in Sign-Up mode
                if (isSignUpMode) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { viewModel.setName(it) },
                        label = { Text("Full Name", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Email Input
                OutlinedTextField(
                    value = email,
                    onValueChange = { viewModel.setEmail(it) },
                    label = { Text("Email Address", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color.Gray) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Password Input
                OutlinedTextField(
                    value = password,
                    onValueChange = { viewModel.setPassword(it) },
                    label = { Text("Password", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons
                Button(
                    onClick = { viewModel.submit() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0284C7) // Sky blue color
                    )
                ) {
                    Text(
                        text = if (isSignUpMode) "Sign Up" else "Log In",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSignUpMode) "Already have an account?" else "Don't have an account?",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                    TextButton(onClick = { viewModel.toggleAuthMode() }) {
                        Text(
                            text = if (isSignUpMode) "Log In" else "Sign Up",
                            color = Color(0xFF38BDF8),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Or separator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f).height(1.dp).background(Color.DarkGray))
                Text(
                    text = " OR ",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(modifier = Modifier.weight(1f).height(1.dp).background(Color.DarkGray))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Google login button
            OutlinedButton(
                onClick = { onGoogleSignInClick() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Modern premium Google Sign-In Text
                    Text(
                        text = "Continue with Google",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Loading spinner overlay
        if (uiState is AuthUiState.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x990F172A)), // Semi-transparent dark background
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF38BDF8))
            }
        }

        // Error message feedback banner
        if (uiState is AuthUiState.Error) {
            val errorMsg = (uiState as AuthUiState.Error).message
            Text(
                text = errorMsg,
                color = Color(0xFFEF4444), // Crimson Red
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFEF2F2), RoundedCornerShape(8.dp))
                    .padding(12.dp)
                    .align(Alignment.BottomCenter)
            )
        }
    }
}
