package com.example.studiora.ui.common

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.studiora.util.CloudinaryHelper
import com.example.studiora.viewmodel.AuthViewModel
import com.example.studiora.viewmodel.ProfileUpdateState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val currentUser by authViewModel.currentUserData.collectAsStateWithLifecycle()
    val profileUpdateState by authViewModel.profileUpdateState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Editable fields
    var name by remember(currentUser) { mutableStateOf(currentUser?.name ?: "") }
    var phone by remember(currentUser) { mutableStateOf(currentUser?.phone ?: "") }
    var address by remember(currentUser) { mutableStateOf(currentUser?.address ?: "") }
    var parentName by remember(currentUser) { mutableStateOf(currentUser?.parentName ?: "") }
    var parentPhone by remember(currentUser) { mutableStateOf(currentUser?.parentPhone ?: "") }
    var subject by remember(currentUser) { mutableStateOf(currentUser?.subject ?: "") }

    var isUploadingPhoto by remember { mutableStateOf(false) }
    var isUploadingDoc by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Photo picker
    val photoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isUploadingPhoto = true
            scope.launch {
                val result = CloudinaryHelper.uploadFile(context, it, "image")
                result.onSuccess { url ->
                    val updated = currentUser!!.copy(profileImageUrl = url)
                    authViewModel.updateProfile(updated)
                }.onFailure { e ->
                    Toast.makeText(context, "Photo upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
                isUploadingPhoto = false
            }
        }
    }

    // Document picker
    val docLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isUploadingDoc = true
            scope.launch {
                val result = CloudinaryHelper.uploadFile(context, it, "auto")
                result.onSuccess { url ->
                    val updated = currentUser!!.copy(documentUrl = url)
                    authViewModel.updateProfile(updated)
                }.onFailure { e ->
                    Toast.makeText(context, "Document upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
                isUploadingDoc = false
            }
        }
    }

    // Handle profile update state
    LaunchedEffect(profileUpdateState) {
        when (profileUpdateState) {
            is ProfileUpdateState.Success -> {
                Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
                authViewModel.resetProfileUpdateState()
            }
            is ProfileUpdateState.Error -> {
                Toast.makeText(context, (profileUpdateState as ProfileUpdateState.Error).message, Toast.LENGTH_LONG).show()
                authViewModel.resetProfileUpdateState()
            }
            else -> {}
        }
    }

    val isSaving = profileUpdateState is ProfileUpdateState.Loading

    if (showLogoutDialog) {
        LogoutConfirmDialog(
            onConfirm = {
                authViewModel.signOut()
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
            },
            onDismiss = { showLogoutDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, "Logout", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Header with avatar ────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                            )
                        )
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Profile photo
                    Box(contentAlignment = Alignment.BottomEnd) {
                        if (currentUser?.profileImageUrl.isNullOrEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f))
                                    .border(3.dp, MaterialTheme.colorScheme.onPrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    currentUser?.name?.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        } else {
                            AsyncImage(
                                model = currentUser?.profileImageUrl,
                                contentDescription = "Profile photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .border(3.dp, MaterialTheme.colorScheme.onPrimary, CircleShape)
                            )
                        }

                        // Camera edit button
                        if (isUploadingPhoto) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .size(30.dp)
                                    .clickable { photoLauncher.launch("image/*") }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = "Change photo",
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        currentUser?.name ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        currentUser?.email ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                    ) {
                        Text(
                            currentUser?.role ?: "",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Basic Info Section ────────────────────────────────────────
            val isAdmin = currentUser?.role == "ADMIN"
            ProfileSectionCard(
                title = "Basic Information",
                icon = if (isAdmin) Icons.Default.Business else Icons.Default.Person
            ) {
                ProfileTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = if (isAdmin) "Organization Name" else "Full Name",
                    icon = if (isAdmin) Icons.Default.Business else Icons.Default.Person
                )
                Spacer(modifier = Modifier.height(12.dp))
                ProfileTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = if (isAdmin) "Organization Phone" else "Phone Number",
                    icon = Icons.Default.Phone
                )
                Spacer(modifier = Modifier.height(12.dp))
                ProfileTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = if (isAdmin) "Organization Address" else "Address",
                    icon = if (isAdmin) Icons.Default.LocationCity else Icons.Default.Home
                )
                // Teacher-only subject field
                if (currentUser?.role == "TEACHER") {
                    Spacer(modifier = Modifier.height(12.dp))
                    ProfileTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = "Subject",
                        icon = Icons.Default.Book
                    )
                }
            }

            // Parent / Guardian & Legal Document — only for STUDENT and TEACHER
            if (currentUser?.role != "ADMIN") {
                Spacer(modifier = Modifier.height(12.dp))

                // ── Parents Details ───────────────────────────────────────────
                ProfileSectionCard(title = "Parent / Guardian Details", icon = Icons.Default.FamilyRestroom) {
                    ProfileTextField(
                        value = parentName,
                        onValueChange = { parentName = it },
                        label = "Parent / Guardian Name",
                        icon = Icons.Default.Person
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ProfileTextField(
                        value = parentPhone,
                        onValueChange = { parentPhone = it },
                        label = "Parent / Guardian Phone",
                        icon = Icons.Default.Phone
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── Legal Document Section ────────────────────────────────────
                ProfileSectionCard(title = "Legal Document", icon = Icons.Default.Description) {
                    if (currentUser?.documentUrl.isNullOrEmpty()) {
                        Text(
                            "No document uploaded yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Document uploaded",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    if (isUploadingDoc) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Uploading document...", style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { docLauncher.launch("*/*") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Upload, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (currentUser?.documentUrl.isNullOrEmpty()) "Upload Document" else "Replace Document"
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Save Button ───────────────────────────────────────────────
            Button(
                onClick = {
                    currentUser?.let { user ->
                        authViewModel.updateProfile(
                            user.copy(
                                name = name.trim(),
                                phone = phone.trim(),
                                address = address.trim(),
                                parentName = parentName.trim(),
                                parentPhone = parentPhone.trim(),
                                subject = subject.trim()
                            )
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Save, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Changes", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProfileSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
    )
}





