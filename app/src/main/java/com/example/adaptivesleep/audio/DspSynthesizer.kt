package com.example.adaptivesleep.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import android.util.Log
import com.example.adaptivesleep.data.model.AcousticMode
import com.example.adaptivesleep.data.model.AudioSynthesisParams
import com.example.adaptivesleep.data.model.NoiseType
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Real-time Algorithmic DSP AudioTrack Synthesizer.
 * Generates continuous stereo PCM audio streams with zero-latency buffer updates,
 * continuous phase accumulators, linear parameter slewing, and cross-fading.
 */
class DspSynthesizer {

    private val tag = "DspSynthesizer"
    val sampleRate = 44100
    private val bufferSizeSamples = 2048 // ~46ms buffer chunk for low latency and zero dropouts

    private var audioTrack: AudioTrack? = null
    private var synthesisThread: Thread? = null
    private val isRunning = AtomicBoolean(false)

    // Current target parameters
    @Volatile
    private var targetParams = AudioSynthesisParams()

    // Smoothly slewed internal synthesis state (sample-by-sample linear interpolation)
    private var currentCarrierHz = 160.0f
    private var currentBeatHz = 2.0f
    private var currentMasterVolume = 0.0f
    private var currentNoiseVolume = 0.0f
    private var currentCutoffHz = 1200.0f

    // Phase accumulators (radians in [0, 2*PI))
    private var phaseLeft = 0.0
    private var phaseRight = 0.0
    private var phaseModulation = 0.0
    private var phaseMonaural1 = 0.0
    private var phaseMonaural2 = 0.0

    // Mode cross-fading state
    private var activeMode = AcousticMode.BINAURAL
    private var previousMode = AcousticMode.BINAURAL
    private var crossfadeProgress = 1.0f // 1.0 = fully transitioned

    // Ambient Noise Generator internal state
    // Pink noise (Paul Kellet 6-stage filter)
    private var p0 = 0.0
    private var p1 = 0.0
    private var p2 = 0.0
    private var p3 = 0.0
    private var p4 = 0.0
    private var p5 = 0.0
    private var p6 = 0.0

    // Brown noise leaky integrator
    private var brownVal = 0.0

    // Low-pass filter state for noise
    private var lpFilteredNoise = 0.0

    // Smart Alarm Chime State
    @Volatile
    var isAlarmChiming = false
    private var alarmTimerSamples = 0L
    private val chimeFrequencies = floatArrayOf(329.63f, 440.0f, 554.37f, 659.25f) // E4, A4, C#5, E5
    private val chimePhases = DoubleArray(4)

    /**
     * Updates synthesis target parameters smoothly.
     */
    fun updateParams(params: AudioSynthesisParams) {
        if (params.acousticMode != targetParams.acousticMode) {
            previousMode = activeMode
            activeMode = params.acousticMode
            crossfadeProgress = 0.0f // Trigger smooth crossfade
        }
        targetParams = params
    }

    /**
     * Starts the audio synthesis thread and stream.
     */
    @Synchronized
    fun start() {
        if (isRunning.get()) return

        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val trackBufferSize = maxOf(minBufferSize * 2, bufferSizeSamples * 4)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()
            )
            .setBufferSizeInBytes(trackBufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        isRunning.set(true)
        audioTrack?.play()

        synthesisThread = Thread {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
            runSynthesisLoop()
        }.apply {
            name = "AdaptiveSleep-DSP"
            start()
        }
        Log.d(tag, "AudioTrack synthesis engine initialized and running")
    }

    /**
     * Stops the audio synthesis thread cleanly.
     */
    @Synchronized
    fun stop() {
        if (!isRunning.get()) return
        isRunning.set(false)
        try {
            synthesisThread?.interrupt()
            synthesisThread?.join(500)
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            Log.e(tag, "Error stopping DSP engine", e)
        } finally {
            audioTrack = null
            synthesisThread = null
        }
        Log.d(tag, "AudioTrack synthesis engine stopped")
    }

