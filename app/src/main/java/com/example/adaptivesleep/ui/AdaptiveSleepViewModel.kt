package com.example.adaptivesleep.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.adaptivesleep.audio.AudioOutputRouter
import com.example.adaptivesleep.audio.DspSynthesizer
import com.example.adaptivesleep.data.model.AcousticMode
import com.example.adaptivesleep.data.model.AudioSynthesisParams
import com.example.adaptivesleep.data.model.NoiseType
import com.example.adaptivesleep.data.model.PhysiologicalState
import com.example.adaptivesleep.data.model.SleepStage
import com.example.adaptivesleep.data.model.SmartAlarmConfig
import com.example.adaptivesleep.data.model.TransducerType
import com.example.adaptivesleep.engine.AdaptiveMappingEngine
import com.example.adaptivesleep.health.HealthConnectManager
import com.example.adaptivesleep.health.SensorRepository
import com.example.adaptivesleep.ui.components.SleepSessionSummary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalTime
import kotlin.random.Random

/**
 * UI State for the AdaptiveSleep Nightstand Experience.
 */
data class AdaptiveSleepUiState(
    val vitals: PhysiologicalState = PhysiologicalState(),
    val synthesisParams: AudioSynthesisParams = AudioSynthesisParams(),
    val activeTransducer: TransducerType = TransducerType.HEADPHONES,
    val activeDeviceDescription: String = "Detecting audio device...",
    val manualModeOverride: AcousticMode? = null,
    val isPlaying: Boolean = false,
    val baseCarrierHz: Float = 160.0f,
    val noiseType: NoiseType = NoiseType.PINK,
    val noiseVolume: Float = 0.25f,
    val noiseFilterCutoffHz: Float = 1200.0f,
    val masterVolume: Float = 0.85f,
    val smartAlarmConfig: SmartAlarmConfig = SmartAlarmConfig(),
    val isHealthConnectAvailable: Boolean = false,
    val hasHealthPermissions: Boolean = false,
    val isSimulationMode: Boolean = true,
    val sleepTimerMinutes: Int = 0, // 0 = continuous
    val sessionElapsedSeconds: Long = 0L,
    val isDimmedMode: Boolean = false,
    val sessionSummary: SleepSessionSummary? = null
)

class AdaptiveSleepViewModel(application: Application) : AndroidViewModel(application) {

    val healthConnectManager = HealthConnectManager(application)
    val sensorRepository = SensorRepository(application, healthConnectManager)
    val audioOutputRouter = AudioOutputRouter(application)
    val dspSynthesizer = DspSynthesizer()
    val mappingEngine = AdaptiveMappingEngine()

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val _uiState = MutableStateFlow(AdaptiveSleepUiState())
    val uiState: StateFlow<AdaptiveSleepUiState> = _uiState.asStateFlow()

    private var sessionTimerJob: Job? = null
    private var mappingLoopJob: Job? = null
    private var hapticPulseJob: Job? = null

    // Vitals history buffer during active session for generating summary
    private val sessionHrHistory = mutableListOf<Int>()
    private val sessionHrvHistory = mutableListOf<Double>()
    private val sessionStagesHistory = mutableListOf<SleepStage>()

    init {
        checkHealthConnectStatus()

        // Audio focus listeners
        audioOutputRouter.onAudioFocusLostListener = {
            if (_uiState.value.isPlaying) {
                // Pause on phone call / navigation interrupt
                dspSynthesizer.stop()
                _uiState.update { it.copy(isPlaying = false) }
            }
        }
        audioOutputRouter.onAudioFocusGainedListener = {
            // Restore playback
            if (_uiState.value.sessionElapsedSeconds > 0) {
                dspSynthesizer.start()
                _uiState.update { it.copy(isPlaying = true) }
            }
        }

        // Observe Transducer Route Changes
        viewModelScope.launch {
            audioOutputRouter.activeTransducer.collect { transducer ->
                _uiState.update { it.copy(activeTransducer = transducer) }
                recalculateAudioParams()
            }
        }

        viewModelScope.launch {
            audioOutputRouter.activeDeviceDescription.collect { desc ->
                _uiState.update { it.copy(activeDeviceDescription = desc) }
            }
        }

        // Observe Physiological State updates
        viewModelScope.launch {
            sensorRepository.currentState.collect { vitals ->
                _uiState.update { it.copy(vitals = vitals) }
                if (_uiState.value.isPlaying) {
                    sessionHrHistory.add(vitals.heartRateBpm)
                    sessionHrvHistory.add(vitals.hrvRmssdMillis)
                    sessionStagesHistory.add(vitals.sleepStage)
                }
                recalculateAudioParams()
            }
        }

        // Periodic Dynamic Mapping and Alarm check loop (runs every second)
        mappingLoopJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                recalculateAudioParams()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioOutputRouter.release()
        dspSynthesizer.stop()
        sessionTimerJob?.cancel()
        mappingLoopJob?.cancel()
        hapticPulseJob?.cancel()
        vibrator?.cancel()
    }

