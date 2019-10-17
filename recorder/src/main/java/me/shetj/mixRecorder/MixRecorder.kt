package me.shetj.mixRecorder


import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Process
import android.text.TextUtils
import android.util.Log
import me.shetj.player.PermissionListener
import me.shetj.player.PlayerListener
import me.shetj.player.RecordListener
import me.shetj.recorder.BaseRecorder
import me.shetj.recorder.PCMFormat
import me.shetj.recorder.RecordState
import me.shetj.recorder.util.BytesTransUtil
import me.shetj.recorder.util.LameUtils
import java.io.File
import java.io.IOException


/**
 * 混合录音
 */
class MixRecorder : BaseRecorder {

    private val TAG = this.javaClass.simpleName
    //======================Lame Default Settings=====================
    private var defaultLameInChannel = 2 //声道数量
    private var defaultLameMp3Quality = 5 //音频质量，好像LAME已经不使用它了
    private var mAudioRecord: AudioRecord? = null
    private var mPlayBackMusic: PlayBackMusic? = null

    /**
     * 系统自带的去噪音，增强以及回音问题
     */
    private var mNoiseSuppressor: NoiseSuppressor? = null
    private var mAcousticEchoCanceler: AcousticEchoCanceler? = null
    private var mAutomaticGainControl: AutomaticGainControl? = null

    /**
     * 输出的文件
     */
    private var mRecordFile: File? = null
    private var mEncodeThread: MixEncodeThread? = null
    private var mRecordListener: RecordListener? = null
    private var mPermissionListener: PermissionListener? = null

    var isRecording = false
        private set
    //背景音乐相关
    private var backgroundMusicIsPlay: Boolean = false //记录是否暂停
    private var mSendError: Boolean = false
    var isPause: Boolean = false //暂停录制
    //缓冲数量
    private var mBufferSize: Int = 0
    //录制时间
    var duration = 0L
        private set
    //最大时间
    private var mMaxTime: Long = 3600000
    //提醒时间
    private var mRemindTime = (3600000 - 10000).toLong()
    //通知速度，毫秒
    private var speed: Long = 300
    //当前状态
    /**
     * 当前录制状态
     * @return
     */
    var state = RecordState.STOPPED
        private set
    private var bytesPerSecond: Int = 0  //PCM文件大小=采样率采样时间采样位深/8*通道数（Bytes）

    private var is2Channel = true //默认是双声道
    private var bgLevel = 0.30f//背景音乐
    private var isContinue: Boolean = false //是否写在文件末尾

