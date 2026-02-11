package com.sandeep.silentzone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.sandeep.silentzone.R
import com.sandeep.silentzone.RingerMode

@Composable
fun SilentZoneAnimation(mode: RingerMode) {
    // 1. Load the Composition
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.anim_normal_mode))
    
    // 2. Control Playback
    // Normal Mode: Loop Forever
    // Silent Mode: Pause at 0% (or current frame)
    val isPlaying = mode == RingerMode.NORMAL
    
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        isPlaying = isPlaying,
        speed = 1.0f
    )
    
    // 3. UI Colors
    val cardColor = if (mode == RingerMode.SILENT) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // 4. Render Animation
            if (composition != null) {
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                        .graphicsLayer {
                            scaleX = 1.1f
                            scaleY = 1.1f
                            // Dim the animation in silent mode
                            alpha = if (mode == RingerMode.SILENT) 0.5f else 1.0f
                        }
                )
            } else {
                Text("Loading Animation...", style = MaterialTheme.typography.bodySmall)
            }

            // 5. Status Label
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(bottom = 1.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (mode == RingerMode.SILENT) "Silent Zone Active" else "Normal Zone",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
