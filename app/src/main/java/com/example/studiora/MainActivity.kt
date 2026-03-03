package com.example.studiora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.rememberNavController
import com.example.studiora.model.UserRoles
import com.example.studiora.navigation.AppNavigation
import com.example.studiora.repository.AuthRepository
import com.example.studiora.ui.theme.NunitoFontFamily
import com.example.studiora.ui.theme.StudioraTheme
import com.example.studiora.util.CloudinaryHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Cloudinary
        CloudinaryHelper.initialize(this)

        setContent {
            StudioraTheme {
                var showSplash by remember { mutableStateOf(true) }
                var startDestination by remember { mutableStateOf("login") }
                val navController = rememberNavController()
                val scope = rememberCoroutineScope()

                LaunchedEffect(Unit) {
                    scope.launch {
                        // Determine start destination based on logged-in user role
                        val currentUser = FirebaseAuth.getInstance().currentUser
                        if (currentUser != null) {
                            try {
                                val repo = AuthRepository()
                                val result = repo.getUserById(currentUser.uid)
                                result.onSuccess { user ->
                                    startDestination = when (user.role) {
                                        UserRoles.ADMIN -> "admin_dashboard"
                                        UserRoles.TEACHER -> "teacher_dashboard"
                                        else -> "student_dashboard"
                                    }
                                }.onFailure {
                                    startDestination = "login"
                                }
                            } catch (e: Exception) {
                                startDestination = "login"
                            }
                        }
                        delay(2500)
                        showSplash = false
                    }
                }

                if (showSplash) {
                    AnimatedSplashScreen()
                } else {
                    AppNavigation(navController = navController, startDestination = startDestination)
                }
            }
        }
    }
}

@Composable
fun AnimatedSplashScreen() {
    // Logo scale animation
    val logoScale = remember { Animatable(0.5f) }
    val logoAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val taglineAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Logo pop in
        launch {
            logoScale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        }
        launch {
            logoAlpha.animateTo(1f, animationSpec = tween(600))
        }
        delay(400)
        textAlpha.animateTo(1f, animationSpec = tween(500))
        delay(200)
        taglineAlpha.animateTo(1f, animationSpec = tween(500))
    }

    // Dot pulse animation
    val dotTransition = rememberInfiniteTransition(label = "dots")
    val dot1Alpha by dotTransition.animateFloat(0.2f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "d1")
    val dot2Alpha by dotTransition.animateFloat(0.2f, 1f, infiniteRepeatable(tween(600, 200), RepeatMode.Reverse), label = "d2")
    val dot3Alpha by dotTransition.animateFloat(0.2f, 1f, infiniteRepeatable(tween(600, 400), RepeatMode.Reverse), label = "d3")

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0D1B2A),
                            Color(0xFF1A3550),
                            Color(0xFF1F4068)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Logo with bounce animation
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Studiora Logo",
                    modifier = Modifier
                        .size(180.dp)
                        .scale(logoScale.value)
                        .alpha(logoAlpha.value)
                )
                Spacer(modifier = Modifier.height(20.dp))
                // App name
                Text(
                    text = "Studiora",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 36.sp,
                    color = Color.White,
                    modifier = Modifier.alpha(textAlpha.value)
                )
                Spacer(modifier = Modifier.height(6.dp))
                // Tagline
                Text(
                    text = "Smart Learning Management",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.alpha(taglineAlpha.value)
                )
                Spacer(modifier = Modifier.height(48.dp))
                // Animated loading dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.alpha(taglineAlpha.value)
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White.copy(alpha = dot1Alpha)))
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White.copy(alpha = dot2Alpha)))
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White.copy(alpha = dot3Alpha)))
                }
            }
        }
    }
}