    /**
     * Main high-priority DSP synthesis loop.
     */
    private fun runSynthesisLoop() {
        val pcmBuffer = ShortArray(bufferSizeSamples * 2) // Interleaved Stereo (L, R)
        val twoPi = 2.0 * PI
        val dt = 1.0 / sampleRate

        // Slewing filter coefficients (low-pass smoothing for zero clicks)
        val freqSlewFactor = 0.0015f
        val volSlewFactor = 0.002f

        while (isRunning.get() && !Thread.currentThread().isInterrupted) {
            val params = targetParams
            val targetCarrier = params.carrierFrequencyHz.coerceIn(80.0f, 350.0f)
            val targetBeat = params.beatFrequencyHz.coerceIn(0.2f, 30.0f)
            val targetMaster = if (params.isPlaying) params.masterVolume.coerceIn(0.0f, 1.0f) else 0.0f
            val targetNoise = if (params.isPlaying && params.noiseType != NoiseType.OFF) {
                params.noiseVolume.coerceIn(0.0f, 1.0f)
            } else 0.0f
            val targetCutoff = params.noiseFilterCutoffHz.coerceIn(200.0f, 4000.0f)

            var bufferIdx = 0

            for (i in 0 until bufferSizeSamples) {
                // 1. Slew parameters continuously
                currentCarrierHz += (targetCarrier - currentCarrierHz) * freqSlewFactor
                currentBeatHz += (targetBeat - currentBeatHz) * freqSlewFactor
                currentMasterVolume += (targetMaster - currentMasterVolume) * volSlewFactor
                currentNoiseVolume += (targetNoise - currentNoiseVolume) * volSlewFactor
                currentCutoffHz += (targetCutoff - currentCutoffHz) * freqSlewFactor

                if (crossfadeProgress < 1.0f) {
                    crossfadeProgress = (crossfadeProgress + (1.0f / 8820.0f)).coerceAtMost(1.0f) // ~200ms crossfade
                }

                // 2. Synthesize Mode Signals
                val sampleCarrier = currentCarrierHz.toDouble()
                val sampleBeat = currentBeatHz.toDouble()

                // Phase updates (modulo 2*PI)
                phaseLeft = (phaseLeft + twoPi * sampleCarrier * dt) % twoPi
                phaseRight = (phaseRight + twoPi * (sampleCarrier + sampleBeat) * dt) % twoPi
                phaseModulation = (phaseModulation + twoPi * sampleBeat * dt) % twoPi
                phaseMonaural1 = (phaseMonaural1 + twoPi * sampleCarrier * dt) % twoPi
                phaseMonaural2 = (phaseMonaural2 + twoPi * (sampleCarrier + sampleBeat) * dt) % twoPi

                // Calculate samples for active and previous modes during crossfade
                val (currL, currR) = computeModeSample(activeMode)
                val (finalToneL, finalToneR) = if (crossfadeProgress < 1.0f) {
                    val (prevL, prevR) = computeModeSample(previousMode)
                    val wCurr = crossfadeProgress
                    val wPrev = 1.0f - crossfadeProgress
                    Pair(prevL * wPrev + currL * wCurr, prevR * wPrev + currR * wCurr)
                } else {
                    Pair(currL, currR)
                }

                // 3. Generate Ambient Masking Noise
                val noiseSample = generateNoiseSample(params.noiseType, currentCutoffHz) * currentNoiseVolume

                // 4. Synthesize Gentle Smart Alarm Chime if active
                var alarmSampleL = 0.0
                var alarmSampleR = 0.0
                if (isAlarmChiming) {
                    val (alL, alR) = computeAlarmChime(dt)
                    alarmSampleL = alL
                    alarmSampleR = alR
                }

                // 5. Mix channels and apply Master Volume
                val mixedL = (finalToneL * 0.7 + noiseSample * 0.3 + alarmSampleL) * currentMasterVolume
                val mixedR = (finalToneR * 0.7 + noiseSample * 0.3 + alarmSampleR) * currentMasterVolume

                // 6. Soft-clipping saturation to prevent digital distortion
                val clippedL = softClip(mixedL)
                val clippedR = softClip(mixedR)

                // Write interleaved 16-bit PCM
                pcmBuffer[bufferIdx++] = (clippedL * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
                pcmBuffer[bufferIdx++] = (clippedR * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
            }

            // Write buffer chunk to AudioTrack
            val written = audioTrack?.write(pcmBuffer, 0, pcmBuffer.size) ?: -1
            if (written < 0) {
                Log.w(tag, "AudioTrack write returned $written")
            }
        }
    }

    private fun computeModeSample(mode: AcousticMode): Pair<Double, Double> {
        return when (mode) {
            AcousticMode.BINAURAL -> {
                // Left gets carrier, Right gets carrier + beat
                val l = sin(phaseLeft)
                val r = sin(phaseRight)
                Pair(l, r)
            }
            AcousticMode.ISOCHRONIC -> {
                // Carrier modulated by raised cosine envelope pulsing at beat frequency
                val carrier = sin(phaseLeft)
                // Smooth envelope: 0.5 * (1 + cos(theta))^1.6 for comfortable acoustic pulse
                val envelope = ((0.5 + 0.5 * cos(phaseModulation)).coerceIn(0.0, 1.0)).pow(1.6)
                val isochronic = carrier * envelope
                Pair(isochronic, isochronic)
            }
            AcousticMode.MONAURAL -> {
                // Mathematically pre-mixed sum sent equally to both stereo channels
                val monaural = 0.5 * (sin(phaseMonaural1) + sin(phaseMonaural2))
                Pair(monaural, monaural)
            }
        }
    }

    private fun generateNoiseSample(noiseType: NoiseType, cutoffHz: Float): Double {
        if (noiseType == NoiseType.OFF) return 0.0

        // White noise base [-1.0, 1.0]
        val white = (Random.nextDouble() * 2.0) - 1.0

        val rawNoise = when (noiseType) {
            NoiseType.PINK -> {
                // Paul Kellet's filter for true -3dB/octave pink noise
                p0 = 0.99886 * p0 + white * 0.0555179
                p1 = 0.99332 * p1 + white * 0.0750759
                p2 = 0.96900 * p2 + white * 0.1538520
                p3 = 0.86650 * p3 + white * 0.3104856
                p4 = 0.55000 * p4 + white * 0.5329522
                p5 = -0.7616 * p5 - white * 0.0168980
                val pink = (p0 + p1 + p2 + p3 + p4 + p5 + p6 + white * 0.5362) * 0.11
                p6 = white * 0.115926
                pink
            }
            NoiseType.BROWN -> {
                // Leaky integrator for -6dB/octave Brownian noise
                brownVal = (0.985 * brownVal) + (0.015 * white)
                brownVal * 3.5
            }
            NoiseType.OFF -> 0.0
        }

        // Apply 1-pole Low-Pass Filter based on cutoffHz
        val rc = 1.0 / (2.0 * PI * cutoffHz)
        val alpha = (1.0 / sampleRate) / (rc + (1.0 / sampleRate))
        lpFilteredNoise += alpha * (rawNoise - lpFilteredNoise)
        return lpFilteredNoise
    }

    private fun computeAlarmChime(dt: Double): Pair<Double, Double> {
        alarmTimerSamples++
        val twoPi = 2.0 * PI
        val cycleSamples = (sampleRate * 2.5).toLong() // 2.5s chime repeat cycle
        val cyclePos = (alarmTimerSamples % cycleSamples).toDouble() / sampleRate // seconds into cycle

        var mixedChime = 0.0
        // Arpeggiate through 4 soothing pentatonic bells with gentle exponential decay
        for (idx in chimeFrequencies.indices) {
            val noteDelay = idx * 0.35 // staggered chime notes
            val noteTime = cyclePos - noteDelay
            if (noteTime > 0.0) {
                val decay = exp(-noteTime * 2.2) // smooth bell decay envelope
                chimePhases[idx] = (chimePhases[idx] + twoPi * chimeFrequencies[idx] * dt) % twoPi
                mixedChime += sin(chimePhases[idx]) * decay * 0.35
            }
        }

        // Add gentle stereo spatial spread
        val chimeL = mixedChime * 0.9
        val chimeR = mixedChime * 0.85
        return Pair(chimeL, chimeR)
    }

    /**
     * Tanh-based soft-clipper to prevent harsh digital wrap-around distortion.
     */
    private fun softClip(x: Double): Double {
        return when {
            x > 1.0 -> (2.0 / 3.0) + (x - 1.0) / (1.0 + (x - 1.0).pow(2))
            x < -1.0 -> -((2.0 / 3.0) + (-x - 1.0) / (1.0 + (-x - 1.0).pow(2)))
            else -> x - (x.pow(3) / 3.0)
        }
    }
}
