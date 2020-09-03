package me.shetj.recorder.simRecorder

import me.shetj.recorder.core.BaseRecorder
import me.shetj.recorder.core.BuildConfig
import me.shetj.recorder.core.PermissionListener
import me.shetj.recorder.core.RecordListener


@JvmOverloads
fun simRecorderBuilder(
    audioSource: BaseRecorder.AudioSource = BaseRecorder.AudioSource.MIC,
    isDebug: Boolean = BuildConfig.DEBUG,
    mMaxTime: Int = 1800 * 1000,
    samplingRate: Int = 44100,
    mp3BitRate: Int = 128,//96(高),32（低）
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