    fun checkHealthConnectStatus() {
        viewModelScope.launch {
            val available = healthConnectManager.isSdkAvailable()
            val permissions = if (available) healthConnectManager.hasAllPermissions() else false
            _uiState.update {
                it.copy(
                    isHealthConnectAvailable = available,
                    hasHealthPermissions = permissions,
                    isSimulationMode = !permissions // Fall back to simulation if no permissions
                )
            }
            sensorRepository.setSimulationMode(_uiState.value.isSimulationMode)
        }
    }

    fun togglePlayback() {
        val nextPlaying = !_uiState.value.isPlaying

        if (nextPlaying) {
            audioOutputRouter.requestAudioFocus()
            dspSynthesizer.start()
            startSessionTimer()
            sessionHrHistory.clear()
            sessionHrvHistory.clear()
            sessionStagesHistory.clear()
            _uiState.update { it.copy(isPlaying = true, sessionElapsedSeconds = 0L) }
        } else {
            val elapsed = _uiState.value.sessionElapsedSeconds
            audioOutputRouter.abandonAudioFocus()
            dspSynthesizer.stop()
            sessionTimerJob?.cancel()
            _uiState.update { it.copy(isPlaying = false) }

            // If session lasted more than 3 seconds, show summary insights
            if (elapsed >= 3) {
                generateSessionSummary("Manual Sleep Session Completion")
            }
        }
        recalculateAudioParams()
    }

    fun setBaseCarrier(carrierHz: Float) {
        _uiState.update { it.copy(baseCarrierHz = carrierHz) }
        recalculateAudioParams()
    }

    fun setNoiseType(type: NoiseType) {
        _uiState.update { it.copy(noiseType = type) }
        recalculateAudioParams()
    }

    fun setNoiseVolume(vol: Float) {
        _uiState.update { it.copy(noiseVolume = vol.coerceIn(0f, 1f)) }
        recalculateAudioParams()
    }

    fun setNoiseFilterCutoff(cutoffHz: Float) {
        _uiState.update { it.copy(noiseFilterCutoffHz = cutoffHz) }
        recalculateAudioParams()
    }

    fun setMasterVolume(vol: Float) {
        _uiState.update { it.copy(masterVolume = vol.coerceIn(0f, 1f)) }
        recalculateAudioParams()
    }

    fun setAcousticModeOverride(mode: AcousticMode?) {
        _uiState.update { it.copy(manualModeOverride = mode) }
        recalculateAudioParams()
    }

    fun setSleepTimer(minutes: Int) {
        _uiState.update { it.copy(sleepTimerMinutes = minutes) }
    }

    fun toggleDimmedMode() {
        _uiState.update { it.copy(isDimmedMode = !it.isDimmedMode) }
    }

    fun setSimulationMode(enabled: Boolean) {
        _uiState.update { it.copy(isSimulationMode = enabled) }
        sensorRepository.setSimulationMode(enabled)
    }

    fun simulateStage(stage: SleepStage) {
        sensorRepository.simulateStage(stage)
    }

    fun simulateSpike() {
        sensorRepository.simulateArousalSpike()
    }

    // --- Smart Alarm Management ---

    fun updateSmartAlarmConfig(
        enabled: Boolean,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int
    ) {
        _uiState.update {
            it.copy(
                smartAlarmConfig = it.smartAlarmConfig.copy(
                    isEnabled = enabled,
                    windowStartHour = startHour,
                    windowStartMinute = startMinute,
                    windowEndHour = endHour,
                    windowEndMinute = endMinute,
                    isAlarmRinging = false
                )
            )
        }
        recalculateAudioParams()
    }

    fun dismissAlarm() {
        stopHapticAlarm()
        val triggerReason = _uiState.value.smartAlarmConfig.alarmTriggerReason
        _uiState.update {
            it.copy(
                smartAlarmConfig = it.smartAlarmConfig.copy(
                    isAlarmRinging = false,
                    alarmTriggerReason = ""
                )
            )
        }
        dspSynthesizer.isAlarmChiming = false
        recalculateAudioParams()

        generateSessionSummary(triggerReason.ifBlank { "Gentle Smart Alarm Wake-Up" })
    }

    fun snoozeAlarm(minutes: Int = 9) {
        stopHapticAlarm()
        val now = LocalTime.now().plusMinutes(minutes.toLong())
        _uiState.update {
            it.copy(
                smartAlarmConfig = it.smartAlarmConfig.copy(
                    isEnabled = true,
                    windowStartHour = now.hour,
                    windowStartMinute = now.minute,
                    windowEndHour = now.plusMinutes(5).hour,
                    windowEndMinute = now.plusMinutes(5).minute,
                    isAlarmRinging = false,
                    alarmTriggerReason = ""
                )
            )
        }
        dspSynthesizer.isAlarmChiming = false
        recalculateAudioParams()
    }

