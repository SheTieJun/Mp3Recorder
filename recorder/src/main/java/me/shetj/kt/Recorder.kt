package me.shetj.kt

import me.shetj.mixRecorder.MixRecorder
import me.shetj.player.PermissionListener
import me.shetj.player.RecordListener
import me.shetj.recorder.BaseRecorder
import me.shetj.recorder.BuildConfig
import me.shetj.recorder.MP3Recorder

@JvmOverloads
fun recorderBuilder( audioSource: BaseRecorder.AudioSource = BaseRecorder.AudioSource.MIC,
                    isDebug: Boolean = BuildConfig.DEBUG,
                    mMaxTime: Int = 1800 * 1000,
                    mp3Quality: Int = 5,
                    permissionListener: PermissionListener ? =null,
                    recordListener: RecordListener ?= null,
                    wax: Float = 1f

): MP3Recorder {
    return MP3Recorder(audioSource.type, isDebug)
        .setMaxTime(mMaxTime)
        .setMp3Quality(mp3Quality)
        .setPermissionListener(permissionListener)
        .setRecordListener(recordListener)
        .setWax(wax)
}


@JvmOverloads
fun mixRecorderBuilder( audioSource: BaseRecorder.AudioSource = BaseRecorder.AudioSource.MIC,
                       channel: Int = 2,
                       mMaxTime: Int = 1800 * 1000,
                       mp3Quality: Int = 5,
                       permissionListener: PermissionListener ? =null,
                       recordListener: RecordListener ?= null,
                       wax: Float = 1f

): MixRecorder {
    return MixRecorder(audioSource.type,channel)
        .setMaxTime(mMaxTime)
        .setMp3Quality(mp3Quality)
        .setPermissionListener(permissionListener)
        .setRecordListener(recordListener)
        .setWax(wax)
}


fun simpleRecorderBuilder(simpleName:BaseRecorder.RecorderType = BaseRecorder.RecorderType.MIX,
                          audioSource: BaseRecorder.AudioSource = BaseRecorder.AudioSource.MIC,
                          mMaxTime: Int = 1800 * 1000,
                          mp3Quality: Int = 5,
                          permissionListener: PermissionListener ? =null,
                          recordListener: RecordListener ?= null,
                          wax: Float = 1f
): BaseRecorder{
   return when(simpleName){
        BaseRecorder.RecorderType.MIX ->
            mixRecorderBuilder(audioSource = audioSource,
                mMaxTime = mMaxTime,
                mp3Quality = mp3Quality,
                permissionListener = permissionListener,
                recordListener = recordListener,
                wax = wax)
       BaseRecorder.RecorderType.SIM  ->
            recorderBuilder(audioSource = audioSource,
                mMaxTime = mMaxTime,
                mp3Quality = mp3Quality,
                permissionListener = permissionListener,
                recordListener = recordListener,
                wax = wax)
   }
}