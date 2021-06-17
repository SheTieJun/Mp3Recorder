package me.shetj.mp3recorder.record.utils

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import me.shetj.base.ktx.logi
import me.shetj.base.tools.app.Utils
import me.shetj.base.tools.file.EnvironmentStorage
import me.shetj.player.PlayerListener
import me.shetj.recorder.core.*
import me.shetj.recorder.mixRecorder.mixRecorder

/**
 * 录音工具类
 */
class MixRecordUtils(
    private val callBack: SimRecordListener?
) : RecordListener, PermissionListener {

    val TIME = 2 * 60 * 1000

    val isRecording: Boolean
        get() {
            return mRecorder?.state == RecordState.RECORDING
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
    private var saveFile = ""

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
                mRecorder?.setOutputFile(saveFile, !TextUtils.isEmpty(file))
                mRecorder?.start()
            }
            RecordState.PAUSED -> {
                mRecorder?.resume()
            }
            RecordState.RECORDING -> {
                mRecorder?.pause()
            }
        }
    }

    /**
     * VOICE_COMMUNICATION 消除回声和噪声问题
     * MIC 麦克风- 因为有噪音问题
     */
    private fun initRecorder() {
        mRecorder = mixRecorder(
            Utils.app,
            mMaxTime = TIME,
            isDebug = true,
            recordListener = this,
            permissionListener = this
        )
        mRecorder?.setMaxTime(TIME, TIME - 20 * 1000)
    }

    fun isPause(): Boolean {
        return mRecorder?.state == RecordState.PAUSED
    }

    fun startOrPauseBGM() {
        if (mRecorder?.isPlayMusic() == true) {
            if (mRecorder?.isPauseMusic() == true) {
                mRecorder?.resumeMusic()
            } else {
                mRecorder?.pauseMusic()
            }
        } else {
            mRecorder?.startPlayMusic()
        }
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

    /**
     * 设置开始录制时间
     * @param startTime 已经录制的时间
     */
    fun setTime(startTime: Long) {
        this.startTime = startTime
        setMaxTime((TIME - startTime).toInt())
        callBack?.onRecording((startTime / 1000), 0)
    }

    /**
     * 设置最大录制时间
     */
    fun setMaxTime(maxTime: Int) {
        mRecorder?.setMaxTime(maxTime)
    }

    /**
     * 录音异常
     */
    private fun resolveError() {
        FileUtils.deleteFile(saveFile)
        if (mRecorder != null && mRecorder!!.isRecording) {
            mRecorder!!.complete()
        }
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
        callBack?.onStart()
    }

    override fun onReset() {
    }

    override fun onRecording(time: Long, volume: Int) {
        callBack?.onRecording(((startTime + time) / 1000), volume)
    }

    override fun onPause() {
        callBack?.onPause()
    }

    override fun onRemind(duration: Long) {
        callBack?.onRemind(duration)
    }

    override fun onSuccess(isAutoComplete: Boolean, file: String, time: Long) {
        callBack?.onSuccess(isAutoComplete, file, (time / 1000))
    }

    override fun onMaxChange(time: Long) {
        callBack?.onMaxChange(time / 1000)
    }

    override fun onError(e: Exception) {
        resolveError()
        callBack?.onError(e)
    }

    fun setVolume(volume: Float) {
        mRecorder?.setBGMVolume(volume)
    }

    fun setBackGroundUrl(context: Context?, url: Uri) {
        if (context != null) {
            AudioUtils.getAudioChannel(context,url).toString().logi()
            mRecorder!!.setAudioChannel(AudioUtils.getAudioChannel(context,url))
            mRecorder!!.setBackgroundMusic(context, url, null)
        }
    }

}
