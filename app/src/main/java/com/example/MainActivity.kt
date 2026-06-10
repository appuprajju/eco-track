package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModelProvider
import com.example.data.remote.PerformanceTracer
import com.example.di.AppContainer
import com.example.ui.screens.EcoTrackMainApp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.EcoTrackViewModel
import com.example.ui.viewmodel.EcoTrackViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Safe-check Firebase option signatures & initialize manually to prevent background crash runs
        PerformanceTracer.initializeFirebaseIfConfigured(applicationContext)

        // Start Firebase App Startup latency monitoring instantly
        PerformanceTracer.startAppStartupTrace()

        super.onCreate(savedInstanceState)
        
        // 1. Initialize Dependency Injection container
        val appContainer = AppContainer(applicationContext)
        
        // 2. Instantiate MVVM ViewModel via Factory
        val factory = EcoTrackViewModelFactory(appContainer.repository)
        val viewModel = ViewModelProvider(this, factory)[EcoTrackViewModel::class.java]

        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                // Finalize startup trace immediately on first complete composition render pass
                LaunchedEffect(Unit) {
                    PerformanceTracer.stopAppStartupTrace()
                }

                // 3. Mount fully localized production carbon platform
                EcoTrackMainApp(viewModel = viewModel)
            }
        }
    }
}
