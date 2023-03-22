
package me.shetj.recorder.soundtouch

import android.media.AudioFormat
import me.shetj.recorder.core.BaseRecorder
import me.shetj.recorder.core.Mp3Option

/**
 * SoundTouchRecorder
 */
fun Mp3Option.buildST(): BaseRecorder {

    return with(this) {
        // 初始化变音参数，默认没有变化
        STRecorder(audioSource)
            .setMaxTime(mMaxTime)
            .setMp3Quality(mp3Quality)
            .setSamplingRate(samplingRate)
            .setMp3BitRate(mp3BitRate)
            .setPermissionListener(permissionListener)
            .setRecordListener(recordListener)
            .setPCMListener(pcmListener)
           .apply {
                setAudioChannel(if (AudioFormat.CHANNEL_IN_STEREO == audioChannel) 2 else 1)
                setDebug(isDebug)
            }.apply {
//                STRecorder 没有背景音乐，所以不需要处理耳机相关
//                context?.let {
//                    setContextToPlugConfig(context)
//                    setContextToVolumeConfig(context)
//                }
            }
    }
}
