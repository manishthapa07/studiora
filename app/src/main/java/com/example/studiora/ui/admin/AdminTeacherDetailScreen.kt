package com.example.studiora.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.studiora.model.User
import com.example.studiora.viewmodel.AdminViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTeacherDetailScreen(
    teacherId: String,
    navController: NavController,
    adminViewModel: AdminViewModel
) {
    val teachers by adminViewModel.teachers.collectAsStateWithLifecycle()
    val classes by adminViewModel.classes.collectAsStateWithLifecycle()
    val students by adminViewModel.students.collectAsStateWithLifecycle()
    val teacherClassAttendance by adminViewModel.teacherClassAttendance.collectAsStateWithLifecycle()
    val teacherOwnAttendance by adminViewModel.teacherOwnAttendance.collectAsStateWithLifecycle()
    val teacherOwnOverallPct by adminViewModel.teacherOwnOverallPct.collectAsStateWithLifecycle()
    val teacherOwnMonthly by adminViewModel.teacherOwnMonthly.collectAsStateWithLifecycle()
    val teacherOwnRecentDays by adminViewModel.teacherOwnRecentDays.collectAsStateWithLifecycle()
    val operationState by adminViewModel.operationState.collectAsStateWithLifecycle()

    val teacher = teachers.find { it.uid == teacherId }
    val teacherClass = classes.find { it.teacherId == teacherId }
    val classStudents = students.filter { it.classId == teacherClass?.classId }

    var showMarkDialog by remember { mutableStateOf(false) }
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val alreadyMarkedToday = teacherOwnAttendance.any { it.first == today }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(teacherId) {
        adminViewModel.resetOperationState() // clear stale state
        adminViewModel.loadTeachers()
        adminViewModel.refreshUserById(teacherId) // force-refresh this specific teacher
        adminViewModel.loadClasses()
        adminViewModel.loadStudents()
        adminViewModel.loadTeacherOwnAttendance(teacherId)
    }
    LaunchedEffect(teacherClass) {
        teacherClass?.classId?.let { adminViewModel.loadTeacherClassAttendance(it) }
    }

    DisposableEffect(Unit) {
        onDispose { adminViewModel.clearSelectedUserAttendance() }
    }

    // Show toast on result, do NOT touch showMarkDialog here
    LaunchedEffect(operationState) {
        when (val s = operationState) {
            is com.example.studiora.viewmodel.OperationState.Success -> {
                if (s.message.isNotEmpty()) {
                    android.widget.Toast.makeText(context, s.message, android.widget.Toast.LENGTH_SHORT).show()
                }
                adminViewModel.resetOperationState()
            }
            is com.example.studiora.viewmodel.OperationState.Error -> {
                android.widget.Toast.makeText(context, s.message, android.widget.Toast.LENGTH_LONG).show()
                adminViewModel.resetOperationState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Teacher Details", fontWeight = FontWeight.Bold)
                        teacher?.let {
                            Text(it.name, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (teacher == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            // Teacher Profile Card
            item { TeacherProfileCard(teacher = teacher, teacherClass = teacherClass) }

            // Class Stats
            item {
                TeacherClassStatsCard(
                    className = teacherClass?.name ?: "No class assigned",
                    studentCount = classStudents.size
                )
            }

            // ── Teacher Own Attendance Section ───────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(title = "Teacher Attendance", icon = Icons.Default.HowToReg)
                    Button(
                        onClick = { showMarkDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (alreadyMarkedToday)
                                MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            if (alreadyMarkedToday) Icons.Default.EditCalendar else Icons.Default.AddTask,
                            null, modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (alreadyMarkedToday) "Update Today" else "Mark Today",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Teacher overall attendance card
            item { TeacherOverallAttendanceCard(overallPct = teacherOwnOverallPct) }

            // Monthly
            item { SectionHeader(title = "Monthly Breakdown", icon = Icons.Default.CalendarMonth) }
            if (teacherOwnMonthly.isEmpty()) {
                item { EmptyDataHint("No attendance recorded yet") }
            } else {
                items(teacherOwnMonthly.entries.toList()) { (month, pct) ->
                    MonthlyAttendanceRow(month = month, percentage = pct)
                }
            }

            // Recent 10 days
            item { SectionHeader(title = "Recent 10 Days (Day-wise)", icon = Icons.Default.DateRange) }
            if (teacherOwnRecentDays.isEmpty()) {
                item { EmptyDataHint("No recent records") }
            } else {
                items(teacherOwnRecentDays) { (date, status) ->
                    DayAttendanceRow(date = date, status = status)
                }
            }

            // ── Students Attendance in teacher's class ───────────────────────
            if (teacherClass != null) {
                item {
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(4.dp))
                    SectionHeader(
                        title = "Students in ${teacherClass.name}",
                        icon = Icons.Default.People
                    )
                }
                if (classStudents.isEmpty()) {
                    item { EmptyDataHint("No students in this class yet") }
                } else {
                    items(classStudents) { student ->
                        val pct = teacherClassAttendance[student.uid] ?: 0f
                        StudentAttendanceSummaryRow(
                            student = student,
                            percentage = pct,
                            onClick = { navController.navigate("admin_student_detail/${student.uid}") }
                        )
                    }
                }
            } else {
                item { EmptyDataHint("Teacher not assigned to any class") }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    // Mark Attendance Dialog
    if (showMarkDialog) {
        MarkTeacherAttendanceDialog(
            teacherName = teacher?.name ?: "",
            today = today,
            currentStatus = teacherOwnAttendance.find { it.first == today }?.second,
            onDismiss = { showMarkDialog = false },
            onMark = { status ->
                adminViewModel.markTeacherAttendance(teacherId, today, status)
                showMarkDialog = false  // close immediately — don't wait for LaunchedEffect
            }
        )
    }
}

@Composable
fun MarkTeacherAttendanceDialog(
    teacherName: String,
    today: String,
    currentStatus: String?,
    onDismiss: () -> Unit,
    onMark: (String) -> Unit
) {
    var selected by remember { mutableStateOf(currentStatus ?: "PRESENT") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.HowToReg, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Mark Attendance",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                HorizontalDivider()

                // Teacher info
                Text(
                    "Teacher: $teacherName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Date: $today",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Present / Absent toggle row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val presentSelected = selected == "PRESENT"
                    // Present
                    OutlinedButton(
                        onClick = { selected = "PRESENT" },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (presentSelected)
                                Color(0xFF2E7D32).copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surface
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (presentSelected) 2.dp else 1.dp,
                            color = if (presentSelected) Color(0xFF2E7D32)
                            else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Icon(
                            Icons.Default.CheckCircle, null,
                            modifier = Modifier.size(18.dp),
                            tint = if (presentSelected) Color(0xFF2E7D32)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Present",
                            color = if (presentSelected) Color(0xFF2E7D32)
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (presentSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    val absentSelected = selected == "ABSENT"
                    // Absent
                    OutlinedButton(
                        onClick = { selected = "ABSENT" },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (absentSelected)
                                Color(0xFFC62828).copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surface
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (absentSelected) 2.dp else 1.dp,
                            color = if (absentSelected) Color(0xFFC62828)
                            else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Icon(
                            Icons.Default.Cancel, null,
                            modifier = Modifier.size(18.dp),
                            tint = if (absentSelected) Color(0xFFC62828)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Absent",
                            color = if (absentSelected) Color(0xFFC62828)
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (absentSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                HorizontalDivider()

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cancel
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }
                    // Confirm
                    Button(
                        onClick = { onMark(selected) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Confirm", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun TeacherOverallAttendanceCard(overallPct: Float) {
    val color = when {
        overallPct >= 75f -> Color(0xFF2E7D32)
        overallPct >= 50f -> Color(0xFFF57F17)
        else -> Color(0xFFC62828)
    }
    Card(
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { overallPct / 100f },
                    modifier = Modifier.fillMaxSize(),
                    color = color,
                    trackColor = color.copy(alpha = 0.15f),
                    strokeWidth = 7.dp
                )
                Text(
                    "${overallPct.toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
            }
            Spacer(Modifier.width(20.dp))
            Column {
                Text(
                    "Overall Attendance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                val badge = when {
                    overallPct >= 75f -> "✅ Good Standing"
                    overallPct >= 50f -> "⚠️ Needs Improvement"
                    else -> "❌ Critical — Below 50%"
                }
                Text(badge, style = MaterialTheme.typography.bodySmall, color = color,
                    fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun TeacherProfileCard(teacher: User, teacherClass: com.example.studiora.model.Class?) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar / Photo
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (teacher.profileImageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = teacher.profileImageUrl,
                            contentDescription = "Profile photo",
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize()
                                .background(MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                teacher.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(teacher.name, style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondary)
                    Spacer(Modifier.height(4.dp))
                    TeacherDetailChip(label = teacher.email, icon = Icons.Default.Email)
                    if (teacher.phone.isNotEmpty()) {
                        Spacer(Modifier.height(3.dp))
                        TeacherDetailChip(label = teacher.phone, icon = Icons.Default.Phone)
                    }
                    if (teacher.subject.isNotEmpty()) {
                        Spacer(Modifier.height(3.dp))
                        TeacherDetailChip(label = "Subject: ${teacher.subject}", icon = Icons.Default.Book)
                    }
                    teacherClass?.let {
                        Spacer(Modifier.height(3.dp))
                        TeacherDetailChip(label = "Class: ${it.name}", icon = Icons.Default.Class)
                    }
                }
            }
            // Extra details
            if (teacher.address.isNotEmpty() || teacher.parentName.isNotEmpty() ||
                teacher.parentPhone.isNotEmpty() || teacher.documentUrl.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.2f))
                Spacer(Modifier.height(10.dp))
                if (teacher.address.isNotEmpty()) {
                    TeacherDetailChip(label = teacher.address, icon = Icons.Default.Home)
                    Spacer(Modifier.height(4.dp))
                }
                if (teacher.parentName.isNotEmpty()) {
                    TeacherDetailChip(label = "Parent/Guardian: ${teacher.parentName}", icon = Icons.Default.FamilyRestroom)
                    Spacer(Modifier.height(4.dp))
                }
                if (teacher.parentPhone.isNotEmpty()) {
                    TeacherDetailChip(label = "Parent Phone: ${teacher.parentPhone}", icon = Icons.Default.Phone)
                    Spacer(Modifier.height(4.dp))
                }
                if (teacher.documentUrl.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Description, null, modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.8f))
                        Spacer(Modifier.width(4.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.2f)
                        ) {
                            Text("Document uploaded ✓",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TeacherDetailChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(12.dp),
            tint = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.8f))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.9f))
    }
}

@Composable
fun TeacherClassStatsCard(className: String, studentCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TeacherStatItem(
                icon = Icons.Default.Class,
                label = "Assigned Class",
                value = className,
                color = MaterialTheme.colorScheme.primary
            )
            VerticalDivider(modifier = Modifier.height(48.dp))
            TeacherStatItem(
                icon = Icons.Default.People,
                label = "Total Students",
                value = "$studentCount",
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
fun TeacherStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
    }
}

@Composable
fun StudentAttendanceSummaryRow(student: User, percentage: Float, onClick: () -> Unit) {
    val color = when {
        percentage >= 75f -> Color(0xFF2E7D32)
        percentage >= 50f -> Color(0xFFF57F17)
        else -> Color(0xFFC62828)
    }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    student.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(student.name, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold)
                Text(student.email, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${percentage.toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
                LinearProgressIndicator(
                    progress = { percentage / 100f },
                    modifier = Modifier.width(60.dp).height(5.dp).clip(RoundedCornerShape(3.dp)),
                    color = color,
                    trackColor = color.copy(alpha = 0.15f)
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp))
        }
    }
}

