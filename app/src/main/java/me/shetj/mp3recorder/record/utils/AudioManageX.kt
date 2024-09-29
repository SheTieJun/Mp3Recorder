@file:Suppress("DEPRECATION")

package me.shetj.mp3recorder.record.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.media.AudioRecordingConfiguration
import android.os.Build
import androidx.core.content.ContextCompat

/**
 *
 * 用ExoPlayer他会自行处理，就不需要用的这个了
 * @param context 上下文
 *
 * * [requestAudioFocus]  申请音频焦点
 * * [abandonFocus] 放弃音频焦点
 * * [setOnAudioFocusChangeListener] 监听焦点变化
 */

class AudioManagerX(context: Context) {

    private var onAudioFocusChangeListener: OnAudioFocusChange? = null

    private var mAudioManager: AudioManager? = null

    private var playbackDelayed = false
    private var playbackNowAuthorized = false
    private var resumeOnFocusGain = false

    private val focusLock = Any()

    private val focusChangeListener: OnAudioFocusChangeListener =
        OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS -> {
                    synchronized(focusLock) {
                        resumeOnFocusGain = false
                        playbackDelayed = false
                    }
                    onAudioFocusChangeListener?.onLoss()
                }

                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    synchronized(focusLock) {
                        // only resume if playback is being interrupted
                        resumeOnFocusGain = false
                        playbackDelayed = false
                    }
                    // 短暂性丢失焦点，当其他应用申请AUDIOFOCUS_GAIN_TRANSIENT或AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE时，
                    onAudioFocusChangeListener?.onLossTransient()
                }

                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    onAudioFocusChangeListener?.onLossTransientCanDuck()
                }

                AudioManager.AUDIOFOCUS_GAIN -> {
                    if (playbackDelayed || resumeOnFocusGain) {
                        synchronized(focusLock) {
                            playbackDelayed = false
                            resumeOnFocusGain = false
                        }
                        onAudioFocusChangeListener?.onGain()
                    }
                }
            }
        }

    private val audioFocusRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // Android 8.0+
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder().run {
                    setUsage(AudioAttributes.USAGE_GAME)
                    setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    build()
                }
            )
            .setWillPauseWhenDucked(true)
            .setOnAudioFocusChangeListener(focusChangeListener).build()
    } else {
        null
    }

    init {
        init(context)
    }

    fun getAudioManager() = mAudioManager

    /**
     * 申请音频焦点
     */
    fun requestAudioFocus() {
        if (mAudioManager == null) return
        val res = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // Android 8.0+
            if (audioFocusRequest == null) return
            mAudioManager!!.requestAudioFocus(audioFocusRequest)
        } else {
            mAudioManager?.requestAudioFocus(
                focusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        synchronized(focusLock) {
            playbackNowAuthorized = when (res) {
                AudioManager.AUDIOFOCUS_REQUEST_FAILED -> {
                    onAudioFocusChangeListener?.onLoss()
                    false
                }

                AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                    onAudioFocusChangeListener?.onGain()
                    true
                }

                AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> {
                    onAudioFocusChangeListener?.onGain()
                    false
                }

                else -> false
            }
        }
    }

    /**
     * 放弃音频焦点，防止内存泄漏
     */
    fun abandonFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val abandonAudioFocusRequest =
                audioFocusRequest?.let { mAudioManager?.abandonAudioFocusRequest(it) }
            abandonAudioFocusRequest == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
                    mAudioManager?.abandonAudioFocus(focusChangeListener)
        }
    }

    fun setOnAudioFocusChangeListener(onAudioFocusChangeListener: OnAudioFocusChange) {
        this.onAudioFocusChangeListener = onAudioFocusChangeListener
    }

    fun adjustStreamVolume() {
        mAudioManager?.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_RAISE,
            AudioManager.FLAG_SHOW_UI
        )
    }

    // 获取最佳采样率
    fun getBestSampleRate(): Int {
        val sampleRateStr: String? = mAudioManager?.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
        val sampleRate: Int = sampleRateStr?.let { str ->
            Integer.parseInt(str).takeUnless { it == 0 }
        } ?: 44100 // Use a default value if property not found
        return sampleRate
    }

    // 获取最佳缓冲大小
    fun getBestBufferSize(): Int {
        val bufferSizeStr: String? = mAudioManager?.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
        val bufferSize: Int = bufferSizeStr?.let { str ->
            Integer.parseInt(str).takeUnless { it == 0 }
        } ?: 256 // Use a default value if property not found
        return bufferSize
    }


    private fun init(context: Context) {
        mAudioManager = ContextCompat.getSystemService(context.applicationContext, AudioManager::class.java) as AudioManager

        checkDevice()
    }



    fun checkDevice(): StringBuilder {
        val deviceTypeMap = mutableMapOf<Int,String>()
        deviceTypeMap[AudioDeviceInfo.TYPE_WIRED_HEADPHONES] = "有线耳机麦克风";
        deviceTypeMap[AudioDeviceInfo.TYPE_BLUETOOTH_SCO] = "蓝牙麦克风";
        deviceTypeMap[AudioDeviceInfo.TYPE_TELEPHONY] = "通话麦克风音频输入";
        deviceTypeMap[AudioDeviceInfo.TYPE_BUILTIN_MIC] = "内置麦克风";
        deviceTypeMap[AudioDeviceInfo.TYPE_FM_TUNER] = "FM收音机设备";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            deviceTypeMap[AudioDeviceInfo.TYPE_REMOTE_SUBMIX] = "虚拟混音设备"
        };
        val devices = mAudioManager?.getDevices(AudioManager.GET_DEVICES_INPUTS)

        val deviceInfoString = StringBuilder()
        devices?.forEach { device ->
            if (device == null) {
                return@forEach
            }
            deviceInfoString.append("\n\n产品名称：\t")
            deviceInfoString.append(device.getProductName())


            deviceInfoString.append("\n\n是否Source：\t")
            deviceInfoString.append(device.isSource)

            deviceInfoString.append("\n设备类型：\t")
            deviceInfoString.append(deviceTypeMap[device.type]?:device.type)

            deviceInfoString.append("\n设备ID：\t")
            deviceInfoString.append(device.id)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                deviceInfoString.append("\n设备位置：\t")
                deviceInfoString.append(device.address)
            }

            deviceInfoString.append("\n支持采样率：\t")
            deviceInfoString.append(device.sampleRates.joinToString(prefix = "[", postfix = "]", separator = ","))

            deviceInfoString.append("\n支持Channel数量：\t")
            deviceInfoString.append(device.getChannelCounts().joinToString(prefix = "[", postfix = "]", separator = ","))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                deviceInfoString.append("\n支持metadata封装类型：\t")
                deviceInfoString.append(device.encapsulationModes.joinToString(prefix = "[", postfix = "]", separator = ","))
            }

            deviceInfoString.append("\n支持Encoding类型：")
            deviceInfoString.append(device.encodings.joinToString(prefix = "[", postfix = "]", separator = ",", transform = {
                toLogFriendlyEncoding(it)
            }))
        }
        deviceInfoString.append("\n\n----------- 当前使用的正在录音设备----------- \n\t")

        if (   mAudioManager?.activeRecordingConfigurations.isNullOrEmpty()){
            deviceInfoString.append("\n\n暂无：\t")
        }

        mAudioManager?.activeRecordingConfigurations?.forEach {  audioRecordingConfiguration ->
            val device = audioRecordingConfiguration.audioDevice ?: return@forEach
            deviceInfoString.append("\n\n产品名称：\t")
            deviceInfoString.append(device.getProductName())


            deviceInfoString.append("\n\n是否Source：\t")
            deviceInfoString.append(device.isSource)

            deviceInfoString.append("\n设备类型：\t")
            deviceInfoString.append(deviceTypeMap[device.type]?:device.type)

            deviceInfoString.append("\n设备ID：\t")
            deviceInfoString.append(device.id)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                deviceInfoString.append("\n设备位置：\t")
                deviceInfoString.append(device.address)
            }

            deviceInfoString.append("\n支持采样率：\t")
            deviceInfoString.append(device.sampleRates.joinToString(prefix = "[", postfix = "]", separator = ","))

            deviceInfoString.append("\n支持Channel数量：\t")
            deviceInfoString.append(device.getChannelCounts().joinToString(prefix = "[", postfix = "]", separator = ","))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                deviceInfoString.append("\n支持metadata封装类型：\t")
                deviceInfoString.append(device.encapsulationModes.joinToString(prefix = "[", postfix = "]", separator = ","))
            }

            deviceInfoString.append("\n支持Encoding类型：")
            deviceInfoString.append(device.encodings.joinToString(prefix = "[", postfix = "]", separator = ",", transform = {
                toLogFriendlyEncoding(it)
            }))
        }
        return deviceInfoString
    }

    /**
     * Get record device
     * 获取正在录音的设备
     * @return
     */
    fun getRecordDevice(): AudioRecordingConfiguration? {
        return mAudioManager?.activeRecordingConfigurations?.get(0)
    }

    fun toLogFriendlyEncoding(enc: Int): String {
        return when (enc) {
            AudioFormat.ENCODING_INVALID -> "ENCODING_INVALID"
            AudioFormat.ENCODING_PCM_16BIT -> "ENCODING_PCM_16BIT"
            AudioFormat.ENCODING_PCM_8BIT -> "ENCODING_PCM_8BIT"
            AudioFormat.ENCODING_PCM_FLOAT -> "ENCODING_PCM_FLOAT"
            AudioFormat.ENCODING_AC3 -> "ENCODING_AC3"
            AudioFormat.ENCODING_E_AC3 -> "ENCODING_E_AC3"
            AudioFormat.ENCODING_DTS -> "ENCODING_DTS"
            AudioFormat.ENCODING_DTS_HD -> "ENCODING_DTS_HD"
            AudioFormat.ENCODING_MP3 -> "ENCODING_MP3"
            AudioFormat.ENCODING_AAC_LC -> "ENCODING_AAC_LC"
            AudioFormat.ENCODING_AAC_HE_V1 -> "ENCODING_AAC_HE_V1"
            AudioFormat.ENCODING_AAC_HE_V2 -> "ENCODING_AAC_HE_V2"
            AudioFormat.ENCODING_IEC61937 -> "ENCODING_IEC61937"
            AudioFormat.ENCODING_DOLBY_TRUEHD -> "ENCODING_DOLBY_TRUEHD"
            AudioFormat.ENCODING_AAC_ELD -> "ENCODING_AAC_ELD"
            AudioFormat.ENCODING_AAC_XHE -> "ENCODING_AAC_XHE"
            AudioFormat.ENCODING_AC4 -> "ENCODING_AC4"
            AudioFormat.ENCODING_E_AC3_JOC -> "ENCODING_E_AC3_JOC"
            AudioFormat.ENCODING_DOLBY_MAT -> "ENCODING_DOLBY_MAT"
            AudioFormat.ENCODING_OPUS -> "ENCODING_OPUS"
            AudioFormat.ENCODING_PCM_24BIT_PACKED -> "ENCODING_PCM_24BIT_PACKED"
            AudioFormat.ENCODING_PCM_32BIT -> "ENCODING_PCM_32BIT"
            AudioFormat.ENCODING_MPEGH_BL_L3 -> "ENCODING_MPEGH_BL_L3"
            AudioFormat.ENCODING_MPEGH_BL_L4 -> "ENCODING_MPEGH_BL_L4"
            AudioFormat.ENCODING_MPEGH_LC_L3 -> "ENCODING_MPEGH_LC_L3"
            AudioFormat.ENCODING_MPEGH_LC_L4 -> "ENCODING_MPEGH_LC_L4"
//            AudioFormat.ENCODING_DTS_UHD_P1 -> "ENCODING_DTS_UHD_P1"
            AudioFormat.ENCODING_DRA -> "ENCODING_DRA"
//            AudioFormat.ENCODING_DTS_HD_MA -> "ENCODING_DTS_HD_MA"
//            AudioFormat.ENCODING_DTS_UHD_P2 -> "ENCODING_DTS_UHD_P2"
//            AudioFormat.ENCODING_DSD -> "ENCODING_DSD"
            else -> "invalid encoding $enc"
        }
    }

    fun onDestroy() {
        mAudioManager = null
        onAudioFocusChangeListener = null
        abandonFocus()
    }

    interface OnAudioFocusChange {

        /**
         * 失去了Audio Focus，并将会持续很长的时间。这里因为可能会停掉很长时间，所以不仅仅要停止Audio的播放，最好直接释放掉Media资源。
         */
        fun onLoss() {
        }

        /**
         * 获得了Audio Focus；
         */
        fun onGain() {
        }

        /**
         * 暂时失去Audio Focus，并会很快再次获得。必须停止Audio的播放，但是因为可能会很快再次获得AudioFocus，这里可以不释放Media资源；
         */
        fun onLossTransient() {
        }

        /**
         * 暂时失去AudioFocus，但是可以继续播放，不过要在降低音量。
         */
        fun onLossTransientCanDuck() {
        }
    }
}
