
@file:Suppress("DEPRECATION")

package me.shetj.recorder.mixRecorder

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioFormat.CHANNEL_OUT_MONO
import android.media.AudioFormat.CHANNEL_OUT_STEREO
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.atomic.AtomicBoolean
import me.shetj.player.PlayerListener
import me.shetj.recorder.core.BaseRecorder
import me.shetj.recorder.core.Channel
import me.shetj.recorder.core.PlugConfigs

/**
 * 播放音乐，用来播放PCM
 *
 * 1.支持暂停 pause(),resume() <br>
 * 2.支持循环播放setLoop(boolean isLoop)<br>
 * 3. 支持切换背景音乐 setBackGroundUrl(String path)<br>
 * update 2019年10月11日
 * 添加时间进度记录(注意返回不是在主线程需要自己设置在主线程)
 *
 * TODO seekTo 缺失功能
 *
 */
internal class PlayBackMusic(private var defaultChannel: Int = CHANNEL_OUT_MONO, var plugConfigs: PlugConfigs?) {

    var isPlayingMusic = false
        private set
    var isIsPause = false
        private set
    val bufferSize: Int
        get() = if (mAudioDecoder == null) {
            AudioDecoder.BUFFER_SIZE
        } else {
            mAudioDecoder!!.bufferSize
        }

    private var mAudioDecoder: AudioDecoder? = null
    private val backGroundBytes = LinkedBlockingDeque<ByteArray>() // new ArrayDeque<>();// ArrayDeque不是线程安全的
    private var mIsRecording = false // 路由中
    private var mIsLoop = false // 循环
    private var need = AtomicBoolean(false) // 是否需要进行延迟
    private val playHandler: PlayHandler = PlayHandler(this)
    private var audioTrack: AudioTrack? = null
    private var volume = 0.3f
    private val frameListener = object : BackGroundFrameListener {
        override fun onFrameArrive(bytes: ByteArray) {
            // 如果设置耳机配置相关
            if (plugConfigs != null) {
                // 只有连上了耳机才会使用写入的方式，否则只用外放的方式
                if (plugConfigs?.needBGBytes() == true) {
                    addBackGroundBytes(bytes)
                }
            } else {
                // 如果没有设置耳机相关，直接写入和外放都用,可能会有叠音
                addBackGroundBytes(bytes)
            }
        }
    }
    private var playerListener: PlayerListener? = null

    private val isPCMDataEos: Boolean
        get() = if (mAudioDecoder == null) {
            true
        } else {
            mAudioDecoder!!.isPCMExtractorEOS
        }

    private class PlayHandler(private val playBackMusic: PlayBackMusic) : Handler(Looper.getMainLooper()) {

        private var playerListener: PlayerListener? = null

