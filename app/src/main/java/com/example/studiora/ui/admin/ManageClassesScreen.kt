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
import com.example.studiora.model.Class
import com.example.studiora.ui.common.CenteredAddButton
import com.example.studiora.viewmodel.AdminViewModel
import com.example.studiora.viewmodel.OperationState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageClassesScreen(navController: NavController, adminViewModel: AdminViewModel) {
    val classes by adminViewModel.classes.collectAsStateWithLifecycle()
    val teachers by adminViewModel.teachers.collectAsStateWithLifecycle()
    val operationState by adminViewModel.operationState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var classToDelete by remember { mutableStateOf<Class?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { adminViewModel.loadClasses(); adminViewModel.loadTeachers() }

    LaunchedEffect(operationState) {
        when (val state = operationState) {
            is OperationState.Success -> { Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show(); adminViewModel.resetOperationState() }
            is OperationState.Error -> { Toast.makeText(context, state.message, Toast.LENGTH_LONG).show(); adminViewModel.resetOperationState() }
            else -> {}
        }
    }

    val filtered = if (searchQuery.isBlank()) classes
    else classes.filter { it.name.contains(searchQuery, ignoreCase = true) || it.teacherName.contains(searchQuery, ignoreCase = true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Classes", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.onPrimary, navigationIconContentColor = MaterialTheme.colorScheme.onPrimary)
            )
        },
        floatingActionButton = {}
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), MaterialTheme.colorScheme.background)))) {
            if (operationState is OperationState.Loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (classes.isEmpty()) {
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Class, null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("No classes yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(20.dp))
                    Box(modifier = Modifier.padding(horizontal = 32.dp)) {
                        CenteredAddButton(label = "Add Class", icon = Icons.Default.Add, onClick = { showAddDialog = true })
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(12.dp)) }

                    // Search bar
                    item {
                        OutlinedTextField(
                            value = searchQuery, onValueChange = { searchQuery = it },
                            placeholder = { Text("Search classes…") },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, null) } },
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                        )
                    }

                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Class, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (searchQuery.isBlank()) "${classes.size} Class(es)" else "${filtered.size} Result(s)",
                                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    if (filtered.isEmpty()) {
                        item { Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) { Text("No classes match \"$searchQuery\"", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                    } else {
                        itemsIndexed(filtered) { index, cls ->
                            ClassCard(
                                cls = cls, index = index,
                                onDelete = { classToDelete = cls },
                                onManageSchedule = { navController.navigate("manage_class_schedule/${cls.classId}") }
                            )
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        CenteredAddButton(
                            label = "Add New Class",
                            icon = Icons.Default.Add,
                            onClick = { showAddDialog = true }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddClassDialog(teachers = teachers, onDismiss = { showAddDialog = false }, onAdd = { name, teacherId, teacherName ->
            adminViewModel.createClass(name, teacherId, teacherName); showAddDialog = false
        })
    }

    classToDelete?.let { cls ->
        AlertDialog(
            onDismissRequest = { classToDelete = null },
            icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Class", fontWeight = FontWeight.Bold) },
            text = { Text("Delete '${cls.name}'? Students and courses are not deleted.") },
            confirmButton = { Button(onClick = { adminViewModel.deleteClass(cls.classId); classToDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { classToDelete = null }) { Text("Cancel") } },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun ClassCard(cls: Class, index: Int = 0, onDelete: () -> Unit, onManageSchedule: () -> Unit) {
    val accentColors = listOf(Color(0xFF1565C0), Color(0xFF2E7D32), Color(0xFF6A1B9A), Color(0xFF00838F), Color(0xFFE65100), Color(0xFFC62828))
    val accent = accentColors[index % accentColors.size]

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
                    Icon(Icons.Default.Class, null, tint = accent, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(cls.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(cls.teacherName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(20.dp)) {
                        Text(
                            "${cls.schedule.size} schedule slot(s)",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                IconButton(onClick = onManageSchedule) { Icon(Icons.Default.Schedule, "Schedule", tint = MaterialTheme.colorScheme.secondary) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddClassDialog(
    teachers: List<com.example.studiora.model.User>,
    onDismiss: () -> Unit,
    onAdd: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedTeacherId by remember { mutableStateOf("") }
    var selectedTeacherName by remember { mutableStateOf("Select Teacher") }
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val tertiary = MaterialTheme.colorScheme.tertiary

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
                        .background(Brush.horizontalGradient(listOf(tertiary, tertiary.copy(alpha = 0.75f))))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(52.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Class, null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("Create Class", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Set up a new class & assign a teacher", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                    }
                }
                // Fields
                Column(
                    modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        label = { Text("Class Name (e.g. CS-2A)") },
                        leadingIcon = { Icon(Icons.Default.Class, null) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = tertiary)
                    )
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(
                            value = selectedTeacherName, onValueChange = {}, readOnly = true,
                            label = { Text("Assign Teacher") }, leadingIcon = { Icon(Icons.Default.Person, null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = tertiary)
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            teachers.forEach { teacher ->
                                DropdownMenuItem(text = { Text("${teacher.name} (${teacher.subject})") }, onClick = { selectedTeacherId = teacher.uid; selectedTeacherName = teacher.name; expanded = false })
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
                                    name.isBlank() -> Toast.makeText(context, "Enter class name", Toast.LENGTH_SHORT).show()
                                    selectedTeacherId.isEmpty() -> Toast.makeText(context, "Select a teacher", Toast.LENGTH_SHORT).show()
                                    else -> onAdd(name.trim(), selectedTeacherId, selectedTeacherName)
                                }
                            },
                            modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = tertiary)
                        ) { Text("Create Class", fontWeight = FontWeight.Bold, color = Color.White) }
                    }
                }
            }
        }
    }
}
