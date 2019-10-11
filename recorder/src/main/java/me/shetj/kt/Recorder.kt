package me.shetj.kt

import android.media.MediaRecorder
import me.shetj.mixRecorder.MixRecorder
import me.shetj.player.PermissionListener
import me.shetj.player.RecordListener
import me.shetj.recorder.BuildConfig
import me.shetj.recorder.MP3Recorder

@JvmOverloads
fun recorderBuilder(audioSource: Int = MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    isDebug: Boolean = BuildConfig.DEBUG,
                    mMaxTime: Int = 1800 * 1000,
                    mp3Quality: Int = 5,
                    permissionListener: PermissionListener ? =null,
                    recordListener: RecordListener ?= null,
                    wax: Float = 1f

): MP3Recorder {
    return MP3Recorder(audioSource, isDebug)
        .setMaxTime(mMaxTime)
        .setMp3Quality(mp3Quality)
        .setPermissionListener(permissionListener)
        .setRecordListener(recordListener)
        .setWax(wax)
}


@JvmOverloads
fun mixRecorderBuilder(audioSource: Int = MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                       channel: Int = 2,
                       mMaxTime: Int = 1800 * 1000,
                       mp3Quality: Int = 5,
                       permissionListener: PermissionListener ? =null,
                       recordListener: RecordListener ?= null,
                       wax: Float = 1f

): MixRecorder {
    return MixRecorder(audioSource,channel)
        .setMaxTime(mMaxTime)
        .setMp3Quality(mp3Quality)
        .setPermissionListener(permissionListener)
        .setRecordListener(recordListener)
        .setWax(wax)
}