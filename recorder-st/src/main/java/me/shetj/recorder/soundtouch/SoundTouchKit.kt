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
package me.shetj.recorder.soundtouch

import android.util.Log
import androidx.annotation.FloatRange
import me.shetj.ndk.soundtouch.SoundTouch
import me.shetj.recorder.core.ISoundTouchCore

/**
 * @author stj
 * @Date 2021/11/4-18:17
 * @Email 375105540@qq.com
 * 变音控制
 */
internal class SoundTouchKit : ISoundTouchCore {

    private var isUseST: Boolean = true // 是否进行变音

    private var tempo: Float = 1f // 指定节拍，设置新的节拍tempo，源tempo=1.0，小于1则变慢；大于1变快
    private var pitch: Float = 0f // 指定音调值大于0 是变女生，小于0是变男声
    private var rate: Float = 1f // 指定播放速率，源rate=1.0，小于1变慢；大于1

    private val soundTouch: SoundTouch by lazy { SoundTouch() }

    private var handle: Long = 0

    internal fun init(channel: Int, samplingRate: Int) {
        if (handle == 0L) {
            handle = soundTouch.newInstance()
        }
        soundTouch.init(handle, channel, samplingRate, tempo, pitch, rate)
    }

    override fun changeUse(isUseST: Boolean) {
        this.isUseST = isUseST
    }

    override fun isUse(): Boolean {
        return isUseST
    }

    /**
     * rate (-50 .. +100 %)
     */
    override fun setRateChange(@FloatRange(from = -50.0, to = 100.0) rateChange: Float) {
        this.rate = 1f + 0.01f * rateChange
        soundTouch.setRateChange(handle, rateChange)
    }

    override fun setTempoChange(@FloatRange(from = -50.0, to = 100.0) tempoChange: Float) {
        this.tempo = 1f + 0.01f * tempoChange
        soundTouch.setTempoChange(handle, tempoChange)
    }

    //  指定节拍，设置新的节拍tempo，源tempo=1.0，小于1则变慢；大于1变快
    override fun setTempo(tempo: Float) {
        this.tempo = tempo
        soundTouch.setTempo(handle, tempo)
    }

    // 在源pitch的基础上，使用半音(Semitones)设置新的pitch [-12.0,12.0]
    override fun setPitchSemiTones(@FloatRange(from = -12.0, to = 12.0) pitch: Float) {
        this.pitch = pitch
        soundTouch.setPitchSemiTones(handle, pitch)
    }

    // 指定播放速率，源rate=1.0，小于1变慢；大于1
    override fun setRate(rate: Float) {
        this.rate = rate
        soundTouch.setRate(handle, rate)
    }

    // 只处理wav 文件
    override fun processFile(inputFile: String, outputFile: String): Boolean {
        return if (soundTouch.processFile(handle, inputFile, outputFile) == 0) {
            true
        } else {
            Log.e("soundTouch", soundTouch.getErrorString())
            false
        }
    }

    fun receiveSamples(outputBuf: ShortArray): Int {
        print("putSamples :$handle")
        return soundTouch.receiveSamples(handle, outputBuf)
    }

    fun putSamples(samples: ShortArray, len: Int) {
        print("putSamples :$handle")
        soundTouch.putSamples(handle, samples, len)
    }

    // 重置到最开始的值
    fun clean() {
        tempo = 1f
        pitch = 0f
        rate = 1f
    }

    fun destroy() {
        if (handle != 0L) {
            soundTouch.deleteInstance(handle)
            handle = 0
        }
    }

    // 处理玩最后的数据
    fun flush(mp3buf: ShortArray): Int {
        return soundTouch.flush(handle, mp3buf)
    }
}
