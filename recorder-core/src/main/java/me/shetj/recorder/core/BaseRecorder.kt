package me.shetj.recorder.core

import android.content.Context
import android.media.AudioFormat
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.TextUtils
import android.util.Log
import androidx.annotation.IntRange
import me.shetj.player.PlayerListener
import java.io.File
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt


abstract class BaseRecorder {

    //region 录音的方式 /来源 Record Type
    enum class RecorderType {
        SIM, //
        MIX
    }

    //endregion Record Type
    protected var defaultAudioSource = MediaRecorder.AudioSource.VOICE_COMMUNICATION
    protected var defaultChannelConfig = AudioFormat.CHANNEL_IN_MONO // defaultLameInChannel =1
    protected var defaultLameInChannel = 1 //声道数量
    protected var defaultLameMp3Quality = 3 //音频质量，好像LAME已经不使用它了
    /*
    * 16Kbps= 电话音质
    * 24Kbps= 增加电话音质、短波广播、长波广播、欧洲制式中波广播
    * 40Kbps= 美国制式中波广播
    * 56Kbps= 话音
    * 64Kbps= 增加话音（手机铃声最佳比特率设定值、手机单声道MP3播放器最佳设定值）
    * 112Kbps= FM调频立体声广播
    * 128Kbps= 磁带（手机立体声MP3播放器最佳设定值、低档MP3播放器最佳设定值）
    * 160Kbps= HIFI 高保真（中高档MP3播放器最佳设定值）
    * 192Kbps= CD（高档MP3播放器最佳设定值）
    * 256Kbps= Studio音乐工作室（音乐发烧友适用）
    * 实际上随着技术的进步，比特率也越来越高，MP3的最高比特率为320Kbps，但一些格式可以达到更高的比特率和更高的音质。
    * 比如正逐渐兴起的APE音频格式，能够提供真正发烧级的无损音质和相对于WAV格式更小的体积，其比特率通常为550kbps-----950kbps。
     */
    protected var defaultLameMp3BitRate = 96 //32 太低，(96,128) 比较合适
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
    protected var recordThread: Thread? = null
    protected var isPause: Boolean = true
    private var isDebug = false

    //声音增强,不建议使用
    protected var wax = 1f
    protected var bgLevel: Float = 03f
    // 录音Recorder 是否在活动,暂停的时候 isActive 还是true,只有录音结束了才会为false
    var isActive = false
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

