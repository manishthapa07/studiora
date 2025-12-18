package com.example.studiora.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.studiora.ui.auth.ForgotPasswordScreen
import com.example.studiora.ui.auth.LoginScreen
import com.example.studiora.ui.auth.RegistrationScreen
import com.example.studiora.ui.dashboard.DashboardScreen

@Composable
fun AppNavigation(navController: NavHostController, startDestination: String) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("login") {
            LoginScreen(navController = navController)
        }

        composable("register") {
            RegistrationScreen(navController = navController)
        }

        composable("forgotPassword") {
            ForgotPasswordScreen(navController = navController)
        }

        composable("dashboard") {
            DashboardScreen(navController = navController)
        }
    }
}

