package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.model.AppRoute
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.KioskScreen
import com.example.ui.theme.SellMoreTheme
import com.example.viewmodel.SellMoreViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SellMoreTheme {
                val viewModel: SellMoreViewModel = viewModel()
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = AppRoute.Kiosk
                ) {
                    composable<AppRoute.Kiosk> {
                        KioskScreen(
                            viewModel = viewModel,
                            onNavigateToAdmin = {
                                navController.navigate(AppRoute.Admin) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                    composable<AppRoute.Admin> {
                        AdminDashboardScreen(
                            viewModel = viewModel,
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
