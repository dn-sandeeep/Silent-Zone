package com.sandeep.silentzone.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sandeep.silentzone.ImportantContact
import com.sandeep.silentzone.LocationZone
import com.sandeep.silentzone.RingerMode
import com.sandeep.silentzone.ui.theme.*

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = GlassWhite),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}

@Composable
fun PulseStatusHeader(mode: RingerMode, isFallback: Boolean = false) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    val floatingOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatingOffset"
    )

    val gradientColors = when (mode) {
        RingerMode.SILENT -> listOf(RoseAccent, Color(0xFF9F1239))
        RingerMode.VIBRATE -> listOf(TealAccent, Color(0xFF115E59))
        RingerMode.NORMAL -> listOf(IndigoAccent, Color(0xFF3730A3))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .background(Brush.verticalGradient(listOf(DeepSpace, MidnightBlue)))
            .padding(bottom = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Animated Pulse Rings
        Canvas(modifier = Modifier.size(220.dp)) {
            drawCircle(
                color = gradientColors[0],
                radius = (size.minDimension / 2) * pulseScale,
                alpha = pulseAlpha,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Main Status Icon with Glow
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .offset(y = floatingOffset.dp),
                contentAlignment = Alignment.Center
            ) {
                // Glow Background
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .blur(20.dp)
                        .background(gradientColors[0].copy(alpha = 0.5f), CircleShape)
                )
                
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(gradientColors))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (mode) {
                            RingerMode.SILENT -> Icons.Default.NotificationsOff
                            RingerMode.VIBRATE -> Icons.Default.Vibration
                            RingerMode.NORMAL -> Icons.Default.NotificationsActive
                        },
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isFallback) "${when (mode) {
                    RingerMode.SILENT -> "Silent"
                    RingerMode.VIBRATE -> "Vibrate"
                    RingerMode.NORMAL -> "Normal"
                }} (Fallback)" else when (mode) {
                    RingerMode.SILENT -> "Silent Mode"
                    RingerMode.VIBRATE -> "Vibrate Mode"
                    RingerMode.NORMAL -> "Normal Mode"
                },
                style = MaterialTheme.typography.headlineMedium.copy(
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Black
                ),
                color = Color.White
            )
            
            Text(
                text = when (mode) {
                    RingerMode.SILENT -> "ALL SOUNDS MUTED"
                    RingerMode.VIBRATE -> "HAPTIC FEEDBACK ONLY"
                    RingerMode.NORMAL -> "ALL SOUNDS ENABLED"
                },
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = gradientColors[0].copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun ImportantContactItemCard(contact: ImportantContact, onDelete: () -> Unit) {
    GlassCard(modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(IndigoAccent.copy(alpha = 0.1f))
                        .border(1.dp, IndigoAccent.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = IndigoAccent
                    )
                }
                Column {
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = contact.phoneNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(RoseAccent.copy(alpha = 0.1f))
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = RoseAccent
                )
            }
        }
    }
}

@Composable
fun DashboardSectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelLarge.copy(
            letterSpacing = 1.5.sp,
            fontWeight = FontWeight.Black
        ),
        color = Color.White.copy(alpha = 0.5f),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp)
    )
}

