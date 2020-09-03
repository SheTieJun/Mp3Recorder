package me.shetj.recorder.core

import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import me.shetj.player.PlayerListener
import java.io.File
import kotlin.math.sqrt


abstract class BaseRecorder {
    private val TAG = this.javaClass.simpleName
    //region 录音的方式 /来源 Record Type
    enum class RecorderType(name: String) {
        SIM("Mp3Recorder"), //
        MIX("MixRecorder")
    }

    enum class AudioSource(var type: Int) {
        MIC(MediaRecorder.AudioSource.MIC),
        VOICE_COMMUNICATION(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
    }

    /**
     *   MONO(1), //单声道
     *   STEREO(2) //双声道
     */
    enum class AudioChannel(var type: Int) {
        MONO(1), //单声道
        STEREO(2) //双声道
    }
    //endregion Record Type


    //region 录音的状态，声音和时间
    protected var mVolume: Int = 0

    var isRecording = false
        protected set

    //当前状态
    var state = RecordState.STOPPED
        protected set

    //录制时间
    var duration = 0L
        protected set
    var isPause: Boolean = false
    private var isDebug = false
    //endregion 录音的状态和时间

    //region public method 公开的方法
    abstract val realVolume: Int

    //设置是否使用耳机配置方式
    abstract fun setContextToPlugConfig(context: Context): BaseRecorder

    abstract fun setContextToVolumeConfig(context: Context): BaseRecorder

    //设置输出路径
    abstract fun setOutputFile(outputFile: String, isContinue: Boolean = false): BaseRecorder

    //设置输出路径
    abstract fun setOutputFile(outputFile: File, isContinue: Boolean = false): BaseRecorder

    //设置录音监听
    abstract fun setRecordListener(recordListener: RecordListener?): BaseRecorder

    //设置权限监听
    abstract fun setPermissionListener(permissionListener: PermissionListener?): BaseRecorder

    //设计背景音乐的url,本地的
    abstract fun setBackgroundMusic(url: String): BaseRecorder

    //设置背景音乐的监听
    abstract fun setBackgroundMusicListener(listener: PlayerListener): BaseRecorder

    //初始Lame录音输出质量
    abstract fun setMp3Quality(mp3Quality: Int): BaseRecorder

    //设置比特率，关系声音的质量
    abstract fun setMp3BitRate(mp3BitRate: Int): BaseRecorder

    //设置采样率
    abstract fun setSamplingRate(rate: Int): BaseRecorder

    //初始最大录制时间
    abstract fun setMaxTime(mMaxTime: Int): BaseRecorder

    //设置增强系数
    abstract fun setWax(wax: Float): BaseRecorder

    //设置背景声音大小
    abstract fun setVolume(volume: Float): BaseRecorder

    //开始录音
    abstract fun start()

    //停止录音
    abstract fun stop()

    //重新开始录音
    abstract fun onResume()

    //替换输出文件
    abstract fun updateDataEncode(outputFilePath: String)

    //暂停录音
    abstract fun onPause()

    //开始播放音乐
    abstract fun startPlayMusic()

    //是否在播放音乐
    abstract fun isPauseMusic(): Boolean

    //暂停背景音乐
    abstract fun pauseMusic()

    //重新播放背景音乐
    abstract fun resumeMusic()

    //重置
    abstract fun onReset()

    //结束释放
    abstract fun onDestroy()
    //endregion public method

    //region  计算真正的时间，如果过程中有些数据太小，就直接置0，防止噪音

    fun setDebug(isDebug: Boolean): BaseRecorder {
        this.isDebug = isDebug
        return this
    }

    protected fun calculateRealVolume(buffer: ShortArray, readSize: Int) {
        var sum = 0.0
        for (i in 0 until readSize) {
            // 这里没有做运算的优化，为了更加清晰的展示代码
            sum += (buffer[i] * buffer[i]).toDouble()
        }
        if (readSize > 0) {
            val amplitude = sum / readSize
            mVolume = sqrt(amplitude).toInt()
            if (mVolume < 5) {
                for (i in 0 until readSize) {
                    buffer[i] = 0
                }
            }
        }
    }

    protected fun calculateRealVolume(buffer: ByteArray) {
        val shorts = BytesTransUtil.bytes2Shorts(buffer)
        val readSize = shorts.size
        calculateRealVolume(shorts, readSize)
    }

    protected fun logInfo(info: String) {
        if (isDebug) {
            Log.d(TAG, info)
        }
    }
    //endregion  计算真正的时间，如果过程中有些数据太小，就直接置0，防止噪音
}
