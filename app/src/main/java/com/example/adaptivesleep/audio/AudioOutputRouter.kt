package com.example.adaptivesleep.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import com.example.adaptivesleep.data.model.AcousticMode
import com.example.adaptivesleep.data.model.TransducerType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Monitors active audio output routing using AudioManager and AudioDeviceInfo.
 * Automatically classifies output as Isolated Stereo (Headphones) vs Open Room Speakers.
 * Manages system Audio Focus and graceful disconnect fallbacks.
 */
class AudioOutputRouter(private val context: Context) {

    private val tag = "AudioOutputRouter"
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _activeTransducer = MutableStateFlow(TransducerType.HEADPHONES)
    val activeTransducer: StateFlow<TransducerType> = _activeTransducer.asStateFlow()

    private val _recommendedAcousticMode = MutableStateFlow(AcousticMode.BINAURAL)
    val recommendedAcousticMode: StateFlow<AcousticMode> = _recommendedAcousticMode.asStateFlow()

    private val _activeDeviceDescription = MutableStateFlow("Detecting audio route...")
    val activeDeviceDescription: StateFlow<String> = _activeDeviceDescription.asStateFlow()

    // Listener for cross-fade triggering when transducer changes mid-playback
    var onTransducerChangedListener: ((TransducerType, AcousticMode) -> Unit)? = null
    var onAudioFocusLostListener: (() -> Unit)? = null
    var onAudioFocusGainedListener: (() -> Unit)? = null

    private var audioFocusRequest: AudioFocusRequest? = null

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.d(tag, "Permanent Audio Focus lost")
                onAudioFocusLostListener?.invoke()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.d(tag, "Transient Audio Focus lost (ducking or temporary pause)")
                onAudioFocusLostListener?.invoke()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d(tag, "Audio Focus regained")
                onAudioFocusGainedListener?.invoke()
            }
        }
    }

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            reevaluateAudioRoute()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            reevaluateAudioRoute()
        }
    }

    init {
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)
        reevaluateAudioRoute()
    }

    fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()

            audioFocusRequest = request
            audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
    }

    fun release() {
        abandonAudioFocus()
        try {
            audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        } catch (e: Exception) {
            Log.e(tag, "Failed to unregister AudioDeviceCallback", e)
        }
    }

    fun reevaluateAudioRoute() {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        var isHeadphonesConnected = false
        var deviceName = "Built-in Speaker"

        for (device in devices) {
            val type = device.type
            val isHeadphone = when (type) {
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLE_HEADSET,
                AudioDeviceInfo.TYPE_USB_HEADSET -> true
                else -> false
            }

            if (isHeadphone) {
                isHeadphonesConnected = true
                val product = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    device.productName?.toString()?.takeIf { it.isNotBlank() }
                } else null
                deviceName = product ?: when (type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLE_HEADSET -> "Bluetooth Headphones"
                    AudioDeviceInfo.TYPE_USB_HEADSET -> "USB-C Headphones"
                    else -> "Wired Headphones"
                }
                break
            }
        }

        val newTransducer = if (isHeadphonesConnected) TransducerType.HEADPHONES else TransducerType.OPEN_SPEAKERS
        val newMode = if (isHeadphonesConnected) AcousticMode.BINAURAL else AcousticMode.ISOCHRONIC
        val desc = if (isHeadphonesConnected) "$deviceName (Isolated Stereo)" else "Phone / Room Speaker (Open Acoustic)"

        val changed = _activeTransducer.value != newTransducer
        _activeTransducer.value = newTransducer
        _recommendedAcousticMode.value = newMode
        _activeDeviceDescription.value = desc

        if (changed) {
            Log.d(tag, "Audio route shifted: $newTransducer -> mode $newMode ($desc)")
            onTransducerChangedListener?.invoke(newTransducer, newMode)
        }
    }

    /**
     * Allows manual override for testing specific acoustic modes.
     */
    fun forceAcousticMode(mode: AcousticMode) {
        _recommendedAcousticMode.value = mode
    }
}
