package com.example.studiora.ui.student

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.studiora.model.AttendanceRecord
import com.example.studiora.viewmodel.AuthViewModel
import com.example.studiora.viewmodel.StudentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentAttendanceScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    studentViewModel: StudentViewModel
) {
    val currentUser by authViewModel.currentUserData.collectAsStateWithLifecycle()
    val courses by studentViewModel.courses.collectAsStateWithLifecycle()

    // Day/class-level records & percentage (courseId = "")
    val classAttendanceRecords by studentViewModel.classAttendanceRecords.collectAsStateWithLifecycle()
    val attendancePercentage by studentViewModel.attendancePercentage.collectAsStateWithLifecycle()

    // Course-specific records grouped by courseId
    val courseAttendanceMap by studentViewModel.courseAttendanceMap.collectAsStateWithLifecycle()
    val courseAttendancePct by studentViewModel.courseAttendancePct.collectAsStateWithLifecycle()

    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            studentViewModel.loadAttendanceByStudent(user.uid)
            if (user.classId.isNotEmpty()) studentViewModel.loadCoursesByClass(user.classId)
        }
    }

    val classPresent = classAttendanceRecords.count { it.status == "PRESENT" }
    val classAbsent = classAttendanceRecords.count { it.status == "ABSENT" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Attendance", fontWeight = FontWeight.Bold) },
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
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                // ── Section 1: Class / Day-level Attendance ─────────────────
                item {
                    SectionHeader(
                        title = "Class Attendance",
                        subtitle = "Whole-day sessions marked by your teacher",
                        icon = Icons.Default.CalendarMonth,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    AttendanceSummaryCard(
                        percentage = attendancePercentage,
                        presentCount = classPresent,
                        absentCount = classAbsent,
                        total = classAttendanceRecords.size,
                        label = "Overall Class"
                    )
                }

                if (classAttendanceRecords.isEmpty()) {
                    item {
                        EmptyAttendanceBox(message = "No class-level attendance recorded yet")
                    }
                } else {
                    items(classAttendanceRecords) { record ->
                        AttendanceHistoryCard(record = record, courseName = null)
                    }
                }

                // ── Section 2: Course-wise Attendance ───────────────────────
                item { Spacer(modifier = Modifier.height(8.dp)) }
                item {
                    SectionHeader(
                        title = "Course Attendance",
                        subtitle = "Attendance per individual course",
                        icon = Icons.Default.Book,
                        color = Color(0xFF2E7D32)
                    )
                }

                if (courseAttendanceMap.isEmpty()) {
                    item {
                        EmptyAttendanceBox(message = "No course-specific attendance recorded yet")
                    }
                } else {
                    // For each course that has records, show a sub-card with its own stats + history
                    courseAttendanceMap.forEach { (cId, records) ->
                        val courseName = courses.find { it.courseId == cId }?.name ?: cId
                        val pct = courseAttendancePct[cId] ?: 0f
                        val present = records.count { it.status == "PRESENT" }
                        val absent = records.count { it.status == "ABSENT" }

                        item {
                            CourseAttendanceSection(
                                courseName = courseName,
                                percentage = pct,
                                presentCount = present,
                                absentCount = absent,
                                records = records
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

// ── Reusable composables ─────────────────────────────────────────────────────

@Composable
fun SectionHeader(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AttendanceSummaryCard(
    percentage: Float,
    presentCount: Int,
    absentCount: Int,
    total: Int,
    label: String
) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = when {
                percentage >= 75f -> MaterialTheme.colorScheme.primaryContainer
                percentage >= 50f -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.errorContainer
            }
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { percentage / 100f },
                    modifier = Modifier.size(100.dp),
                    strokeWidth = 9.dp,
                    color = when {
                        percentage >= 75f -> MaterialTheme.colorScheme.primary
                        percentage >= 50f -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.error
                    },
                    trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${percentage.toInt()}%", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                AttendanceStatChip("Present", presentCount, MaterialTheme.colorScheme.primary)
                AttendanceStatChip("Absent", absentCount, MaterialTheme.colorScheme.error)
                AttendanceStatChip("Total", total, MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                when {
                    percentage >= 75f -> "✅ Good standing! Keep it up."
                    percentage >= 50f -> "⚠️ Attendance is below average."
                    total == 0 -> "No attendance records yet."
                    else -> "❌ Critical! Attendance is very low."
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun CourseAttendanceSection(
    courseName: String,
    percentage: Float,
    presentCount: Int,
    absentCount: Int,
    records: List<AttendanceRecord>
) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Book, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(courseName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text("${records.size} session(s)  •  Present: $presentCount  •  Absent: $absentCount",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // Percentage badge
                Surface(
                    color = when {
                        percentage >= 75f -> Color(0xFF2E7D32)
                        percentage >= 50f -> Color(0xFFF57F17)
                        else -> MaterialTheme.colorScheme.error
                    },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        "${percentage.toInt()}%",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (expanded) {
                HorizontalDivider()
                records.forEach { record ->
                    AttendanceHistoryCard(record = record, courseName = courseName)
                }
            }
        }
    }
}

@Composable
fun EmptyAttendanceBox(message: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.EventBusy, null, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(8.dp))
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun AttendanceStatChip(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$count", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun AttendanceHistoryCard(record: AttendanceRecord, courseName: String?) {
    val isPresent = record.status == "PRESENT"
    Card(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (isPresent)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isPresent) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = null,
                tint = if (isPresent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(record.date, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                if (courseName != null) {
                    Text("Course: $courseName", style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E7D32))
                }
                Text("By: ${record.markedByName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isPresent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            ) {
                Text(
                    if (isPresent) "PRESENT" else "ABSENT",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

