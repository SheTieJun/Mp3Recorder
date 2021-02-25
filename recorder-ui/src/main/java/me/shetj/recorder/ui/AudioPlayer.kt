package me.shetj.recorder.ui


import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.TextUtils
import me.shetj.player.PlayerListener
import java.util.concurrent.atomic.AtomicBoolean


/**
 * **@author：** shetj<br></br>
 * **@createTime：** 2018/10/23 0023<br></br>
 * **@email：** 375105540@qq.com<br></br>
 * **@describe** [AudioPlayer] 音乐播放<br></br>
 * ** 播放 [AudioPlayer.playOrPause]}**<br></br>
 * ** 设置但是不播放 [AudioPlayer.playNoStart]**<br></br>
 * ** 暂停  [AudioPlayer.pause] <br></br>
 * ** 恢复  [AudioPlayer.resume] <br></br>
 * ** 停止  [AudioPlayer.stopPlay] <br></br>
 * ** 滑动seekBar 停止计时  [AudioPlayer.stopProgress]   <br></br>
 * ** 开始计时  [AudioPlayer.startProgress]   <br></br>
 * 如果不想通过焦点控制，请不要设置[setAudioManager]
 * <br></br>
 ********** */
class AudioPlayer : MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
    MediaPlayer.OnCompletionListener, MediaPlayer.OnSeekCompleteListener {

    private val HANDLER_PLAYING = 0x201 //正在录音
    private val HANDLER_START = 0x202   //开始了
    private val HANDLER_COMPLETE = 0x203//完成
    private val HANDLER_ERROR = 0x205   //错误
    private val HANDLER_PAUSE = 0x206   //暂停
    private val HANDLER_RESUME = 0x208  //暂停后开始
    private val HANDLER_RESET = 0x209   //重置

    private var mediaPlayer: MediaPlayer? = null
    private var mAudioManager: AudioManager? = null
    private var context: Context? = null

    private var focusChangeListener =
        OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                    // 获取焦点
                    setVolume(1.0f)
                }
                AudioManager.AUDIOFOCUS_LOSS -> {
                    // 失去焦点很长一段时间，必须停止所有的audio播放，清理资源
                    pause()
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    // 暂时失去焦点，但是很快就会重新获得，在此状态应该暂停所有音频播放，但是不能清除资源
                    pause()
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    // 暂时失去焦点，但是允许持续播放音频(以很小的声音)，不需要完全停止播放。
                    setVolume(0.1f)
                }
            }
        }

    /**
     * 播放回调
     */
    private var listener: PlayerListener? = null

    /**
     * 获取当前播放的url
     * @return currentUrl
     */
    var currentUrl :String ?= null
        private set

    var currentUri: Uri? = null
        private set

    private var header: MutableMap<String, String>? = null

    /**
     * [AudioPlayer.onPrepared] and [AudioPlayer.onSeekComplete]
     * true 才会会开始, false 会暂停
     */
    private val isPlay = AtomicBoolean(true)

    /**
     * 是否是循环[setLoop]
     */
    private var isLoop: Boolean = false

    /**
     * [AudioPlayer.onPrepared]
     * 如果大于0 表示，不是从头开始,每次使用过后重置
     */
    private var seekToPlay = 0


    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                HANDLER_PLAYING -> if (listener != null
                    && mediaPlayer != null
                    && mediaPlayer!!.isPlaying
                    && !hasMessages(HANDLER_PLAYING)
                ) {
                    listener!!.onProgress(mediaPlayer!!.currentPosition, mediaPlayer!!.duration)
                    this.sendEmptyMessageDelayed(HANDLER_PLAYING, 300)
                }
                HANDLER_START -> {
                    if (listener != null && mediaPlayer != null) {
                        listener!!.onStart(mediaPlayer!!.duration)
                    }
                    if (mAudioManager != null) {
                        requestAudioFocus()
                        mAudioManager!!.mode = AudioManager.MODE_NORMAL
                    }
                }
                HANDLER_RESUME -> {
                    listener?.onResume()
                    if (mAudioManager != null) {
                        requestAudioFocus()
                        mAudioManager!!.mode = AudioManager.MODE_NORMAL
                    }
                }
                HANDLER_COMPLETE -> listener?.onCompletion()
                HANDLER_ERROR -> listener?.onError(msg.obj as Exception)
                HANDLER_PAUSE -> listener?.onPause()
                HANDLER_RESET -> listener?.onStop()
            }
        }
    }

    /**
     * 是否暂停
     * @return
     */
    val isPause: Boolean
        get() = !isPlaying

    /**
     * 是否正在播放
     * @return
     */
    val isPlaying: Boolean
        get() = mediaPlayer != null && mediaPlayer!!.isPlaying

    val currentPosition: Int
        get() = if (mediaPlayer != null) {
            mediaPlayer!!.duration
        } else 0


    constructor() {
        initMedia()
    }

    constructor(audioManager: AudioManager) {
        this.mAudioManager = audioManager
        initMedia()
    }

    /**
     * 设置 [AudioManager] 获取声道
     * @param context 上下文
     */
    fun setAudioManager(context: Context) {
        mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    @Suppress("DEPRECATION")
    fun requestAudioFocus() {
        if (mAudioManager == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {            //Android 8.0+
            val audioFocusRequest =
                AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setOnAudioFocusChangeListener(focusChangeListener).build()
            audioFocusRequest.acceptsDelayedFocusGain()
            mAudioManager!!.requestAudioFocus(audioFocusRequest)
        } else {
            mAudioManager!!.requestAudioFocus(
                focusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }
    }

    /**
     * 设置声音
     * @param volume
     */
    fun setVolume(volume: Float) {
        if (mediaPlayer != null) {
            mediaPlayer!!.setVolume(volume, volume)
        }
    }

    /**
     * 通过url比较
     * 1.进行播放
     * 2.暂停操作
     * 每一个url 和 [PlayerListener] 是一对一存在的
     * @param url 播放的url
     * @param listener 监听变化
     */
    fun playOrPause(url: String?, listener: PlayerListener?) {
        //判断是否是当前播放的url
        if (url == currentUrl && mediaPlayer != null) {
            if (listener != null) {
                this.listener = listener
            }
            if (mediaPlayer!!.isPlaying) {
                pause()
            } else {
                resume()
            }
        } else {
            pause() //先停下来，有些手机会报文件结束异常的错误
            //先让当前url回调结束
            this.listener?.onCompletion()
            //直接播放
            play(url, listener)
        }
    }

    fun playOrPause(context: Context, uri: Uri?, header: MutableMap<String, String>?, listener: PlayerListener?) {
        //判断是否是当前播放的url
        if (uri == currentUri && mediaPlayer != null) {
            if (listener != null) {
                this.listener = listener
            }
            if (mediaPlayer!!.isPlaying) {
                pause()
            } else {
                resume()
            }
        } else {
            pause() //先停下来，有些手机会报文件结束异常的错误
            //先让当前url回调结束
            this.listener?.onCompletion()
            //直接播放
            play(context, uri, header, listener)
        }
    }

    /**
     * 只有在【不播放】的时候设置起效
     * 如果是【播放中】，请使用 [seekTo]
     */
    fun setSeekToPlay(seekToPosition: Int) {
        if (isPause) {
            seekToPlay = seekToPosition
        } else {
            seekTo(seekToPosition)
        }
    }

    /**
     * 只更新listener
     */
    fun updateListener(listener: PlayerListener?) {
        this.listener = listener
    }

    /**
     * 播放url
     * @param url 播放的url
     * @param listener 监听变化
     */
    private fun play(url: String?, listener: PlayerListener? = null) {

        if (TextUtils.isEmpty(url)) {
            listener?.onError(RuntimeException("url can not be null"))
        }
        url?.let {
            this.listener = listener
            currentUrl = url
            initMedia()
            configMediaPlayer()
        }
    }

    private fun play(
        context: Context,
        uri: Uri?,
        header: MutableMap<String, String>? = null,
        listener: PlayerListener? = null
    ) {

        if (uri == null) {
            listener?.onError(Exception("uri can not be null"))
        }
        uri?.let {
            this.listener = listener
            currentUri = uri
            this.context = context
            this.header = header
            initMedia()
            configMediaPlayer()
        }
    }

    /**
     * 设置 但是 【不播放】
     * 作用：用来记录还没开始播放，就拖动了【注意是为了列表播放】
     * 可以用 [seekToPlay] 代替，但是在列表时，需要处理好情况
     * @param url 文件路径
     * @param listener 回调监听
     */
    fun playNoStart(url: String?, listener: PlayerListener? = null) {
        if (TextUtils.isEmpty(url)) {
            listener?.onError(Exception("url can not be null"))
        }
        url?.let {
            this.listener = listener
            setIsPlay(false)
            currentUrl = url
            initMedia()
            configMediaPlayer()
        }
    }

    fun playNoStart(
        context: Context,
        uri: Uri?,
        header: MutableMap<String, String>? = null,
        listener: PlayerListener? = null
    ) {
        if (uri == null) {
            listener?.onError(Exception("uri can not be null"))
        }
        uri?.let {
            this.listener = listener
            setIsPlay(false)
            currentUri = uri
            this.context = context
            this.header = header
            initMedia()
            configMediaPlayer()
        }
    }


    /**
     * 暂停，并且停止计时
     */
    fun pause() {
        if (mediaPlayer != null && mediaPlayer!!.isPlaying
            && (!TextUtils.isEmpty(currentUrl)||currentUri != null)
        ) {
            stopProgress()
            mediaPlayer!!.pause()
            handler.sendEmptyMessage(HANDLER_PAUSE)
        }
    }

    /**
     * 恢复，并且开始计时
     */
    fun resume() {
        if (isPause && (!TextUtils.isEmpty(currentUrl)||currentUri != null)) {
            mediaPlayer!!.start()
            if (seekToPlay != 0) {
                mediaPlayer!!.seekTo(seekToPlay)
                seekToPlay = 0
            }
            handler.sendEmptyMessage(HANDLER_RESUME)
            startProgress()
        }
    }

    fun stopPlay() {
        try {
            if (null != mediaPlayer) {
                stopProgress()
                mediaPlayer!!.stop()
                handler.sendEmptyMessage(HANDLER_RESET)
                release()
            }
        } catch (e: Exception) {
            release()
        }

    }

    /**
     * 外部设置进度变化
     */
    fun seekTo(seekTo: Int) {
        if (mediaPlayer != null && (!TextUtils.isEmpty(currentUrl)||currentUri != null)) {
            setIsPlay(!isPause)
            mediaPlayer!!.start()
            mediaPlayer!!.seekTo(seekTo)
        }
    }

    fun reset() {
        pause()
        seekTo(0)
    }


    /**
     * 修改是否循环
     * @param isLoop
     */
    fun setLoop(isLoop: Boolean) {
        this.isLoop = isLoop
        if (mediaPlayer != null) {
            mediaPlayer!!.isLooping = isLoop
        }
    }

    /**
     * 开始计时
     * 使用场景：拖动结束
     */
    fun startProgress() {
        handler.sendEmptyMessage(HANDLER_PLAYING)
    }

    /**
     * 停止计时
     * 使用场景：拖动进度条
     */
    fun stopProgress() {
        handler.removeMessages(HANDLER_PLAYING)
    }


    private fun configMediaPlayer() {
        try {
            mediaPlayer!!.reset()
            if (currentUrl != null) {
                mediaPlayer!!.setDataSource(currentUrl)
            } else if (currentUri != null && context != null) {
                mediaPlayer!!.setDataSource(context!!, currentUri!!, header)
            }
            mediaPlayer!!.prepareAsync()
            mediaPlayer!!.setOnPreparedListener(this)
            mediaPlayer!!.setOnErrorListener(this)
            mediaPlayer!!.setOnCompletionListener(this)
            mediaPlayer!!.setOnSeekCompleteListener(this)
            mediaPlayer!!.isLooping = isLoop
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 清空播放信息
     */
    private fun release() {
        currentUrl = null
        currentUri = null
        //释放MediaPlay
        if (null != mediaPlayer) {
            mediaPlayer!!.release()
            mediaPlayer = null
        }
    }

    /**
     * 设置是否是播放状态
     * @param isPlay
     */
    private fun setIsPlay(isPlay: Boolean) {
        this.isPlay.set(isPlay)
    }

    /**
     * 设置媒体
     */
    @Suppress("DEPRECATION")
    private fun initMedia() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val attrBuilder = AudioAttributes.Builder()
                attrBuilder.setLegacyStreamType(AudioManager.STREAM_MUSIC)
                mediaPlayer!!.setAudioAttributes(attrBuilder.build())
            } else {
                mediaPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
            }
        }
    }

    override fun onPrepared(mp: MediaPlayer) {
        if (seekToPlay != 0) {
            mp.seekTo(seekToPlay)
            seekToPlay = 0
        }
        if (!isPlay.get()) {
            setIsPlay(true)
            return
        }
        mp.start()
        handler.sendEmptyMessage(HANDLER_START)
        startProgress()
    }

    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        val message = Message.obtain()
        message.what = HANDLER_ERROR
        message.obj =
            RuntimeException(
                String.format(
                    "what = %s extra = %s",
                    what.toString(),
                    extra.toString()
                )
            )
        handler.sendMessage(message)
        release()
        return true
    }

    override fun onCompletion(mp: MediaPlayer) {
        if (!mp.isLooping) {
            stopProgress()
            release()
            listener?.onCompletion()
        }
    }

    override fun onSeekComplete(mp: MediaPlayer) {
        if (!isPlay.get()) {
            setIsPlay(true)
            pause()
        }
    }

}
