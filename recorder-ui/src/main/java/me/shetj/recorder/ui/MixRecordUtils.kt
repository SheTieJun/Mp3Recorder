package me.shetj.recorder.ui

import android.text.TextUtils
import me.shetj.base.S
import me.shetj.base.tools.file.EnvironmentStorage
import me.shetj.player.PlayerListener
import me.shetj.recorder.core.*
import me.shetj.recorder.mixRecorder.MixRecorder
import me.shetj.recorder.mixRecorder.PlayBackMusic
import me.shetj.recorder.mixRecorder.mixRecorder

/**
 * 录音工具类
 */
class MixRecordUtils(
    private val maxTime: Int = 30 * 60 * 1000,
    private val callBack: SimRecordListener?
) : RecordListener, PermissionListener {

    val isRecording: Boolean
        get() {
            return if (mRecorder != null) {
                mRecorder?.isRecording!! && !mRecorder?.isPause!!
            } else {
                false
            }
        }

    fun hasRecord(): Boolean {
        return if (mRecorder != null) {
            mRecorder?.duration!! > 0 && mRecorder!!.state != RecordState.STOPPED
        } else {
            false
        }
    }

    init {
        initRecorder()
    }

    private var startTime: Long = 0 //秒 s
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
                    val mRecordFile =
                        EnvironmentStorage.getPath(packagePath = "record") + "/" + System.currentTimeMillis() + ".mp3"
                    this.saveFile = mRecordFile
                } else {
                    this.saveFile = file
                }
                mRecorder?.onReset()
                mRecorder?.setOutputFile(saveFile!!, !TextUtils.isEmpty(file))
                mRecorder?.start()
            }
            RecordState.PAUSED -> {
                mRecorder?.onResume()
            }
            RecordState.RECORDING -> {
                mRecorder?.onPause()
            }
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
                        val mRecordFile =
                            EnvironmentStorage.getPath(packagePath = "record") + "/" + System.currentTimeMillis() + ".mp3"
                        this.saveFile = mRecordFile
                    } else {
                        this.saveFile = file
                    }
                } else {
                    this.saveFile = file
                }
                mRecorder?.onReset()
                mRecorder?.setOutputFile(saveFile!!, !TextUtils.isEmpty(file))
                mRecorder?.start()
            }
            RecordState.RECORDING -> {
                mRecorder?.stop()
            }
        }
    }

    /**
     * VOICE_COMMUNICATION 消除回声和噪声问题
     * MIC 麦克风- 因为有噪音问题
     */
    private fun initRecorder() {
        mRecorder = mixRecorder(
                S.app,
                recordListener = this,
                permissionListener = this,
                isDebug = false
        )
        mRecorder?.setMaxTime(maxTime, 60 * 1000)
    }

    fun isPause(): Boolean {
        return mRecorder?.state == RecordState.PAUSED
    }

    fun setBackgroundPlayerListener(listener: PlayerListener) {
        mRecorder?.setBackgroundMusicListener(listener)
    }

    fun getBgPlayer(): PlayBackMusic {
        return (mRecorder!! as MixRecorder).bgPlayer
    }

    fun pause() {
        mRecorder?.onPause()
    }

    fun clear() {
        mRecorder?.onDestroy()
    }

    fun reset() {
        mRecorder?.onReset()
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
        if (mRecorder != null && mRecorder!!.isRecording) {
            mRecorder!!.stop()
        }
        cleanPath()
    }

    /**
     * 停止录音
     */
    fun stopFullRecord() {
        mRecorder?.stop()
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

    override fun onSuccess(file: String, time: Long) {
        callBack?.onSuccess(file, time)
    }

    override fun onMaxChange(time: Long) {
        callBack?.onMaxChange(time)
    }

    override fun onError(e: Exception) {
        resolveError()
        e.printStackTrace()
        callBack?.onError(e)
    }

    override fun autoComplete(file: String, time: Long) {
        callBack?.autoComplete(file, time)
    }

    fun setVolume(volume: Float) {
        mRecorder?.setVolume(volume)
    }

    fun destroy() {
        mRecorder?.onDestroy()
    }


}
