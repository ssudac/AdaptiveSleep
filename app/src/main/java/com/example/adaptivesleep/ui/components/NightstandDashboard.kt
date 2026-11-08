package com.example.adaptivesleep.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adaptivesleep.data.model.AcousticMode
import com.example.adaptivesleep.data.model.BrainwaveBand
import com.example.adaptivesleep.data.model.NoiseType
import com.example.adaptivesleep.data.model.PhysiologicalState
import com.example.adaptivesleep.data.model.SleepStage
import com.example.adaptivesleep.data.model.SmartAlarmConfig
import com.example.adaptivesleep.data.model.TransducerType

/**
 * Top vitals card showing current Sleep Stage with custom color accents and descriptions.
 */
@Composable
fun SleepStageCard(
    vitals: PhysiologicalState,
    targetBand: BrainwaveBand,
    modifier: Modifier = Modifier
) {
    val stageColor by animateColorAsState(
        targetValue = when (vitals.sleepStage) {
            SleepStage.AWAKE -> Color(0xFFFBBF24) // Amber
            SleepStage.LIGHT -> Color(0xFF38BDF8) // Cyan
            SleepStage.DEEP -> Color(0xFF818CF8)  // Indigo
            SleepStage.REM -> Color(0xFF34D399)   // Emerald
            SleepStage.UNKNOWN -> Color(0xFF94A3B8)
        },
        label = "StageColor"
    )

    val stageIcon: ImageVector = when (vitals.sleepStage) {
        SleepStage.AWAKE -> Icons.Default.WbSunny
        SleepStage.LIGHT -> Icons.Default.Bedtime
        SleepStage.DEEP -> Icons.Default.Nightlight
        SleepStage.REM -> Icons.Default.RemoveRedEye
        SleepStage.UNKNOWN -> Icons.Default.Psychology
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sleep_stage_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF131826)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, stageColor.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(stageColor.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = stageIcon,
                            contentDescription = vitals.sleepStage.displayName,
                            tint = stageColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "CURRENT SLEEP PHASE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF64748B),
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = vitals.sleepStage.displayName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color(0xFFF8FAFC),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                // Target Brainwave Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(stageColor.copy(alpha = 0.18f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = targetBand.name,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = stageColor,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "${vitals.sleepStage.description} • Modulating ${targetBand.stateDescription.lowercase()}.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF94A3B8),
                    lineHeight = 18.sp
                )
            )
        }
    }
}

/**
 * Real-time Physiological Vitals card: Heart Rate, HRV RMSSD, and Baseline Delta.
 */
@Composable
fun VitalsCard(
    vitals: PhysiologicalState,
    modifier: Modifier = Modifier
) {
    val heartTransition = rememberInfiniteTransition(label = "HeartPulse")
    val pulseDuration = (60000 / vitals.heartRateBpm.coerceIn(40, 140)).coerceIn(400, 1500)
    val heartScale by heartTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = pulseDuration / 2),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HeartScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("vitals_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF131826)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Heart Rate Metric
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Heart Rate",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier
                        .size(32.dp)
                        .scale(heartScale)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${vitals.heartRateBpm}",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                color = Color(0xFFF8FAFC),
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "BPM",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8))
                        )
                    }
                    val deltaPrefix = if (vitals.deltaHr >= 0) "+${vitals.deltaHr}" else "${vitals.deltaHr}"
                    Text(
                        text = "Δ $deltaPrefix bpm vs rest (${vitals.baselineRestingHr})",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (vitals.deltaHr > 10) Color(0xFFFBBF24) else Color(0xFF64748B)
                        )
                    )
                }
            }

            // HRV RMSSD Metric
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "HRV (RMSSD)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = String.format("%.0f", vitals.hrvRmssdMillis),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ms",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8))
                    )
                }
                val autonomicText = when {
                    vitals.hrvRmssdMillis >= 65.0 -> "Deep parasympathetic"
                    vitals.hrvRmssdMillis >= 45.0 -> "Relaxed / Settling"
                    else -> "Autonomic arousal"
                }
                Text(
                    text = autonomicText,
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
                )
            }
        }
    }
}

