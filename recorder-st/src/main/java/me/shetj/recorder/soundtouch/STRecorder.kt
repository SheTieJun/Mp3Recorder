package me.shetj.recorder.soundtouch

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.os.Message
import android.os.Process
import me.shetj.player.PlayerListener
import me.shetj.recorder.core.*
import me.shetj.ndk.lame.LameUtils
import java.io.IOException


/**
 * 录制MP3 边录边转
 */
internal class STRecorder : BaseRecorder {

    override val recorderType: RecorderType = RecorderType.SIM

    private var mAudioRecord: AudioRecord? = null
    private var mEncodeThread: DataSTEncodeThread? = null
    private var context: Context? = null

    /**
     * 输出的文件
     */
    private var mPCMBuffer: ShortArray? = null
    private var mSendError: Boolean = false

    /**
     * 音量变化监听
     */
    private var volumeConfig: VolumeConfig? = null

    //缓冲数量
    private var mBufferSize: Int = 0

    /**
     * pcm数据的速度，默认300
     * 数据越大，速度越慢
     */
    private var waveSpeed = 300
        set(waveSpeed) {
            if (this.waveSpeed <= 0) {
                return
            }
            field = waveSpeed
        }

    //背景音乐相关
    private var backgroundMusicUrl: String? = null
    private var backgroundMusicPlayerListener: PlayerListener? = null
    private var backgroundMusicUri: Uri? = null
    private var header: MutableMap<String, String>? = null


    override fun setContextToPlugConfig(context: Context): BaseRecorder {
        logInfo("SimRecorder no use it")
        return this
    }

    override fun setContextToVolumeConfig(context: Context): BaseRecorder {
        volumeConfig = VolumeConfig.getInstance(context.applicationContext)
        volumeConfig?.registerReceiver()
        return this
    }

    constructor() {
    }

    /**
     *
     * @param audioSource MediaRecorder.AudioSource.MIC
     */
    constructor(@Source audioSource: Int = MediaRecorder.AudioSource.MIC,@Channel channel: Int = 1) {
        this.defaultAudioSource = audioSource
        defaultLameInChannel = channel
        defaultChannelConfig = when (channel) {
            1 -> {
                AudioFormat.CHANNEL_IN_MONO
            }
            2 -> {
                AudioFormat.CHANNEL_IN_STEREO
            }
            else -> defaultAudioSource
        }
        is2Channel = defaultLameInChannel == 2
        release()
    }

    override fun getSoundTouch(): ISoundTouchCore {
        return SoundTouchKit.getInstance()
    }

    override fun setAudioChannel(@Channel channel: Int): Boolean {
        if (isActive) {
            return false
        }
        is2Channel = channel == 2
        defaultLameInChannel = when {
            channel <= 1 -> {
                defaultChannelConfig = AudioFormat.CHANNEL_IN_MONO
                release()
                1
            }
            channel >= 2 -> {
                defaultChannelConfig = AudioFormat.CHANNEL_IN_STEREO
                release()
                2
            }
            else -> defaultAudioSource
        }
        return true
    }

    override fun setAudioSource(audioSource: Int): Boolean {
        if (!isActive) {
            defaultAudioSource = audioSource
            return true
        }
        return false
    }

    override fun updateDataEncode(outputFilePath: String) {
        setOutputFile(outputFilePath, false)
        mEncodeThread?.update(outputFilePath)
    }

    /**
     * 设置回调
     * @param recordListener
     */
    override fun setRecordListener(recordListener: RecordListener?): STRecorder {
        this.mRecordListener = recordListener
        return this
    }

    override fun setPermissionListener(permissionListener: PermissionListener?): STRecorder {
        this.mPermissionListener = permissionListener
        return this
    }

    override fun setMaxTime(maxTime: Int, remindDiffTime: Int?): STRecorder {
        if (maxTime < 0) {
            return this
        }
        this.mMaxTime = maxTime.toLong()
        handler.sendEmptyMessage(HANDLER_MAX_TIME)
        if (remindDiffTime != null && remindDiffTime < maxTime) {
            this.mRemindTime = (maxTime - remindDiffTime).toLong()
        } else {
            this.mRemindTime = (maxTime - 10000).toLong()
        }
        return this
    }

