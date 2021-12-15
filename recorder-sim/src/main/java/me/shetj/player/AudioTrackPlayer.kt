/*
 * MIT License
 *
 * Copyright (c) 2019 SheTieJun
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package me.shetj.player

import android.annotation.TargetApi
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.text.TextUtils
import android.util.Log
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException

/**
 * PCM音频播放manager
 * 必须先设置setContext
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
object AudioTrackPlayer {
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
        val channelConfig = AudioFormat.CHANNEL_OUT_MONO
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
        } else {
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