    protected val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                HANDLER_RECORDING -> {
                    if (mRecordListener != null && state == RecordState.RECORDING) {
                        logInfo("Recording:  mDuration = $duration ,volume = $realVolume and state is recording  = ${state == RecordState.RECORDING}")
                        //录制回调
                        mRecordListener!!.onRecording(duration, realVolume)
                        //提示快到录音时间了
                        if (isRemind && duration > mRemindTime) {
                            isRemind = false
                            mRecordListener!!.onRemind(duration)
                        }
                    }
                }
                HANDLER_START -> {
                    logInfo("started:  mDuration = $duration , mRemindTime = $mRemindTime")
                    if (mRecordListener != null) {
                        mRecordListener!!.onStart()
                    }
                }
                HANDLER_RESUME -> {
                    logInfo("resume:  mDuration = $duration")
                    if (mRecordListener != null) {
                        mRecordListener!!.onResume()
                    }
                }
                HANDLER_COMPLETE -> {
                    logInfo("complete: mDuration = $duration")
                    if (mRecordListener != null && mRecordFile != null) {
                        mRecordListener!!.onSuccess(false, mRecordFile!!.absolutePath, duration)
                    }
                }
                HANDLER_AUTO_COMPLETE -> {
                    logInfo("auto complete: mDuration = $duration")
                    if (mRecordListener != null && mRecordFile != null) {
                        mRecordListener!!.onSuccess(true, mRecordFile!!.absolutePath, duration)
                    }
                }
                HANDLER_ERROR -> {
                    logInfo("error : mDuration = $duration")
                    if (mRecordListener != null) {
                        if (msg.obj != null){
                            mRecordListener!!.onError(msg.obj as Exception)
                        }else{
                            mRecordListener!!.onError(Exception("record error：AudioRecord read MIC error maybe not permission!"))
                        }
                    }
                }
                HANDLER_PAUSE -> {
                    logInfo("pause:  mDuration = $duration")
                    if (mRecordListener != null) {
                        mRecordListener!!.onPause()
                    }
                }
                HANDLER_PERMISSION -> {
                    logInfo("permission：record fail ,maybe need permission")
                    if (mPermissionListener != null) {
                        mPermissionListener!!.needPermission()
                    }
                }
                HANDLER_RESET -> {
                    logInfo("reset:")
                    if (mRecordListener != null) {
                        mRecordListener!!.onReset()
                    }
                }
                HANDLER_MAX_TIME -> if (mRecordListener != null) {
                    mRecordListener!!.onMaxChange(mMaxTime)
                }
                else -> {
                }
            }
        }
    }

    //录音的方式
    abstract val recorderType:RecorderType

    //设置是否使用耳机配置方式
    abstract fun setContextToPlugConfig(context: Context): BaseRecorder

    //设置声音配置，设置后，修改设置声音大小会修改系统播放声音的大小
    abstract fun setContextToVolumeConfig(context: Context): BaseRecorder

    /**
     * 设置录音输出文件,
     * @param outputFile 设置输出路径
     * @param isContinue 表示是否拼接在文件末尾，继续录制的一种
     */
    open fun setOutputFile(outputFile: String, isContinue: Boolean = false): BaseRecorder {
        if (TextUtils.isEmpty(outputFile)) {
            val message = Message.obtain()
            message.what = HANDLER_ERROR
            message.obj = Exception("outputFile is not null")
            handler.sendMessage(message)
        } else {
            setOutputFile(File(outputFile), isContinue)
        }
        return this
    }


    /**
     * 设置录音输出文件
     * @param outputFile 设置输出路径
     * @param isContinue 表示是否拼接在文件末尾，继续录制的一种
     */
     open fun setOutputFile(outputFile: File, isContinue: Boolean = false): BaseRecorder {
        if (outputFile.exists()){
            Log.w(TAG, "setOutputFile: outputFile is exists, if not Continue may cover it(如果没有isContinue = true,会覆盖文件)", )
        }
        mRecordFile = outputFile
        this.isContinue = isContinue
        return this
    }

    //设置录音监听
    abstract fun setRecordListener(recordListener: RecordListener?): BaseRecorder

    //设置权限监听
    abstract fun setPermissionListener(permissionListener: PermissionListener?): BaseRecorder

    //设计背景音乐的url,本地的
    abstract fun setBackgroundMusic(url: String): BaseRecorder

    //是否循环播放，默认true
    abstract fun setLoopMusic(isLoop: Boolean): BaseRecorder

    //背景音乐的url,兼容Android Q
    abstract fun setBackgroundMusic(
        context: Context,
        uri: Uri,
        header: MutableMap<String, String>?
    ): BaseRecorder

    //设置背景音乐的监听
    abstract fun setBackgroundMusicListener(listener: PlayerListener): BaseRecorder

    //初始Lame录音输出质量
    open fun setMp3Quality(@IntRange(from = 0 ,to = 9)mp3Quality: Int): BaseRecorder {
        this.defaultLameMp3Quality = mp3Quality
        return this
    }

    //设置比特率，关系声音的质量
    open fun setMp3BitRate(@IntRange(from = 16) mp3BitRate: Int): BaseRecorder {
        this.defaultLameMp3BitRate = mp3BitRate
        return this
    }

    //设置采样率
    open fun setSamplingRate(@IntRange(from = 8000) rate: Int): BaseRecorder {
        this.defaultSamplingRate = rate
        return this
    }

    //设置音频声道数量，每次录音前可以设置修改，开始录音后无法修改
    abstract fun setAudioChannel(@IntRange(from = 1,to = 2)channel: Int = 1):Boolean

    //设置音频来源，每次录音前可以设置修改，开始录音后无法修改
    abstract fun setAudioSource(@Source audioSource: Int = MediaRecorder.AudioSource.MIC):Boolean


    //初始最大录制时间 和提醒时间 remind = maxTime - remindDiffTime
    abstract fun setMaxTime(maxTime: Int, remindDiffTime: Int? = null): BaseRecorder


    //设置增强系数(不建议修改，因为会产生噪音~)
    open fun setWax(wax: Float): BaseRecorder {
        this.wax = wax
        return this
    }

    //设置背景声音大小
    abstract fun setBGMVolume(volume: Float): BaseRecorder

    //移除背景音乐
    abstract fun cleanBackgroundMusic()

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
    abstract fun isPlayMusic(): Boolean

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

    open fun getSoundTouch():ISoundTouchCore{
        throw error("该录音工具不支持变音功能；The recorder does not support SoundTouch")
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
            mVolume = (log10(sqrt(sum / readSize)) * 20).toInt()
            if (mVolume < 5) {
                for (i in 0 until readSize) {
                    buffer[i] = 0
                }
            } else if (mVolume > 100) {
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

    protected fun logError(info: String) {
        Log.e(TAG, info)
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

    companion object {
        const val HANDLER_RECORDING = 0x101 //正在录音
        const val HANDLER_START = HANDLER_RECORDING + 1//开始了
        const val HANDLER_RESUME = HANDLER_START + 1//暂停后开始
        const val HANDLER_COMPLETE = HANDLER_RESUME + 1//完成
        const val HANDLER_AUTO_COMPLETE = HANDLER_COMPLETE + 1//最大时间完成
        const val HANDLER_ERROR = HANDLER_AUTO_COMPLETE + 1//错误
        const val HANDLER_PAUSE = HANDLER_ERROR + 1//暂停
        const val HANDLER_RESET = HANDLER_PAUSE + 1//暂停
        const val HANDLER_PERMISSION = HANDLER_RESET + 1//需要权限
        const val HANDLER_MAX_TIME = HANDLER_PERMISSION + 1//设置了最大时间
        const val FRAME_COUNT = 160
        val DEFAULT_AUDIO_FORMAT = PCMFormat.PCM_16BIT
        const val TAG = "Recorder"
    }

    //endregion  计算真正的时间，如果过程中有些数据太小，就直接置0，防止噪音
}