    fun testAlarmNow() {
        _uiState.update {
            it.copy(
                smartAlarmConfig = it.smartAlarmConfig.copy(
                    isEnabled = true,
                    isAlarmRinging = true,
                    alarmTriggerReason = "Test Demonstration: Light sleep phase simulation triggered gentle morning awakening."
                )
            )
        }
        if (!_uiState.value.isPlaying) {
            audioOutputRouter.requestAudioFocus()
            dspSynthesizer.start()
            _uiState.update { it.copy(isPlaying = true) }
        }
        startHapticAlarm()
        recalculateAudioParams()
    }

    fun dismissSummary() {
        _uiState.update { it.copy(sessionSummary = null) }
    }

    private fun startHapticAlarm() {
        hapticPulseJob?.cancel()
        hapticPulseJob = viewModelScope.launch {
            while (isActive && _uiState.value.smartAlarmConfig.isAlarmRinging) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(200)
                    }
                } catch (e: Exception) {
                    // Ignore vibration errors on devices without vibrator motor
                }
                delay(1200L)
            }
        }
    }

    private fun stopHapticAlarm() {
        hapticPulseJob?.cancel()
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            // Safe ignore
        }
    }

    private fun generateSessionSummary(wakeReason: String) {
        val elapsed = _uiState.value.sessionElapsedSeconds
        val avgHr = if (sessionHrHistory.isNotEmpty()) sessionHrHistory.average().toInt() else _uiState.value.vitals.heartRateBpm
        val minHr = if (sessionHrHistory.isNotEmpty()) sessionHrHistory.minOrNull() ?: (_uiState.value.vitals.baselineRestingHr - 6) else 54
        val avgHrv = if (sessionHrvHistory.isNotEmpty()) sessionHrvHistory.average() else _uiState.value.vitals.hrvRmssdMillis

        val totalStages = sessionStagesHistory.size.coerceAtLeast(1)
        val deepCount = sessionStagesHistory.count { it == SleepStage.DEEP }
        val lightCount = sessionStagesHistory.count { it == SleepStage.LIGHT }
        val remCount = sessionStagesHistory.count { it == SleepStage.REM }

        val deepPct = if (sessionStagesHistory.isNotEmpty()) (deepCount * 100 / totalStages) else 35
        val remPct = if (sessionStagesHistory.isNotEmpty()) (remCount * 100 / totalStages) else 22
        val lightPct = 100 - deepPct - remPct

        val summary = SleepSessionSummary(
            durationSeconds = elapsed,
            avgHeartRate = avgHr,
            minHeartRate = minHr,
            avgHrvRmssd = avgHrv,
            deepSleepPercent = deepPct.coerceIn(10, 60),
            lightSleepPercent = lightPct.coerceIn(20, 70),
            remSleepPercent = remPct.coerceIn(10, 40),
            wakeUpTrigger = wakeReason
        )
        _uiState.update { it.copy(sessionSummary = summary) }
    }

    private fun recalculateAudioParams() {
        val current = _uiState.value
        val effectiveAcousticMode = current.manualModeOverride
            ?: audioOutputRouter.recommendedAcousticMode.value

        val (newParams, updatedAlarm) = mappingEngine.evaluate(
            vitals = current.vitals,
            baseCarrierHz = current.baseCarrierHz,
            activeAcousticMode = effectiveAcousticMode,
            noiseType = current.noiseType,
            noiseVolume = current.noiseVolume,
            noiseFilterCutoff = current.noiseFilterCutoffHz,
            masterVolume = current.masterVolume,
            isPlaying = current.isPlaying,
            smartAlarmConfig = current.smartAlarmConfig
        )

        // Trigger gentle haptics if alarm just began ringing
        if (updatedAlarm.isAlarmRinging && !current.smartAlarmConfig.isAlarmRinging) {
            startHapticAlarm()
        }

        dspSynthesizer.isAlarmChiming = updatedAlarm.isAlarmRinging
        dspSynthesizer.updateParams(newParams)

        _uiState.update {
            it.copy(
                synthesisParams = newParams,
                smartAlarmConfig = updatedAlarm
            )
        }
    }

    private fun startSessionTimer() {
        sessionTimerJob?.cancel()
        sessionTimerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                val newElapsed = _uiState.value.sessionElapsedSeconds + 1
                val timerMinutes = _uiState.value.sleepTimerMinutes
                if (timerMinutes > 0 && newElapsed >= timerMinutes * 60) {
                    // Sleep timer completed: stop playback smoothly
                    togglePlayback()
                    break
                }
                _uiState.update { it.copy(sessionElapsedSeconds = newElapsed) }
            }
        }
    }
}
