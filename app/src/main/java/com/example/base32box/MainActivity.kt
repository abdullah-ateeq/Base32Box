package com.example.base32box

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.base32box.ui.navigation.Base32NavGraph
import com.example.base32box.ui.theme.Base32BoxTheme
import com.example.base32box.ui.viewmodels.HomeViewModel
import com.example.base32box.ui.viewmodels.SettingsViewModel

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val userPrefs by settingsViewModel.preferences.collectAsState()

            Base32BoxTheme(appTheme = userPrefs.theme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    Base32NavGraph(
                        navController = navController,
                        homeViewModel = homeViewModel,
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        homeViewModel.onAppExitOrClearRequested()
    }
}
