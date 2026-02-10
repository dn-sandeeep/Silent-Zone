package com.sandeep.silentzone.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.rounded.TableRestaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.sandeep.silentzone.RingerMode

@Composable
fun SilentZoneAnimation(mode: RingerMode) {
    val infiniteTransition = rememberInfiniteTransition(label = "AmbientEffects")
    
    // Pulse Animation for WiFi (Scale 1f -> 1.2f -> 1f)
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    // Floating Animation for "Zzz" (Y offset)
    val floatY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -20f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Float"
    )

    // Background Color Transition
    val cardColor by animateColorAsState(
        targetValue = if (mode == RingerMode.SILENT) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) 
                      else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(1000),
        label = "CardColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp), // Increased height slightly
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            
            // Animation for Person Position (0f to 1f)
            val targetValue = if (mode == RingerMode.SILENT) 1f else 0f
            val progress by animateFloatAsState(
                targetValue = targetValue,
                animationSpec = tween(durationMillis = 1500, easing = LinearEasing),
                label = "Progress"
            )

            val isInZone = progress > 0.4f
            val isAtDesk = progress > 0.8f

            // Color Animations
            val wifiColor by animateColorAsState(
                targetValue = if (isInZone) Color(0xFF4CAF50) else Color.Gray,
                label = "WiFiColor"
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Status Icons Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // WiFi Status with Pulse
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isInZone) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .scale(pulseScale)
                                        .background(wifiColor.copy(alpha = 0.2f), RoundedCornerShape(50))
                                )
                            }
                            Icon(
                                imageVector = if (isInZone) Icons.Default.Wifi else Icons.Default.SignalWifiOff,
                                contentDescription = null,
                                tint = wifiColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = if (isInZone) "Connected" else "Disconnected",
                            style = MaterialTheme.typography.labelSmall,
                            color = wifiColor
                        )
                    }
                    
                    // Phone Status
                    StatusIcon(
                        icon = if (isInZone) Icons.Default.DoNotDisturbOn else Icons.Default.Notifications,
                        label = if (isInZone) "Silent" else "Ringing",
                        tint = if (isInZone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // The Scene (Room & Animation)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(2f)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                ) {
                    // Room Area (Right side)
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.6f)
                            .align(Alignment.CenterEnd)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 24.dp), // Push slightly left
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Top Side Chairs
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Chair, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f))
                                Icon(Icons.Default.Chair, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f))
                            }

                            // Table Surface
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .size(width = 80.dp, height = 40.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                // Computer/Workstation
                                Icon(
                                    imageVector = Icons.Default.Computer,
                                    contentDescription = "Desk",
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            // Bottom Side Chairs
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Chair, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f))
                                Icon(Icons.Default.Chair, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f))
                            }
                        }

                        // Zzz Particles when at Desk (Adjusted position)
                        if (isAtDesk) {
                             Text(
                                "Zzz",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .offset(x = -40.dp, y = -50.dp + floatY.dp) // Move up and centered relative to table
                            )
                        }
                    }

                    // Animated Person
                    val startX = 20.dp
                    val endX = 180.dp // Adjusted for table position
                    val userPositionX = startX + (endX - startX) * progress
                    
                    Icon(
                        imageVector = if (isAtDesk) Icons.Default.Chair else Icons.Default.Person,
                        contentDescription = "Person",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.CenterStart)
                            .offset(x = userPositionX)
                    )
                }
            }
        }
    }
}

@Composable
fun StatusIcon(icon: ImageVector, label: String, tint: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}
