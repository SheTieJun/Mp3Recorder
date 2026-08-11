
package me.shetj.player

import android.widget.SeekBar

/**
 * 设置URL,
 * tip：不会主动开始播放 ,需要主动开始请使用[playOrPause]
 */
fun AudioPlayer.playNoStart(
    url: String,
    onStart: (duration: Int) -> Unit = { _: Int -> },
    onPause: () -> Unit = {},
    onResume: () -> Unit = {},
    onStop: () -> Unit = {},
    onCompletion: () -> Unit = {},
    onError: (throwable: Exception?) -> Unit = {},
    onProgress: (current: Int, duration: Int) -> Unit = { _: Int, _: Int -> }
): AudioPlayer {

    playNoStart(
        url,
        object : PlayerListener {
            override fun onStart(duration: Int) {
                onStart(duration)
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

            override fun onError(throwable: Exception?) {
                onError(throwable)
            }

            override fun onProgress(current: Int, duration: Int) {
                onProgress(current, duration)
            }
        }
    )
    return this
}

/**
 * 播放和暂停相互切换
 * 如果切换了URL,会先执行上一个[PlayerListener.onCompletion]
 */
fun AudioPlayer.playOrPause(
    url: String,
    onStart: (duration: Int) -> Unit = { _: Int -> },
    onPause: () -> Unit = {},
    onResume: () -> Unit = {},
    onStop: () -> Unit = {},
    onCompletion: () -> Unit = {},
    onError: (throwable: Exception?) -> Unit = {},
    onProgress: (current: Int, duration: Int) -> Unit = { _: Int, _: Int -> }
): AudioPlayer {

    playOrPause(
        url,
        object : PlayerListener {
            override fun onStart(duration: Int) {
                onStart(duration)
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

            override fun onError(throwable: Exception?) {
                onError(throwable)
            }

            override fun onProgress(current: Int, duration: Int) {
                onProgress(current, duration)
            }
        }
    )
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
                setSeekToPlay(seekBar.progress)
                if (!isPause) {
                    startProgress()
                }
            }
        })
    }
}

/**
 * 有时需要重新替换部分
 * 如果需要替换全部 [AudioPlayer.updateListener(listener: PlayerListener)]
 */
fun AudioPlayer.updateListener(
    onStart: (duration: Int) -> Unit = { _: Int -> },
    onPause: () -> Unit = {},
    onResume: () -> Unit = {},
    onStop: () -> Unit = {},
    onCompletion: () -> Unit = {},
    onError: (throwable: Exception?) -> Unit = {},
    onProgress: (current: Int, duration: Int) -> Unit = { _: Int, _: Int -> }
): AudioPlayer {
    updateListener(object : PlayerListener {
        override fun onStart(duration: Int) {
            onStart(duration)
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

        override fun onError(throwable: Exception?) {
            onError(throwable)
        }

        override fun onProgress(current: Int, duration: Int) {
            onProgress(current, duration)
        }
    })
    return this
}