    // region Start recording. Create an encoding thread. Start record from this
    override fun start() {
        if (mRecordFile == null) {
            logInfo("mRecordFile is Null")
            return
        }
        if (isActive || recordThread?.isAlive == true) {
            return
        }
        // 提早，防止init或startRecording被多次调用
        isActive = true
        mSendError = false
        //初始化
        duration = 0
        try {
            initAudioRecorder()
            mAudioRecord!!.startRecording()
        } catch (ex: Exception) {
            onError(ex)
            return
        }

        object : Thread() {
            var isError = false

            //PCM文件大小 = 采样率采样时间采样位深 / 8*通道数（Bytes）
            var bytesPerSecond =
                mAudioRecord!!.sampleRate * mapFormat(mAudioRecord!!.audioFormat) / 8 * mAudioRecord!!.channelCount

            override fun run() {
                //设置线程权限
                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
                onStart()
                while (isActive) {
                    val readSize = mAudioRecord!!.read(mPCMBuffer!!, 0, mBufferSize)
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
                            val readTime = 1000.0 * readSize.toDouble() * 2 / bytesPerSecond
                            mEncodeThread!!.addTask(mPCMBuffer!!, readSize)
                            calculateRealVolume(mPCMBuffer!!, readSize)
                            //short 是2个字节 byte 是1个字节8位
                            onRecording(readTime)
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
                    if (isError) {
                        mEncodeThread!!.sendErrorMessage()
                    } else {
                        mEncodeThread!!.sendStopMessage()
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                } finally {
                    if (!isError) {
                        if (isAutoComplete) {
                            handler.sendEmptyMessage(HANDLER_AUTO_COMPLETE)
                        } else {
                            handler.sendEmptyMessage(HANDLER_COMPLETE)
                        }
                    }
                }
            }

        }.also {
            recordThread = it
        }.start()
    }
    // endregion Start recording. Create an encoding thread. Start record from this

    @Deprecated("不支持背景音乐", replaceWith = ReplaceWith("no use"))
    override fun setBackgroundMusic(url: String): STRecorder {
        logError("不支持背景音乐")
        this.backgroundMusicUri = null
        this.header = null
        this.backgroundMusicUrl = url
        return this
    }

    @Deprecated("不支持背景音乐", replaceWith = ReplaceWith("no use"))
    override fun setBackgroundMusic(
        context: Context,
        uri: Uri,
        header: MutableMap<String, String>?
    ): BaseRecorder {
        logError("不支持背景音乐")
        this.context = context.applicationContext
        this.backgroundMusicUrl = null
        this.backgroundMusicUri = uri
        this.header = header
        return this
    }

    @Deprecated("不支持背景音乐", replaceWith = ReplaceWith("no use"))
    override fun setLoopMusic(isLoop: Boolean): BaseRecorder {
        this.bgmIsLoop = isLoop
        logError("不支持背景音乐")
        return this
    }

    @Deprecated("不支持背景音乐", replaceWith = ReplaceWith("no use"))
    override fun setBackgroundMusicListener(listener: PlayerListener): STRecorder {
        logError("不支持背景音乐")
        return this
    }

    override fun complete() {
        if (state !== RecordState.STOPPED) {
            isPause = false
            isActive = false
            state = RecordState.STOPPED
            backgroundMusicIsPlay = false
        }
    }


    /**
     * 重新开始
     */
    override fun resume() {
        if (state === RecordState.PAUSED) {
            isPause = false
            state = RecordState.RECORDING
            handler.sendEmptyMessage(HANDLER_RESUME)
        }
    }

    /**
     * 设置背景音乐的大小
     */
    @Deprecated("不支持背景音乐", replaceWith = ReplaceWith("no use"))
    override fun setBGMVolume(volume: Float): STRecorder {
        logError("不支持背景音乐")
        return this
    }

    override fun cleanBackgroundMusic() {
        backgroundMusicUrl = null
        backgroundMusicUri = null
        header = null
    }

    /**
     * 暂停
     */
    override fun pause() {
        if (state === RecordState.RECORDING) {
            isPause = true
            state = RecordState.PAUSED
            handler.sendEmptyMessage(HANDLER_PAUSE)
        }
    }

    @Deprecated("不支持背景音乐", replaceWith = ReplaceWith("no use"))
    override fun isPlayMusic(): Boolean {
        logError("不支持背景音乐")
        return false
    }

    /**
     * 重置
     */
    override fun reset() {
        isActive = false
        isPause = false
        state = RecordState.STOPPED
        duration = 0L
        mRecordFile = null
        handler.sendEmptyMessage(HANDLER_RESET)
    }


    override fun destroy() {
        isActive = false
        isPause = false
        state = RecordState.STOPPED
        mRecordFile = null
        release()
        handler.removeCallbacksAndMessages(null)
        volumeConfig?.unregisterReceiver()
        SoundTouchKit.onDestroy()
    }


    @Deprecated("不支持背景音乐", replaceWith = ReplaceWith("no use"))
    override fun startPlayMusic() {
        logError("不支持背景音乐")
    }

    @Deprecated("不支持背景音乐", replaceWith = ReplaceWith("no use"))
    override fun isPauseMusic(): Boolean {
        logError("不支持背景音乐")
        return true
    }

    @Deprecated("不支持背景音乐")
    override fun pauseMusic() {
        logError("不支持背景音乐")
    }

    @Deprecated("不支持背景音乐", replaceWith = ReplaceWith("no use"))
    override fun resumeMusic() {
        logError("不支持背景音乐")
    }

    /**
     * Initialize audio recorder
     */
    @SuppressLint("MissingPermission")
    @Throws(IOException::class)
    private fun initAudioRecorder() {
        mBufferSize = AudioRecord.getMinBufferSize(
            defaultSamplingRate,
            defaultChannelConfig, DEFAULT_AUDIO_FORMAT.audioFormat
        )
        val bytesPerFrame = DEFAULT_AUDIO_FORMAT.bytesPerFrame
        /* Get number of samples. Calculate the buffer size
         * (round up to the factor of given frame size)
         * 使能被整除，方便下面的周期性通知
         * */
        var frameSize = mBufferSize / bytesPerFrame
        if (frameSize % FRAME_COUNT != 0) {
            frameSize += FRAME_COUNT - frameSize % FRAME_COUNT
            mBufferSize = frameSize * bytesPerFrame
        }
//        Log.i(TAG, "mBufferSize = $mBufferSize")
        /* Setup audio recorder */
        mAudioRecord = AudioRecord(
            defaultAudioSource,
            defaultSamplingRate, defaultChannelConfig, DEFAULT_AUDIO_FORMAT.audioFormat,
            mBufferSize
        )
        mPCMBuffer = ShortArray(mBufferSize)

        initAEC(mAudioRecord!!.audioSessionId)
        LameUtils.init(
            defaultSamplingRate,
            defaultLameInChannel,
            defaultSamplingRate,
            defaultLameMp3BitRate,
            defaultLameMp3Quality
        )
        mEncodeThread = DataSTEncodeThread(
            mRecordFile!!,
            mBufferSize,
            isContinue,
            defaultChannelConfig == AudioFormat.CHANNEL_IN_STEREO
        )
        mEncodeThread!!.start()
        mAudioRecord!!.setRecordPositionUpdateListener(
            mEncodeThread,
            mEncodeThread!!.getEncodeHandler()
        )
        mAudioRecord!!.positionNotificationPeriod = FRAME_COUNT
    }

    /***************************private method  */


    private fun onStart() {
        if (state !== RecordState.RECORDING) {
            handler.sendEmptyMessage(HANDLER_START)
            state = RecordState.RECORDING
            duration = 0L
            isRemind = true
            isPause = false
        }
    }


    private fun onError(ex: Exception? = null) {
        isPause = false
        isActive = false
        val message = Message.obtain()
        message.what = HANDLER_ERROR
        message.obj = ex
        handler.sendMessage(message)
        state = RecordState.STOPPED
        backgroundMusicIsPlay = false
    }


    /**
     * 计算时间
     * @param readTime
     */
    private fun onRecording(readTime: Double) {
        duration += readTime.toLong()
        if (state == RecordState.RECORDING) {
            handler.sendEmptyMessageDelayed(HANDLER_RECORDING, waveSpeed.toLong())
            if (mMaxTime in 1..duration) {
                autoStop()
            }
        }
    }

    private fun autoStop() {
        if (state !== RecordState.STOPPED) {
            isPause = false
            isActive = false
            isAutoComplete = true
            state = RecordState.STOPPED
            backgroundMusicIsPlay = false
        }
    }


    private fun mapFormat(format: Int): Int {
        return when (format) {
            AudioFormat.ENCODING_PCM_8BIT -> 8
            AudioFormat.ENCODING_PCM_16BIT -> 16
            else -> 16
        }
    }

}