package com.fitlife.ai.ui.screens.camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.fitlife.ai.data.local.entity.CalorieEntryEntity
import com.fitlife.ai.viewmodel.CalorieTrackerUiState
import com.fitlife.ai.viewmodel.CalorieTrackerViewModel
import java.nio.ByteBuffer

@Composable
fun CalorieTrackerScreen(
    viewModel: CalorieTrackerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showCamera by remember { mutableStateOf(false) }
    var cameraMode by remember { mutableStateOf("food") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showTargetsDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<CalorieEntryEntity?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) showCamera = true }

    val hasCameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED

    LaunchedEffect(uiState.scannedFood) {
        if (uiState.scannedFood != null) showAddDialog = true
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Calorie Tracker", style = MaterialTheme.typography.headlineMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        cameraMode = "food"
                        if (hasCameraPermission) showCamera = true
                        else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }) {
                        Icon(Icons.Default.CameraAlt, "Scan food", modifier = Modifier.size(20.dp))
                        Text(" Scan Food")
                    }
                    OutlinedButton(onClick = {
                        cameraMode = "barcode"
                        if (hasCameraPermission) showCamera = true
                        else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }) {
                        Text("Barcode")
                    }
                }
            }
        }

        if (uiState.isLoading) {
            item { CircularProgressIndicator(modifier = Modifier.fillMaxWidth().padding(16.dp)) }
        }

        uiState.error?.let {
            item {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        uiState.recognizedText?.let {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Recognized:", style = MaterialTheme.typography.titleSmall)
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        item {
            MacroDashboardCard(uiState = uiState, onEditTargets = { showTargetsDialog = true })
        }

        item {
            Button(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Add Entry Manually")
            }
        }

        if (uiState.entries.isEmpty()) {
            item { Text("No entries yet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }

        items(uiState.entries) { entry ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(entry.foodName, style = MaterialTheme.typography.titleSmall)
                        Row {
                            Text("${entry.calories} cal", style = MaterialTheme.typography.bodyMedium)
                            entry.mealType?.let { Text(" · $it", style = MaterialTheme.typography.bodyMedium) }
                        }
                    }
                    IconButton(onClick = { editing = entry; showAddDialog = true }) {
                        Icon(Icons.Default.Edit, "Edit")
                    }
                    IconButton(onClick = { viewModel.deleteEntry(entry.id) }) {
                        Icon(Icons.Default.Delete, "Delete")
                    }
                }
            }
        }
    }

    if (showCamera) {
        CameraCaptureDialog(
            title = if (cameraMode == "barcode") "Scan Barcode" else "Scan Food Label",
            onDismiss = { showCamera = false },
            onImageCaptured = { bitmap ->
                if (cameraMode == "barcode") viewModel.scanBarcode(bitmap)
                else viewModel.analyzeFoodImage(bitmap)
                showCamera = false
            }
        )
    }

    if (showTargetsDialog) {
        MacroTargetsDialog(
            initialCalories = uiState.calorieTarget?.toString() ?: "",
            initialProtein = uiState.proteinTargetG?.toString() ?: "",
            initialCarbs = uiState.carbsTargetG?.toString() ?: "",
            initialFat = uiState.fatTargetG?.toString() ?: "",
            onDismiss = { showTargetsDialog = false },
            onSave = { calories, protein, carbs, fat ->
                viewModel.saveMacroTargets(
                    calories.toIntOrNull(),
                    protein.toIntOrNull(),
                    carbs.toIntOrNull(),
                    fat.toIntOrNull()
                )
                showTargetsDialog = false
            }
        )
    }

    if (showAddDialog) {
        val entry = editing
        AddCalorieDialog(
            editing = entry != null,
            initialFoodName = entry?.foodName ?: uiState.scannedFood,
            initialCalories = entry?.calories?.toString() ?: uiState.scannedCalories?.toString(),
            initialMealType = entry?.mealType,
            initialProtein = entry?.proteinG?.toString() ?: uiState.scannedProtein?.toString(),
            initialCarbs = entry?.carbsG?.toString() ?: uiState.scannedCarbs?.toString(),
            initialFat = entry?.fatG?.toString() ?: uiState.scannedFat?.toString(),
            searchResults = uiState.searchResults,
            onSearchChange = { viewModel.searchFoods(it) },
            onDismiss = { showAddDialog = false; editing = null; viewModel.clearFoodSearch() },
            onAdd = { name, calories, mealType, protein, carbs, fat ->
                if (entry != null) {
                    viewModel.updateEntry(entry.id, name, calories, mealType)
                } else {
                    viewModel.addEntry(name, calories, mealType, protein, carbs, fat)
                }
                viewModel.clearScan()
                showAddDialog = false
                editing = null
            }
        )
    }
}

@Composable
fun CameraCaptureDialog(
    title: String = "Scan Food Label",
    onDismiss: () -> Unit,
    onImageCaptured: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember { LifecycleCameraController(context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also {
                        it.controller = controller
                        controller.bindToLifecycle(lifecycleOwner)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(300.dp)
            )
        },
        confirmButton = {
            TextButton(onClick = {
                controller.takePicture(
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            val bitmap = imageProxyToBitmap(image)
                            image.close()
                            onImageCaptured(bitmap)
                        }
                        override fun onError(exception: ImageCaptureException) {
                            exception.printStackTrace()
                        }
                    }
                )
            }) { Text("Capture") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    val buffer: ByteBuffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

@Composable
fun AddCalorieDialog(
    editing: Boolean = false,
    initialFoodName: String? = null,
    initialCalories: String? = null,
    initialMealType: String? = null,
    initialProtein: String? = null,
    initialCarbs: String? = null,
    initialFat: String? = null,
    searchResults: List<com.fitlife.ai.data.local.entity.FoodItemEntity> = emptyList(),
    onSearchChange: (String) -> Unit = {},
    onDismiss: () -> Unit,
    onAdd: (String, Int, String?, Double?, Double?, Double?) -> Unit
) {
    var foodName by remember { mutableStateOf(initialFoodName ?: "") }
    var calories by remember { mutableStateOf(initialCalories ?: "") }
    var mealType by remember { mutableStateOf(initialMealType ?: "") }
    var protein by remember { mutableStateOf(initialProtein ?: "") }
    var carbs by remember { mutableStateOf(initialCarbs ?: "") }
    var fat by remember { mutableStateOf(initialFat ?: "") }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(initialFoodName, initialCalories, initialMealType, initialProtein, initialCarbs, initialFat) {
        foodName = initialFoodName ?: ""
        calories = initialCalories ?: ""
        mealType = initialMealType ?: ""
        protein = initialProtein ?: ""
        carbs = initialCarbs ?: ""
        fat = initialFat ?: ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when {
                    editing -> "Edit Calorie Entry"
                    initialFoodName != null -> "Scan Result"
                    else -> "Add Calorie Entry"
                }
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        onSearchChange(it)
                    },
                    label = { Text("Search food database") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (searchResults.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.height(160.dp)) {
                        items(searchResults) { food ->
                            Card(
                                onClick = {
                                    foodName = food.name
                                    calories = food.calories.toString()
                                    protein = food.proteinG.toString()
                                    carbs = food.carbsG.toString()
                                    fat = food.fatG.toString()
                                    mealType = if (mealType.isBlank()) "snack" else mealType
                                    searchQuery = ""
                                    onSearchChange("")
                                },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Column(Modifier.padding(8.dp)) {
                                    Text(food.name, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "${food.calories} cal · P ${food.proteinG}g · C ${food.carbsG}g · F ${food.fatG}g · ${food.servingSize}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedTextField(value = foodName, onValueChange = { foodName = it }, label = { Text("Food Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = calories, onValueChange = { calories = it }, label = { Text("Calories") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = mealType, onValueChange = { mealType = it }, label = { Text("Meal Type") }, placeholder = { Text("breakfast/lunch/dinner/snack") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = protein, onValueChange = { protein = it }, label = { Text("Protein (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = carbs, onValueChange = { carbs = it }, label = { Text("Carbs (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = fat, onValueChange = { fat = it }, label = { Text("Fat (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cal = calories.toIntOrNull() ?: return@TextButton
                    if (foodName.isBlank()) return@TextButton
                    onAdd(
                        foodName,
                        cal,
                        mealType.ifBlank { null },
                        protein.toDoubleOrNull(),
                        carbs.toDoubleOrNull(),
                        fat.toDoubleOrNull()
                    )
                },
                enabled = foodName.isNotBlank() && calories.isNotBlank()
            ) { Text(if (editing) "Save" else "Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun isToday(ts: Long): Boolean {
    val cal = java.util.Calendar.getInstance()
    val target = java.util.Calendar.getInstance()
    target.timeInMillis = ts
    return cal.get(java.util.Calendar.YEAR) == target.get(java.util.Calendar.YEAR) &&
        cal.get(java.util.Calendar.DAY_OF_YEAR) == target.get(java.util.Calendar.DAY_OF_YEAR)
}

@Composable
private fun MacroDashboardCard(
    uiState: CalorieTrackerUiState,
    onEditTargets: () -> Unit
) {
    val today = uiState.entries.filter { isToday(it.date) }
    val cal = today.sumOf { it.calories }
    val protein = today.sumOf { it.proteinG ?: 0.0 }
    val carbs = today.sumOf { it.carbsG ?: 0.0 }
    val fat = today.sumOf { it.fatG ?: 0.0 }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Today's Macros", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onEditTargets) { Text("Targets") }
            }
            MacroBar(label = "Calories", consumed = cal.toDouble(), target = uiState.calorieTarget?.toDouble(), unit = "kcal")
            MacroBar(label = "Protein", consumed = protein, target = uiState.proteinTargetG?.toDouble(), unit = "g")
            MacroBar(label = "Carbs", consumed = carbs, target = uiState.carbsTargetG?.toDouble(), unit = "g")
            MacroBar(label = "Fat", consumed = fat, target = uiState.fatTargetG?.toDouble(), unit = "g")
        }
    }
}

@Composable
private fun MacroBar(label: String, consumed: Double, target: Double?, unit: String) {
    val progress = if (target != null && target > 0) (consumed / target).toFloat() else 0f
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${consumed.toInt()}/${
                    target?.let { "${it.toInt()}" } ?: "∞"
                } $unit",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LinearProgressIndicator(
            progress = { if (progress >= 1f) 1f else progress },
            modifier = Modifier.fillMaxWidth(),
            color = if (progress >= 1f) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun MacroTargetsDialog(
    initialCalories: String,
    initialProtein: String,
    initialCarbs: String,
    initialFat: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var calories by remember { mutableStateOf(initialCalories) }
    var protein by remember { mutableStateOf(initialProtein) }
    var carbs by remember { mutableStateOf(initialCarbs) }
    var fat by remember { mutableStateOf(initialFat) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Macro Targets") },
        text = {
            Column {
                OutlinedTextField(value = calories, onValueChange = { calories = it }, label = { Text("Daily Calories (kcal)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = protein, onValueChange = { protein = it }, label = { Text("Protein (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = carbs, onValueChange = { carbs = it }, label = { Text("Carbs (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = fat, onValueChange = { fat = it }, label = { Text("Fat (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(calories, protein, carbs, fat) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
