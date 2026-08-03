package com.fitlife.ai.ui.screens.blood

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitlife.ai.data.local.entity.BloodMarkerDto
import com.fitlife.ai.data.local.entity.BloodReportEntity
import com.fitlife.ai.ui.screens.camera.CameraCaptureDialog
import com.fitlife.ai.viewmodel.BloodViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BloodScreen(
    onBack: () -> Unit = {},
    viewModel: BloodViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showSourceDialog by remember { mutableStateOf(false) }
    var showCamera by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bitmap = decodeBitmap(context, uri)
            if (bitmap != null) {
                viewModel.extractFromPhoto(bitmap)
            } else {
                Toast.makeText(context, "Could not read the selected image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) showCamera = true }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Blood Work", style = MaterialTheme.typography.headlineMedium)
                Text("Analyze lab results with AI", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = { showSourceDialog = true }) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Text(" Add Report")
            }
        }
        Spacer(Modifier.height(16.dp))

        uiState.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
        }

        if (uiState.isExtracting) {
            CircularProgressIndicator(modifier = Modifier.fillMaxWidth().padding(16.dp))
            Text("Reading report with AI vision...", style = MaterialTheme.typography.bodyMedium)
            return
        }

        if (uiState.draftMarkers.isNotEmpty()) {
            MarkerEditor(
                markers = uiState.draftMarkers,
                rawText = uiState.draftRawText,
                onMarkersChange = viewModel::updateDraftMarkers,
                isSaving = uiState.isAnalyzing,
                onSave = { viewModel.saveAndAnalyze() },
                onCancel = viewModel::clearDraft
            )
            return
        }

        if (uiState.reports.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.HealthAndSafety, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("No blood work yet", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Upload a lab report photo and FitLife AI will extract your markers and analyze them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.reports) { report ->
                    ReportCard(
                        report = report,
                        isAnalyzing = uiState.isAnalyzing,
                        onAnalyze = { viewModel.analyzeReport(report.id) },
                        onDelete = { viewModel.deleteReport(report.id) }
                    )
                }
            }
        }
    }

    if (showSourceDialog) {
        AlertDialog(
            onDismissRequest = { showSourceDialog = false },
            title = { Text("Add Blood Report") },
            text = {
                Column {
                    Text("Choose how you'd like to add your lab report.")
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            showSourceDialog = false
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                                PackageManager.PERMISSION_GRANTED
                            ) showCamera = true
                            else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }) {
                            Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(18.dp))
                            Text(" Camera")
                        }
                        Button(onClick = {
                            showSourceDialog = false
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }) {
                            Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(18.dp))
                            Text(" Gallery")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showSourceDialog = false }) { Text("Cancel") } }
        )
    }

    if (showCamera) {
        CameraCaptureDialog(
            onDismiss = { showCamera = false },
            onImageCaptured = { bitmap ->
                viewModel.extractFromPhoto(bitmap)
                showCamera = false
            }
        )
    }
}

private fun decodeBitmap(context: android.content.Context, uri: Uri): Bitmap? {
    return context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream)
    }
}

@Composable
private fun MarkerEditor(
    markers: List<BloodMarkerDto>,
    rawText: String?,
    onMarkersChange: (List<BloodMarkerDto>) -> Unit,
    isSaving: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    var newName by remember { mutableStateOf("") }
    var newValue by remember { mutableStateOf("") }
    var newUnit by remember { mutableStateOf("") }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("Review extracted markers", style = MaterialTheme.typography.titleLarge)
            Text("Confirm or edit the values before analysis.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!rawText.isNullOrBlank()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Raw text", style = MaterialTheme.typography.titleSmall)
                        Text(rawText, style = MaterialTheme.typography.bodySmall, maxLines = 4)
                    }
                }
            }
        }
        items(markers) { marker ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(marker.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${marker.value?.toString() ?: "-"} ${marker.unit}" +
                                (if (marker.refLow != null && marker.refHigh != null) " (ref ${marker.refLow}-${marker.refHigh})" else ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = {
                        onMarkersChange(markers.filterNot { it.name == marker.name && it.value == marker.value })
                    }) {
                        Icon(Icons.Default.Delete, "Remove marker")
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Add marker", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Marker name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Row {
                        OutlinedTextField(value = newValue, onValueChange = { newValue = it }, label = { Text("Value") }, singleLine = true, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(value = newUnit, onValueChange = { newUnit = it }, label = { Text("Unit") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (newName.isNotBlank()) {
                                onMarkersChange(
                                    markers + BloodMarkerDto(
                                        name = newName.trim(),
                                        value = newValue.toDoubleOrNull(),
                                        unit = newUnit.trim()
                                    )
                                )
                                newName = ""
                                newValue = ""
                                newUnit = ""
                            }
                        },
                        enabled = newName.isNotBlank()
                    ) { Text("Add Marker") }
                }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(onClick = onSave, enabled = markers.isNotEmpty() && !isSaving, modifier = Modifier.weight(1f)) {
                    if (isSaving) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.height(18.dp))
                    else Text("Save & Analyze")
                }
            }
        }
    }
}

@Composable
private fun ReportCard(
    report: BloodReportEntity,
    isAnalyzing: Boolean,
    onAnalyze: () -> Unit,
    onDelete: () -> Unit
) {
    val markerCount = report.markersJson?.let { json ->
        try { org.json.JSONArray(json).length() } catch (e: Exception) { 0 }
    } ?: 0
    var expanded by remember { mutableStateOf(false) }
    val date = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(report.reportDate))

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(date, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "$markerCount markers${if (report.analysisText.isNullOrBlank()) "" else " · analyzed"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete") }
            }
            Spacer(Modifier.height(8.dp))
            if (report.analysisText.isNullOrBlank()) {
                Button(onClick = onAnalyze, enabled = !isAnalyzing, modifier = Modifier.fillMaxWidth()) {
                    if (isAnalyzing) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.height(18.dp))
                    else Text("Run AI Analysis")
                }
            } else {
                Button(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (expanded) "Hide Analysis" else "View Analysis")
                }
                if (expanded) {
                    Spacer(Modifier.height(8.dp))
                    Text(report.analysisText, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
