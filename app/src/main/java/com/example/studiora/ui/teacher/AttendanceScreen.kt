package com.example.studiora.ui.teacher

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.studiora.model.AttendanceRecord
import com.example.studiora.model.User
import com.example.studiora.viewmodel.AuthViewModel
import com.example.studiora.viewmodel.OperationState
import com.example.studiora.viewmodel.TeacherViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    navController: NavController,
    classId: String,
    className: String,
    authViewModel: AuthViewModel,
    teacherViewModel: TeacherViewModel,
    courseId: String = "",          // "" = whole-class attendance
    courseName: String = ""         // "" = whole-class attendance
) {
    val currentUser by authViewModel.currentUserData.collectAsStateWithLifecycle()
    val students by teacherViewModel.students.collectAsStateWithLifecycle()
    val todayAttendanceRecords by teacherViewModel.todayAttendanceRecords.collectAsStateWithLifecycle()
    val todayAttendanceLoaded by teacherViewModel.todayAttendanceLoaded.collectAsStateWithLifecycle()
    val classAttendanceSummary by teacherViewModel.classAttendanceSummary.collectAsStateWithLifecycle()
    val courseAttendanceSummary by teacherViewModel.courseAttendanceSummary.collectAsStateWithLifecycle()
    val operationState by teacherViewModel.operationState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // true = this is a course-specific attendance session
    val isCourseAttendance = courseId.isNotEmpty()

    // The summary shown in the Overview tab — course-specific when in course mode, class-wide otherwise
    val overviewSummary = if (isCourseAttendance) courseAttendanceSummary else classAttendanceSummary

    var attendanceMap by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    // Track which (date) the current attendanceMap was populated for
    var initializedForDate by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    // Last 5 dates (today first)
    val recentDates = remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        (0 until 5).map { offset ->
            cal.time = Date()
            cal.add(Calendar.DAY_OF_YEAR, -offset)
            sdf.format(cal.time)
        }
    }
    var selectedDate by remember { mutableStateOf(today) }

    LaunchedEffect(classId) {
        teacherViewModel.loadStudentsByClass(classId)
        teacherViewModel.loadClassAttendanceSummary(classId)
    }

    // Load course summary whenever we enter course mode
    LaunchedEffect(courseId) {
        if (courseId.isNotEmpty()) teacherViewModel.loadCourseAttendanceSummary(courseId)
    }

    // Single source of truth: reload attendance whenever date changes
    // Pass courseId so course-specific records are fetched (not all class records)
    LaunchedEffect(classId, selectedDate, courseId) {
        initializedForDate = "" // mark as not yet initialized for this date
        teacherViewModel.loadTodayAttendance(classId, selectedDate, courseId)
    }

    // Pre-populate attendanceMap ONLY when:
    //  1. students are loaded
    //  2. records for the CURRENT selectedDate have been fetched (loaded flag = true)
    //  3. we haven't already built the map for this date
    LaunchedEffect(students, todayAttendanceLoaded, selectedDate) {
        if (students.isNotEmpty() && todayAttendanceLoaded && initializedForDate != selectedDate) {
            val savedStatusMap = todayAttendanceRecords.associate { it.studentId to (it.status == "PRESENT") }
            attendanceMap = students.associate { student ->
                student.uid to (savedStatusMap[student.uid] ?: false)
            }
            initializedForDate = selectedDate
        }
    }

    LaunchedEffect(operationState) {
        when (val state = operationState) {
            is OperationState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                teacherViewModel.resetOperationState()
            }
            is OperationState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                teacherViewModel.resetOperationState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            if (isCourseAttendance) "Course Attendance" else "Attendance",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (isCourseAttendance) "$courseName • $selectedDate" else "$className • $selectedDate",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
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
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Show course vs class badge when course-specific
            if (isCourseAttendance) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Book,
                            null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Course-specific attendance for: $courseName  •  Class: $className",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Manual") },
                    icon = { Icon(Icons.AutoMirrored.Filled.FormatListBulleted, null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Overview") },
                    icon = { Icon(Icons.Default.BarChart, null) }
                )
            }

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
                AnimatedContent(targetState = selectedTab, label = "attendance_tab") { tab ->
                    when (tab) {
                        0 -> ManualAttendanceTab(
                            students = students,
                            attendanceMap = attendanceMap,
                            selectedDate = selectedDate,
                            recentDates = recentDates,
                            onDateSelected = { date -> selectedDate = date },
                            onToggle = { uid ->
                                attendanceMap = attendanceMap.toMutableMap().apply {
                                    this[uid] = !(this[uid] ?: false)
                                }
                            },
                            onMarkAll = { attendanceMap = students.associate { it.uid to true } },
                            onMarkNone = { attendanceMap = students.associate { it.uid to false } },
                            onSubmit = {
                                val records = students.map { student ->
                                    // Stable record ID:
                                    //   Course attendance: classId_courseId_studentId_date
                                    //   Whole-class attendance: classId_studentId_date
                                    val recordId = if (isCourseAttendance)
                                        "${classId}_${courseId}_${student.uid}_${selectedDate}"
                                    else
                                        "${classId}_${student.uid}_${selectedDate}"
                                    AttendanceRecord(
                                        recordId = recordId,
                                        studentId = student.uid,
                                        studentName = student.name,
                                        classId = classId,
                                        courseId = courseId,   // "" for whole-class
                                        date = selectedDate,
                                        status = if (attendanceMap[student.uid] == true) "PRESENT" else "ABSENT",
                                        markedBy = currentUser?.uid ?: "",
                                        markedByName = currentUser?.name ?: "",
                                        method = "MANUAL"
                                    )
                                }
                                teacherViewModel.markBulkAttendance(records, classId, courseId)
                            }
                        )
                        1 -> ClassAttendanceOverviewTab(
                            students = students,
                            classAttendanceSummary = overviewSummary,
                            isCourseAttendance = isCourseAttendance,
                            courseOrClassName = if (isCourseAttendance) courseName else className
                        )
                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
fun ManualAttendanceTab(
    students: List<User>,
    attendanceMap: Map<String, Boolean>,
    selectedDate: String,
    recentDates: List<String>,
    onDateSelected: (String) -> Unit,
    onToggle: (String) -> Unit,
    onMarkAll: () -> Unit,
    onMarkNone: () -> Unit,
    onSubmit: () -> Unit
) {
    val presentCount = attendanceMap.values.count { it }
    val total = students.size
    val today = recentDates.firstOrNull() ?: selectedDate

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Date selector chips — last 5 days
        Text(
            "Select Date",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(recentDates) { date ->
                val isSelected = date == selectedDate
                val label = when (date) {
                    today -> "Today"
                    else -> {
                        val sdf = SimpleDateFormat("EEE, dd MMM", Locale.getDefault())
                        try { sdf.format(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)!!) } catch (e: Exception) { date }
                    }
                }
                FilterChip(
                    selected = isSelected,
                    onClick = { onDateSelected(date) },
                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(3.dp, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Present: $presentCount / $total",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Absent: ${total - presentCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Row {
                        TextButton(onClick = onMarkAll) { Text("All Present") }
                        TextButton(onClick = onMarkNone) { Text("All Absent") }
                    }
                }
                if (total > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { presentCount.toFloat() / total },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF2E7D32),
                        trackColor = MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (students.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.People, null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No students in this class", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(students) { index, student ->
                    val isPresent = attendanceMap[student.uid] ?: false
                    val accentColors = listOf(
                        Color(0xFF1565C0), Color(0xFF2E7D32), Color(0xFF6A1B9A),
                        Color(0xFF00838F), Color(0xFFE65100), Color(0xFFC62828)
                    )
                    val accent = accentColors[index % accentColors.size]

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isPresent)
                                Color(0xFFE8F5E9)
                            else
                                MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isPresent) Color(0xFF2E7D32).copy(alpha = 0.15f)
                                        else accent.copy(alpha = 0.12f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    student.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPresent) Color(0xFF2E7D32) else accent
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(student.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    if (isPresent) "✓ Present" else "✗ Absent",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isPresent) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                )
                            }
                            Switch(
                                checked = isPresent,
                                onCheckedChange = { onToggle(student.uid) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF2E7D32),
                                    checkedTrackColor = Color(0xFF2E7D32).copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = students.isNotEmpty()
        ) {
            Icon(Icons.Default.Check, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Submit Attendance ($presentCount/${students.size} present)", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun ClassAttendanceOverviewTab(
    students: List<User>,
    classAttendanceSummary: Map<String, Float>,
    isCourseAttendance: Boolean = false,
    courseOrClassName: String = ""
) {
    if (students.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.BarChart, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(8.dp))
                Text("No students to show", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    val sorted = students.sortedByDescending { classAttendanceSummary[it.uid] ?: 0f }
    val avgPct = if (classAttendanceSummary.isNotEmpty())
        classAttendanceSummary.values.average().toFloat() else 0f

    val accentColor = if (isCourseAttendance) Color(0xFF2E7D32) else Color(0xFF1565C0)
    val summaryTitle = if (isCourseAttendance) "Course Average  •  $courseOrClassName" else "Class Average Attendance"
    val aboveCount = students.count { (classAttendanceSummary[it.uid] ?: 0f) >= 75f }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Average card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCourseAttendance)
                        Color(0xFF2E7D32).copy(alpha = 0.12f)
                    else
                        MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { (avgPct / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.size(80.dp),
                            strokeWidth = 8.dp,
                            color = when {
                                avgPct >= 75f -> Color(0xFF2E7D32)
                                avgPct >= 50f -> Color(0xFFF57F17)
                                else -> MaterialTheme.colorScheme.error
                            },
                            trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        )
                        Text(
                            "${avgPct.toInt()}%",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Column {
                        // Course/class badge
                        if (isCourseAttendance) {
                            Surface(
                                color = Color(0xFF2E7D32).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Book, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Course", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Text(
                            summaryTitle,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "$aboveCount / ${students.size} students ≥ 75%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (classAttendanceSummary.isEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "No sessions recorded yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Individual Attendance (sorted)",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        itemsIndexed(sorted) { index, student ->
            val pct = classAttendanceSummary[student.uid] ?: 0f
            val hasRecord = classAttendanceSummary.containsKey(student.uid)
            val pctColor = when {
                !hasRecord -> MaterialTheme.colorScheme.outline
                pct >= 75f -> Color(0xFF2E7D32)
                pct >= 50f -> Color(0xFFF57F17)
                else -> Color(0xFFC62828)
            }
            val accentColors = if (isCourseAttendance)
                listOf(Color(0xFF2E7D32), Color(0xFF1B5E20), Color(0xFF388E3C), Color(0xFF00695C), Color(0xFF004D40))
            else
                listOf(Color(0xFF1565C0), Color(0xFF2E7D32), Color(0xFF6A1B9A), Color(0xFF00838F), Color(0xFFE65100), Color(0xFFC62828))
            val accent = accentColors[index % accentColors.size]

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(22.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            student.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            fontWeight = FontWeight.Bold,
                            color = accent,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(student.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        if (!hasRecord) {
                            Text(
                                if (isCourseAttendance) "No sessions recorded" else "No records yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        } else {
                            LinearProgressIndicator(
                                progress = { (pct / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = pctColor,
                                trackColor = pctColor.copy(alpha = 0.15f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = pctColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            if (hasRecord) "${pct.toInt()}%" else "—",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Bold,
                            color = pctColor,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

