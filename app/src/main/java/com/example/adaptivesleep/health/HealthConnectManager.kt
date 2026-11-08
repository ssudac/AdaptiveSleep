package com.example.adaptivesleep.health

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.adaptivesleep.data.model.PhysiologicalState
import com.example.adaptivesleep.data.model.SleepStage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Manages Google Health Connect interactions: permissions, availability checks,
 * read queries, and change-token listeners.
 */
class HealthConnectManager(private val context: Context) {

    private val tag = "HealthConnectManager"

    val healthConnectClient: HealthConnectClient? by lazy {
        if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
            HealthConnectClient.getOrCreate(context)
        } else {
            null
        }
    }

    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class)
    )

    fun isSdkAvailable(): Boolean {
        return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    }

    suspend fun hasAllPermissions(): Boolean = withContext(Dispatchers.IO) {
        val client = healthConnectClient ?: return@withContext false
        try {
            val granted = client.permissionController.getGrantedPermissions()
            permissions.all { it in granted }
        } catch (e: Exception) {
            Log.e(tag, "Failed to inspect permissions", e)
            false
        }
    }

    /**
     * Reads recent physiological data from Health Connect (last 30 minutes).
     */
    suspend fun readRecentMetrics(baselineRestingHr: Int = 60): PhysiologicalState? = withContext(Dispatchers.IO) {
        val client = healthConnectClient ?: return@withContext null
        if (!hasAllPermissions()) return@withContext null

        try {
            val now = Instant.now()
            val thirtyMinutesAgo = now.minus(30, ChronoUnit.MINUTES)
            val timeFilter = TimeRangeFilter.between(thirtyMinutesAgo, now)

            // 1. Query Heart Rate
            val hrRequest = ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = timeFilter,
                ascendingOrder = false,
                pageSize = 10
            )
            val hrResponse = client.readRecords(hrRequest)
            val latestHeartRate = hrResponse.records
                .flatMap { it.samples }
                .maxByOrNull { it.time }
                ?.beatsPerMinute?.toInt()

            // 2. Query HRV RMSSD
            val hrvRequest = ReadRecordsRequest(
                recordType = HeartRateVariabilityRmssdRecord::class,
                timeRangeFilter = timeFilter,
                ascendingOrder = false,
                pageSize = 5
            )
            val hrvResponse = client.readRecords(hrvRequest)
            val latestHrv = hrvResponse.records
                .maxByOrNull { it.time }
                ?.heartRateVariabilityMillis

            // 3. Query Sleep Session and Stages
            val sleepRequest = ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(now.minus(12, ChronoUnit.HOURS), now),
                ascendingOrder = false,
                pageSize = 1
            )
            val sleepResponse = client.readRecords(sleepRequest)
            val latestSession = sleepResponse.records.firstOrNull()

            val detectedStage = latestSession?.stages?.maxByOrNull { it.endTime }?.let { stageRecord ->
                when (stageRecord.stage) {
                    SleepSessionRecord.STAGE_TYPE_AWAKE -> SleepStage.AWAKE
                    SleepSessionRecord.STAGE_TYPE_LIGHT -> SleepStage.LIGHT
                    SleepSessionRecord.STAGE_TYPE_DEEP -> SleepStage.DEEP
                    SleepSessionRecord.STAGE_TYPE_REM -> SleepStage.REM
                    SleepSessionRecord.STAGE_TYPE_OUT_OF_BED -> SleepStage.AWAKE
                    else -> SleepStage.LIGHT
                }
            } ?: inferSleepStageFromVitals(latestHeartRate, latestHrv, baselineRestingHr)

            PhysiologicalState(
                heartRateBpm = latestHeartRate ?: 64,
                hrvRmssdMillis = latestHrv ?: 48.0,
                sleepStage = detectedStage,
                baselineRestingHr = baselineRestingHr,
                timestampMillis = System.currentTimeMillis(),
                isLiveHealthConnect = true
            )
        } catch (e: Exception) {
            Log.e(tag, "Error reading Health Connect records", e)
            null
        }
    }

    /**
     * Obtains change token for real-time change polling.
     */
    suspend fun getChangesToken(): String? = withContext(Dispatchers.IO) {
        val client = healthConnectClient ?: return@withContext null
        try {
            client.getChangesToken(
                ChangesTokenRequest(
                    recordTypes = setOf(
                        SleepSessionRecord::class,
                        HeartRateRecord::class,
                        HeartRateVariabilityRmssdRecord::class
                    )
                )
            )
        } catch (e: Exception) {
            Log.e(tag, "Failed to get changes token", e)
            null
        }
    }

    /**
     * Infers sleep stage if wearable hasn't generated stage segments yet.
     */
    private fun inferSleepStageFromVitals(hr: Int?, hrv: Double?, baselineHr: Int): SleepStage {
        if (hr == null) return SleepStage.UNKNOWN
        val delta = hr - baselineHr
        return when {
            delta >= 10 -> SleepStage.AWAKE
            delta in -2..4 && (hrv ?: 50.0) >= 60.0 -> SleepStage.DEEP
            delta in 3..8 -> SleepStage.REM
            else -> SleepStage.LIGHT
        }
    }
}
