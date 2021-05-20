package me.shetj.recorder.simRecorder

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Process
import android.text.TextUtils
import me.shetj.player.AudioPlayer
import me.shetj.player.PlayerListener
import me.shetj.recorder.core.*
import me.shetj.recorder.util.LameUtils
import java.io.File
import java.io.IOException


/**
 * 录制MP3 边录边转
 * 1.可以去换录制来源
 * 2.可以增强录制的声音
 *
 * 目前只支持单声道
 */
class SimRecorder : BaseRecorder {
    //======================Lame Default Settings=====================
    private var mAudioRecord: AudioRecord? = null
    private var mEncodeThread: DataEncodeThread? = null
    private var backgroundPlayer: AudioPlayer? = null
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

    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                HANDLER_RECORDING -> {
                    logInfo("msg.what = HANDLER_RECORDING  \n mDuration = $duration ")
                    if (mRecordListener != null) {
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
                    logInfo("msg.what = HANDLER_START  \n mDuration =$duration ")
                    if (mRecordListener != null) {
                        mRecordListener!!.onStart()
                    }
                }
                HANDLER_RESUME -> {
                    logInfo("msg.what = HANDLER_RESUME  \n mDuration = $duration ")
                    if (mRecordListener != null) {
                        mRecordListener!!.onResume()
                    }
                }
                HANDLER_COMPLETE -> {
                    logInfo("msg.what = HANDLER_COMPLETE  \n mDuration = $duration ")
                    if (mRecordListener != null) {
                        mRecordListener!!.onSuccess(false, mRecordFile!!.absolutePath, duration)
                    }
                }
                HANDLER_AUTO_COMPLETE -> {
                    logInfo("msg.what = HANDLER_AUTO_COMPLETE  \n mDuration = $duration ")
                    if (mRecordListener != null) {
                        mRecordListener!!.onSuccess(true, mRecordFile!!.absolutePath, duration)
                    }
                }
                HANDLER_ERROR -> {
                    logInfo("msg.what = HANDLER_ERROR  \n mDuration = $duration ")
                    if (mRecordListener != null) {
                        mRecordListener!!.onError(Exception("record error!"))
                    }
                }
                HANDLER_PAUSE -> {
                    logInfo("msg.what = HANDLER_PAUSE  \n mDuration = $duration ")
                    if (mRecordListener != null) {
                        mRecordListener!!.onPause()
                    }
                }
                HANDLER_PERMISSION -> {
                    logInfo("msg.what = HANDLER_PERMISSION  \n mDuration = $duration ")
                    if (mPermissionListener != null) {
                        mPermissionListener!!.needPermission()
                    }
                }
                HANDLER_RESET -> {
                    logInfo("msg.what = HANDLER_RESET  \n mDuration = $duration ")
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
    /***************************public method  */
    /**
     * 返回背景音乐的播放器
     * @return
     */
    private val bgPlayer: AudioPlayer
        get() {
            initBgMusicPlayer()
            return backgroundPlayer!!
        }

    override fun setContextToPlugConfig(context: Context): BaseRecorder {
        logInfo("SimRecorder no use it")
        return this
    }

    override fun setContextToVolumeConfig(context: Context): BaseRecorder {
        volumeConfig = VolumeConfig.getInstance(context.applicationContext)
        volumeConfig?.registerReceiver()
        return this
    }

    constructor()

    /**
     *
     * @param audioSource MediaRecorder.AudioSource.MIC
     */
    constructor(audioSource: Int = MediaRecorder.AudioSource.MIC) {
        this.defaultAudioSource = audioSource
        release()
    }

    /**
     * 设置录音输出文件
     * @param outputFile
     */
    override fun setOutputFile(outputFile: String, isContinue: Boolean): SimRecorder {
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

    override fun setMp3Quality(mp3Quality: Int): SimRecorder {
        this.defaultLameMp3Quality = when {
            mp3Quality < 0 -> 0
            mp3Quality > 9 -> 9
            else -> mp3Quality
        }
        return this
    }

    override fun setSamplingRate(rate: Int): SimRecorder {
        if (defaultSamplingRate < 8000) return this
        this.defaultSamplingRate = rate
        return this
    }


    override fun setMp3BitRate(mp3BitRate: Int): SimRecorder {
        if (mp3BitRate < 32) return this
        this.defaultLameMp3BitRate = mp3BitRate
        return this
    }


    /**
     * 设置录音输出文件
     * @param outputFile
     */
    override fun setOutputFile(outputFile: File, isContinue: Boolean): SimRecorder {
        mRecordFile = outputFile
        this.isContinue = isContinue
        return this
    }

    override fun updateDataEncode(outputFilePath: String) {
        setOutputFile(outputFilePath, false)
        mEncodeThread?.update(outputFilePath)
    }

    /**
     * 设置增强系数
     * @param wax
     */
    override fun setWax(wax: Float): SimRecorder {
        this.wax = wax
        return this
    }

    /**
     * 设置回调
     * @param recordListener
     */
    override fun setRecordListener(recordListener: RecordListener?): SimRecorder {
        this.mRecordListener = recordListener
        return this
    }

    override fun setPermissionListener(permissionListener: PermissionListener?): SimRecorder {
        this.mPermissionListener = permissionListener
        return this
    }

    override fun setMaxTime(maxTime: Int, remindDiffTime: Int?): SimRecorder {
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
            var bytesPerSecond =
                mAudioRecord!!.sampleRate * mapFormat(mAudioRecord!!.audioFormat) / 8 * mAudioRecord!!.channelCount

            override fun run() {
                //设置线程权限
                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
                onStart()
                while (isRecording) {
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
                            /**
                             * x2 转成字节做时间计算
                             */
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
                } catch (ex: Exception) {
                    ex.printStackTrace()
                } finally {
                    if (isAutoComplete) {
                        handler.sendEmptyMessage(HANDLER_AUTO_COMPLETE)
                    } else {
                        handler.sendEmptyMessage(HANDLER_COMPLETE)
                    }
                }

                if (isError) {
                    mEncodeThread!!.sendErrorMessage()
                } else {
                    mEncodeThread!!.sendStopMessage()
                }
            }

        }.start()
    }
    // endregion Start recording. Create an encoding thread. Start record from this

    override fun setBackgroundMusic(url: String): SimRecorder {
        this.backgroundMusicUri = null
        this.header = null
        this.backgroundMusicUrl = url
        return this
    }

    override fun setBackgroundMusic(
        context: Context,
        uri: Uri,
        header: MutableMap<String, String>?
    ): BaseRecorder {
        this.context = context.applicationContext
        this.backgroundMusicUrl = null
        this.backgroundMusicUri = uri
        this.header = header
        return this
    }

    override fun setLoopMusic(isLoop: Boolean): BaseRecorder {
        this.bgmIsLoop = isLoop
        return this
    }

    override fun setBackgroundMusicListener(listener: PlayerListener): SimRecorder {
        this.backgroundMusicPlayerListener = listener
        return this
    }

    override fun complete() {
        if (state !== RecordState.STOPPED) {
            isPause = false
            isRecording = false
            state = RecordState.STOPPED
            backgroundMusicIsPlay = false
            bgPlayer.stopPlay()
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
            if (backgroundMusicIsPlay) {
                bgPlayer.resume()
            }
        }
    }

    /**
     * 设置背景音乐的大小
     */
    override fun setBGMVolume(volume: Float): SimRecorder {
        if (volumeConfig == null) {
            val volume1 = when {
                volume < 0 -> 0f
                volume > 1 -> 1f
                else -> volume
            }
            val bgPlayer = bgPlayer
            bgPlayer.setVolume(volume1)
            this.bgLevel = volume1
        } else {
            volumeConfig?.setAudioVoiceF(volume)
        }
        return this
    }

    override fun cleanBackgroundMusic() {
        backgroundMusicUrl = null
        backgroundMusicUri = null
        header = null
        bgPlayer.stopPlay()
    }

    /**
     * 暂停
     */
    override fun pause() {
        if (state === RecordState.RECORDING) {
            isPause = true
            state = RecordState.PAUSED
            handler.sendEmptyMessage(HANDLER_PAUSE)
            backgroundMusicIsPlay = bgPlayer.isPlaying
            bgPlayer.pause()
        }
    }

    override fun isPlayMusic(): Boolean {
        return bgPlayer.isPlayingMusic
    }

    /**
     * 重置
     */
    override fun reset() {
        isRecording = false
        isPause = false
        state = RecordState.STOPPED
        duration = 0L
        mRecordFile = null
        backgroundMusicIsPlay = bgPlayer.isPlaying
        handler.sendEmptyMessage(HANDLER_RESET)
        bgPlayer.stopPlay()
    }


    override fun destroy() {
        isRecording = false
        isPause = false
        state = RecordState.STOPPED
        mRecordFile = null
        release()
        bgPlayer.stopPlay()
        handler.removeCallbacksAndMessages(null)
        volumeConfig?.unregisterReceiver()
    }


    override fun startPlayMusic() {
        if (!bgPlayer.isPlaying) {
            if (backgroundMusicUrl != null) {
                bgPlayer.playOrPause(
                    url = backgroundMusicUrl,
                    listener = backgroundMusicPlayerListener
                )
            } else if (backgroundMusicUri != null && context != null) {
                bgPlayer.playOrPause(
                    context = context!!,
                    uri = backgroundMusicUri,
                    header = header,
                    listener = backgroundMusicPlayerListener
                )
            }
        }
    }

    override fun isPauseMusic(): Boolean {
        return bgPlayer.isPause
    }

    override fun pauseMusic() {
        if (!bgPlayer.isPause) {
            bgPlayer.pause()
        }
    }

    override fun resumeMusic() {
        if (bgPlayer.isPause) {
            bgPlayer.resume()
        }
    }

    /**
     * Initialize audio recorder
     */
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
        mEncodeThread = DataEncodeThread(mRecordFile!!, mBufferSize, isContinue)
        mEncodeThread!!.start()
        mAudioRecord!!.setRecordPositionUpdateListener(mEncodeThread, mEncodeThread!!.handler)
        mAudioRecord!!.positionNotificationPeriod = FRAME_COUNT
    }

    /***************************private method  */

    private fun initBgMusicPlayer() {
        if (backgroundPlayer == null) {
            backgroundPlayer = AudioPlayer()
        }
        backgroundPlayer!!.setLoop(this.bgmIsLoop)
    }


    private fun onStart() {
        if (state !== RecordState.RECORDING) {
            handler.sendEmptyMessage(HANDLER_START)
            state = RecordState.RECORDING
            duration = 0L
            isRemind = true
            isPause = false
            if (backgroundMusicIsPlay) {
                if (backgroundMusicUrl != null) {
                    bgPlayer.playNoStart(
                        url = backgroundMusicUrl,
                        listener = backgroundMusicPlayerListener
                    )
                } else if (backgroundMusicUri != null && context != null) {
                    bgPlayer.playNoStart(
                        context = context!!,
                        uri = backgroundMusicUri,
                        header = header,
                        listener = backgroundMusicPlayerListener
                    )
                }
            }
        }
    }


    private fun onError() {
        isPause = false
        isRecording = false
        handler.sendEmptyMessage(HANDLER_ERROR)
        state = RecordState.STOPPED
        backgroundMusicIsPlay = false
        bgPlayer.stopPlay()
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
            isRecording = false
            isAutoComplete = true
            state = RecordState.STOPPED
            backgroundMusicIsPlay = false
            bgPlayer.stopPlay()
        }
    }


    private fun mapFormat(format: Int): Int {
        return when (format) {
            AudioFormat.ENCODING_PCM_8BIT -> 8
            AudioFormat.ENCODING_PCM_16BIT -> 16
            else -> 0
        }
    }

}