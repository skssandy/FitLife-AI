package com.fitlife.ai.ui.screens.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
fun ProgressScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Progress") }) }
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
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Body") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Activity") })
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Streaks") })
                }
            }

            when (selectedTab) {
                0 -> {
                    item {
                        ProgressCard("Weight", "78.5 kg", "-0.5 kg this week", Icons.Default.MonitorWeight, MaterialTheme.colorScheme.primary)
                    }
                    item {
                        ProgressCard("Body Fat", "18.2%", "-0.3% this month", Icons.Default.Percent, MaterialTheme.colorScheme.secondary)
                    }
                    item {
                        ProgressCard("Muscle Mass", "35.1 kg", "+0.2 kg this week", Icons.Default.FitnessCenter, MaterialTheme.colorScheme.tertiary)
                    }
                    item {
                        Card(shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Body Measurements", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(12.dp))
                                MeasurementRow("Waist", "82 cm", "-1 cm")
                                MeasurementRow("Hips", "96 cm", "—")
                                MeasurementRow("Chest", "102 cm", "+0.5 cm")
                                MeasurementRow("Arms", "35 cm", "+0.5 cm")
                            }
                        }
                    }
                }
                1 -> {
                    item {
                        Card(shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Weekly Activity", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    listOf("Mon" to 45, "Tue" to 0, "Wed" to 60, "Thu" to 30, "Fri" to 50, "Sat" to 0, "Sun" to 40).forEach { (day, mins) ->
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box(
                                                modifier = Modifier
                                                    .width(24.dp)
                                                    .height((mins * 1.5f).dp.coerceAtLeast(4.dp))
                                                    .background(
                                                        if (mins > 0) MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.surfaceVariant,
                                                        RoundedCornerShape(4.dp)
                                                    )
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(day, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item { ProgressCard("Total Workouts", "23", "This month", Icons.Default.FitnessCenter, MaterialTheme.colorScheme.primary) }
                    item { ProgressCard("Avg Duration", "42 min", "Per session", Icons.Default.Schedule, MaterialTheme.colorScheme.secondary) }
                }
                2 -> {
                    item { ProgressCard("Current Streak", "7 days", "Best: 14 days", Icons.Default.LocalFireDepartment, MaterialTheme.colorScheme.primary) }
                    item { ProgressCard("Workouts This Month", "18", "Goal: 20", Icons.Default.EmojiEvents, MaterialTheme.colorScheme.secondary) }
                    item { ProgressCard("Calories Burned", "12,450", "This month", Icons.Default.LocalFireDepartment, MaterialTheme.colorScheme.tertiary) }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun ProgressCard(title: String, value: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: androidx.compose.ui.graphics.Color) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.headlineMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun MeasurementRow(name: String, value: String, change: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(name, style = MaterialTheme.typography.bodyMedium)
        Row {
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Text(change, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