/**
 * Transducer & Output Routing Card: Automatically switches between Binaural Beats
 * (Headphones) and Isochronic / Monaural Tones (Open Room Speakers).
 */
@Composable
fun TransducerCard(
    activeTransducer: TransducerType,
    deviceDescription: String,
    activeMode: AcousticMode,
    manualOverride: AcousticMode?,
    onSelectMode: (AcousticMode?) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("transducer_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131826)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val icon = if (activeTransducer == TransducerType.HEADPHONES) {
                        Icons.Default.Headphones
                    } else {
                        Icons.Default.Speaker
                    }
                    val iconTint = if (activeTransducer == TransducerType.HEADPHONES) {
                        Color(0xFF38BDF8)
                    } else {
                        Color(0xFFFBBF24)
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(iconTint.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = "Audio Routing",
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "ACOUSTIC TRANSDUCER ROUTING",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF64748B),
                                letterSpacing = 1.1.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = deviceDescription,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFFF1F5F9),
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Delivery Mode Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = manualOverride == null,
                    onClick = { onSelectMode(null) },
                    label = { Text("Auto", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF2563EB),
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("mode_auto")
                )
                FilterChip(
                    selected = activeMode == AcousticMode.BINAURAL && manualOverride != null,
                    onClick = { onSelectMode(AcousticMode.BINAURAL) },
                    label = { Text("Binaural", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF38BDF8),
                        selectedLabelColor = Color(0xFF0F172A)
                    ),
                    modifier = Modifier.testTag("mode_binaural")
                )
                FilterChip(
                    selected = activeMode == AcousticMode.ISOCHRONIC && manualOverride != null,
                    onClick = { onSelectMode(AcousticMode.ISOCHRONIC) },
                    label = { Text("Isochronic", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFF59E0B),
                        selectedLabelColor = Color(0xFF0F172A)
                    ),
                    modifier = Modifier.testTag("mode_isochronic")
                )
                FilterChip(
                    selected = activeMode == AcousticMode.MONAURAL && manualOverride != null,
                    onClick = { onSelectMode(AcousticMode.MONAURAL) },
                    label = { Text("Monaural", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFA855F7),
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("mode_monaural")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${activeMode.displayName}: ${activeMode.description}.",
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
            )
        }
    }
}

/**
 * Smart Alarm Configuration Card: Configures morning wake-up window and displays live monitoring status.
 */
