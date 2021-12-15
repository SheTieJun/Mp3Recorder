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
package me.shetj.recorder.mixRecorder

import android.util.Log
import me.shetj.recorder.core.BaseRecorder
import me.shetj.recorder.core.BytesTransUtil

internal class ReadMixTask(
    rawData: ByteArray, // 录制的人声音
    private val wax: Float, // 人声音增强
    private val bgData: ByteArray?, // 录制的背景音乐 可能没有
    private val bgWax: Float // 背景声音降低
) {
    private val rawData: ByteArray = rawData.clone()

    fun getData(): ShortArray {
        val mixBuffer = mixBuffer(rawData, bgData) ?: return BytesTransUtil.bytes2Shorts(
            BytesTransUtil.changeDataWithVolume(rawData, wax)
        )
        return BytesTransUtil.bytes2Shorts(BytesTransUtil.changeDataWithVolume(mixBuffer, wax))
    }

    /**
     * 混合 音频,
     */
    private fun mixBuffer(buffer: ByteArray, bgData: ByteArray?): ByteArray? {
        try {
            if (bgData != null) {
                // 如果有背景音乐
                val bytes = BytesTransUtil.changeDataWithVolume(
                    bgData,
                    bgWax
                )
                return BytesTransUtil.averageMix(arrayOf(buffer, bytes))
            }
            return buffer
        } catch (e: Exception) {
            Log.e(BaseRecorder.TAG, "mixBuffer error : ${e.message}")
            return buffer
        }
    }
}