@Composable
fun ModeToggleCard(
    title: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    activeColor: Color = IndigoAccent
) {
    val scale by animateFloatAsState(if (isActive) 1.05f else 1f, label = "scale")
    
    Card(
        modifier = Modifier
            .height(110.dp)
            .width(105.dp)
            .scale(scale)
            .clickable { onClick() }
            .border(
                1.dp, 
                if (isActive) activeColor.copy(alpha = 0.5f) else GlassBorder, 
                RoundedCornerShape(24.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) activeColor.copy(alpha = 0.15f) else GlassWhite
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) activeColor else Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isActive) Color.White else Color.White.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
fun DndActionCard(onGrantAccess: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .border(1.dp, RoseAccent.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = RoseAccent.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(RoseAccent.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.NotificationsPaused, null, tint = RoseAccent)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Silent Mode Restricted",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "Grant DND access to enable full silence.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            Button(
                onClick = onGrantAccess,
                colors = ButtonDefaults.buttonColors(containerColor = RoseAccent),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Text("FIX NOW", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun OperationOverlay(state: com.sandeep.silentzone.OperationState) {
    AnimatedVisibility(
        visible = state !is com.sandeep.silentzone.OperationState.Idle,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            val color = when (state) {
                is com.sandeep.silentzone.OperationState.Loading -> CyanAccent
                is com.sandeep.silentzone.OperationState.Success -> TealAccent
                is com.sandeep.silentzone.OperationState.Error -> RoseAccent
                else -> Color.Gray
            }
            
            Card(
                modifier = Modifier
                    .wrapContentWidth()
                    .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = MidnightBlue.copy(alpha = 0.9f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (state is com.sandeep.silentzone.OperationState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = color,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = when (state) {
                                is com.sandeep.silentzone.OperationState.Success -> Icons.Default.CheckCircle
                                is com.sandeep.silentzone.OperationState.Error -> Icons.Default.Error
                                else -> Icons.Default.Info
                            },
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Text(
                        text = when (state) {
                            is com.sandeep.silentzone.OperationState.Loading -> "Processing..."
                            is com.sandeep.silentzone.OperationState.Success -> state.message
                            is com.sandeep.silentzone.OperationState.Error -> state.message
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionWarningCard(onGrantAccess: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        colors = CardDefaults.cardColors(containerColor = RoseAccent.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, RoseAccent.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.PriorityHigh,
                    contentDescription = null,
                    tint = RoseAccent
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Permission Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = RoseAccent
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "SilentZone needs 'Do Not Disturb' access to manage your ringer modes automatically.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onGrantAccess,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = RoseAccent),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text("Grant Access", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun SsidSelectionDialog(
    ssids: List<String>,
    onSsidSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Nearby Networks", fontWeight = FontWeight.Black, color = Color.White)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ssids.forEach { ssid ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(GlassWhite)
                            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                            .clickable { onSsidSelected(ssid) }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(32.dp).background(CyanAccent.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Wifi, null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                        }
                        Text(ssid, color = Color.White, fontWeight = FontWeight.Medium)
                    }
                }
                if (ssids.isEmpty()) {
                    Text("Searching for networks...", color = Color.White.copy(alpha = 0.4f))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = RoseAccent, fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(32.dp),
        containerColor = MidnightBlue
    )
}

@Composable
fun ZoneItemCard(ssid: String, onDelete: () -> Unit) {
    GlassCard(modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyanAccent.copy(alpha = 0.1f))
                        .border(1.dp, CyanAccent.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Wifi, null, tint = CyanAccent)
                }
                Text(ssid, fontWeight = FontWeight.Bold, color = Color.White)
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(RoseAccent.copy(alpha = 0.1f))
            ) {
                Icon(Icons.Default.DeleteOutline, null, tint = RoseAccent)
            }
        }
    }
}

@Composable
fun ModeSelectionDialog(
    ssid: String,
    onModeSelected: (RingerMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidnightBlue,
        shape = RoundedCornerShape(32.dp),
        title = { Text("Configure Zone", fontWeight = FontWeight.Black, color = Color.White) },
        text = {
            Column {
                Text("Switch mode when connected to $ssid:", color = Color.White.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { onModeSelected(RingerMode.SILENT) },
                        modifier = Modifier.weight(1f).height(60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RoseAccent),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("SILENT", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onModeSelected(RingerMode.VIBRATE) },
                        modifier = Modifier.weight(1f).height(60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("VIBRATE", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color.White.copy(alpha = 0.4f))
            }
        }
    )
}

@Composable
fun RadiusSelectionDialog(
    onRadiusSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var radius by remember { mutableStateOf(100f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidnightBlue,
        shape = RoundedCornerShape(32.dp),
        title = { Text("Search Radius", fontWeight = FontWeight.Black, color = Color.White) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("${radius.toInt()}m", style = MaterialTheme.typography.displaySmall, color = CyanAccent, fontWeight = FontWeight.Black)
                }
                Spacer(modifier = Modifier.height(24.dp))
                Slider(
                    value = radius,
                    onValueChange = { radius = it },
                    valueRange = 50f..1000f,
                    steps = 18,
                    colors = SliderDefaults.colors(
                        thumbColor = CyanAccent,
                        activeTrackColor = CyanAccent,
                        inactiveTrackColor = GlassBorder
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onRadiusSelected(radius) },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("SET RADIUS", color = MidnightBlue, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun LocationZoneItemCard(zone: LocationZone, onDelete: () -> Unit) {
    GlassCard(modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(TealAccent.copy(alpha = 0.1f))
                        .border(1.dp, TealAccent.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.LocationOn, null, tint = TealAccent)
                }
                Column {
                    Text(zone.name, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("${zone.radius.toInt()}m range", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
                }
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(RoseAccent.copy(alpha = 0.1f))
            ) {
                Icon(Icons.Default.DeleteOutline, null, tint = RoseAccent)
            }
        }
    }
}