@Composable
fun SmartAlarmCard(
    alarmConfig: SmartAlarmConfig,
    onToggle: (Boolean) -> Unit,
    onEditWindow: () -> Unit,
    onTestAlarm: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("smart_alarm_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131826)),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (alarmConfig.isEnabled) Color(0xFFF59E0B).copy(alpha = 0.5f) else Color(0xFF1E293B)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFFF59E0B).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = "Smart Alarm",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "SMART SLEEP ALARM",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF64748B),
                                letterSpacing = 1.1.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Wake-Up Window: ${alarmConfig.formatWindow()}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFFF8FAFC),
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                Switch(
                    checked = alarmConfig.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFFF59E0B)
                    ),
                    modifier = Modifier.testTag("smart_alarm_toggle")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val statusText = if (alarmConfig.isEnabled) {
                "Active • Evaluates sleep stages within the window and triggers alarm during the lightest sleep phase."
            } else {
                "Disabled • Turn on to awaken refreshed without abrupt sleep inertia."
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onEditWindow,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("edit_alarm_window_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Change Window", fontSize = 12.sp, color = Color(0xFFCBD5E1))
                }

                Button(
                    onClick = onTestAlarm,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("test_alarm_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF59E0B).copy(alpha = 0.2f),
                        contentColor = Color(0xFFFBBF24)
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Preview Alarm", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/**
 * Audio Synthesis Tuning & Ambient Noise controls.
 */
@Composable
fun AudioControlsCard(
    carrierHz: Float,
    beatHz: Float,
    noiseType: NoiseType,
    noiseVolume: Float,
    noiseFilterCutoffHz: Float,
    masterVolume: Float,
    onCarrierChanged: (Float) -> Unit,
    onNoiseTypeChanged: (NoiseType) -> Unit,
    onNoiseVolumeChanged: (Float) -> Unit,
    onNoiseCutoffChanged: (Float) -> Unit,
    onMasterVolumeChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("audio_controls_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131826)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "ACOUSTIC ENTRAINMENT & NOISE MASKING",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF64748B),
                        letterSpacing = 1.1.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Carrier Frequency Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Base Carrier Pitch",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFE2E8F0))
                )
                Text(
                    text = "${carrierHz.toInt()} Hz",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Slider(
                value = carrierHz,
                onValueChange = onCarrierChanged,
                valueRange = 100f..250f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF38BDF8),
                    activeTrackColor = Color(0xFF38BDF8)
                ),
                modifier = Modifier.testTag("carrier_slider")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Soothing Noise Selector
            Text(
                text = "Ambient Noise Masking",
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFE2E8F0))
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NoiseType.entries.forEach { type ->
                    FilterChip(
                        selected = noiseType == type,
                        onClick = { onNoiseTypeChanged(type) },
                        label = { Text(type.name, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF475569),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            if (noiseType != NoiseType.OFF) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Noise Volume",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                    )
                    Text(
                        text = "${(noiseVolume * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFCBD5E1))
                    )
                }
                Slider(
                    value = noiseVolume,
                    onValueChange = onNoiseVolumeChanged,
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF94A3B8),
                        activeTrackColor = Color(0xFF64748B)
                    ),
                    modifier = Modifier.testTag("noise_volume_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Warmth (Low-Pass Filter)",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                    )
                    Text(
                        text = "${noiseFilterCutoffHz.toInt()} Hz",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFCBD5E1))
                    )
                }
                Slider(
                    value = noiseFilterCutoffHz,
                    onValueChange = onNoiseCutoffChanged,
                    valueRange = 400f..3000f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF94A3B8),
                        activeTrackColor = Color(0xFF64748B)
                    )
                )
            }
        }
    }
}

/**
 * Diagnostic & Simulation Panel to test dynamic rule evaluation and sensor triggers in real-time.
 */
@Composable
fun SimulationPanel(
    isSimulationMode: Boolean,
    isHealthConnectAvailable: Boolean,
    onToggleSimulation: (Boolean) -> Unit,
    onSimulateStage: (SleepStage) -> Unit,
    onSimulateSpike: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("simulation_panel"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SENSOR SIMULATION & DEMO CONTROLS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                }

                Switch(
                    checked = isSimulationMode,
                    onCheckedChange = onToggleSimulation,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = Color(0xFF38BDF8)
                    ),
                    modifier = Modifier.testTag("simulation_mode_toggle")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (isSimulationMode) {
                    "Simulation active: Tap any stage below to immediately trigger the real-time DSP carrier & beat transitions."
                } else {
                    "Live Health Connect mode: Polling connected wearable SleepSession, HeartRate, and HRV data."
                },
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onSimulateStage(SleepStage.AWAKE) },
                    modifier = Modifier.weight(1f).testTag("sim_awake"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Awake", fontSize = 11.sp, color = Color(0xFFFBBF24))
                }
                OutlinedButton(
                    onClick = { onSimulateStage(SleepStage.LIGHT) },
                    modifier = Modifier.weight(1f).testTag("sim_light"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Light", fontSize = 11.sp, color = Color(0xFF38BDF8))
                }
                OutlinedButton(
                    onClick = { onSimulateStage(SleepStage.DEEP) },
                    modifier = Modifier.weight(1f).testTag("sim_deep"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Deep", fontSize = 11.sp, color = Color(0xFF818CF8))
                }
                OutlinedButton(
                    onClick = { onSimulateStage(SleepStage.REM) },
                    modifier = Modifier.weight(1f).testTag("sim_rem"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("REM", fontSize = 11.sp, color = Color(0xFF34D399))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onSimulateSpike,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sim_spike"),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Simulate Autonomic HR Spike (Arousal Event)", fontSize = 11.sp, color = Color(0xFFEF4444))
            }
        }
    }
}
