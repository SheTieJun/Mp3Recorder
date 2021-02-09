package me.shetj.mp3recorder

import android.content.Context
import me.shetj.recorder.core.PermissionListener
import me.shetj.recorder.core.RecordListener
import me.shetj.recorder.core.BaseRecorder
import me.shetj.recorder.mixRecorder.mixRecorder
import me.shetj.recorder.simRecorder.simRecorder


@JvmOverloads
fun mp3Recorder(
    context: Context,
    simpleName: BaseRecorder.RecorderType = BaseRecorder.RecorderType.MIX,
    audioSource: BaseRecorder.AudioSource = BaseRecorder.AudioSource.MIC,
    isDebug: Boolean = BuildConfig.DEBUG,
    mMaxTime: Int = 1800 * 1000,
    samplingRate: Int = 44100,
    mp3BitRate: Int = 128,//96(高),32（低）
    mp3Quality: Int = 1,
    channel: BaseRecorder.AudioChannel = BaseRecorder.AudioChannel.STEREO,
    permissionListener: PermissionListener? = null,
    recordListener: RecordListener? = null,
    wax: Float = 1f
): BaseRecorder {
   return mp3RecorderNoContext(
        simpleName,
        audioSource,
        isDebug,
        mMaxTime,
        samplingRate,
        mp3BitRate,
        mp3Quality,
        channel,
        permissionListener,
        recordListener,
        wax
    ).setContextToVolumeConfig(context)
        .setContextToVolumeConfig(context)
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
    mp3BitRate: Int = 128,//96(高),32（低）
    mp3Quality: Int = 1,
    channel: BaseRecorder.AudioChannel = BaseRecorder.AudioChannel.STEREO,
    permissionListener: PermissionListener? = null,
    recordListener: RecordListener? = null,
    wax: Float = 1f
): BaseRecorder {
    return when (simpleName) {
        BaseRecorder.RecorderType.MIX ->
            mixRecorder(
                audioSource = audioSource,
                isDebug = isDebug,
                channel = channel,
                mMaxTime = mMaxTime,
                mp3Quality = mp3Quality,
                samplingRate = samplingRate,
                mp3BitRate = mp3BitRate,
                permissionListener = permissionListener,
                recordListener = recordListener,
                wax = wax
            )
        BaseRecorder.RecorderType.SIM ->
             simRecorder(
                audioSource = audioSource,
                isDebug = isDebug,
                mMaxTime = mMaxTime,
                mp3Quality = mp3Quality,
                samplingRate = samplingRate,
                mp3BitRate = mp3BitRate,
                permissionListener = permissionListener,
                recordListener = recordListener,
                wax = wax
            )
    }
}
