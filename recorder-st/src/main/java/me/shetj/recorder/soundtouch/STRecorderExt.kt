package me.shetj.recorder.soundtouch

import android.content.Context
import android.media.AudioFormat
import me.shetj.recorder.core.BaseRecorder
import me.shetj.recorder.core.Mp3RecorderOption


/**
 * SoundTouchRecorder
 */
fun Mp3RecorderOption.buildST(context: Context? = null): BaseRecorder {

    return with(this) {
        //初始化变音参数，默认没有变化
        SoundTouchKit.getInstance().init(audioChannel,samplingRate,1,0f,1f)

        STRecorder(audioSource)
            .setMaxTime(mMaxTime)
            .setMp3Quality(mp3Quality)
            .setSamplingRate(samplingRate)
            .setMp3BitRate(mp3BitRate)
            .setPermissionListener(permissionListener)
            .setRecordListener(recordListener)
            .setWax(wax).apply {
                setAudioChannel(if (AudioFormat.CHANNEL_IN_STEREO == audioChannel) 2 else 1)
                setDebug(isDebug)
            }.apply {
                context?.let {
                    setContextToPlugConfig(context)
                    setContextToVolumeConfig(context)
                }
            }

    }
}