package com.example.studiora.ui.student

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ExitToApp
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.studiora.ui.common.LogoutConfirmDialog
import com.example.studiora.ui.common.StudioraBottomNav
import com.example.studiora.ui.common.studentNavItems
import com.example.studiora.viewmodel.AuthViewModel
import com.example.studiora.viewmodel.StudentViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboardScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    studentViewModel: StudentViewModel
) {
    val currentUser by authViewModel.currentUserData.collectAsStateWithLifecycle()
    val attendancePercentage by studentViewModel.attendancePercentage.collectAsStateWithLifecycle()
    // Use class-level records only for dashboard stats (course records are separate)
    val classAttendanceRecords by studentViewModel.classAttendanceRecords.collectAsStateWithLifecycle()
    val courses by studentViewModel.courses.collectAsStateWithLifecycle()
    val classSchedule by studentViewModel.classSchedule.collectAsStateWithLifecycle()
    var showLogoutDialog by remember { mutableStateOf(false) }

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

    LaunchedEffect(Unit) { authViewModel.loadCurrentUser() }
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            studentViewModel.loadAttendanceByStudent(user.uid)
            if (user.classId.isNotEmpty()) {
                studentViewModel.loadCoursesByClass(user.classId)
                studentViewModel.loadClassSchedule(user.classId)
            }
        }
    }

    val today = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
    val todayFormatted = SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(Date())
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Good Morning"
        hour < 17 -> "Good Afternoon"
        else -> "Good Evening"
    }
    val todaysClasses = classSchedule?.schedule?.filter {
        it.day.equals(today, ignoreCase = true)
    }?.sortedBy { it.startTime } ?: emptyList()

    val presentCount = classAttendanceRecords.count { it.status == "PRESENT" }
    val absentCount = classAttendanceRecords.count { it.status == "ABSENT" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Studiora", fontWeight = FontWeight.Bold)
                        Text(
                            "Student",
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
                                    (currentUser?.name?.firstOrNull()?.toString() ?: "S").uppercase(),
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
                items = studentNavItems,
                currentRoute = "student_dashboard",
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
                    // Avatar circle — show profile pic if available
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
                                text = (currentUser?.name?.firstOrNull()?.toString() ?: "S").uppercase(),
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
                            currentUser?.name ?: "Student",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            "📅 $todayFormatted",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Stats Row ─────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Courses card
                StudentStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    iconBg = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary,
                    value = "${courses.size}",
                    label = "Courses",
                    onClick = { navController.navigate("student_courses") }
                )
                // Attendance card
                StudentStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.HowToReg,
                    iconBg = when {
                        attendancePercentage >= 75f -> Color(0xFFE8F5E9)
                        attendancePercentage >= 50f -> Color(0xFFFFF8E1)
                        else -> Color(0xFFFFEBEE)
                    },
                    iconTint = when {
                        attendancePercentage >= 75f -> Color(0xFF2E7D32)
                        attendancePercentage >= 50f -> Color(0xFFF57F17)
                        else -> Color(0xFFC62828)
                    },
                    value = "${attendancePercentage.toInt()}%",
                    label = "Attendance",
                    onClick = { navController.navigate("student_attendance") }
                )
                // Present/Absent card
                StudentStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CalendarToday,
                    iconBg = MaterialTheme.colorScheme.secondaryContainer,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    value = "$presentCount",
                    label = "Days Present",
                    onClick = { navController.navigate("student_attendance") }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Attendance Progress ───────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Attendance Progress", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        val statusColor = when {
                            attendancePercentage >= 75f -> Color(0xFF2E7D32)
                            attendancePercentage >= 50f -> Color(0xFFF57F17)
                            else -> MaterialTheme.colorScheme.error
                        }
                        val statusText = when {
                            attendancePercentage >= 75f -> "Good"
                            attendancePercentage >= 50f -> "Warning"
                            classAttendanceRecords.isEmpty() -> "No Data"
                            else -> "Critical"
                        }
                        Surface(
                            color = statusColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                statusText,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                color = statusColor,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { (attendancePercentage / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = when {
                            attendancePercentage >= 75f -> Color(0xFF2E7D32)
                            attendancePercentage >= 50f -> Color(0xFFF57F17)
                            else -> MaterialTheme.colorScheme.error
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("0%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Target: 75%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("100%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AttendancePill(label = "Present", count = presentCount, color = Color(0xFF2E7D32), modifier = Modifier.weight(1f))
                        AttendancePill(label = "Absent", count = absentCount, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                        AttendancePill(label = "Total", count = classAttendanceRecords.size, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Today's Classes ───────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Today's Classes", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(today, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        onClick = { navController.navigate("student_weekly_routine") },
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            "Full Routine",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            if (todaysClasses.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Weekend, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No classes today 🎉", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                        Text("Enjoy your free time!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    todaysClasses.forEachIndexed { index, scheduleItem ->
                        val course = courses.find { it.courseId == scheduleItem.courseId }
                        val colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary,
                            MaterialTheme.colorScheme.tertiary,
                            Color(0xFF7B1FA2),
                            Color(0xFF00838F)
                        )
                        val accent = colors[index % colors.size]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp, RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Color accent bar
                                Box(
                                    modifier = Modifier
                                        .width(5.dp)
                                        .height(72.dp)
                                        .background(accent, RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            course?.name ?: scheduleItem.courseId,
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            course?.teacherName?.let { "👤 $it" } ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(
                                        color = accent.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(14.dp), tint = accent)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "${scheduleItem.startTime} - ${scheduleItem.endTime}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = accent,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Quick Actions ─────────────────────────────────────────────
            Text(
                "Quick Actions",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StudentQuickActionCard(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    iconBg = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "My Courses",
                    subtitle = "${courses.size} course(s) enrolled in your class",
                    onClick = { navController.navigate("student_courses") }
                )
                StudentQuickActionCard(
                    icon = Icons.Default.HowToReg,
                    iconBg = when {
                        attendancePercentage >= 75f -> Color(0xFFE8F5E9)
                        else -> Color(0xFFFFEBEE)
                    },
                    iconTint = when {
                        attendancePercentage >= 75f -> Color(0xFF2E7D32)
                        else -> MaterialTheme.colorScheme.error
                    },
                    title = "My Attendance",
                    subtitle = "Overall: ${attendancePercentage.toInt()}% — ${presentCount} present, ${absentCount} absent",
                    onClick = { navController.navigate("student_attendance") }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showLogoutDialog) {
        LogoutConfirmDialog(
            onConfirm = {
                authViewModel.signOut()
                showLogoutDialog = false
                navController.navigate("login") { popUpTo(0) { inclusive = true } }
            },
            onDismiss = { showLogoutDialog = false }
        )
    }
}

@Composable
private fun StudentStatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    value: String,
    label: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .shadow(3.dp, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun AttendancePill(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("$count", fontWeight = FontWeight.Bold, color = color, fontSize = 18.sp)
            Text(label, style = MaterialTheme.typography.bodySmall, color = color.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun StudentQuickActionCard(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
