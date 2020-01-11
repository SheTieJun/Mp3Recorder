package me.shetj.mp3recorder.record.utils

import android.media.AudioManager
import android.media.MediaPlayer
import android.text.TextUtils
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * **@author：** shetj<br></br>
 * **@createTime：** 2018/10/23 0023<br></br>
 * **@email：** 375105540@qq.com<br></br>
 * **@describe** [MediaPlayerUtils] 音乐播放<br></br>
 *
 * ** 播放 [MediaPlayerUtils.playOrStop]}**<br></br>
 * ** 暂停  [MediaPlayerUtils.pause] <br></br>
 * ** 恢复  [MediaPlayerUtils.resume] ()} <br></br>
 * ** 停止  [MediaPlayerUtils.stopPlay] ()} <br></br>
 * <br></br>
 ****** */
class MediaPlayerUtils : LifecycleListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnSeekCompleteListener {
    private var mCompositeDisposable: CompositeDisposable? = null
    private var mediaPlayer: MediaPlayer? = null
    private var listener: SPlayerListener? = null
    /**
     * 获取当前播放的url
     * @return currentUrl
     */
    var currentUrl = ""
    private var timeDisposable: Disposable? = null
    private val isPlay = AtomicBoolean(true)

    /**
     * 是否暂停
     * @return
     */
    val isPause: Boolean
        get() = !(mediaPlayer != null && mediaPlayer!!.isPlaying)

    init {
        initMedia()
    }

