package me.shetj.mp3recorder.record.utils

import android.text.TextUtils
import me.shetj.base.tools.file.SDCardUtils
import me.shetj.kt.simpleRecorderBuilder
import me.shetj.player.PermissionListener
import me.shetj.player.PlayerListener
import me.shetj.player.RecordListener
import me.shetj.recorder.mixRecorder.MixRecorder
import me.shetj.recorder.mixRecorder.PlayBackMusic
import me.shetj.recorder.simRecorder.BaseRecorder
import me.shetj.recorder.simRecorder.RecordState
import me.shetj.recorder.util.FileUtils

/**
 * 录音工具类
 */
class MixRecordUtils(private val callBack: RecordCallBack?
) : RecordListener, PermissionListener {
    val isRecording: Boolean
        get() {
            return if (mRecorder !=null) {
                mRecorder?.isRecording!! && !mRecorder?.isPause!!
            }else{
                false
            }
        }
    fun hasRecord(): Boolean {
        return  if (mRecorder !=null) {
            mRecorder?.duration!!> 0 && mRecorder!!.state != RecordState.STOPPED
        }else{
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
    fun startOrPause(file :String = "") {
        if (mRecorder == null) {
            initRecorder()
        }
        when (mRecorder?.state) {
            RecordState.STOPPED -> {
                if (TextUtils.isEmpty(file)) {
                    val mRecordFile = SDCardUtils.getPath("record") + "/" + System.currentTimeMillis() + ".mp3"
                    this.saveFile = mRecordFile
                }else{
                    this.saveFile = file
                }
                mRecorder?.onReset()
                mRecorder?.setOutputFile(saveFile,!TextUtils.isEmpty(file))
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

    /**
     * VOICE_COMMUNICATION 消除回声和噪声问题
     * MIC 麦克风- 因为有噪音问题
     */
    private fun initRecorder() {
        mRecorder =  simpleRecorderBuilder(
            mMaxTime = 3600 * 1000,
            mp3Quality = 1,
            isDebug = true,
            recordListener = this,
            permissionListener = this)
    }

    fun isPause():Boolean{
        return  mRecorder?.state == RecordState.PAUSED
    }

    fun setBackgroundPlayerListener(listener : PlayerListener) {
        mRecorder?.setBackgroundMusicListener(listener)
    }

    fun getBgPlayer(): PlayBackMusic {
        return (mRecorder!! as MixRecorder).bgPlayer
    }

    fun  pause(){
        mRecorder?.onPause()
    }
    fun clear() {
        mRecorder?.onDestroy()
    }

    fun reset() {
        mRecorder?.onReset()
    }
    /**
     * 设置开始录制时间
     * @param startTime 已经录制的时间
     */
    fun setTime(startTime: Long) {
        this.startTime = startTime
        setMaxTime((3600000 - startTime).toInt())
        callBack?.onRecording((startTime/1000).toInt(),0)
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
            mRecorder!!.stop()
        }
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
        callBack?.start()
    }

    override fun onResume() {
        callBack?.start()
    }

    override fun onReset() {
    }

    override fun onRecording(time: Long, volume: Int) {
        callBack?.onRecording(((startTime + time)/1000).toInt(),volume)
    }

    override fun onPause() {
        callBack?.pause()
    }

    override fun onRemind(mDuration: Long) {

    }

    override fun onSuccess(file: String, time: Long) {
        callBack?.onSuccess(file, (time/1000).toInt())
    }

    override fun setMaxProgress(time: Long) {
        callBack?.onMaxProgress((3600).toInt())
    }

    override fun onError(e: Exception) {
        resolveError()
        callBack?.onError(e)
    }

    override fun autoComplete(file: String, time: Long) {
        callBack?.autoComplete(file,  (time/1000).toInt())
    }

    fun setVolume(volume: Float) {
        mRecorder?.setVolume(volume)
    }


}
