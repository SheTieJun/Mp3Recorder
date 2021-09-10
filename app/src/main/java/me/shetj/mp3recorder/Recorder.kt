package me.shetj.mp3recorder

import android.content.Context
import me.shetj.recorder.core.BaseRecorder
import me.shetj.recorder.core.PermissionListener
import me.shetj.recorder.core.RecordListener
import me.shetj.recorder.core.recorder
import me.shetj.recorder.mixRecorder.buildMix
import me.shetj.recorder.simRecorder.buildSim


@JvmOverloads
fun mp3Recorder(
    context: Context,
    simpleName: BaseRecorder.RecorderType = BaseRecorder.RecorderType.MIX,
    audioSource: BaseRecorder.AudioSource = BaseRecorder.AudioSource.MIC,
    isDebug: Boolean = BuildConfig.DEBUG,
    mMaxTime: Int = 1800 * 1000,
    samplingRate: Int = 44100,
    mp3BitRate: Int = 64,//96(高),32（低）
    mp3Quality: Int = 1,
    channel: BaseRecorder.AudioChannel = BaseRecorder.AudioChannel.STEREO,
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
        }
    }
}

/**
 * mix 表示混合
 * sim 不支持单双声道录制
 */
@JvmOverloads
fun mp3RecorderNoContext(
    simpleName: BaseRecorder.RecorderType = BaseRecorder.RecorderType.MIX,
    audioSource: BaseRecorder.AudioSource = BaseRecorder.AudioSource.MIC,
    isDebug: Boolean = BuildConfig.DEBUG,
    mMaxTime: Int = 1800 * 1000,
    samplingRate: Int = 44100,
    mp3BitRate: Int = 64,//96(高),32（低）
    mp3Quality: Int = 1,
    channel: BaseRecorder.AudioChannel = BaseRecorder.AudioChannel.STEREO,
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
            BaseRecorder.RecorderType.MIX -> it.buildMix()
            BaseRecorder.RecorderType.SIM -> it.buildSim()
        }
    }
}
