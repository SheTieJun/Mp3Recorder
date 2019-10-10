package me.shetj.kt

import android.media.MediaRecorder
import androidx.annotation.IntRange
import me.shetj.mixRecorder.MixRecorder
import me.shetj.player.PermissionListener
import me.shetj.player.RecordListener
import me.shetj.recorder.BuildConfig
import me.shetj.recorder.MP3Recorder
import java.nio.channels.Channel

@JvmOverloads
fun recorderBuilder(audioSource: Int = MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    isDebug: Boolean = BuildConfig.DEBUG,
                    mMaxTime: Int = 1800 * 1000,
                    @IntRange(from = 0, to = 9) mp3Quality: Int = 5,
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
                       @IntRange(from = 1, to = 2) channel: Int = 2,
                       mMaxTime: Int = 1800 * 1000,
                       @IntRange(from = 0, to = 9) mp3Quality: Int = 5,
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