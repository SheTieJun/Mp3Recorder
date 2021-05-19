package me.shetj.recorder.core

import android.content.Context
import android.media.AudioFormat
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.net.Uri
import android.util.Log
import me.shetj.player.PlayerListener
import java.io.File
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt


abstract class BaseRecorder {
    private val TAG = this.javaClass.simpleName
    //region 录音的方式 /来源 Record Type
    enum class RecorderType {
        SIM, //
        MIX
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
    protected var defaultAudioSource = MediaRecorder.AudioSource.VOICE_COMMUNICATION
    protected var defaultChannelConfig = AudioFormat.CHANNEL_IN_MONO
    protected var defaultLameInChannel = 1 //声道数量
    protected var defaultLameMp3Quality = 3 //音频质量，好像LAME已经不使用它了
    protected var defaultLameMp3BitRate = 96 //32 太低，96,128 比较合适
    protected var defaultSamplingRate = 44100
    protected var is2Channel = false //默认是双声道
    protected var mRecordFile: File? = null //文件输出，中途可以替换
    protected var mRecordListener: RecordListener? = null
    protected var mPermissionListener: PermissionListener? = null

    //region 系统自带的去噪音，增强以及回音问题
    private var mNoiseSuppressor: NoiseSuppressor? = null
    private var mAcousticEchoCanceler: AcousticEchoCanceler? = null
    private var mAutomaticGainControl: AutomaticGainControl? = null
    //endregion 系统自带的去噪音，增强以及回音问题

    //最大时间
    protected var mMaxTime: Long = 3600000

    //提醒时间
    protected var mRemindTime = (3600000 - 10000).toLong()
    protected var isAutoComplete = false
    //region 录音的状态，声音和时间
    protected var mVolume: Int = 0
    protected var backgroundMusicIsPlay: Boolean = false //记录是否暂停
    protected var bgmIsLoop: Boolean = false
    protected var isRemind: Boolean = true
    protected var isContinue = false //是否继续录制
    //是否暂停
    protected var isPause: Boolean = true
    private var isDebug = false
    //声音增强,不建议使用
    protected var wax = 1f
    protected var bgLevel: Float = 03f
    var isRecording = false
        protected set
    //当前状态
    var state = RecordState.STOPPED
        protected set
    //录制时间
    var duration = 0L
        protected set
    //endregion 录音的状态和时间

    //region public method 公开的方法
    val realVolume: Int
        get() = mVolume
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

    abstract fun setLoopMusic(isLoop:Boolean):BaseRecorder

    //背景音乐的url,兼容Android Q
    abstract fun setBackgroundMusic(context: Context,uri: Uri,header:MutableMap<String,String>?): BaseRecorder

    //设置背景音乐的监听
    abstract fun setBackgroundMusicListener(listener: PlayerListener): BaseRecorder

    //初始Lame录音输出质量
    abstract fun setMp3Quality(mp3Quality: Int): BaseRecorder

    //设置比特率，关系声音的质量
    abstract fun setMp3BitRate(mp3BitRate: Int): BaseRecorder

    //设置采样率
    abstract fun setSamplingRate(rate: Int): BaseRecorder

    //初始最大录制时间 和提醒时间 remind = maxTime - remindDiffTime
    abstract fun setMaxTime(maxTime: Int, remindDiffTime: Int? = null): BaseRecorder

    //设置增强系数
    abstract fun setWax(wax: Float): BaseRecorder

    //设置背景声音大小
    abstract fun setVolume(volume: Float): BaseRecorder

    //开始录音
    abstract fun start()

    //完成录音
    abstract fun complete()

    //重新开始录音
    abstract fun resume()

    //替换输出文件
    abstract fun updateDataEncode(outputFilePath: String)

    //暂停录音
    abstract fun pause()

    //是否设置了并且开始播放了背景音乐
    abstract fun isPlayMusic():Boolean

    //开始播放音乐
    abstract fun startPlayMusic()