    /**
     * 重新播放url
     * @param url
     * @param listener
     */
    private fun play(url: String, listener: SPlayerListener?) {
        if (null != listener) {
            this.listener = listener
        } else {
            this.listener = EasyPlayerListener()
        }
        currentUrl = url
        if (null == mediaPlayer) {
            initMedia()
        }
        try {
            mediaPlayer!!.reset()
            mediaPlayer!!.setDataSource(url)
            mediaPlayer!!.prepareAsync()
            //监听
            mediaPlayer!!.setOnPreparedListener(this)
            mediaPlayer!!.setOnErrorListener(this)
            mediaPlayer!!.setOnCompletionListener(this)
            mediaPlayer!!.setOnSeekCompleteListener(this)
            //是否循环
            if (listener != null) {
                mediaPlayer!!.isLooping = listener.isLoop
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    /**
     * 重新播放url
     * @param url
     */
    fun playNoStart(url: String, listener: SPlayerListener?) {
        if (null != listener) {
            this.listener = listener
        } else {
            this.listener = EasyPlayerListener()
        }
        setIsPlay(false)
        currentUrl = url
        if (null == mediaPlayer) {
            initMedia()
        }
        try {
            mediaPlayer!!.reset()
            mediaPlayer!!.setDataSource(url)
            mediaPlayer!!.prepareAsync()
            //监听
            mediaPlayer!!.setOnPreparedListener(this)
            mediaPlayer!!.setOnErrorListener(this)
            mediaPlayer!!.setOnCompletionListener(this)
            mediaPlayer!!.setOnSeekCompleteListener(this)
            //是否循环
            if (listener != null) {
                mediaPlayer!!.isLooping = listener.isLoop
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun setIsPlay(isPlay: Boolean) {
        this.isPlay.set(isPlay)
    }

    /**
     * 通过url 比较 进行播放 还是暂停操作
     * @param url 播放的url
     * @param listener 监听变化
     */
    fun playOrStop(url: String, listener: SPlayerListener?) {

        //判断是否是当前播放的url
        if (url == currentUrl && mediaPlayer != null) {
            if (listener != null) {
                this.listener = listener
            } else {
                this.listener = EasyPlayerListener()
            }
            if (mediaPlayer!!.isPlaying) {
                pause()
                this.listener!!.onPause()
            } else {
                resume()
                this.listener!!.onResume()
            }
        } else {
            //直接播放
            play(url, listener)
        }
    }

    /**
     * 修改是否循环
     * @param isLoop
     */
    fun changeLoop(isLoop: Boolean) {
        if (mediaPlayer != null) {
            mediaPlayer!!.isLooping = isLoop
        }
    }

    /**
     * 外部设置进度变化
     */
    fun seekTo(changeSize: Int) {
        if (mediaPlayer != null && !TextUtils.isEmpty(currentUrl)) {
            setIsPlay(!isPause)
            mediaPlayer!!.start()
            mediaPlayer!!.seekTo(changeSize)
        }
    }

    /**
     * 清空播放信息
     */
    private fun release() {
        unDispose()
        currentUrl = ""
        //释放MediaPlay
        if (null != mediaPlayer) {
            mediaPlayer!!.release()
            mediaPlayer = null
        }
    }

    fun updateListener(listener: SPlayerListener?) {
        if (listener != null) {
            this.listener = listener
        }
    }

    fun getCurrentPosition(): Int {
        return if (mediaPlayer != null) {
            mediaPlayer!!.currentPosition
        } else 0
    }


    /**
     * 开始计时
     */
      fun startProgress() {
        if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
            timeDisposable = Flowable.interval(0, 150, TimeUnit.MILLISECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ aLong ->
                        if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
                            listener!!.onProgress(mediaPlayer!!.currentPosition, mediaPlayer!!.duration)
                        }
                    }, { throwable -> stopProgress() })
            addDispose(timeDisposable!!)
        }
    }

    /**
     * 停止计时
     */
      fun stopProgress() {
        if (timeDisposable != null && !timeDisposable!!.isDisposed) {
            timeDisposable!!.dispose()
            timeDisposable = null
        }
    }

    /**
     * 暂停，并且停止计时
     */
    fun pause() {
        if (mediaPlayer != null && mediaPlayer!!.isPlaying
                && !TextUtils.isEmpty(currentUrl)) {
            stopProgress()
            mediaPlayer!!.pause()
            listener!!.onPause()
        }
    }

    /**
     * 恢复，并且开始计时
     */
    fun resume() {
        if (isPause && !TextUtils.isEmpty(currentUrl)) {
            mediaPlayer!!.start()
            listener!!.onResume()
            startProgress()
        }
    }


    fun stopPlay() {
        try {
            if (null != mediaPlayer) {
                startProgress()
                mediaPlayer!!.stop()
                listener!!.onStop()
                release()
            }
        } catch (e: Exception) {
            release()
        }

    }

    /**
     * 设置媒体
     */
    private fun initMedia() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()

            if (null == listener) {
                listener = EasyPlayerListener()
            }
            mediaPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
        }
    }

    override fun onStart() {}

    override fun onStop() {
        stopPlay()
    }

    override fun onResume() {
        resume()
    }

    override fun onPause() {
        pause()
    }

    override fun onDestroy() {
        release()
    }

    override fun onPrepared(mp: MediaPlayer) {
        if (!isPlay.get()) {
            setIsPlay(true)
            return
        }
        mp.start()
        startProgress()
        listener!!.onStart(currentUrl)
    }


    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        listener!!.onError(Throwable(String.format("what = %d extra = %d", what, extra)))
        release()
        return true
    }

    override fun onCompletion(mp: MediaPlayer) {
        if (!listener!!.isNext(this)) {
            listener!!.onCompletion()
            stopProgress()
            release()
        }
    }

    /**
     * 将 [Disposable] 添加到 [CompositeDisposable] 中统一管理
     * 可在 {onDestroy() 中使用 [.unDispose] 停止正在执行的 RxJava 任务,避免内存泄漏
     *
     * @param disposable
     */
    private fun addDispose(disposable: Disposable) {
        if (mCompositeDisposable == null) {
            mCompositeDisposable = CompositeDisposable()
        }
        mCompositeDisposable!!.add(disposable)
    }

    /**
     * 停止集合中正在执行的 RxJava 任务
     */
    private fun unDispose() {
        if (mCompositeDisposable != null) {
            mCompositeDisposable!!.clear()
        }
    }

    override fun onSeekComplete(mp: MediaPlayer) {
        if (!isPlay.get()) {
            setIsPlay(true)
            onPause()
        }
    }
}
