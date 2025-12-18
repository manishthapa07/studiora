package com.example.studiora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.studiora.navigation.AppNavigation
import com.google.firebase.auth.FirebaseAuth
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.example.studiora.ui.theme.StudioraTheme

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        val startDestination = if (currentUser != null) "dashboard" else "login"

        setContent {
            StudioraTheme {
                var showContent by remember { mutableStateOf(false) }
                val navController = rememberNavController()

                // Show splash logo for 2 seconds
                LaunchedEffect(Unit) {
                    delay(2000)
                    showContent = true
                }

                if (!showContent) {
                    // Professional Splash Screen
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                            MaterialTheme.colorScheme.background
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.logo),
                                contentDescription = "App Logo",
                                modifier = Modifier.size(200.dp)
                            )
                        }
                    }
                } else {
                    AppNavigation(navController = navController, startDestination = startDestination)
                }
            }
        }
    }
}
