package me.shetj.mp3recorder.record.utils

import android.content.Context
import android.media.AudioManager
import android.media.MediaRecorder
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.MutableLiveData
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import me.shetj.base.BaseKit
import me.shetj.base.ktx.logD
import me.shetj.base.ktx.logI
import me.shetj.base.tools.app.Utils
import me.shetj.base.tools.file.EnvironmentStorage
import me.shetj.mp3recorder.record.RecordingNotification
import me.shetj.ndk.lame.LameUtils
import me.shetj.player.PlayerListener
import me.shetj.recorder.core.*
import me.shetj.recorder.mixRecorder.buildMix
import me.shetj.recorder.simRecorder.buildSim
import me.shetj.recorder.soundtouch.buildST
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * 录音工具类
 */
class RecordUtils(
    private val callBack: SimRecordListener?
) : RecordListener, PermissionListener, PCMListener {

    private var bgmUrl: Uri? = null
    private var listener: PlayerListener? = null
    private var recorderType: BaseRecorder.RecorderType = BaseRecorder.RecorderType.MIX

    val TIME = 5 * 60 * 1000L

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
    val recorderLiveDate: MutableLiveData<BaseRecorder.RecorderType> = MutableLiveData()

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
                saveFile.logI()
                mRecorder?.setOutputFile(saveFile, !TextUtils.isEmpty(file))
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

    fun showChangeDialog(context: Context) {
        MaterialAlertDialogBuilder(context)
            .setTitle("切换录音工具")
            .setSingleChoiceItems(arrayOf("MixRecorder", "SimRecorder", "STRecorder"), getSelectPosition(recorderType))
            { dialog, which ->
                updateRecorderType(which)
                dialog.dismiss()
            }.show()
    }

    fun getRecorderTypeName(): String {
        return when (recorderType) {
            BaseRecorder.RecorderType.MIX -> "MixRecorder"
            BaseRecorder.RecorderType.SIM -> "SimRecorder"
            BaseRecorder.RecorderType.ST -> "STRecorder"
        }
    }

    private fun getSelectPosition(recorderType: BaseRecorder.RecorderType): Int {
        return when (recorderType) {
            BaseRecorder.RecorderType.MIX -> 0
            BaseRecorder.RecorderType.SIM -> 1
            BaseRecorder.RecorderType.ST -> 2
        }
    }

    fun updateRecorderType(position: Int) {
        val recorderType = when (position) {
            0 -> BaseRecorder.RecorderType.MIX
            1 -> BaseRecorder.RecorderType.SIM
            2 -> BaseRecorder.RecorderType.ST
            else -> BaseRecorder.RecorderType.MIX
        }
        updateRecorderType(recorderType)
    }

    /**
     * 更新录音模式
     */
    private fun updateRecorderType(recorderType: BaseRecorder.RecorderType) {
        if (hasRecord()) {
            mRecorder?.complete()
        }
        "updateRecorderType:${getRecorderTypeName()}".logI()
        this.recorderType = recorderType
        recorderLiveDate.postValue(recorderType)
        initRecorder()
    }

    private fun initRecorder() {
        mRecorder = recorder {
            mMaxTime = 5 * 60 * 1000
            isDebug = true
            samplingRate = 48000
            audioSource = MediaRecorder.AudioSource.MIC
            audioChannel = 1
            mp3BitRate = 128
            mp3Quality = 5
            recordListener = this@RecordUtils
            permissionListener = this@RecordUtils
            pcmListener = this@RecordUtils
        }.let {
            when (recorderType) {
                BaseRecorder.RecorderType.MIX -> it.buildMix(Utils.app)
                    .also {
                        it.isEnableVBR(false) // 请不要使用，虽然可以正常播放，但是会时间错误获取会错误，暂时没有解决方法
                        it.setFilter(3000, 200)
                    }

                BaseRecorder.RecorderType.SIM -> it.buildSim(Utils.app)
                BaseRecorder.RecorderType.ST -> it.buildST()
            }
        }

        if (recorderType == BaseRecorder.RecorderType.ST) {
            mRecorder!!.getSoundTouch().changeUse(true)
            mRecorder!!.getSoundTouch().setPitchSemiTones(10f) //往女声变
//            mRecorder!!.getSoundTouch().setRateChange(50f) //加速，会导致录音计时> 实际时间
            Toast.makeText(BaseKit.app, "变声，不可以使用背景音乐", Toast.LENGTH_LONG).show()
        }
        mRecorder?.setMaxTime(TIME, TIME - 20 * 1000)
        listener?.let { setBackgroundPlayerListener(it) }
        bgmUrl?.let { setBackGroundUrl(Utils.app, it) }
    }

    fun startOrPauseBGM() {
        mRecorder?.let { recorder ->
            if (recorder.isPlayMusic()) {
                if (recorder.isPauseMusic()) {
                    recorder.resumeMusic()
                } else {
                    recorder.pauseMusic()
                }
            } else {
                recorder.startPlayMusic()
            }
        }
    }

    fun setBackgroundPlayerListener(listener: PlayerListener) {
        this.listener = listener
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
        mRecorder?.setCurDuration(startTime)
        callBack?.onRecording(startTime, -1)
    }

    /**
     * 设置最大录制时间
     */
    fun setMaxTime(maxTime: Int) {
        mRecorder?.setMaxTime(maxTime.toLong())
    }

    /**
     * 录音异常
     */
    private fun resolveError() {
        if (mRecorder != null && mRecorder!!.isActive) {
            mRecorder!!.complete()
        }
        FileUtils.deleteFile(saveFile)
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
        RecordingNotification.notify(Utils.app, RecordingNotification.RECORD_NOTIFICATION_RECORD_ING)
        callBack?.onStart()
    }

    override fun onResume() {
        RecordingNotification.notify(Utils.app, RecordingNotification.RECORD_NOTIFICATION_RECORD_ING)
        callBack?.onStart()
    }

    override fun onReset() {
    }

    override fun onRecording(time: Long, volume: Int) {
        callBack?.onRecording((startTime + time), volume)
    }

    override fun onPause() {
        RecordingNotification.notify(Utils.app, RecordingNotification.RECORD_NOTIFICATION_RECORD_PAUSE)
        callBack?.onPause()
    }

    override fun onRemind(duration: Long) {
        callBack?.onRemind(duration)
    }

    override fun onSuccess(isAutoComplete: Boolean, file: String, time: Long) {
        RecordingNotification.notify(Utils.app, RecordingNotification.RECORD_NOTIFICATION_RECORD_COMPLETE)
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
            this.bgmUrl = url
            AudioUtils.getAudioChannel(context, url).toString().logI()
            mRecorder!!.setAudioChannel(AudioUtils.getAudioChannel(context, url))
            mRecorder!!.setBackgroundMusic(context, url, null)
        }
    }

    override fun onBeforePCMToMp3(pcm: ShortArray): ShortArray {
        val pcmdb = calculateRealVolume(pcm, pcm.size)
        "修改PCM前DB:$pcmdb".logD("onBeforePCMToMp3")
        val adjustVoice = BytesTransUtil.adjustVoice(pcm, 3)
        val afterdb = calculateRealVolume(adjustVoice, adjustVoice.size)
        "修改PCM后DB:$afterdb".logD("onBeforePCMToMp3")
        return adjustVoice
    }


    private fun calculateRealVolume(buffer: ShortArray, readSize: Int): Int {
        var sum = 0.0
        var mVolume = 0
        for (i in 0 until readSize) {
            // 这里没有做运算的优化，为了更加清晰的展示代码
            sum += abs((buffer[i] * buffer[i]).toDouble())
        }
        if (readSize > 0) {
            mVolume = (log10(sqrt(sum / readSize)) * 20).toInt()
            if (mVolume < 0) {
                mVolume = 0
            } else if (mVolume > 100) {
                mVolume = 100
            }
        }
        return mVolume
    }
}
