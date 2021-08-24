package me.shetj.recorder.mixRecorder

import android.content.Context
import me.shetj.recorder.core.BaseRecorder
import me.shetj.recorder.core.Mp3RecorderOption
import me.shetj.recorder.core.PermissionListener
import me.shetj.recorder.core.RecordListener


/**
 * @param context 用来时设置监听系统声音和耳机变化的
 */
fun Mp3RecorderOption.buildMix(context: Context?=null): BaseRecorder {
    return with(this) {
        MixRecorder(audioSource.type, audioChannel.type)
            .setMaxTime(mMaxTime)
            .setMp3Quality(mp3Quality)
            .setSamplingRate(samplingRate)
            .setMp3BitRate(mp3BitRate)
            .setPermissionListener(permissionListener)
            .setRecordListener(recordListener)
            .setDebug(isDebug)
            .setWax(wax).apply {
                context?.let {
                    setContextToVolumeConfig(context)
                    setContextToPlugConfig(context)
                }
            }
    }
}


@Deprecated("此方法，后面版本将会去掉",replaceWith = ReplaceWith("RecorderBuilder"))
@JvmOverloads
fun mixRecorder(
    context: Context,
    audioSource: BaseRecorder.AudioSource = BaseRecorder.AudioSource.MIC,
    channel: BaseRecorder.AudioChannel = BaseRecorder.AudioChannel.STEREO,
    isDebug: Boolean = false,
    mMaxTime: Int = 1800 * 1000,
    samplingRate: Int = 44100,
    mp3BitRate: Int = 64 ,//128 /96(高),32（低）
    mp3Quality: Int = 1,
    permissionListener: PermissionListener? = null,
    recordListener: RecordListener? = null,
    wax: Float = 1f

): MixRecorder {
    return MixRecorder(audioSource.type, channel.type)
        .setMaxTime(mMaxTime)
        .setMp3Quality(mp3Quality)
        .setSamplingRate(samplingRate)
        .setMp3BitRate(mp3BitRate)
        .setPermissionListener(permissionListener)
        .setRecordListener(recordListener)
        .setWax(wax).apply {
            setDebug(isDebug)
        }.apply {
            setContextToVolumeConfig(context)
        }
        .setContextToPlugConfig(context)
}

/**
 * 混合音频的录制
 */
@Deprecated("此方法，后面版本将会去掉",replaceWith = ReplaceWith("RecorderBuilder"))
@JvmOverloads
fun mixRecorder(
    audioSource: BaseRecorder.AudioSource = BaseRecorder.AudioSource.MIC,
    channel: BaseRecorder.AudioChannel = BaseRecorder.AudioChannel.STEREO,
    isDebug: Boolean = false,
    mMaxTime: Int = 1800 * 1000,
    samplingRate: Int = 44100,
    mp3BitRate: Int = 64 ,//128 /96(高),32（低）
    mp3Quality: Int = 1,
    permissionListener: PermissionListener? = null,
    recordListener: RecordListener? = null,
    wax: Float = 1f

): MixRecorder {
    return MixRecorder(audioSource.type, channel.type)
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
