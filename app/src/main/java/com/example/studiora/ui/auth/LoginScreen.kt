package com.example.studiora.ui.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.studiora.model.UserRoles
import com.example.studiora.ui.common.AppLogo
import com.example.studiora.viewmodel.AuthState
import com.example.studiora.viewmodel.AuthViewModel

@Composable
fun LoginScreen(navController: NavController, authViewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val isLoading = authState is AuthState.Loading

    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Success -> {
                authViewModel.resetState()
                when (state.user.role) {
                    UserRoles.ADMIN   -> navController.navigate("admin_dashboard")   { popUpTo("login") { inclusive = true } }
                    UserRoles.TEACHER -> navController.navigate("teacher_dashboard") { popUpTo("login") { inclusive = true } }
                    else              -> navController.navigate("student_dashboard") { popUpTo("login") { inclusive = true } }
                }
            }
            is AuthState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                authViewModel.resetState()
            }
            else -> {}
        }
    }

    // ── Deep navy → dark-blue gradient background ─────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D1B2A), Color(0xFF1A3550), Color(0xFF1F4068))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(56.dp))

            // Logo
            AppLogo(size = 110.dp)

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                "Studiora",
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                "Smart Learning Management",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.65f)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // ── White login card ──────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(20.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(28.dp)) {

                    Text(
                        "Welcome Back!",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0D1B2A)
                    )
                    Text(
                        "Sign in to your account",
                        fontSize = 13.sp,
                        color = Color(0xFF7A8694)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Email
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, null, tint = Color(0xFF1A4D7A))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = Color(0xFF1A4D7A),
                            unfocusedBorderColor = Color(0xFFCDD5DF),
                            focusedLabelColor    = Color(0xFF1A4D7A),
                            unfocusedContainerColor = Color(0xFFF8FAFB),
                            focusedContainerColor   = Color.White
                        ),
                        enabled = !isLoading
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Password
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, null, tint = Color(0xFF1A4D7A))
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    null,
                                    tint = Color(0xFF7A8694)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = Color(0xFF1A4D7A),
                            unfocusedBorderColor = Color(0xFFCDD5DF),
                            focusedLabelColor    = Color(0xFF1A4D7A),
                            unfocusedContainerColor = Color(0xFFF8FAFB),
                            focusedContainerColor   = Color.White
                        ),
                        enabled = !isLoading
                    )

                    // Forgot password
                    TextButton(
                        onClick = { navController.navigate("forgotPassword") },
                        modifier = Modifier.align(Alignment.End),
                        enabled = !isLoading
                    ) {
                        Text(
                            "Forgot Password?",
                            color = Color(0xFF1A4D7A),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Sign In button
                    Button(
                        onClick = {
                            when {
                                email.isEmpty()    -> Toast.makeText(context, "Please enter your email",    Toast.LENGTH_SHORT).show()
                                password.isEmpty() -> Toast.makeText(context, "Please enter your password", Toast.LENGTH_SHORT).show()
                                else               -> authViewModel.signIn(email.trim(), password)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1A4D7A)
                        ),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "Sign In",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── "New here?" section ──────────────────────────────────────
            Text(
                "New to Studiora?",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Register org button — bright orange so it's impossible to miss
            Button(
                onClick = { navController.navigate("org_register") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF9933)   // bright orange
                ),
                enabled = !isLoading
            ) {
                Text(
                    "Create Organization Account",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}