        fun setPlayListener(playerListener: PlayerListener) {
            this.playerListener = playerListener
        }

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                PROCESS_ERROR -> {
                    playBackMusic.release()
                    playerListener?.onError(msg.obj as? Exception)
                }
                PROCESS_REPLAY -> {
                    playBackMusic.restartMusic()
                }
                PROCESS_PLAYING -> {
                    playerListener?.onProgress(msg.arg1, msg.arg2)
                }
                PROCESS_STOP -> {
                    playBackMusic.release()
                    playerListener?.onStop()
                }
                PROCESS_COMPLETE -> {
                    playerListener?.onCompletion()
                }
            }
        }
    }

    /**
     * 设置或者切换背景音乐
     * @param path
     * @return
     */
    fun setBackGroundUrl(path: String): PlayBackMusic {
        if (isIsPause) {
            releaseDecoder()
            initDecoder(path)
        } else {
            isIsPause = true
            releaseDecoder()
            initDecoder(path)
            isIsPause = false
        }
        return this
    }

    fun setBackGroundUrl(context: Context, uri: Uri, header: MutableMap<String, String>?): PlayBackMusic {
        if (isIsPause) {
            releaseDecoder()
            initDecoder(context, uri, header)
        } else {
            isIsPause = true
            releaseDecoder()
            initDecoder(context, uri, header)
            isIsPause = false
        }
        return this
    }

    fun setBackGroundPlayListener(playerListener: PlayerListener) {
        this.playerListener = playerListener
        playHandler.setPlayListener(playerListener)
    }

    private fun initDecoder(path: String) {
        mAudioDecoder = AudioDecoder()
        mAudioDecoder?.setMp3FilePath(path)
    }

    private fun initDecoder(context: Context, uri: Uri, header: MutableMap<String, String>?) {
        mAudioDecoder = AudioDecoder()
        mAudioDecoder?.setMp3FilePath(context, uri, header)
    }

    private fun releaseDecoder() {
        if (mAudioDecoder != null) {
            mAudioDecoder?.release()
            mAudioDecoder = null
        }
        isPlayingMusic = false
    }

    /**
     * mIsRecording 标识外部是否正在录音，只有开始录音
     * @param enable
     * @return
     */
    fun setNeedRecodeDataEnable(enable: Boolean): PlayBackMusic {
        mIsRecording = enable
        return this
    }

    /**
     * 是否循环播放
     * @param isLoop 是否循环
     */
    fun setLoop(isLoop: Boolean): PlayBackMusic {
        mIsLoop = isLoop
        return this
    }

    /**
     * 开始播放
     * @return
     */
    fun startPlayBackMusic() {
        if (mAudioDecoder == null) {
            Log.e(BaseRecorder.TAG, "AudioDecoder no null, please set setBackGroundUrl first")
            return
        }
        // 开始加载音乐数据
        initPCMData()
        isPlayingMusic = true
        PlayNeedMixAudioTask(frameListener).start()
        playerListener?.onStart(
            ((mAudioDecoder?.mediaFormat?.getLong(MediaFormat.KEY_DURATION) ?: 1) / 1000).toInt()
        )
    }

    fun getBackGroundBytes(): ByteArray? {
        if (backGroundBytes.isEmpty()) {
            return null
        }
        return backGroundBytes.poll()
    }

    fun hasFrameBytes(): Boolean {
        return !backGroundBytes.isEmpty()
    }

    fun frameBytesSize(): Int {
        return backGroundBytes.size
    }

    fun stop() {
        isPlayingMusic = false
        playHandler.sendEmptyMessage(PROCESS_STOP)
    }

    fun resume() {
        if (isPlayingMusic) {
            isIsPause = false
            need.compareAndSet(false, true)
            playerListener?.onResume()
        }
    }

    fun pause() {
        if (isPlayingMusic) {
            isIsPause = true
            playerListener?.onPause()
        }
    }

    fun release(): PlayBackMusic {
        isPlayingMusic = false
        isIsPause = false
        mAudioDecoder?.release()
        backGroundBytes.clear()
        return this
    }

    fun cleanMusic() {
        releaseDecoder()
    }

    /**
     * 这样的方式控制同步 需要添加到队列时判断同时在播放和录制
     */
    private fun addBackGroundBytes(bytes: ByteArray) {
        if (isPlayingMusic && mIsRecording) {
            backGroundBytes.add(bytes) // what if out of memory?
        }
    }

    /**
     * 重新开始播放
     */
    private fun restartMusic() {
        // 等于 mAudioDecoder.isPCMExtractorEOS()  = true 表示已经结束了
        // 如果是循环模式要重新开始解码
        if (isPCMDataEos && mIsLoop) {
            // 重新开始播放mp3 -> pcm
            initPCMData()
        }
        Log.i(BaseRecorder.TAG, "restartMusic")
    }

    /**
     * 解析 mp3 --> pcm
     */
    private fun initPCMData() {
        Log.i(BaseRecorder.TAG, "initPCMData")
        mAudioDecoder!!.startPcmExtractor()
    }

    /**
     * 这里新开一个线程
     * 自己解析出来 pcm data
     */
    private inner class PlayNeedMixAudioTask(private val listener: BackGroundFrameListener?) :
        Thread() {
        override fun run() {
            try {
                if (audioTrack == null) {
                    audioTrack = initAudioTrack()
                    setVolume(volume)
                }
                // 开始播放
                audioTrack!!.play()
                // 延迟合成
                fixAudioZip()
                while (isPlayingMusic) {
                    if (!isIsPause) {
                        if (need.compareAndSet(true, false)) {
                            fixAudioZip()
                        }
                        val pcm = mAudioDecoder!!.pcmData
                        val temp = pcm?.bufferBytes
                        if (pcm == null || temp == null) {
                            if (mIsLoop) {
                                Log.e(BaseRecorder.TAG, "PlayBackMusic start Loop ")
                                playHandler.sendEmptyMessage(PROCESS_REPLAY)
                                sleep(99)
                            }
                            continue
                        }
                        audioTrack!!.write(temp, 0, temp.size)
                        if (mAudioDecoder != null && mAudioDecoder!!.mediaFormat != null) {
                            playHandler.sendMessage(Message.obtain().apply {
                                this.what = PROCESS_PLAYING
                                this.arg1 = (pcm.time / 1000).toInt()
                                this.arg2 =
                                    (mAudioDecoder!!.mediaFormat!!.getLong(MediaFormat.KEY_DURATION) / 1000).toInt()
                            })
                        }
                        listener?.onFrameArrive(temp)
                    } else {
                        // 如果是暂停
                        try {
                            // 防止死循环ANR
                            sleep(500)
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }
                    }
                }
                audioTrack!!.stop()
                audioTrack!!.flush()
                audioTrack!!.release()
                audioTrack = null
            } catch (e: Exception) {
                Log.e(BaseRecorder.TAG, "PlayBackMusic error:" + e.message)
                playHandler.sendMessage(Message.obtain().apply {
                    what = PROCESS_ERROR
                    obj = e
                })
            } finally {
                isPlayingMusic = false
                isIsPause = false
                playHandler.sendEmptyMessage(PROCESS_COMPLETE)
            }
        }

        private fun fixAudioZip() {
            if (defaultChannel == CHANNEL_OUT_MONO) {
                // 音乐实际开始会慢一点
                repeat(10) {
                    listener?.onFrameArrive(ByteArray(1))
                }
            } else {
                // 30 的时候 外放 快于 合成
                repeat(8) {
                    listener?.onFrameArrive(ByteArray(1))
                }
            }
        }
    }

    private fun initAudioTrack(): AudioTrack {
        val bufferSize = AudioTrack.getMinBufferSize(
            mSampleRate,
            defaultChannel, mAudioEncoding
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(mAudioEncoding)
                        .setSampleRate(mSampleRate)
                        .setChannelMask(defaultChannel)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(bufferSize)
                .build()
        } else {
            return AudioTrack(
                AudioManager.STREAM_MUSIC,
                mSampleRate, defaultChannel, mAudioEncoding, bufferSize,
                AudioTrack.MODE_STREAM
            )
        }
    }

    fun setVolume(volume: Float) {
        this.volume = volume
        if (audioTrack != null) {
            audioTrack!!.setVolume(volume)
        }
    }

    /**
     * 更新声道数量
     */
    internal fun updateChannel(@Channel channel: Int) {
        defaultChannel = when (channel) {
            1 -> {
                CHANNEL_OUT_MONO
            }
            2 -> {
                CHANNEL_OUT_STEREO
            }
            else -> CHANNEL_OUT_MONO
        }
        audioTrack = null
    }

    internal interface BackGroundFrameListener {
        fun onFrameArrive(bytes: ByteArray)
    }

    companion object {
        private const val PROCESS_STOP = 3
        private const val PROCESS_ERROR = PROCESS_STOP + 1
        private const val PROCESS_REPLAY = PROCESS_ERROR + 1
        private const val PROCESS_PLAYING = PROCESS_REPLAY + 1
        private const val PROCESS_COMPLETE = PROCESS_PLAYING + 1
        private const val mSampleRate = 44100
        private const val mAudioEncoding = AudioFormat.ENCODING_PCM_16BIT // 一个采样点16比特-2个字节
    }
}
