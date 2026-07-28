package com.fitlife.ai.ui.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val color: androidx.compose.ui.graphics.Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val pages = listOf(
        OnboardingPage(Icons.Default.FitnessCenter, "Welcome to FitLife AI", "Your AI-powered fitness companion that adapts to your goals, body, and lifestyle.", MaterialTheme.colorScheme.primary),
        OnboardingPage(Icons.Default.SmartToy, "AI Personal Coach", "Get personalized workout plans, meal suggestions, and real-time guidance powered by Gemini AI.", MaterialTheme.colorScheme.secondary),
        OnboardingPage(Icons.Default.Restaurant, "Smart Nutrition Tracking", "Log meals, track macros, and get AI-powered nutrition advice tailored to your goals.", MaterialTheme.colorScheme.tertiary),
        OnboardingPage(Icons.Default.Analytics, "Blood Report Analysis", "Upload blood reports and get AI-powered analysis with actionable health insights.", MaterialTheme.colorScheme.primary),
        OnboardingPage(Icons.Default.Female, "Cycle Tracking", "Track your menstrual cycle, symptoms, and get personalized workout & nutrition advice for each phase.", MaterialTheme.colorScheme.secondary),
        OnboardingPage(Icons.Default.TrendingUp, "Progress Dashboard", "Track weight, body measurements, and visual progress with detailed analytics charts.", MaterialTheme.colorScheme.tertiary),
        OnboardingPage(Icons.Default.HealthAndSafety, "Health Connect", "Sync steps, heart rate, sleep, and workouts with Google Health Connect.", MaterialTheme.colorScheme.primary),
        OnboardingPage(Icons.Default.CloudSync, "Offline-First Sync", "Use the app anywhere — your data syncs automatically when you're back online.", MaterialTheme.colorScheme.secondary),
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val p = pages[page]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = p.icon,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                    tint = p.color
                )
                Spacer(modifier = Modifier.height(48.dp))
                Text(
                    text = p.title,
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = p.description,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(pages.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (pagerState.currentPage == index) 12.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            if (pagerState.currentPage == pages.lastIndex) {
                Button(
                    onClick = onComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Get Started", style = MaterialTheme.typography.titleMedium)
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.padding(start = 8.dp))
                }
            } else {
                OutlinedButton(
                    onClick = onComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Skip", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Next", style = MaterialTheme.typography.titleMedium)
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}
