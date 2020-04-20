package me.shetj.kt

import android.widget.SeekBar
import me.shetj.player.AudioPlayer
import me.shetj.player.PlayerListener


/**
 * 设置URL,
 * 注意：不会开始播放
 */
fun AudioPlayer.playNoStart(
    url: String,
    onStart: (url: String, duration: Int) -> Unit = { _: String, _: Int -> },
    onPause: () -> Unit = {},
    onResume: () -> Unit = {},
    onStop: () -> Unit = {},
    onCompletion: () -> Unit = {},
    onError: (throwable: Exception) -> Unit = {},
    onProgress: (current: Int, duration: Int) -> Unit = { _: Int, _: Int -> }
): AudioPlayer {

    playNoStart(url, object : PlayerListener {
        override fun onStart(url: String, duration: Int) {
            onStart(url, duration)
        }

        override fun onPause() {
            onPause()
        }

        override fun onResume() {
            onResume()
        }

        override fun onStop() {
            onStop()
        }

        override fun onCompletion() {
            onCompletion()
        }

        override fun onError(throwable: Exception) {
            onError(throwable)
        }

        override fun onProgress(current: Int, duration: Int) {
            onProgress(current, duration)
        }
    })
    return this
}

/**
 * 播放和暂停相互切换
 * 如果切换了URL,会先执行上一个[PlayerListener.onCompletion]
 */
fun AudioPlayer.playOrPause(
    url: String,
    onStart: (url: String, duration: Int) -> Unit = { _: String, _: Int -> },
    onPause: () -> Unit = {},
    onResume: () -> Unit = {},
    onStop: () -> Unit = {},
    onCompletion: () -> Unit = {},
    onError: (throwable: Exception) -> Unit = {},
    onProgress: (current: Int, duration: Int) -> Unit = { _: Int, _: Int -> }
): AudioPlayer {

    playOrPause(url, object : PlayerListener {
        override fun onStart(url: String, duration: Int) {
            onStart(url, duration)
        }

        override fun onPause() {
            onPause()
        }

        override fun onResume() {
            onResume()
        }

        override fun onStop() {
            onStop()
        }

        override fun onCompletion() {
            onCompletion()
        }

        override fun onError(throwable: Exception) {
            onError(throwable)
        }

        override fun onProgress(current: Int, duration: Int) {
            onProgress(current, duration)
        }
    })
    return this
}

fun AudioPlayer.setSeekBar(seekBar: SeekBar?) {
    seekBar?.apply {
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: SeekBar) {
                stopProgress()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                seekTo(seekBar.progress)
                if (!isPause) {
                    startProgress()
                }
            }
        })
    }
}

/**
 * 有时需要重新替换,比如列表滑动
 */
fun AudioPlayer.updateListener(
    onStart: (url: String, duration: Int) -> Unit = { _: String, _: Int -> },
    onPause: () -> Unit = {},
    onResume: () -> Unit = {},
    onStop: () -> Unit = {},
    onCompletion: () -> Unit = {},
    onError: (throwable: Exception) -> Unit = {},
    onProgress: (current: Int, duration: Int) -> Unit = { _: Int, _: Int -> }
): AudioPlayer {
    updateListener(object : PlayerListener {
        override fun onStart(url: String, duration: Int) {
            onStart(url, duration)
        }

        override fun onPause() {
            onPause()
        }

        override fun onResume() {
            onResume()
        }

        override fun onStop() {
            onStop()
        }

        override fun onCompletion() {
            onCompletion()
        }

        override fun onError(throwable: Exception) {
            onError(throwable)
        }

        override fun onProgress(current: Int, duration: Int) {
            onProgress(current, duration)
        }
    })
    return this
}




