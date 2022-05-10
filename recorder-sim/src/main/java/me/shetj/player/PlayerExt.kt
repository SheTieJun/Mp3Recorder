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
