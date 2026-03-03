package com.example.studiora.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.studiora.ui.common.StudioraBottomNav
import com.example.studiora.ui.common.adminNavItems
import com.example.studiora.viewmodel.AdminViewModel
import com.example.studiora.viewmodel.AuthViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    adminViewModel: AdminViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val currentUser by authViewModel.currentUserData.collectAsStateWithLifecycle()
    val teachers by adminViewModel.teachers.collectAsStateWithLifecycle()
    val students by adminViewModel.students.collectAsStateWithLifecycle()
    val classes by adminViewModel.classes.collectAsStateWithLifecycle()
    val courseCount by adminViewModel.courseCount.collectAsStateWithLifecycle()

    // Reload current user whenever this screen resumes (e.g. back from Profile)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                authViewModel.loadCurrentUser()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        authViewModel.loadCurrentUser()
        adminViewModel.loadAllDashboardData()
    }

    val today = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault()).format(Date())
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Good Morning"
        hour < 17 -> "Good Afternoon"
        else -> "Good Evening"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Studiora", fontWeight = FontWeight.Bold)
                        Text(
                            "Admin",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("profile") }) {
                        if (currentUser?.profileImageUrl.isNullOrEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    (currentUser?.name?.firstOrNull()?.toString() ?: "A").uppercase(),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        } else {
                            AsyncImage(
                                model = currentUser?.profileImageUrl,
                                contentDescription = "Profile",
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            StudioraBottomNav(
                items = adminNavItems,
                currentRoute = "admin_dashboard",
                onItemSelected = { route -> navController.navigate(route) { launchSingleTop = true } }
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
            // ── Hero Header ───────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Avatar — show profile pic if available
                    if (!currentUser?.profileImageUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = currentUser?.profileImageUrl,
                            contentDescription = "Profile",
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                (currentUser?.name?.firstOrNull()?.toString() ?: "A").uppercase(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "$greeting,",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                        Text(
                            currentUser?.name ?: "Admin",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    "Organization Admin",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "📅 $today",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Stats 2x2 Grid ────────────────────────────────────────────
            Text(
                "Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AdminStatCard(
                        icon = Icons.Default.People,
                        label = "Teachers",
                        count = teachers.size,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("manage_teachers") }
                    )
                    AdminStatCard(
                        icon = Icons.Default.Person,
                        label = "Students",
                        count = students.size,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("manage_students") }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AdminStatCard(
                        icon = Icons.Default.Class,
                        label = "Classes",
                        count = classes.size,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("manage_classes") }
                    )
                    AdminStatCard(
                        icon = Icons.Default.Book,
                        label = "Courses",
                        count = courseCount,
                        color = Color(0xFFE65100),
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("admin_courses") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Management Section ────────────────────────────────────────
            Text(
                "Management",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ManagementActionCard(
                    icon = Icons.Default.DateRange,
                    title = "Manage Class Routine",
                    subtitle = "Set class timetable: subject, day & time duration",
                    badge = "${classes.size} classes",
                    badgeColor = MaterialTheme.colorScheme.primary,
                    isPrimary = true,
                    onClick = { navController.navigate("manage_class_routine") }
                )
                ManagementActionCard(
                    icon = Icons.Default.Class,
                    title = "Manage Classes",
                    subtitle = "Create classes & assign teachers",
                    badge = "${classes.size} active",
                    badgeColor = MaterialTheme.colorScheme.tertiary,
                    onClick = { navController.navigate("manage_classes") }
                )
                ManagementActionCard(
                    icon = Icons.Default.People,
                    title = "Manage Teachers",
                    subtitle = "Add or remove teachers from the system",
                    badge = "${teachers.size} total",
                    badgeColor = MaterialTheme.colorScheme.primary,
                    onClick = { navController.navigate("manage_teachers") }
                )
                ManagementActionCard(
                    icon = Icons.Default.Person,
                    title = "Manage Students",
                    subtitle = "Add or remove students, assign to classes",
                    badge = "${students.size} total",
                    badgeColor = MaterialTheme.colorScheme.secondary,
                    onClick = { navController.navigate("manage_students") }
                )
                ManagementActionCard(
                    icon = Icons.Default.Book,
                    title = "Manage Courses",
                    subtitle = "Add or remove courses, assign to classes",
                    badge = "$courseCount total",
                    badgeColor = Color(0xFFE65100),
                    onClick = { navController.navigate("admin_courses") }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

}

@Composable
fun AdminStatCard(
    icon: ImageVector,
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "$count",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ManagementActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: String = "",
    badgeColor: Color = Color.Unspecified,
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (isPrimary) 6.dp else 3.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPrimary)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isPrimary)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isPrimary)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isPrimary)
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (badge.isNotEmpty()) {
                val effectiveBadgeColor = if (badgeColor == Color.Unspecified)
                    MaterialTheme.colorScheme.primary else badgeColor
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = effectiveBadgeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        badge,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = effectiveBadgeColor,
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
