package com.example.studiora.ui.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.studiora.model.Course
import com.example.studiora.model.ScheduleItem
import com.example.studiora.ui.common.CenteredAddButton
import com.example.studiora.viewmodel.AdminViewModel
import com.example.studiora.viewmodel.OperationState

private val DAYS = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageClassScheduleScreen(
    classId: String,
    navController: NavController,
    adminViewModel: AdminViewModel
) {
    val selectedClass by adminViewModel.selectedClass.collectAsStateWithLifecycle()
    val courses by adminViewModel.courses.collectAsStateWithLifecycle()
    val operationState by adminViewModel.operationState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var schedule by remember { mutableStateOf(listOf<ScheduleItem>()) }
    var showDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ScheduleItem?>(null) }

    LaunchedEffect(classId) {
        adminViewModel.loadClassDetails(classId)
        adminViewModel.loadCourses()
    }

    LaunchedEffect(selectedClass) {
        selectedClass?.let { schedule = it.schedule }
    }

    LaunchedEffect(operationState) {
        when (val state = operationState) {
            is OperationState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                adminViewModel.resetOperationState()
                navController.popBackStack()
            }
            is OperationState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
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
                        Text("Manage Schedule", fontWeight = FontWeight.Bold)
                        selectedClass?.let {
                            Text(
                                it.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = { adminViewModel.updateClassSchedule(classId, schedule) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save", fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {}
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), MaterialTheme.colorScheme.background)))
        ) {
            val classCourses = courses.filter { it.classId == classId }

            if (schedule.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("No schedule slots yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(20.dp))
                    Box(modifier = Modifier.padding(horizontal = 32.dp)) {
                        CenteredAddButton(label = "Add Schedule Slot", icon = Icons.Default.Add, onClick = { editingItem = null; showDialog = true })
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${schedule.size} Slot(s)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    itemsIndexed(schedule) { index, item ->
                        val course = classCourses.find { it.courseId == item.courseId }
                        ScheduleItemCard(
                            item = item,
                            courseName = course?.name ?: "Unknown",
                            index = index,
                            onEdit = { editingItem = item; showDialog = true },
                            onDelete = { schedule = schedule - item }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        CenteredAddButton(
                            label = "Add Schedule Slot",
                            icon = Icons.Default.Add,
                            onClick = { editingItem = null; showDialog = true }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    if (showDialog) {
        val classCourses = courses.filter { it.classId == classId }
        ScheduleItemDialog(
            item = editingItem,
            courses = classCourses,
            onDismiss = { showDialog = false },
            onConfirm = { newItem ->
                schedule = if (editingItem == null) schedule + newItem
                else schedule.map { if (it == editingItem) newItem else it }
                showDialog = false
            }
        )
    }
}

@Composable
fun ScheduleItemCard(item: ScheduleItem, courseName: String, index: Int = 0, onEdit: () -> Unit, onDelete: () -> Unit) {
    val dayColors = mapOf(
        "Monday" to Color(0xFF1565C0), "Tuesday" to Color(0xFF2E7D32),
        "Wednesday" to Color(0xFF6A1B9A), "Thursday" to Color(0xFF00838F),
        "Friday" to Color(0xFFE65100), "Saturday" to Color(0xFFC62828), "Sunday" to Color(0xFF4A148C)
    )
    val accentColors = listOf(Color(0xFF1565C0), Color(0xFF2E7D32), Color(0xFF6A1B9A), Color(0xFF00838F), Color(0xFFE65100), Color(0xFFC62828))
    val accent = dayColors[item.day] ?: accentColors[index % accentColors.size]

    Card(
        modifier = Modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(5.dp).height(80.dp).background(accent, RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)))
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(item.day.take(3).uppercase(), fontWeight = FontWeight.Bold, color = accent, style = MaterialTheme.typography.labelMedium)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(courseName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(11.dp), tint = accent)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(item.day, style = MaterialTheme.typography.bodySmall, color = accent, fontWeight = FontWeight.SemiBold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(11.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("${item.startTime} – ${item.endTime}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleItemDialog(
    item: ScheduleItem?,
    courses: List<Course>,
    onDismiss: () -> Unit,
    onConfirm: (ScheduleItem) -> Unit
) {
    var dayExpanded by remember { mutableStateOf(false) }
    var courseExpanded by remember { mutableStateOf(false) }
    var day by remember { mutableStateOf(item?.day ?: "") }
    var startTime by remember { mutableStateOf(item?.startTime ?: "") }
    var endTime by remember { mutableStateOf(item?.endTime ?: "") }
    val selectedCourse = courses.find { it.courseId == item?.courseId }
    var selectedCourseId by remember { mutableStateOf(item?.courseId ?: "") }
    var selectedCourseName by remember { mutableStateOf(selectedCourse?.name ?: "Select Course") }
    val context = LocalContext.current
    val secondary = MaterialTheme.colorScheme.secondary
    val isEdit = item != null

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier.fillMaxWidth(0.92f).shadow(16.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(secondary, secondary.copy(alpha = 0.75f))))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(52.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Schedule, null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            if (isEdit) "Edit Schedule Slot" else "Add Schedule Slot",
                            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White
                        )
                        Text(
                            if (isEdit) "Update the schedule details" else "Set day, time & course",
                            style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
                // Fields
                Column(
                    modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExposedDropdownMenuBox(expanded = dayExpanded, onExpandedChange = { dayExpanded = it }) {
                        OutlinedTextField(
                            value = day.ifEmpty { "Select Day" }, onValueChange = {}, readOnly = true,
                            label = { Text("Day") }, leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(dayExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = secondary)
                        )
                        ExposedDropdownMenu(expanded = dayExpanded, onDismissRequest = { dayExpanded = false }) {
                            DAYS.forEach { d ->
                                DropdownMenuItem(text = { Text(d) }, onClick = { day = d; dayExpanded = false })
                            }
                        }
                    }
                    OutlinedTextField(
                        value = startTime, onValueChange = { startTime = it },
                        label = { Text("Start Time (e.g. 09:00 AM)") },
                        leadingIcon = { Icon(Icons.Default.AccessTime, null) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = secondary)
                    )
                    OutlinedTextField(
                        value = endTime, onValueChange = { endTime = it },
                        label = { Text("End Time (e.g. 10:00 AM)") },
                        leadingIcon = { Icon(Icons.Default.AccessTime, null) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = secondary)
                    )
                    ExposedDropdownMenuBox(expanded = courseExpanded, onExpandedChange = { courseExpanded = it }) {
                        OutlinedTextField(
                            value = selectedCourseName, onValueChange = {}, readOnly = true,
                            label = { Text("Course") }, leadingIcon = { Icon(Icons.Default.Book, null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(courseExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = secondary)
                        )
                        ExposedDropdownMenu(expanded = courseExpanded, onDismissRequest = { courseExpanded = false }) {
                            courses.forEach { course ->
                                DropdownMenuItem(text = { Text(course.name) }, onClick = { selectedCourseId = course.courseId; selectedCourseName = course.name; courseExpanded = false })
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(12.dp)) {
                            Text("Cancel", fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = {
                                when {
                                    day.isBlank() -> Toast.makeText(context, "Select a day", Toast.LENGTH_SHORT).show()
                                    startTime.isBlank() -> Toast.makeText(context, "Enter start time", Toast.LENGTH_SHORT).show()
                                    endTime.isBlank() -> Toast.makeText(context, "Enter end time", Toast.LENGTH_SHORT).show()
                                    selectedCourseId.isBlank() -> Toast.makeText(context, "Select a course", Toast.LENGTH_SHORT).show()
                                    else -> onConfirm(ScheduleItem(day, startTime, endTime, selectedCourseId))
                                }
                            },
                            modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = secondary)
                        ) { Text(if (isEdit) "Save Changes" else "Add Slot", fontWeight = FontWeight.Bold, color = Color.White) }
                    }
                }
            }
        }
    }
}
