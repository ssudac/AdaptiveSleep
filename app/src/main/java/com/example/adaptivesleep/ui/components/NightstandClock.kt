package com.example.adaptivesleep.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun NightstandClock(
    isDimmed: Boolean,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf(LocalTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalTime.now()
            delay(1000L)
        }
    }

    val timeString = currentTime.format(DateTimeFormatter.ofPattern("hh:mm"))
    val amPmString = currentTime.format(DateTimeFormatter.ofPattern("a"))
    val secondsString = currentTime.format(DateTimeFormatter.ofPattern("ss"))

    val textColor = if (isDimmed) Color(0xFF64748B) else Color(0xFFF8FAFC)
    val accentColor = if (isDimmed) Color(0xFF475569) else Color(0xFF38BDF8)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.testTag("nightstand_clock")
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = timeString,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    letterSpacing = (-1).sp
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = amPmString,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                )
                Text(
                    text = ":$secondsString",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF64748B)
                    )
                )
            }
        }
    }
}
