
package me.shetj.recorder.soundtouch

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
            .enableAudioEffect(enableAudioEffect)
           .apply {
                setAudioChannel(audioChannel)
                setDebug(isDebug)
            }
    }
}
