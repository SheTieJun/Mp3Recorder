package me.shetj.mp3recorder.record.utils

import android.media.AudioFormat
import android.media.MediaRecorder
import me.shetj.base.ktx.logi
import me.shetj.base.tools.app.ArmsUtils
import me.shetj.base.tools.app.Utils
import me.shetj.base.tools.file.EnvironmentStorage
import me.shetj.base.tools.file.FileUtils
import me.shetj.base.tools.json.EmptyUtils
import me.shetj.player.PlayerListener
import me.shetj.recorder.core.*
import me.shetj.recorder.simRecorder.buildSim
import java.io.File

class RecordUtils(
    private val callBack: RecordCallBack?
) : RecordListener, PermissionListener {
    val isRecording: Boolean
        get() {
            return if (mRecorder != null) {
                mRecorder?.isActive!!
            } else {
                false
            }
        }

    fun hasRecord(): Boolean {
        return if (mRecorder != null) {
            mRecorder?.duration!! - startTime > 0 && mRecorder!!.state != RecordState.STOPPED
        } else {
            false
        }
    }

    init {
        initRecorder()
    }

    private var startTime: Int = 0
    private var mRecorder: BaseRecorder? = null
    private var saveFile = ""


    @JvmOverloads
    fun startOrPause(file: String = "", isContinue: Boolean = false) {
        if (mRecorder == null) {
            initRecorder()
        }
        when (mRecorder?.state) {
            RecordState.STOPPED -> {
                if (EmptyUtils.isEmpty(file)) {
                    val mRecordFile =
                        EnvironmentStorage.getPath(packagePath = "record") + "/" + System.currentTimeMillis() + ".mp3"
                    this.saveFile = mRecordFile
                } else {
                    this.saveFile = file
                }
                mRecorder?.setOutputFile(saveFile, isContinue)
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
        mRecorder = recorder {
            isDebug = true
            recordListener = this@RecordUtils
            permissionListener = this@RecordUtils
            audioChannel = AudioFormat.CHANNEL_IN_STEREO
            audioSource =  MediaRecorder.AudioSource.MIC
        }.buildSim(Utils.app)
    }

    fun setBackgroundMusic(url: String) {
        mRecorder!!.setAudioChannel(AudioUtils.getAudioChannel(url))
        AudioUtils.getAudioChannel(url).toString().logi()
        mRecorder?.setBackgroundMusic(url)
    }

    fun setBackgroundPlayerListener(listener: PlayerListener) {
        mRecorder?.setBackgroundMusicListener(listener)
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
    fun setTime(startTime: Int) {
        this.startTime = startTime
        callBack?.onRecording(startTime, 0)
    }

    /**
     * 设置最大录制时间
     */
    fun setMaxTime(maxTime: Int) {
        mRecorder?.setMaxTime(maxTime * 1000)
    }

    /**
     * 录音异常
     */
    private fun resolveError() {
        FileUtils.deleteFile(File(saveFile))
        if (  mRecorder!!.isActive) {
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
        callBack?.start()
    }

    override fun onResume() {
        callBack?.start()
    }

    override fun onReset() {
        callBack?.start()
    }

    override fun onRecording(time: Long, volume: Int) {
        callBack?.onRecording(startTime + (time / 1000).toInt(), volume)
    }

    override fun onPause() {
        callBack?.pause()
    }

    override fun onRemind(duration: Long) {
        ArmsUtils.makeText("已录制" + duration / 60000 + "分钟，本条录音还可以继续录制10秒")
    }

    override fun onSuccess(isAutoComplete:Boolean,file: String, time: Long) {
        if (isAutoComplete){
            callBack?.autoComplete(file, (time / 1000).toInt())
        }else {
            callBack?.onSuccess(file, (time / 1000).toInt())
        }
    }

    override fun onMaxChange(time: Long) {
        callBack?.onMaxProgress((time / 1000).toInt())
    }

    override fun onError(e: Exception) {
        resolveError()
        callBack?.onError(e)
    }


}
