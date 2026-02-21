package com.example.voltroute.presentation.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.voltroute.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * SplashScreen - Initial loading screen with animations
 *
 * Animation sequence:
 * 1. Fade in + scale up logo and text (800ms, simultaneous)
 * 2. Progress bar fills (1200ms)
 * 3. Brief pause (300ms)
 * 4. Navigate to main screen
 *
 * Total duration: ~2.3 seconds
 */
@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit
) {
    // Animation values
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.8f) }
    val progress = remember { Animatable(0f) }

    // Execute animation sequence
    LaunchedEffect(Unit) {
        // Step 1 & 2: Fade in and scale up simultaneously
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 800)
            )
        }
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )

        // Step 3: Animate progress bar
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1200)
        )

        // Step 4: Pause before navigation
        delay(300)

        // Step 5: Navigate away from splash
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .alpha(alpha.value)
                .scale(scale.value),
            horizontalAlignment = CenterHorizontally,
            verticalArrangement = spacedBy(16.dp)
        ) {
            // VoltRoute Logo (PNG)
            Image(
                painter = painterResource(id = R.drawable.voltroutelogo),
                contentDescription = "VoltRoute Logo",
                modifier = Modifier.size(200.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Progress indicator
            LinearProgressIndicator(
                progress = { progress.value },
                modifier = Modifier
                    .width(200.dp)
                    .height(4.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )

            // Loading text
            Text(
                text = "Loading...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
    }
}
