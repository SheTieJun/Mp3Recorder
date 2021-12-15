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
package me.shetj.mp3recorder.record.utils

import android.widget.SeekBar
import me.shetj.player.AudioPlayer
import me.shetj.player.PlayerListener
import me.shetj.player.setSeekBar

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
class MediaPlayerUtils  {

    private var mediaPlayer: AudioPlayer = AudioPlayer()

    val currentUrl: String
     get() {
        return mediaPlayer.currentUrl?:""
     }

    val isPause: Boolean
        get() = mediaPlayer.isPause

    /**
     * 重新播放url
     * @param url
     */
    fun playNoStart(url: String, listener: PlayerListener?) {
        mediaPlayer.playNoStart(url,listener = listener)
    }

    /**
     * 通过url 比较 进行播放 还是暂停操作
     * @param url 播放的url
     * @param listener 监听变化
     */
    fun playOrStop(url: String, listener: PlayerListener?) {
        mediaPlayer.playOrPause(url,listener)
    }

    /**
     * 修改是否循环
     * @param isLoop
     */
    fun changeLoop(isLoop: Boolean) {
        mediaPlayer.setLoop(isLoop)
    }

    /**
     * 外部设置进度变化
     */
    fun seekTo(changeSize: Int) {
        mediaPlayer.seekTo(changeSize)
    }


    fun updateListener(listener: PlayerListener?) {
        mediaPlayer.updateListener(listener)
    }

    fun getCurrentPosition(): Int {
        return mediaPlayer.currentPosition
    }

    fun setSeekToPlay(seekToPosition: Int) {
        mediaPlayer.setSeekToPlay(seekToPosition)
    }

    /**
     * 开始计时
     */
    fun startProgress() {
        mediaPlayer.startProgress()
    }

    /**
     * 停止计时
     */
    fun stopProgress() {
        mediaPlayer.stopProgress()
    }

    /**
     * 暂停，并且停止计时
     */
    fun pause() {
        mediaPlayer.pause()
    }

    /**
     * 恢复，并且开始计时
     */
    fun resume() {
        mediaPlayer.resume()
    }


    fun stopPlay() {
        mediaPlayer.stopPlay()
    }

    fun setSeekBar(seekBar: SeekBar) {
        mediaPlayer.setSeekBar(seekBar)
    }

}
