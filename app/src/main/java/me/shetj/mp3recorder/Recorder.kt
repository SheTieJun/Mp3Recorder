
package me.shetj.mp3recorder

import android.content.Context
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
    mMaxTime: Long = 1800 * 1000,
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
            BaseRecorder.RecorderType.ST -> it.buildST()
        }
    }
}
