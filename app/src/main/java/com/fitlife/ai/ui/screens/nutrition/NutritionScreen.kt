package com.fitlife.ai.ui.screens.nutrition

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen() {
    var waterMl by remember { mutableIntStateOf(1800) }
    val goalWaterMl = 3000

    val meals = listOf(
        NutritionMeal("Breakfast", "Oatmeal with berries & protein shake", 450.0, "32g", "52g", "12g"),
        NutritionMeal("Lunch", "Grilled chicken breast with quinoa & vegetables", 650.0, "48g", "45g", "22g"),
        NutritionMeal("Snack", "Greek yogurt with almonds", 200.0, "18g", "12g", "10g"),
    )

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Nutrition") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                // Calorie summary
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Daily Summary", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("1,300", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("/ 2,000 kcal", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            MacroItem("Protein", "98g", "150g", MaterialTheme.colorScheme.primary)
                            MacroItem("Carbs", "109g", "250g", MaterialTheme.colorScheme.secondary)
                            MacroItem("Fat", "44g", "65g", MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }

            item {
                // Water tracker
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WaterDrop, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Water Intake", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.weight(1f))
                            Text("${waterMl}ml / ${goalWaterMl}ml", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (waterMl.toFloat() / goalWaterMl).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(150, 250, 500).forEach { amount ->
                                FilledTonalButton(
                                    onClick = { waterMl = (waterMl + amount).coerceAtMost(goalWaterMl) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${amount}ml")
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Meals", style = MaterialTheme.typography.titleLarge)
                    FilledTonalButton(onClick = {}) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Log Food")
                    }
                }
            }

            items(meals) { meal ->
                NutritionMealCard(meal)
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

data class NutritionMeal(
    val type: String,
    val name: String,
    val calories: Double,
    val protein: String,
    val carbs: String,
    val fat: String
)

@Composable
fun MacroItem(label: String, current: String, goal: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(current, style = MaterialTheme.typography.titleMedium, color = color)
        Text("/ $goal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun NutritionMealCard(meal: NutritionMeal) {
    Card(shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                when (meal.type) {
                    "Breakfast" -> Icons.Default.WbSunny
                    "Lunch" -> Icons.Default.WbCloudy
                    else -> Icons.Default.NightlightRound
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(meal.type, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(meal.name, style = MaterialTheme.typography.bodyMedium)
                Text("${meal.protein} P • ${meal.carbs} C • ${meal.fat} F", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("${meal.calories.toInt()}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(" kcal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
