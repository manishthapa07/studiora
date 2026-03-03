package com.example.studiora.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.studiora.model.User
import com.example.studiora.viewmodel.AdminViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminStudentDetailScreen(
    studentId: String,
    navController: NavController,
    adminViewModel: AdminViewModel
) {
    val students by adminViewModel.students.collectAsStateWithLifecycle()
    val classes by adminViewModel.classes.collectAsStateWithLifecycle()
    val overallPct by adminViewModel.selectedUserOverallPct.collectAsStateWithLifecycle()
    val monthlyAttendance by adminViewModel.selectedUserMonthlyAttendance.collectAsStateWithLifecycle()
    val recentDays by adminViewModel.selectedUserRecentDays.collectAsStateWithLifecycle()

    val student = students.find { it.uid == studentId }
    val className = classes.find { it.classId == student?.classId }?.name ?: "Unassigned"

    LaunchedEffect(studentId) {
        adminViewModel.loadStudents()          // reload full list
        adminViewModel.refreshUserById(studentId) // also force-refresh this specific user
        adminViewModel.loadClasses()
        adminViewModel.loadStudentAttendance(studentId)
    }

    DisposableEffect(Unit) {
        onDispose { adminViewModel.clearSelectedUserAttendance() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Student Details", fontWeight = FontWeight.Bold)
                        student?.let {
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
        if (student == null) {
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
                        listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                            MaterialTheme.colorScheme.background)
                    )
                )
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            // Profile Card
            item {
                StudentProfileCard(student = student, className = className)
            }

            // Overall Attendance Card
            item {
                OverallAttendanceCard(overallPct = overallPct)
            }

            // Monthly Attendance
            item {
                SectionHeader(title = "Monthly Attendance", icon = Icons.Default.CalendarMonth)
            }
            if (monthlyAttendance.isEmpty()) {
                item { EmptyDataHint("No attendance data available") }
            } else {
                items(monthlyAttendance.entries.toList()) { (month, pct) ->
                    MonthlyAttendanceRow(month = month, percentage = pct)
                }
            }

            // Recent 10 Days
            item {
                Spacer(Modifier.height(4.dp))
                SectionHeader(title = "Recent 10 Days (Day-wise)", icon = Icons.Default.DateRange)
            }
            if (recentDays.isEmpty()) {
                item { EmptyDataHint("No recent attendance records") }
            } else {
                items(recentDays) { (date, status) ->
                    DayAttendanceRow(date = date, status = status)
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun StudentProfileCard(student: User, className: String) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar / Photo
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (student.profileImageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = student.profileImageUrl,
                            contentDescription = "Profile photo",
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize()
                                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                student.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(student.name, style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.height(4.dp))
                    DetailChip(label = student.email, icon = Icons.Default.Email)
                    if (student.phone.isNotEmpty()) {
                        Spacer(Modifier.height(3.dp))
                        DetailChip(label = student.phone, icon = Icons.Default.Phone)
                    }
                    Spacer(Modifier.height(3.dp))
                    DetailChip(label = "Class: $className", icon = Icons.Default.Class)
                }
            }
            // Extra details
            if (student.address.isNotEmpty() || student.parentName.isNotEmpty() ||
                student.parentPhone.isNotEmpty() || student.documentUrl.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
                Spacer(Modifier.height(10.dp))
                if (student.address.isNotEmpty()) {
                    DetailChip(label = student.address, icon = Icons.Default.Home)
                    Spacer(Modifier.height(4.dp))
                }
                if (student.parentName.isNotEmpty()) {
                    DetailChip(label = "Parent: ${student.parentName}", icon = Icons.Default.FamilyRestroom)
                    Spacer(Modifier.height(4.dp))
                }
                if (student.parentPhone.isNotEmpty()) {
                    DetailChip(label = "Parent Phone: ${student.parentPhone}", icon = Icons.Default.Phone)
                    Spacer(Modifier.height(4.dp))
                }
                if (student.documentUrl.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(student.documentUrl))
                            context.startActivity(intent)
                        }
                    ) {
                        Icon(Icons.Default.Description, null, modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                        Spacer(Modifier.width(4.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Document uploaded ✓",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(10.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(12.dp),
            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f))
    }
}

@Composable
fun OverallAttendanceCard(overallPct: Float) {
    val color = when {
        overallPct >= 75f -> Color(0xFF2E7D32)
        overallPct >= 50f -> Color(0xFFF57F17)
        else -> Color(0xFFC62828)
    }
    Card(
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(72.dp),
                contentAlignment = Alignment.Center
            ) {
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
                Text("Overall Attendance", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
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
fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun MonthlyAttendanceRow(month: String, percentage: Float) {
    val color = when {
        percentage >= 75f -> Color(0xFF2E7D32)
        percentage >= 50f -> Color(0xFFF57F17)
        else -> Color(0xFFC62828)
    }
    // Parse "yyyy-MM" to readable month name
    val displayMonth = try {
        val ld = LocalDate.parse("$month-01")
        val monthName = ld.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        "$monthName ${ld.year}"
    } catch (_: Exception) { month }

    Card(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(displayMonth, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            // Progress bar
            LinearProgressIndicator(
                progress = { percentage / 100f },
                modifier = Modifier.width(80.dp).height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = color,
                trackColor = color.copy(alpha = 0.15f)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "${percentage.toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = color,
                modifier = Modifier.width(40.dp)
            )
        }
    }
}

@Composable
fun DayAttendanceRow(date: String, status: String) {
    val isPresent = status == "PRESENT"
    val color = if (isPresent) Color(0xFF2E7D32) else Color(0xFFC62828)
    val bgColor = if (isPresent) Color(0xFF2E7D32).copy(alpha = 0.08f) else Color(0xFFC62828).copy(alpha = 0.08f)

    // Parse date to readable
    val displayDate = try {
        val ld = LocalDate.parse(date)
        val dayName = ld.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        val fmt = DateTimeFormatter.ofPattern("dd MMM yyyy")
        "$dayName, ${ld.format(fmt)}"
    } catch (_: Exception) { date }

    Card(
        modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isPresent) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(displayDate, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface)
            Surface(shape = RoundedCornerShape(20.dp), color = color.copy(alpha = 0.15f)) {
                Text(
                    if (isPresent) "Present" else "Absent",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun EmptyDataHint(message: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
    }
}



