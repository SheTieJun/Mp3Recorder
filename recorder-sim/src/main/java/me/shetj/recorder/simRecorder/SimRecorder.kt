/*
 * MIT License
 *
 * Copyright (c) 2019 SheTieJun
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package me.shetj.recorder.simRecorder

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
import me.shetj.player.AudioPlayer
import me.shetj.player.PlayerListener
import me.shetj.recorder.core.BaseRecorder
import me.shetj.recorder.core.Channel
import me.shetj.recorder.core.PermissionListener
import me.shetj.recorder.core.RecordListener
import me.shetj.recorder.core.RecordState
import me.shetj.recorder.core.Source
import me.shetj.recorder.core.VolumeConfig

/**
 * 录制MP3 边录边转
 */
internal class SimRecorder : BaseRecorder {

    override val recorderType: RecorderType = RecorderType.SIM

    private var mAudioRecord: AudioRecord? = null
    private var mEncodeThread: DataEncodeThread? = null
    private var backgroundPlayer: AudioPlayer? = null
    private var context: Context? = null

    /**
     * 输出的文件
     */
    private var mPCMBuffer: ShortArray? = null
    private var mSendError: Boolean = false
    private var bytesPerSecond: Int = 0 // PCM文件大小=采样率采样时间采样位深/8*通道数（Bytes）

    // 缓冲数量
    private var mBufferSize: Int = 0

    /**
     * pcm数据的速度，默认300
     * 数据越大，速度越慢
     */
    private var waveSpeed = 500
        set(waveSpeed) {
            if (this.waveSpeed <= 0) {
                return
            }
            field = waveSpeed
        }

    // 背景音乐相关
    private var backgroundMusicUrl: String? = null
    private var backgroundMusicPlayerListener: PlayerListener? = null
    private var backgroundMusicUri: Uri? = null
    private var header: MutableMap<String, String>? = null

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

    constructor() {
    }

    /**
     *
     * @param audioSource MediaRecorder.AudioSource.MIC
     */
    constructor(@Source audioSource: Int = MediaRecorder.AudioSource.MIC, @Channel channel: Int = 1) {
        this.defaultAudioSource = audioSource
        this.defaultLameInChannel = channel
        this.defaultChannelConfig = when (channel) {
            1 -> {
                AudioFormat.CHANNEL_IN_MONO
            }
            2 -> {
                AudioFormat.CHANNEL_IN_STEREO
            }
            else -> defaultAudioSource
        }
        this.is2Channel = defaultLameInChannel == 2
        releaseAEC()
    }

    override fun setAudioChannel(@Channel channel: Int): Boolean {
        if (isActive) {
            Log.e(TAG, "setAudioChannel error ,need state isn't isActive|录音没有完成，无法进行修改 ")
            return false
        }
        this.is2Channel = channel == 2
        this.defaultLameInChannel = channel
        this.defaultChannelConfig = when (channel) {
            1 -> {
                AudioFormat.CHANNEL_IN_MONO
            }
            2 -> {
                AudioFormat.CHANNEL_IN_STEREO
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
        Log.e(TAG, "setAudioChannel error ,need state isn't isActive|录音没有完成，无法进行修改 ")
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
    override fun setRecordListener(recordListener: RecordListener?): SimRecorder {
        this.mRecordListener = recordListener
        return this
    }

    override fun setPermissionListener(permissionListener: PermissionListener?): SimRecorder {
        this.mPermissionListener = permissionListener
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
        // 初始化
        duration = 0
        try {
            initAudioRecorder()
            mAudioRecord!!.startRecording()
        }catch (ex:IllegalStateException){
            handler.sendEmptyMessage(HANDLER_PERMISSION)
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
                            onError()
                            isError = true
                        }
                    } else {
                        if (readSize > 0) {
                            if (isPause) {
                                continue
                            }
                            mEncodeThread!!.addTask(mPCMBuffer!!, readSize,mute)
                            calculateRealVolume(mPCMBuffer!!, readSize)
                            // short 是2个字节 byte 是1个字节8位
                            onRecording(readSize* 2)
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
            isActive = false
            state = RecordState.STOPPED
            backgroundMusicIsPlay = false
            bgPlayer.stopPlay()
        }
    }

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

    override fun reset() {
        isActive = false
        isPause = false
        state = RecordState.STOPPED
        duration = 0L
        recordBufferSize = 0
        mRecordFile = null
        backgroundMusicIsPlay = bgPlayer.isPlaying
        handler.sendEmptyMessage(HANDLER_RESET)
        bgPlayer.stopPlay()
    }

    override fun destroy() {
        isActive = false
        isPause = false
        state = RecordState.STOPPED
        duration = 0L
        recordBufferSize = 0
        mRecordFile = null
        context = null
        releaseAEC()
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
        /* Setup audio recorder
              * 音频源：可以使用麦克风作为采集音频的数据源。defaultAudioSource
              * 采样率：一秒钟对声音数据的采样次数，采样率越高，音质越好。defaultSamplingRate
              * 音频通道：单声道，双声道等，defaultChannelConfig
              * 缓冲区大小：音频数据写入缓冲区的总数：mBufferSize
              * */
        mAudioRecord = AudioRecord(
            defaultAudioSource,
            defaultSamplingRate, defaultChannelConfig, DEFAULT_AUDIO_FORMAT.audioFormat,
            mBufferSize
        )
        mPCMBuffer = ShortArray(mBufferSize)

        // 1秒时间需要多少字节，用来计算已经录制了多久
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
        mEncodeThread = DataEncodeThread(
            mRecordFile!!,
            mBufferSize,
            isContinue,
            defaultChannelConfig == AudioFormat.CHANNEL_IN_STEREO
        )
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
            recordBufferSize = 0L
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

    private fun onError(ex: Exception? = null) {
        isPause = false
        isActive = false
        val message = Message.obtain()
        message.what = HANDLER_ERROR
        message.obj = ex
        handler.sendMessage(message)
        state = RecordState.STOPPED
        backgroundMusicIsPlay = false
        bgPlayer.stopPlay()
    }

    /**
     * 计算时间
     * @param readSize 字节数
     */
    private fun onRecording(readSize: Int) {
        recordBufferSize += readSize
        duration = ((recordBufferSize*1000.0)/bytesPerSecond).toLong()
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
            bgPlayer.stopPlay()
        }
    }
}
