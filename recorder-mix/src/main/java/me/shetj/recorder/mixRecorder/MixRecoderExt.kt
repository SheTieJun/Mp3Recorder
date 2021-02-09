package me.shetj.recorder.mixRecorder

import android.content.Context
import me.shetj.recorder.core.BaseRecorder
import me.shetj.recorder.core.PermissionListener
import me.shetj.recorder.core.RecordListener




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
