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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.studiora.model.User
import com.example.studiora.ui.common.CenteredAddButton
import com.example.studiora.viewmodel.AdminViewModel
import com.example.studiora.viewmodel.OperationState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageStudentsScreen(navController: NavController, adminViewModel: AdminViewModel) {
    val students by adminViewModel.students.collectAsStateWithLifecycle()
    val classes by adminViewModel.classes.collectAsStateWithLifecycle()
    val operationState by adminViewModel.operationState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var studentToDelete by remember { mutableStateOf<User?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedClassFilter by remember { mutableStateOf("ALL") }

    LaunchedEffect(Unit) {
        adminViewModel.loadStudents()
        adminViewModel.loadClasses()
    }

    LaunchedEffect(operationState) {
        when (val state = operationState) {
            is OperationState.Success -> { Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show(); adminViewModel.resetOperationState() }
            is OperationState.Error -> { Toast.makeText(context, state.message, Toast.LENGTH_LONG).show(); adminViewModel.resetOperationState() }
            else -> {}
        }
    }

    val filtered = students.filter { s ->
        val classMatch = selectedClassFilter == "ALL" || s.classId == selectedClassFilter
        val queryMatch = searchQuery.isBlank() || s.name.contains(searchQuery, ignoreCase = true) || s.email.contains(searchQuery, ignoreCase = true)
        classMatch && queryMatch
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Students", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.onPrimary, navigationIconContentColor = MaterialTheme.colorScheme.onPrimary)
            )
        },
        floatingActionButton = {}
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), MaterialTheme.colorScheme.background)))) {
            if (operationState is OperationState.Loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (students.isEmpty()) {
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("No students found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(20.dp))
                    Box(modifier = Modifier.padding(horizontal = 32.dp)) {
                        CenteredAddButton(label = "Add Student", icon = Icons.Default.PersonAdd, onClick = { showAddDialog = true })
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(12.dp)) }

                    // Summary card
                    item {
                        Card(modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(16.dp)), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                AdminSummaryChip("Total", "${students.size}", MaterialTheme.colorScheme.secondary)
                                VerticalDivider(modifier = Modifier.height(40.dp))
                                AdminSummaryChip("Classes", "${classes.size}", MaterialTheme.colorScheme.tertiary)
                                VerticalDivider(modifier = Modifier.height(40.dp))
                                val unassigned = students.count { it.classId.isEmpty() }
                                AdminSummaryChip("Unassigned", "$unassigned", if (unassigned > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Search bar
                    item {
                        OutlinedTextField(
                            value = searchQuery, onValueChange = { searchQuery = it },
                            placeholder = { Text("Search students…") },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, null) } },
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                        )
                    }

                    // Class filter chips
                    item {
                        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(selected = selectedClassFilter == "ALL", onClick = { selectedClassFilter = "ALL" }, label = { Text("All Classes") },
                                    leadingIcon = if (selectedClassFilter == "ALL") { { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) } } else null,
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer, selectedLabelColor = MaterialTheme.colorScheme.secondary))
                            }
                            items(classes.size) { i ->
                                val cls = classes[i]
                                FilterChip(selected = selectedClassFilter == cls.classId, onClick = { selectedClassFilter = cls.classId }, label = { Text(cls.name) },
                                    leadingIcon = if (selectedClassFilter == cls.classId) { { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) } } else null,
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer, selectedLabelColor = MaterialTheme.colorScheme.secondary))
                            }
                        }
                    }

                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (searchQuery.isBlank() && selectedClassFilter == "ALL") "${students.size} Student(s)" else "${filtered.size} Result(s)",
                                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    if (filtered.isEmpty()) {
                        item { Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) { Text("No students match the filter", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                    } else {
                        itemsIndexed(filtered) { index, student ->
                            StudentListCard(
                                user = student, index = index,
                                className = classes.find { it.classId == student.classId }?.name ?: "Unassigned",
                                onDelete = { studentToDelete = student },
                                onClick = { navController.navigate("admin_student_detail/${student.uid}") }
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                    item {
                        CenteredAddButton(
                            label = "Add New Student",
                            icon = Icons.Default.PersonAdd,
                            onClick = { showAddDialog = true }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddStudentDialog(classes = classes, onDismiss = { showAddDialog = false }, onAdd = { name, email, password, phone, classId ->
            adminViewModel.addStudent(name, email, password, phone, classId); showAddDialog = false
        })
    }

    studentToDelete?.let { student ->
        AlertDialog(
            onDismissRequest = { studentToDelete = null },
            icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Remove Student", fontWeight = FontWeight.Bold) },
            text = { Text("Remove ${student.name} from the system?") },
            confirmButton = { Button(onClick = { adminViewModel.deleteStudent(student.uid); studentToDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { studentToDelete = null }) { Text("Cancel") } },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun StudentListCard(user: User, index: Int = 0, className: String, onDelete: () -> Unit, onClick: () -> Unit = {}) {
    val accentColors = listOf(Color(0xFF1565C0), Color(0xFF2E7D32), Color(0xFF6A1B9A), Color(0xFF00838F), Color(0xFFE65100), Color(0xFFC62828))
    val accent = accentColors[index % accentColors.size]

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(5.dp).height(80.dp).background(accent, RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)))
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(46.dp).clip(CircleShape).background(accent.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    if (user.profileImageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = user.profileImageUrl,
                            contentDescription = "Profile",
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        Text(user.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?", fontWeight = FontWeight.Bold, color = accent, style = MaterialTheme.typography.titleMedium)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(user.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    Text(user.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(20.dp)) {
                        Row(modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Class, null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(className, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (user.phone.isNotEmpty()) Text("📞 ${user.phone}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStudentDialog(
    classes: List<com.example.studiora.model.Class>,
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var phone by remember { mutableStateOf("") }
    var selectedClassId by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var selectedClassName by remember { mutableStateOf("Select Class") }
    var submitAttempted by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val primary = MaterialTheme.colorScheme.primary

    val hasMinLength  = password.length >= 8
    val hasUppercase  = password.any { it.isUpperCase() }
    val hasDigit      = password.any { it.isDigit() }
    val passwordValid = hasMinLength && hasUppercase && hasDigit
    val showRules     = password.isNotEmpty() || submitAttempted

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
                        .background(Brush.horizontalGradient(listOf(primary, primary.copy(alpha = 0.75f))))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(52.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PersonAdd, null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("Add Student", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Fill in the student's details below", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                    }
                }
                // Fields
                Column(
                    modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, leadingIcon = { Icon(Icons.Default.Person, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primary))
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, leadingIcon = { Icon(Icons.Default.Email, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primary))

                    // Password with inline rules
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password (min 8 chars)") },
                            leadingIcon = { Icon(Icons.Default.Lock, null) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        null, tint = Color(0xFF7A8694)
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            isError = submitAttempted && !passwordValid,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primary, errorBorderColor = MaterialTheme.colorScheme.error)
                        )
                        if (showRules) {
                            PasswordRuleItem("At least 8 characters", hasMinLength)
                            PasswordRuleItem("At least 1 uppercase letter (A–Z)", hasUppercase)
                            PasswordRuleItem("At least 1 number (0–9)", hasDigit)
                        }
                    }

                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone (optional)") }, leadingIcon = { Icon(Icons.Default.Phone, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primary))
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(
                            value = selectedClassName, onValueChange = {}, readOnly = true,
                            label = { Text("Select Class") }, leadingIcon = { Icon(Icons.Default.Class, null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primary)
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            classes.forEach { cls ->
                                DropdownMenuItem(text = { Text("${cls.name} (${cls.teacherName})") }, onClick = { selectedClassId = cls.classId; selectedClassName = cls.name; expanded = false })
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
                                submitAttempted = true
                                when {
                                    name.isBlank()           -> Toast.makeText(context, "Enter name", Toast.LENGTH_SHORT).show()
                                    email.isBlank()          -> Toast.makeText(context, "Enter email", Toast.LENGTH_SHORT).show()
                                    !passwordValid           -> { /* rules shown inline */ }
                                    selectedClassId.isEmpty() -> Toast.makeText(context, "Select a class", Toast.LENGTH_SHORT).show()
                                    else -> onAdd(name.trim(), email.trim(), password, phone.trim(), selectedClassId)
                                }
                            },
                            modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primary)
                        ) { Text("Add Student", fontWeight = FontWeight.Bold, color = Color.White) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PasswordRuleItem(text: String, passed: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp)
    ) {
        Icon(
            imageVector = if (passed) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (passed) Color(0xFF2E7D32) else Color(0xFFB00020),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            color = if (passed) Color(0xFF2E7D32) else Color(0xFFB00020)
        )
    }
}

