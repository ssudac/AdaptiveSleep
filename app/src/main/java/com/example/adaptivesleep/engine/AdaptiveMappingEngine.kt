package com.example.adaptivesleep.engine

import com.example.adaptivesleep.data.model.AcousticMode
import com.example.adaptivesleep.data.model.AudioSynthesisParams
import com.example.adaptivesleep.data.model.BrainwaveBand
import com.example.adaptivesleep.data.model.NoiseType
import com.example.adaptivesleep.data.model.PhysiologicalState
import com.example.adaptivesleep.data.model.SleepStage
import com.example.adaptivesleep.data.model.SmartAlarmConfig
import java.time.LocalTime

/**
 * Dynamic Rule Evaluator and Mapping Engine.
 * Translates physiological vitals (HR, HRV, sleep stage, HR delta) and smart alarm criteria
 * into real-time audio synthesis parameters.
 */
class AdaptiveMappingEngine {

    /**
     * Computes the target audio synthesis parameters based on live physiological metrics,
     * user configuration, and smart alarm status.
     */
    fun evaluate(
        vitals: PhysiologicalState,
        baseCarrierHz: Float,
        activeAcousticMode: AcousticMode,
        noiseType: NoiseType,
        noiseVolume: Float,
        noiseFilterCutoff: Float,
        masterVolume: Float,
        isPlaying: Boolean,
        smartAlarmConfig: SmartAlarmConfig,
        currentTime: LocalTime = LocalTime.now()
    ): Pair<AudioSynthesisParams, SmartAlarmConfig> {

        var updatedAlarm = smartAlarmConfig

        // 1. Evaluate Smart Alarm Logic
        if (smartAlarmConfig.isEnabled && !smartAlarmConfig.isAlarmRinging) {
            val inWindow = smartAlarmConfig.isWithinWindow(currentTime)
            val pastEnd = smartAlarmConfig.isPastWindowEnd(currentTime)

            if (inWindow) {
                // Check if user transitioned into Light Sleep or Awake within the window
                if (vitals.sleepStage == SleepStage.LIGHT || vitals.sleepStage == SleepStage.AWAKE) {
                    updatedAlarm = smartAlarmConfig.copy(
                        isAlarmRinging = true,
                        alarmTriggerReason = "Gentle wake-up triggered: Detected ${vitals.sleepStage.displayName} phase inside window."
                    )
                }
            } else if (pastEnd) {
                // Window expired: trigger deadline alarm so the user never oversleeps
                updatedAlarm = smartAlarmConfig.copy(
                    isAlarmRinging = true,
                    alarmTriggerReason = "Wake-up window deadline reached."
                )
            }
        }

        // If Smart Alarm is actively ringing, transition brainwaves to Beta to naturally dispel sleep inertia!
        if (updatedAlarm.isAlarmRinging) {
            val alarmParams = AudioSynthesisParams(
                carrierFrequencyHz = 220.0f,
                beatFrequencyHz = 14.5f, // Beta wave for alert wakefulness
                acousticMode = activeAcousticMode,
                targetBand = BrainwaveBand.BETA,
                noiseType = NoiseType.OFF,
                noiseVolume = 0.0f,
                noiseFilterCutoffHz = noiseFilterCutoff,
                masterVolume = masterVolume,
                isPlaying = true
            )
            return Pair(alarmParams, updatedAlarm)
        }

        // 2. Normal Adaptive Sleep Entrainment Calculation
        val deltaHr = vitals.deltaHr

        // Base Target Frequencies per stage
        val (targetBand, nominalBeatHz, carrierOffset) = when (vitals.sleepStage) {
            SleepStage.AWAKE -> {
                // Awake / High autonomic arousal: 8–12 Hz Alpha waves
                val alphaBeat = when {
                    deltaHr > 15 -> 11.5f // Stress spike: start at higher alpha
                    deltaHr > 5 -> 10.0f
                    else -> 8.5f
                }
                Triple(BrainwaveBand.ALPHA, alphaBeat, 15.0f)
            }
            SleepStage.LIGHT -> {
                // Light sleep / Settling: 4–7 Hz Theta waves
                val thetaBeat = when {
                    deltaHr > 6 -> 6.5f // HR slightly elevated: gentle glide from upper theta
                    deltaHr < -2 -> 4.5f
                    else -> 5.5f
                }
                Triple(BrainwaveBand.THETA, thetaBeat, 0.0f)
            }
            SleepStage.DEEP -> {
                // Deep restorative slow-wave sleep: 0.5–3.5 Hz Delta waves
                // Decreasing HR with deepening sleep progressively lowers beat deeper into Delta (1.5 Hz -> 0.8 Hz)
                val deltaBeat = when {
                    deltaHr <= -10 -> 0.8f // Profound slow wave
                    deltaHr <= -5 -> 1.2f
                    deltaHr <= 0 -> 1.8f
                    else -> 2.5f
                }
                Triple(BrainwaveBand.DELTA, deltaBeat, -25.0f)
            }
            SleepStage.REM -> {
                // REM Sleep: Low Theta / upper Delta (3.5–5.5 Hz) stabilization
                val remBeat = if (deltaHr > 4) 4.8f else 3.8f
                Triple(BrainwaveBand.THETA, remBeat, -10.0f)
            }
            SleepStage.UNKNOWN -> {
                Triple(BrainwaveBand.THETA, 5.0f, 0.0f)
            }
        }

        // Apply carrier offset for brain comfort (e.g. lower pitch during deep delta sleep)
        val finalCarrier = (baseCarrierHz + carrierOffset).coerceIn(100.0f, 260.0f)

        // Dynamic noise modulation: slightly raise warm masking noise during high HR or awake state
        val dynamicNoiseVol = if (vitals.sleepStage == SleepStage.AWAKE && deltaHr > 8) {
            (noiseVolume * 1.2f).coerceAtMost(0.8f)
        } else {
            noiseVolume
        }

        val params = AudioSynthesisParams(
            carrierFrequencyHz = finalCarrier,
            beatFrequencyHz = nominalBeatHz,
            acousticMode = activeAcousticMode,
            targetBand = targetBand,
            noiseType = noiseType,
            noiseVolume = dynamicNoiseVol,
            noiseFilterCutoffHz = noiseFilterCutoff,
            masterVolume = masterVolume,
            isPlaying = isPlaying
        )

        return Pair(params, updatedAlarm)
    }
}
