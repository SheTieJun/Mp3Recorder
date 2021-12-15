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
package me.shetj.mp3recorder

import android.content.Context
import android.media.AudioFormat
import android.media.MediaRecorder
import me.shetj.recorder.core.*
import me.shetj.recorder.mixRecorder.buildMix
import me.shetj.recorder.simRecorder.buildSim
import me.shetj.recorder.soundtouch.buildST

@JvmOverloads
fun mp3Recorder(
    context: Context,
    simpleName: BaseRecorder.RecorderType = BaseRecorder.RecorderType.MIX,
    @Source audioSource: Int = MediaRecorder.AudioSource.VOICE_COMMUNICATION,
    isDebug: Boolean = BuildConfig.DEBUG,
    mMaxTime: Int = 1800 * 1000,
    samplingRate: Int = 44100,
    mp3BitRate: Int = 64,//96(高),32（低）
    mp3Quality: Int = 1,
    @Channel channel: Int = 2,
    permissionListener: PermissionListener? = null,
    recordListener: RecordListener? = null,
    wax: Float = 1f
): BaseRecorder {
    return recorder {
        this.audioSource = audioSource
        this.isDebug = isDebug
        this.audioChannel = channel
        this.mMaxTime = mMaxTime
        this.mp3Quality = mp3Quality
        this.samplingRate = samplingRate
        this.mp3BitRate = mp3BitRate
        this.permissionListener = permissionListener
        this.recordListener = recordListener
        this.wax = wax
    }.let {
        when (simpleName) {
            BaseRecorder.RecorderType.MIX -> it.buildMix(context)
            BaseRecorder.RecorderType.SIM -> it.buildSim(context)
            BaseRecorder.RecorderType.ST -> it.buildST(context)
        }
    }
}
