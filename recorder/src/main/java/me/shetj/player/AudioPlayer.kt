package me.shetj.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.TextUtils


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
 * ** 停止计时  [AudioPlayer.stopProgress]   <br></br>
 * ** 开始计时  [AudioPlayer.startProgress]   <br></br>
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
    /**
     * 播放回调
     */
    private var listener: PlayerListener? = null
    /**
     * 获取当前播放的url
     * @return currentUrl
     */
    var currentUrl = ""
        private set
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
                HANDLER_PLAYING -> if (listener != null && mediaPlayer != null && mediaPlayer!!.isPlaying) {
                    listener!!.onProgress(mediaPlayer!!.currentPosition, mediaPlayer!!.duration)
                    this.sendEmptyMessageDelayed(HANDLER_PLAYING, 300)
                }
                HANDLER_START -> {
                    if (listener != null && mediaPlayer != null) {
                        listener!!.onStart(currentUrl, mediaPlayer!!.duration)
                    }
                    if (mAudioManager != null) {
                        mAudioManager!!.mode = AudioManager.MODE_NORMAL
                    }
                }
                HANDLER_RESUME -> {
                    listener?.onResume()
                    if (mAudioManager != null) {
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

    val duration: Int
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

    //	/**
    //	 * 如果想使用边下载边播放 就使用该方法获取到新的url 再进行播放
    //	 * @param context 上下文
    //	 * @param urlString 播放的URL,必须是HTTP
    //	 * @return 下载的链接地址
    //	 */
    //	public String getCacheUrl(Context context, String urlString){
    //		if (context != null  && !TextUtils.isEmpty(urlString) && urlString.startsWith("http")) {
    //			return  Manager.newInstance().getProxy(context).getProxyUrl(urlString);
    //		}
    //		return urlString;
    //	}


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
     * 通过url 比较 进行播放 还是暂停操作
     * @param url 播放的url
     * @param listener 监听变化
     */
    fun playOrPause(url: String, listener: PlayerListener?) {
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
            //直接播放
            play(url, listener)
        }
    }


    /**
     * 播放url
     * @param url 播放的url
     * @param listener 监听变化
     */
    private fun play(url: String?, listener: PlayerListener? =null) {

        if (TextUtils.isEmpty(url)){
            listener?.onError(Exception("url can not be null"))
        }
        url?.let {
            if (null != listener) {
                this.listener = listener
            }
            currentUrl = url
            initMedia()
            configMediaPlayer()
        }
    }

    /**
     * 设置 但是不播放
     * @param url 文件路径
     * @param listener 回调监听
     */
    fun playNoStart(url: String?, listener: PlayerListener? = null) {

        if (TextUtils.isEmpty(url)){
            listener?.onError(Exception("url can not be null"))
        }
        url?.let {
            if (null != listener) {
                this.listener = listener
            }
            setIsPlay(false)
            currentUrl = url
            initMedia()
            configMediaPlayer()
        }
    }

    private fun configMediaPlayer() {
        try {
            mediaPlayer!!.reset()
            mediaPlayer!!.setDataSource(currentUrl)
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
     * 暂停，并且停止计时
     */
    fun pause() {
        if (mediaPlayer != null && mediaPlayer!!.isPlaying
            && !TextUtils.isEmpty(currentUrl)
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
        if (isPause && !TextUtils.isEmpty(currentUrl)) {
            mediaPlayer!!.start()
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
        if (mediaPlayer != null && !TextUtils.isEmpty(currentUrl)) {
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
     * 清空播放信息
     */
    private fun release() {
        currentUrl = ""
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


    /**
     * 设置媒体
     */
    private fun initMedia() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
            val attrBuilder = AudioAttributes.Builder()
            attrBuilder.setLegacyStreamType(AudioManager.STREAM_MUSIC)
            mediaPlayer!!.setAudioAttributes(attrBuilder.build())
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
        startProgress()
        handler.sendEmptyMessage(HANDLER_START)
    }

    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        val message = Message.obtain()
        message.what = HANDLER_ERROR
        message.obj =
            Exception(String.format("what = %s extra = %s", what.toString(), extra.toString()))
        handler.sendMessage(message)
        release()
        return true
    }

    override fun onCompletion(mp: MediaPlayer) {
        if (!mp.isLooping) {
            listener?.onCompletion()
            stopProgress()
            release()
        }
    }

    override fun onSeekComplete(mp: MediaPlayer) {
        if (!isPlay.get()) {
            setIsPlay(true)
            pause()
        }
    }

}