package com.example.adaptivesleep.health

import android.content.Context
import android.util.Log
import com.example.adaptivesleep.data.model.PhysiologicalState
import com.example.adaptivesleep.data.model.SleepStage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Sensor Repository coordinating live Health Connect reads with an intelligent
 * fallback sensor simulator for testing, low-latency responsiveness, and wearable sync latency.
 */
class SensorRepository(
    private val context: Context,
    private val healthConnectManager: HealthConnectManager
) {
    private val tag = "SensorRepository"
    private val repositoryScope = CoroutineScope(Dispatchers.Default + Job())

    private val _currentState = MutableStateFlow(
        PhysiologicalState(
            heartRateBpm = 64,
            hrvRmssdMillis = 55.0,
            sleepStage = SleepStage.LIGHT,
            baselineRestingHr = 60,
            isLiveHealthConnect = false
        )
    )
    val currentState: StateFlow<PhysiologicalState> = _currentState.asStateFlow()

    private val _isSimulationMode = MutableStateFlow(true)
    val isSimulationMode: StateFlow<Boolean> = _isSimulationMode.asStateFlow()

    private var pollingJob: Job? = null

    init {
        startPolling()
    }

    fun setSimulationMode(enabled: Boolean) {
        _isSimulationMode.value = enabled
        startPolling()
    }

    fun setBaselineHr(hr: Int) {
        _currentState.value = _currentState.value.copy(baselineRestingHr = hr)
    }

    /**
     * Injects a specific physiological stage for immediate demonstration and acoustic evaluation.
     */
    fun simulateStage(stage: SleepStage) {
        _isSimulationMode.value = true
        val baseline = _currentState.value.baselineRestingHr
        val (hr, hrv) = when (stage) {
            SleepStage.AWAKE -> Pair(baseline + 16, 32.0)
            SleepStage.LIGHT -> Pair(baseline + 5, 52.0)
            SleepStage.DEEP -> Pair(baseline - 8, 78.0)
            SleepStage.REM -> Pair(baseline + 7, 42.0)
            SleepStage.UNKNOWN -> Pair(baseline, 50.0)
        }
        _currentState.value = PhysiologicalState(
            heartRateBpm = hr,
            hrvRmssdMillis = hrv,
            sleepStage = stage,
            baselineRestingHr = baseline,
            timestampMillis = System.currentTimeMillis(),
            isLiveHealthConnect = false
        )
    }

    /**
     * Simulates an arousal / stress event (e.g., sudden heart rate spike).
     */
    fun simulateArousalSpike() {
        _isSimulationMode.value = true
        val baseline = _currentState.value.baselineRestingHr
        _currentState.value = PhysiologicalState(
            heartRateBpm = baseline + 28,
            hrvRmssdMillis = 22.0,
            sleepStage = SleepStage.AWAKE,
            baselineRestingHr = baseline,
            timestampMillis = System.currentTimeMillis(),
            isLiveHealthConnect = false
        )
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = repositoryScope.launch {
            while (isActive) {
                if (_isSimulationMode.value) {
                    runSimulationCycle()
                    delay(3000L) // 3s micro-cycle in simulation mode for dynamic live feedback
                } else {
                    runHealthConnectCycle()
                    delay(15000L) // 15s polling interval for Health Connect
                }
            }
        }
    }

    private suspend fun runHealthConnectCycle() {
        try {
            val liveData = healthConnectManager.readRecentMetrics(_currentState.value.baselineRestingHr)
            if (liveData != null) {
                _currentState.value = liveData
            } else {
                Log.d(tag, "Health Connect returned null data, retaining current baseline")
            }
        } catch (e: Exception) {
            Log.e(tag, "Health Connect polling exception", e)
        }
    }

    private fun runSimulationCycle() {
        val curr = _currentState.value
        // Natural physiological breathing oscillation (sinus arrhythmia jitter)
        val hrJitter = Random.nextInt(-1, 2)
        val hrvJitter = (Random.nextDouble() - 0.5) * 1.5

        val targetHr = when (curr.sleepStage) {
            SleepStage.AWAKE -> curr.baselineRestingHr + 15
            SleepStage.LIGHT -> curr.baselineRestingHr + 4
            SleepStage.DEEP -> curr.baselineRestingHr - 8
            SleepStage.REM -> curr.baselineRestingHr + 6
            SleepStage.UNKNOWN -> curr.baselineRestingHr
        }

        // Smooth gradual trend towards target
        val newHr = ((curr.heartRateBpm * 0.85) + (targetHr * 0.15)).toInt() + hrJitter
        val clampedHr = newHr.coerceIn(45, 120)
        val newHrv = (curr.hrvRmssdMillis + hrvJitter).coerceIn(15.0, 110.0)

        _currentState.value = curr.copy(
            heartRateBpm = clampedHr,
            hrvRmssdMillis = newHrv,
            timestampMillis = System.currentTimeMillis()
        )
    }
}