    //是否在播放音乐
    abstract fun isPauseMusic(): Boolean

    //暂停背景音乐
    abstract fun pauseMusic()

    //重新播放背景音乐
    abstract fun resumeMusic()

    //重置
    abstract fun reset()

    //结束释放
    abstract fun destroy()
    //endregion public method

    //region  计算真正的时间，如果过程中有些数据太小，就直接置0，防止噪音

    fun setDebug(isDebug: Boolean): BaseRecorder {
        this.isDebug = isDebug
        return this
    }

    /**
     *
     * 求得平均值之后，如果是平方和则代入常数系数为10的公式中，
     *
     * 如果是绝对值的则代入常数系数为20的公式中，算出分贝值。
     */
    protected fun calculateRealVolume(buffer: ShortArray, readSize: Int) {
        var sum = 0.0
        for (i in 0 until readSize) {
            // 这里没有做运算的优化，为了更加清晰的展示代码
            sum += abs((buffer[i] * buffer[i]).toDouble())
        }
        if (readSize > 0) {
            mVolume = (log10(sqrt(sum / readSize)) *20).toInt()
            if (mVolume < 5) {
                for (i in 0 until readSize) {
                    buffer[i] = 0
                }
            }else if (mVolume > 100){
                mVolume = 100
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

    protected fun initAEC(mAudioSessionId: Int) {
        if (mAudioSessionId != 0) {
            if (NoiseSuppressor.isAvailable()) {
                if (mNoiseSuppressor != null) {
                    mNoiseSuppressor!!.release()
                    mNoiseSuppressor = null
                }

                mNoiseSuppressor = NoiseSuppressor.create(mAudioSessionId)
                if (mNoiseSuppressor != null) {
                    mNoiseSuppressor!!.enabled = true
                } else {
                    Log.i(TAG, "Failed to create NoiseSuppressor.")
                }
            } else {
                Log.i(TAG, "Doesn't support NoiseSuppressor")
            }

            if (AcousticEchoCanceler.isAvailable()) {
                if (mAcousticEchoCanceler != null) {
                    mAcousticEchoCanceler!!.release()
                    mAcousticEchoCanceler = null
                }

                mAcousticEchoCanceler = AcousticEchoCanceler.create(mAudioSessionId)
                if (mAcousticEchoCanceler != null) {
                    mAcousticEchoCanceler!!.enabled = true
                    // mAcousticEchoCanceler.setControlStatusListener(listener)setEnableStatusListener(listener)
                } else {
                    Log.i(TAG, "Failed to initAEC.")
                    mAcousticEchoCanceler = null
                }
            } else {
                Log.i(TAG, "Doesn't support AcousticEchoCanceler")
            }

            if (AutomaticGainControl.isAvailable()) {
                if (mAutomaticGainControl != null) {
                    mAutomaticGainControl!!.release()
                    mAutomaticGainControl = null
                }

                mAutomaticGainControl = AutomaticGainControl.create(mAudioSessionId)
                if (mAutomaticGainControl != null) {
                    mAutomaticGainControl!!.enabled = true
                } else {
                    Log.i(TAG, "Failed to create AutomaticGainControl.")
                }

            } else {
                Log.i(TAG, "Doesn't support AutomaticGainControl")
            }
        }

    }

    protected fun release() {
        if (null != mAcousticEchoCanceler) {
            mAcousticEchoCanceler!!.enabled = false
            mAcousticEchoCanceler!!.release()
            mAcousticEchoCanceler = null
        }
        if (null != mAutomaticGainControl) {
            mAutomaticGainControl!!.enabled = false
            mAutomaticGainControl!!.release()
            mAutomaticGainControl = null
        }
        if (null != mNoiseSuppressor) {
            mNoiseSuppressor!!.enabled = false
            mNoiseSuppressor!!.release()
            mNoiseSuppressor = null
        }
    }

    //endregion  计算真正的时间，如果过程中有些数据太小，就直接置0，防止噪音
}
