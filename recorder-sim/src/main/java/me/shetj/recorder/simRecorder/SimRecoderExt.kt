package me.shetj.recorder.simRecorder

import android.content.Context
import me.shetj.recorder.core.BaseRecorder
import me.shetj.recorder.core.Mp3RecorderOption
import me.shetj.recorder.core.PermissionListener
import me.shetj.recorder.core.RecordListener





fun Mp3RecorderOption.build(context: Context?=null): BaseRecorder {
    return with(this) {
        SimRecorder(audioSource.type)
            .setMaxTime(mMaxTime)
            .setMp3Quality(mp3Quality)
            .setSamplingRate(samplingRate)
            .setMp3BitRate(mp3BitRate)
            .setPermissionListener(permissionListener)
            .setRecordListener(recordListener)
            .setWax(wax).apply {
                setDebug(isDebug)

            }.apply {
                context?.let {
                    setContextToPlugConfig(context)
                    setContextToVolumeConfig(context)
                }
            }
    }
}

@Deprecated("此方法，后面版本有可能会去掉",replaceWith = ReplaceWith("RecorderBuilder"))
@JvmOverloads
fun simRecorder(
    context: Context,
    audioSource: BaseRecorder.AudioSource = BaseRecorder.AudioSource.MIC,
    isDebug: Boolean = false,
    mMaxTime: Int = 1800 * 1000,
    samplingRate: Int = 44100,
    mp3BitRate: Int = 64,//96(高),32（低）
    mp3Quality: Int = 1,
    permissionListener: PermissionListener? = null,
    recordListener: RecordListener? = null,
    wax: Float = 1f

): SimRecorder {
    return SimRecorder(audioSource.type)
        .setMaxTime(mMaxTime)
        .setMp3Quality(mp3Quality)
        .setSamplingRate(samplingRate)
        .setMp3BitRate(mp3BitRate)
        .setPermissionListener(permissionListener)
        .setRecordListener(recordListener)
        .setWax(wax).apply {
            setDebug(isDebug)

        }.apply {
            setContextToPlugConfig(context)
            setContextToVolumeConfig(context)
        }
}

@Deprecated("此方法，后面版本有可能会去掉",replaceWith = ReplaceWith("RecorderBuilder"))
@JvmOverloads
fun simRecorder(
    audioSource: BaseRecorder.AudioSource = BaseRecorder.AudioSource.MIC,
    isDebug: Boolean = false,
    mMaxTime: Int = 1800 * 1000,
    samplingRate: Int = 44100,
    mp3BitRate: Int = 64,//96(高),32（低）
    mp3Quality: Int = 1,
    permissionListener: PermissionListener? = null,
    recordListener: RecordListener? = null,
    wax: Float = 1f

): SimRecorder {
    return SimRecorder(audioSource.type)
        .setMaxTime(mMaxTime)
        .setMp3Quality(mp3Quality)
        .setSamplingRate(samplingRate)
        .setMp3BitRate(mp3BitRate)
        .setPermissionListener(permissionListener)
        .setRecordListener(recordListener)
        .setWax(wax).apply {
            setDebug(isDebug)
        }
}