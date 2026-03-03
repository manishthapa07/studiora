package com.example.studiora.ui.student

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.studiora.viewmodel.AuthViewModel
import com.example.studiora.viewmodel.StudentViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentWeeklyRoutineScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    studentViewModel: StudentViewModel
) {
    val currentUser by authViewModel.currentUserData.collectAsStateWithLifecycle()
    val classSchedule by studentViewModel.classSchedule.collectAsStateWithLifecycle()
    val courses by studentViewModel.courses.collectAsStateWithLifecycle()

    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            if (user.classId.isNotEmpty()) {
                studentViewModel.loadClassSchedule(user.classId)
                studentViewModel.loadCoursesByClass(user.classId)
            }
        }
    }

    val orderedDays = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    val today = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())

    // Track which days are expanded; today is expanded by default
    val expandedDays = remember(today) { mutableStateMapOf<String, Boolean>().apply {
        orderedDays.forEach { put(it, it.equals(today, ignoreCase = true)) }
    }}

    val dayColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        Color(0xFF7B1FA2),
        Color(0xFF00838F),
        Color(0xFFE65100),
        Color(0xFF2E7D32)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weekly Routine", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
        if (classSchedule == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null,
                        modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No routine available", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Your class schedule hasn't been set up yet.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(orderedDays) { day ->
                    val daySchedule = classSchedule!!.schedule
                        .filter { it.day.equals(day, ignoreCase = true) }
                        .sortedBy { it.startTime }

                    val colorIndex = orderedDays.indexOf(day) % dayColors.size
                    val accentColor = dayColors[colorIndex]
                    val isToday = day.equals(today, ignoreCase = true)
                    val isExpanded = expandedDays[day] == true

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(if (isToday) 5.dp else 2.dp, RoundedCornerShape(16.dp))
                            .animateContentSize(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isToday)
                                accentColor.copy(alpha = 0.07f)
                            else
                                MaterialTheme.colorScheme.surface
                        )
                    ) {
                        // ── Clickable header row ──────────────────────────
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedDays[day] = !isExpanded }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(accentColor)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = day,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                                if (isToday) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = accentColor.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Text(
                                            "Today",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = accentColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${daySchedule.size} class${if (daySchedule.size != 1) "es" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // ── Expandable content ────────────────────────────
                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp)) {
                                HorizontalDivider(color = accentColor.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(10.dp))

                                if (daySchedule.isEmpty()) {
                                    Text(
                                        "No classes scheduled",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        daySchedule.forEachIndexed { index, scheduleItem ->
                                            val course = courses.find { it.courseId == scheduleItem.courseId }
                                            val itemColors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.secondary,
                                                MaterialTheme.colorScheme.tertiary,
                                                Color(0xFF7B1FA2),
                                                Color(0xFF00838F)
                                            )
                                            val itemAccent = itemColors[index % itemColors.size]

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(itemAccent.copy(alpha = 0.06f))
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(4.dp)
                                                        .height(40.dp)
                                                        .clip(RoundedCornerShape(2.dp))
                                                        .background(itemAccent)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = course?.name ?: scheduleItem.courseId,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    if (course?.teacherName?.isNotBlank() == true) {
                                                        Text(
                                                            text = "👤 ${course.teacherName}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                                Surface(
                                                    color = itemAccent.copy(alpha = 0.12f),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Default.AccessTime, contentDescription = null,
                                                            modifier = Modifier.size(12.dp), tint = itemAccent)
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text(
                                                            text = "${scheduleItem.startTime} - ${scheduleItem.endTime}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = itemAccent,
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
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}
