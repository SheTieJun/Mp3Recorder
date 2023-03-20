
package me.shetj.recorder.mixRecorder

import android.content.Context
import me.shetj.recorder.core.BaseRecorder
import me.shetj.recorder.core.Mp3Option

/**
 * @param context 用来时设置监听系统声音和耳机变化的
 */
fun Mp3Option.buildMix(context: Context? = null): BaseRecorder {
    return with(this) {
        MixRecorder(audioSource, audioChannel)
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

/**
 * 默认build 是mix
 */
fun Mp3Option.build(context: Context? = null) = buildMix(context)
