package com.fitlife.ai.ui.screens.nutrition

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitlife.ai.data.MealPlan
import com.fitlife.ai.util.FoodDiet
import com.fitlife.ai.viewmodel.NutritionPlanUiState
import com.fitlife.ai.viewmodel.NutritionPlanViewModel

@Composable
fun NutritionPlanScreen(
    onBack: () -> Unit = {},
    viewModel: NutritionPlanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDish by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Nutrition Plan", style = MaterialTheme.typography.headlineMedium)
                    Text("Plan, macros & adherence", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(onClick = onBack) { Text("Back") }
            }
        }

        uiState.error?.let {
            item {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }

        if (uiState.isLoading) {
            item { CircularProgressIndicator(modifier = Modifier.padding(16.dp)) }
            return@LazyColumn
        }

        item {
            AdherenceCard(score = uiState.adherenceToday)
        }

        item {
            MacroRingsCard(uiState = uiState)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(onClick = { viewModel.recalculateTargets() }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                    Text(" Recalculate Targets")
                }
                if (!uiState.hasProfileData) {
                    OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                        Text("Complete Profile")
                    }
                }
            }
        }

        item {
            OutlinedButton(onClick = { showAddDish = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Text(" Add custom dish to your plan")
            }
        }

        uiState.targets?.let { targets ->
            item {
                Text("Daily Targets", style = MaterialTheme.typography.titleLarge)
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TargetStat("Calories", "${targets.calories} kcal")
                        TargetStat("Protein", "${targets.proteinG} g")
                        TargetStat("Carbs", "${targets.carbsG} g")
                        TargetStat("Fat", "${targets.fatG} g")
                    }
                }
            }
        }

        item {
            Text("Today's Plan", style = MaterialTheme.typography.titleLarge)
        }

        if (uiState.plan.isEmpty()) {
            item {
                Text(
                    "Set your targets first to generate a meal plan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(uiState.plan, key = { it.name }) { meal ->
                MealPlanCard(meal = meal)
            }
        }

        if (uiState.weeklyAdherence.isNotEmpty()) {
            item { Text("Adherence This Week", style = MaterialTheme.typography.titleLarge) }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        uiState.weeklyAdherence.forEach { (label, score) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(36.dp)
                            ) {
                                Text("$score", style = MaterialTheme.typography.labelSmall)
                                Spacer(Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(90.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                        )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height((90.dp * score.coerceIn(0, 100) / 100f))
                                            .align(Alignment.BottomCenter)
                                            .background(
                                                when {
                                                    score >= 70 -> MaterialTheme.colorScheme.primary
                                                    score >= 40 -> MaterialTheme.colorScheme.tertiary
                                                    else -> MaterialTheme.colorScheme.error
                                                },
                                                RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                            )
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(label, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDish) {
        AddDishDialog(
            onDismiss = { showAddDish = false },
            onAdd = { name, category, serving, calories, protein, carbs, fat, dietLabel ->
                viewModel.addCustomDish(name, category, serving, calories, protein, carbs, fat, dietLabel)
                showAddDish = false
            }
        )
    }
}

@Composable
private fun AddDishDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, Int, Double, Double, Double, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Indian Staples") }
    var serving by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var dietLabel by remember { mutableStateOf(FoodDiet.options.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add custom dish") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Missing a dish or menu item? Add it here and it will be suggested in your meal plan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Dish name") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = serving, onValueChange = { serving = it },
                        label = { Text("Serving") }, placeholder = { Text("e.g. 1 cup") },
                        singleLine = true, modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = calories, onValueChange = { calories = it },
                        label = { Text("Calories") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true, modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = protein, onValueChange = { protein = it },
                        label = { Text("Protein (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true, modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = carbs, onValueChange = { carbs = it },
                        label = { Text("Carbs (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true, modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = fat, onValueChange = { fat = it },
                        label = { Text("Fat (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true, modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = category, onValueChange = { category = it },
                        label = { Text("Category") },
                        singleLine = true, modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text("Diet", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FoodDiet.options.forEach { option ->
                        FilterChip(
                            selected = dietLabel == option,
                            onClick = { dietLabel = option },
                            label = { Text(option) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val c = calories.toIntOrNull() ?: return@TextButton
                    if (name.isBlank()) return@TextButton
                    onAdd(
                        name, category, serving,
                        c, protein.toDoubleOrNull() ?: 0.0, carbs.toDoubleOrNull() ?: 0.0,
                        fat.toDoubleOrNull() ?: 0.0, dietLabel
                    )
                },
                enabled = name.isNotBlank() && calories.toIntOrNull() != null
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun TargetStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AdherenceCard(score: Double) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Ring(
                progress = score.toFloat(),
                size = 84.dp,
                strokeWidth = 10.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface
            )
            Column {
                Text("Daily Adherence", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${(score * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "Weighted: calories 40% · protein 30% · carbs 15% · fat 10% · water 5%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MacroRingsCard(uiState: NutritionPlanUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Macros Today", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MacroRing(
                    label = "Calories",
                    consumed = uiState.caloriesEaten.toFloat(),
                    target = uiState.targets?.calories?.toFloat() ?: 0f,
                    color = MaterialTheme.colorScheme.primary
                )
                MacroRing(
                    label = "Protein",
                    consumed = uiState.proteinEaten.toFloat(),
                    target = uiState.targets?.proteinG?.toFloat() ?: 0f,
                    color = MaterialTheme.colorScheme.tertiary
                )
                MacroRing(
                    label = "Water",
                    consumed = uiState.waterMl.toFloat(),
                    target = uiState.hydrationTargetMl?.toFloat() ?: 0f,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(Modifier.height(12.dp))
            MacroBar("Protein", uiState.proteinEaten, uiState.targets?.proteinG ?: 0, "g")
            MacroBar("Carbs", uiState.carbsEaten, uiState.targets?.carbsG ?: 0, "g")
            MacroBar("Fat", uiState.fatEaten, uiState.targets?.fatG ?: 0, "g")
        }
    }
}

@Composable
private fun MacroRing(label: String, consumed: Float, target: Float, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Ring(
            progress = if (target > 0) (consumed / target).coerceIn(0f, 1f) else 0f,
            size = 64.dp,
            strokeWidth = 8.dp,
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(
            if (target > 0) "${consumed.toInt()}/${target.toInt()}" else "${consumed.toInt()}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MacroBar(label: String, consumed: Double, target: Int, unit: String) {
    val progress = if (target > 0) (consumed / target).toFloat() else 0f
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(
            "${consumed.toInt()}/${if (target > 0) target else "∞"} $unit",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    LinearProgressIndicator(
        progress = { if (progress >= 1f) 1f else progress },
        modifier = Modifier.fillMaxWidth().height(6.dp),
        color = if (progress >= 1f) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant
    )
}

@Composable
private fun Ring(
    progress: Float,
    size: Dp,
    strokeWidth: Dp,
    color: Color,
    trackColor: Color
) {
    Canvas(modifier = Modifier.size(size)) {
        val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        val inset = strokeWidth.toPx() / 2
        val arcSize = Size(size.toPx() - inset * 2, size.toPx() - inset * 2)
        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = stroke
        )
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * progress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = stroke
        )
    }
}

@Composable
private fun MealPlanCard(meal: MealPlan) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(meal.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("~${meal.targetCalories} kcal", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            if (meal.items.isEmpty()) {
                Text("Add foods to the database to see suggestions.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                meal.items.forEach { food ->
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(food.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(
                            "${food.calories} kcal · P${food.proteinG} C${food.carbsG} F${food.fatG}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
