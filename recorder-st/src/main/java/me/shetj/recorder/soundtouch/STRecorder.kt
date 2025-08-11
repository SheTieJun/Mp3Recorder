package me.shetj.recorder.soundtouch

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.os.Message
import android.os.Process
import android.util.Log
import java.io.IOException
import me.shetj.ndk.lame.LameUtils
import me.shetj.recorder.core.BaseRecorder
import me.shetj.recorder.core.ISoundTouchCore
import me.shetj.recorder.core.PlayerListener
import me.shetj.recorder.core.RecordState
import me.shetj.recorder.core.Source

/**
 * 录制MP3 边录边转
 */
internal class STRecorder : BaseRecorder {

    override val recorderType: RecorderType = RecorderType.SIM

    /**
     * 输出的文件
     */
    private var mPCMBuffer: ShortArray? = null
    private var mSendError: Boolean = false

    // 缓冲数量
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

    private val soundTouch: SoundTouchKit by lazy { SoundTouchKit() }

    override fun setContextToPlugConfig(context: Context): BaseRecorder {
        logInfo("STRecorder no use it")
        return this
    }

    override fun setContextToVolumeConfig(context: Context): BaseRecorder {
        logInfo("STRecorder no use it")
        return this
    }

    constructor() {
    }

    /**
     *
     * @param audioSource MediaRecorder.AudioSource.MIC
     */
    constructor(
        @Source audioSource: Int = MediaRecorder.AudioSource.MIC,
        channel: Int = 1
    ) {
        this.mAudioSource = audioSource
        mLameInChannel = channel
        mChannelConfig = when (channel) {
            1 -> {
                AudioFormat.CHANNEL_IN_MONO
            }

            2 -> {
                AudioFormat.CHANNEL_IN_STEREO
            }

            else -> AudioFormat.CHANNEL_IN_STEREO
        }
        is2Channel = mLameInChannel == 2
        releaseAEC()
    }

    override fun getSoundTouch(): ISoundTouchCore {
        return soundTouch
    }

    override fun setAudioChannel(channel: Int): Boolean {
        if (isActive) {
            Log.e(TAG, "setAudioSource error ,need state isn't isActive|录音没有完成，无法进行修改 ")
            return false
        }
        is2Channel = channel == 2
        mLameInChannel = when {
            channel <= 1 -> {
                mChannelConfig = AudioFormat.CHANNEL_IN_MONO
                releaseAEC()
                1
            }

            channel >= 2 -> {
                mChannelConfig = AudioFormat.CHANNEL_IN_STEREO
                releaseAEC()
                2
            }

            else -> 2
        }
        return true
    }

    override fun setAudioSource(audioSource: Int): Boolean {
        if (!isActive) {
            mAudioSource = audioSource
            return true
        }
        Log.e(TAG, "setAudioSource error ,need state isn't isActive|录音没有完成，无法进行修改 ")
        return false
    }

    override fun updateDataEncode(outputFilePath: String, isContinue: Boolean) {
        setOutputFile(outputFilePath, isContinue)
        mEncodeThread?.isContinue = isContinue
        mEncodeThread?.update(outputFilePath)
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
        // 初始化
        duration = 0
        try {
            initAudioRecorder()
            mAudioRecord!!.startRecording()
        } catch (ex: IllegalStateException) {
            handler.sendEmptyMessage(HANDLER_PERMISSION)
            isActive = false
            ex.printStackTrace()
            return
        } catch (ex: Exception) {
            onError(ex)
            return
        }

        object : Thread() {
            var isError = false

            override fun run() {
                // 设置线程权限
                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
                onStart()
                while (isActive) {
                    val readSize = mAudioRecord!!.read(mPCMBuffer!!, 0, mBufferSize)
                    if (readSize == AudioRecord.ERROR_INVALID_OPERATION || readSize == AudioRecord.ERROR_BAD_VALUE) {
                        if (!mSendError) {
                            mSendError = true
                            handler.sendEmptyMessage(HANDLER_PERMISSION)
                            isError = true
                            onError(Exception("recording error , may be need permission :android.permission.RECORD_AUDIO"))
                            logError("recording error , may be need permission :android.permission.RECORD_AUDIO")
                        }
                    } else {
                        if (readSize > 0) {
                            if (isPause) {
                                continue
                            }
                            mEncodeThread!!.addTask(mPCMBuffer!!, readSize, mute)
                            calculateRealVolume(mPCMBuffer!!, readSize)
                            // short 是2个字节 byte 是1个字节8位
                            onRecording(readSize *2)
                        } else {
                            if (!mSendError) {
                                mSendError = true
                                handler.sendEmptyMessage(HANDLER_PERMISSION)
                                logError("recording error , may be need permission :android.permission.RECORD_AUDIO")
                                isError = true
                                onError(Exception("recording error , may be need permission :android.permission.RECORD_AUDIO"))
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
        return this
    }

    @Deprecated("不支持背景音乐", replaceWith = ReplaceWith("no use"))
    override fun setBackgroundMusic(
        context: Context,
        uri: Uri,
        header: MutableMap<String, String>?
    ): BaseRecorder {
        logError("不支持背景音乐")
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
        super.reset()
        soundTouch.clean()
    }

    override fun destroy() {
        super.destroy()
        soundTouch.destroy()
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

    @Deprecated("不支持背景音乐", replaceWith = ReplaceWith("no use"))
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
            mSamplingRate,
            mChannelConfig, DEFAULT_AUDIO_FORMAT.audioFormat
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
        /* Setup audio recorder
        * 音频源：可以使用麦克风作为采集音频的数据源。defaultAudioSource
        * 采样率：一秒钟对声音数据的采样次数，采样率越高，音质越好。defaultSamplingRate
        * 音频通道：单声道，双声道等，defaultChannelConfig
        * 缓冲区大小：音频数据写入缓冲区的总数：mBufferSize
        * */
        mAudioRecord = AudioRecord(
            mAudioSource,
            mSamplingRate, mChannelConfig, DEFAULT_AUDIO_FORMAT.audioFormat,
            mBufferSize
        )

        // 缓冲区大小
        mPCMBuffer = ShortArray(mBufferSize)

        // PCM文件大小 = 采样率采样时间采样位深 / 8*通道数（Bytes）
        bytesPerSecond =
            mAudioRecord!!.sampleRate * mapFormat(mAudioRecord!!.audioFormat) / 8 * mAudioRecord!!.channelCount

        // 初始化变音
        soundTouch.init(mLameInChannel, mSamplingRate)

        initAudioEffect(mAudioRecord!!.audioSessionId)

        LameUtils.init(
            mSamplingRate,
            mLameInChannel,
            mSamplingRate,
            mLameMp3BitRate,
            mMp3Quality,
            lowpassFreq,
            highpassFreq,
            openVBR,
            isDebug
        )
        mEncodeThread = DataSTEncodeThread(
            mRecordFile!!,
            mBufferSize,
            isContinue,
            mChannelConfig == AudioFormat.CHANNEL_IN_STEREO,
            soundTouch,
            openVBR
        )
        mEncodeThread!!.start()
        mEncodeThread!!.setPCMListener(mPCMListener)
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
            recordBufferSize = 0
            duration = 0
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
     * @return boolean false 表示触发了自动完成
     */
    private fun onRecording(readSize: Int): Boolean {
        recordBufferSize += readSize
        duration = (1000.0 * recordBufferSize.toDouble() / bytesPerSecond).toLong()
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
        if (state !== RecordState.STOPPED) {
            isPause = false
            isActive = false
            isAutoComplete = true
            state = RecordState.STOPPED
            backgroundMusicIsPlay = false
        }
    }
}
