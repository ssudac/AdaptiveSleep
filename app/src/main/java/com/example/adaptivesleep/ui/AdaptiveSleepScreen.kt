package com.example.adaptivesleep.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WbIncandescent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import com.example.adaptivesleep.data.model.BrainwaveBand
import com.example.adaptivesleep.data.model.TransducerType
import com.example.adaptivesleep.ui.components.AlarmWindowDialog
import com.example.adaptivesleep.ui.components.AudioControlsCard
import com.example.adaptivesleep.ui.components.NightstandClock
import com.example.adaptivesleep.ui.components.SimulationPanel
import com.example.adaptivesleep.ui.components.SleepStageCard
import com.example.adaptivesleep.ui.components.SmartAlarmCard
import com.example.adaptivesleep.ui.components.SmartAlarmDialog
import com.example.adaptivesleep.ui.components.TransducerCard
import com.example.adaptivesleep.ui.components.VitalsCard
import com.example.adaptivesleep.ui.components.WaveformVisualizer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveSleepScreen(
    viewModel: AdaptiveSleepViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAlarmWindowDialog by remember { mutableStateOf(false) }

    // Health Connect Permission Launcher
    val healthPermissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->
        viewModel.checkHealthConnectStatus()
    }

    // OLED Nightstand Dimmed Mode Overlay
    if (uiState.isDimmedMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF020408))
                .clickable { viewModel.toggleDimmedMode() }
                .testTag("dimmed_mode_overlay"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                NightstandClock(isDimmed = true)
                Spacer(modifier = Modifier.height(18.dp))
                WaveformVisualizer(
                    carrierHz = uiState.synthesisParams.carrierFrequencyHz,
                    beatHz = uiState.synthesisParams.beatFrequencyHz,
                    targetBand = uiState.synthesisParams.targetBand,
                    mode = uiState.synthesisParams.acousticMode,
                    isPlaying = uiState.isPlaying,
                    isDimmed = true,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "${uiState.vitals.sleepStage.displayName} • ${uiState.vitals.heartRateBpm} BPM",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tap anywhere to wake display",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF334155))
                )
            }
        }
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF08090C),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "AdaptiveSleep",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF8FAFC)
                            )
                        )
                        Text(
                            text = "Autonomous Acoustic Sleep Modulation",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF64748B)
                            )
                        )
                    }
                },
                actions = {
                    // Dim Display Action for nightstand
                    IconButton(
                        onClick = { viewModel.toggleDimmedMode() },
                        modifier = Modifier.testTag("dim_mode_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Brightness2,
                            contentDescription = "Dim Display for Sleep",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF08090C)
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Health Connect Status Banner (if permissions needed)
            if (uiState.isHealthConnectAvailable && !uiState.hasHealthPermissions) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1E293B).copy(alpha = 0.9f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth().testTag("health_connect_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HealthAndSafety,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Connect Google Health Connect",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = Color(0xFFF1F5F9),
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = "Read real-time SleepSession, HR & HRV from your wearable.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFF94A3B8)
                                        )
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    healthPermissionLauncher.launch(viewModel.healthConnectManager.permissions)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF38BDF8),
                                    contentColor = Color(0xFF0F172A)
                                ),
                                modifier = Modifier.testTag("grant_health_permissions_button")
                            ) {
                                Text("Connect", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 2. Nightstand Clock & Real-Time Waveform Visualizer
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        NightstandClock(isDimmed = false)

                        Spacer(modifier = Modifier.height(16.dp))

                        WaveformVisualizer(
                            carrierHz = uiState.synthesisParams.carrierFrequencyHz,
                            beatHz = uiState.synthesisParams.beatFrequencyHz,
                            targetBand = uiState.synthesisParams.targetBand,
                            mode = uiState.synthesisParams.acousticMode,
                            isPlaying = uiState.isPlaying,
                            isDimmed = false
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Target Entrainment Frequency Badge
                        val band = uiState.synthesisParams.targetBand
                        val beatHzStr = String.format("%.1f", uiState.synthesisParams.beatFrequencyHz)
                        val carrierHzStr = String.format("%.0f", uiState.synthesisParams.carrierFrequencyHz)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF1E293B))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "${band.name} $beatHzStr Hz",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = when (band) {
                                        BrainwaveBand.DELTA -> Color(0xFF818CF8)
                                        BrainwaveBand.THETA -> Color(0xFF38BDF8)
                                        BrainwaveBand.ALPHA -> Color(0xFFFBBF24)
                                        BrainwaveBand.BETA -> Color(0xFF34D399)
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "•  Carrier $carrierHzStr Hz",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF94A3B8)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Master Session Play / Stop Button
                        Button(
                            onClick = { viewModel.togglePlayback() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("master_playback_button"),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.isPlaying) Color(0xFFEF4444) else Color(0xFF2563EB),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = if (uiState.isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = if (uiState.isPlaying) "Stop Sleep Session" else "Start Sleep Session",
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (uiState.isPlaying) {
                                    val mins = uiState.sessionElapsedSeconds / 60
                                    val secs = uiState.sessionElapsedSeconds % 60
                                    String.format("STOP SLEEP AID (%02d:%02d)", mins, secs)
                                } else {
                                    "START ADAPTIVE SLEEP AID"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Sleep Timer Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "Sleep Timer",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Auto-Shutoff Timer:",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                0 to "Continuous",
                                15 to "15m",
                                30 to "30m",
                                45 to "45m",
                                60 to "60m"
                            ).forEach { (mins, label) ->
                                androidx.compose.material3.FilterChip(
                                    selected = uiState.sleepTimerMinutes == mins,
                                    onClick = { viewModel.setSleepTimer(mins) },
                                    label = { Text(label, fontSize = 11.sp) },
                                    colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF38BDF8),
                                        selectedLabelColor = Color(0xFF0F172A)
                                    ),
                                    modifier = Modifier.weight(1f).testTag("timer_chip_$mins")
                                )
                            }
                        }
                    }
                }
            }

            // 3. Current Sleep Stage Card
            item {
                SleepStageCard(
                    vitals = uiState.vitals,
                    targetBand = uiState.synthesisParams.targetBand
                )
            }

            // 4. Physiological Vitals (Heart Rate & HRV)
            item {
                VitalsCard(vitals = uiState.vitals)
            }

            // 5. Transducer & Output Routing Card
            item {
                TransducerCard(
                    activeTransducer = uiState.activeTransducer,
                    deviceDescription = uiState.activeDeviceDescription,
                    activeMode = uiState.synthesisParams.acousticMode,
                    manualOverride = uiState.manualModeOverride,
                    onSelectMode = { viewModel.setAcousticModeOverride(it) }
                )
            }

            // 6. Smart Alarm Card
            item {
                SmartAlarmCard(
                    alarmConfig = uiState.smartAlarmConfig,
                    onToggle = { isEnabled ->
                        viewModel.updateSmartAlarmConfig(
                            enabled = isEnabled,
                            startHour = uiState.smartAlarmConfig.windowStartHour,
                            startMinute = uiState.smartAlarmConfig.windowStartMinute,
                            endHour = uiState.smartAlarmConfig.windowEndHour,
                            endMinute = uiState.smartAlarmConfig.windowEndMinute
                        )
                    },
                    onEditWindow = { showAlarmWindowDialog = true },
                    onTestAlarm = { viewModel.testAlarmNow() }
                )
            }

            // 7. Audio Tuning & Noise Masking
            item {
                AudioControlsCard(
                    carrierHz = uiState.baseCarrierHz,
                    beatHz = uiState.synthesisParams.beatFrequencyHz,
                    noiseType = uiState.noiseType,
                    noiseVolume = uiState.noiseVolume,
                    noiseFilterCutoffHz = uiState.noiseFilterCutoffHz,
                    masterVolume = uiState.masterVolume,
                    onCarrierChanged = { viewModel.setBaseCarrier(it) },
                    onNoiseTypeChanged = { viewModel.setNoiseType(it) },
                    onNoiseVolumeChanged = { viewModel.setNoiseVolume(it) },
                    onNoiseCutoffChanged = { viewModel.setNoiseFilterCutoff(it) },
                    onMasterVolumeChanged = { viewModel.setMasterVolume(it) }
                )
            }

            // 8. Interactive Testing & Simulation Panel
            item {
                SimulationPanel(
                    isSimulationMode = uiState.isSimulationMode,
                    isHealthConnectAvailable = uiState.isHealthConnectAvailable,
                    onToggleSimulation = { viewModel.setSimulationMode(it) },
                    onSimulateStage = { viewModel.simulateStage(it) },
                    onSimulateSpike = { viewModel.simulateSpike() }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Smart Alarm Trigger Modal (Gentle Wake-Up)
    if (uiState.smartAlarmConfig.isAlarmRinging) {
        SmartAlarmDialog(
            alarmConfig = uiState.smartAlarmConfig,
            onDismiss = { viewModel.dismissAlarm() },
            onSnooze = { viewModel.snoozeAlarm(9) }
        )
    }

    // Alarm Window Configuration Dialog
    if (showAlarmWindowDialog) {
        AlarmWindowDialog(
            initialStartHour = uiState.smartAlarmConfig.windowStartHour,
            initialStartMinute = uiState.smartAlarmConfig.windowStartMinute,
            initialEndHour = uiState.smartAlarmConfig.windowEndHour,
            initialEndMinute = uiState.smartAlarmConfig.windowEndMinute,
            onConfirm = { sH, sM, eH, eM ->
                viewModel.updateSmartAlarmConfig(
                    enabled = true,
                    startHour = sH,
                    startMinute = sM,
                    endHour = eH,
                    endMinute = eM
                )
                showAlarmWindowDialog = false
            },
            onDismiss = { showAlarmWindowDialog = false }
        )
    }

    // Sleep Session Summary Insights Dialog
    uiState.sessionSummary?.let { summary ->
        com.example.adaptivesleep.ui.components.SleepSummaryDialog(
            summary = summary,
            onDismiss = { viewModel.dismissSummary() }
        )
    }
}
