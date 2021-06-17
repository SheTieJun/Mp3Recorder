package me.shetj.recorder.mixRecorder


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
import me.shetj.player.PlayerListener
import me.shetj.recorder.core.*
import me.shetj.recorder.util.LameUtils
import java.io.File
import java.io.IOException


/**
 * 混合录音
 */
class MixRecorder : BaseRecorder {

    override val recorderType: RecorderType = RecorderType.MIX
    //region 参数

    //region  Lame Default Setting （Lame的设置）
    private var mAudioRecord: AudioRecord? = null
    private var mPlayBackMusic: PlayBackMusic? = null
    private var mEncodeThread: MixEncodeThread? = null
    //endregion Lame Default Settings

    //region 背景音乐相关
    private var plugConfigs: PlugConfigs? = null
    private var volumeConfig: VolumeConfig? = null
    //endregion 背景音乐相关

    //region 其他
    private var mSendError: Boolean = false

    //缓冲数量
    private var mBufferSize: Int = 0
    private var bytesPerSecond: Int = 0  //PCM文件大小=采样率采样时间采样位深/8*通道数（Bytes）
    //endregion 其他

    //endregion 参数
    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                HANDLER_RECORDING -> {
                    if (mRecordListener != null && state == RecordState.RECORDING) {
                        logInfo("msg.what = HANDLER_RECORDING  \n mDuration = $duration ,volume = $realVolume and state is recording  = ${state == RecordState.RECORDING}")
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
                    logInfo("msg.what = HANDLER_START  \n mDuration = $duration\n mRemindTime = $mRemindTime")
                    if (mRecordListener != null) {
                        mRecordListener!!.onStart()
                    }
                }
                HANDLER_RESUME -> {
                    logInfo("msg.what = HANDLER_RESUME  \n mDuration = $duration")
                    if (mRecordListener != null) {
                        mRecordListener!!.onResume()
                    }
                }
                HANDLER_COMPLETE -> {
                    logInfo("msg.what = HANDLER_COMPLETE  \n mDuration = $duration")
                    if (mRecordListener != null && mRecordFile != null) {
                        mRecordListener!!.onSuccess(false, mRecordFile!!.absolutePath, duration)
                    }
                }
                HANDLER_AUTO_COMPLETE -> {
                    logInfo("msg.what = HANDLER_AUTO_COMPLETE  \n mDuration = $duration")
                    if (mRecordListener != null && mRecordFile != null) {
                        mRecordListener!!.onSuccess(true, mRecordFile!!.absolutePath, duration)
                    }
                }
                HANDLER_ERROR -> {
                    logInfo("msg.what = HANDLER_ERROR  \n mDuration = $duration")
                    if (mRecordListener != null) {
                        mRecordListener!!.onError(Exception("record error!"))
                    }
                }
                HANDLER_PAUSE -> {
                    logInfo("msg.what = HANDLER_PAUSE  \n mDuration = $duration")
                    if (mRecordListener != null) {
                        mRecordListener!!.onPause()
                    }
                }
                HANDLER_PERMISSION -> {
                    logInfo("msg.what = HANDLER_PERMISSION  \n mDuration = $duration")
                    if (mPermissionListener != null) {
                        mPermissionListener!!.needPermission()
                    }
                }
                HANDLER_RESET -> {
                    logInfo("msg.what = HANDLER_RESET  \n mDuration = $duration")
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
    //region 公开方法！
    /**
     * 返回背景音乐的
     * @return
     */
    internal val bgPlayer: PlayBackMusic
        get() {
            initPlayer()
            return mPlayBackMusic!!
        }

    override fun setContextToPlugConfig(context: Context): MixRecorder {
        plugConfigs = PlugConfigs.getInstance(context.applicationContext)
        bgPlayer.plugConfigs = plugConfigs
        return this
    }

    override fun setContextToVolumeConfig(context: Context): BaseRecorder {
        volumeConfig = VolumeConfig.getInstance(context.applicationContext)
        volumeConfig!!.registerReceiver()
        return this
    }

    constructor()

    /**
     * @param audioSource 最好是  [MediaRecorder.AudioSource.VOICE_COMMUNICATION] 或者 [MediaRecorder.AudioSource.MIC]
     * @param channel 声道数量 (1 或者 2)
     */
    constructor(audioSource: Int = MediaRecorder.AudioSource.MIC, channel: Int = 1) {
        defaultAudioSource = audioSource
        is2Channel = channel == 2
        defaultLameInChannel = when {
            channel <= 1 -> {
                defaultChannelConfig = AudioFormat.CHANNEL_IN_MONO
                release()
                initPlayer()
                1
            }
            channel >= 2 -> {
                defaultChannelConfig = AudioFormat.CHANNEL_IN_STEREO
                release()
                initPlayer()
                2
            }
            else -> defaultAudioSource
        }
    }

    override fun setMp3Quality(mp3Quality: Int): MixRecorder {
        this.defaultLameMp3Quality = when {
            mp3Quality < 0 -> 0
            mp3Quality > 9 -> 9
            else -> mp3Quality
        }
        return this
    }

    override fun setSamplingRate(rate: Int): MixRecorder {
        if (defaultSamplingRate < 8000) return this //低于8000 没有意义
        this.defaultSamplingRate = rate
        return this
    }

    override fun setMp3BitRate(mp3BitRate: Int): MixRecorder {
        if (mp3BitRate < 32) return this //低于32 也没有意义
        this.defaultLameMp3BitRate = mp3BitRate
        return this
    }

    override fun setAudioChannel(channel: Int): Boolean {
        if (!isRecording) {
            is2Channel = channel == 2
            defaultLameInChannel = when {
                channel <= 1 -> {
                    defaultChannelConfig = AudioFormat.CHANNEL_IN_MONO
                    1
                }
                channel >= 2 -> {
                    defaultChannelConfig = AudioFormat.CHANNEL_IN_STEREO
                    2
                }
                else -> defaultAudioSource
            }
            release()
            initPlayer()
            bgPlayer.updateChannel(defaultLameInChannel)
            return true
        }
        logInfo("setAudioChannel error ,need state  isn't Recording ")
        return false
    }

    override fun setAudioSource(audioSource: Int): Boolean {
        if (!isRecording) {
            defaultAudioSource = audioSource
            return true
        }
        logInfo("setAudioSource error ,need state  isn't Recording ")
        return false
    }

    /***************************public method  */

    /**
     * 最好是本地音乐,网络音乐可能存在出现卡顿
     * 默认循环播放
     * @param url
     */
    override fun setBackgroundMusic(url: String): MixRecorder {
        bgPlayer.setBackGroundUrl(url)
        bgPlayer.setLoop(bgmIsLoop)
        return this
    }

    override fun setBackgroundMusic(
        context: Context,
        uri: Uri,
        header: MutableMap<String, String>?
    ): MixRecorder {
        initPlayer()
        mPlayBackMusic!!.setBackGroundUrl(context, uri, header)
        mPlayBackMusic!!.setLoop(this.bgmIsLoop)
        return this
    }

    override fun setLoopMusic(isLoop: Boolean): BaseRecorder {
        this.bgmIsLoop = isLoop
        bgPlayer.setLoop(isLoop)
        return this
    }

    private fun initPlayer() {
        if (mPlayBackMusic == null) {
            mPlayBackMusic = PlayBackMusic(
                when (is2Channel) {
                    true -> AudioFormat.CHANNEL_OUT_STEREO
                    else -> AudioFormat.CHANNEL_OUT_MONO
                }, plugConfigs
            )
        }
    }

    /**
     * 设置录音输出文件
     * @param outputFile
     */
    override fun setOutputFile(outputFile: String, isContinue: Boolean): MixRecorder {
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
    override fun setOutputFile(outputFile: File, isContinue: Boolean): MixRecorder {
        mRecordFile = outputFile
        this.isContinue = isContinue
        return this
    }


    override fun updateDataEncode(outputFilePath: String) {
        setOutputFile(outputFilePath, false)
        mEncodeThread?.update(outputFilePath)
    }

    /**
     * 设置回调
     * @param recordListener
     */
    override fun setRecordListener(recordListener: RecordListener?): MixRecorder {
        this.mRecordListener = recordListener
        return this
    }

    override fun setPermissionListener(permissionListener: PermissionListener?): MixRecorder {
        this.mPermissionListener = permissionListener
        return this
    }

    override fun setMaxTime(maxTime: Int, remindDiffTime: Int?): MixRecorder {
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

    /**
     * 设置增强系数
     * @param wax
     */
    override fun setWax(wax: Float): MixRecorder {
        this.wax = wax
        return this
    }

    override fun setBackgroundMusicListener(listener: PlayerListener): MixRecorder {
        bgPlayer.setBackGroundPlayListener(listener)
        return this
    }

    override fun setBGMVolume(volume: Float): MixRecorder {
        val volume1 = when {
            volume < 0 -> 0f
            volume > 1 -> 1f
            else -> volume
        }
        if (volumeConfig == null) {
            val bgPlayer = bgPlayer
            bgPlayer.setVolume(volume1)
            this.bgLevel = volume1
        } else {
            volumeConfig!!.setAudioVoiceF(volume)
        }
        return this
    }

    override fun cleanBackgroundMusic() {
        bgPlayer.cleanMusic()
    }

    /**
     * Start recording. Create an encoding thread. Start record from this
     * u need start on the same thread
     */
    override fun start() {
        plugConfigs?.registerReceiver()
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
            override fun run() {
                //设置线程权限
                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
                onStart()
                while (isRecording) {
                    /*
                      这里需要与 背景音乐读取出来的数据长度 一样,
                      如果没有背景音乐才使用获取 mBufferSize
                     */
                    val samplesPerFrame =
                        if (bgPlayer.isPlayingMusic) bgPlayer.bufferSize else mBufferSize
                    val buffer = ByteArray(samplesPerFrame)
                    val readCode = mAudioRecord!!.read(buffer, 0, samplesPerFrame)
                    if (readCode == AudioRecord.ERROR_INVALID_OPERATION || readCode == AudioRecord.ERROR_BAD_VALUE) {
                        if (!mSendError) {
                            mSendError = true
                            handler.sendEmptyMessage(HANDLER_PERMISSION)
                            onError()
                            isError = true
                        }
                    } else {
                        if (readCode > 0) {
                            if (isPause) {
                                continue
                            }
                            val readTime = 1000.0 * readCode.toDouble() / bytesPerSecond
                            //计算时间长度,同时判断是否达到最大录制时间
                            if (onRecording(readTime)) {
                                mEncodeThread!!.addTask(
                                    buffer,
                                    wax,
                                    mPlayBackMusic!!.getBackGroundBytes(),
                                    volumeConfig?.currVolumeF ?: bgLevel
                                )
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


    override fun complete() {
        if (state != RecordState.STOPPED) {
            state = RecordState.STOPPED
            isAutoComplete = false
            plugConfigs?.unregisterReceiver()
            isRecording = false
            isPause = false
            if (mPlayBackMusic != null) {
                mPlayBackMusic!!.setNeedRecodeDataEnable(false)
                bgPlayer.release()
            }
        }
    }

    /**
     * 重新开始
     */
    override fun resume() {
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
    override fun pause() {
        if (state === RecordState.RECORDING) {
            this.isPause = true
            state = RecordState.PAUSED
            handler.sendEmptyMessage(HANDLER_PAUSE)
            backgroundMusicIsPlay = !bgPlayer.isIsPause
            bgPlayer.pause()
        }
    }

    override fun isPlayMusic(): Boolean {
        return bgPlayer.isPlayingMusic
    }

    override fun startPlayMusic() {
        if (!bgPlayer.isPlayingMusic && isRecording) {
            bgPlayer.startPlayBackMusic()
        }
    }

    override fun isPauseMusic(): Boolean {
        return bgPlayer.isIsPause
    }

    override fun pauseMusic() {
        if (!bgPlayer.isIsPause) {
            bgPlayer.pause()
        }
    }

    override fun resumeMusic() {
        if (bgPlayer.isIsPause) {
            bgPlayer.resume()
        }
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
        handler.sendEmptyMessage(HANDLER_RESET)
        backgroundMusicIsPlay = !bgPlayer.isIsPause
        bgPlayer.release()
    }


    override fun destroy() {
        isRecording = false
        isPause = false
        mRecordFile = null
        bgPlayer.release()
        release()
        handler.removeCallbacksAndMessages(null)
        volumeConfig?.unregisterReceiver()
        plugConfigs?.unregisterReceiver()
    }
    //endregion 公开方法！

    //region 初始化 Initialize audio recorder
    @Throws(IOException::class)
    private fun initAudioRecorder() {
        mBufferSize = AudioRecord.getMinBufferSize(
            defaultSamplingRate,
            defaultChannelConfig, DEFAULT_AUDIO_FORMAT.audioFormat
        )
        val bytesPerFrame = DEFAULT_AUDIO_FORMAT.bytesPerFrame
        var frameSize = mBufferSize / bytesPerFrame
        if (frameSize % FRAME_COUNT != 0) {
            frameSize += FRAME_COUNT - frameSize % FRAME_COUNT
            mBufferSize = frameSize * bytesPerFrame
        }
        //获取合适的buffer大小
        /* Setup audio recorder */
        mAudioRecord = AudioRecord(
            defaultAudioSource,
            defaultSamplingRate, defaultChannelConfig, DEFAULT_AUDIO_FORMAT.audioFormat,
            mBufferSize
        )

        //1秒时间需要多少字节，用来计算已经录制了多久
        bytesPerSecond =
            mAudioRecord!!.sampleRate * mapFormat(mAudioRecord!!.audioFormat) / 8 * mAudioRecord!!.channelCount

        initAEC(mAudioRecord!!.audioSessionId)

        LameUtils.init(
            defaultSamplingRate,
            defaultLameInChannel,
            defaultSamplingRate,
            defaultLameMp3BitRate,
            defaultLameMp3Quality
        )
        mEncodeThread = MixEncodeThread(mRecordFile!!, mBufferSize, isContinue, is2Channel)
        mEncodeThread!!.start()
        mAudioRecord!!.setRecordPositionUpdateListener(mEncodeThread, mEncodeThread!!.handler)
        mAudioRecord!!.positionNotificationPeriod = FRAME_COUNT
    }
    //endregion

    //region private method  私有方法
    private fun onStart() {
        if (state !== RecordState.RECORDING) {
            handler.sendEmptyMessage(HANDLER_START)
            state = RecordState.RECORDING
            isRemind = true
            isPause = false
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
            bgPlayer.release()
        }
    }


    /**
     * 计算时间
     * @param readTime
     * @return boolean false 表示触发了自动完成
     */
    private fun onRecording(readTime: Double): Boolean {
        duration += readTime.toLong()
        if (state == RecordState.RECORDING) {
            handler.sendEmptyMessage(HANDLER_RECORDING)
            if (mMaxTime in 1..duration) {
                autoStop()
                return false
            }
        }
        return true
    }


    private fun autoStop() {
        if (state != RecordState.STOPPED) {
            state = RecordState.STOPPED
            isAutoComplete = true
            plugConfigs?.unregisterReceiver()
            isPause = false
            isRecording = false
            backgroundMusicIsPlay = false
            if (mPlayBackMusic != null) {
                bgPlayer.setNeedRecodeDataEnable(false)
                bgPlayer.release()
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

    //endregion
}

