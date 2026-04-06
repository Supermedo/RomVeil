package com.glyph.launcher.ui

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.glyph.launcher.ui.main.MainScreen
import com.glyph.launcher.ui.permission.StoragePermissionScreen
import com.glyph.launcher.ui.settings.SettingsScreen
import com.glyph.launcher.ui.setup.SetupScreen
import com.glyph.launcher.util.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@Composable
fun GlyphApp(
    appViewModel: GlyphAppViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val activity = LocalContext.current as? Activity

    NavHost(
        navController = navController,
        startDestination = Route.PERMISSION
    ) {
        composable(Route.PERMISSION) {
            val scope = rememberCoroutineScope()
            StoragePermissionScreen(
                onPermissionGranted = {
                    scope.launch {
                        val complete = appViewModel.isSetupCompleteOnce()
                        if (complete) {
                            navController.navigate(Route.MAIN) {
                                popUpTo(Route.PERMISSION) { inclusive = true }
                            }
                        } else {
                            navController.navigate(Route.SETUP) {
                                popUpTo(Route.PERMISSION) { inclusive = true }
                            }
                        }
                    }
                },
                onBack = { activity?.finish() }
            )
        }

        composable(Route.SETUP) {
            SetupScreen(
                onSetupComplete = {
                    navController.navigate(Route.MAIN) {
                        popUpTo(Route.SETUP) { inclusive = true }
                    }
                },
                onBack = {
                    if (!navController.popBackStack()) activity?.finish()
                }
            )
        }

        composable(Route.MAIN) {
            MainScreen(
                onNavigateToSetup = {
                    navController.navigate(Route.SETUP)
                },
                onNavigateToSettings = {
                    navController.navigate(Route.SETTINGS)
                },
                onBack = { activity?.onBackPressed() }
            )
        }

        composable(Route.SETTINGS) {
            SettingsScreen(
                onBack = {
                    navController.popBackStack()
                },
                onRescan = {
                    // Rescan only re-scans the already added folder(s); stay in Settings (no navigate to Setup)
                }
            )
        }
    }
}

object Route {
    const val PERMISSION = "permission"
    const val SETUP = "setup"
    const val MAIN = "main"
    const val SETTINGS = "settings"
}

@HiltViewModel
class GlyphAppViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    val isSetupComplete = preferencesManager.isSetupComplete
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** Read persisted setup-complete flag once (used when deciding where to go after permission). */
    suspend fun isSetupCompleteOnce(): Boolean = preferencesManager.isSetupComplete.first()
}
