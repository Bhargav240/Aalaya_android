package com.aalaya.pg

import android.Manifest
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aalaya.pg.core.data.AuthState
import com.aalaya.pg.core.data.ThemeMode
import com.aalaya.pg.feature.main.AppViewModel
import com.aalaya.pg.feature.main.ThemeViewModel
import com.aalaya.pg.navigation.AppRoot
import com.aalaya.pg.ui.theme.HamsaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels()

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { _ ->
        // Result handled; screens check permission via LocationProvider
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called before super.onCreate to hand off from the launch theme.
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition {
            appViewModel.authState.value is AuthState.Loading
        }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        requestLocationOnFirstLaunch()

        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val mode by themeViewModel.mode.collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val darkTheme = when (mode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            HamsaTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppRoot()
                }
            }
        }
    }

    private fun requestLocationOnFirstLaunch() {
        val prefs = getSharedPreferences("aalaya_app_prefs", Context.MODE_PRIVATE)
        val hasRequested = prefs.getBoolean("first_launch_location_requested", false)
        if (!hasRequested) {
            prefs.edit().putBoolean("first_launch_location_requested", true).apply()
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }
}
