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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun AlarmWindowDialog(
    initialStartHour: Int,
    initialStartMinute: Int,
    initialEndHour: Int,
    initialEndMinute: Int,
    onConfirm: (startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var startHour by remember { mutableStateOf(initialStartHour) }
    var startMinute by remember { mutableStateOf(initialStartMinute) }
    var windowSpanMinutes by remember {
        val startMins = initialStartHour * 60 + initialStartMinute
        val endMins = initialEndHour * 60 + initialEndMinute
        val span = if (endMins >= startMins) endMins - startMins else (1440 - startMins + endMins)
        mutableStateOf(span.coerceIn(15, 60))
    }

    val computedEnd = remember(startHour, startMinute, windowSpanMinutes) {
        LocalTime.of(startHour, startMinute).plusMinutes(windowSpanMinutes.toLong())
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF131826))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(24.dp))
                .padding(22.dp)
                .testTag("alarm_window_dialog")
        ) {
            Column {
                Text(
                    text = "Configure Wake-Up Window",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color(0xFFF8FAFC),
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "The system detects when your body enters Light Sleep within this window to wake you with minimal sleep inertia.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF94A3B8),
                        lineHeight = 18.sp
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Start Time Display
                val startTimeStr = LocalTime.of(startHour, startMinute).format(DateTimeFormatter.ofPattern("hh:mm a"))
                val endTimeStr = computedEnd.format(DateTimeFormatter.ofPattern("hh:mm a"))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0F172A))
                        .padding(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Target Window: $startTimeStr – $endTimeStr",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color(0xFFF59E0B),
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "$windowSpanMinutes-minute smart transition buffer",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Start Hour Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Window Start Hour", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFCBD5E1)))
                    Text(
                        String.format("%02d:00 (%s)", startHour, if (startHour < 12) "AM" else "PM"),
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                    )
                }
                Slider(
                    value = startHour.toFloat(),
                    onValueChange = { startHour = it.toInt() },
                    valueRange = 4f..11f,
                    steps = 6,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF38BDF8),
                        activeTrackColor = Color(0xFF38BDF8)
                    )
                )

                // Start Minute Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Start Minute", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFCBD5E1)))
                    Text(
                        String.format(":%02d", startMinute),
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                    )
                }
                Slider(
                    value = (startMinute / 15).toFloat(),
                    onValueChange = { startMinute = (it.toInt() * 15).coerceIn(0, 45) },
                    valueRange = 0f..3f,
                    steps = 2,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF38BDF8),
                        activeTrackColor = Color(0xFF38BDF8)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Window Span selector chips
                Text("Window Buffer Duration", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFCBD5E1)))
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(15, 20, 30, 45).forEach { span ->
                        FilterChip(
                            selected = windowSpanMinutes == span,
                            onClick = { windowSpanMinutes = span },
                            label = { Text("$span min") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFF59E0B),
                                selectedLabelColor = Color(0xFF0F172A)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                    Button(
                        onClick = {
                            onConfirm(startHour, startMinute, computedEnd.hour, computedEnd.minute)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF59E0B),
                            contentColor = Color(0xFF0F172A)
                        )
                    ) {
                        Text("Save Window", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
