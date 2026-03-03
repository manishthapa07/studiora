package com.example.studiora.ui.student

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.studiora.model.StudyMaterial
import com.example.studiora.viewmodel.StudentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentMaterialsScreen(
    navController: NavController,
    courseId: String,
    courseName: String,
    studentViewModel: StudentViewModel
) {
    val materials by studentViewModel.materials.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(courseId) { studentViewModel.loadMaterialsByCourse(courseId) }

    // Group materials by type
    val pdfs = materials.filter { it.fileType.equals("PDF", ignoreCase = true) }
    val images = materials.filter { it.fileType.equals("IMAGE", ignoreCase = true) || it.fileType.equals("PHOTO", ignoreCase = true) }
    val videos = materials.filter { it.fileType.equals("VIDEO", ignoreCase = true) }
    val links = materials.filter { it.fileType.equals("LINK", ignoreCase = true) || it.fileType.equals("URL", ignoreCase = true) }

    fun openUrl(url: String) {
        if (url.isEmpty()) { Toast.makeText(context, "No file URL available", Toast.LENGTH_SHORT).show(); return }
        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        catch (e: Exception) { Toast.makeText(context, "Cannot open this file", Toast.LENGTH_SHORT).show() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Study Materials", fontWeight = FontWeight.Bold)
                        Text(courseName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
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
        Box(
            modifier = Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), MaterialTheme.colorScheme.background)))
        ) {
            if (materials.isEmpty()) {
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("No materials yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Your teacher hasn't uploaded anything for this course", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(12.dp)) }

                    // Summary chips row
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (pdfs.isNotEmpty()) MaterialTypeBadge("📄 ${pdfs.size} PDF", Color(0xFFB71C1C))
                            if (images.isNotEmpty()) MaterialTypeBadge("🖼️ ${images.size} Image", Color(0xFF1565C0))
                            if (videos.isNotEmpty()) MaterialTypeBadge("▶️ ${videos.size} Video", Color(0xFF2E7D32))
                            if (links.isNotEmpty()) MaterialTypeBadge("🔗 ${links.size} Link", Color(0xFF6A1B9A))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${materials.size} material(s) available", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // All materials in one list with type-aware cards
                    itemsIndexed(materials) { _, material ->
                        StudentMaterialCard(material = material, onOpen = { openUrl(material.fileUrl) })
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun MaterialTypeBadge(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(20.dp)) {
        Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun StudentMaterialCard(material: StudyMaterial, onOpen: () -> Unit) {
    val (icon, accent) = when (material.fileType.uppercase()) {
        "PDF" -> Pair<ImageVector, Color>(Icons.Default.PictureAsPdf, Color(0xFFB71C1C))
        "IMAGE", "PHOTO" -> Pair<ImageVector, Color>(Icons.Default.Image, Color(0xFF1565C0))
        "VIDEO" -> Pair<ImageVector, Color>(Icons.Default.PlayCircle, Color(0xFF2E7D32))
        "LINK", "URL" -> Pair<ImageVector, Color>(Icons.Default.Link, Color(0xFF6A1B9A))
        else -> Pair<ImageVector, Color>(Icons.AutoMirrored.Filled.InsertDriveFile, Color(0xFF00838F))
    }
    val typeLabel = when (material.fileType.uppercase()) {
        "PDF" -> "PDF Document"
        "IMAGE", "PHOTO" -> "Image"
        "VIDEO" -> "Video"
        "LINK", "URL" -> "Link"
        else -> "File"
    }

    Card(
        modifier = Modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.width(5.dp).height(80.dp)
                    .background(accent, RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = accent, modifier = Modifier.size(26.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(material.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    if (material.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(material.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(color = accent.copy(alpha = 0.12f), shape = RoundedCornerShape(20.dp)) {
                            Text(typeLabel, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, color = accent)
                        }
                        Text("by ${material.uploaderName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = onOpen,
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = accent.copy(alpha = 0.12f), contentColor = accent),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Open", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
