package com.example.adaptivesleep.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.adaptivesleep.data.model.AcousticMode
import com.example.adaptivesleep.data.model.BrainwaveBand
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Real-time canvas visualizer rendering mathematically accurate acoustic wave envelopes,
 * carrier oscillations, and soothing ambient resonance synchronized to beat frequency.
 */
@Composable
fun WaveformVisualizer(
    carrierHz: Float,
    beatHz: Float,
    targetBand: BrainwaveBand,
    mode: AcousticMode,
    isPlaying: Boolean,
    isDimmed: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "WaveformAnimation")

    // Continuous progress phase for wave translation
    val animPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase"
    )

    // Pulse animation synchronized with beat frequency (clamped between 0.5s and 2.5s)
    val pulseDuration = (1000f / beatHz.coerceIn(0.5f, 15f)).toInt().coerceIn(300, 2500)
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = pulseDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    val primaryColor = when (targetBand) {
        BrainwaveBand.DELTA -> Color(0xFF818CF8) // Indigo Delta
        BrainwaveBand.THETA -> Color(0xFF38BDF8) // Cyan Theta
        BrainwaveBand.ALPHA -> Color(0xFFFBBF24) // Amber Alpha
        BrainwaveBand.BETA -> Color(0xFF34D399)  // Emerald Beta
    }

    val secondaryColor = when (targetBand) {
        BrainwaveBand.DELTA -> Color(0xFFC084FC) // Soft Purple
        BrainwaveBand.THETA -> Color(0xFF2DD4BF) // Teal
        BrainwaveBand.ALPHA -> Color(0xFFF97316) // Orange
        BrainwaveBand.BETA -> Color(0xFF60A5FA)  // Sky
    }

    val glowAlphaMultiplier = if (isDimmed) 0.3f else 1.0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(170.dp)
            .testTag("waveform_visualizer")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f

            if (!isPlaying) {
                // Resting idle ambient flat line with gentle breathing shimmer
                val restingPath = Path()
                restingPath.moveTo(0f, centerY)
                for (x in 0..width.toInt() step 8) {
                    val y = centerY + sin((x / width * 4 * PI + animPhase).toDouble()).toFloat() * 3f
                    restingPath.lineTo(x.toFloat(), y)
                }
                drawPath(
                    path = restingPath,
                    color = primaryColor.copy(alpha = 0.25f * glowAlphaMultiplier),
                    style = Stroke(width = 2.dp.toPx())
                )
                return@Canvas
            }

            // Draw background ambient resonance glow ellipse
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = (0.22f * pulseAlpha) * glowAlphaMultiplier),
                        Color.Transparent
                    ),
                    center = Offset(width / 2f, centerY),
                    radius = width * 0.45f
                )
            )

            val wavePath1 = Path()
            val wavePath2 = Path()

            val steps = width.toInt()
            val carrierCycles = 5.0f // visual carrier cycles across the display
            val beatCycles = (beatHz * 0.6f).coerceIn(0.8f, 3.5f) // visual envelope cycles

            for (x in 0..steps step 4) {
                val progress = x / width

                val carrierWave = sin((progress * carrierCycles * 2 * PI + animPhase * 2).toDouble()).toFloat()

                val (envelope1, envelope2) = when (mode) {
                    AcousticMode.BINAURAL -> {
                        // Left & Right separate phase envelopes
                        val envL = sin((progress * beatCycles * 2 * PI + animPhase).toDouble()).toFloat() * 0.5f + 0.5f
                        val envR = sin((progress * beatCycles * 2 * PI + animPhase + PI * 0.5).toDouble()).toFloat() * 0.5f + 0.5f
                        Pair(envL, envR)
                    }
                    AcousticMode.ISOCHRONIC -> {
                        // Pulsed raised-cosine amplitude packet
                        val mod = (0.5f + 0.5f * cos((progress * beatCycles * 2 * PI + animPhase).toDouble()).toFloat()).pow(1.8f)
                        Pair(mod, mod)
                    }
                    AcousticMode.MONAURAL -> {
                        // Beating interference envelope
                        val monEnv = cos((progress * beatCycles * PI + animPhase).toDouble()).toFloat()
                        Pair(monEnv, monEnv)
                    }
                }

                val amp = (height * 0.35f) * (if (isDimmed) 0.6f else 1.0f)
                val y1 = centerY + (carrierWave * envelope1 * amp)
                val y2 = centerY - (carrierWave * envelope2 * amp * 0.85f)

                if (x == 0) {
                    wavePath1.moveTo(x.toFloat(), y1)
                    wavePath2.moveTo(x.toFloat(), y2)
                } else {
                    wavePath1.lineTo(x.toFloat(), y1)
                    wavePath2.lineTo(x.toFloat(), y2)
                }
            }

            // Draw primary acoustic stream
            drawPath(
                path = wavePath1,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.3f * glowAlphaMultiplier),
                        primaryColor.copy(alpha = 0.95f * glowAlphaMultiplier),
                        secondaryColor.copy(alpha = 0.95f * glowAlphaMultiplier),
                        secondaryColor.copy(alpha = 0.3f * glowAlphaMultiplier)
                    )
                ),
                style = Stroke(width = 3.dp.toPx())
            )

            // Draw stereo complementary stream (for binaural/stereo depth)
            if (mode == AcousticMode.BINAURAL) {
                drawPath(
                    path = wavePath2,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            secondaryColor.copy(alpha = 0.2f * glowAlphaMultiplier),
                            secondaryColor.copy(alpha = 0.75f * glowAlphaMultiplier),
                            primaryColor.copy(alpha = 0.75f * glowAlphaMultiplier),
                            secondaryColor.copy(alpha = 0.2f * glowAlphaMultiplier)
                        )
                    ),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}