    //声音增强
    private var wax = 1f
    private var bgWax = 1.5f

    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                HANDLER_RECORDING -> {
                    Log.d(TAG, "msg.what = HANDLER_RECORDING  \n mDuration = $duration")
                    if (mRecordListener != null) {
                        //录制回调
                        mRecordListener!!.onRecording(duration, realVolume)
                        //提示快到录音时间了
                        if (mMaxTime > 150000 && mMaxTime > duration && duration > mRemindTime && duration - mRemindTime > 400) {
                            mRecordListener!!.onRemind(duration)
                        }
                    }
                }
                HANDLER_START -> {
                    Log.d(TAG, "msg.what = HANDLER_START  \n mDuration = $duration")
                    if (mRecordListener != null) {
                        mRecordListener!!.onStart()
                    }
                }
                HANDLER_RESUME -> {
                    Log.d(TAG, "msg.what = HANDLER_RESUME  \n mDuration = $duration")
                    if (mRecordListener != null) {
                        mRecordListener!!.onResume()
                    }
                }
                HANDLER_COMPLETE -> {
                    Log.d(TAG, "msg.what = HANDLER_COMPLETE  \n mDuration = $duration")
                    if (mRecordListener != null) {
                        mRecordListener!!.onSuccess(mRecordFile!!.absolutePath, duration)
                    }
                }
                HANDLER_AUTO_COMPLETE -> {
                    Log.d(TAG, "msg.what = HANDLER_AUTO_COMPLETE  \n mDuration = $duration")
                    if (mRecordListener != null) {
                        mRecordListener!!.autoComplete(mRecordFile!!.absolutePath, duration)
                    }
                }
                HANDLER_ERROR -> {
                    Log.d(TAG, "msg.what = HANDLER_ERROR  \n mDuration = $duration")
                    if (mRecordListener != null) {
                        mRecordListener!!.onError(Exception("record error!"))
                    }
                }
                HANDLER_PAUSE -> {
                    Log.d(TAG, "msg.what = HANDLER_PAUSE  \n mDuration = $duration")
                    if (mRecordListener != null) {
                        mRecordListener!!.onPause()
                    }
                }
                HANDLER_PERMISSION -> {
                    Log.d(TAG, "msg.what = HANDLER_PERMISSION  \n mDuration = $duration")
                    if (mPermissionListener != null) {
                        mPermissionListener!!.needPermission()
                    }
                }
                HANDLER_RESET -> {
                    Log.d(TAG, "msg.what = HANDLER_RESET  \n mDuration = $duration")
                    if (mRecordListener != null) {
                        mRecordListener!!.onReset()
                    }
                }
                HANDLER_MAX_TIME -> if (mRecordListener != null) {
                    mRecordListener!!.setMaxProgress(mMaxTime)
                }
                else -> {
                }
            }
        }
    }

    /**
     * 返回背景音乐的
     * @return
     */
    val bgPlayer: PlayBackMusic
        get() {
            initPlayer()
            return mPlayBackMusic!!
        }

    override val realVolume: Int
        get() = mVolume

    /**
     * 获取相对音量。 超过最大值时取最大值。
     *
     * @return 音量
     */
    val volume: Int
        get() = if (mVolume >= MAX_VOLUME) {
            MAX_VOLUME
        } else mVolume

    /**
     * 根据资料假定的最大值。 实测时有时超过此值。
     *
     * @return 最大音量值。
     */
    val maxVolume: Int
        get() = MAX_VOLUME

    private val bufferSize: Int
        get() {
            mBufferSize = AudioRecord.getMinBufferSize(
                DEFAULT_SAMPLING_RATE,
                DEFAULT_CHANNEL_CONFIG, DEFAULT_AUDIO_FORMAT.audioFormat
            )
            val bytesPerFrame = DEFAULT_AUDIO_FORMAT.bytesPerFrame
            Log.i(TAG, "mBufferSize = $mBufferSize")
            var frameSize = mBufferSize / bytesPerFrame
            if (frameSize % FRAME_COUNT != 0) {
                frameSize += FRAME_COUNT - frameSize % FRAME_COUNT
                mBufferSize = frameSize * bytesPerFrame
            }
            Log.i(TAG, "mBufferSize = $mBufferSize")
            return mBufferSize
        }


    constructor() {}

    /**
     * @param audioSource 最好是  [MediaRecorder.AudioSource.VOICE_COMMUNICATION] 或者 [MediaRecorder.AudioSource.MIC]
     * @param channel 声道数量 (1 或者 2)
     */
    constructor(audioSource: Int,   channel: Int) {
        defaultAudioSource = audioSource
        defaultLameInChannel = when {
            channel < 1 -> 1
            channel > 2 -> 2
            else -> channel
        }
        is2Channel = defaultLameInChannel == 2
    }

    fun setMp3Quality(mp3Quality: Int): MixRecorder {
        this.defaultLameMp3Quality = mp3Quality
        return this
    }


    /***************************public method  */

    /**
     * 最好是本地音乐,网络音乐可能存在出现卡顿
     * 默认循环播放
     * @param url
     */
    fun setBackgroundMusic(url: String): MixRecorder {
        if (!TextUtils.isEmpty(url)) {
            setBackgroundMusic(url, true)
        } else {
            throw NullPointerException("setBackgroundMusic -> url not null")
        }
        return this
    }

    fun setBackgroundMusic(url: String, isLoop: Boolean): MixRecorder {
        if (!TextUtils.isEmpty(url)) {
            initPlayer()
            mPlayBackMusic!!.setBackGroundUrl(url)
            mPlayBackMusic!!.setLoop(isLoop)
        } else {
            throw NullPointerException("setBackgroundMusic -> url not null")
        }
        return this
    }

    private fun initPlayer() {
        if (mPlayBackMusic == null) {
            mPlayBackMusic = PlayBackMusic(
                when (defaultLameInChannel == 2) {
                    true -> AudioFormat.CHANNEL_OUT_STEREO
                    else -> AudioFormat.CHANNEL_IN_LEFT
                }
            )
        }
    }
    /**
     * 设置录音输出文件
     * @param outputFile
     */
    @JvmOverloads
    fun setOutputFile(outputFile: String, isContinue: Boolean = false): MixRecorder {
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
     * @param outputFile
     */
    @JvmOverloads
    fun setOutputFile(outputFile: File, isContinue: Boolean = false): MixRecorder {
        mRecordFile = outputFile
        this.isContinue = isContinue
        return this
    }

    /**
     * 设置回调
     * @param recordListener
     */
    fun setRecordListener(recordListener: RecordListener?): MixRecorder {
        this.mRecordListener = recordListener
        return this
    }

    fun setPermissionListener(permissionListener: PermissionListener?): MixRecorder {
        this.mPermissionListener = permissionListener
        return this
    }

    /**
     * 设置最大录制时间
     * @param mMaxTime 最大录制时间  默认一个小时？
     * 提示时间时10秒前
     */
    fun setMaxTime(mMaxTime: Int): MixRecorder {
        this.mMaxTime = mMaxTime.toLong()
        this.mRemindTime = (mMaxTime - 10000).toLong()
        handler.sendEmptyMessage(HANDLER_MAX_TIME)
        return this
    }

    /**
     * 设置增强系数
     * @param wax
     */
    fun setWax(wax: Float): MixRecorder {
        this.wax = wax
        this.bgWax = wax*1.5f
        return this
    }

    /**
     * 设置通知速度 毫秒
     * @param speed 毫秒 默认300毫秒提醒一次
     */
    fun setSpeed(speed: Long): MixRecorder {
        this.speed = speed
        return this
    }

    fun setBackgroundMusicListener(playerListener: PlayerListener): MixRecorder {
        bgPlayer.setBackGroundPlayListener(playerListener)
        return this
    }

    fun setVolume(volume: Float): MixRecorder {
        val volume1 = when {
            volume < 0 -> 0f
            volume > 1 -> 1f
            else -> volume
        }
        val bgPlayer = bgPlayer
        bgPlayer.setVolume(volume1)
        this.bgLevel = volume1
        return this
    }

    /**
     * Start recording. Create an encoding thread. Start record from this
     */
    fun start() {
        if (isRecording) {
            return
        }
        // 提早，防止init或startRecording被多次调用
        isRecording = true
        //初始化
        duration = 0
        try {
            initAudioRecorder()
            mAudioRecord!!.startRecording()
        } catch (ex: Exception) {
            if (mRecordListener != null) {
                mRecordListener!!.onError(ex)
            }
            onError()
            handler.sendEmptyMessage(HANDLER_PERMISSION)
            return
        }

        object : Thread() {
            var isError = false
            //PCM文件大小 = 采样率采样时间采样位深 / 8*通道数（Bytes）
            override fun run() {
                //设置线程权限
                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
                onStart()
                while (isRecording) {
                    val samplesPerFrame = bgPlayer.bufferSize // 这里需要与 背景音乐读取出来的数据长度 一样
                    var buffer: ByteArray? = ByteArray(samplesPerFrame)
                    val readSize = mAudioRecord!!.read(buffer!!, 0, buffer.size)
                    if (readSize == AudioRecord.ERROR_INVALID_OPERATION || readSize == AudioRecord.ERROR_BAD_VALUE) {
                        if (!mSendError) {
                            mSendError = true
                            handler.sendEmptyMessage(HANDLER_PERMISSION)
                            onError()
                            isError = true
                        }
                    } else {
                        if (readSize > 0) {
                            if (isPause) {
                                continue
                            }
                            val readTime =
                                1000.0 * readSize.toDouble() * (if (is2Channel) 1 else 2).toDouble() / bytesPerSecond  //双声道和单声道的计算不一样
                            onRecording(readTime) //计算时间长度
                            buffer = mixBuffer(buffer) // 混合背景音乐

                            if (buffer != null) {
                                mEncodeThread!!.addTask(buffer, readSize)
                                calculateRealVolume(buffer)
                            }
                        } else {
                            if (!mSendError) {
                                mSendError = true
                                handler.sendEmptyMessage(HANDLER_PERMISSION)
                                onError()
                                isError = true
                            }
                        }
                    }

                }
                try {
                    mAudioRecord!!.stop()
                    mAudioRecord!!.release()
                    mAudioRecord = null
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }

                if (isError) {
                    mEncodeThread!!.sendErrorMessage()
                } else {
                    mEncodeThread!!.sendStopMessage()
                }
            }

        }.start()
    }




    fun stop() {
        if (state !== RecordState.STOPPED) {
            isPause = false
            isRecording = false
            if (mPlayBackMusic != null) {
                mPlayBackMusic!!.setNeedRecodeDataEnable(false)
            }
            handler.sendEmptyMessage(HANDLER_COMPLETE)
            state = RecordState.STOPPED
        }
        bgPlayer.release()
    }

    /**
     * 重新开始
     */
    fun onResume() {
        if (state === RecordState.PAUSED) {
            this.isPause = false
            state = RecordState.RECORDING
            handler.sendEmptyMessage(HANDLER_RESUME)
            if (backgroundMusicIsPlay) {
                bgPlayer.resume()
            }
        }
    }

    /**
     * 暂停
     */
    fun onPause() {
        if (state === RecordState.RECORDING) {
            this.isPause = true
            state = RecordState.PAUSED
            handler.sendEmptyMessage(HANDLER_PAUSE)
            backgroundMusicIsPlay = !bgPlayer.isIsPause
            bgPlayer.pause()
        }
    }

    fun startPlayMusic(){
        if (!bgPlayer.isPlayingMusic) {
            bgPlayer.startPlayBackMusic()
        }
    }

    fun isPauseMusic(): Boolean {
        return bgPlayer.isIsPause
    }

    fun pauseMusic(){
        if (!bgPlayer.isIsPause){
            bgPlayer.pause()
        }
    }

    fun resumeMusic(){
        if (bgPlayer.isIsPause){
            bgPlayer.resume()
        }
    }

    /**
     * 重置
     */
    fun onReset() {
        isRecording = false
        isPause = false
        state = RecordState.STOPPED
        duration = 0L
        mRecordFile = null
        handler.sendEmptyMessage(HANDLER_RESET)
        backgroundMusicIsPlay = !bgPlayer.isIsPause
        bgPlayer.release()
    }


    fun onDestroy() {
        bgPlayer.release()
        release()
    }

    /**
     * Initialize audio recorder
     */
    @Throws(IOException::class)
    private fun initAudioRecorder() {
        bufferSize  //获取合适的buffer大小
        /* Setup audio recorder */
        mAudioRecord = AudioRecord(
            defaultAudioSource,
            DEFAULT_SAMPLING_RATE, DEFAULT_CHANNEL_CONFIG, DEFAULT_AUDIO_FORMAT.audioFormat,
            mBufferSize
        )
        //1秒时间需要多少字节，用来计算已经录制了多久
        bytesPerSecond =
            mAudioRecord!!.sampleRate * mapFormat(mAudioRecord!!.audioFormat) / 8 * mAudioRecord!!.channelCount
        initAEC(mAudioRecord!!.audioSessionId)
        LameUtils.init(
            DEFAULT_SAMPLING_RATE,
            defaultLameInChannel,
            DEFAULT_SAMPLING_RATE,
            DEFAULT_LAME_MP3_BIT_RATE,
            defaultLameMp3Quality
        )
        mEncodeThread = MixEncodeThread(mRecordFile!!, mBufferSize, isContinue, is2Channel)
        mEncodeThread!!.start()
        mAudioRecord!!.setRecordPositionUpdateListener(mEncodeThread, mEncodeThread!!.handler)
        mAudioRecord!!.positionNotificationPeriod = FRAME_COUNT
    }

    /**
     * 混合 音频
     */
    private fun mixBuffer(buffer: ByteArray?): ByteArray? {
        if (mPlayBackMusic != null && mPlayBackMusic!!.hasFrameBytes()) {
            val bytes = BytesTransUtil.changeDataWithVolume(
                mPlayBackMusic!!.getBackGroundBytes()!!,
                bgLevel
            )
            val mine = BytesTransUtil.changeDataWithVolume(buffer!!, bgWax)
            return BytesTransUtil.averageMix(arrayOf(mine, bytes))
        }
        return BytesTransUtil.changeDataWithVolume(buffer!!, wax)
    }

    /***************************private method  */
    private fun onStart() {
        if (state !== RecordState.RECORDING) {
            handler.sendEmptyMessage(HANDLER_START)
            state = RecordState.RECORDING
            duration = 0L
            if (mPlayBackMusic != null) {
                mPlayBackMusic!!.setNeedRecodeDataEnable(true)
            }
        }
    }


    private fun onError() {
        isPause = false
        isRecording = false
        handler.sendEmptyMessage(HANDLER_ERROR)
        state = RecordState.STOPPED
        backgroundMusicIsPlay = false
        if (mPlayBackMusic != null) {
            mPlayBackMusic!!.setNeedRecodeDataEnable(false)
        }
    }


    /**
     * 计算时间
     * @param readTime
     */
    private fun onRecording(readTime: Double) {
        duration += readTime.toLong()
        handler.sendEmptyMessageDelayed(HANDLER_RECORDING, speed)
        if (mMaxTime in 1..duration) {
            autoStop()
        }
    }


    private fun autoStop() {
        if (state !== RecordState.STOPPED) {
            isPause = false
            isRecording = false
            handler.sendEmptyMessageDelayed(HANDLER_AUTO_COMPLETE, speed)
            state = RecordState.STOPPED
            backgroundMusicIsPlay = false
            if (mPlayBackMusic != null) {
                mPlayBackMusic!!.setNeedRecodeDataEnable(false)
            }
        }
    }

    private fun mapFormat(format: Int): Int {
        return when (format) {
            AudioFormat.ENCODING_PCM_8BIT -> 8
            AudioFormat.ENCODING_PCM_16BIT -> 16
            else -> 0
        }
    }


    private fun initAEC(mAudioSessionId: Int) {
        if (mAudioSessionId != 0 ) {
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

    private fun release() {
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

        private val HANDLER_RECORDING = 0x101 //正在录音
        private val HANDLER_START = 0x102//开始了
        private val HANDLER_RESUME = 0x108//暂停后开始
        private val HANDLER_COMPLETE = 0x103//完成
        private val HANDLER_AUTO_COMPLETE = 0x104//最大时间完成
        private val HANDLER_ERROR = 0x105//错误
        private val HANDLER_PAUSE = 0x106//暂停
        private val HANDLER_RESET = 0x109//暂停
        private val HANDLER_PERMISSION = 0x107//需要权限
        private val HANDLER_MAX_TIME = 0x110//设置了最大时间
        //=======================AudioRecord Default Settings=======================
        private var defaultAudioSource = MediaRecorder.AudioSource.VOICE_COMMUNICATION
        /**
         * 以下三项为默认配置参数。Google Android文档明确表明只有以下3个参数是可以在所有设备上保证支持的。
         */
        private val DEFAULT_SAMPLING_RATE = 44100
        private val DEFAULT_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO
        /**
         * 下面是对此的封装
         * private static final int DEFAULT_AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
         */
        private val DEFAULT_AUDIO_FORMAT = PCMFormat.PCM_16BIT
        /**
         * Encoded bit rate. MP3 file will be encoded with bit rate 32kbps
         */
        private val DEFAULT_LAME_MP3_BIT_RATE = 32
        //==================================================================
        /**
         * 自定义 每160帧作为一个周期，通知一下需要进行编码
         */
        private val FRAME_COUNT = 160
        private val MAX_VOLUME = 2000
    }
}

