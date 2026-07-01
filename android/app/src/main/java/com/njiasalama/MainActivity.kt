package com.njiasalama

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.njiasalama.ui.map.MapScreen
import com.njiasalama.ui.theme.AppTheme

/**
 * MainActivity is the primary entry point of our Android application.
 * When the cyclist launches the app, this Activity is booted first by the Android OS.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enables edge-to-edge layout, allowing the UI (like status bar) to merge elegantly with system edges
        enableEdgeToEdge()
        // setContent binds Jetpack Compose's UI layouts to the screen lifecycle
        setContent {
            AppTheme {
                // Scaffold provides a basic Material 3 layout structure (supporting top bars, bottom bars)
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Render our MapScreen, passing the inner padding to respect status/navigation bars
                    MapScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}