package me.shetj.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.text.TextUtils
import android.util.Log
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException

/**
 * PCM音频播放manager
 * 必须先设置setContext
 */
object AudioTrackManager {
    private var PATH: String? = null
    private var mAudioTrack: AudioTrack? = null
    private var bufferSize: Int = 0
    private var mAudioManager: AudioManager? = null
    private var fileInputStream: FileInputStream? = null
    var isStartPlay = false
        private set

    fun setContext(context: Context, path: String) {
        mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        this.PATH = path
        Log.i("AudioTrackManager", "setContext: path = $path")
        init()
    }

    private fun init() {
        val audioFormatEncode = AudioFormat.ENCODING_PCM_16BIT
        val channelConfig = AudioFormat.CHANNEL_IN_STEREO
        val mSampleRate = 44100

        bufferSize = AudioTrack.getMinBufferSize(mSampleRate, channelConfig, audioFormatEncode)
        val sessionId = mAudioManager!!.generateAudioSessionId()
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val audioFormat = AudioFormat.Builder()
            .setSampleRate(mSampleRate)
            .setEncoding(audioFormatEncode)
            .setChannelMask(channelConfig)
            .build()
        mAudioTrack = AudioTrack(
            audioAttributes,
            audioFormat,
            bufferSize * 2,
            AudioTrack.MODE_STREAM,
            sessionId
        )
        try {
            fileInputStream = FileInputStream(PATH!!)
        } catch (e: FileNotFoundException) {
            Log.e("AudioTrackManager", "init: $e")
        }

    }

    fun startThread() {
        if (!TextUtils.isEmpty(PATH)) {
            isStartPlay = true
            PlayThread().start()
        }else{
            throw NullPointerException("PATH not be null,please setContext")
        }
    }

    fun stopThread() {
        if (isStartPlay) {
            isStartPlay = false
            mAudioTrack?.stop()
            mAudioTrack?.release()
        }
    }

    private class PlayThread : Thread() {
        override fun run() {
            super.run()
            if (isStartPlay) {
                val buffer = ByteArray(bufferSize)
                mAudioTrack!!.play()
                try {
                    while (fileInputStream!!.read(buffer) > 0 && isStartPlay) {
                        mAudioTrack!!.write(buffer, 0, buffer.size)
                    }
                    stopThread()
                } catch (e: IOException) {
                    Log.e("AudioTrackManager", "IOException $e")
                }
            }
        }
    }
}