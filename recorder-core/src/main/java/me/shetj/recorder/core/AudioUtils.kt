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
package me.shetj.recorder.core

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri

object AudioUtils {

    /**
     * 对声音进行变化
     * @param audioSamples
     * @param volume
     * @return
     */
    fun adjustVolume(audioSamples: ByteArray, volume: Float): ByteArray {
        val array = ByteArray(audioSamples.size)
        var i = 0
        while (i < array.size) {
            // convert byte pair to int
            var buf1 = audioSamples[i + 1].toInt()
            var buf2 = audioSamples[i].toInt()
            buf1 = (buf1 and 0xff).shl(8)
            buf2 = (buf2 and 0xff)
            var res = (buf1 or buf2)
            res = (res * volume).toInt()
            // convert back
            array[i] = res.toByte()
            array[i + 1] = (res shr 8).toByte()
            i += 2
        }
        return array
    }

    fun getAudioFormat(url: String): MediaFormat {
        val mediaExtractor = MediaExtractor() // 此类可分离视频文件的音轨和视频轨道
        mediaExtractor.setDataSource(url) // 媒体文件的位置
        return mediaExtractor.getTrackFormat(0).also {
            mediaExtractor.release()
        }
    }

    fun getAudioChannel(url: String): Int {
        return getAudioFormat(url).getInteger(MediaFormat.KEY_CHANNEL_COUNT)
    }

    fun getAudioFormat(context: Context, url: Uri): MediaFormat {
        val mediaExtractor = MediaExtractor() // 此类可分离视频文件的音轨和视频轨道
        mediaExtractor.setDataSource(context, url, null) // 媒体文件的位置
        return mediaExtractor.getTrackFormat(0).also {
            mediaExtractor.release()
        }
    }

    fun getAudioChannel(context: Context, url: Uri): Int {
        return getAudioFormat(context, url).getInteger(MediaFormat.KEY_CHANNEL_COUNT)
    }
}
