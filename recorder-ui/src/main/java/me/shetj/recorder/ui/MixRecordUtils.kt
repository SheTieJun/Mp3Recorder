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
package me.shetj.recorder.ui

import android.content.Context
import android.text.TextUtils
import java.io.File
import me.shetj.player.PlayerListener
import me.shetj.recorder.core.BaseRecorder
import me.shetj.recorder.core.FileUtils
import me.shetj.recorder.core.PermissionListener
import me.shetj.recorder.core.RecordListener
import me.shetj.recorder.core.RecordState
import me.shetj.recorder.core.SimRecordListener
import me.shetj.recorder.core.recorder
import me.shetj.recorder.mixRecorder.buildMix

/**
 * 录音工具类
 */
class MixRecordUtils(
    private val context: Context,
    private val maxTime: Long = 30 * 60 * 1000L,
    private val callBack: SimRecordListener?
) : RecordListener, PermissionListener {

    val isRecording: Boolean
        get() {
            return mRecorder?.state == RecordState.RECORDING
        }

    fun hasRecord(): Boolean {
        return mRecorder?.state != RecordState.STOPPED
    }

    init {
        initRecorder()
    }

    private val mp3Path: String
        get() {
            val root = context.filesDir.absolutePath
            val path = StringBuilder(root)
            val dirFile = File("$path/record")
            if (!dirFile.exists()) {
                dirFile.mkdir()
            }
            path.append("/").append("record")
            return path.toString()
        }

    private var startTime: Long = 0 // 秒 s
    private var mRecorder: BaseRecorder? = null
    var saveFile: String? = null
        private set

    @JvmOverloads
    fun startOrPause(file: String = "") {
        if (mRecorder == null) {
            initRecorder()
        }
        when (mRecorder?.state) {
            RecordState.STOPPED -> {
                if (TextUtils.isEmpty(file)) {
                    val mRecordFile = mp3Path + "/" + System.currentTimeMillis() + ".mp3"
                    this.saveFile = mRecordFile
                } else {
                    this.saveFile = file
                }
                mRecorder?.setOutputFile(saveFile!!, !TextUtils.isEmpty(file))
                mRecorder?.start()
            }
            RecordState.PAUSED -> {
                mRecorder?.resume()
            }
            RecordState.RECORDING -> {
                mRecorder?.pause()
            }
            else -> {}
        }
    }

    @JvmOverloads
    fun startOrComplete(file: String = "") {
        if (mRecorder == null) {
            initRecorder()
        }
        when (mRecorder?.state) {
            RecordState.STOPPED -> {
                if (TextUtils.isEmpty(file)) {
                    if (TextUtils.isEmpty(file)) {
                        val mRecordFile = mp3Path + "/" + System.currentTimeMillis() + ".mp3"
                        this.saveFile = mRecordFile
                    } else {
                        this.saveFile = file
                    }
                } else {
                    this.saveFile = file
                }
                mRecorder?.setOutputFile(saveFile!!, !TextUtils.isEmpty(file))
                mRecorder?.start()
            }
            RecordState.RECORDING -> {
                mRecorder?.complete()
            }
            RecordState.PAUSED -> {
            }
            else -> {}
        }
    }

    /**
     * VOICE_COMMUNICATION 消除回声和噪声问题
     * MIC 麦克风- 因为有噪音问题
     */
    private fun initRecorder() {
        mRecorder = recorder {
            recordListener = this@MixRecordUtils
            permissionListener = this@MixRecordUtils
            isDebug = false
        }.buildMix(context)
        mRecorder?.setMaxTime(maxTime, 60 * 1000)
    }

    fun isPause(): Boolean {
        return mRecorder?.state == RecordState.PAUSED
    }

    fun setBackgroundPlayerListener(listener: PlayerListener) {
        mRecorder?.setBackgroundMusicListener(listener)
    }

    fun pause() {
        mRecorder?.pause()
    }

    fun clear() {
        mRecorder?.destroy()
    }

    fun reset() {
        mRecorder?.reset()
    }

    fun cleanPath() {
        saveFile?.let {
            FileUtils.deleteFile(it)
            saveFile = null
        }
    }

    /**
     * 录音异常
     */
    private fun resolveError() {
        if (isRecording) {
            mRecorder!!.complete()
        }
        cleanPath()
    }

    /**
     * 停止录音
     */
    fun stopFullRecord() {
        mRecorder?.complete()
    }

    override fun needPermission() {
        callBack?.needPermission()
    }

    override fun onStart() {
        callBack?.onStart()
    }

    override fun onResume() {
        callBack?.onResume()
    }

    override fun onReset() {
        callBack?.onReset()
    }

    override fun onRecording(time: Long, volume: Int) {
        callBack?.onRecording(startTime + time, volume)
    }

    override fun onPause() {
        callBack?.onPause()
    }

    override fun onRemind(duration: Long) {
        callBack?.onRemind(duration)
    }

    override fun onSuccess(isAutoComplete: Boolean, file: String, time: Long) {
        callBack?.onSuccess(isAutoComplete, file, time)
    }

    override fun onMaxChange(time: Long) {
        callBack?.onMaxChange(time)
    }

    override fun onError(e: Exception) {
        resolveError()
        e.printStackTrace()
        callBack?.onError(e)
    }

    fun setVolume(volume: Float) {
        mRecorder?.setBGMVolume(volume)
    }

    fun destroy() {
        mRecorder?.destroy()
    }
}
