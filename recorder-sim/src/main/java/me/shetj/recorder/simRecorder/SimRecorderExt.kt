
package me.shetj.recorder.simRecorder

import android.content.Context
import me.shetj.recorder.core.BaseRecorder
import me.shetj.recorder.core.Mp3Option

/**
 * @param context 用来时设置监听系统声音和耳机变化的
 */
fun Mp3Option.buildSim(context: Context? = null): BaseRecorder {
    return with(this) {
        SimRecorder(audioSource)
            .setMaxTime(mMaxTime)
            .setMp3Quality(mp3Quality)
            .setSamplingRate(samplingRate)
            .setMp3BitRate(mp3BitRate)
            .setPermissionListener(permissionListener)
            .setRecordListener(recordListener)
            .setPCMListener(pcmListener)
            .enableAudioEffect(enableAudioEffect)
            .apply {
                setAudioChannel(audioChannel)
                setDebug(isDebug)
            }.apply {
                context?.let {
                    setContextToPlugConfig(context)
                    setContextToVolumeConfig(context)
                }
            }
    }
}
