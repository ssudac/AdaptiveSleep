package com.example

import com.example.adaptivesleep.data.model.AcousticMode
import com.example.adaptivesleep.data.model.BrainwaveBand
import com.example.adaptivesleep.data.model.NoiseType
import com.example.adaptivesleep.data.model.PhysiologicalState
import com.example.adaptivesleep.data.model.SleepStage
import com.example.adaptivesleep.data.model.SmartAlarmConfig
import com.example.adaptivesleep.engine.AdaptiveMappingEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class AdaptiveSleepEngineTest {

    private val engine = AdaptiveMappingEngine()

    @Test
    fun `deep sleep with low heart rate produces delta band slow-wave`() {
        val vitals = PhysiologicalState(
            heartRateBpm = 50,
            hrvRmssdMillis = 75.0,
            sleepStage = SleepStage.DEEP,
            baselineRestingHr = 60
        )

        val (params, alarm) = engine.evaluate(
            vitals = vitals,
            baseCarrierHz = 160f,
            activeAcousticMode = AcousticMode.BINAURAL,
            noiseType = NoiseType.PINK,
            noiseVolume = 0.2f,
            noiseFilterCutoff = 1200f,
            masterVolume = 0.8f,
            isPlaying = true,
            smartAlarmConfig = SmartAlarmConfig(isEnabled = false),
            currentTime = LocalTime.of(3, 0)
        )

        assertEquals(BrainwaveBand.DELTA, params.targetBand)
        assertTrue("Delta beat frequency should be <= 1.5 Hz", params.beatFrequencyHz <= 1.5f)
        assertFalse("Alarm should not be ringing", alarm.isAlarmRinging)
    }

    @Test
    fun `light sleep produces theta band settling wave`() {
        val vitals = PhysiologicalState(
            heartRateBpm = 64,
            hrvRmssdMillis = 52.0,
            sleepStage = SleepStage.LIGHT,
            baselineRestingHr = 60
        )

        val (params, _) = engine.evaluate(
            vitals = vitals,
            baseCarrierHz = 160f,
            activeAcousticMode = AcousticMode.ISOCHRONIC,
            noiseType = NoiseType.BROWN,
            noiseVolume = 0.3f,
            noiseFilterCutoff = 1000f,
            masterVolume = 0.8f,
            isPlaying = true,
            smartAlarmConfig = SmartAlarmConfig(isEnabled = false),
            currentTime = LocalTime.of(4, 0)
        )

        assertEquals(BrainwaveBand.THETA, params.targetBand)
        assertTrue("Theta beat should be in 4-7 Hz range", params.beatFrequencyHz in 4.0f..7.0f)
    }

    @Test
    fun `awake state with elevated heart rate targets alpha band for autonomic calming`() {
        val vitals = PhysiologicalState(
            heartRateBpm = 82,
            hrvRmssdMillis = 30.0,
            sleepStage = SleepStage.AWAKE,
            baselineRestingHr = 60
        )

        val (params, _) = engine.evaluate(
            vitals = vitals,
            baseCarrierHz = 160f,
            activeAcousticMode = AcousticMode.BINAURAL,
            noiseType = NoiseType.PINK,
            noiseVolume = 0.25f,
            noiseFilterCutoff = 1200f,
            masterVolume = 0.8f,
            isPlaying = true,
            smartAlarmConfig = SmartAlarmConfig(isEnabled = false),
            currentTime = LocalTime.of(23, 30)
        )

        assertEquals(BrainwaveBand.ALPHA, params.targetBand)
        assertTrue("Alpha beat should be in 8-12 Hz range", params.beatFrequencyHz in 8.0f..12.0f)
    }

    @Test
    fun `smart alarm triggers when light sleep detected inside wake-up window`() {
        val vitals = PhysiologicalState(
            heartRateBpm = 66,
            hrvRmssdMillis = 50.0,
            sleepStage = SleepStage.LIGHT, // Light Sleep detected inside window
            baselineRestingHr = 60
        )

        val alarmConfig = SmartAlarmConfig(
            isEnabled = true,
            windowStartHour = 7,
            windowStartMinute = 0,
            windowEndHour = 7,
            windowEndMinute = 30,
            isAlarmRinging = false
        )

        val (params, updatedAlarm) = engine.evaluate(
            vitals = vitals,
            baseCarrierHz = 160f,
            activeAcousticMode = AcousticMode.BINAURAL,
            noiseType = NoiseType.OFF,
            noiseVolume = 0f,
            noiseFilterCutoff = 1200f,
            masterVolume = 0.8f,
            isPlaying = true,
            smartAlarmConfig = alarmConfig,
            currentTime = LocalTime.of(7, 12) // Inside window 7:00 - 7:30
        )

        assertTrue("Smart alarm should trigger upon light sleep detection", updatedAlarm.isAlarmRinging)
        assertEquals(BrainwaveBand.BETA, params.targetBand) // Switches to Beta for gentle wakefulness
        assertTrue(updatedAlarm.alarmTriggerReason.contains("Light Sleep"))
    }

    @Test
    fun `smart alarm does not trigger before window starts`() {
        val vitals = PhysiologicalState(
            heartRateBpm = 65,
            hrvRmssdMillis = 50.0,
            sleepStage = SleepStage.LIGHT,
            baselineRestingHr = 60
        )

        val alarmConfig = SmartAlarmConfig(
            isEnabled = true,
            windowStartHour = 7,
            windowStartMinute = 0,
            windowEndHour = 7,
            windowEndMinute = 30,
            isAlarmRinging = false
        )

        val (_, updatedAlarm) = engine.evaluate(
            vitals = vitals,
            baseCarrierHz = 160f,
            activeAcousticMode = AcousticMode.BINAURAL,
            noiseType = NoiseType.OFF,
            noiseVolume = 0f,
            noiseFilterCutoff = 1200f,
            masterVolume = 0.8f,
            isPlaying = true,
            smartAlarmConfig = alarmConfig,
            currentTime = LocalTime.of(6, 45) // Before window
        )

        assertFalse("Alarm should not trigger before window start", updatedAlarm.isAlarmRinging)
    }

    @Test
    fun `smart alarm triggers at window end deadline even if still in deep sleep`() {
        val vitals = PhysiologicalState(
            heartRateBpm = 52,
            hrvRmssdMillis = 70.0,
            sleepStage = SleepStage.DEEP, // Still in deep sleep at deadline
            baselineRestingHr = 60
        )

        val alarmConfig = SmartAlarmConfig(
            isEnabled = true,
            windowStartHour = 7,
            windowStartMinute = 0,
            windowEndHour = 7,
            windowEndMinute = 30,
            isAlarmRinging = false
        )

        val (_, updatedAlarm) = engine.evaluate(
            vitals = vitals,
            baseCarrierHz = 160f,
            activeAcousticMode = AcousticMode.BINAURAL,
            noiseType = NoiseType.OFF,
            noiseVolume = 0f,
            noiseFilterCutoff = 1200f,
            masterVolume = 0.8f,
            isPlaying = true,
            smartAlarmConfig = alarmConfig,
            currentTime = LocalTime.of(7, 31) // Past window deadline
        )

        assertTrue("Alarm should trigger as failsafe when deadline is reached", updatedAlarm.isAlarmRinging)
        assertTrue(updatedAlarm.alarmTriggerReason.contains("deadline reached"))
    }
}
