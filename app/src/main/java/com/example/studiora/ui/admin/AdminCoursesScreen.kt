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
import com.example.studiora.repository.CourseRepository
import com.example.studiora.ui.common.CenteredAddButton
import com.example.studiora.viewmodel.AdminViewModel
import com.example.studiora.viewmodel.OperationState
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCoursesScreen(navController: NavController, adminViewModel: AdminViewModel) {
    val classes by adminViewModel.classes.collectAsStateWithLifecycle()
    val teachers by adminViewModel.teachers.collectAsStateWithLifecycle()
    val operationState by adminViewModel.operationState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var courses by remember { mutableStateOf<List<Course>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var courseToDelete by remember { mutableStateOf<Course?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val courseRepository = remember { CourseRepository() }
    val scope = rememberCoroutineScope()
    val organizationId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""

    LaunchedEffect(Unit) {
        adminViewModel.loadClasses(); adminViewModel.loadTeachers()
        courseRepository.getAllCourses(organizationId).onSuccess { courses = it }
    }

    LaunchedEffect(operationState) {
        when (val state = operationState) {
            is OperationState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                adminViewModel.resetOperationState()
                courseRepository.getAllCourses(organizationId).onSuccess { courses = it }
            }
            is OperationState.Error -> { Toast.makeText(context, state.message, Toast.LENGTH_LONG).show(); adminViewModel.resetOperationState() }
            else -> {}
        }
    }

    val filtered = if (searchQuery.isBlank()) courses
    else courses.filter { it.name.contains(searchQuery, ignoreCase = true) || it.teacherName.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Courses", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.onPrimary, navigationIconContentColor = MaterialTheme.colorScheme.onPrimary)
            )
        },
        floatingActionButton = {}
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), MaterialTheme.colorScheme.background)))) {
            if (courses.isEmpty()) {
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Book, null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("No courses yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(20.dp))
                    Box(modifier = Modifier.padding(horizontal = 32.dp)) {
                        CenteredAddButton(label = "Add Course", icon = Icons.Default.Add, onClick = { showAddDialog = true })
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                    item {
                        OutlinedTextField(
                            value = searchQuery, onValueChange = { searchQuery = it },
                            placeholder = { Text("Search courses…") },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, null) } },
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Book, null, modifier = Modifier.size(16.dp), tint = Color(0xFFE65100))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (searchQuery.isBlank()) "${courses.size} Course(s)" else "${filtered.size} Result(s)",
                                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    if (filtered.isEmpty()) {
                        item { Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) { Text("No courses match \"$searchQuery\"", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                    } else {
                        itemsIndexed(filtered) { index, course ->
                            CourseCard(
                                course = course, index = index,
                                className = classes.find { it.classId == course.classId }?.name ?: "—",
                                onDelete = { courseToDelete = course },
                                onManageMaterials = { navController.navigate("admin_course_materials/${course.courseId}/${course.name}") }
                            )
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        CenteredAddButton(label = "Add New Course", icon = Icons.Default.Add, onClick = { showAddDialog = true })
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddCourseDialog(classes = classes, teachers = teachers, onDismiss = { showAddDialog = false }, onAdd = { name, description, classId, teacherId, teacherName ->
            scope.launch {
                val courseId = FirebaseDatabase.getInstance().reference.child("courses").push().key ?: return@launch
                val course = Course(courseId, name, description, classId, teacherId, teacherName, organizationId)
                courseRepository.addCourse(course).onSuccess {
                    courseRepository.getAllCourses(organizationId).onSuccess { courses = it }
                    Toast.makeText(context, "Course added", Toast.LENGTH_SHORT).show()
                }.onFailure { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() }
            }
            showAddDialog = false
        })
    }

    courseToDelete?.let { course ->
        AlertDialog(
            onDismissRequest = { courseToDelete = null },
            icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Course", fontWeight = FontWeight.Bold) },
            text = { Text("Delete '${course.name}'? Materials will also be removed.") },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        courseRepository.deleteCourse(course.courseId).onSuccess {
                            courseRepository.getAllCourses(organizationId).onSuccess { courses = it }
                            Toast.makeText(context, "Course deleted", Toast.LENGTH_SHORT).show()
                        }
                    }
                    courseToDelete = null
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { courseToDelete = null }) { Text("Cancel") } },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun CourseCard(course: Course, index: Int = 0, className: String, onDelete: () -> Unit, onManageMaterials: () -> Unit = {}) {
    val accentColors = listOf(Color(0xFF1565C0), Color(0xFF2E7D32), Color(0xFF6A1B9A), Color(0xFF00838F), Color(0xFFE65100), Color(0xFFC62828))
    val accent = accentColors[index % accentColors.size]

    Card(
        modifier = Modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(5.dp).height(100.dp).background(accent, RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)))
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Book, null, tint = accent, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(course.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(20.dp)) {
                                Text(course.teacherName, modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(20.dp)) {
                                Text(className, modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        if (course.description.isNotEmpty()) Text(course.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onManageMaterials,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = accent),
                    border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Upload, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Manage Materials", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCourseDialog(
    classes: List<com.example.studiora.model.Class>,
    teachers: List<com.example.studiora.model.User>,
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedClassId by remember { mutableStateOf("") }
    var selectedClassName by remember { mutableStateOf("Select Class") }
    var selectedTeacherId by remember { mutableStateOf("") }
    var selectedTeacherName by remember { mutableStateOf("Select Teacher") }
    var classExpanded by remember { mutableStateOf(false) }
    var teacherExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val orange = Color(0xFFE65100)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier.fillMaxWidth(0.92f).shadow(16.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column {
                // Header
                Box(
                    modifier = Modifier.fillMaxWidth().background(
                        Brush.horizontalGradient(listOf(orange, orange.copy(alpha = 0.75f)))
                    ).padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(52.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Book, null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("Add Course", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Assign a course to a class & teacher", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                    }
                }
                // Fields
                Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Course Name") }, leadingIcon = { Icon(Icons.Default.Book, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = orange))
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description (optional)") }, modifier = Modifier.fillMaxWidth(), maxLines = 2, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = orange))
                    ExposedDropdownMenuBox(expanded = classExpanded, onExpandedChange = { classExpanded = it }) {
                        OutlinedTextField(value = selectedClassName, onValueChange = {}, readOnly = true, label = { Text("Select Class") }, leadingIcon = { Icon(Icons.Default.Class, null) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(classExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = orange))
                        ExposedDropdownMenu(expanded = classExpanded, onDismissRequest = { classExpanded = false }) {
                            classes.forEach { cls -> DropdownMenuItem(text = { Text(cls.name) }, onClick = { selectedClassId = cls.classId; selectedClassName = cls.name; classExpanded = false }) }
                        }
                    }
                    ExposedDropdownMenuBox(expanded = teacherExpanded, onExpandedChange = { teacherExpanded = it }) {
                        OutlinedTextField(value = selectedTeacherName, onValueChange = {}, readOnly = true, label = { Text("Assign Teacher") }, leadingIcon = { Icon(Icons.Default.Person, null) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(teacherExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = orange))
                        ExposedDropdownMenu(expanded = teacherExpanded, onDismissRequest = { teacherExpanded = false }) {
                            teachers.forEach { teacher -> DropdownMenuItem(text = { Text("${teacher.name} (${teacher.subject})") }, onClick = { selectedTeacherId = teacher.uid; selectedTeacherName = teacher.name; teacherExpanded = false }) }
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
                                    name.isBlank() -> Toast.makeText(context, "Enter course name", Toast.LENGTH_SHORT).show()
                                    selectedClassId.isEmpty() -> Toast.makeText(context, "Select a class", Toast.LENGTH_SHORT).show()
                                    selectedTeacherId.isEmpty() -> Toast.makeText(context, "Select a teacher", Toast.LENGTH_SHORT).show()
                                    else -> onAdd(name.trim(), description.trim(), selectedClassId, selectedTeacherId, selectedTeacherName)
                                }
                            },
                            modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = orange)
                        ) { Text("Add Course", fontWeight = FontWeight.Bold, color = Color.White) }
                    }
                }
            }
        }
    }
}

