@file:Suppress("DEPRECATION")

package me.shetj.recorder.mixRecorder

import android.content.Context
import android.media.*
import android.media.AudioFormat.CHANNEL_OUT_MONO
import android.media.AudioFormat.CHANNEL_OUT_STEREO
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import me.shetj.player.PlayerListener
import me.shetj.recorder.core.PlugConfigs
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.atomic.AtomicBoolean


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
class PlayBackMusic(private var defaultChannel: Int = CHANNEL_OUT_MONO,var plugConfigs: PlugConfigs?) {

    private var mAudioDecoder: AudioDecoder? = null
    private val backGroundBytes =
        LinkedBlockingDeque<ByteArray>()//new ArrayDeque<>();// ArrayDeque不是线程安全的
    var isPlayingMusic = false
        private set
    private var mIsRecording = false
    private var mIsLoop = false
    private var need = AtomicBoolean(false)
    var isIsPause = false
        private set
    private val playHandler: PlayHandler = PlayHandler(this)
    private var audioTrack: AudioTrack? = null
    private var volume = 0.3f
    private val frameListener = object : BackGroundFrameListener {
        override fun onFrameArrive(bytes: ByteArray) {
            //如果设置耳机配置相关
            if (plugConfigs !=null ) {
                //只有连上了耳机才会使用写入的方式，否则只用外放的方式
                if (plugConfigs?.connected == true) {
                    addBackGroundBytes(bytes)
                }
            }else{
                //如果没有设置耳机相关，直接写入和外放都用,可能会有叠音
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

    val bufferSize: Int
        get() = if (mAudioDecoder == null) {
            AudioDecoder.BUFFER_SIZE
        } else {
            mAudioDecoder!!.bufferSize
        }

    private class PlayHandler(private val playBackMusic: PlayBackMusic) :
        Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                PROCESS_STOP, PROCESS_ERROR -> playBackMusic.release()
                PROCESS_REPLAY -> playBackMusic.restartMusic()
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

    fun setBackGroundUrl(context: Context,uri: Uri,header: MutableMap<String,String>?): PlayBackMusic {
        if (isIsPause) {
            releaseDecoder()
            initDecoder(context,uri,header)
        } else {
            isIsPause = true
            releaseDecoder()
            initDecoder(context,uri,header)
            isIsPause = false
        }
        isPlayingMusic = false
        return this
    }

    fun setBackGroundPlayListener(playerListener: PlayerListener) {
        this.playerListener = playerListener
    }

    private fun initDecoder(path: String) {
        mAudioDecoder = AudioDecoder()
        mAudioDecoder?.setMp3FilePath(path)
    }

    private fun initDecoder(context: Context,uri: Uri,header: MutableMap<String,String>?) {
        mAudioDecoder = AudioDecoder()
        mAudioDecoder?.setMp3FilePath(context,uri,header)
    }

    private fun releaseDecoder() {
        if (mAudioDecoder != null) {
            mAudioDecoder?.release()
            mAudioDecoder = null
        }
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
    fun setLoop(isLoop: Boolean) {
        mIsLoop = isLoop
    }

    /**
     * 开始播放
     * @return
     */
    fun startPlayBackMusic() {
        if (mAudioDecoder == null) {
            Log.e("mixRecorder","AudioDecoder no null, please set setBackGroundUrl first")
            return
        }
        //开始加载音乐数据
        initPCMData()
        isPlayingMusic = true
        PlayNeedMixAudioTask(frameListener).start()
        playerListener?.onStart((mAudioDecoder?.mediaFormat?.getLong(MediaFormat.KEY_DURATION)?:1/ 1000).toInt())
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

    /**
     * 暂停播放
     * @return
     */
    fun stop() {
        isPlayingMusic = false
        playerListener?.onStop()
    }

    fun resume() {
        if (isPlayingMusic) {
            isIsPause = false
            need.compareAndSet(false,true)
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

    fun cleanMusic(){
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
        //等于 mAudioDecoder.isPCMExtractorEOS()  = true 表示已经结束了
        //如果是循环模式要重新开始解码
        if (isPCMDataEos && mIsLoop) {
            //重新开始播放mp3 -> pcm
            initPCMData()
        }
        Log.i("PlayBackMusic", "restartMusic")
    }

    /**
     * 解析 mp3 --> pcm
     */
    private fun initPCMData() {
        mAudioDecoder!!.startPcmExtractor()
    }

    /**
     * 虽然可以新建多个 AsyncTask的子类的实例，但是AsyncTask的内部Handler和ThreadPoolExecutor都是static的，
     * 这么定义的变 量属于类的，是进程范围内共享的，所以AsyncTask控制着进程范围内所有的子类实例，
     * 而且该类的所有实例都共用一个线程池和Handler
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
                //延迟合成
                fixAudioZip()
                while (isPlayingMusic) {
                    if (!isIsPause) {
                        if (need.compareAndSet(true,false)){
                            fixAudioZip()
                        }
                        val pcm = mAudioDecoder!!.pcmData
                        val temp = pcm?.bufferBytes
                        if (pcm == null || temp == null) {
                            if (mIsLoop) {
                                playHandler.sendEmptyMessage(PROCESS_REPLAY)
                                sleep(100)
                            }
                            continue
                        }
                        audioTrack!!.write(temp, 0, temp.size)
                        if (mAudioDecoder != null && mAudioDecoder!!.mediaFormat != null) {
                            playerListener?.onProgress(
                                (pcm.time / 1000).toInt(),
                                (mAudioDecoder!!.mediaFormat!!.getLong(MediaFormat.KEY_DURATION) / 1000).toInt()
                            )
                        }
                        listener?.onFrameArrive(temp)
                    } else {
                        //如果是暂停
                        try {
                            //防止死循环ANR
                            sleep(500)
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }
                    }
                }
                playerListener?.onStop()
                audioTrack!!.stop()
                audioTrack!!.flush()
                audioTrack!!.release()
                audioTrack = null
            } catch (e: Exception) {
                Log.e("mp3Recorder", "error:" + e.message)
                playerListener?.onError(e)
            } finally {
                isPlayingMusic = false
                isIsPause = false
                playerListener?.onCompletion()
            }
        }

        private fun fixAudioZip() {
            if (defaultChannel == CHANNEL_OUT_MONO) {
                //音乐实际开始会慢一点
                repeat(10) {
                    listener?.onFrameArrive(ByteArray(1))
                }
            } else {
                //30 的时候 外放 快于 合成
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                audioTrack!!.setVolume(volume)
            } else {
                audioTrack!!.setStereoVolume(volume, volume)
            }
        }
    }

    internal fun updateChannel(channel: Int) {
        defaultChannel = when {
            channel <= 1 -> {
                CHANNEL_OUT_MONO
            }
            channel >= 2 -> {
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
        private const val PROCESS_ERROR = 4
        private const val PROCESS_REPLAY = 5
        private const val mSampleRate = 44100
        private const val mAudioEncoding = AudioFormat.ENCODING_PCM_16BIT//一个采样点16比特-2个字节
    }
}
