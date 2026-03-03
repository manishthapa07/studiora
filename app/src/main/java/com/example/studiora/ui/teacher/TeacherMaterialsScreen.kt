package com.example.studiora.ui.teacher

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Launch
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
import com.example.studiora.model.StudyMaterial
import com.example.studiora.ui.common.CenteredAddButton
import com.example.studiora.util.CloudinaryHelper
import com.example.studiora.viewmodel.AuthViewModel
import com.example.studiora.viewmodel.OperationState
import com.example.studiora.viewmodel.TeacherViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherMaterialsScreen(
    navController: NavController,
    courseId: String,
    courseName: String,
    authViewModel: AuthViewModel,
    teacherViewModel: TeacherViewModel
) {
    val currentUser by authViewModel.currentUserData.collectAsStateWithLifecycle()
    val materials by teacherViewModel.materials.collectAsStateWithLifecycle()
    val operationState by teacherViewModel.operationState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var materialToDelete by remember { mutableStateOf<StudyMaterial?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("ALL") }
    var isUploading by remember { mutableStateOf(false) }

    // Initialize Cloudinary
    LaunchedEffect(Unit) { CloudinaryHelper.initialize(context) }

    LaunchedEffect(courseId) { teacherViewModel.loadMaterialsByCourse(courseId) }

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

    val fileTypes = listOf("ALL", "PDF", "VIDEO", "IMAGE", "LINK", "OTHER")
    val filtered = materials.filter { m ->
        val typeMatch = selectedType == "ALL" || m.fileType == selectedType
        val queryMatch = searchQuery.isBlank() ||
            m.title.contains(searchQuery, ignoreCase = true) ||
            m.description.contains(searchQuery, ignoreCase = true)
        typeMatch && queryMatch
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Study Materials", fontWeight = FontWeight.Bold)
                        Text(
                            courseName,
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
        },
        floatingActionButton = {}
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
            if (isUploading || operationState is OperationState.Loading) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Uploading…", style = MaterialTheme.typography.bodyMedium)
                }
            } else if (materials.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(paddingValues),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Description, null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        "No materials uploaded yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Tap + to upload the first study material",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CenteredAddButton(
                        label = "Upload Material",
                        icon = Icons.Default.Upload,
                        onClick = { showAddDialog = true }
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(12.dp)) }

                    // Search bar
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search materials…") },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, null)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    // Type filter chips
                    item {
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(fileTypes) { type ->
                                FilterChip(
                                    selected = selectedType == type,
                                    onClick = { selectedType = type },
                                    label = { Text(type) },
                                    leadingIcon = if (selectedType == type) {
                                        { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                                        selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }

                    // Count row
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Description, null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (searchQuery.isBlank() && selectedType == "ALL")
                                    "${materials.size} Material(s)"
                                else
                                    "${filtered.size} of ${materials.size} shown",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (filtered.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No materials match the current filter",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(filtered) { material ->
                            MaterialCard(
                                material = material,
                                canDelete = true,
                                onOpen = {
                                    if (material.fileUrl.isNotEmpty()) {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(material.fileUrl))
                                        context.startActivity(intent)
                                    } else {
                                        Toast.makeText(context, "No file URL available", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onDelete = { materialToDelete = material }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        CenteredAddButton(
                            label = "Upload Material",
                            icon = Icons.Default.Upload,
                            onClick = { showAddDialog = true }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        TeacherAddMaterialDialog(
            onDismiss = { showAddDialog = false },
            onAddUrl = { title, description, fileUrl, fileType ->
                teacherViewModel.addStudyMaterial(
                    title, description, fileUrl, fileType, courseId,
                    currentUser?.uid ?: "", currentUser?.name ?: ""
                )
                showAddDialog = false
            },
            onUploadFile = { title, description, uri, fileType ->
                showAddDialog = false
                isUploading = true
                scope.launch {
                    val resourceType = when (fileType) {
                        "IMAGE" -> "image"
                        "VIDEO" -> "video"
                        else -> "auto"
                    }
                    CloudinaryHelper.uploadFile(context, uri, resourceType).fold(
                        onSuccess = { url ->
                            isUploading = false
                            teacherViewModel.addStudyMaterial(
                                title, description, url, fileType, courseId,
                                currentUser?.uid ?: "", currentUser?.name ?: ""
                            )
                        },
                        onFailure = { error ->
                            isUploading = false
                            Toast.makeText(context, "Upload failed: ${error.message}", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }
        )
    }

    materialToDelete?.let { material ->
        AlertDialog(
            onDismissRequest = { materialToDelete = null },
            icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Material", fontWeight = FontWeight.Bold) },
            text = { Text("Delete '${material.title}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        teacherViewModel.deleteMaterial(material.materialId, courseId)
                        materialToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { materialToDelete = null }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun MaterialCard(
    material: StudyMaterial,
    canDelete: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val typeColor = when (material.fileType) {
        "PDF" -> Color(0xFFC62828)
        "VIDEO" -> Color(0xFF1565C0)
        "IMAGE" -> Color(0xFF2E7D32)
        "LINK" -> Color(0xFF6A1B9A)
        else -> Color(0xFF00838F)
    }
    Card(
        modifier = Modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(80.dp)
                    .background(typeColor, RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(typeColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (material.fileType) {
                            "PDF" -> Icons.Default.PictureAsPdf
                            "VIDEO" -> Icons.Default.PlayCircle
                            "IMAGE" -> Icons.Default.Image
                            "LINK" -> Icons.Default.Link
                            else -> Icons.Default.Description
                        },
                        contentDescription = null, tint = typeColor, modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(material.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Surface(color = typeColor.copy(alpha = 0.1f), shape = RoundedCornerShape(20.dp)) {
                        Text(
                            material.fileType,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = typeColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (material.description.isNotEmpty()) {
                        Text(material.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                    }
                    Text("By: ${material.uploaderName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (material.fileUrl.isNotEmpty()) {
                        IconButton(onClick = onOpen) {
                            Icon(Icons.AutoMirrored.Filled.Launch, "Open", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (canDelete) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherAddMaterialDialog(
    onDismiss: () -> Unit,
    onAddUrl: (String, String, String, String) -> Unit,
    onUploadFile: (String, String, Uri, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var fileUrl by remember { mutableStateOf("") }
    var fileType by remember { mutableStateOf("PDF") }
    var uploadMode by remember { mutableStateOf(true) } // true = upload file, false = URL
    var expanded by remember { mutableStateOf(false) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    val fileTypes = listOf("PDF", "VIDEO", "IMAGE", "LINK", "OTHER")
    val primary = MaterialTheme.colorScheme.primary

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedUri = uri
        uri?.let {
            val type = context.contentResolver.getType(it)
            fileType = when {
                type?.startsWith("image/") == true -> "IMAGE"
                type?.startsWith("video/") == true -> "VIDEO"
                type == "application/pdf" -> "PDF"
                else -> "OTHER"
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier.fillMaxWidth(0.92f).shadow(16.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column {
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
                            Icon(Icons.Default.UploadFile, null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("Add Study Material", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Upload a file or enter a URL", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                    }
                }
                Column(
                    modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Mode toggle
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = uploadMode, onClick = { uploadMode = true },
                            label = { Text("Upload File") },
                            leadingIcon = { Icon(Icons.Default.Upload, null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = !uploadMode, onClick = { uploadMode = false },
                            label = { Text("Enter URL") },
                            leadingIcon = { Icon(Icons.Default.Link, null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = title, onValueChange = { title = it },
                        label = { Text("Title") }, leadingIcon = { Icon(Icons.Default.Title, null) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primary)
                    )
                    OutlinedTextField(
                        value = description, onValueChange = { description = it },
                        label = { Text("Description (optional)") },
                        modifier = Modifier.fillMaxWidth(), maxLines = 2,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primary)
                    )
                    if (uploadMode) {
                        Button(
                            onClick = { filePickerLauncher.launch("*/*") },
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AttachFile, null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (selectedUri != null) "File Selected ✓" else "Select File")
                        }
                        selectedUri?.let {
                            Text("File: ${it.lastPathSegment ?: "Unknown"}", style = MaterialTheme.typography.bodySmall, color = primary)
                        }
                    } else {
                        OutlinedTextField(
                            value = fileUrl, onValueChange = { fileUrl = it },
                            label = { Text("File URL / Link") }, leadingIcon = { Icon(Icons.Default.Link, null) },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primary)
                        )
                    }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(
                            value = fileType, onValueChange = {}, readOnly = true,
                            label = { Text("File Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primary)
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            fileTypes.forEach { type ->
                                DropdownMenuItem(text = { Text(type) }, onClick = { fileType = type; expanded = false })
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Cancel", fontWeight = FontWeight.SemiBold) }
                        Button(
                            onClick = {
                                when {
                                    title.isBlank() -> Toast.makeText(context, "Enter title", Toast.LENGTH_SHORT).show()
                                    uploadMode && selectedUri == null -> Toast.makeText(context, "Select a file", Toast.LENGTH_SHORT).show()
                                    !uploadMode && fileUrl.isBlank() -> Toast.makeText(context, "Enter URL", Toast.LENGTH_SHORT).show()
                                    uploadMode -> selectedUri?.let { onUploadFile(title.trim(), description.trim(), it, fileType) }
                                    else -> onAddUrl(title.trim(), description.trim(), fileUrl.trim(), fileType)
                                }
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primary)
                        ) { Text(if (uploadMode) "Upload" else "Add", fontWeight = FontWeight.Bold, color = Color.White) }
                    }
                }
            }
        }
    }
}

// Keep legacy AddMaterialDialog for backward compatibility (URL-only)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMaterialDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String) -> Unit
) {
    TeacherAddMaterialDialog(
        onDismiss = onDismiss,
        onAddUrl = onAdd,
        onUploadFile = { _, _, _, _ -> } // no-op: callers should switch to TeacherAddMaterialDialog
    )
}
