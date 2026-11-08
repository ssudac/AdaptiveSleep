package com.example.adaptivesleep.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.window.Dialog

data class SleepSessionSummary(
    val durationSeconds: Long,
    val avgHeartRate: Int,
    val minHeartRate: Int,
    val avgHrvRmssd: Double,
    val deepSleepPercent: Int,
    val lightSleepPercent: Int,
    val remSleepPercent: Int,
    val wakeUpTrigger: String
)

@Composable
fun SleepSummaryDialog(
    summary: SleepSessionSummary,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F172A),
                            Color(0xFF08090C)
                        )
                    )
                )
                .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                .padding(22.dp)
                .testTag("sleep_summary_dialog")
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(Color(0xFF38BDF8).copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Sleep Summary",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Sleep Session Insights",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF8FAFC)
                    )
                )

                val hours = summary.durationSeconds / 3600
                val mins = (summary.durationSeconds % 3600) / 60
                val secs = summary.durationSeconds % 60
                val durationStr = if (hours > 0) "${hours}h ${mins}m" else "${mins}m ${secs}s"

                Text(
                    text = "Total Sleep Entrainment: $durationStr",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF94A3B8))
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Vitals Row Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131826))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("AVG HR", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B)))
                            Text("${summary.avgHeartRate} BPM", style = MaterialTheme.typography.titleMedium.copy(color = Color(0xFFF1F5F9), fontWeight = FontWeight.Bold))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("RESTING MIN", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B)))
                            Text("${summary.minHeartRate} BPM", style = MaterialTheme.typography.titleMedium.copy(color = Color(0xFF34D399), fontWeight = FontWeight.Bold))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("AVG HRV", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B)))
                            Text("${summary.avgHrvRmssd.toInt()} ms", style = MaterialTheme.typography.titleMedium.copy(color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Sleep Stage Distribution Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131826))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Sleep Stage Architecture",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFCBD5E1),
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Deep sleep bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Deep Sleep (Slow-Wave)", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF818CF8)))
                            Text("${summary.deepSleepPercent}%", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFF8FAFC), fontWeight = FontWeight.Bold))
                        }
                        LinearProgressIndicator(
                            progress = { summary.deepSleepPercent / 100f },
                            color = Color(0xFF818CF8),
                            trackColor = Color(0xFF1E293B),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Light sleep bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Light Sleep (Settling)", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF38BDF8)))
                            Text("${summary.lightSleepPercent}%", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFF8FAFC), fontWeight = FontWeight.Bold))
                        }
                        LinearProgressIndicator(
                            progress = { summary.lightSleepPercent / 100f },
                            color = Color(0xFF38BDF8),
                            trackColor = Color(0xFF1E293B),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // REM sleep bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("REM Dream State", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF34D399)))
                            Text("${summary.remSleepPercent}%", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFF8FAFC), fontWeight = FontWeight.Bold))
                        }
                        LinearProgressIndicator(
                            progress = { summary.remSleepPercent / 100f },
                            color = Color(0xFF34D399),
                            trackColor = Color(0xFF1E293B),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }
                }

                if (summary.wakeUpTrigger.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Alarm: ${summary.wakeUpTrigger}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFF59E0B),
                            textAlign = TextAlign.Center
                        )
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("dismiss_summary_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Complete Session", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
