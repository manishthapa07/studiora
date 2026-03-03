package com.example.studiora.ui.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.studiora.ui.common.StudioraBottomNav
import com.example.studiora.ui.common.teacherNavItems
import com.example.studiora.viewmodel.AuthViewModel
import com.example.studiora.viewmodel.TeacherViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDashboardScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    teacherViewModel: TeacherViewModel
) {
    val currentUser by authViewModel.currentUserData.collectAsStateWithLifecycle()
    val classes by teacherViewModel.classes.collectAsStateWithLifecycle()
    val courses by teacherViewModel.courses.collectAsStateWithLifecycle()
    val ownOverallPct by teacherViewModel.ownOverallPct.collectAsStateWithLifecycle()
    val ownRecentDays by teacherViewModel.ownRecentDays.collectAsStateWithLifecycle()

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
        currentUser?.uid?.let {
            teacherViewModel.loadClassesByTeacher(it)
            teacherViewModel.loadCoursesByTeacher(it)
            teacherViewModel.loadOwnAttendance(it)
        }
    }

    val today = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
    val todaysClasses = classes.flatMap { cls ->
        cls.schedule
            .filter { it.day.equals(today, ignoreCase = true) }
            .map { scheduleItem -> Triple(cls, scheduleItem, courses.find { c -> c.courseId == scheduleItem.courseId }) }
    }.sortedBy { it.second.startTime }

    val pctPresent = ownRecentDays.count { it.second == "PRESENT" }
    val pctAbsent = ownRecentDays.count { it.second == "ABSENT" }

    val attendanceColor = when {
        ownOverallPct >= 75f -> Color(0xFF2E7D32)
        ownOverallPct >= 50f -> Color(0xFFF57F17)
        else -> Color(0xFFC62828)
    }

    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Good Morning"
        hour < 17 -> "Good Afternoon"
        else -> "Good Evening"
    }
    val todayFormatted = SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(Date())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Studiora", fontWeight = FontWeight.Bold)
                        Text(
                            "Teacher",
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
                                    (currentUser?.name?.firstOrNull()?.toString() ?: "T").uppercase(),
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
                items = teacherNavItems,
                currentRoute = "teacher_dashboard",
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
                                (currentUser?.name?.firstOrNull()?.toString() ?: "T").uppercase(),
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
                            currentUser?.name ?: "Teacher",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!currentUser?.subject.isNullOrEmpty()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text(
                                        currentUser!!.subject,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                "📅 $todayFormatted",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            )
                        }
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
                TeacherStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Class,
                    iconBg = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary,
                    value = "${classes.size}",
                    label = "Classes",
                    onClick = { navController.navigate("teacher_weekly_routine") }
                )
                TeacherStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Book,
                    iconBg = MaterialTheme.colorScheme.secondaryContainer,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    value = "${courses.size}",
                    label = "Courses",
                    onClick = { navController.navigate("teacher_courses") }
                )
                TeacherStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.HowToReg,
                    iconBg = when {
                        ownOverallPct >= 75f -> Color(0xFFE8F5E9)
                        ownOverallPct >= 50f -> Color(0xFFFFF8E1)
                        else -> Color(0xFFFFEBEE)
                    },
                    iconTint = attendanceColor,
                    value = "${ownOverallPct.toInt()}%",
                    label = "Attendance",
                    onClick = { }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── My Attendance Card ────────────────────────────────────────
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
                        Text("My Attendance", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        val statusText = when {
                            ownOverallPct >= 75f -> "Good"
                            ownOverallPct >= 50f -> "Warning"
                            ownRecentDays.isEmpty() -> "No Data"
                            else -> "Critical"
                        }
                        Surface(
                            color = attendanceColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                statusText,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                color = attendanceColor,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { (ownOverallPct / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = attendanceColor,
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
                        TeacherAttendancePill(label = "Present", count = pctPresent, color = Color(0xFF2E7D32), modifier = Modifier.weight(1f))
                        TeacherAttendancePill(label = "Absent", count = pctAbsent, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                        TeacherAttendancePill(label = "Recorded", count = ownRecentDays.size, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
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
                        onClick = { navController.navigate("teacher_weekly_routine") },
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
                    todaysClasses.forEachIndexed { index, (cls, scheduleItem, course) ->
                        val accentColors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary,
                            MaterialTheme.colorScheme.tertiary,
                            Color(0xFF7B1FA2),
                            Color(0xFF00838F)
                        )
                        val accent = accentColors[index % accentColors.size]
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
                                Box(
                                    modifier = Modifier
                                        .width(5.dp)
                                        .height(80.dp)
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
                                            "📚 ${cls.name}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = {
                                                val cName = (course?.name ?: scheduleItem.courseId).replace("/", "-")
                                                if (scheduleItem.courseId.isNotEmpty()) {
                                                    // Course-specific attendance
                                                    navController.navigate("course_attendance/${cls.classId}/${cls.name}/${scheduleItem.courseId}/$cName")
                                                } else {
                                                    navController.navigate("attendance/${cls.classId}/${cls.name}")
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, "Mark Attendance", tint = accent, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── My Classes ────────────────────────────────────────────────
            if (classes.isNotEmpty()) {
                Text(
                    "My Classes",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    classes.forEachIndexed { index, cls ->
                        val accentColors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary,
                            MaterialTheme.colorScheme.tertiary,
                            Color(0xFF7B1FA2),
                            Color(0xFF00838F)
                        )
                        val accent = accentColors[index % accentColors.size]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(3.dp, RoundedCornerShape(14.dp)),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(5.dp)
                                        .height(72.dp)
                                        .background(accent, RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(accent.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Class, null, tint = accent, modifier = Modifier.size(24.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(cls.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            "${cls.schedule.size} schedule slot(s)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { navController.navigate("teacher_students/${cls.classId}") }) {
                                        Icon(Icons.Default.People, "Students", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { navController.navigate("attendance/${cls.classId}/${cls.name}") }) {
                                        Icon(Icons.Default.CheckCircle, "Attendance", tint = accent)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

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
                TeacherQuickActionCard(
                    icon = Icons.Default.Book,
                    iconBg = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "My Courses",
                    subtitle = "${courses.size} course(s) across ${classes.size} class(es)",
                    onClick = { navController.navigate("teacher_courses") }
                )
                TeacherQuickActionCard(
                    icon = Icons.Default.CheckCircle,
                    iconBg = Color(0xFFE8F5E9),
                    iconTint = Color(0xFF2E7D32),
                    title = "Mark Attendance",
                    subtitle = if (classes.isNotEmpty()) "Tap to mark attendance for ${classes.first().name}" else "No class assigned yet",
                    onClick = {
                        if (classes.isNotEmpty()) navController.navigate("attendance/${classes.first().classId}/${classes.first().name}")
                    }
                )
                TeacherQuickActionCard(
                    icon = Icons.Default.People,
                    iconBg = MaterialTheme.colorScheme.secondaryContainer,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    title = "My Students",
                    subtitle = if (classes.isNotEmpty()) "View students in ${classes.first().name}" else "No class assigned yet",
                    onClick = {
                        if (classes.isNotEmpty()) navController.navigate("teacher_students/${classes.first().classId}")
                    }
                )
                TeacherQuickActionCard(
                    icon = Icons.Default.CalendarMonth,
                    iconBg = MaterialTheme.colorScheme.tertiaryContainer,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    title = "Weekly Routine",
                    subtitle = "View your full weekly class schedule",
                    onClick = { navController.navigate("teacher_weekly_routine") }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

}

@Composable
private fun TeacherStatCard(
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
private fun TeacherAttendancePill(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
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
private fun TeacherQuickActionCard(
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

// Keep TeacherOwnAttendanceSummaryCard for backward-compat in case it's referenced elsewhere
@Composable
fun TeacherOwnAttendanceSummaryCard(
    overallPct: Float,
    recentDays: List<Pair<String, String>>
) {
    // Delegated to inline content in dashboard now, kept for compatibility
}
