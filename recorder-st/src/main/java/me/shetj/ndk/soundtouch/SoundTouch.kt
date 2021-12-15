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
package me.shetj.ndk.soundtouch

/**
 * @author stj
 * @Date 2021/11/4-18:17
 * @Email 375105540@qq.com
 */

internal class SoundTouch {

    companion object {
        init {
            System.loadLibrary("soundTouch")
        }
    }

    external fun newInstance(): Long
    external fun deleteInstance(handle: Long)
    external fun getVersionString(): String
    external fun getErrorString(): String

    external fun init(
        handle: Long,
        channels: Int, // 设置声道(1单,2双)
        sampleRate: Int, // 设置采样率
        tempo: Float, // 指定节拍，设置新的节拍tempo，源tempo=1.0，小于1则变慢；大于1变快
        pitch: Float, // 指定音调值重点， 大于0 是变女生，小于0是变男声
        speed: Float // 指定播放速率 源rate=1.0，小于1变慢；大于1
    )

    // 在原速1.0基础上，按百分比做增量，取值(-50 .. +100 %)
    external fun setRateChange(handle: Long, rateChange: Float)
    external fun setTempoChange(handle: Long, tempoChange: Float)
    external fun setTempo(handle: Long, tempo: Float)
    external fun setPitchSemiTones(handle: Long, pitch: Float)
    external fun setRate(handle: Long, speed: Float)

    // 直接WAV处理文件
    external fun processFile(handle: Long, inputFile: String, outputFile: String): Int

    // putSamples 的次数可能小于receiveSamples
    external fun putSamples(handle: Long, samples: ShortArray, len: Int)

    // 读取数据
    external fun receiveSamples(handle: Long, outputBuf: ShortArray): Int

    // 获取最后一段数据
    external fun flush(handle: Long, mp3buf: ShortArray): Int
}
