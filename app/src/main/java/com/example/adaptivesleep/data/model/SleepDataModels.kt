package com.example.adaptivesleep.data.model

import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Standard sleep stages aligned with Health Connect SleepSessionRecord stages.
 */
enum class SleepStage(val displayName: String, val description: String) {
    AWAKE("Awake", "Conscious resting or autonomic arousal"),
    LIGHT("Light Sleep", "N1/N2 transitional settling sleep"),
    DEEP("Deep Sleep", "N3 restorative slow-wave delta sleep"),
    REM("REM Sleep", "Rapid eye movement dream state"),
    UNKNOWN("Resting", "Calibrating physiological sensors")
}

/**
 * Brainwave entrainment frequency bands.
 */
enum class BrainwaveBand(
    val displayName: String,
    val frequencyRange: ClosedFloatingPointRange<Float>,
    val stateDescription: String
) {
    DELTA("Delta (0.5 – 3.5 Hz)", 0.5f..3.5f, "Deep restorative sleep & cellular recovery"),
    THETA("Theta (4.0 – 7.0 Hz)", 4.0f..7.0f, "Drowsiness, meditation & sleep onset"),
    ALPHA("Alpha (8.0 – 12.0 Hz)", 8.0f..12.0f, "Calm alertness & stress mitigation"),
    BETA("Beta (13.0 – 20.0 Hz)", 13.0f..20.0f, "Gentle wakefulness & natural morning arousal")
}

/**
 * Output acoustic delivery method based on detected transducer.
 */
enum class AcousticMode(val displayName: String, val description: String) {
    BINAURAL("Binaural Beats", "Independent stereo channels for isolated headphones/earbuds"),
    ISOCHRONIC("Isochronic Tones", "Amplitude-pulsed pure tones for open room speakers"),
    MONAURAL("Monaural Beats", "Mathematically mixed acoustic beats for open speakers")
}

/**
 * Physical audio transducer detected by AudioManager.
 */
enum class TransducerType(val displayName: String, val isIsolatedStereo: Boolean) {
    HEADPHONES("Headphones / Earbuds", true),
    OPEN_SPEAKERS("Device / Room Speakers", false),
    UNKNOWN("Default Audio Device", false)
}

/**
 * Background soothing ambient sound generator.
 */
enum class NoiseType(val displayName: String) {
    OFF("None"),
    PINK("Pink Noise (Warm 1/f)"),
    BROWN("Brownian Noise (Deep 1/f²)")
}

/**
 * Current physiological snapshot collected from sensors or Health Connect.
 */
data class PhysiologicalState(
    val heartRateBpm: Int = 68,
    val hrvRmssdMillis: Double = 52.0,
    val sleepStage: SleepStage = SleepStage.LIGHT,
    val baselineRestingHr: Int = 60,
    val timestampMillis: Long = System.currentTimeMillis(),
    val isLiveHealthConnect: Boolean = false
) {
    val deltaHr: Int get() = heartRateBpm - baselineRestingHr
}

/**
 * Dynamic parameters calculated by the Mapping Engine and passed to the DSP Synthesizer.
 */
data class AudioSynthesisParams(
    val carrierFrequencyHz: Float = 160.0f,
    val beatFrequencyHz: Float = 2.0f,
    val acousticMode: AcousticMode = AcousticMode.BINAURAL,
    val targetBand: BrainwaveBand = BrainwaveBand.DELTA,
    val noiseType: NoiseType = NoiseType.PINK,
    val noiseVolume: Float = 0.25f,
    val noiseFilterCutoffHz: Float = 1200.0f,
    val masterVolume: Float = 0.8f,
    val isPlaying: Boolean = false
)

/**
 * Configuration and state for the Smart Alarm.
 */
data class SmartAlarmConfig(
    val isEnabled: Boolean = false,
    val windowStartHour: Int = 7,
    val windowStartMinute: Int = 0,
    val windowEndHour: Int = 7,
    val windowEndMinute: Int = 30,
    val isAlarmRinging: Boolean = false,
    val alarmTriggerReason: String = ""
) {
    fun formatWindow(): String {
        val start = LocalTime.of(windowStartHour, windowStartMinute).format(DateTimeFormatter.ofPattern("hh:mm a"))
        val end = LocalTime.of(windowEndHour, windowEndMinute).format(DateTimeFormatter.ofPattern("hh:mm a"))
        return "$start – $end"
    }

    fun isWithinWindow(currentTime: LocalTime): Boolean {
        val start = LocalTime.of(windowStartHour, windowStartMinute)
        val end = LocalTime.of(windowEndHour, windowEndMinute)
        return if (start.isBefore(end)) {
            !currentTime.isBefore(start) && !currentTime.isAfter(end)
        } else {
            // Handles overnight alarm window (e.g., 23:45 to 00:15)
            !currentTime.isBefore(start) || !currentTime.isAfter(end)
        }
    }

    fun isPastWindowEnd(currentTime: LocalTime): Boolean {
        val end = LocalTime.of(windowEndHour, windowEndMinute)
        return currentTime.isAfter(end)
    }
}
